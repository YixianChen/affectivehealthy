package se.sics.ah3.old;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.GregorianCalendar;


import android.graphics.Color;
import android.text.InputFilter.LengthFilter;
import android.util.Log;

public class ColorBuffer {
	int numBufferParts = 3;
	static int colorComponents = 4;
	static int tracks = 2;
	
	int bufferLength;
	public int getBufferLength()
	{
	 return bufferLength;
	}
	
	FloatBuffer[] buffer = null;
	FloatBuffer redBuffer = null;
	FloatBuffer greenBuffer = null;
	long[] bufferStart;
	//long startTime;
	long granularity = 1000;
	float[] colorValues = new float[8];
	DataStore _dataStore;
	IColorProvider _colorProvider;
	
	public ColorBuffer(int length, IColorProvider colorProvider, DataStore dataStore, long startTime, long granularity)
	{
		int offset =(int)(startTime / granularity);
		_dataStore = dataStore;
		_colorProvider = colorProvider;
		buffer = new FloatBuffer[numBufferParts];
		bufferStart = new long[numBufferParts];
		bufferLength = length;
		for(int i = 0; i < numBufferParts; i++)
		{
			fillNewBufferFromEnd(i, offset- (i-numBufferParts)*bufferLength, offset);
		}
	}
	
	private FloatBuffer fillNewBuffer(int targetIndex, long start, long dataPos) {
		bufferStart[targetIndex] = start;
		buffer[targetIndex] = null;
		GregorianCalendar startTime = new GregorianCalendar();
		//GregorianCalendar endTime = new GregorianCalendar();
		startTime.setTimeInMillis(start*granularity);
		//endTime.setTimeInMillis((start+bufferLength)*granularity);
		float[] colors = _colorProvider.fillArousalColorBuffer(_dataStore, bufferLength, startTime, granularity);//fillNewGradientColor(bufferLength, start);
		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		buffer[targetIndex] = cbb.asFloatBuffer();
		if(colors == null || cbb == null || buffer[targetIndex] == null)
		{
			Log.d("AHERROR", "Failed to fill new buffer:" + targetIndex + " from : " + start + " at:" + dataPos);
		}
		//colorBuffer = cbb.asShortBuffer();
		buffer[targetIndex].put(colors);
		buffer[targetIndex].position(0);
		//buffer[targetIndex].position((dataPos-start)*tracks*4);//calculation seems to be wrong will crash after a while since start and dataposition drift appart
		return buffer[targetIndex];
	}
	
	private FloatBuffer fillNewBufferFromEnd(int targetIndex, long end, long dataPos) {
		long start = end - bufferLength;
		//startTime.setTimeInMillis(end.getTimeInMillis()-(bufferLength*granularity));
		bufferStart[targetIndex] = start;
		buffer[targetIndex] = null;
		GregorianCalendar startTime = new GregorianCalendar();
		//GregorianCalendar endTime = new GregorianCalendar();
		startTime.setTimeInMillis(start*granularity);
		//endTime.setTimeInMillis((start+bufferLength)*granularity);
		float[] colors = _colorProvider.fillArousalColorBuffer(_dataStore, bufferLength, startTime, granularity);//fillNewGradientColor(bufferLength, start);
		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		buffer[targetIndex] = cbb.asFloatBuffer();
		if(colors == null || cbb == null || buffer[targetIndex] == null)
		{
			Log.d("AHERROR", "Failed to fill new buffer:" + targetIndex + " from : " + start + " at:" + dataPos);
		}
		//colorBuffer = cbb.asShortBuffer();
		buffer[targetIndex].put(colors);
		buffer[targetIndex].position(0);
		//buffer[targetIndex].position((dataPos-start)*tracks*4);//calculation seems to be wrong will crash after a while since start and dataposition drift appart
		return buffer[targetIndex];
	}

	public void setColorBuffer(long setTime, int color)
	{
		long start = bufferStart[0];
		long end = start+bufferLength;
		long dataPos = setTime/granularity;
		FloatBuffer buf = null;
		if(setTime < start)//trying to get data from before buffered history
		{
			shiftBuffers(false);
			buf = fillNewBuffer(0,start-bufferLength, dataPos);
		}
		else
		{
			for(int i = 0; i < numBufferParts; i++)
			{
				start = bufferStart[i];
				end = start+bufferLength;
				if(dataPos >= start && dataPos < end)
				{
					buffer[i].position((int)(dataPos-start)*tracks*4);
					buf = buffer[i];
					break;
				}		
			}
		}
		if(buf == null)
		{
			shiftBuffers(true);
			start = start+bufferLength;
			buf = fillNewBuffer(numBufferParts-1,start,dataPos);//trying to get data from after buffered history
		}
		setColorInBuffer(setTime, start, buf, color);
	}
	
	public void setColorInBuffer(long setTime, long start, FloatBuffer buf, int color)
	{
		/*long offset = setTime - start;
		if(offset > 0 && offset < buf.capacity())
		{
			buf[offset*8 + 0] = (float) (Color.red(color)/255.0);
			buf[offset*8 + 1] = (float) (Color.green(color)/255.0);
			buf[offset*8 + 2] = (float) (Color.blue(color)/255.0);
			buf[offset*8 + 3] = 1.0f;
			buf[offset*8 + 4] = (float) (Color.red(color)/255.0);
			buf[offset*8 + 5] = (float) (Color.green(color)/255.0);
			buf[offset*8 + 6] = (float) (Color.blue(color)/255.0);
			buf[offset*8 + 7] = 1.0f;
		}*/
	}
	
	public FloatBuffer getColorBuffer(long queryTime)
	{
		long scope = bufferLength * numBufferParts;
		long dataPos = queryTime/granularity;
		if(dataPos > bufferStart[numBufferParts-1] - bufferLength && dataPos < bufferStart[0] + bufferLength + scope )
			return getCloseColorBuffer(queryTime);
		else
		{
			//new far off buffer
			//prev
			
			
			for(int i = 0; i < numBufferParts; i++)
			{
				//fillNewBuffer(i, dataPos+ (i-(numBufferParts/2))*bufferLength, dataPos);
				fillNewBufferFromEnd(i, dataPos- (i-(numBufferParts/2))*bufferLength + bufferLength, dataPos);
			}
			return getCloseColorBuffer(queryTime);
		}
	}
	
	public FloatBuffer getCloseColorBuffer(long queryTime)
	{
		long start = bufferStart[numBufferParts-1];//early time index
		long end = start+bufferLength;//late time index
		long dataPos = queryTime/granularity;
		if(dataPos < start)//trying to get data from before buffered history
		{
			shiftBuffers(false);
			//return fillNewBuffer(0,start-bufferLength, dataPos);
			return fillNewBufferFromEnd(numBufferParts-1,start, dataPos);
		}
		for(int i = numBufferParts-1; i >= 0; i--)
		{
			start = bufferStart[i];
			end = start+bufferLength;
			if(dataPos >= start && dataPos <= end)// include end or start or both?
			{
				long pos = end - dataPos; //should be latest = 0; earliest = 181
				buffer[i].position((int)(pos)*tracks*4);//4 for rgba
				return buffer[i];
			}		
		}
		shiftBuffers(true);
		//return fillNewBuffer(numBufferParts-1,start+bufferLength,dataPos);//trying to get data from after buffered history
		return fillNewBufferFromEnd(0,start+2*bufferLength,dataPos);//trying to get data from after buffered history
	}
	
	
	private void shiftBuffers(boolean up) {
		if(!up)
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

	
	public static void setColor(float[] target, int position,int c)
	{
		int colorComponents = 4;
		target[position*2*colorComponents] =  (float) Color.red(c)/255;
		target[position*2*colorComponents+1] =  (float) Color.green(c)/255;
		target[position*2*colorComponents+2] =  (float) Color.blue(c)/255;
		target[position*2*colorComponents+3] =  (float) Color.alpha(c)/255;
		target[position*2*colorComponents+4] =  (float) Color.red(c)/255;
		target[position*2*colorComponents+5] =  (float) Color.green(c)/255;
		target[position*2*colorComponents+6] =  (float) Color.blue(c)/255;
		target[position*2*colorComponents+7] =  (float) Color.alpha(c)/255;
		target[position*2*colorComponents] =  (float) Color.red(c)/255;
	}
	
	public void update(int pos, long time, int color)
	{
		FloatBuffer buf = getColorBuffer(time);
		int oldPos = buf.position();
		if(pos > 0 && pos < buf.limit()-8)
		{
			buf.position(pos*8);
		//set position
		colorValues[0]=(float) (Color.red(color)/255.0);
		colorValues[1]=(float) (Color.green(color)/255.0);
		colorValues[2]=(float) (Color.blue(color)/255.0);
		colorValues[3]=1.0f;
		colorValues[4]=(float) (Color.red(color)/255.0);
		colorValues[5]=(float) (Color.green(color)/255.0);
		colorValues[6]=(float) (Color.blue(color)/255.0);
		colorValues[7]=1.0f;
		//put data
		//if(buf.position() > 0 && buf.position() < buf.limit()-8)
			buf.put(colorValues);
			buf.position(oldPos);
		}
	}

	private void time2spiralPos(long start, long time) {
		//long dif = 
		
		
	}
	
	/*public static void setColor(float[] target,long start, GregorianCalendar time, long granularity, int c)
	{
		long endTimeInMillis = (start+target.length)*granularity;
		int position;
	}*/
}
