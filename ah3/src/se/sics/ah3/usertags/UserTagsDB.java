package se.sics.ah3.usertags;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserTagsDB extends SQLiteOpenHelper {
	private static final String DBNAME = "AH3Energy";
	
	private static final String TABLENAME = "usertags";

	public UserTagsDB(Context context)
	{
		super(context, DBNAME, null, 10);
	}
	
	public void create()
	{
		onCreate(getReadableDatabase());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create tables

		String cmd = "CREATE TABLE IF NOT EXISTS "
				+ TABLENAME + " ("
				+ "ID INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ "TIME INTEGER UNIQUE, "
				+ "tag TEXT" +");";
			db.execSQL(cmd);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLENAME);
	}

	public Cursor get(long from, long to)
	{
		return getReadableDatabase().query(TABLENAME, new String[]{"ID", "TIME", "tag"}, "TIME >= ? AND TIME <= ?",new String[]{""+from, ""+to}, null, null, "TIME DESC");
	}
	
	// returns id (or -1)
	public long insert(long time, String tag)
	{
		ContentValues values = new ContentValues();
		values.put("TIME", time);
		values.put("tag", tag);
		return getWritableDatabase().insert(TABLENAME, null, values);
	}
	
	public boolean setTag(long id, String tag)
	{
		ContentValues values = new ContentValues();
		values.put("tag", tag);
		int res = getWritableDatabase().update(TABLENAME, values, "ID = ?", new String[]{""+id});
		return res==1;	// makes sure 1 row was affected
	}
}
