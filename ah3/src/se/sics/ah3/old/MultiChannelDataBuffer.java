package se.sics.ah3.old;

import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;


public class MultiChannelDataBuffer {
	private BioDataBuffer[] buffers;
	public ConcurrentHashMap<String, BioDataBuffer> mBuffers;
	DataStore mDataStore;
	IBioDataProvider mProvider;
	
	public boolean AddChannel(String name, BioDataBuffer buffer)
	{
		if(mBuffers == null)
		{
			mBuffers = new ConcurrentHashMap<String, BioDataBuffer>();
		}
		if(mBuffers.contains(name))
			return false;
		mBuffers.put(name, buffer);
		return true;
	}
	
	public boolean FillBuffers(GregorianCalendar start, GregorianCalendar end, String channel1, String channel2, String channel3)
	{
		//mProvider.fillBioBuffer(mDataStore, start, end, mBuffers.get(channel1), mBuffers.get(channel1), mBuffers.get(channel1));
		return true;
	}
	
	
}
