package de.telekom.lab.emo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

import de.telekom.lab.emo.control.ServerManager;
import de.telekom.lab.emo.db.DBManager;

public class Emotion_BroadcastingActivity extends SherlockActivity implements
		View.OnClickListener {
	private static final String TAG = "Emotion_BroadcastingActivity";
	private static final boolean D = true;

	static final int DIALOG_USER_ID = 0;
	CircleView cView;
	SharedPreferences sharedPrefs;
	// TextView messageTextView;
	ProgressDialog pdialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (pdialog != null && pdialog.isShowing()) {
			pdialog.dismiss();
		}
		setContentView(R.layout.main);

		LinearLayout l = new LinearLayout(this);
		l.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		l.setOrientation(LinearLayout.VERTICAL);
		l.setBackgroundResource(R.drawable.gradient_bg);

		RelativeLayout l2 = new RelativeLayout(this);
		l2.setLayoutParams(new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT));
		l2.setBackgroundResource(R.drawable.test);

		int numberOfElements = Emotions.getInstance().getMaximumEmotionType();
		View[] elems = new View[numberOfElements];

		for (int i = 0; i < numberOfElements; i++) {

			Button newButton = new Button(this);
			newButton.setLayoutParams(new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT));
			newButton.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
					.getInstance().getEmotionIcon(i), 0, 0);
			newButton.setOnClickListener(this);
			newButton.setId(i);
			newButton
					.setBackgroundResource(R.drawable.main_menu_button_selector);

			elems[i] = newButton;
		}
	
		Display d = getWindowManager().getDefaultDisplay();
		@SuppressWarnings("deprecation")
		int width = d.getWidth();

		DisplayMetrics dm = getResources().getDisplayMetrics();

		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_NORMAL) == Configuration.SCREENLAYOUT_SIZE_NORMAL) {

			switch (dm.densityDpi) {
			case DisplayMetrics.DENSITY_LOW:
				cView = new CircleView(this, (width / 2) - 30, elems, 0, 0);
				System.out.println("low");
				break;
			case DisplayMetrics.DENSITY_MEDIUM:
				cView = new CircleView(this, (width / 2) - 30, elems, 0, 0);
				System.out.println("medium");
				break;
			case DisplayMetrics.DENSITY_XHIGH:
				cView = new CircleView(this, (width / 2) - 80, elems, 0, 0);
				System.out.println("xhigh");
				break;
			case DisplayMetrics.DENSITY_HIGH:
				cView = new CircleView(this, (width / 2) - 45, elems, 0, 0);
				System.out.println("high");
				break;
			default:
				break;
			}

		} else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_SMALL) == Configuration.SCREENLAYOUT_SIZE_SMALL) {
			cView = new CircleView(this, (width / 2) - 20, elems, 0, 0);
		}
		// }

		l2.addView(cView);
		l.addView(l2);
		setContentView(l);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Intent serverIntent = new Intent(this,
				de.telekom.lab.emo.control.ServerManager.class);
		startService(serverIntent);

		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showGPSEnablingSettingsAlert();
		}

		if (networkInfo == null) {
			showNetworkEnablingSettingsAlert();
		}

		Thread t = new Thread(new Runnable() {

			public void run() {
				// TODO Auto-generated method stub
				Intent LocationIntent = new Intent(
						Emotion_BroadcastingActivity.this,
						de.telekom.lab.emo.control.LocationService.class);
				startService(LocationIntent);
			}
		});
		t.start();

	}

	private void showNetworkEnablingSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		alertDialog.setTitle("Network Settings");

		alertDialog
				.setMessage("Network is not enabled. Please check your settings");

		alertDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(
								Settings.ACTION_WIRELESS_SETTINGS);
						startActivity(intent);
					}
				});

		alertDialog.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

		alertDialog.show();
	}

	private void showGPSEnablingSettingsAlert() {

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		alertDialog.setTitle("GPS Settings");

		alertDialog
				.setMessage("GPS is not enabled. Please check your settings");

		alertDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(intent);
					}
				});

		alertDialog.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

		alertDialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.emo_activity_menu, menu);
		return true;

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_USER_ID:
			Dialog dialog1 = new Dialog(this);
			dialog1.setContentView(R.layout.dialog_userid);
			TextView text = (TextView) dialog1.findViewById(R.id.userID);
			DBManager db = new DBManager(this);
			db.open();
			String code = db.getUserIdentifier();
			text.setText(code);
			dialog1.setTitle("User ID Report");
			return dialog1;
		}
		return null;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, Emotion_BroadcastingActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.map_menu:
			showProgressDialog();
			runMapActivity(null);
			return true;
		case R.id.settings_menu:
			showSettings();
			return true;
		case R.id.see_around:
			lunchAugmentedRealityActivity();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showSettings() {
		startActivity(new Intent(this, QuickPrefsActivity.class));
	}

	private void lunchAugmentedRealityActivity() {
		Intent intent = new Intent(this,
				de.telekom.lab.emo.gui.AugmentedRealityActivity.class);
		Bundle data = new Bundle();
		data.putParcelable(CopyOfMapViewActivity.DATA_SERVERMANAGER_MESSENGER,
				mService);
		intent.putExtra(CopyOfMapViewActivity.BUNDLE_MAPVIEWACTIVITY, data);
		startActivity(intent);
	}

	private void alertClearMap() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Are you sure you want to clear all locally stored emotion data?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Emotion_BroadcastingActivity.this
										.clearMapData();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void clearMapData() {
		DBManager dbManager = new DBManager(this);
		dbManager.open();
		if (dbManager.isDatabaseOpen()) {
			dbManager.deleteAll();
			dbManager.close();
		}
		Message msg = Message.obtain(null, ServerManager.MSG_RESET);
		try {
			mService.send(msg);

		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	Messenger mService = null;
	boolean mIsBound = false;
	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			Message msg = Message.obtain(null, ServerManager.MSG_RESET);
			try {
				mService.send(msg);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d("EMO", "onServiceDisconnected");
			mService = null;
		
		}
	};

	void doBindService() {
		Log.d("EMO", "doBindService");
		bindService(new Intent(this, ServerManager.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (D)
			Log.d(TAG, "doUnbindService");
		if (mService != null) {
			Message msg = Message.obtain(null, ServerManager.MSG_PUBLISH);
			try {
				if (D)
					Log.d(TAG, "doUnbindService-send message");
				mService.send(msg);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			if (D)
				Log.d(TAG, "doUnbindService-use stop Service");
			Intent serverIntent = new Intent(this,
					de.telekom.lab.emo.control.ServerManager.class);
			stopService(serverIntent);
		}

		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d("EMO", "onStart");
		if (pdialog != null && pdialog.isShowing()) {
			pdialog.dismiss();
		}
		String mapT = sharedPrefs.getString("clearmapvalues", "-1");
		Log.d("clearmap", mapT);
		int clearmap = Integer.parseInt(mapT);
		switch (clearmap) {
		case 2:
			alertClearMap();
			break;

		default:
			break;
		}

		if (mService == null)
			doBindService();
		else {
			Message msg = Message
					.obtain(null, ServerManager.MSG_TEST_MESSAGING);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
		stopAllServices();
	}

	private void stopAllServices() {
		Intent LocationIntent = new Intent(this,
				de.telekom.lab.emo.control.LocationService.class);
		stopService(LocationIntent);
	}

	public void showProgressDialog() {
		pdialog = new ProgressDialog(this);
		pdialog.setCancelable(true);
		pdialog.setMessage("Loading ...");
		pdialog.show();
	}

	public void onClick(View view) {
		click(Emotions.getInstance().getEmotionID(view.getId()));
		showProgressDialog();
		runMapActivity(view);
	}

	public void runMapActivity(View view) {
		Intent intent = new Intent(this,
				de.telekom.lab.emo.CopyOfMapViewActivity.class);
		Bundle data = new Bundle();
		data.putParcelable(CopyOfMapViewActivity.DATA_SERVERMANAGER_MESSENGER,
				mService);
		data.putBoolean(CopyOfMapViewActivity.DATA_IS_DIRECT_RUN, view == null);
		if (view != null)
			data.putInt(CopyOfMapViewActivity.DATA_SMILE_TYPE, view.getId());
		intent.putExtra(CopyOfMapViewActivity.BUNDLE_MAPVIEWACTIVITY, data);
		startActivity(intent);
	}

	private void click(int emotionType) {
		Log.d("emotion type", "" + emotionType);
		// messageCallBack("<h2>Title "+emotionType+"</h2><br><p>Description here</p>",
		// null);
		if (mService != null) {
			Message msg = Message.obtain(null, ServerManager.MSG_SMILE_TYPE);
			msg.arg1 = emotionType;
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void messageCallBack(String msg, String onClickHTML) {
	}

}

class ImageAdapter extends BaseAdapter {

	private final Emotion_BroadcastingActivity mContext;

	public ImageAdapter(Emotion_BroadcastingActivity mm) {
		mContext = mm;
	}

	public int getCount() {
		return Emotions.getInstance().getMenuCount();
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		Button imageView;
		if (convertView == null) { // if it's not recycled, initialize some
									// attributes
			imageView = new Button(mContext);

			switch (position) {

			case 0:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(0), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(0);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 1:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(1), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(1);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 2:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(2), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(2);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 3:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(3), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(3);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 4:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(4), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(4);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 5:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(5), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(5);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 6:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(6), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(6);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 7:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(7), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(7);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 8:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(8), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(8);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 9:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(9), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(9);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;

			default:
				break;
			}

		} else {
			imageView = (Button) convertView;

			switch (position) {
			case 1:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(1), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(1);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 2:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(2), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(2);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 3:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(3), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(3);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 4:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(4), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(4);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 5:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(5), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(5);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 6:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(6), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(6);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 7:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(7), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(7);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 8:

				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(8), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(8);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;
			case 9:
				imageView.setCompoundDrawablesWithIntrinsicBounds(0, Emotions
						.getInstance().getEmotionIcon(9), 0, 0);

				imageView.setOnClickListener(mContext);
				imageView.setId(9);
				imageView.setTextColor(0xffffffff);
				imageView
						.setBackgroundResource(R.drawable.main_menu_button_selector);
				break;

			default:
				break;
			}

		}
		return imageView;

	}

}