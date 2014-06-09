package se.sics.ah3.old;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

public class BioPlux implements IDataSource{

	public static final String TABLE_NAME	= "rawBioPlux";
	//Column names
	public static final String ID			= "id";
	public static final String TIME			= "timestamp";
	public static final String FRAMENR 		= "framenr";
	public static final String DIGITAL 			= "digital";
	public static final String CH1 			= "channel1";
	public static final String CH2 			= "channel2";
	public static final String CH3 			= "channel3";
	public static final String CH4 			= "channel4";
	public static final String CH5 			= "channel5";
	public static final String CH6 			= "channel6";
	public static final String CH7 			= "channel7";
	public static final String CH8 			= "channel8";
	public static final String SYNC 		= "sync_status";

	public static final String[] SERVER_VALS= {ID,TIME,CH1,CH2,CH3,CH4,CH5};


	long mId;
	long mTimestamp;
	byte mFrameNr;
	boolean mDigital;
	short mChannel1;
	short mChannel2;
	short mChannel3;
	short mChannel4;
	short mChannel5;
	short mChannel6;
	short mChannel7;
	short mChannel8;
	byte mSyncStatus;

	public BioPlux() {
	
	}

	public void setValues(String[] tokens)
	{
		if(tokens.length == 10)
		{
			mFrameNr = Byte.parseByte(tokens[0]);
			mDigital = Boolean.parseBoolean(tokens[1]);
			mChannel1 = Short.parseShort(tokens[2]);
			mChannel2 = Short.parseShort(tokens[3]);
			mChannel3 = Short.parseShort(tokens[4]);
			mChannel4 = Short.parseShort(tokens[5]);
			mChannel5 = Short.parseShort(tokens[6]);
			mChannel6 = Short.parseShort(tokens[7]);
			mChannel7 = Short.parseShort(tokens[8]);
			mChannel8 = Short.parseShort(tokens[9]);			
		}
	}
	
	public int processArousal(SignalProcessor processor)
	{
		return processor.process((int)mChannel5);
	}
	
	public int processMovement(SignalProcessor processor)
	{
		return processor.process(new int[]{(int)mChannel2,(int)mChannel3,(int)mChannel4});
	}
	
	public int processPulse(SignalProcessor processor)
	{
		return processor.process(mChannel6);
	}

	public void saveToDb(DataStore helper)
	{
		ContentValues values = new ContentValues();
		values.put(FRAMENR, mFrameNr);
		String[] whereArgs = {(""+mId)};
		SQLiteDatabase db = helper.getWritableDatabase();
		if(mId >= 0)
		{
			//try to update an existing trip
			int success = db.update(BioPlux.TABLE_NAME, values, "ID = ?", whereArgs);
			if(success > 0)
				return;
		}
		mId = db.insert(TABLE_NAME, null, values);

	}

	/////////////////
	//DB related
	////////////////
	public static void createTable(SQLiteDatabase db)
	{
		createTable(TABLE_NAME, db);
	}

	public static void createTable(String tableName, SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ tableName + " ("
				+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ TIME + " TEXT, "
				+ FRAMENR + " INTEGER, "
				+ DIGITAL + " INTEGER, "
				+ CH1 +" INTEGER, "
				+ CH2 +" INTEGER, "
				+ CH3 +" INTEGER, "
				+ CH4 +" INTEGER, "
				+ CH5 +" INTEGER, "
				+ CH6 +" INTEGER, "
				+ CH7 +" INTEGER, "
				+ CH8 +" INTEGER, "
				+ SYNC + " INTEGER "
				+");");
	}

	public static void clean(SQLiteDatabase db)
	{

		db.delete(TABLE_NAME, null, null);
	}

	public static void resync(int oldSyncStatus, int newSyncStatus,SQLiteDatabase db)
	{
		ContentValues values = new ContentValues();
		values.put(SYNC, newSyncStatus);
		String whereClause = SYNC+"=?";
		db.update(TABLE_NAME, values, whereClause, new String[]{""+oldSyncStatus});
	}
	/////////////////
	//Server related
	////////////////
	public static  JSONObject buildJSON(int limit, SQLiteDatabase db) throws JSONException
	{
		return buildJSON(limit, db,true);
	}
	public static  JSONObject buildJSON(int limit, SQLiteDatabase db, boolean onlyUnsynced) throws JSONException
	{
		String table = BioPlux.TABLE_NAME;
		//String[] columns = Trip.ALL_COLUMNS;
		String limitString = "" +limit;
		String whereClause = null;
		if(onlyUnsynced)
		{
			whereClause = SYNC + "=" + 0 +" OR " + SYNC + " IS NULL";
		}
		Cursor c = db.query(table,SERVER_VALS,whereClause,null,null,null,null,limitString);

		int numRows = c.getCount();
		int numCols = c.getColumnCount();
		JSONObject trip = null;
		JSONArray tripArray = new JSONArray();
		c.moveToFirst();
		for(int i = 0; i < numRows; i ++)
		{
			trip = new JSONObject();
			for(int j = 0; j < numCols;j++)
			{
				//currentEntry.put(mapToRemote(c.getColumnName(j)), c.getString(j));
				putType(c, j, trip);
			}
			//putCalculatedFields(c, trip);
			tripArray.put(trip);
			c.moveToNext();
		}
		c.close();
		JSONObject data = new JSONObject();
		data.put("bioPluxRaw", tripArray);
		return data;
	}

	/*private static void putCalculatedFields(Cursor c, JSONObject trip) {
			long start = c.getLong(c.getColumnIndex(START));
			LocationEntry le = Loc

		}*/
	private static void putType(Cursor c, int column,JSONObject json) throws JSONException
	{
		json.put(mapToRemote(c.getColumnName(column)), c.getString(column));
		/*String columnName = c.getColumnName(column);
			if(columnName.equals(AVG_SPEED)||
					columnName.equals(DISTANCE)||
					columnName.equals(SPEED_SUM))
			{
				json.put(mapToRemote(columnName), c.getDouble(column));
				return;
			}

			if(columnName.equals(TRIP)||
					columnName.equals(ID))
			{
				json.put(mapToRemote(columnName), c.getString(column));
				return;
			}
			if(columnName.equals(CREATED_DATE))
			{
				Date d = new Date(c.getLong(column));
				json.put(mapToRemote(columnName), d.toString());
				return;
			}*/
	}
	public static String mapToRemote(String columnName)
	{
		if(columnName.equals(BioPlux.ID))
		{
			return "external_id";
		}
		return columnName;
	}

	/////////////////
	//getter setter
	////////////////
	public long getId() {
		return mId;
	}

	public void setId(long id) {
		mId = id;
	}
	
}


