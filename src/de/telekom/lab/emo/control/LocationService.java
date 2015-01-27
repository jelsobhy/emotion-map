package de.telekom.lab.emo.control;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class LocationService extends Service implements LocationListener {
	// Debugging
	private static final String TAG = "LocationService";
	private static final boolean D = true;

	Location lastKnownLocation;
	LocationManager locationManager;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		super.onStartCommand(intent, flags, startId);

		if (D)
			Log.d(TAG, "onCreate_LocationService");
				locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Creating a criteria object to retrieve provider
		Criteria criteria = new Criteria();
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setPowerRequirement(Criteria.POWER_LOW);

		// Getting the name of the best provider
		String provider = locationManager.getBestProvider(criteria, true);

		if (provider != null) {
			// Getting Current Location
			lastKnownLocation = locationManager.getLastKnownLocation(provider);
		}

		if (mService == null)
			doBindServerService();
		else if (lastKnownLocation != null)
			publishLocation(lastKnownLocation);
		return START_STICKY;
	}

	Messenger mService = null;
	boolean mIsBound = false;
	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			if (lastKnownLocation != null){
				publishLocation(lastKnownLocation);
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			if (D)
				Log.d(TAG, "onServiceDisconnected");
			mService = null;
		}
	};

	private void doBindServerService() {
		if (D)
			Log.d(TAG, "doBindServerService");
		bindService(new Intent(this, ServerManager.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (D)
			Log.d(TAG,
					"onLocationChanged_LocationService: "
							+ location.getAccuracy() + "lat:"
							+ location.getLatitude() + "lon:"
							+ location.getLongitude());
		if (isBetterLocation(location,lastKnownLocation)){
		lastKnownLocation = location;
		publishLocation(lastKnownLocation);
		}
	}

	public void publishLocation(Location l) {
		Message msg = Message.obtain(null, ServerManager.MSG_POSITION);
		msg.obj = l;
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		if (D)
			Log.d(TAG, "onProviderDisabled:" + provider);
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		if (D)
			Log.d(TAG, "onProviderEnabled");
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (D)
			Log.d(TAG, "onStatusChanged");
		// TODO Auto-generated method stub

	}

	public static final int TWO_MINUTES = 1000 * 60 * 4;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}
		if (location == null) {
			// A new location is always better than no location
			return false;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 100;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Override
	public void onDestroy() {
		locationManager.removeUpdates(this);
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
		super.onDestroy();
	}
}
