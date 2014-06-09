package se.sics.ah3.old;

import java.util.GregorianCalendar;


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
		// TODO Auto-generated method stub
		return null;
	}

}
