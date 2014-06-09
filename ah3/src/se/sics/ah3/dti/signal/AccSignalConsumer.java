package se.sics.ah3.dti.signal;

import se.sics.ah3.AHState;
import se.sics.ah3.SignalConsumer;
import se.sics.ah3.SignalConsumerListener;
import se.sics.ah3.database.Column;
import se.sics.ah3.dti.DataMessage;

public class AccSignalConsumer implements SignalConsumer{
	private SignalConsumerListener mListener = null;
	private Normalizer mag;
	private Column accColumn;
	//ValueMapper currentMapper;
	//double pmem = 0;
	//float q=.9f;
	boolean first=true;
	private final int[] mInterests;
	
	float[] lastVals; // = new float[3];
	float accVal = 0.0f;

	private AHState mAHStateInstance = null;

	public AccSignalConsumer(Column col, SignalConsumerListener listener) {
		mInterests = new int[]{
				DataMessage.Field.accX.ordinal(),
				DataMessage.Field.accY.ordinal(),
				DataMessage.Field.accZ.ordinal()
		};
		this.accColumn = col;
		//currentMapper=new ValueMapper(0, 1);
		mag = new Normalizer(9.1f,25,.9f);
		mListener = listener;
		mAHStateInstance = AHState.getInstance();
		
	}

	@Override
	public void consumeSignal(long time, int... value) {
		float[] vals = new float[value.length];
		String s="";
		for (int i = 0; i < value.length; i++) {
			vals[i]= value[i]*9.18f/16384; //16384 comes from DTI manual
			s+=vals[i]+" ";
		}
//		Log.v("ACC",s);
		consume(vals,time);
		
	}

	@Override
	public final int[] getInterest() {
		return mInterests; 		
	}

	public void consume(float[] vals, long timestamp){
		if (lastVals==null)
		{
			lastVals = new float[3];
			System.arraycopy(vals, 0, lastVals, 0,3);
		}

		float diff = Math.abs(vals[0]-lastVals[0]) + Math.abs(vals[1]-lastVals[1]) + Math.abs(vals[2]-lastVals[2]); 
		accVal = accVal*0.2f + diff*0.8f;
		accColumn.insert(timestamp,accVal);
		if (mListener!=null) {
			mListener.signalConsumed(timestamp, accVal);
		}
		System.arraycopy(vals, 0, lastVals, 0,3);
		
		// store raw acc data in table
		mAHStateInstance.mAccDatabaseTable.store(timestamp, vals[0], vals[1], vals[2]);		
	}
}
