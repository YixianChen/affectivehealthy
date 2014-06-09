package se.sics.ah3.old;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


abstract public class BioDataBuffer {
	int numBufferParts = 3;
	static int bioDataComponents = 1;
	static int tracks = 2;//same for all subclasses?
	int bufferLength;
	FloatBuffer[] buffer = null;
	DataStore _dataStore;
	long[] bufferStart;
	protected long granularity = 1000;
	//private Object _bioDataMapper;
	
	public BioDataBuffer(int length,DataStore dataStore)
	{
		_dataStore = dataStore;
		buffer = new FloatBuffer[numBufferParts];
		bufferStart = new long[numBufferParts];
		bufferLength = length;
		/*for(int i = 0; i < numBufferParts; i++)
		{
			fillNewBuffer(i, (i*(length-1)), i*length);
		}*/
	}
	
	//should be overridden
	 protected FloatBuffer fillNewBuffer(int targetIndex, long start, long dataPos) {
		bufferStart[targetIndex] = start;
		buffer[targetIndex] = null;
		//float[] bioData = _bioDataMapper.fillBioDataBuffer(_dataStore, dataPos, bufferLength,field,mapper);
		float[] bioData = new float[bufferLength*bioDataComponents];
		ByteBuffer bdbb = ByteBuffer.allocateDirect(bioData.length * 4);//4 is the size of float in bytes
		bdbb.order(ByteOrder.nativeOrder());
		buffer[targetIndex] = bdbb.asFloatBuffer();
		
		buffer[targetIndex].put(bioData);
		//buffer[targetIndex].position((dataPos-start)*tracks*bioDataComponents);
		return buffer[targetIndex];
	}
	
	public FloatBuffer getBioDataBuffer(int dataPos)
	{
		int start = (int)bufferStart[0];
		int end = start+bufferLength;
		if(dataPos < start)//trying to get data from before buffered history
		{
			shiftBuffers(false);
			return fillNewBuffer(0,start-bufferLength+1, dataPos);
		}
		for(int i = 0; i < numBufferParts; i++)
		{
			start =(int) bufferStart[i];
			end = start+bufferLength;
			if(dataPos >= start && dataPos < end)
			{
				int newpos = (dataPos-start);
				if( newpos < 0)
					newpos = 0;
				if( newpos >= buffer[i].capacity())
					newpos = buffer[i].capacity()-1;
				buffer[i].position(newpos);
				return buffer[i];
			}		
		}
		shiftBuffers(true);
		return fillNewBuffer(numBufferParts-1,start+bufferLength-1,dataPos);//trying to get data from after buffered history
	}
	
	public FloatBuffer getBioDataBuffer(long queryTime)
	{
		long scope = bufferLength * numBufferParts;
		long dataPos = queryTime/granularity;
		if(dataPos > bufferStart[0] - bufferLength && dataPos < bufferStart[numBufferParts-1] + bufferLength + scope )
			return getCloseBioBuffer(queryTime);
		else
		{
			//new far off buffer
			//prev
			for(int i = 0; i < numBufferParts; i++)
			{
				fillNewBuffer(i, (long)(dataPos+ (i-(numBufferParts/2.0))*bufferLength), dataPos);
			}
			return getCloseBioBuffer(queryTime);
		}
	}
	
	protected FloatBuffer getCloseBioBuffer(long queryTime) {
		long start = bufferStart[0];
		long end = start+bufferLength;
		int dataPos = (int) (queryTime/granularity);
		if(dataPos < start)//trying to get data from before buffered history
		{
			shiftBuffers(false);
			return fillNewBuffer(0,start-bufferLength, dataPos);
		}
		for(int i = 0; i < numBufferParts; i++)
		{
			start = bufferStart[i];
			end = start+bufferLength;
			if(dataPos >= start && dataPos < end)
			{
				int newpos = (int)(dataPos-start);
				if( newpos < 0)
					newpos = 0;
				if( newpos >= buffer[i].capacity())
					newpos = buffer[i].capacity()-1;
				buffer[i].position(newpos);
				return buffer[i];
			}		
		}
		shiftBuffers(true);
		return fillNewBuffer(numBufferParts-1,start+bufferLength-1,dataPos);//trying to get data from after buffered history
	
	}
	
	private void shiftBuffers(boolean up) {
		if(up)
		{
			for(int i = 0; i < numBufferParts-1; i++)
			{
				buffer[i]= buffer[i+1];
				bufferStart[i]=bufferStart[i+1];
			}
		}
		else
		{
			for(int i = numBufferParts-1; i > 0; i--)
			{
				buffer[i]= buffer[i-1];
				bufferStart[i]=bufferStart[i-1];
			}
		}
	}
}
