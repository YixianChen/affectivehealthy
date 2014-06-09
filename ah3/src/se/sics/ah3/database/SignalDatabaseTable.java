package se.sics.ah3.database;

import java.util.ArrayList;

import se.sics.ah3.AHState;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SignalDatabaseTable {
	final static String TAG = "SignalDBTable";
	final static String TIME = "TIME"; 

	private String[] colNames = new String[]{TIME};
	private ArrayList<Column> columns = new ArrayList<Column>(); 
	private final SQLiteOpenHelper helper;
	private final String tableName;

	private GSRFilter mGsrFilter;

	// old way of doing it
	//private boolean[] inserts;
	// new way of doing it.
	private float[] colValues;
	private int colValuesSet;				// number of values set for this row...
	private long colTime = Long.MIN_VALUE;	// time corresponding to the current row...

	public SignalDatabaseTable(Context context, String name, SQLiteOpenHelper helper) {
		this.tableName = name;
		this.helper = helper;

		mGsrFilter = new GSRFilter(context);
	}
	
	private void resetColValues()
	{
		if (colValues==null || colValues.length!=columns.size())
			colValues = new float[columns.size()];
		for (int i=0;i<colValues.length;i++) colValues[i] = Float.NaN;
		colValuesSet = 0;
		colTime = Long.MIN_VALUE;
	}
	
	public void processGSRFilter(String gsrColumnName, String gsrFilteredColumnName) {
		// this first one is if you only want to process the ones without filtered value calculated. note, the result will be wrong since the filter is dependant on previous values
		// Cursor gsrCursor = helper.getReadableDatabase().query(tableName, new String[]{TIME, gsrColumnName}, gsrFilteredColumnName + "=?", new String[]{""}, null, null, "TIME ASC");

		// this will reprocess all values
		Cursor gsrCursor = helper.getReadableDatabase().query(tableName, new String[]{TIME, gsrColumnName}, null, null, null, null, "TIME ASC");
		
		SQLiteDatabase writeableDb = helper.getWritableDatabase();
		int count = gsrCursor.getCount();
		int i = 0;
		while(gsrCursor.moveToNext()) {
			float gsrValue = gsrCursor.getFloat(1);
			long time = gsrCursor.getLong(0);
			Log.d(TAG, "reprocessing " + i * 100.0f / count + "%");
			i++;
			float filteredGsrValue = mGsrFilter.filter(gsrValue);
			writeableDb.execSQL("UPDATE " + tableName + " SET " + gsrFilteredColumnName + "=" + filteredGsrValue + " WHERE " + TIME + "=" + time);
		}
	}
	
	
	public synchronized Column createColumn(String name){
		Column c = new Column(columns.size()+1,this);
		columns.add(c);
//		inserts=new boolean[columns.size()];
		resetColValues();
		String[] tmp =new String[colNames.length+1];
		System.arraycopy(colNames, 0, tmp, 0, colNames.length);
		tmp[colNames.length]=name;		
		colNames = tmp;
		return c;
	}

	public String getTableName() {
		return tableName;
	}

	public Cursor getQueryCursor(long from, long to) {
		return helper.getReadableDatabase().query(tableName, colNames, "TIME > ? AND TIME <= ?",new String[]{""+from, ""+to}, null, null, "TIME DESC");
	}

	public Cursor getQueryCursorStraight(long from, long to) {
		return helper.getReadableDatabase().query(tableName, colNames, "TIME > ? AND TIME <= ?",new String[]{""+from, ""+to}, null, null, "TIME ASC");
	}

	public synchronized boolean insert(long time, float value, int icol){
		long td = Math.abs(time-colTime);
		if (!Float.isNaN(colValues[icol-1]) || td>500) {
			// write the current row and reset if current value is already set.
			if (colValuesSet>0)
			{
				insertCurrentRow();
			}
			resetColValues();
			colTime = time;
		}
		if (colValuesSet==0) colTime = time;
		colValues[icol-1] = value;
		colValuesSet++;
		if (colNames[icol].compareTo("GSR")==0) {
			float filteredValue = mGsrFilter.filter(value/65535f)*65535f;
			colValues[icol] = filteredValue;
			AHState.getInstance().mRTgsr = filteredValue;
			colValuesSet++;
		}
		if (colValuesSet==colValues.length) {
			insertCurrentRow();
			resetColValues();
		}
		return true;
	}
	
	private void insertCurrentRow() {
		ContentValues values = new ContentValues();
		values.put(TIME, colTime);
		for (int i = 1; i < colNames.length; i++) {
			values.put(colNames[i], colValues[i-1]);
		}
		try {
			helper.getWritableDatabase().insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		} catch (SQLiteConstraintException sqlce) {
			// we got the same time stamp. should not overwrite or restore this.... just wierd.
		}
	}
	
	public static class Result{
		public long[] times;
		public float[][] columns;
		public Result(long[] times, float[][] columns) {
			super();
			this.times = times;
			this.columns = columns;
		}
		
	}
	
	//Each sub array represent one column
	public Result read(long from, long to, String[] cols){
		
		String[] tcols = new String[cols.length+1];
		tcols[0]="TIME";
		System.arraycopy(cols,0, tcols, 1, cols.length);
		Cursor c = helper.getReadableDatabase().query(tableName, tcols, "TIME >= ? AND TIME <= ?",new String[]{""+from, ""+to}, null, null, null);
		
		float[][] vals = new float[c.getColumnCount()-1][c.getCount()+1];
		long[] times = new long[c.getCount()+1];
		int i=0;
		while(c.moveToNext()){	
			times[i]=c.getLong(0);
			for (int j = 0; j < vals[i].length; j++){				
				vals[j][i]=c.getFloat(j+1);	
			}
			i++;
		}
		
		return new Result(times,vals);			
	}
	
	
	void  createTable(SQLiteDatabase db){	
		System.out.println("Create Table "+tableName);
		String a="", b="";
		
		for (int i = 1; i < colNames.length; i++) {
			a=b + colNames[i]+ " REAL";
			b=a+", ";
		}
						
		String cmd = "CREATE TABLE IF NOT EXISTS "
			+ tableName + " ("
		//	+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
			+ TIME + " INTEGER PRIMARY KEY, "	
			+ a+");";
			System.out.println(cmd);	
		db.execSQL(cmd);
	}
	
	public void deleteAll() {
		helper.getWritableDatabase().execSQL("DROP TABLE " + tableName);
		createTable(helper.getWritableDatabase());
	}
}
