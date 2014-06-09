package se.sics.ah3;

import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import se.sics.ah3.database.Column;
import se.sics.ah3.database.Column.TimeData;
import se.sics.ah3.database.SignalCacheDatabaseTable;
import android.content.Context;
import android.util.Log;

public class DataProxy implements Column.Listener {
	private final static String TAG = "DataProxy";

	public static final int METHOD_MAX = 0;
	public static final int METHOD_MEAN = 1;
	protected final Column mColumn;
	protected final int mMethod;

	private class DataBuffererQuant extends DataBufferer {
		
		SignalCacheDatabaseTable mCacheDatabase = null;
		
		public DataBuffererQuant(String name, long quant) {
			super(quant);
//			mQuantsize = quant;
//			MAX_TIME_FETCH = Math.min(1000*240*7,quant*10);
			MAX_TIME_FETCH = quant*10;
			
			mCacheDatabase = new SignalCacheDatabaseTable(mContext.getApplicationContext(), quant, name + "_cacheQuant_"+quant, null, 11);
		}
		
		public Vector<TimeData> getMoreData(long start, long end) {
		
			if(mCacheDatabase.isCached(start, end)) {
				return mCacheDatabase.get(start, end);
			}
			
			Vector<TimeData> output = new Vector<TimeData>();

			final long quantsize = mQuantsize;

			end = end-(end%quantsize);
			Vector<TimeData> data = mColumn.getTimeData(start, end);
			
//			Log.d("", "getting more data " + start + " " + end + " " + ((end-start)/(1000*60)));
			
			int i=0;
			long time = end; //end-(end%quantsize);
			while(time>start-mQuantsize && i<data.size()) {
				int n = 0;
				float value = 0;
				float top = 0;
				float bottom = 70000;
				TimeData td = data.get(i);
				while(td.time>time && i+n<data.size()) {
					if (td.value>top) top = td.value;
					if (td.value<bottom) bottom = td.value;
					value += td.value;
					if (i+n==data.size()) break;
					td = data.get(i+n);
					n++;
				}
				if (n>0) {
					if (mMethod==METHOD_MEAN)
						value = value / n;
					else if (mMethod==METHOD_MAX)
						value = top;
					td = new TimeData();
					td.time = time;
					td.value = value;
					output.add(td);
					i+=n;
				} else {
				}
				time-=quantsize;
			}

//			Log.d(TAG, "Segment size: " + data.size() + " Output: " + output.size());

			storeOutput(output);
			return output;
		}
		
		private void storeOutput(Vector<TimeData> output) {
			mCacheDatabase.store(output);
		}
	}

	private class DataBuffererRaw extends DataBufferer {
		public DataBuffererRaw() {
			super(1);
		}
	}

	private abstract class DataBufferer {
		long bstart = 0, bend = 0;
		Vector<TimeData> bdata = null;
		long MAX_TIME_FETCH = 1000*60;
		protected final long mQuantsize;
		
		protected DataBufferer(long quantsize) { mQuantsize = quantsize; }
		
		private int find(long time, int left, int right) {
			if (left==right) return right;
			TimeData l = bdata.get(left);
			TimeData r = bdata.get(right);
			if (l.time<time) return left;
			if (r.time>time) return right;
			int middle = (left+right)/2;
			if (middle==left || middle==right) return middle;
			TimeData m = bdata.get(middle);
			if (m.time>time) return find(time,middle,right);
			else return find(time,left,middle);
		}

		private int findIndex(long time) {
			if (bdata.size()==0) return 0;
			return find(time, 0, bdata.size()-1);
		}

		public Vector<TimeData> getMoreData(long start, long end) {
			return mColumn.getTimeData(start, end);
		}

		private String pad(int i) { return pad(""+i); }
		private String pad(String s) {
			if (s.length()<2) return "0"+s;
			return s;
		}
		private String formatTime(long time) {
			Date d = new Date();
			d.setTime(time);
			return (d.getYear()+1900) + "-" + pad(d.getMonth()+1) + "-" + pad(d.getDate()) + " " + d.getHours() + ":" + pad(d.getMinutes()) + "." + pad(d.getSeconds());
		}

		public Collection<TimeData> get(long start, long end) {
			start = start - (start%mQuantsize);
			end = end - (end%mQuantsize);

//			Log.d(TAG, "Asking for data " + formatTime(start) + " " + formatTime(end));
//			Log.d(TAG, "Having data " + formatTime(bstart) + " " + formatTime(bend));

			if (bdata == null || start>bend || end<bstart) {
				mDirty = true;
				if (end-start>MAX_TIME_FETCH) {
					start = end-MAX_TIME_FETCH;
				}
//				Log.d(TAG, "Getting new data " + formatTime(start) + "  " + formatTime(end));
				bdata = getMoreData(start,end);
				bstart = start;
				bend = bdata.size()>0?bdata.get(0).time:start;
				return bdata;
			}

			if (start<bstart) {
				mDirty = true;
				if (bstart-start>MAX_TIME_FETCH) {
					start = bstart-MAX_TIME_FETCH;
				}
//				Log.d(TAG, "Getting earlier data " + formatTime(start) + "  " + formatTime(bstart));
				Vector<TimeData> ts = getMoreData(start, bstart);
				bdata.addAll(ts);
/*				if (ts.size()>0) {
					bstart = ts.get(ts.size()-1).time; //start;
				}*/
				bstart = start;
			}
			if (end>bend) {
				mDirty = true;
//				Log.d("", "end>bend");
				boolean buffering = false;
				if (end-bend>MAX_TIME_FETCH) {
					end = bend + MAX_TIME_FETCH;
					buffering = true;
				}
//				Log.d(TAG, "Getting later data " + formatTime(bend) + "  " + formatTime(end));
				Vector<TimeData> ts = getMoreData(bend+0, end);
				ts.addAll(bdata);
				bdata = ts;
				if (ts.size()>0) {
					bend = ts.get(0).time;
				} else if (!buffering && !AHState.getInstance().isUpdatingRealtime()) {
//					long now = System.currentTimeMillis();
//					bend = Math.min(now-(now%mQuantsize),end-(end%mQuantsize));
				}
			}

			int si = findIndex(start);
			int ei = findIndex(end);
//			if (bdata.get(ei).time<end&&ei>0) ei--;
//			Log.d("", "Getting index: " + si + " " + ei);
			return bdata.subList(ei,si);	// reverse index since buffer starts at endtime
		}
		
		public void dataInserted(long time) {
			if (time>bstart && time<bend) {
				bend = time-(time%mQuantsize);
				Log.d(TAG, "Data Inserted and we are making sure it will be loaded if needed");
			}
		}
	}
	
	private boolean mDirty = false;

	private DataBufferer mDataBuffererRaw = null;
	private DataBufferer mDataBuffererMinutes = null;
	private DataBufferer mDataBufferer5s = null;
	private DataBufferer mDataBufferer60s = null;
	private DataBufferer mDataBufferer240s = null;
	private DataBufferer mDataBufferer7days = null;

	private Context mContext;

	public DataProxy(String name, Column col, int method, Context context) {
		mColumn = col;
		mMethod = method;
		mContext = context;
		
		mDataBuffererRaw = new DataBuffererRaw();
		mDataBuffererMinutes = new DataBuffererQuant(name, 1000*10);
		mDataBufferer5s = new DataBuffererQuant(name, 1000*5);
		mDataBufferer60s = new DataBuffererQuant(name, 1000*60);
		mDataBufferer240s = new DataBuffererQuant(name, 1000*240);
		mDataBufferer7days = new DataBuffererQuant(name, 1000*240*7);
		
		mColumn.setListener(this);
	}

	public synchronized Collection<TimeData> getData(long start, long end) {
		/*
		 * 60 seconds	:	raw
		 * 10 minutes	:	5 seconds
		 * 1 hour		:	10 seconds
		 * 6 hours		:	60 seconds
		 * 1 day		:	240 seconds
		 * 7 days		:	7*240 seconds
		 */
		mDirty = false;
		if (end-start<1000*60*10) {
			return mDataBuffererRaw.get(start, end);
		} else if (end-start<1000*60*20) {
			return mDataBufferer5s.get(start, end);
		} else if (end-start<1000*60*60*2) {
			return mDataBuffererMinutes.get(start, end);
		} else if (end-start<1000*60*60*7) {
			return mDataBufferer60s.get(start, end);
		} else if (end-start<1000*60*60*25) {
			return mDataBufferer240s.get(start, end);
		} else {
			return mDataBufferer7days.get(start, end);
		}
	}
	
	public synchronized boolean isDirty() {
		return mDirty;
	}
	
	public synchronized long getDataResolution(long start, long end) {
		if (end-start<1000*60*10) {
			return 1000;
		} else if (end-start<1000*60*20) {
			return 5*1000*2;
		} else if (end-start<1000*60*60*2) {
			return 10*1000*2;
		} else if (end-start<1000*60*60*7) {
			return 60*1000*2;
		} else if (end-start<1000*60*60*25) {
			return 240*1000*2;
		} else {
			return 7*240*1000*2;
		}
	}

	@Override
	public synchronized void dataInserted(long time) {
		mDataBuffererRaw.dataInserted(time);
		mDataBufferer5s.dataInserted(time);
		mDataBuffererMinutes.dataInserted(time);
		mDataBufferer60s.dataInserted(time);
		mDataBufferer240s.dataInserted(time);
		mDataBufferer7days.dataInserted(time);
	}
}
