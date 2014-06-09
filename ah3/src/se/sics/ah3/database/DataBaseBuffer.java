package se.sics.ah3.database;


import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import se.sics.ah3.InterpretationBuffer;
import android.database.Cursor;
import android.util.Log;


public class DataBaseBuffer {

	private final ArrayList<InterpretationBuffer> values = new ArrayList<InterpretationBuffer>();
//	private final FloatBuffer mTimestampBuffer;
	private final LongBuffer mTimestampBuffer;
	private final SignalDatabaseTable table;
	private final int resolution;
	private long t0=Long.MAX_VALUE;
	private final int length;
	private long time_offset = 0;
	
	// last request
	private long lr_end;
	
	public DataBaseBuffer(SignalDatabaseTable table,int length, int resolution) {
		this.table = table;
		this.resolution = resolution;
		this.length = length;
		mTimestampBuffer = LongBuffer.allocate(length);
	}

	public Column addBuffer(InterpretationBuffer cbuf, String name){
		Column c = table.createColumn(name);
		c.dbBuffers=this;
		cbuf.init(length);		
		c.buffer=cbuf;
		values.add(cbuf);
		return c;
	}
	
	
	
/*	protected void fillBuffers(){
		
		Cursor c = table.getQueryCursor(t0, t0+length*resolution);
		
		if(c.getCount()==0){		
			 nanPad();
			 return;
		}
		
		boolean done=false;
		
		int[] p= new int[values.size()];
		int p_time = 0;
		long time = t0 + length*resolution;

		boolean setoffset = time_offset==0;	// only do this the first time...

		while(c.moveToNext() && !done){	

			long t = c.getLong(0);
			int i=1;

			// do once.
			if (setoffset && lr_end>t) { time_offset = t; setoffset = false; }

			int n = t2i(t);
			int m = Math.min(n,length);
			for(int k=0; k<values.size(); k++){
				InterpretationBuffer b = values.get(k);
				if(p[k]>=length){
					done=true;
					continue;
				}
				float v = c.getFloat(i++);
				
				for(; p[k] < m; p[k]++) {	b.interpret(Float.NaN);				}
				//b.interpret(Float.NaN,m-p[k]);   p[k]=m;

				if(n<length && n == p[k]){
					b.interpret(v);
					p[k]++;
				}
			}
			// add timestamps to timestamp buffer
			long ts = m-p_time;
			long td = t-time;
			for (int j=0;j<ts;j++)
			{
				mTimestampBuffer.put(time);
				time-=resolution;
			}
			if (ts>1) {
				Log.d("DB", "ts is larger than 1: " + ts + " t=" + time);
			}
			p_time = n;
			time = t;
		}
		for(int k=0; k<values.size(); k++){
			InterpretationBuffer b = values.get(k);
			//b.interpret(Float.NaN,length-p[k]);
			for(; p[k] < length; p[k]++) {	b.interpret(Float.NaN);	}
		}
		while(p_time++<length)
			mTimestampBuffer.put(time);
	}
	
	
	
	public void nanPad(){
		for(InterpretationBuffer b : values){
		//	b.interpret(Float.NaN,length);
			for (int i = 0; i < length; i++) {b.interpret(Float.NaN);}
		}
		for (int i=0;i<length;i++)
			mTimestampBuffer.put(t0);
	}

	public void setPosition(long start, long end){
		for(InterpretationBuffer b : values){
			b.setPosition(t2i(end),t2i(start));
			
		}
//		mTimestampBuffer.setPosition(t2i(end),t2i(start));
		mTimestampBuffer.clear();
		mTimestampBuffer.position(t2i(end));
		mTimestampBuffer.limit(t2i(start));
	}
	public void clearPosition(){
		for(InterpretationBuffer b : values){
			b.clear();
			
		}
		mTimestampBuffer.clear();
	}
	protected int t2i(long t) {
		int i = (int)((t-t0)/(double)resolution);
		int j = length -1 - i;
		return j;
	}
	
	public void move(long start, long end){
		move(start,end,true);
	}

	public long last(){
		return t0+ (length-1)*resolution;
	}
	
	public synchronized void update(InterpretationBuffer b, long when, float v){
		if(when<t0 || when > last()   )
			return;

		setPosition(t0, when);
		b.interpret(v);
		mTimestampBuffer.put(when);
	}
	
	 
	public synchronized void move(long start, long end, boolean center){
		
//		long now = System.currentTimeMillis();
//		long OFFSET=now;
//		if(lastStart!=start){
//			//System.out.println("T0 START END: "+(t0-OFFSET)+" "+(start-OFFSET)+", "+(end-OFFSET)+" "+(lastStart>start?"backwards":"forwards"));
//			if(now>= start && now <=end){
//				System.out.println("Viewing NOW");
//			}
//		}		
//		lastStart=start;		

		lr_end = end;

		if(start<t0 || end > last()  ){
			Log.v("DB", "FILL: "+length);
			
			t0 = start- (length*resolution-(end-start))/2;
			t0 = t0 - (t0%resolution);
			clearPosition();
			long tick = System.currentTimeMillis();
			
			fillBuffers();
			
			long tock = System.currentTimeMillis();
			Log.v("DB","time: "+(tock-tick)); 

		}
			
		setPosition(start, end);
	}

	public long getTimeOffset(long t) {
//		return time_offset;
		long pos = mTimestampBuffer.position();
		long offset = mTimestampBuffer.get(t2i(t)); //+resolution/2)); //mTimestampBuffer.position());
//		Log.d("DB" , "DB pos: " + pos + " pos_offset: " + offset + " t0_offset: " + time_offset);
		return time_offset;
	}

	public LongBuffer getTimestampsColumn()
	{
		return mTimestampBuffer;
	}*/
}
