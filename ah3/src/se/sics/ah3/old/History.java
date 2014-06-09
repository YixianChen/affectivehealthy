package se.sics.ah3.old;

import java.util.Date;
import java.util.GregorianCalendar;

import se.sics.ah3.Error;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;

public class History implements IColorProvider, IMovementProvider, IBioDataProvider{
	
	public static final String TABLE_NAME	= "bioHistory";
	//Column names
	public static final String ID			= "id";
	public static final String TIME			= "timestamp";
	public static final String AROUSAL 		= "arousal";
	public static final String MOVEMENT 	= "movment";
	public static final String PULSE 		= "pulse";
	public static final String SYNC 		= "sync_status";

	public static final String[] SERVER_VALS= {ID,TIME,AROUSAL,MOVEMENT,PULSE};
	private static final float NO_DATA = 0;
	private static final long SECONDS_GRANULARITY = 1000;
	private static final float NO_COLOR = 0;
	public enum Channels {CAROUSAL, CMOVEMENT, CPULSE};
	long mId;
	long mTimestamp;
	short mArousal;
	short mMovement;
	short mPulse;
	int mBufferSize;
	int mBufferPos;
	short[] mArousalBuffer;
	short[] mMovementBuffer;
	short[] mPulseBuffer;
	byte mSyncStatus;
	private boolean isInit;
	Date d;

	ColorMapper colorMapper = null;
	
	public History(DataStore _dataStore) {
		// TODO Auto-generated constructor stub
		isInit = false;
		colorMapper = new ColorMapper();
		init(_dataStore.getWritableDatabase());
		
		mBufferSize = 100;
		mMovementBuffer = new short[mBufferSize];
		mArousalBuffer = new short[mBufferSize];
		mPulseBuffer = new short[mBufferSize];
		mBufferPos = 0;
		d = new Date();
		mTimestamp = d.getTime();
		mSyncStatus = 0;
	}
	
	public History(DataStore _dataStore, long id) {
		// TODO Auto-generated constructor stub
		isInit = false;
		colorMapper = new ColorMapper();
		init(_dataStore.getWritableDatabase());
	}
	
	public void setUpdateValue(short val, Channels chan)
	{
		switch(chan)
		{
		case CAROUSAL:
			mArousal = val;
			break;
		case CMOVEMENT:
			mMovement = val;
			break;
		case CPULSE:
			mPulse = val;
			break;
		default:
			break;
		}
	}
	
	public void setUpdateValues(short arousal, short movment, short pulse)
	{
		mTimestamp = d.getTime();
		mArousal = arousal;
		mMovement = movment;
		mPulse = pulse;
		mSyncStatus = 0;
	}
	
	public void setCreateValues(GregorianCalendar date, short arousal2,
			short movement2, short pulse2) {
		mId = -1;
		d = date.getTime();
		mTimestamp = date.getTimeInMillis();
		mArousal = arousal2;
		mPulse = pulse2;
		mMovement = movement2;
	}
	
	public void setBufferdCreateValues(short arousal, short movment, short pulse, DataStore helper)
	{
		mId = -1;
		mTimestamp += 1000;
		mArousalBuffer[mBufferPos] = arousal;
		mMovementBuffer[mBufferPos] = movment;
		mPulseBuffer[mBufferPos] = pulse;
		mBufferPos++;
		if(mBufferPos == mBufferSize)
			saveBufferedToDb(helper);
	}
	
	void init(SQLiteDatabase db)
	{
		createTable(db);
		isInit = true;
	}
	
	public static int updateToDbByTime(DataStore helper,long timeStamp, short val, Channels chan)
	{
		ContentValues values = new ContentValues();
		switch(chan)
		{
		case CAROUSAL:
			values.put(AROUSAL, val);
			break;
		case CMOVEMENT:
			values.put(MOVEMENT, val);
			break;
		case CPULSE:
			values.put(PULSE, val);
			break;
		default:
			break;
		}
		
		SQLiteDatabase db = helper.getWritableDatabase();
		String whereStatement = TIME+"=?";
		String[] whereArgs = {(""+timeStamp)};
		int success = db.update(TABLE_NAME, values, whereStatement, whereArgs);
		if(success == 0)
		{
			//create new entry
			values.put(TIME, timeStamp);
			db.insert(TABLE_NAME, null, values);
		}
		return success;
	}
	
	public void saveToDb(DataStore helper)
	{
		ContentValues values = new ContentValues();
		values.put(TIME, ""+getNowTimeStamp());
		values.put(AROUSAL, mArousal);
		values.put(MOVEMENT, mMovement);
		values.put(PULSE, mPulse);
	
		SQLiteDatabase db = helper.getWritableDatabase();
		if(!isInit)init(db);
		if(mId >= 0)
		{
			String[] whereArgs = {(""+mId)};
			//try to update an existing trip
			int success = db.update(TABLE_NAME, values, "ID = ?", whereArgs);
			if(success > 0)
				return;
		}
		mId = db.insert(TABLE_NAME, null, values);
	}
	
	private long getNowTimeStamp() {
		Date d = new Date();
		//get rid of milliseconds//commented out because we current algorithm can handle milliseconds;
		/*long timeInMilliSec = d.getTime()/1000;
		return timeInMilliSec*1000;*/
		return d.getTime();
	}

	public void saveBufferedToDb(DataStore helper)
	{
		mBufferPos = 0;
		
		SQLiteDatabase db = helper.getWritableDatabase();
		if(!isInit)
		{
			init(db);
		}
		ContentValues values = new ContentValues();
		for(int i = 0; i < mBufferSize; i++)
		{
			values.put(TIME, mTimestamp);
			values.put(AROUSAL, mArousal);
			values.put(MOVEMENT, mMovement);
			values.put(PULSE, mPulse);
			db.insert(TABLE_NAME, null, values);
		}
	}
	
	public void setValuesFromDbByTime(DataStore dataStore, long timeStamp) {
		SQLiteDatabase db = dataStore.getWritableDatabase();
		String table = TABLE_NAME;
		String limitString = "1";
		String whereClause = TIME + " = " + timeStamp ;

		Cursor c = db.query(table,null,whereClause,null,null,null,null,limitString);
		int numRows = c.getCount();
		c.moveToFirst();
		for(int i = 0; i < numRows; i ++)
		{
			mId = c.getLong(c.getColumnIndex(ID));
			mMovement = c.getShort(c.getColumnIndex(MOVEMENT));
			mArousal = c.getShort(c.getColumnIndex(AROUSAL));
			mPulse = c.getShort(c.getColumnIndex(PULSE));
			mSyncStatus = (byte)c.getInt(c.getColumnIndex(SYNC));
			c.moveToNext();
		}
	}
	
	@Override
	public float[] fillMovementBuffer(DataStore dataStore, int dataPos,
			int length) {
		SQLiteDatabase db = dataStore.getWritableDatabase();
		float[] movement = new float[length*1*1];
		//History.checkFill(length,db);

		String table = TABLE_NAME;

		String limitString = "" +(length);
		//String whereClause = ID + ">" + (dataPos-1) ;
		String whereClause = TIME + ">" + (dataPos-1) ;

		Cursor c = db.query(table,null,whereClause,null,null,null,null,limitString);
		
		int numRows = c.getCount();
		//int numCols = c.getColumnCount();
		int movementRaw = 0;
		float movementMapped = 0;
		c.moveToFirst();
		for(int i = 0; i < numRows; i ++)
		{
			movementRaw = c.getInt(c.getColumnIndex(MOVEMENT));
			movementMapped = NumberMapper.mapToFactor(movementRaw);
			
			movement[i] = movementMapped;
			//movement[i*2+1] = (float) (Color.green(color)/255.0);
			
			c.moveToNext();
		}
		//fill rest of buffer with place holder
		for(int i = numRows; i < length; i++)
		{
			movement[i] = 0.0f;
		}
		c.close();
		return movement;
	}
	
	@Override
	public float[] fillMovementBuffer(DataStore dataStore, int bufferLength,
			GregorianCalendar startTime, long granularity) {
		GregorianCalendar end = new GregorianCalendar();
		end.setTimeInMillis(startTime.getTimeInMillis()+(bufferLength*granularity));
		Log.d("AHBuffer", "filling movement buffer from: "+ startTime.getTime().toGMTString()+ " \n \t to: " + end.getTime().toGMTString());
		return fillMovementBuffer(dataStore, bufferLength, startTime, end, granularity);
	
		
	}
	
	public float[] fillMovementBuffer(DataStore dataStore, int bufferLength,
			GregorianCalendar startTime, GregorianCalendar endTime, long granularity) {
		SQLiteDatabase db = dataStore.getWritableDatabase();
		float[] movement = new float[bufferLength*1*1];
		//History.checkFill(length,db);

		String table = TABLE_NAME;

		String limitString = "" +(bufferLength*5);
		//String whereClause = ID + ">" + (dataPos-1) ;
		String whereClause = TIME + " < " + endTime.getTimeInMillis() + " AND " + TIME + " >= " + startTime.getTimeInMillis();
		String orderClause = TIME + " DESC" ;

		//TODO: find out why the where statement does not work!
		Cursor c = db.query(table,null,whereClause,null,null,null,orderClause,limitString);
		
		int numRows = c.getCount();
		//int numCols = c.getColumnCount();
		int movementRaw = 0;
		float movementMapped = 0;
		long timeStamp;
		int insertPosition = 0;
		c.moveToFirst();
		for(int i = 0; i < numRows; i ++)
		{
			movementRaw = c.getInt(c.getColumnIndex(MOVEMENT));
			movementMapped = NumberMapper.mapToFactor(movementRaw);
			timeStamp = c.getLong(c.getColumnIndex(TIME));
			insertPosition = calculateInverseTimeDistance(endTime,timeStamp,SECONDS_GRANULARITY);//calculateTimeDistance(start,timeStamp,SECONDS_GRANULARITY);
			if(insertPosition >= 0 && insertPosition < bufferLength)
				movement[insertPosition] = movementMapped;
			//movement[i*2+1] = (float) (Color.green(color)/255.0);
			
			c.moveToNext();
		}
		//fill rest of buffer with place holder
		for(int i = numRows; i < bufferLength; i++)
		{
			movement[i] = 0.0f;
		}
		c.close();
		return movement;
	}
	
	@Override
	public boolean fillBioBuffer(DataStore dataStore,
			GregorianCalendar start, GregorianCalendar end,
			float[] arousalBuffer, float[] movementBuffer, float[] pulseBuffer) {

		int length = movementBuffer.length;
		SQLiteDatabase db = dataStore.getWritableDatabase();
		String table = TABLE_NAME;
		
		String whereClause = TIME + " < " + end.getTimeInMillis() + " AND " + TIME + " >= " + start.getTimeInMillis();
		String orderClause = TIME + " DESC" ;

		Cursor c = db.query(table,null,whereClause,null,null,null,orderClause,null);
		int numRows = c.getCount();
		c.moveToFirst();
		int bufferPosition = 0;
		int insertPosition = 0;
		long timeStamp = 0;
		Date date;
		int movementRaw = 0;
		float movementMapped = 0;
		int arousalRaw = 0;
		int arousalColor = 0;
		c.moveToFirst();
		ColorMapper cm = new ColorMapper();//TODO: check if this should be a instance variable or a Singleton to speed up
	
		for(int i = 0; i < numRows; i ++)
		{
			movementRaw = c.getInt(c.getColumnIndex(MOVEMENT));
			movementMapped = NumberMapper.mapToFactor(movementRaw);
			arousalRaw = c.getShort(c.getColumnIndex(AROUSAL));
			arousalColor = cm.getErrorMapping(Error.NO_DATA);
			
			timeStamp = c.getLong(c.getColumnIndex(TIME));
			insertPosition = calculateTimeDistance(start,timeStamp,SECONDS_GRANULARITY);
			for(; bufferPosition < insertPosition; bufferPosition++)
			{
				movementBuffer[bufferPosition] = NO_DATA;
				
				arousalBuffer[bufferPosition*8 + 0] = NO_COLOR;
				arousalBuffer[bufferPosition*8 + 1] = NO_COLOR;
				arousalBuffer[bufferPosition*8 + 2] = NO_COLOR;
				arousalBuffer[bufferPosition*8 + 3] = 1.0f;
				arousalBuffer[bufferPosition*8 + 4] = NO_COLOR;
				arousalBuffer[bufferPosition*8 + 5] = NO_COLOR;
				arousalBuffer[bufferPosition*8 + 6] = NO_COLOR;
				arousalBuffer[bufferPosition*8 + 7] = 0.5f;
				
			}
			movementBuffer[bufferPosition] = movementMapped;
			
			arousalBuffer[bufferPosition*8 + 0] = (float) (Color.red(arousalColor)/255.0);
			arousalBuffer[bufferPosition*8 + 1] = (float) (Color.green(arousalColor)/255.0);
			arousalBuffer[bufferPosition*8 + 2] = (float) (Color.blue(arousalColor)/255.0);
			arousalBuffer[bufferPosition*8 + 3] = 1.0f;
			arousalBuffer[bufferPosition*8 + 4] = (float) (Color.red(arousalColor)/255.0);
			arousalBuffer[bufferPosition*8 + 5] = (float) (Color.green(arousalColor)/255.0);
			arousalBuffer[bufferPosition*8 + 6] = (float) (Color.blue(arousalColor)/255.0);
			arousalBuffer[bufferPosition*8 + 7] = 1.0f;
			
			
			/*mArousal = c.getShort(c.getColumnIndex(AROUSAL));
			mPulse = c.getShort(c.getColumnIndex(PULSE));
			mSyncStatus = (byte)c.getInt(c.getColumnIndex(SYNC));*/
			date = new Date(mTimestamp);
			c.moveToNext();		
		}
		
		
		//fill rest of buffer with place holder
		for(int i = insertPosition; i < length; i++)
		{
			movementBuffer[i] = NO_DATA;
			
			arousalBuffer[i*8 + 0] = NO_COLOR;
			arousalBuffer[i*8 + 1] = NO_COLOR;
			arousalBuffer[i*8 + 2] = NO_COLOR;
			arousalBuffer[i*8 + 3] = NO_COLOR;
			arousalBuffer[i*8 + 4] = NO_COLOR;
			arousalBuffer[i*8 + 5] = NO_COLOR;
			arousalBuffer[i*8 + 6] = NO_COLOR;
			arousalBuffer[i*8 + 7] = NO_COLOR;
		}
		c.close();
		return true;
	}
	
	private int calculateTimeDistance(GregorianCalendar earlyTime, long timeStamp,long granularity) {
		
		long difference = timeStamp - earlyTime.getTimeInMillis();
		int res = (int)(difference/granularity);
		return res;
	}
	
	private int calculateInverseTimeDistance(GregorianCalendar latestTime, long timeStamp,long granularity) {
		
		long difference =  latestTime.getTimeInMillis()- timeStamp;
		int res = (int)(difference/granularity);
		return res;
	}

	public float[] fillArousalColorBuffer(DataStore dataStore, int dataPos, int length)
	{
		SQLiteDatabase db = dataStore.getWritableDatabase();
		float[] colors = new float[length*4*2];
		/*
		//History.checkFill(length,db);

		String table = TABLE_NAME;
		//String[] columns = Trip.ALL_COLUMNS;
		String limitString = "" +(length);
		String whereClause = ID + ">" + (dataPos-1) ;

		Cursor c = db.query(table,null,whereClause,null,null,null,null,limitString);

		int numRows = c.getCount();
		//int numCols = c.getColumnCount();
		int arousal = 0 ,g =0 ,b = 0;
		int color = 0;
		c.moveToFirst();
		ColorMapper cm = new ColorMapper();//TODO: check if this should be a instance variable or a Singleton to speed up
		for(int i = 0; i < numRows; i ++)
		{
			arousal= c.getInt(c.getColumnIndex(AROUSAL));
			color = cm.mapToColor(arousal);*/
		
			/*for(int j = 0; j < numCols;j++)
			{
				if(c.getColumnName(j).equals(RED))
					r = c.getInt(j);
				if(c.getColumnName(j).equals(GREEN))
					g = c.getInt(j);
				if(c.getColumnName(j).equals(BLUE))
					b = c.getInt(j);
			}*/
		/*
			colors[i*8+0] = (float) (Color.red(color)/255.0);
			colors[i*8+1] = (float) (Color.green(color)/255.0);
			colors[i*8+2] = (float) (Color.blue(color)/255.0);
			colors[i*8+3] = 1.0f;
			colors[i*8+4] = (float) (Color.red(color)/255.0);
			colors[i*8+5] = (float) (Color.green(color)/255.0);
			colors[i*8+6] = (float) (Color.blue(color)/255.0);
			colors[i*8+7] = 1.0f;
			c.moveToNext();
		}
		for(int i = numRows; i < length; i++)
		{
			color = cm.getErrorMapping(Error.NO_DATA);
			colors[i*8+0] = (float) (Color.red(color)/255.0);
			colors[i*8+1] = (float) (Color.green(color)/255.0);
			colors[i*8+2] = (float) (Color.blue(color)/255.0);
			colors[i*8+3] = 1.0f;
			colors[i*8+4] = (float) (Color.red(color)/255.0);
			colors[i*8+5] = (float) (Color.green(color)/255.0);
			colors[i*8+6] = (float) (Color.blue(color)/255.0);
			colors[i*8+7] = 1.0f;
			
		}
		c.close();*/
		return colors;
	}
	
	public float[] fillArousalColorBuffer(DataStore dataStore, 
			int length, GregorianCalendar earliestTime, long granularity)
	{
		GregorianCalendar latestTime = new GregorianCalendar();
		latestTime.setTimeInMillis(earliestTime.getTimeInMillis()+(length*granularity));
		Log.d("AHBuffer", "filling buffer from: "+ earliestTime.getTime().toGMTString()+ " \n \t to: " + latestTime.getTime().toGMTString());
		return fillArousalColorBuffer(dataStore, length, earliestTime, latestTime, granularity);
	}
	
	public float[] fillArousalColorBuffer(DataStore dataStore, 
			int length, long granularity, GregorianCalendar latestTime)
	{
		GregorianCalendar earliestTime = new GregorianCalendar();
		earliestTime.setTimeInMillis(latestTime.getTimeInMillis()-(length*granularity));
		Log.d("AHBuffer", "filling buffer from: "+ earliestTime.getTime().toGMTString()+ " \n \t to: " + latestTime.getTime().toGMTString());
		return fillArousalColorBuffer(dataStore, length, earliestTime, latestTime, granularity);
	}
	public float[] mockFillArousalColorBuffer(DataStore dataStore, int length, GregorianCalendar earliestTime, GregorianCalendar latestTime, long granularity)
	{
		SQLiteDatabase db = dataStore.getWritableDatabase();
		float[] colors = new float[length*4*2];

		String table = TABLE_NAME;
		String whereClause = TIME + " < " + latestTime.getTimeInMillis() + " AND " + TIME + " >= " + earliestTime.getTimeInMillis();
		String orderClause = TIME + " DESC" ;

		Cursor c = db.query(table,null,whereClause,null,null,null,orderClause,null);
		int numRows = c.getCount();
		long timeStamp;
		int bufferPosition = 0;
		int insertPosition = 0;
		int arousal = 0 ,g =0 ,b = 0;
		int color = 0;
		c.moveToFirst();
		ColorMapper cm = new ColorMapper();//TODO: check if this should be a instance variable or a Singleton to speed up
		for(int i = 0; i < numRows; i ++)
		{
			arousal= c.getInt(c.getColumnIndex(AROUSAL));
			color = cm.mapToColor(arousal);
			timeStamp = c.getLong(c.getColumnIndex(TIME));
			Date debug_d = new Date(timeStamp);
			String gmt = debug_d.toGMTString();
			
			insertPosition = calculateInverseTimeDistance(latestTime, timeStamp, SECONDS_GRANULARITY);
			
			if(insertPosition < length)
			{
				bufferPosition++;
			}
			else
			{
				insertPosition = bufferPosition;
			}
			c.moveToNext();
		}
	
		//debug mark start and end
		colors[0*8+0] = 1.0f;
		colors[0*8+1] = 0.0f;
		colors[0*8+2] = 0.0f;
		colors[0*8+3] = 1.0f;
		colors[(length-1)*8+2] = 1.0f;
		colors[(length-1)*8+3] = 1.0f;
		c.close();
		return colors;
	}
	
	public float[] fillArousalColorBuffer(DataStore dataStore, int length, GregorianCalendar earliestTime, GregorianCalendar latestTime, long granularity)
	{
		SQLiteDatabase db = dataStore.getWritableDatabase();
		float[] colors = new float[length*4*2];

		String table = TABLE_NAME;
		String whereClause = TIME + " < " + latestTime.getTimeInMillis() + " AND " + TIME + " >= " + earliestTime.getTimeInMillis();
		String orderClause = TIME + " DESC" ;

		Cursor c = db.query(table,null,whereClause,null,null,null,orderClause,null);
		int numRows = c.getCount();
		long timeStamp;
		int bufferPosition = 0;
		int insertPosition = 0;
		int arousal = 0 ,g =0 ,b = 0;
		int color = 0;
		c.moveToFirst();
		ColorMapper cm = new ColorMapper();//TODO: check if this should be a instance variable or a Singleton to speed up
		for(int i = 0; i < numRows; i ++)
		{
			arousal= c.getInt(c.getColumnIndex(AROUSAL));
			color = cm.mapToColor(arousal);
			timeStamp = c.getLong(c.getColumnIndex(TIME));
			//Date debug_d = new Date(timeStamp);
			//Log.d("AHColor", debug_d.toGMTString()+ " : " + color);
			//insertPosition = calculateInverseTimeDistance(end,timeStamp,SECONDS_GRANULARITY);//calculateTimeDistance(start,timeStamp,SECONDS_GRANULARITY);
			insertPosition = calculateInverseTimeDistance(latestTime, timeStamp, SECONDS_GRANULARITY);
			//Log.d("AHColor", debug_d.toGMTString()+ " : " + color + " : " + insertPosition + " / " + calculateInverseTimeDistance(end,timeStamp,SECONDS_GRANULARITY));
			if(insertPosition < length)
			{
				/*for(; bufferPosition < insertPosition-1; bufferPosition++)
				{
					colors[bufferPosition*8 + 0] = NO_COLOR;
					colors[bufferPosition*8 + 1] = NO_COLOR;
					colors[bufferPosition*8 + 2] = NO_COLOR;
					colors[bufferPosition*8 + 3] = 0.5f;
					colors[bufferPosition*8 + 4] = NO_COLOR;
					colors[bufferPosition*8 + 5] = NO_COLOR;
					colors[bufferPosition*8 + 6] = NO_COLOR;
					colors[bufferPosition*8 + 7] = 0.5f;
					
				}
				Log.d("AHColor","r: "+(float) (Color.red(color)/255.0)+";"
						+"g: "+(float) (Color.green(color)/255.0)+";"
						+"b: "+(float) (Color.blue(color)/255.0));*/
				colors[insertPosition*8+0] = (float) (Color.red(color)/255.0);
				colors[insertPosition*8+1] = (float) (Color.green(color)/255.0);
				colors[insertPosition*8+2] = (float) (Color.blue(color)/255.0);
				colors[insertPosition*8+3] = 1.0f;
				colors[insertPosition*8+4] = (float) (Color.red(color)/255.0);
				colors[insertPosition*8+5] = (float) (Color.green(color)/255.0);
				colors[insertPosition*8+6] = (float) (Color.blue(color)/255.0);
				colors[insertPosition*8+7] = 1.0f;
				
				/*colors[insertPosition*8+0] = 0.0f;
				colors[insertPosition*8+1] = 0.0f;
				colors[insertPosition*8+2] = 0.0f;
				colors[insertPosition*8+3] = 1.0f;
				colors[insertPosition*8+4] = (float) (Color.red(color)/255.0);
				colors[insertPosition*8+5] = (float) (Color.green(color)/255.0);
				colors[insertPosition*8+6] = (float) (Color.blue(color)/255.0);
				colors[insertPosition*8+7] = 1.0f;*/
				bufferPosition++;
			}
			else
			{
				insertPosition = bufferPosition;
			}
			c.moveToNext();
		}
		/*for(int i = insertPosition; i < length; i++)
		{
			color = cm.getErrorMapping(Error.NO_DATA);
			colors[i*8+0] = (float) (Color.red(color)/255.0);
			colors[i*8+1] = (float) (Color.green(color)/255.0);
			colors[i*8+2] = (float) (Color.blue(color)/255.0);
			colors[i*8+3] = 1.0f;
			colors[i*8+4] = (float) (Color.red(color)/255.0);
			colors[i*8+5] = (float) (Color.green(color)/255.0);
			colors[i*8+6] = (float) (Color.blue(color)/255.0);
			colors[i*8+7] = 1.0f;
		}*/
		//debug mark start and end
		colors[0*8+0] = 1.0f;
		colors[0*8+1] = 0.0f;
		colors[0*8+2] = 0.0f;
		colors[0*8+3] = 1.0f;
		colors[(length-1)*8+2] = 1.0f;
		colors[(length-1)*8+3] = 1.0f;
		c.close();
		return colors;
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
				+ AROUSAL + " INTEGER, "
				+ MOVEMENT + " INTEGER, "
				+ PULSE +" INTEGER, "
				+ SYNC + " INTEGER "
				+");");
	}

	public static void clean(SQLiteDatabase db)
	{
		db.delete(TABLE_NAME, null, null);
	}
	
	public static Channels determinChannel(String fileName) {
		//standard AH2 naming scheme
		
		String[] tokens = fileName.split("_");
		//token 0 should be AHHistory
		//token 1 should be Year
		//token 2 should be Month
		//token 3 should be Day
		//token 4 should be Channel
		//token 5 should be Granularity
		int channel = 0;
		if(tokens.length == 6)
		{
			channel = Integer.parseInt(tokens[4]);
			switch(channel)
			{
			//EAHHeartRate = 64,
			//EAHArousal = 128,
			//EAHArousalS = 136,
			//EAHMovement = 192,
			case 64:
				return History.Channels.CPULSE;
			case 128:
				return History.Channels.CAROUSAL;
			case 192:
				return History.Channels.CMOVEMENT;
			}
		}
		
		return null;
	}

	
}