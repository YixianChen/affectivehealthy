/**
 * 
 */
package se.sics.ah3.database;

import java.util.Vector;

import se.sics.ah3.database.Column.TimeData;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class SignalCacheDatabaseTable extends SQLiteOpenHelper {
	
	private Context mContext;
	
	private String mTableName;
	private long mQuantsize;
	
	public SignalCacheDatabaseTable(Context context, long quantsize, String tableName, CursorFactory factory,
			int version) {
		super(context, "AH3DB", factory, version);
		
		mTableName = tableName;
		mQuantsize = quantsize;
		mContext = context;
		SQLiteDatabase db = getWritableDatabase();
		onCreate(db);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + mTableName + " ( TIME INTEGER PRIMARY KEY, VALUE REAL )");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub		
	}
	
	public boolean isCached(long start, long end) {
		Cursor cursor = getCursor(start, end);
		long expectedCount = (end - start) / mQuantsize;
		// Log.d("database", "cache count " + cursor.getCount() + " expected " + expectedCount);
		if(cursor.getCount() == expectedCount) {
			return true;
		}
		return false;
	}
	
	public Vector<TimeData> get(long start, long end) {
		
		Vector<TimeData> output = new Vector<TimeData>();
		Cursor cursor = getCursor(start, end);
		while(cursor.moveToNext()) {
			TimeData  td = new TimeData();
			td.time = cursor.getLong(0);
			td.value = cursor.getFloat(1);
			output.add(td);
		}
		return output;
	}
	
	private Cursor getCursor(long start, long end) {		
		return getReadableDatabase().query(mTableName, new String[]{"TIME", "VALUE"}, "TIME >= ? AND TIME < ?", new String[]{""+start, ""+end}, null, null, "TIME DESC");
	}

	public void store(Vector<TimeData> output) {
		SQLiteDatabase db = this.getWritableDatabase();
		// check if this output already exists
		for(int i = 0; i < output.size(); i++) {
			db.execSQL("insert or replace into " + mTableName + "(TIME, VALUE) values ( " + output.get(i).time + ", " + output.get(i).value + " )");
		}
	}
}