package se.sics.ah3.sensors;

import se.sics.ah3.database.Column;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Accelerometer implements SensorEventListener {
	final private String TAG = "ACCELEROMETER";
	private Context mContext;
	private SensorManager mSensorManager;
	private Column mColumn;
	private long mSamplingRate;
	private boolean mRunning;

	// Sampling rate is in milliseconds. E.g. 500 means every 500 ms, (i.e. 2 times per second)
	public Accelerometer(Context context, Column column, long samplingRate) {
		mContext = context;
		mSensorManager = (SensorManager)mContext.getSystemService(Activity.SENSOR_SERVICE);
		mColumn = column;
		mSamplingRate = samplingRate;
	}
	
	public void start() {
		mRunning = true;
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void stop() {
		if (mRunning)
			mSensorManager.unregisterListener(this);
		mRunning = false;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	private float[] mLastSensorValues;
	private float mMovement = 0;
	private long mLastWriteTime = 0;

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
		{
			if (mLastSensorValues==null)	// first time we just copy the values into array
			{
				mLastSensorValues = new float[3];
				System.arraycopy(event.values, 0, mLastSensorValues, 0, 3);
				return;
			}
			float movement = 
					Math.abs(event.values[0] - mLastSensorValues[0]) +
					Math.abs(event.values[1] - mLastSensorValues[1]) + 
					Math.abs(event.values[2] - mLastSensorValues[2]);
			System.arraycopy(event.values, 0, mLastSensorValues, 0, 3);
			
			mMovement = movement*0.8f + mMovement*0.2f;	// evening out the movements
			long milliseconds = event.timestamp / 1000000; // System.currentTimeMillis(); //

			if (milliseconds - mLastWriteTime>mSamplingRate)
			{
				mLastWriteTime = milliseconds;
				mColumn.insert(System.currentTimeMillis(), mMovement);
//				Log.d(TAG, "Writing movement data: " + mMovement + " :: " + milliseconds);
			}
		}
	}

}
