package se.sics.ah3.sensors;

import se.sics.ah3.AHState;
import se.sics.ah3.database.Column;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class Microphone {
	private final String TAG = "MICROPHONE";

	private Context mContext;
	private Column mColumn;
	private long mSamplingRate;
	
	private boolean mRunning = false;

	private int mAudioSampleRate;
	private int mBufferSize;
//	private int mNotificationInterval;
	
	private short[] mBuffer;

//	private Runner mRunner;
	
	private MediaRecorder mMediaRecorder;

	public Microphone(Context context, Column column, long samplingRate) {
		mContext = context;
		mColumn = column;
		mSamplingRate = samplingRate;

/*		mAudioSampleRate = 22050;
		mBufferSize = AudioRecord.getMinBufferSize(mAudioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		// make buffer at least one second
		if (mBufferSize<mAudioSampleRate)
			mBufferSize = mAudioSampleRate;
		mBuffer = new short[mBufferSize];*/
	}

/*	private class Runner extends Thread {
		public void run() {
			AudioRecord audioRecorder;
			long volume = 0;
			int bytestoread = mAudioSampleRate / 2;
			audioRecorder = new AudioRecord(AudioSource.MIC, mAudioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
			if (audioRecorder.getState()==AudioRecord.STATE_INITIALIZED)
			{
				try {
					audioRecorder.startRecording();
					int pos = 0;
					while(mRunning)
					{
						int n = audioRecorder.read(mBuffer, pos, bytestoread);
	
						volume = 0;
						for (int i=0;i<n;i++)
						{
							volume += mBuffer[pos+i]; //Math.abs(mBuffer[pos+i]);
						}
						float v = volume /= (double)(n*16384);
						long time = System.currentTimeMillis();
						mColumn.insert(time, v*16);
						Log.d(TAG, "MIC volume: " + v + "(" + volume + ") time: " + time + " n: " + n);
	
						pos += n;
						if (pos>=mBufferSize-bytestoread-1)
						{
							pos = 0;
						}
					}
				}
				catch(Exception e) {
					Log.d(TAG, "Microphone error: " + e.getMessage());
				}
				audioRecorder.stop();
				audioRecorder.release();
			}
			Log.d(TAG, "Done");
			this.notify();
		}
	}*/

	private Thread mThread = null;

	public void start() {
/*		mRunning = true;
		mRunner = new Runner();
		mRunner.start();*/
		mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile("/dev/null"); 
        try {
        	mMediaRecorder.prepare();
        } catch (Exception e) {}
        mMediaRecorder.start();
        
        mRunning = true;
        mThread = new Thread(new Runnable() {public void run() {
        	AHState ah = AHState.getInstance();
        	while(mRunning)
        	{
        		float value = 0;
        		try {
        			Thread.sleep(mSamplingRate);
        			int volume = mMediaRecorder.getMaxAmplitude();
        			float v = 40000*volume/32768.0f; //volume/(32768.0f/300.0f); //(volume / 32768.0f)*2f-1f;
        			value = v*0.8f + value*0.2f;
        			long time = System.currentTimeMillis();
        			mColumn.insert(time, value);
        			ah.mRTgsr = value;
        			ah.mRTtime = time;
//        			Log.d(TAG, "Volume: " + volume + " value: " + value + " time: " + time);
        		} catch (InterruptedException ie)
        		{
        			//
        		}
        	}
    		mMediaRecorder.stop();
    		mMediaRecorder.release();
        };});
        mThread.start();
	}

	public void stop() {
/*		mRunning = false;
		try {
			mRunner.wait();
		} catch (InterruptedException ie) {}*/

		mRunning = false;
		if (mThread!=null)
		{
			mThread.interrupt();
			mThread = null;
		}
	}
}
