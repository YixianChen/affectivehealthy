package se.sics.ah3;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import se.sics.ah3.database.SignalDatabaseTable;
import se.sics.ah3.settings.AHSettings;
import se.sics.ah3.usertags.UserTags.UserTag;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

public class DataUploader implements Runnable {
	public static final String TAG = "Uploader";
	
//	public static final String SHARED_PREFS = "AffectiveHealth";
	//private static final String API_URL = "http://rost.me/biodata/api.php";
	private static final String API_URL = "http://newaffectivehealth.mobilelifecentre.org/api.php";
	//private static final String API_URL = "http://diyainteractive.com/health/api.php";
	
	
	public static final int STATE_IDLE = 0;
	public static final int STATE_UPLOAD = 1;
	public static final int STATE_DONE = 2;

	private String mUserName = "dump";

	private Context mContext;

	public interface Listener {
		void uploaderStopped(String reason);
		void uploaderIssue(String msg);
	}
	private Listener mListener = null;

	long sent_start,sent_end;
	int mState = STATE_IDLE;
	int status_toupload = 0;
	int status_progress = 0;
	
	SignalDatabaseTable mTable;
	private boolean mStream = false;

	private Thread mThread;
	private boolean mRun = true;

	private class SampleTag extends Sample {
		String tag;
		SampleTag(UserTag tag) {
			time = tag.getTime();
			this.tag = tag.getTag();
		}

		public String toString() {
			return "{\"t\":"+time+",\"tag\":\""+tag+"\"}";
		}
	}

	private class Sample {
		long time;
		float gsr;
		float acc;

		Sample() {
			// nothing
		}
		Sample(Cursor c) {
			// indexes:
			//   0: time
			//   1: acc	(xyz combined and filtered)
			//   2: gsr
			time = c.getLong(0);
			acc = c.getFloat(1);
			gsr = c.getFloat(2);
		}

		public String toString() {
			return "{\"t\":"+time+",\"gsr\":"+gsr+",\"acc\":"+acc+"}";
		}
	}

	public DataUploader(Context ctx, SignalDatabaseTable table, boolean stream, String username) {
		mContext = ctx;
		this.mTable = table;
		mStream = stream;
		mUserName = username;

			// load these from sharedpreferences
		SharedPreferences prefs = ctx.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		sent_start = prefs.getLong("sent_start", 0);
		sent_end = prefs.getLong("sent_end", 0);
		
		mTimeLastSampleSent = sent_end;
	}
	
	void setSentEnd(long time) {
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("sent_end", time);
		editor.apply();
		sent_end = time;
	}
	void setSentStart(long time) {
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("sent_start", time);
		editor.apply();
		sent_start = time;
	}
	
	public void run() {
		final int SAMPLES = 1000;
		mState = STATE_IDLE;
		mRun = true;
		while(mRun) {
			long now = new Date().getTime();
			Cursor cursor = mTable.getQueryCursorStraight(sent_end, now);
			int n = cursor.getCount();
			Log.d(TAG, "To upload: " + n);
			int i = 0;
			status_toupload = n;
			status_progress = 0;
			int fails = 0;
//			while(i<n)
			
				try {
					while(i<n) {
						mState = STATE_UPLOAD;
						Vector<Sample> samples = getSamples(cursor, SAMPLES);
						upload("GSR", samples);
						Vector<Sample> tags = getTags();
						if (tags.size()>0) {
							Log.d(TAG, "Uploading tags " + tags.size());
							upload("TAG", tags);
						}
						if (samples.size()>0)
							setSentEnd(samples.lastElement().time+1);	// shift it one ms later.
						else break;
						status_progress += samples.size();
						i += samples.size();
					}
					
					fails = 0;
				} catch (IOException e) {
					Log.d(TAG, "Upload connection disconnected... " + e.getMessage());
					fails++;
				}
			
			cursor.close();
			if (!mStream) break;
			mState = STATE_IDLE;
			try {
				Thread.sleep(10*1000);	// wait a minute
			} catch(InterruptedException ie) {
				// 
			}
		}
		mState = STATE_DONE;
		mListener.uploaderStopped("Done");
	}
	
	private long mTimeLastSampleSent = 0;
	private Vector<Sample> getTags() {
		Vector<UserTag> tags = AHState.getInstance().mUserTags.getUserTags(mTimeLastSampleSent, sent_end);
		mTimeLastSampleSent = sent_end;
		return tagsToSamples(tags);
	}
	
	private Vector<Sample> tagsToSamples(Vector<UserTag> tags) {
		Vector<Sample> samples = new Vector<Sample>();
		for (UserTag tag : tags) {
			samples.add(new SampleTag(tag));
		}
		return samples;
	}

	Vector<Sample> getSamples(Cursor cursor, int n) {
		Vector<Sample> samples = new Vector<Sample>();
		int i=0;
		while(i++<n && cursor.moveToNext()) {
			samples.add(new Sample(cursor));
		}
		return samples;
	}

	void upload(String type, Vector<Sample> samples) throws IOException {
		Connection client = Jsoup.connect(API_URL);
		client.data("cmd", "data");
		client.data("type", type);
		client.data("user", mUserName);
		for(Sample s : samples) {
			client.data("samples[]", s.toString());
			Log.d(type, s.toString());
		}
		client.method(Connection.Method.POST);
		Log.d(TAG, "Uploading " + samples.size() + " samples...");
		if (samples.size()>0) {
			Sample s1 = samples.get(0);
			Sample s2 = samples.get(samples.size()-1);
			Log.d(TAG, "Timestamp range: " + s1.time + ", " + s2.time);
		}
		Connection.Response response = client.execute();
		Log.d("database error ",response.body());
	}

	public void start(Listener listener) {
		mListener = listener;
		mRun = true;
		mThread = new Thread(this);
		mThread.start();
	}
	
	public void stop() {
		mRun = false;
		mThread.interrupt();
	}
    public int getMStatus(){
      return mState;    	
    }
	public String getStatusString() {
		return "Uploading. " + status_progress + " / " + status_toupload;
	}
	public int getProgress(){
	    return status_progress/status_toupload;
	}
	
}
