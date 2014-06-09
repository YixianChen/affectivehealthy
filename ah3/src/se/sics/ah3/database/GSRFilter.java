package se.sics.ah3.database;

import se.sics.ah3.settings.AHSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class GSRFilter {
	private final static String PREF_NAME_MAX = "FILTER_MAX";
	private final static String PREF_NAME_MIN = "FILTER_MIN";

	private Context mContext;

	private float mMin, mMax;

	private float mMaxsmooth = 0.99f;
	private float mMinsmooth = 0.99999f;
	private float mDynamicReduction = 0.995f;

	private float[] mBufSorted = new float[7];
	private float[] mBuf = new float[7];
	private int mBufIndex=0;

	public GSRFilter(Context context) {
		mContext = context;
		
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		mMin = prefs.getFloat(PREF_NAME_MIN, 1.0f);
		mMax = prefs.getFloat(PREF_NAME_MAX, 0.0f);
		
		for (int i=0;i<7;i++) {
			mBuf[i] = 0;
		}
	}

	public float filter(float val) {
		// median filter
		mBuf[mBufIndex] = val;
		System.arraycopy(mBuf, 0, mBufSorted, 0, mBuf.length);
		for (int i=0;i<6;i++) {
			for (int j=i+1;j<7;j++) {
				if (mBufSorted[i]>mBufSorted[j]) {
					float t = mBufSorted[i];
					mBufSorted[i] = mBufSorted[j];
					mBufSorted[j] = t;
				}
			}
		}
		val = mBufSorted[3];
		mBufIndex = (mBufIndex+1) % mBuf.length;

		float xv = mMax;
		float nv = mMin;

		xv = val*(1-mMaxsmooth)+xv*mMaxsmooth;
		if (val>xv) xv = val;
		nv = val*(1-mMinsmooth)+nv*mMinsmooth;
		if (val<nv) nv = val;

		mMin = nv;
		mMax = xv;

		float valNoBase = val - mMin;
		float gain = 1 / ((1-mDynamicReduction) + mMax * mDynamicReduction);
		
		saveAttribs();

		return valNoBase * gain;
	}
	
	private void saveAttribs() {
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME,0);
		Editor edit = prefs.edit();
		edit.putFloat(PREF_NAME_MAX, mMax);
		edit.putFloat(PREF_NAME_MIN, mMin);
		edit.commit();
	}
}
