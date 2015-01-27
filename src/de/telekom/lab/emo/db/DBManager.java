package de.telekom.lab.emo.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import de.telekom.lab.emo.Emotions;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.util.Log;

public class DBManager {
	// Debugging
    private static final String TAG = "DBManager";
    private static final boolean D = true;    
    
    private DatabaseOpenHelper mOpenHelper;
    private static String userIDENTIFIER=null;;
    
    private SQLiteDatabase database;
    private String[] allColumns = { 
    		EmoRecord.ID,
    		EmoRecord.EMO_TYPE,
    		EmoRecord.TIME,
    		EmoRecord.Geo_LAT,
    		EmoRecord.Geo_LON,
    		EmoRecord.GEO_ALT,
    		EmoRecord.GEO_ACC,
    		EmoRecord.STATE
		};
    
    public DBManager(Context context) {
    	mOpenHelper = new DatabaseOpenHelper(context);
	}

	public void open() throws SQLException {
		database = mOpenHelper.getWritableDatabase();
	}

	public void close() {
		if (database!=null){
			database.close();
			database=null;
		}
		if (mOpenHelper!=null){
			mOpenHelper.close();
		}
	}
	
	public boolean isDatabaseOpen(){
		if (database!=null && database.isOpen())
			return true;
		return false;
	}
	
	public EmoRecord insertEmoLocation(EmoRecord emo) {	
		
		long insertId = database.insert(DatabaseOpenHelper.DATABASE_NAME, null,
				emo.getContentvalues());
		if (D) Log.d(TAG,"insertEmoLocation : ID "+insertId+ " type:"+emo.getEmoType());
		Cursor cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,
				allColumns,EmoRecord.ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		EmoRecord newEmoRec = cursorToEmoRec(cursor);
		newEmoRec.setPublished(emo.isPublished());
		newEmoRec.setState(EmoRecord.FULL_INITIALIZED);
		cursor.close();
		return newEmoRec;
	}
	
	public void deleteAll(){
		if (D) Log.d(TAG,"deleteAll");
		database.delete(DatabaseOpenHelper.DATABASE_NAME, null, null);
	}
	
	public ArrayList<EmoRecord> getAllRecord(){
		if (D) Log.d(TAG,"getAllRecord");
		ArrayList<EmoRecord> recs = new ArrayList<EmoRecord>();

		Cursor cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,
				allColumns, null, null, null, null, null);

		cursor.moveToFirst();
		EmoRecord emrc;
		while (!cursor.isAfterLast()) {
			emrc = cursorToEmoRec(cursor);
			if (D) Log.d(TAG, "Loading: "+emrc.toString());
			recs.add(emrc);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return recs;
	}
	
	public ArrayList<EmoRecord> getAllRecordBtw(long start, long end){
		if (D) Log.d(TAG,"getAllRecordBtw");
		ArrayList<EmoRecord> recs = new ArrayList<EmoRecord>();

		Cursor cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,
				allColumns,EmoRecord.TIME+">"+start+" and "+EmoRecord.TIME+"<"+end , null, null, null, null);

		cursor.moveToFirst();
		EmoRecord emrc;
		while (!cursor.isAfterLast()) {
			emrc = cursorToEmoRec(cursor);
			recs.add(emrc);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return recs;
	}
	
	public boolean updateGeo(EmoRecord emo){
		if (D) Log.d(TAG,"updateGeo : ID "+emo.getId());
		long num = database.update(DatabaseOpenHelper.DATABASE_NAME, emo.getGeoUpdateContentvalues(), EmoRecord.ID+"="+emo.getId(),null);
		if (num>0)
			return true;
		return false;
	}
	
	public long getStartDate(){
		if (D) Log.d(TAG,"getStartDate");
		Cursor cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,new String[] { "min(" + EmoRecord.TIME + ")" }, null, null,
                null, null, null); 

		cursor.moveToFirst();
		long  r= cursor.getLong(0);
		cursor.close();
		return r;
	}
	
	public int getLatestEmotionType(){
		int emotype=Emotions.EMOTIO_HAPPY;
		Cursor cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,new String[] { "max(" + EmoRecord.TIME + ")" }, null, null,
                null, null, null); 

		cursor.moveToFirst();
		long  r= cursor.getLong(0);
		cursor.close();
		
		cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,
				new String[] {EmoRecord.EMO_TYPE},EmoRecord.TIME+">="+r , null, null, null, null);
		cursor.moveToFirst();
		emotype=cursor.getInt(0);

		// Make sure to close the cursor
		cursor.close();
		return emotype;
	}
	
	private EmoRecord cursorToEmoRec(Cursor cursor) {
		EmoRecord newRec = new EmoRecord();
		newRec.setId(cursor.getInt(0));
		newRec.set(cursor.getInt(1), cursor.getLong(2), cursor.getDouble(3), cursor.getDouble(4),cursor.getDouble(5), cursor.getInt(6),cursor.getInt(7));
		return newRec;
	}
	
	private POI cursorToPOI(Cursor cursor) {
		POI newPOI = new POI();
		newPOI.setID(cursor.getInt(0));
		newPOI.type=cursor.getInt(1);
		newPOI.set(cursor.getDouble(3), cursor.getDouble(4));
		return newPOI;
	}
	
	public ArrayList<POI> getAllEmoArround(Location l, int max, int dis){
		double radius= 0.001*dis/100;		
		if (D) Log.d(TAG,"getAllEmoArround");
		ArrayList<POI> recs = new ArrayList<POI>();

		Cursor cursor = database.query(DatabaseOpenHelper.DATABASE_NAME,
				allColumns,EmoRecord.Geo_LAT+">"+(l.getLatitude()-radius)+" and "+EmoRecord.Geo_LAT+"<"+(l.getLatitude()+radius)+ " and "+
						EmoRecord.Geo_LON+">"+(l.getLongitude()-radius)+" and "+EmoRecord.Geo_LON+"<"+(l.getLongitude()+radius)
				, null, null, null, null);

		cursor.moveToFirst();
		POI poi;
		while (!cursor.isAfterLast()) {
			poi = cursorToPOI(cursor);
			recs.add(poi);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return recs;
	}
	
	public String getUserIdentifier(){
		try{
			if (userIDENTIFIER==null){
				loadUserIdentifier();
				if (userIDENTIFIER==null){
					// create the identifier
					userIDENTIFIER =md5(getDeviceProperties());
					saveUserIdentifier();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		Log.d("BUILD","ID:" +userIDENTIFIER);
		return userIDENTIFIER;
	}
	
	private String getDeviceProperties(){
		StringBuffer buf = new StringBuffer();
    	buf.append(Build.FINGERPRINT);
    	buf.append(System.currentTimeMillis());
    	buf.append(Math.random());
    	Log.d("BUILD", buf.toString());
    	return buf.toString();
	}
	
	private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            
            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
	
	private void loadUserIdentifier(){
		Cursor cursor = database.query(DatabaseOpenHelper.TABLE_USER_NAME,new String[] { Queries.USER_C_RANDOM}, null, null,
                null, null, null); 
		if (cursor.moveToLast())
			userIDENTIFIER= cursor.getString(0);
		cursor.close();
	
	}
	
	private void saveUserIdentifier(){
		ContentValues values = new ContentValues();
		values.put(Queries.USER_C_RANDOM, userIDENTIFIER);
	
		long insertId = database.insert(DatabaseOpenHelper.TABLE_USER_NAME, null,
				values);
	}
}
