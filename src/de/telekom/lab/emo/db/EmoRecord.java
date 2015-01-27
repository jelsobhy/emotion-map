package de.telekom.lab.emo.db;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;


public class EmoRecord  {

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "TIME DESC";

    /**
     * id 
     * <P>Type: INTEGER</P>
     */
    public static final String ID = "ID";
    
    /**
     * EMO_TYPE type of emotion
     * <P>Type: INTEGER</P>
     */
    public static final String EMO_TYPE = "EMO_TYPE";

    /**
     * TIME
     * <P>Type: INTEGER(long from System.curentTimeMillis())</P>
     */
    public static final String TIME = "TIME";

    /**
     * Latetute
     * <P>Type: REAL (long from System.curentTimeMillis())</P>
     */
    public static final String Geo_LAT = "Geo_LAT";

    /**
     * Longetiute
     * <P>Type: REAL (long from System.curentTimeMillis())</P>
     */
    public static final String Geo_LON = "Geo_LON";
    
    /**
     * Accuracy of position
     * <P>Type: INTEGER(long from System.curentTimeMillis())</P>
     */
    public static final String GEO_ACC = "GEO_ACC";
    
    /**
     * Altitute of position
     * <P>Type: REAL</P>
     */
    public static final String GEO_ALT = "GEO_ALT";
    
    /**
     * Accuracy of position
     * <P>Type: INTEGER(long from System.curentTimeMillis())</P>
     */
    public static final String STATE = "STATE";
    
    private int id;
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private int emoType;
    private long time;
    private double lat;
    private double lon;
    private double alt;
    private int acc;
    private int state;
//    boolean isPositionSet=false;
    
    boolean isPublished =false;
    boolean isStored=false;
    
    public final static int UNKNOWN_ID=-1;
    
    public EmoRecord(){
    	this.id=UNKNOWN_ID;
    	this.isPublished =false;
    	this.isStored=false;
    	set(0,0,0,0,0,0,NOT_INITIALIZED);
    }
    
    public EmoRecord(int emoType,long time){
    	this.id=UNKNOWN_ID;
    	this.isPublished =false;
    	this.isStored=false;
    	set(emoType,time,0,0,0,0,0);
//    	this.isPositionSet=false;
    	setState(TYPE_INITIALIZED);
    }
    
    public boolean isPublished() {
		return isPublished;
	}
    
	public boolean isStored() {
		return isStored;
	}

	public void setStored(boolean isStored) {
		this.isStored = isStored;
	}

	public void setPublished(boolean isPublished) {
		this.isPublished = isPublished;
	}

	public void set(int emoType,long time, double lat, double lon,double alt, int acc,int state){
    	
    	this.emoType=emoType;
    	this.time=time;
    	this.lat=lat;
    	this.lon=lon;
    	this.alt=alt;
    	this.acc=acc;
    	this.state =state;
    }
    
    public void setGeo(double lat, double lon, double alt,int acc){
    	this.lat=lat;
    	this.lon=lon;
    	this.acc=acc;
    	this.alt=alt;
//    	isPositionSet=true;
    	setState(POS_INITIALIZED);
    }

//    public boolean isPositionSet() {
//		return isPositionSet;
//	}

//	public void setPositionSet(boolean isPositionSet) {
////		this.isPositionSet = isPositionSet;
//		setState(POS_INITIALIZED);
//	}

	public ContentValues getContentvalues(){
    	ContentValues values = new ContentValues();
    	
    	values.put(EMO_TYPE, new Integer(this.emoType));
    	values.put(TIME, new Long(this.time));
    	values.put(Geo_LAT, new Double(this.lat));
    	values.put(Geo_LON, new Double(this.lon));
    	values.put(GEO_ALT, new Double(this.alt));
    	values.put(GEO_ACC, new Integer(this.acc));
    	values.put(STATE, new Integer(this.state));
    	
    	return values;
    }
	
	public ContentValues getGeoUpdateContentvalues(){
    	ContentValues values = new ContentValues();
    	values.put(Geo_LAT, new Double(this.lat));
    	values.put(Geo_LON, new Double(this.lon));
    	values.put(GEO_ALT, new Double(this.alt));
    	values.put(GEO_ACC, new Integer(this.acc));
    	return values;
    }
	
	public int getState() {
		return state;
	}

	public void setState(int state) {
		switch(this.state){
		case NOT_INITIALIZED:
			this.state = state;
			break;
		case POS_INITIALIZED:
			if (state==TYPE_INITIALIZED)
				this.state=FULL_INITIALIZED;
			else
				this.state = state;
			break;
		case TYPE_INITIALIZED:
			if (state==POS_INITIALIZED)
				this.state=FULL_INITIALIZED;
			else
				this.state = state;
			break;
		case FULL_INITIALIZED:
			if (state==NOT_INITIALIZED)
				this.state=NOT_INITIALIZED;
			break;
		}
		
	}
	
	public boolean isPositionInserted(){
		if (this.state==POS_INITIALIZED || this.state==FULL_INITIALIZED )
			return true;
		return false;
	}
	
	public boolean isTypeInserted(){
		if (this.state==TYPE_INITIALIZED || this.state==FULL_INITIALIZED )
			return true;
		return false;
	}
	
	public int getEmoType() {
		return emoType;
	}

	public void setEmoType(int emoType) {
		this.emoType = emoType;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public int getAcc() {
		return acc;
	}

	public void setAcc(int acc) {
		this.acc = acc;
	}
	
	public String toString(){
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z" 	);
		return (String.format("Type: %1d, Lat: %2f Lon: %3f Alt: %4f , Acc:%5d ",getEmoType(),getLat(), getLon(),getAlt(),getAcc())+sdf.format(new Date(getTime())));
	}
	
//	public POI getPOI(){
//		POI p=new POI();
//		p.lat=(int)(this.lat*1000000);
//		p.lon=(int)(this.lon*1000000);
//		
//	}
	
	public double getAlt() {
		return alt;
	}

	public void setAlt(double alt) {
		this.alt = alt;
	}

	public static final int NOT_INITIALIZED=0;
	public static final int POS_INITIALIZED=1;
	public static final int TYPE_INITIALIZED=2;
	public static final int FULL_INITIALIZED=3;
}
