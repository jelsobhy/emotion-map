package de.telekom.lab.emo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	
	// Debugging
    private static final String TAG = "DatabaseOpenHelper";
    private static final boolean D = true;
	
	public static final String DATABASE_NAME = "EMO_DB";
	public static final String TABLE_USER_NAME = "EMO_USER_DB";
	private static final int DATABASE_VERSION = 5;

	public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Queries.CREATE_DB_EMO_TABLE);
		createUserTable(db);
	}
	
	private void createUserTable(SQLiteDatabase db){
		db.execSQL(Queries.CREATE_DB_USER_TABLE);	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (D)
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
		
		if (oldVersion==4 && newVersion==5){
			createUserTable(db);
		}else{
	        db.execSQL("DROP TABLE IF EXISTS "+DATABASE_NAME );
	        onCreate(db);
		}
	}
}
