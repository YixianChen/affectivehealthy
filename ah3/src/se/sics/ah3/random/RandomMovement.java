package se.sics.ah3.random;

import java.util.GregorianCalendar;

import android.text.InputFilter.LengthFilter;

import se.sics.ah3.old.DataStore;
import se.sics.ah3.old.IMovementProvider;

public class RandomMovement implements IMovementProvider{

	@Override
	public float[] fillMovementBuffer(DataStore dataStore, int dataPos,
			int length) {
		
		float[] movement = new float[length*1*1];
		

		for(int i = 0; i < movement.length; i ++)
		{
			movement[i] = (float)Math.random();
		}
		
		return movement;
	}

	@Override
	public float[] fillMovementBuffer(DataStore dataStore, int bufferLength,
			GregorianCalendar startTime, long granularity) {	
		return fillMovementBuffer(dataStore,0,bufferLength);
	}

}
