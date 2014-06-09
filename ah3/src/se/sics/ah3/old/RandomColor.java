package se.sics.ah3.old;

import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

public class RandomColor implements IColorProvider {
	public static final String TABLE_NAME	= "randomColor";

	public static final String ID			= "id";
	public static final String RED			= "red";
	public static final String GREEN		= "green";
	public static final String BLUE			= "blue";

	int mColor;
	boolean isInit = false;

	public float[] fillArousalColorBuffer(DataStore dataStore, int dataPos, int length)
	{
		
		SQLiteDatabase db = dataStore.getWritableDatabase();
		if(!isInit)
		{
			createTable(TABLE_NAME,db);
			isInit = true;
		}
		float[] colors = new float[length*4*2];
		RandomColor.checkFill(length,db);

		String table = TABLE_NAME;
		//String[] columns = Trip.ALL_COLUMNS;
		String limitString = "" +(length);
		String whereClause = ID + ">" + (dataPos-1) ;

		Cursor c = db.query(table,null,whereClause,null,null,null,null,limitString);

		int numRows = c.getCount();
		//int numCols = c.getColumnCount();
		int r =0 ,g =0 ,b = 0;
		c.moveToFirst();
		for(int i = 0; i < numRows; i ++)
		{
			r = c.getInt(c.getColumnIndex(RED));
			g = c.getInt(c.getColumnIndex(GREEN));
			b = c.getInt(c.getColumnIndex(BLUE));
			/*for(int j = 0; j < numCols;j++)
			{
				if(c.getColumnName(j).equals(RED))
					r = c.getInt(j);
				if(c.getColumnName(j).equals(GREEN))
					g = c.getInt(j);
				if(c.getColumnName(j).equals(BLUE))
					b = c.getInt(j);
			}*/
			colors[i*8+0] = (float) (r/255.0);
			colors[i*8+1] = (float) (g/255.0);
			colors[i*8+2] = (float) (b/255.0);
			colors[i*8+3] = 1.0f;
			colors[i*8+4] = (float) (r/255.0);
			colors[i*8+5] = (float) (g/255.0);
			colors[i*8+6] = (float) (b/255.0);
			colors[i*8+7] = 1.0f;
			c.moveToNext();
		}
		c.close();
		return colors;
	}
	
	public static void createTable(SQLiteDatabase db)
	{
		createTable(TABLE_NAME, db);
	}

	public static void createTable(String tableName, SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ tableName + " ("
				+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ RED + " INTEGER, "
				+ GREEN + " INTEGER, "
				+ BLUE + " INTEGER "
				+");");
	}

	public static int getEntry(int pos, SQLiteDatabase db) 
	{
		int color = 0;
		String table = TABLE_NAME;
		//String[] columns = Trip.ALL_COLUMNS;
		String limitString = "" +1;
		String whereClause = ID + "=" + pos ;

		Cursor c = db.query(table,null,whereClause,null,null,null,null,limitString);

		int numRows = c.getCount();
		int numCols = c.getColumnCount();
		int r =0 ,g =0 ,b = 0;
		c.moveToFirst();
		for(int i = 0; i < numRows; i ++)
		{
			r = c.getInt(c.getColumnIndex(RED));
			g = c.getInt(c.getColumnIndex(GREEN));
			b = c.getInt(c.getColumnIndex(BLUE));
			/*for(int j = 0; j < numCols;j++)
			{
				if(c.getColumnName(j).equals(RED))
					r = c.getInt(j);
				if(c.getColumnName(j).equals(GREEN))
					g = c.getInt(j);
				if(c.getColumnName(j).equals(BLUE))
					b = c.getInt(j);
			}*/

			c.moveToNext();
		}
		c.close();
		color = Color.argb(0, r, g, b);
		return color;
	}

	public static void addEntry(int color, SQLiteDatabase db) 
	{
		ContentValues values = new ContentValues();
		values.put(RED, Color.red(color));
		values.put(GREEN, Color.green(color));
		values.put(BLUE, Color.blue(color));
		db.insert(TABLE_NAME, null, values);
	}
	
	
	public static void fillRandom(int size,SQLiteDatabase db)
	{
		int color;
		for(int i = 0; i < size; i++)
		{
			color = getColor();
			addEntry(color, db);
		}
	}
	
	public static int getColor()
	{
		int r,g,b;
		r = (int)(Math.random()*255);
		g = (int)(Math.random()*255);
		b = (int)(Math.random()*255);
		return Color.rgb(r, g, b);
	}
	
	public static void checkFill(int size,SQLiteDatabase db)
	{
		Cursor c = db.query(TABLE_NAME,null,null,null,null,null,null,null);
		int numRows = c.getCount();
		c.close();
		if(numRows < size)
		{
			int addSize = size-numRows;
			fillRandom(addSize, db);
		}
	}

	@Override
	public float[] fillArousalColorBuffer(DataStore dataStore, int length,
			GregorianCalendar start, GregorianCalendar end, long granularity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float[] fillArousalColorBuffer(DataStore dataStore, int length,
			GregorianCalendar start, long granularity) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
