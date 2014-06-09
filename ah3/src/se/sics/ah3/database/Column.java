package se.sics.ah3.database;

import java.util.Vector;

import se.sics.ah3.InterpretationBuffer;
import android.database.Cursor;

public class Column {
	protected InterpretationBuffer buffer;
	private int index;
	protected DataBaseBuffer dbBuffers;
	private final SignalDatabaseTable table;
	private long mLastTime;

	public interface Listener {
		public void dataInserted(long time);
	}
	
	private Listener mListener;

	public Column(int index,SignalDatabaseTable table) {
		super();
		this.table = table;
		this.index = index;
	}
	
//	public FloatBuffer getBuffer(long start, long end){
//		
//		synchronized (dbBuffers) {
//			dbBuffers.move(start, end);
//			return buffer.getBuffer().duplicate(); 
//		}
//		
//	}
	
	public void setListener(Listener listener) {
		mListener = listener;
	}
	
	public static class TimeData {
		public long time;
		public float value;
	}

	public Vector<TimeData> getTimeData(long start, long end) {
		Vector<TimeData> timedata = new Vector<TimeData>();
		Cursor c = table.getQueryCursor(start, end);

		boolean done = false;
		while(c.moveToNext() && !done){	
			long t = c.getLong(0);

			TimeData td = new TimeData();
			td.time = t;
			td.value = c.getFloat(index);
			timedata.add(td);
		}
		c.close();

		return timedata;
	}
	
	public boolean insert(long time, float value){
		boolean ret  = table.insert(time, value, index);
//		dbBuffers.update(buffer, time, value);

		if (mLastTime<time) mLastTime = time;

		if (mListener!=null) {
			mListener.dataInserted(time);
		}

		return ret;
	}
	
//	public long getTimeOffset(long t) {
//		return dbBuffers.getTimeOffset(t);
//	}
//	
//	public long getLastTime() {
//		return mLastTime;
//	}
}
