package se.sics.ah3.old;

import java.util.GregorianCalendar;

import android.os.Handler;

public class DataEngine {
	private History mHistory = null;
	private IDataSource mSource = null;
    private SignalProcessor mArousalProcessor = null;
	private SignalProcessor mMovementProcessor = null;
	private SignalProcessor mPulseProcessor = null;
	private DataStore mDataStore = null;
//	private Realtime mRealtime = null;
	private GregorianCalendar mDate = null;
	
//	public void setRealtime(Realtime realtime)
//	{
//		mRealtime = realtime;
//	}
	
	public DataEngine(History history,
			IDataSource dataSource, 
			AccelProcessor movementProcessor,
			GSRProcessor arousalProcessor, 
			EcgProcessor pulseProcessor,
			DataStore dataStore)
	{
		mHistory = history;
		mSource = dataSource;
		mArousalProcessor = arousalProcessor;
		mMovementProcessor = movementProcessor;
		mPulseProcessor = pulseProcessor;
		mDataStore = dataStore;
		mDate = new GregorianCalendar();
		mDate.set(GregorianCalendar.MILLISECOND, 0);
	}
	Handler handler = new Handler();
	boolean shouldStop = false;
	long timeInMilliSec = 1000;
	private Runnable onEverySecond = new Runnable() 
	{	
		public void run() 
	    {
	        short arousal =(short)mSource.processArousal(mArousalProcessor);
	        short movement = (short)mSource.processMovement(mMovementProcessor);
	    	short pulse = 0;//mSource.processPulse(mPulseProcessor);
	        mDate.add(GregorianCalendar.SECOND, 1);
	    	mHistory.setCreateValues(mDate, arousal, movement, pulse);
	    	mHistory.saveToDb(mDataStore);
//	    	if(mRealtime != null)
//	    	{
//	    		mRealtime.mArousal = arousal;
//	    		mRealtime.mMovement = movement;
//	    	}
	        if(!shouldStop)
	    		handler.postDelayed(onEverySecond, 1000);
	    }
	};
	
	public void start()
	{
		shouldStop = false;
		handler.post(onEverySecond);
	}
	
	public void stop ()
	{
		shouldStop = true;
	}
	
	public void setTimeInterval(long intervalInMilliSec)
	{
		timeInMilliSec = intervalInMilliSec;
	}

}
