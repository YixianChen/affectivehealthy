/**
 * 
 */
package se.sics.ah3.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class AccDatabaseTable extends SQLiteOpenHelper {
		
	private String mTableName;

	public AccDatabaseTable(Context context, CursorFactory factory,	int version) {
		super(context, "AH3DB", factory, version);
		
		mTableName = "AccRawData";
		SQLiteDatabase db = getWritableDatabase();
		onCreate(db);
		
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + mTableName + " ( TIME INTEGER PRIMARY KEY, X REAL, Y REAL, Z REAL )");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub		
	}
	
	public void store(long time, float x, float y, float z) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("insert or replace into " + mTableName + "(TIME, X, Y, Z) values ( " + time + ", " + x + ", " + y + ", " + z + " )");
	}
}