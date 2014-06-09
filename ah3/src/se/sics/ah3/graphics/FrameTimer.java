package se.sics.ah3.graphics;

import android.util.Log;

/**
 * Simple timer class to debug print and keep track of frame time
 * @author mareri
 *
 */

public class FrameTimer {
	private int mFrames;
	private long mPreviousSample, mPreviousFrame;
	private final static int SAMPLE_INTERVAL = 100;
	
	public FrameTimer() {
		mFrames = 0;
		mPreviousSample = System.nanoTime();
	}
	
	/**
	 * Call once and only once per frame
	 */
	public int tick() {
		mFrames++;
		if(mFrames % SAMPLE_INTERVAL == 0) {
			long current = System.nanoTime();
			double seconds = (current - mPreviousSample) / 1.0e9;
			double msPerFrame = (1000 * seconds) / mFrames;
			
			//Log.i(Core.TAG, "mspf: " + msPerFrame);
			
			mFrames = 0;
			mPreviousSample = current;
		}
		
		long current = System.nanoTime();
		int frameTime = (int)(current - mPreviousFrame);
		mPreviousFrame = current;
		return frameTime;
	}
}
