/**
 * 
 */
package se.sics.ah3.dti.signal;

import android.util.FloatMath;

public class Normalizer{
	public float max;
	float avg=9.8f;
	float q;
	long lastTime;
	float normalized;
	float mag;
	float minmag, maxmag;
	public Normalizer(float offset, float maxmag) {
		this(offset, maxmag, .97f);
	}
	
	public Normalizer(float offset, float maxmag, float q) {
		this.minmag = offset;
		this.maxmag = maxmag;		
		this.q=q;
	}
	public Normalizer(){
		this(9.8f,24);
	}
	
	public float getNormalized(){
		return normalized;
	}
	public void update(float[] v) {
	
		mag =FloatMath.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
		
		avg=q*avg+(1-q)*mag;
		if(avg>max) 
			max=avg;
	
	
		
		long now = System.currentTimeMillis();
		if(now-lastTime >1000){
			normalized = Math.max(0,Math.min(maxmag-minmag,max-minmag))/(maxmag-minmag);
			
			lastTime=now;		
			max=0;
		}			

	}
	@Override
	public String toString() {
		return "mag: "+mag+", max: "+max+" avg: "+avg;
	}
}