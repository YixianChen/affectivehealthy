/**
 * 
 */
package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.Color;

import se.sics.ah3.InterpretationBuffer;

public class GsrBuffer implements InterpretationBuffer
{
	private FloatBuffer b;
	//private ColorMapper _cm;
	float inmax=400, inmin=100, outmax=1, outmin=0;
			
	public GsrBuffer(float inmin, float inmax, float outmin, float outmax) {
		super();
		this.inmax = inmax;
		this.inmin = inmin;
		this.outmax = outmax;
		this.outmin = outmin;
	}
	
	public GsrBuffer()
	{
		super();
	}
	public void init(int length){
		
		ByteBuffer cbb = ByteBuffer.allocateDirect(length * 4);
		cbb.order(ByteOrder.nativeOrder());
		b = cbb.asFloatBuffer();
		System.out.println("b: "+b.capacity());
		
	}
	public  FloatBuffer getBuffer(){
		return b;
	}
	
	private final float[] vals = new float[1]; 
	public void putNaNColor(){
		//System.out.println("b: "+b.capacity()+" "+b.position()+" "+b.limit());
		//System.out.println(b.remaining());
		vals[0]=0;
	} 
	
	public void interpret( float v) {
		//map val to 4095
		/*int raw = (int)v*16;
		if(v == Float.NaN)
			raw = Error.NO_DATA;
		updateVals(raw);*/
		
		//updateVals(v);
		if (Float.isNaN(v)) {
			vals[0] = Float.NaN;
		} else {
			vals[0] = v; //(v-300.0f)/300.0f;// 16f * v / 256f;
		}
		b.put(vals);
	}
	
	public void setPosition(int pos, int lim){
		//System.out.println("pos lim: "+pos+" "+lim);
		b.clear();
		b.position(pos);
		b.limit(lim);
		
	}
	
	public void clear(){
		b.clear();
	}
}