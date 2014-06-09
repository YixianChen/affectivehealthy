package se.sics.ah3.dti.signal;

import se.sics.ah3.SignalConsumer;
import se.sics.ah3.SignalConsumerListener;
import se.sics.ah3.database.Column;
import se.sics.ah3.dti.DataMessage;
import android.util.Log;

public class GsrSignalConsumer implements SignalConsumer{

	private SignalConsumerListener mListener = null;
	private Column column;
	private int[] mInterests;

	public GsrSignalConsumer(Column column, SignalConsumerListener listener) {
		this.column = column;
		mListener = listener;

		mInterests = new int[]{DataMessage.Field.skinConductance.ordinal()};
	}

	@Override
	public void consumeSignal(long time, int... value) {
		float gsr = value[0];
//		Log.v("Bluetooth","GSR:"+gsr);
		column.insert(time, gsr);
		if (mListener!=null) {
			mListener.signalConsumed(time, gsr);
		}
	}

	@Override
	public final int[] getInterest() {
		return mInterests;
	}
}
