package de.telekom.lab.emo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import pl.mg6.android.maps.extensions.ClusteringSettings;
import pl.mg6.android.maps.extensions.GoogleMap;
import pl.mg6.android.maps.extensions.GoogleMap.InfoWindowAdapter;
import pl.mg6.android.maps.extensions.GoogleMap.OnInfoWindowClickListener;
import pl.mg6.android.maps.extensions.Marker;
import pl.mg6.android.maps.extensions.SupportMapFragment;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;

import de.telekom.lab.emo.control.ServerManager;
import de.telekom.lab.emo.db.DBManager;
import de.telekom.lab.emo.db.EmoRecord;
import de.telekom.lab.emo.gui.DateRangeDialog;
import de.telekom.lab.emo.gui.RangeChangeListener;

@SuppressLint({ "UseValueOf", "HandlerLeak" })
public class MapViewActivity extends FragmentActivity implements
		RangeChangeListener, LocationListener {

	// layouts
	LinearLayout linearLayout;
	SupportMapFragment mapFragment;

	// control
	DBManager dbManager;
	ServerManager serverManager = new ServerManager();
	Messenger serverMessenger;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	// map and location
	Marker marker;
	GoogleMap mapView;
	Location currentLocation;
	LocationManager locationManager;
	HashMap<Bitmap, Marker> markerHashMap = new HashMap<Bitmap, Marker>();
	ArrayList<EmoRecord> allPoints = new ArrayList<EmoRecord>();
	List<EmoRecord> serverEmos = new ArrayList<EmoRecord>();
	EditText mapSearch;

	// booleans
	private static final boolean D = true;
	boolean isItDirectlyStarted = false;
	boolean isCurrentSmileAdded;
	boolean isCarte = true;
	boolean result = false;

	// Strings
	private static final String TAG = "MapViewActivity";
	public final static String BUNDLE_MAPVIEWACTIVITY = "BUNDLE_MapViewActivity";
	public final static String DATA_SMILE_TYPE = "DATA_SMILE_TYPE";
	public final static String DATA_IS_DIRECT_RUN = "DATA_IS_DIRECT_RUN";
	public final static String DATA_SERVERMANAGER_MESSENGER = "DATA_SERVERMANAGER_MESSENGER";

	// integers
	int smileType = Emotions.EMOTIO_UNKNOWN;
	int numberOfEmosOnServer;
	public static final int DIALOG_REPORT_ID = 0;
	public static final int DIALOG_TIME_RANGE_ID = 1;
	EmoRecord currentEmorecord;

	// buttons, bars, formats
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z",
			Locale.GERMANY);
	Toast locationToast;
	ProgressBar progreesBar;
	ImageButton ib;
	Context mContext;
	EditText mapSearchBox;
	ArrayList<Integer> mSelectedItems;
	ListView listView;
	Button getChoice;
	CustomAdapter adapter;
	List<String> markerTexts;
	AutoCompleteTextView editTextAddress;
	ProgressDialog pdialog;

	// settings
	int mapType = 0;
	SharedPreferences sharedPrefs;
	boolean clustering;
	boolean editorIsClicked = false;
	String provider;

	// handler
	private final MutableData[] dataArray = {
			new MutableData(6, new LatLng(-50, 0)),
			new MutableData(28, new LatLng(-52, 1)),
			new MutableData(496, new LatLng(-51, -2)), };

	final Handler inchandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String newRS = (String) msg.getData().get("RS");
			serverEmos = parseAllData(newRS);
			numberOfEmosOnServer = serverEmos.size();
		}

	};

	private final Handler handler = new Handler();
	private final Runnable dataUpdater = new Runnable() {

		@Override
		public void run() {
			for (MutableData data : dataArray) {
				data.value = 7 + 3 * data.value;
			}
			onDataUpdate();
			handler.postDelayed(this, 1000);
		}
	};

	public MapViewActivity(Context context) {
		mContext = context;
	}

	public MapViewActivity() {
		mContext = this;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		progreesBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBarCounter = 0;
		currentEmorecord = null;
		isCarte = true;

		if (D)
			Log.d(TAG, "onCreate_MapViewActivity");
		Bundle data = getIntent().getBundleExtra(BUNDLE_MAPVIEWACTIVITY);
		if ((data) != null) {
			serverMessenger = data.getParcelable(DATA_SERVERMANAGER_MESSENGER);
			registerItSelf();
			isItDirectlyStarted = data.getBoolean(DATA_IS_DIRECT_RUN, true);
			smileType = data.getInt(DATA_SMILE_TYPE, Emotions.EMOTIO_UNKNOWN);
			if (D)
				Log.d(TAG, "smileType: " + smileType);
		} else
			isItDirectlyStarted = true;

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		// String synch = sharedPrefs.getString("synchronizedb", "-1");
		// int synchT = Integer.parseInt(synch);
		// if(synchT == 2){
		// synchronizeWithServer();
		// }

		setContentView(R.layout.map_view);

		editTextAddress = (AutoCompleteTextView) findViewById(R.id.actv_country);
		editTextAddress.setVisibility(View.GONE);
		editTextAddress.setAdapter(new AutoCompleteAdapter(this));

		// Getting Google Play availability status
		int status = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());
		// Showing status
		if (status != ConnectionResult.SUCCESS) {
			// Google Play Services are not available
			int requestCode = 10;

			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					requestCode);
			dialog.show();
		} else {

			setUpMapIfNeeded();
		}

		// Getting LocationManager object from System Service
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		// Creating a criteria object to retrieve provider

		Criteria criteria = new Criteria();
		// criteria.setAccuracy(Criteria.ACCURACY_FINE);
		// criteria.setAltitudeRequired(false);
		// criteria.setBearingRequired(false);
		// criteria.setCostAllowed(true);
		// criteria.setPowerRequirement(Criteria.POWER_LOW);

		// Getting the name of the best provider
		provider = locationManager.getBestProvider(criteria, true);
		// Getting Current Location
		if (provider != null) {
			currentLocation = locationManager.getLastKnownLocation(provider);
			// if(mapView.getMyLocation() !=null){
			// onLocationChanged(mapView.getMyLocation());
			// } else {
			// onLocationChanged(currentLocation);
			// }
			locationManager.requestLocationUpdates(provider, 20, 0, this);
		}

		// LocationManager locationManager = (LocationManager) this
		// .getSystemService(Context.LOCATION_SERVICE);
		// Location mapCenter2 = locationManager
		// .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		// Location mapCenter1 = locationManager
		// .getLastKnownLocation(LocationManager.GPS_PROVIDER);
		//
		// currentLocation = mapCenter1;
		// LatLng centerPoint;
		// if (mapCenter1 == null
		// || LocationUtility.getInstance().isBetterLocation(mapCenter2,
		// mapCenter1))
		// currentLocation = mapCenter2;
		//
		// if (currentLocation == null) {
		// centerPoint = new LatLng(52.516325, 13.376684);
		// if (D)
		// Log.d(TAG, "current position: null");
		// } else {
		// centerPoint = new LatLng(
		// (int) (currentLocation.getLatitude() * 1000000),
		// (int) (currentLocation.getLongitude() * 1000000));
		// Calendar c = Calendar.getInstance();
		// c.setTimeInMillis(currentLocation.getTime());
		// if (D)
		// Log.d(TAG,
		// "Last known position:" + currentLocation.getAccuracy()
		// + ", Time:" + sdf.format(c.getTime()));
		// }
		//
		// if (currentLocation == null || !isItDirectlyStarted) {
		// locationToast = Toast.makeText(this, "Waiting for location",
		// Toast.LENGTH_LONG);
		// locationToast.show();
		// }

		isCurrentSmileAdded = false;
		serverManager.getAllEmoRecords(inchandler);

		// mapView.moveCamera(CameraUpdateFactory.newLatLng(centerPoint));

		clustering = sharedPrefs.getBoolean("clustering", false);
		if (clustering) {
			mapView.setClustering(new ClusteringSettings()
					.iconDataProvider(new DemoIconProvider(getResources())));
		}

		editorIsClicked = !editorIsClicked;
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title_map);
		addActionListeners();
		checkFilterActive();

	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.

		if (mapView == null) {
			mapView = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.mapview)).getExtendedMap();

			// Check if we were successful in obtaining the map.
			if (mapView != null) {
				// The Map is verified. It is now safe to manipulate the map.
				isCarte = !isCarte;
				mapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				String mapT = sharedPrefs.getString("updateIntervalValues",
						"-1");
				mapType = Integer.parseInt(mapT);
				// if(mapT.equals("Hybrid")) {
				// mapView.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				// }else if(mapT.equals("Normal")){
				// mapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				// }else if(mapT.equals("Sattelite")){
				// mapView.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				// }else if(mapT.equals("Terrain")){
				// mapView.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
				// }
				switch (mapType) {
				case 1:
					mapView.setMapType(GoogleMap.MAP_TYPE_HYBRID);
					break;
				case 2:
					mapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					break;
				case 3:
					mapView.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
					break;
				case 4:
					mapView.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
					break;
				default:
					break;
				}
				UiSettings settings = mapView.getUiSettings();
				settings.setZoomControlsEnabled(true);
				settings.setMyLocationButtonEnabled(false);
				settings.setCompassEnabled(true);
				settings.setAllGesturesEnabled(true);
				mapView.setMyLocationEnabled(true);

				mapView.setInfoWindowAdapter(new InfoWindowAdapter() {

					private TextView tv;
					{
						tv = new TextView(MapViewActivity.this);
						tv.setTextColor(Color.BLACK);
					}

					private final Collator collator = Collator.getInstance();
					private final Comparator<Marker> comparator = new Comparator<Marker>() {
						@Override
						public int compare(Marker lhs, Marker rhs) {
							String leftTitle = lhs.getTitle();
							String rightTitle = rhs.getTitle();
							if (leftTitle == null && rightTitle == null) {
								return 0;
							}
							if (leftTitle == null) {
								return 1;
							}
							if (rightTitle == null) {
								return -1;
							}
							return collator.compare(leftTitle, rightTitle);
						}
					};

					@Override
					public View getInfoWindow(Marker marker) {
						return null;
					}

					@Override
					public View getInfoContents(Marker marker) {
						if (marker.isCluster()) {
							List<Marker> markers = marker.getMarkers();
							int i = 0;
							String text = "";
							while (i < 3 && markers.size() > 0) {
								Marker m = Collections.min(markers, comparator);
								String title = m.getTitle();
								if (title == null) {
									break;
								}
								text += title + "\n";
								markers.remove(m);
								i++;
							}
							if (text.length() == 0) {
								text = "Markers with mutable data";
							} else if (markers.size() > 0) {
								text += "and " + markers.size() + " more...";
							} else {
								text = text.substring(0, text.length() - 1);
							}
							tv.setText(text);
							return tv;
						} else {
							Object data = marker.getData();
							if (data instanceof MutableData) {
								MutableData mutableData = (MutableData) data;
								tv.setText("Value: " + mutableData.value);
								return tv;
							}
						}

						return null;
					}
				});

				mapView.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

					@Override
					public void onInfoWindowClick(Marker marker) {
						if (marker.isCluster()) {
							List<Marker> markers = marker.getMarkers();
							Builder builder = LatLngBounds.builder();
							for (Marker m : markers) {
								builder.include(m.getPosition());
							}
							LatLngBounds bounds = builder.build();
							mapView.animateCamera(CameraUpdateFactory
									.newLatLngBounds(
											bounds,
											getResources()
													.getDimensionPixelSize(
															R.dimen.padding)));
						}
					}
				});

				// Set default zoom
				// mapView.moveCamera(CameraUpdateFactory.zoomTo(15f));

			}
		}
	}

	private void onDataUpdate() {
		Marker m = mapView.getMarkerShowingInfoWindow();
		if (m != null && !m.isCluster() && m.getData() instanceof MutableData) {
			m.showInfoWindow();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(dataUpdater);
		mapView.setMyLocationEnabled(false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		showProgressBar(true);
		try {
			MapsInitializer.initialize(this);
		} catch (GooglePlayServicesNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (provider != null) {
			locationManager.requestLocationUpdates(provider, 20, 0, this);
		}
		handler.post(dataUpdater);
		editTextAddress.setAdapter(new AutoCompleteAdapter(this));
		mapView.setMyLocationEnabled(true);
		serverManager.getAllEmoRecords(inchandler);
		editorIsClicked = !editorIsClicked;

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (D)
			Log.d(TAG, "onStart");
		if (!isItDirectlyStarted && !isCurrentSmileAdded)
			showProgressBar(true);
		loadData(this.mode);
	}

	private void addActionListeners() {
		new Runnable() {
			@Override
			public void run() {
				if (mapView.getMyLocation() != null) {
					onLocationChanged(mapView.getMyLocation());
				} else {
					onLocationChanged(currentLocation);
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showProgressBar(false);
						((ImageButton) findViewById(R.id.imageViewLocation))
								.setImageDrawable(getResources().getDrawable(
										R.drawable.menu_map_location_found));
					}
				});

			}
		};

		((ImageButton) findViewById(R.id.imageViewLocation))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mapView.getMyLocation() != null) {
							onLocationChanged(mapView.getMyLocation());
						} else {
							if (currentLocation != null) {
								onLocationChanged(currentLocation);
							}
						}

					}
				});

		((ImageButton) findViewById(R.id.imageViewPrivacy))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (MapViewActivity.this.mode == PERSONAL_INFO) {
							MapViewActivity.this.loadData(Global_INFO);
						} else {
							MapViewActivity.this.loadData(PERSONAL_INFO);
						}

					}
				});

		((ImageButton) findViewById(R.id.imageViewShare))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						editTextAddress.setVisibility(View.GONE);
						showShareContentMenu();
					}
				});

		((ImageButton) findViewById(R.id.imageViewTimeSettings))
				.setOnClickListener(new OnClickListener() {
					@Override
					@SuppressWarnings("deprecation")
					public void onClick(View v) {
						editTextAddress.setVisibility(View.GONE);
						showDialog(DIALOG_TIME_RANGE_ID);
					}
				});

		((ImageButton) findViewById(R.id.imageViewRefresh))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						editTextAddress.setVisibility(View.GONE);
						loadData(mode);
					}
				});

		((ImageButton) findViewById(R.id.imageViewSearch))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						editorIsClicked = !editorIsClicked;
						if (editorIsClicked) {
							editTextAddress.setVisibility(View.VISIBLE);
							((ImageButton) findViewById(R.id.imageViewSearch))
									.setImageResource(R.drawable.ic_menu_close_clear_cancel);
						} else {
							editTextAddress.setVisibility(View.GONE);
							((ImageButton) findViewById(R.id.imageViewSearch))
									.setImageResource(R.drawable.ic_menu_search);
						}

					}
				});

		((ImageButton) findViewById(R.id.imageViewFilter))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						showFilterMenu();
					}
				});
	}

	public void showFilterMenu() {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle("Filter Emotions you like");
		dialog.setContentView(R.layout.item);
		markerTexts = new ArrayList<String>();
		listView = (ListView) dialog.findViewById(R.id.list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

		List<Item> mListe = new ArrayList<Item>();

		for (int i = 0; i < Emotions.getInstance().getMaximumEmotionType(); i++) {
			mListe.add(new Item(BitmapFactory.decodeResource(
					this.getResources(), Emotions.getInstance().icons[i]),
					Emotions.getInstance().getEmotionText(i), false));
		}

		adapter = new CustomAdapter(this, mListe);
		listView.setAdapter(adapter);

		markerTexts.clear();

		Button button = (Button) dialog.findViewById(R.id.getchoice);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				List<Item> checkedList = adapter.getmList();
				for (int i = 0; i < checkedList.size(); i++) {
					if (checkedList.get(i).isCheckBoxSetting()) {
						String text = checkedList.get(i).getText();
						markerTexts.add(text);
					}
				}

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						List<Marker> markers = mapView.getMarkers();
						if (markers.size() != 0) {
							for (int i = 0; i < mapView.getMarkers().size(); i++) {
								boolean visible = false;
								for (int j = 0; j < markerTexts.size(); j++) {
									if (mapView.getMarkers().get(i).getTitle()
											.equals(markerTexts.get(j))) {
										visible = true;
										break;
									}

								}
								mapView.getMarkers().get(i).setVisible(visible);
							}

						}
					}
				});

				dialog.dismiss();

			}
		});

		dialog.show();
	}

	public void showShareContentMenu() {
		shareIt(EMOTION_ONLY);
	}

	private final static int PERSONAL_INFO = 1;
	private final static int Global_INFO = 2;
	private int mode = PERSONAL_INFO;
	private final Toast loadingData = null;
	Toast serverLoad = null;
	public boolean personal;

	@SuppressLint("UseValueOf")
	private void loadData(int mode) {
		this.mode = mode;
		ImageButton icon = ((ImageButton) findViewById(R.id.imageViewPrivacy));
		switch (this.mode) {
		case PERSONAL_INFO:
			personal = true;
			if (icon != null)
				icon.setImageDrawable(getResources().getDrawable(
						R.drawable.menu_map_private));
			if (checkFilterActive())
				showEmoHistory(setToStartOfDay(currentCalFrom)
						.getTimeInMillis(), setToEndOfDay(currentCalUntil)
						.getTimeInMillis());
			else
				showEmoHistory(0, Calendar.getInstance().getTimeInMillis());
			if (loadingData != null)
				loadingData.cancel();
			break;
		case Global_INFO:
			personal = false;
			GlobalModeTask globalModeTask = new GlobalModeTask();
			globalModeTask.execute();
		

			if (icon != null)
				icon.setImageDrawable(getResources().getDrawable(
						R.drawable.menu_map_global));
			break;
		}

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

	final static int EMOTION_ONLY = 1;
	final static int EMOTION_GEO = 2;

	@SuppressLint("NewApi")
	private void shareIt(int shareType) {
		Uri uriToImage = null;
		Bitmap bitmap;
		if (shareType == EMOTION_GEO) {
			// Making screen shot
			mapFragment = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.mapview);
			mapFragment.getView().getRootView().setDrawingCacheEnabled(true);
			bitmap = Bitmap.createBitmap(mapFragment.getView().getRootView()
					.getDrawingCache());

		} else {
			Drawable d = this.getResources().getDrawable(
					Emotions.getInstance().getEmotionIcon(smileType));
			bitmap = ((BitmapDrawable) d).getBitmap();

		}
		bitmap = overlayPoweredBy(bitmap, shareType);
		String path = Environment.getExternalStorageDirectory().toString();
		OutputStream fOut = null;
		File file = new File(path, File.separator + "EmoMap");
		file.mkdirs();

		file = new File(path, File.separator + "EmoMap" + File.separator
				+ "my_emo.png");
		try {
			fOut = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			fOut.flush();
			fOut.close();
			uriToImage = Uri.fromFile(file);
		} catch (Exception e1) {
			e1.printStackTrace();
			Toast.makeText(this, "Unable to create snapshot!",
					Toast.LENGTH_LONG).show();
			return;
		}

		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		Uri[] uriArry = new Uri[1];
		uriArry[0] = uriToImage;
		shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				"See what I am feeling");
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
				"I am feeling...");
		shareIntent.setType("image/jpeg");
		startActivity(Intent.createChooser(shareIntent,
				getResources().getText(R.string.send_to)));

	}

	final static int MAX_FONT_SIZE = 20;
	final static int Min_FONT_SIZE = 12;

	String poweredBy = "Powered by EmoMap";
	int boundry = 5;

	private Bitmap overlayPoweredBy(Bitmap bmp1, int type) {
		Drawable logo;
		if (type == EMOTION_ONLY)
			logo = this.getResources().getDrawable(R.drawable.logo_small);
		else
			logo = this.getResources().getDrawable(R.drawable.logo_large);

		Bitmap bitmapLogo = ((BitmapDrawable) logo).getBitmap();

		Paint paint = new Paint();

		int imgW = bmp1.getWidth();
		int imgH = bmp1.getHeight() + bitmapLogo.getHeight() + boundry;
		if (bitmapLogo.getWidth() + boundry > bmp1.getWidth() / 2) {
			imgW = (bitmapLogo.getWidth() + boundry) * 2;
		}

		Bitmap bmOverlay = Bitmap.createBitmap(imgW, imgH, bmp1.getConfig());

		paint.setColor(Color.rgb(240, 240, 240));
		paint.setStyle(Style.FILL);
		Canvas canvas = new Canvas(bmOverlay);
		canvas.drawPaint(paint);
		canvas.drawBitmap(bmp1, (imgW - bmp1.getWidth()) / 2, 0, null);
		canvas.drawBitmap(bitmapLogo, imgW - bitmapLogo.getWidth() - boundry,
				imgH - bitmapLogo.getHeight() - boundry, null);
		return bmOverlay;
	}

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ServerManager.MSG_UPDATE_CURRENT_MARKER:
				update((EmoRecord) msg.obj);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void registerItSelf() {
		Message msg = Message.obtain(null, ServerManager.MSG_REG_MAP_VIEWER);
		try {
			msg.replyTo = mMessenger;
			serverMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void unRegisterItSelf() {
		Message msg = Message.obtain(null, ServerManager.MSG_UN_REG_MAP_VIEWER);
		try {
			msg.replyTo = mMessenger;
			serverMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_activity_menu, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private final int numOfSmile = -1;
	private final int numOfSmileCurrectPosition = -1;
	Calendar currentCalFrom = null;
	Calendar currentCalUntil = null;
	Calendar startDateCal = null;
	Calendar endDateCal = null;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_REPORT_ID:
			Dialog dialog1 = new Dialog(this);
			dialog1.setContentView(R.layout.report_dialog);
			TextView text = (TextView) dialog1
					.findViewById(R.id.numSmileReport);
			if (personal) {
				text.setText("" + numOfSmile);
			} else {
				text.setText("" + numberOfEmosOnServer);
			}
			text = (TextView) dialog1.findViewById(R.id.numEmoCorrectP);
			text.setText("" + numOfSmileCurrectPosition);
			dialog1.setTitle("Report");
			return dialog1;
		case DIALOG_TIME_RANGE_ID:
			DateRangeDialog dialog2 = new DateRangeDialog(this);
			dialog2.setTitle(getResources().getString(R.string.timeRangeDialog));
			DBManager dbM = new DBManager(this);
			dbM.open();
			long startDate = Calendar.getInstance().getTimeInMillis();
			if (dbM.isDatabaseOpen()) {
				startDate = dbM.getStartDate();
			}
			dbM.close();

			startDateCal = Calendar.getInstance();
			startDateCal.setTimeInMillis(startDate);
			startDateCal.set(Calendar.HOUR_OF_DAY, 0);
			startDateCal.set(Calendar.MINUTE, 0);
			startDateCal.set(Calendar.MILLISECOND, 0);

			if (currentCalFrom == null)
				currentCalFrom = (Calendar) startDateCal.clone();

			endDateCal = Calendar.getInstance();
			endDateCal.set(Calendar.HOUR_OF_DAY, 0);
			endDateCal.set(Calendar.MINUTE, 0);
			endDateCal.set(Calendar.MILLISECOND, 0);

			if (currentCalUntil == null)
				currentCalUntil = (Calendar) endDateCal.clone();

			dialog2.setRange(startDateCal, endDateCal,
					(Calendar) currentCalFrom.clone(),
					(Calendar) currentCalUntil.clone(), this);
			return dialog2;

		}
		return null;
	}

	private Marker addMarkersToMap(LatLng point, Bitmap bitmap, int emotype) {
		if (mapView != null) {
			Bitmap b = Bitmap.createScaledBitmap(bitmap, 48, 48, false);
			marker = mapView
					.addMarker(new MarkerOptions()
							.title(Emotions.getInstance().getEmotionTextByType(
									emotype)).position(point)
							.icon(BitmapDescriptorFactory.fromBitmap(b)));

			return marker;
		}
		return null;
	}

	private void updateCurrentMarker() {
		if (currentEmorecord == null || !currentEmorecord.isPositionInserted()) {
			return;
		}
		Drawable drawable = this.getResources().getDrawable(
				Emotions.getInstance().getEmotionIcon(
						currentEmorecord.getEmoType()));
		Bitmap bitmapDrawable = ((BitmapDrawable) drawable).getBitmap();
		Marker marker = markerHashMap.get(bitmapDrawable);
		if (marker == null) {
			markerHashMap.put(bitmapDrawable, marker);
		}

		isCurrentSmileAdded = true;
	}

	private void showEmoHistory(long start, long end) {
		if (D)
			Log.d("TEST", "showEmoHistory");
		showProgressBar(true);
		DBManager dbmanager = new DBManager(this);
		dbmanager.open();

		mapView.clear();
		if (dbmanager.isDatabaseOpen()) {
			ArrayList<EmoRecord> emRecordList = dbmanager.getAllRecordBtw(
					start, end);
			for (EmoRecord emr : emRecordList) {

				Drawable drawable = this.getResources().getDrawable(
						Emotions.getInstance().getEmotionIconByType(
								emr.getEmoType()));

				addMarkersToMap(new LatLng(emr.getLat(), emr.getLon()),
						((BitmapDrawable) drawable).getBitmap(),
						emr.getEmoType());
				// Log.d("marker emotype",""+ emr.getEmoType());
			}

			dbmanager.close();
		} else {
		

			showProgressBar(false);
		}
	}

	protected boolean isLocationDisplayed() {
		return true;
	}

	public void update(EmoRecord er) {
		if (locationToast != null)
			locationToast.cancel();
		currentEmorecord = er;
		if (!isItDirectlyStarted) {
			showProgressBar(false);
			updateCurrentMarker();
		}
		// updateBestLocation(l);
		if (D)
			Log.d(TAG,
					"New Location: " + er.getAcc() + " "
							+ sdf.format(new Date(er.getTime())));

	}

	

	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onDestroy() {
		unRegisterItSelf();
		super.onDestroy();
	}

	long progressBarCounter = 0;

	private void showProgressBar(boolean show) {
		progreesBar = (ProgressBar) findViewById(R.id.progressBar1);
		progreesBar.setIndeterminate(show);
		if (show) {
			progressBarCounter++;
			progreesBar.setVisibility(View.VISIBLE);
		} else {
			progressBarCounter--;
			if (progressBarCounter <= 0) {
				progressBarCounter = 0;
				progreesBar.setVisibility(View.INVISIBLE);
			}
		}

	}

	@Override
	public void onRangeChanged(Calendar from, Calendar until) {

		boolean isFilterChanges = true;
		if (isEqualDays(currentCalFrom, from)
				&& isEqualDays(currentCalUntil, until))
			isFilterChanges = false;

		currentCalFrom = (Calendar) from.clone();
		currentCalUntil = (Calendar) until.clone();
		checkFilterActive();
		if (isFilterChanges)
			loadData(mode);
	}

	@SuppressWarnings("deprecation")
	private boolean checkFilterActive() {
		ImageButton timeSetting = ((ImageButton) findViewById(R.id.imageViewTimeSettings));
		if (isEqualDays(currentCalFrom, startDateCal)
				&& isEqualDays(currentCalUntil, endDateCal)) {
			timeSetting.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.button_selection_background));
			return false;
		} else {
			timeSetting.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.filter_active));
			return true;
		}
	}

	public boolean isEqualDays(Calendar c1, Calendar c2) {
		if (c1 == null && c2 == null)
			return true;
		if (c1 == null || c2 == null)
			return false;
		if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
				&& c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
				&& c1.get(Calendar.DAY_OF_MONTH) == c2
						.get(Calendar.DAY_OF_MONTH))
			return true;
		return false;
	}

	public Calendar setToEndOfDay(Calendar c) {
		Calendar c1 = (Calendar) c.clone();
		c1.set(Calendar.HOUR_OF_DAY, 24);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);
		return c1;
	}

	public Calendar setToStartOfDay(Calendar c) {
		Calendar c1 = (Calendar) c.clone();
		c1.set(Calendar.HOUR_OF_DAY, 0);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);
		return c1;
	}

	@Override
	public void onLocationChanged(Location location) {
		double latitude = location.getLatitude();
		// Getting longitude of the current location
		double longitude = location.getLongitude();
		// Creating a LatLng object for the current location
		LatLng latLng = new LatLng(latitude, longitude);
		// Showing the current location in Google Map
		mapView.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		// Zoom in the Google Map
		mapView.animateCamera(CameraUpdateFactory.zoomTo(15f));
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

	private static class MutableData {

		private int value;

		private LatLng position;

		public MutableData(int value, LatLng position) {
			this.value = value;
			this.setPosition(position);
		}

		@SuppressWarnings("unused")
		public LatLng getPosition() {
			return position;
		}

		public void setPosition(LatLng position) {
			this.position = position;
		}
	}

	public List<EmoRecord> getServerEmos() {
		return serverEmos;
	}

	public void showProgressDialog() {
		pdialog = new ProgressDialog(this);
		pdialog.setCancelable(true);
		pdialog.setMessage("Loading ...");
		pdialog.show();
	}

	private class GlobalModeTask extends AsyncTask<Void, Integer, Integer> {

		ProgressDialog bladialog;

		@Override
		protected void onPreExecute() {

			bladialog = new ProgressDialog(CopyOfMapViewActivity.this);
			bladialog.setCancelable(true);
			bladialog.setMessage("Loading ...");
			bladialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// bladialog.setMax(serverEmos.size());
			bladialog.show();

			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Void... params) {


			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// bladialog.incrementProgressBy(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer result) {

			MarkerTask ta = new MarkerTask();
			ta.execute();
			bladialog.dismiss();
			super.onPostExecute(result);
		}

	}

	private class MarkerTask extends AsyncTask<Void, Integer, Integer> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Void... params) {
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// bladialog.incrementProgressBy(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer result) {
			final Resources res = CopyOfMapViewActivity.this.getResources();

			mapView.clear();
			List<EmoRecord> emos = serverEmos;

			if (checkFilterActive()) {
				for (EmoRecord emr : emos) {

					Long currentStart = new Long(
							setToStartOfDay(currentCalFrom).getTimeInMillis());
					Long currentEnd = new Long(setToEndOfDay(currentCalUntil)
							.getTimeInMillis());
					Long emoTime = new Long(emr.getTime());

					if (currentStart.compareTo(emoTime) <= 0
							&& currentEnd.compareTo(emoTime) >= 0) {
						Drawable drawable = res.getDrawable(Emotions
								.getInstance().getEmotionIconByType(
										emr.getEmoType()));

						addMarkersToMap(new LatLng(emr.getLat(), emr.getLon()),
								((BitmapDrawable) drawable).getBitmap(),
								emr.getEmoType());
					}
				}
			} else {

				for (EmoRecord emr : emos) {
					Drawable drawable = res.getDrawable(Emotions.getInstance()
							.getEmotionIconByType(emr.getEmoType()));

					addMarkersToMap(new LatLng(emr.getLat(), emr.getLon()),
							((BitmapDrawable) drawable).getBitmap(),
							emr.getEmoType());
				}
			}

		}

	}



	public static JSONObject getLocationInfo(String address) {

		HttpGet httpGet = new HttpGet(
				"http://maps.google.com/maps/api/geocode/json?address="
						+ address + "&sensor=false");
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jsonObject;
	}

	public static LatLng getGeoPoint(JSONObject jsonObject) {

		Double lon = new Double(0);
		Double lat = new Double(0);

		try {

			lon = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
					.getJSONObject("geometry").getJSONObject("location")
					.getDouble("lng");

			lat = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
					.getJSONObject("geometry").getJSONObject("location")
					.getDouble("lat");

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new LatLng((int) (lat * 1E6), (int) (lon * 1E6));

	}

	private class AutoCompleteAdapter extends ArrayAdapter<Address> implements
			Filterable {

		private final LayoutInflater mInflater;
		private final Geocoder mGeocoder;
		private final StringBuilder mSb = new StringBuilder();

		public AutoCompleteAdapter(final Context context) {
			super(context, -1);
			mInflater = LayoutInflater.from(context);
			mGeocoder = new Geocoder(context);
		}

		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			final TextView tv;
			if (convertView != null) {
				tv = (TextView) convertView;
			} else {
				tv = (TextView) mInflater.inflate(
						android.R.layout.simple_dropdown_item_1line, parent,
						false);
				tv.setTextSize(14f);
			}

			tv.setText(createFormattedAddressFromAddress(getItem(position)));

			editTextAddress
					.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						//
						@Override
						public boolean onEditorAction(TextView v, int actionId,
								KeyEvent event) {
							//
							if (actionId == EditorInfo.IME_ACTION_SEARCH
									|| actionId == EditorInfo.IME_ACTION_DONE
									|| actionId == EditorInfo.IME_ACTION_GO
									|| event.getAction() == KeyEvent.ACTION_DOWN
									&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

								InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
								imm.hideSoftInputFromWindow(
										editTextAddress.getWindowToken(), 0);

								Address finalAddress = getItem(position);
								if (finalAddress != null) {
									mapView.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(
											finalAddress.getLatitude(),
											finalAddress.getLongitude())));
								}

								return true;
							}

							return false;
						}

					});

			return tv;
		}

		private String createFormattedAddressFromAddress(final Address address) {
			mSb.setLength(0);
			final int addressLineSize = address.getMaxAddressLineIndex();
			for (int i = 0; i < addressLineSize; i++) {
				mSb.append(address.getAddressLine(i));
				if (i != addressLineSize - 1) {
					mSb.append(", ");
				}
			}
			return mSb.toString();
		}

		@Override
		public android.widget.Filter getFilter() {
			Filter myFilter = new Filter() {

				@Override
				protected FilterResults performFiltering(
						final CharSequence constraint) {
					List<Address> addressList = null;
					if (constraint != null) {
						try {
							addressList = mGeocoder.getFromLocationName(
									(String) constraint, 5);
						} catch (IOException e) {
						}
					}
					if (addressList == null) {
						addressList = new ArrayList<Address>();
					}

					final FilterResults filterResults = new FilterResults();
					filterResults.values = addressList;
					filterResults.count = addressList.size();

					return filterResults;
				}

				@Override
				@SuppressWarnings("unchecked")
				protected void publishResults(final CharSequence contraint,
						final FilterResults results) {
					clear();
					for (Address address : (List<Address>) results.values) {
						add(address);
					}
					if (results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}

				@Override
				public CharSequence convertResultToString(
						final Object resultValue) {
					return resultValue == null ? "" : ((Address) resultValue)
							.getAddressLine(0);
				}
			};
			return myFilter;
		}
	}

}
