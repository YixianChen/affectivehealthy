/**
 * 
 */
package se.sics.ah3.dti.signal;


public class ValueMapper{

	float minhue,  maxhue;
	
	public ValueMapper( float minhue, float maxhue ) {
		this.minhue = minhue;
		this.maxhue = maxhue;
		
	}

	public float map(float normalized){
		return minhue + (normalized)*(maxhue-minhue);
	}

}