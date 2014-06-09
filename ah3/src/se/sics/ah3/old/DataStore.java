package se.sics.ah3.old;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.sics.ah3.random.RandomColor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;

public class DataStore extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "affectiveHealth";
	private static final int DATABASE_VERSION = 1;
	int mTestUploadLimit = 100;
	int _testSetSize = 1000;
	//private ServerHelper mServerHelper;
	//private Uploader uploader = null;
	public DataStore(Context ctx) {
		super(ctx, DATABASE_NAME + ".db", null, DATABASE_VERSION);
		SQLiteDatabase db = getWritableDatabase();
		createTables(db);
		//RandomColor.checkFill(_testSetSize, db);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		int curVersion = oldVersion;
		String sql;
		while(curVersion < newVersion)
		{
			switch (curVersion) {
			default:
				break;
			}
		}
	}
	
	public int getArousalColor(int pos)
	{
		if(pos > _testSetSize)
			return 0;
		SQLiteDatabase db = getWritableDatabase();
		return RandomColor.getEntry(pos,db);
	}
	
	public void dropTables()
	{
		getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+ History.TABLE_NAME);
		getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+ BioPlux.TABLE_NAME);
		getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+ RandomColor.TABLE_NAME);
	}

	public void createTables() {
		SQLiteDatabase db = getWritableDatabase();
		createTables(db);
	}
	
	private void createTables(SQLiteDatabase db)
	{
		History.createTable(db);
		//BioPlux.createTable(db);
		//RandomColor.createTable(db);
	}

	
	public void printDb()
	{
		//printTable(BioPlux.TABLE_NAME, null,200);
		printTable(History.TABLE_NAME, null,200);
		//printTable(RandomColor.TABLE_NAME, null,200);
	}
	
	public void printTable(String table, String[] columns, int limit)
	{
		SQLiteDatabase db = getReadableDatabase();
		Log.i("AH3DB",""+countEntries(table,db));
		String limitString = "" +limit;
    	Cursor c = db.query(table,columns,null,null,null,null,null,limitString);
    	int numRows = c.getCount();
    	int numCols = c.getColumnCount();
   		c.moveToFirst();
   		String entry = "";
   		for(int j = 0; j < numCols;j++)
   		{
   			entry += c.getColumnName(j) + "\t|\t";
   		}
   		Log.i("AH3DB", entry);
   		for(int i = 0; i < numRows; i ++)
   		{
   			entry = "";
   			for(int j = 0; j < numCols;j++)
   			{
   				entry += c.getString(j) + "\t|\t";
   			}
   			Log.i("AH3DB", entry);
   			c.moveToNext();
   		}
	}
	
	public void backup(SQLiteDatabase db) 
	{
		try{
			String dbPath = db.getPath();
		    File src = new File(dbPath);
			InputStream in = new FileInputStream(src);
			File dst = new File(Environment.getExternalStorageDirectory(),"dbBackup.db");
		    //File dst = new File(File.getExternalFilesDir(null, "dbBackup.db"));
			OutputStream out = new FileOutputStream(dst);

		    // Transfer bytes from in to out
		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0) {
		        out.write(buf, 0, len);
		    }
		    in.close();
		    out.close();
		}
		catch (IOException e) {
			Log.e("AH3DB_Backup", e.getMessage());
		}
		
	}
	
	public float distance(Location start, Location destination)
	{
		return start.distanceTo(destination);
	}
	
	public float speed(Location start, Location destination)
	{
		float meters = distance(start,destination);
		long d1 = start.getTime();
		long d2 = destination.getTime();
		long milliseconds = d2 - d1;
		double seconds = milliseconds/1000;
		float resultSpeed = (float) (meters/seconds);
		float measuredSpeed1 = start.getSpeed();
		float measuredSpeed2 = destination.getSpeed();
		return resultSpeed;
	}
	
	
	/*
	public boolean uploadTripsToServer() {
		try {
			JSONObject data = Trip.buildJSON(20, this.getReadableDatabase(),false);
			ServerHelper s = getServerHelper();
			if(s.upload(data,"trips"))
			{
				//mark entries as synced
				JSONArray dataArray = data.getJSONArray("trip");
				for(int i = 0; i < dataArray.length(); i++)
				{
					int id = dataArray.getJSONObject(i).getInt(LocationEntry.mapToRemote(LocationEntry.ID));
					ContentValues values = new ContentValues();
					values.put(LocationEntry.SYNC, 1);
					String whereClause = LocationEntry.ID + "=?";
					String[] whereArgs = {"" + id};
					getWritableDatabase().update(LocationEntry.TABLE_NAME, values, whereClause, whereArgs);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void uploadToServer()
	{
		if(uploader == null)
		{
			uploader = new Uploader();
			uploader.start();
		}
		else
		{
			uploader.stop();
			uploader = null;
		}
	}
	
	public boolean uploadLocationsToServer()
	{
		try {
			int limit = 50;
			JSONObject data = LocationEntry.buildJSONforLocations(limit,this.getReadableDatabase());
			ServerHelper s = new ServerHelper();
			if(s.upload(data,"location_entries"))
			{
				//mark entries as synced
				JSONArray dataArray = data.getJSONArray("location_entry");
				for(int i = 0; i < dataArray.length(); i++)
				{
					int id = dataArray.getJSONObject(i).getInt(LocationEntry.mapToRemote(LocationEntry.ID));
					ContentValues values = new ContentValues();
					values.put(LocationEntry.SYNC, 1);
					String whereClause = LocationEntry.ID + "=?";
					String[] whereArgs = {"" + id};
					getWritableDatabase().update(LocationEntry.TABLE_NAME, values, whereClause, whereArgs);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	*/
	
	public boolean cleanUpDatabase()
	{
		SQLiteDatabase db = getWritableDatabase();
		
		Cursor c = db.query(BioPlux.TABLE_NAME, null, BioPlux.SYNC + "=1", null, null, null, null);
		c.moveToFirst();
		for(int i = 0; i < c.getCount(); i++)
		{
			db.delete(BioPlux.TABLE_NAME, BioPlux.ID+"=?",  new String[]{c.getString(c.getColumnIndex(BioPlux.ID))});
		}
		return true;
	}
	
	public int countEntries(String table, SQLiteDatabase db)
	{
		final String SQL_STATEMENT = "SELECT COUNT(*) FROM ";
		Cursor c = db.rawQuery(SQL_STATEMENT+table+" ;", null);
		int count = -1;
		if(c.getCount() > 0)
		{
			c.moveToFirst();
			count = c.getInt(0);
		}
		c.close();
		return count;
		//return 0;
	}

	/*private ServerHelper getServerHelper()
	{
		if(mServerHelper == null)
		{
			mServerHelper = new ServerHelper();
		}
		return mServerHelper;
	}
	
	private class Uploader extends Thread
	{
		private Handler mHandler = new Handler();
		public Uploader() {
			
		}
		
		@Override
		public void run() 
		{

			try {
				int limit = 50;
				JSONObject data = LocationEntry.buildJSONforLocations(limit,getReadableDatabase());
				ServerHelper s = new ServerHelper();
				if(s.upload(data,"location_entries"))
				{
					//mark entries as synced
					JSONArray dataArray = data.getJSONArray("location_entry");
					for(int i = 0; i < dataArray.length(); i++)
					{
						int id = dataArray.getJSONObject(i).getInt(LocationEntry.mapToRemote(LocationEntry.ID));
						ContentValues values = new ContentValues();
						values.put(LocationEntry.SYNC, 1);
						String whereClause = LocationEntry.ID + "=?";
						String[] whereArgs = {"" + id};
						getWritableDatabase().update(LocationEntry.TABLE_NAME, values, whereClause, whereArgs);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			try {
				JSONObject data = Trip.buildJSON(20, getReadableDatabase());
				ServerHelper s = getServerHelper();
				if(s.upload(data,"trips"))
				{
					//mark entries as synced
					JSONArray dataArray = data.getJSONArray("trip");
					for(int i = 0; i < dataArray.length(); i++)
					{
						int id = dataArray.getJSONObject(i).getInt(Trip.mapToRemote(Trip.ID));
						ContentValues values = new ContentValues();
						values.put(Trip.SYNC, 1);
						String whereClause = Trip.ID + "=?";
						String[] whereArgs = {"" + id};
						getWritableDatabase().update(Trip.TABLE_NAME, values, whereClause, whereArgs);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
			mHandler.postDelayed(this, 10000);
		}
	}*/
}
