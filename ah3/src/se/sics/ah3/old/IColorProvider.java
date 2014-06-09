package se.sics.ah3.old;

import java.util.GregorianCalendar;


public interface IColorProvider {
	public float[] fillArousalColorBuffer(DataStore dataStore, int dataPos, int length);
	public float[] fillArousalColorBuffer(DataStore dataStore, int length, GregorianCalendar start, GregorianCalendar end, long granularity);
	public float[] fillArousalColorBuffer(DataStore dataStore, int length, GregorianCalendar start, long granularity);
	
}
