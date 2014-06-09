package se.sics.ah3.old;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.GregorianCalendar;


import android.util.Log;

public class MovementBuffer extends BioDataBuffer{

	IMovementProvider _bioDataProvider;
	public MovementBuffer(int length, IMovementProvider dataMapper, DataStore dataStore) {
		super(length, dataStore);
		_bioDataProvider = dataMapper;
		for(int i = 0; i < numBufferParts; i++)
		{
			fillNewBuffer(i, (i*(length-1)), i*length);
		}
	}
	
	@Override
	protected FloatBuffer fillNewBuffer(int targetIndex, long start, long dataPos) {
		bufferStart[targetIndex] = start;
		buffer[targetIndex] = null;
		GregorianCalendar startTime = new GregorianCalendar();
		//GregorianCalendar endTime = new GregorianCalendar();
		startTime.setTimeInMillis(start*granularity);
		//endTime.setTimeInMillis((start+bufferLength)*granularity);
		float[] movement = _bioDataProvider.fillMovementBuffer(_dataStore, bufferLength, startTime, granularity);//fillNewGradientColor(bufferLength, start);
		ByteBuffer bdbb = ByteBuffer.allocateDirect(movement.length * 4);
		bdbb.order(ByteOrder.nativeOrder());
		buffer[targetIndex] = bdbb.asFloatBuffer();
		//colorBuffer = cbb.asShortBuffer();
		buffer[targetIndex].put(movement);
		buffer[targetIndex].position(0);
		//buffer[targetIndex].position((dataPos-start)*tracks*4);//calculation seems to be wrong whil crash after a while since start and dataposition drift appart
		return buffer[targetIndex];
	}
	
}
	
