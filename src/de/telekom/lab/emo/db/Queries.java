package de.telekom.lab.emo.db;

public class Queries {
	public final static String CREATE_DB_EMO_TABLE= "CREATE TABLE "+DatabaseOpenHelper.DATABASE_NAME+" (" +
			EmoRecord.ID+" INTEGER PRIMARY KEY autoincrement," +
			EmoRecord.EMO_TYPE+" INTEGER not null," +
			EmoRecord.TIME+" INTEGER not null," +
			EmoRecord.Geo_LAT+" REAL," +
			EmoRecord.Geo_LON+" REAL," +
			EmoRecord.GEO_ALT+" REAL," +
			EmoRecord.GEO_ACC+" INTEGER," +
			EmoRecord.STATE+" INTEGER );";
	
	public final static String USER_C_ID="ID";
	public final static String USER_C_RANDOM="RAND";
	public final static String CREATE_DB_USER_TABLE= "CREATE TABLE "+DatabaseOpenHelper.TABLE_USER_NAME+" (" +
			USER_C_ID+" INTEGER PRIMARY KEY autoincrement," +
			USER_C_RANDOM+" TEXT not null);";
	
            
}
