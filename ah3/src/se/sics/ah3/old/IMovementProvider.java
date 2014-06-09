package se.sics.ah3.old;

import java.util.GregorianCalendar;


public interface IMovementProvider {
	public float[] fillMovementBuffer(DataStore dataStore, int dataPos, int length);

	public float[] fillMovementBuffer(DataStore dataStore, int bufferLength,
			GregorianCalendar startTime, long granularity);
}
