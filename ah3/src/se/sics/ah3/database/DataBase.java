/**
 * 
 */
package se.sics.ah3.database;

import java.util.ArrayList;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class DataBase extends SQLiteOpenHelper {
	
	private Context mContext;
	
	ArrayList<SignalDatabaseTable> tables  = new ArrayList<SignalDatabaseTable>();

	public DataBase(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		
		mContext = context;
		
		onCreate(getWritableDatabase());
	}

	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion==10 && newVersion==11) {
			db.execSQL("ALTER TABLE MYVALS ADD COLUMN FILTERED_GSR REAL");
		} else {
			for (SignalDatabaseTable table : tables) {
				String name = table.getTableName();
				db.execSQL("DROP TABLE IF EXISTS "+name);
			}
		}
	}	
	
	public void create(){
		onCreate(getWritableDatabase());
	}

	public SignalDatabaseTable createTable(String name){
		SignalDatabaseTable table = new SignalDatabaseTable(mContext, name,this);
		tables.add(table);		
		//table.createTable(getWritableDatabase());
		return table;
		
	}
	
	public SignalDatabaseTable getTable(String name){		
		for (SignalDatabaseTable table : tables) {
			if(table.getTableName().equals(name))				
				return table;
		}
		return null;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		//System.out.println("CREATE");		
		for (SignalDatabaseTable table : tables) {
			table.createTable(db);
		}
		
	}
}