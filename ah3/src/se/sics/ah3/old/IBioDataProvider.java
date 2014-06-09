package se.sics.ah3.old;

import java.util.GregorianCalendar;


public interface IBioDataProvider {
	public boolean fillBioBuffer(DataStore dataStore,
			GregorianCalendar start, GregorianCalendar end,
			float[] arousalBuffer, float[] movementBuffer, float[] pulseBuffer);
}
