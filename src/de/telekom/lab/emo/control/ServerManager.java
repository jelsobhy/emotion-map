package de.telekom.lab.emo.control;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import de.telekom.lab.emo.BoundingBox;
import de.telekom.lab.emo.db.DBManager;
import de.telekom.lab.emo.db.EmoRecord;
import de.telekom.lab.emo.webservices.MyWSL;
import de.telekom.lab.emo.webservices.WebService;


@SuppressLint({ "HandlerLeak", "ShowToast" })
public class ServerManager extends Service {
	// Debugging
    private static final String TAG = "ServerManager";
    private static final boolean D = true;
    
	public static final int MSG_SMILE_TYPE=1;
	public static final int MSG_REQ_LAYER=2;
	public static final int MSG_POSITION=3;
	public static final int MSG_PUBLISH=4;
	public static final int MSG_TEST_MESSAGING=5;
	public static final int MSG_RESET=6;
	public static final int MSG_REG_MAP_VIEWER=7;
	public static final int MSG_UN_REG_MAP_VIEWER=8;
	public static final int MSG_UPDATE_CURRENT_MARKER=9;
	public static final int MSG_REG_AR_ACTIVITY=10;
	public static final int MSG_UN_REG_AR_ACTIVITY=12;
	public static final int MSG_UPDATE_CURRENT_POSITION=11;
	public static final int MSG_REQ_ALL_DATA =13;
	public static final int MSG_RS = 0;
	public static final int MSG_SYNCHRONIZE = 14;
	
	public static final String KEY_BOUNDING_BOX="BOUNDING_BOX";
	public static final int NEEDED_ACCURACY=50;
	
	Messenger mClient=null; 
	Messenger mClientAR=null; 
	Location bestLocation=null;
	Location newLocation=null;

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	String userIDENTIFIER=null;
	List<EmoRecord> allData = new ArrayList<EmoRecord>();
	List<EmoRecord> allEmosOnUserDatabase = new ArrayList<EmoRecord>();
	
	EmoRecord emoRecord=new EmoRecord();
	class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SMILE_TYPE:
                	if (D) Log.d(TAG, "MSG_SMILE_TYPE type:"+msg.arg1);
                	synchronized (emoRecord){
	                	if (emoRecord!=null &&!emoRecord.isPublished())
	                		publish(true);
	                	EmoRecord newemoRecord=new EmoRecord(msg.arg1,Calendar.getInstance().getTimeInMillis());
	                	if (emoRecord!=null && emoRecord.isPositionInserted())
	                		newemoRecord.setGeo(emoRecord.getLat(), emoRecord.getLon(),emoRecord.getAlt(), emoRecord.getAcc());
	                	emoRecord=newemoRecord;
                	}
                	smileTypeArrived();
                    break;
                case MSG_REQ_LAYER:
                	if (D) Log.d(TAG, "MSG_REQ_LAYER");
                	if (mClient==null)
                		mClient=msg.replyTo;	
                	layerIsRequested((BoundingBox) msg.getData().getParcelable(KEY_BOUNDING_BOX));

                    break;
                case MSG_REG_MAP_VIEWER:
                	if (D) Log.d(TAG, "MSG_REG_MAP_VIEWER");
                	if (mClient==null)
                		mClient=msg.replyTo;	
                	if (emoRecord!=null && emoRecord.isTypeInserted()){
                		if (mClient!=null){
            				Message msg2 = Message.obtain(null,
            		                ServerManager.MSG_UPDATE_CURRENT_MARKER);
            				 msg2.obj=emoRecord;
            			      try {
            					mClient.send(msg2);
            				} catch (RemoteException e) {
            					e.printStackTrace();
            				}
            			}
                	}
                	
                	break;
                case MSG_REG_AR_ACTIVITY:
                	if (D) Log.d(TAG, "MSG_REG_AR_ACTIVITY");
                	if (mClientAR==null)
                		mClientAR=msg.replyTo;	
                	if (emoRecord!=null && emoRecord.isPositionInserted()){
                		if (mClientAR!=null){
            				Message msg2 = Message.obtain(null,
            		                ServerManager.MSG_UPDATE_CURRENT_POSITION);
            				 msg2.obj=bestLocation;
            			      try {
            			    	  mClientAR.send(msg2);
            				} catch (RemoteException e) {
            					e.printStackTrace();
            				}
            			}
                	}
                	
                	break;
                case MSG_UN_REG_MAP_VIEWER:
                	if (D) Log.d(TAG, "MSG_UN_REG_MAP_VIEWER");
                	mClient=null;
                	break;
                case MSG_UN_REG_AR_ACTIVITY:
                	if (D) Log.d(TAG, "MSG_UN_REG_AR_ACTIVITY");
                	mClientAR=null;
                	break;	
                case MSG_POSITION:
                	if (D) Log.d(TAG, "MSG_POSITION");
                	newLocation=(Location)msg.obj;
                	
                	positionIsArrived();
                	break;
                case MSG_PUBLISH:
                	if (D) Log.d(TAG, "MSG_PUBLISH");
                	if (!emoRecord.isPublished()){
                		publish(true);
                	}
                	stopTheService();
                	break;
                case MSG_TEST_MESSAGING:
                	if (D) Log.d(TAG, "MSG_TEST_MESSAGING");
                	testMessage();
                	break;
                case MSG_RESET:
                	emoRecord=new EmoRecord();
                	break;
                case MSG_REQ_ALL_DATA:
//                	getAllEmoRecords();
                	break;
                case MSG_SYNCHRONIZE:
                	List<EmoRecord> records = getAllUserEmos();
                	for (EmoRecord emoRecord : records) {
						if(!emoRecord.isPublished()){
							ServerManager.this.emoRecord = emoRecord;
							publishToServer();
						}
					}
                	
                	break;
                default:
                    super.handleMessage(msg);
            }
        }


    }

	@Override
	public IBinder onBind(Intent arg0) {
		 return mMessenger.getBinder();
	}
	
	public List<EmoRecord> getAllUserEmos() {
		DBManager dbmanager= new DBManager(this);
		dbmanager.open();
		if (dbmanager.isDatabaseOpen()){
			allEmosOnUserDatabase = dbmanager.getAllRecord();
			dbmanager.close();
		}
		return allEmosOnUserDatabase;
	}
	
	public void smileTypeArrived(){
		emoRecord.setState(EmoRecord.TYPE_INITIALIZED);
		if (emoRecord.getState()==EmoRecord.FULL_INITIALIZED)
			publish(false);
	}
	
	public void positionIsArrived(){
//		Toast.makeText(this, "POS arrived, ACC "+bestLocation.getAccuracy(), 500).show();
		if (newLocation!=null && bestLocation!=null)
			Log.d(TAG, newLocation.getAccuracy() +", old "+bestLocation.getAccuracy());
		if(LocationUtility.getInstance().isBetterLocation(newLocation, bestLocation) ){
			bestLocation=newLocation;
			emoRecord.setLat(bestLocation.getLatitude());
			emoRecord.setLon(bestLocation.getLongitude());
			emoRecord.setAcc((int)bestLocation.getAccuracy());
			emoRecord.setState(EmoRecord.POS_INITIALIZED);
			
			

			if (mClient!=null){
				Message msg = Message.obtain(null,
		                ServerManager.MSG_UPDATE_CURRENT_MARKER);
				 msg.obj=emoRecord;
			      try {
					mClient.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
			if (mClientAR!=null){
				Message msg = Message.obtain(null,
		                ServerManager.MSG_UPDATE_CURRENT_POSITION);
				 msg.obj=bestLocation ;
			      try {
			    	  mClientAR.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	
		if (isPositionAccurateEnough() && !emoRecord.isPublished())
			publish(false);
	}
	
	private boolean isPositionAccurateEnough(){
		if (bestLocation==null || bestLocation.getAccuracy()>NEEDED_ACCURACY)
			return false;
		return true;
	}
	
	public void publish(boolean withForce){
		if (emoRecord.getState()!=EmoRecord.FULL_INITIALIZED ) return;
		if (emoRecord.isStored()){
			// update!
			DBManager dbmanager= new DBManager(this);
			dbmanager.open();
			if (dbmanager.isDatabaseOpen()){
				dbmanager.updateGeo(emoRecord);
				dbmanager.close();
			}
		}else{
			// store it
			
			emoRecord.setStored(true);
			DBManager dbmanager= new DBManager(this);
			dbmanager.open();
			if (this.userIDENTIFIER==null)
				this.userIDENTIFIER=dbmanager.getUserIdentifier();
			if (dbmanager.isDatabaseOpen()){
				emoRecord=dbmanager.insertEmoLocation(emoRecord);
				dbmanager.close();
			}
		}
		if (isPositionAccurateEnough()||withForce){
			if (D)Log.d(TAG, "PUBLISH NOW");
			Toast.makeText(ServerManager.this, "PUBLISH NOW", Toast.LENGTH_LONG);
			publishToServer();
			emoRecord.setPublished(true);
		}
		
			
	}
	final Handler handler = new Handler() 
    { 
         public void handleMessage(Message msg) 
         { 
             String newRS =(String)msg.getData().get("RS");
  		
//             Toast.makeText(ServerManager.this, newRS, Toast.LENGTH_LONG);
         }

		
    };
   
    
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
             if(items.getLength() > 0){
             for (int i=0;i<items.getLength();i++){
                 Node item = items.item(i);
                 double lat = Double.parseDouble(item.getAttributes().item(0).getNodeValue());
                 double lon = Double.parseDouble(item.getAttributes().item(1).getNodeValue());
                 int smileType = Integer.parseInt(item.getAttributes().item(2).getNodeValue());
                 EmoRecord e = new EmoRecord();
                 e.setEmoType(smileType);
                 e.setLat(lat);
                 e.setLon(lon);
                 em.add(e);
             }
            }
         }catch(Exception e){
        	 
         }
         return em;
	} 
	private void publishToServer(){
		
		final WebService ws = new WebService();
        ws.addCallBackListener(new MyWSL(handler));

		Thread	 t = new Thread( new Runnable() {	
			public void run() {
				// TODO Auto-generated method stub
 				String p=ws.make_REQUEST_INSERT(emoRecord,userIDENTIFIER);
 				ws.execute(p);
 			}			
		});
		t.start();
		emoRecord.setPublished(true);
	}
	
	public void synchronizeWithServer(){
		
		final WebService ws = new WebService();
        ws.addCallBackListener(new MyWSL(handler));

		Thread	 t = new Thread( new Runnable() {	
			public void run() {
				// TODO Auto-generated method stub
 				String p=ws.make_REQUEST_SYNCHRONIZE(allEmosOnUserDatabase,userIDENTIFIER);
 				ws.execute(p);
 			}			
		});
		t.start();
//		emoRecord.setPublished(true);
	}
	
	public void getAllEmoRecords(Handler handler){
		final WebService ws = new WebService();
		ws.addCallBackListener(new MyWSL(handler));
		
		Thread t = new Thread(new Runnable() {
			
			public void run() {
				String p = ws.make_REQUEST_GET_ALL();
				System.out.println(p);
				ws.execute(p);
				
			}
		});
		t.start();
	}

	public void layerIsRequested(BoundingBox bbx){
		Toast.makeText(this, "layerIsRequested", 2000).show();
	}

	@Override
    public void onDestroy() {
        Toast.makeText(this, "ServiceManager_onDestroy", Toast.LENGTH_SHORT).show();
    }
	
	public void testMessage(){
		Toast.makeText(this, "TEST MESSAGE", Toast.LENGTH_SHORT).show();
	}
	
	private void stopTheService(){
		Toast.makeText(this, "Stop ServerManager", 2000).show();
		stopSelf();
	}

}
