package de.telekom.lab.emo.gui;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import de.telekom.lab.emo.CopyOfMapViewActivity;
import de.telekom.lab.emo.Emotions;
import de.telekom.lab.emo.R;
import de.telekom.lab.emo.control.ServerManager;
import de.telekom.lab.emo.db.DBManager;
import de.telekom.lab.emo.db.EmoRecord;
import de.telekom.lab.emo.db.POI;
import de.telekom.lab.emo.util.ImageCache;
import de.telekom.lab.emo.util.ImageFetcher;

@SuppressLint("HandlerLeak")
public class AugmentedRealityActivity extends FragmentActivity implements
		SensorEventListener {
	private static final String TAG = "AugmentedRealityActivity";
	private static final boolean D = true;
	private SensorManager mSensorManager;
	private Sensor mOrientation, mAccelerometer, mMagneticField;
	ServerManager serverManager = new ServerManager();
	int numberOfEmosOnServer;
	public static final int PERSONAL_MODE = 1;
	public static final int GLOBAL_MODE = 2;
	public int mode;
	public boolean buttonClicked = false;

	private static final String IMAGE_CACHE_DIR = "images";
	public static final String EXTRA_IMAGE = "extra_image";

	private ImageFetcher mImageFetcher;

	ArrayList<POI> serverEmosWithRange = new ArrayList<POI>();
	// private OrientationEstimator oEstimator;

	private Preview mPreview;
	private SmileLayer smiliLayer;
	// private MessageLayer messageLayeer;
	private ReportLayer reportLayer1;
	Camera mCamera;
	int cameraCurrentlyLocked;
	SharedPreferences sharedPreferences;

	Location currentLocation;
	Location lastLoadedLocation;
	ArrayList<POI> dataStreem;
	// Thread t;
	Messenger serverMessenger;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	Toast positionToast;
	float focalLenght;
	float magnetiuteFieldDeclination = 0;
	float mwF;
	Button personalButton;
	List<EmoRecord> serverEmos = new ArrayList<EmoRecord>();

	final Handler inchandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String newRS = (String) msg.getData().get("RS");
			serverEmos = parseAllData(newRS);
			numberOfEmosOnServer = serverEmos.size();
			System.out.println("hole alle server emos");
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		loadTempData();
		mode = PERSONAL_MODE;

		// Fetch screen height and width, to use as our max size when loading
		// images as this
		// activity runs full screen
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;

		// For this sample we'll use half of the longest width to resize our
		// images. As the
		// image scaling ensures the image is larger than this, we should be
		// left with a
		// resolution that is appropriate for both portrait and landscape. For
		// best image quality
		// we shouldn't divide by 2, but this will use more memory and require a
		// larger memory
		// cache.
		final int longest = (height > width ? height : width) / 2;

		ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(
				this, IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of
													// app memory

		// The ImageFetcher takes care of loading images into our ImageView
		// children asynchronously
		mImageFetcher = new ImageFetcher(this, longest);
		mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
		mImageFetcher.setImageFadeIn(false);

		currentLocation = null;
		lastLoadedLocation = null;
		dataStreem = null;
		serverEmosWithRange = null;
		serverManager.getAllEmoRecords(inchandler);
		Bundle data = getIntent().getBundleExtra(
				CopyOfMapViewActivity.BUNDLE_MAPVIEWACTIVITY);
		if ((data) != null) {
			serverMessenger = data
					.getParcelable(CopyOfMapViewActivity.DATA_SERVERMANAGER_MESSENGER);
			registerItSelf();
		}

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagneticField = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		// Create a RelativeLayout container that will hold a SurfaceView,
		// and set it as the content of our activity.
		mPreview = new Preview(this);
		smiliLayer = new SmileLayer(this);
		// messageLayeer= new MessageLayer(this);
		// messageLayeer.setPosition(50);
		reportLayer1 = new ReportLayer(this);

		// newLayer=new SmileLayer(this);

		setContentView(mPreview);

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		personalButton = new Button(this);
		personalButton.setText("PERSONAL");
		personalButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mode = PERSONAL_MODE;
				buttonClicked = !buttonClicked;
				if (buttonClicked) {
					personalButton.setText("GLOBAL");
				} else {
					personalButton.setText("PERSONAL");
				}
			}
		});

		addContentView(smiliLayer, new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		addContentView(personalButton, lp);

		addContentView(reportLayer1, new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public List<EmoRecord> parseAllData(String newRS) {
		List<EmoRecord> em = new ArrayList<EmoRecord>();
		StringReader sr = new StringReader(newRS);
		InputSource is = new InputSource(sr);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.parse(is);
			Element root = dom.getDocumentElement();
			NodeList items = root.getElementsByTagName("p");
			if (items.getLength() > 0) {
				for (int i = 0; i < items.getLength(); i++) {
					Node item = items.item(i);
					double lat = Double.parseDouble(item.getAttributes()
							.item(0).getNodeValue());
					double lon = Double.parseDouble(item.getAttributes()
							.item(1).getNodeValue());
					int smileType = Integer.parseInt(item.getAttributes()
							.item(2).getNodeValue());
					long time = Long.parseLong(item.getAttributes().item(3)
							.getNodeValue());
					EmoRecord e = new EmoRecord();
					e.setEmoType(smileType);
					e.setLat(lat);
					e.setLon(lon);
					e.setTime(time);
					em.add(e);
				}
			}
		} catch (Exception e) {

		}
		return em;
	}

	boolean hasReportCreated = false;

	@Override
	protected void onResume() {
		super.onResume();
		int delay = SensorManager.SENSOR_DELAY_GAME;
		lastLoadedLocation = null;
		compassTempArraySin = new double[10];
		compassTempArrayCos = new double[10];
		compassTempArrayPointer = 0;

		mSensorManager.registerListener(this, mOrientation, delay);
		mSensorManager.registerListener(this, mAccelerometer, delay);
		mSensorManager.registerListener(this, mMagneticField, delay);
		lastCompass = 500;
		// Open the default i.e. the first rear facing camera.
		mCamera = Camera.open();
		horizontalViewAngle = mCamera.getParameters().getHorizontalViewAngle();
		verticalViewAngle = mCamera.getParameters().getVerticalViewAngle();
		focalLenght = (float) (mCamera.getParameters().getFocalLength() * 0.001);

		Log.d("CAM", "horizontalViewAngle:" + horizontalViewAngle);
		Log.d("CAM", "verticalViewAngle:" + verticalViewAngle);
		horizontalViewAngleHalf = horizontalViewAngle / 2;
		Log.d("CAM", "horizontalViewAngleRHalf:" + horizontalViewAngleHalf);

		mPreview.setCamera(mCamera);
		if (currentLocation == null) {
			positionToast = Toast.makeText(this, "Waiting for location",
					Toast.LENGTH_LONG);
			positionToast.show();
		}
		mwF = -1;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);

		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d("onAccuracyChanged", "accuracy:" + accuracy);
	}

	SensorsData sensorData = new SensorsData();
	float[] RArray = new float[9];
	float[] I = new float[9];
	float[] Rout = new float[9];
	float[] ori = new float[3];
	float[] currentOri = new float[3];
	float lastCompass = 500;
	boolean dataConverted;
	float lastOri;
	double[] compassTempArraySin = new double[10];
	double[] compassTempArrayCos = new double[10];
	double smoothedCompassSin, smoothedCompassCos;
	int compassTempArrayPointer;
	float smootherCompassRounded;
	float horizontalViewAngle, verticalViewAngle;
	double horizontalViewAngleHalf;

	@Override
	public void onSensorChanged(SensorEvent event) {

		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			// Log.d(TAG,"TYPE_ACCELEROMETER");
			sensorData.gravity[0] = event.values[0];
			sensorData.gravity[1] = event.values[1];
			sensorData.gravity[2] = event.values[2];
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			// Log.d(TAG,"TYPE_MAGNETIC_FIELD");
			sensorData.geomagnetic[0] = event.values[0];
			sensorData.geomagnetic[1] = event.values[1];
			sensorData.geomagnetic[2] = event.values[2];

			break;
		case Sensor.TYPE_ORIENTATION:
			// if (smiliLayer!=null)
			// messageLayeer.setOrientationData(event.values[0],event.values[1],event.values[2]);
			break;

		}

		if (currentLocation != null
				&& event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if (mwF == -1) {
				mwF = (float) ((focalLenght * 2 * Math.tan(Math
						.toRadians(horizontalViewAngle / 2))) / mPreview.mPreviewSize.width);
				halfPreviewWidth = mPreview.mPreviewSize.width / 2;
				smiliLayer.setImageSizeRange(mPreview.mPreviewSize.height / 4,
						mPreview.mPreviewSize.height / 20);
				Log.d("getPointInPic", "prWConvertor:" + mwF);
			}
			dataConverted = SensorManager.getRotationMatrix(RArray, I,
					sensorData.gravity, sensorData.geomagnetic);
			if (dataConverted) {
				SensorManager.remapCoordinateSystem(RArray,
						SensorManager.AXIS_X, SensorManager.AXIS_Z, Rout);


				// Rout=RArray;
				SensorManager.getOrientation(Rout, ori);
				currentOri[0] = (float) Math.toDegrees(ori[0]);
				currentOri[1] = (float) Math.toDegrees(ori[1]);
				currentOri[2] = (float) Math.toDegrees(ori[2]);
			

				compassTempArraySin[compassTempArrayPointer] = (float) Math
						.sin(ori[0]);
				compassTempArrayCos[compassTempArrayPointer] = (float) Math
						.cos(ori[0]);
				compassTempArrayPointer++;
				if (compassTempArrayPointer >= compassTempArraySin.length)
					compassTempArrayPointer = 0;

				
				if (Math.abs(currentOri[1]) >= 30) {
					smiliLayer.setEmoList(null);
				}
				smoothedCompassSin = 0;
				smoothedCompassCos = 0;
				for (int i = 0; i < compassTempArraySin.length; i++) {
					smoothedCompassSin += compassTempArraySin[i];
					smoothedCompassCos += compassTempArrayCos[i];
				}
				smoothedCompassSin /= compassTempArraySin.length;
				smoothedCompassCos /= compassTempArraySin.length;
				smootherCompassRounded = (float) Math.toDegrees(Math.atan2(
						smoothedCompassSin, smoothedCompassCos));

				if (smootherCompassRounded < 0)
					smootherCompassRounded += 360;
				

				if (Math.abs(smootherCompassRounded - lastCompass) > 5
						&& Math.abs(currentOri[1]) < 30
						&& serverEmosWithRange != null) {
					lastCompass = smootherCompassRounded;
					// is it significant change?
					checkWhatCanSee(smootherCompassRounded);
					if (smiliLayer != null) {
						smiliLayer.setEmoList(pointArray.values());
					}
				}

			}
		}

	}

	int maxDissInMetter = 20000;
	StringBuilder msg = new StringBuilder();
	double tmp;
	HashMap<Integer, POI> pointArray = new HashMap<Integer, POI>();

	ArrayList<POI> emoInCurrentPosition = new ArrayList<POI>();
	int distanceRangeTobeInCurrentPoint = 5;

	private synchronized void checkWhatCanSee(float ori) {
		Log.d("checkWhatCanSee", "" + ori);
		msg.setLength(0);
		float[] distances = new float[2];
		boolean createCurrentPOIList = false;
		if (!hasReportCreated) {
			emoInCurrentPosition.clear();
			hasReportCreated = true;
			createCurrentPOIList = true;
		}

		if (personalButton.getText().equals("PERSONAL")) {
			pointArray.clear();

			for (POI p : dataStreem) {
				Location.distanceBetween(currentLocation.getLatitude(),
						currentLocation.getLongitude(), p.lat, p.lon, distances);
				if (distances[1] < 0)
					distances[1] += 360;

				Log.d("checkWhatCanSee", "Diss to " + p.name + ":"
						+ distances[0]);
				Log.d("checkWhatCanSee", "Bearing to " + p.name + ":"
						+ distances[1]);
				p.distance = distances[0];

				if (createCurrentPOIList
						&& distances[0] <= distanceRangeTobeInCurrentPoint)
					emoInCurrentPosition.add(p);

				if (distances[0] < maxDissInMetter
						&& isInViewPoint2(ori, distances[1])) {
					Log.d("checkWhatCanSee", "Bingo! id:" + p.getIDNumber());
					if (msg.length() > 0)
						msg.append(",");
					msg.append(p.distance);
					getPointInPic(ori, distances[1], distances[0], p);
					pointArray.put(p.getID(), p);
				} else
					pointArray.remove(p.getID());
			}
		} else {
			pointArray.clear();

			for (POI p : serverEmosWithRange) {

				Location.distanceBetween(currentLocation.getLatitude(),
						currentLocation.getLongitude(), p.lat, p.lon, distances);
				if (distances[1] < 0)
					distances[1] += 360;

				Log.d("checkWhatCanSee", "Diss to " + p.name + ":"
						+ distances[0]);
				Log.d("checkWhatCanSee", "Bearing to " + p.name + ":"
						+ distances[1]);
				p.distance = distances[0];

				if (createCurrentPOIList
						&& distances[0] <= distanceRangeTobeInCurrentPoint)
					emoInCurrentPosition.add(p);

				if (distances[0] < maxDissInMetter
						&& isInViewPoint2(ori, distances[1])) {
					Log.d("checkWhatCanSee", "Bingo! id:" + p.getIDNumber());
					if (msg.length() > 0)
						msg.append(",");
					msg.append(p.distance);
					getPointInPic(ori, distances[1], distances[0], p);
					pointArray.put(p.getID(), p);
				} else
					pointArray.remove(p.getID());
			}
		}
		if (createCurrentPOIList)
			reportLayer1.setData(emoInCurrentPosition, "");
	}

	int halfPreviewWidth;

	private void getPointInPic(float ori, float bearing, float distance,
			POI point) {

		double kAngel = Math.toRadians(Math.abs(ori - bearing));

		double x0 = (Math.sin(kAngel) * distance);
		double y0 = (Math.cos(kAngel) * distance);
		
		point.y = (int) (mPreview.mPreviewSize.height / 10 + Math.random()
				* mPreview.mPreviewSize.height * 0.8);
		point.x = (int) (((x0 * focalLenght) / y0) / mwF);
		if (bearing > ori)
			point.x *= -1;
		point.x = halfPreviewWidth - point.x;
		Log.d("getPointInPic", "x:" + point.x + " Diss: " + distance
				+ ", ori: " + ori + " bearing: " + bearing + "K-Angel:"
				+ kAngel);

	}

	private boolean isInViewPoint2(float ori, float bearing) {
		Log.d("checkWhatCanSee", "Ori:" + ori + ", bearing:" + bearing
				+ ", hVA:" + horizontalViewAngleHalf);
		double diff = Math.abs(ori - bearing);
		if (diff > 180)
			diff = 360 - diff;
		Log.d("checkWhatCanSee", "DIFF:" + diff);
		if (diff < horizontalViewAngleHalf)
			Log.d("IN", "Ori:" + ori + ", bearing:" + bearing + ", hVA:"
					+ horizontalViewAngleHalf + " , DIFF:" + diff);
		return diff < horizontalViewAngleHalf;
	}

	public static int maximumRadious = 500;

	private void updateCurrentPosition(Location l) {
		currentLocation = l;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		maximumRadious = sharedPreferences.getInt("Distance", -1);

		if (lastLoadedLocation == null
				|| currentLocation.distanceTo(lastLoadedLocation) > 50) {
			lastLoadedLocation = currentLocation;
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					Log.d("SML", "RUN");
					DBManager dbmanager = new DBManager(
							AugmentedRealityActivity.this);
					dbmanager.open();
					if (dbmanager.isDatabaseOpen()) {

						dataStreem = dbmanager.getAllEmoArround(
								currentLocation, 50, maximumRadious);

						// server
						serverEmosWithRange = new ArrayList<POI>();
						List<EmoRecord> emos = serverEmos;
						double distance = 0;
						double userLon = currentLocation.getLongitude();
						double userLat = currentLocation.getLongitude();
						int counter = 0;
						for (EmoRecord emr : emos) {
							double emoLon = emr.getLon();
							double emoLat = emr.getLat();

							distance = AugmentedRealityActivity.getDistance(
									emoLat, emoLon, userLat, userLon);
							counter++;
							if (distance < maximumRadious) {
								POI p = new POI();
								p.lon = emr.getLon();
								p.lat = emr.getLat();
								p.name = Emotions.getInstance()
										.getEmotionTextByType(emr.getEmoType());
								p.type = emr.getEmoType();
								// p.setID(emr.getId());
								p.setID(counter);
								serverEmosWithRange.add(p);
							}
						}

						hasReportCreated = false;

						smiliLayer.resetCashedBitmaps();
						reportLayer1.reset();
					}
					dbmanager.close();
				}
			});
			t.start();
		}

		GeomagneticField gf = new GeomagneticField((float) l.getLatitude(),
				(float) l.getLongitude(), (float) l.getAltitude(), l.getTime());
		magnetiuteFieldDeclination = gf.getDeclination();
	}

	public static double getDistance(double placeLat, double placeLon,
			double userLat, double userLon) {

		double lat = (placeLat + userLat) / 2 * 0.01745;
		double dx = 111.3 * Math.cos(lat) * (placeLon - userLon);
		double dy = 111.3 * (placeLat - userLat);
		double distance = Math.sqrt(dx * dx + dy * dy) / 100;

		return distance;
	}


	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ServerManager.MSG_UPDATE_CURRENT_POSITION:
				updateCurrentPosition((Location) msg.obj);
				Log.d("POS", "" + currentLocation.getAccuracy());
				if (positionToast != null)
					positionToast.cancel();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void registerItSelf() {
		Message msg = Message.obtain(null, ServerManager.MSG_REG_AR_ACTIVITY);
		try {
			msg.replyTo = mMessenger;
			serverMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void unRegisterItSelf() {
		Message msg = Message
				.obtain(null, ServerManager.MSG_UN_REG_AR_ACTIVITY);
		try {
			msg.replyTo = mMessenger;
			serverMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unRegisterItSelf();
		super.onDestroy();
	}

}

class SensorsData {
	float[] gravity;
	float[] geomagnetic;

	SensorsData() {
		gravity = new float[3];
		geomagnetic = new float[3];
	}

}

class MessageLayer extends View {
	public MessageLayer(Context context) {
		super(context);
		this.context = context;
	}

	float azimuth, pitch, roll;
	float compass;
	int x1, x2, x3;
	Context context;
	String msg = "";
	float nazi, npitch, nroll, dec;

	public void setOrientationData(float azimuth, float pitch, float roll) {
		this.azimuth = azimuth;
		this.pitch = pitch;
		this.roll = roll;
		compass = (azimuth + pitch + 360) % 360;
		this.invalidate();
		x1 = 50;
		x2 = x1 + 50;
		;
	}

	public void setMessage(String msg) {
		this.msg = msg;
		this.invalidate();
	}

	public void setCalculatedOrientationData(float azimuth, float pitch,
			float roll, float dec) {
		this.nazi = azimuth;
		this.npitch = pitch;
		this.nroll = roll;
		this.dec = dec;
		this.invalidate();
		x3 = 150;
	}

	public void setPosition(int p) {
		this.x1 = p;
		this.x2 = p + 50;
		;
	}

	@Override
	protected void onDraw(Canvas canvas) {

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setTextSize(20);
		paint.setColor(Color.GREEN);
		canvas.drawText(String.format("Azimuth: %1f, Pitch: %2f, Roll: %3f",
				azimuth, pitch, roll), x1, x1, paint);

		canvas.drawText(String.format("Compass: %1f", compass), x2, x2, paint);

		canvas.drawText(String.format(
				"NEW Azimuth: %1f, Pitch: %2f, Roll: %3f, Decl: %4f", nazi,
				npitch, nroll, dec), x1, x3, paint);
		canvas.drawText(msg, x1, x3 + 50 + 50, paint);
		super.onDraw(canvas);
	}

}

class SmileLayer extends View {

	public SmileLayer(Context context) {
		super(context);
		this.context = context;
		maxSize = 150;
		minSize = 20;
	}

	int maxSize, minSize;
	Context context;
	ArrayList<POI> pointList = new ArrayList<POI>();
	HashMap<Integer, Bitmap> pointAroundMe = new HashMap<Integer, Bitmap>();

	public void setEmoList(Collection<POI> p) {
		this.pointList.clear();
		if (p != null) {
			this.pointList.addAll(p);
			Collections.sort(pointList);
			this.invalidate();
		}
	}

	public void setImageSizeRange(int maxSize, int minSize) {
		this.maxSize = maxSize;
		this.minSize = minSize;
	}

	public void resetCashedBitmaps() {
		pointAroundMe.clear();
	}

	int w;
	Bitmap b;

	@Override
	protected void onDraw(Canvas canvas) {
		if (pointList.size() > 0) {
			Paint paint = new Paint();
			for (POI p : pointList) {
				b = pointAroundMe.get(p.getID());
				if (b == null) {
					b = Emotions.getInstance().getBitmap(p.type, context);
					w = (int) ((1 - p.distance
							/ AugmentedRealityActivity.maximumRadious)
							* (maxSize - minSize) + minSize);
					// b = Bitmap.createScaledBitmap(b, w, w, false);
					pointAroundMe.put(p.getID(), b);
				}
				paint.setAlpha(Math
						.min((int) ((1 - p.distance
								/ AugmentedRealityActivity.maximumRadious) * 100 + 170),
								254));
				canvas.drawBitmap(b, p.x - b.getWidth() / 2,
						p.y - b.getHeight() / 2, paint);
			}
		}
		super.onDraw(canvas);
	}
}

class ReportLayer extends View {

	public ReportLayer(Context context) {
		super(context);
		this.context = context;
		size = 100;
		margine = 10;
		b = null;
		right = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.right);
		left = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.left);
	}

	Context context;
	ArrayList<POI> pointList = new ArrayList<POI>();
	int type;
	String message;
	int count;
	int size;
	int margine;
	RectF background;

	public void setData(Collection<POI> data, String message) {
		int[] typeCounter = new int[Emotions.getInstance()
				.getMaximumEmotionType() + 1];
		for (POI p : data)
			typeCounter[p.type]++;
		int maxCountType = -1;

		for (int i = 0; i < typeCounter.length; i++)
			if (typeCounter[i] > maxCountType) {
				maxCountType = typeCounter[i];
				type = i;
			}
		this.message = message;
		this.count = maxCountType;
		if (maxCountType != 0) {
			b = Emotions.getInstance().getBitmap(type, context);
			b = Bitmap.createScaledBitmap(b, size - margine * 2, size - margine
					* 2, true);
		} else
			b = null;

		this.invalidate();
	}

	public void reset() {
		b = null;
	}

	public void clearData() {
		this.message = "";
		b = null;
		this.invalidate();
	}

	int w;
	Bitmap b, right, left;

	@Override
	protected void onDraw(Canvas canvas) {
		if (b != null) {
			Paint paint = new Paint();
			paint.setColor(Color.GRAY);
			paint.setStrokeWidth(3);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setAlpha(100);
			canvas.translate(this.getWidth() - size - 30, 30);
			background = new RectF(0, 0, size, size);
			canvas.drawRoundRect(background, 20.0f, 20.0f, paint);
			canvas.drawBitmap(b, margine, margine, null);
			
		}
		super.onDraw(canvas);
	}
}

class Preview extends ViewGroup implements SurfaceHolder.Callback {
	private final String TAG = "Preview";

	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Size mPreviewSize;
	List<Size> mSupportedPreviewSizes;
	Camera mCamera;

	Preview(Context context) {
		super(context);

		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			mSupportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// We purposely disregard child measurements because act as a
		// wrapper to a SurfaceView that centers the camera preview instead
		// of stretching it.
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (mSupportedPreviewSizes != null) {
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width,
					height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				previewWidth = mPreviewSize.width;
				previewHeight = mPreviewSize.height;
			}

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height
						/ previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0,
						(width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width
						/ previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width,
						(height + scaledChildHeight) / 2);
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			requestLayout();

			mCamera.setParameters(parameters);
			mCamera.startPreview();
		}
	}

}