package se.sics.ah3.model;

import java.util.Vector;

import se.sics.ah3.R;
import se.sics.ah3.graphics.SpiralParser;
import se.sics.ah3.graphics.SpiralParser.SpiralDataVertex;
import se.sics.ah3.interaction.Parameters;
import android.content.Context;
import android.graphics.PointF;

/**
 * Model for the spiral shape
 * @author mareri
 *
 */

public class SpiralFormula {
	
	// this is for the section of the spiral used for the realtime value visualisation
	public final static float REALTIME_SEGMENT_SIZE = 0.1f;
	
	public static float rescaleTToAfterRealtimeSegment(float t) {
		return REALTIME_SEGMENT_SIZE + t * (1.0f - REALTIME_SEGMENT_SIZE);
	}
	
	/**
	 * 
	 */
	static float transformT(float t) {
		return 0.075f + (float)Math.pow(t, 2.0/3.0)*(0.925f-0.075f);
	}
	
	/**
	 * the "main" shape
	 * @param t
	 * @param outCoordinate
	 */
	public static void parametricFormulation(float t, float[] outCoordinate) {
		if (t<0) {
			outCoordinate[0] = outCoordinate[1] = outCoordinate[2] = 0f;
			return;
		}
		//TODO: Border condition
		// fetching preloaded geometry
		if (t>1.0f) t = 1.0f;
		float floatIndex = t * (mVertices.size()-1);
		int index = (int)floatIndex;
		int endIndex = Math.min((index+1), mVertices.size()-1);

		float interpolation = floatIndex - index;
		float it = interpolation;

		float t3 = it*it*it;
		float t2 = it*it;
		
		float x0 = mVertices.get(index).p.x;
		float x1 = mVertices.get(endIndex).p.x;
		float x0p = index>0?mVertices.get(index-1).p.x:x0;
		float x1p = endIndex<mVertices.size()-1?mVertices.get(endIndex+1).p.x:x1;
		float mx0 = (x1 - x0p)/2;
		float mx1 = (x1p - x0)/2;
		
		float x = (2*t3 - 3*t2 + 1)*x0 + (t3 - 2*t2 + it)*mx0 + (-2*t3+3*t2)*x1 + (t3-t2)*mx1;

		float y0 = mVertices.get(index).p.y;
		float y1 = mVertices.get(endIndex).p.y;
		float y0p = index>0?mVertices.get(index-1).p.y:y0;
		float y1p = endIndex<mVertices.size()-1?mVertices.get(endIndex+1).p.y:y1;
		float my0 = (y1 - y0p) / 2;
		float my1 = (y1p - y0) / 2;

		float y = (2*t3 - 3*t2 + 1)*y0 + (t3 - 2*t2 + it)*my0 + (-2*t3+3*t2)*y1 + (t3-t2)*my1;

		outCoordinate[0] = x; //(1f - interpolation) * mVertices.get(index).p.x + interpolation * mVertices.get(endIndex).p.x;
		outCoordinate[1] = y; //(1f - interpolation) * mVertices.get(index).p.y + interpolation * mVertices.get(endIndex).p.y;
		outCoordinate[2] = 0f;
	}
	
	/**
	 * calculate the thickness
	 * @param t
	 * @return
	 */
	public static float getThickness(float t) {
		if (t<0) return 0;
		if (t>1) return 0;

		float floatIndex = t * (mVertices.size()-1);
		int index = (int)floatIndex;
		if (index<0) index = 0;
		if (index>mVertices.size()-1) index = mVertices.size()-1;
		float interpolation = floatIndex - index;
		int endIndex = Math.min((index+1), mVertices.size()-1);
		return (1f - interpolation) * mVertices.get(index).r + interpolation * mVertices.get(endIndex).r;
		
		/*t = transformT(t);
		
		float scale = 1f;
		if(t < THINESS_THRESHOLD) {
			scale = 1f - (THINESS_THRESHOLD - t) /(THINESS_THRESHOLD - THINESS_CLAMP);
			scale = Math.max(0f, Math.min(1f, scale));
		}
		return scale * (float)(Math.sin((1f - t) * Math.PI * 0.5)) * 0.075f;*/
	}
	
	/**
	 * 
	 * @author mareri
	 *
	 */
	public static float[] getNormal(float t) {
		float[] current = new float[3];
		float[] near = new float[3];
		
		parametricFormulation(t, current);
		parametricFormulation(t - 0.0001f, near);
		
		float[] normal = { near[1] - current[1],
						   -near[0] + current[0],
						   0f};
		
		return normal;
	}
	
	public static void getNormal(float t, float[] normal) {
		float[] current = new float[3];
		float[] near = new float[3];
		
		parametricFormulation(t, current);
		parametricFormulation(t - 0.0001f, near);
		
		normal[0] = near[1] - current[1];
		normal[1] = -near[0] + current[0];
	}
	
	public static float[] getTangent(float t) {
		float[] current = new float[3];
		float[] near = new float[3];
		
		parametricFormulation(t, current);
		parametricFormulation(t - 0.0001f, near);
		
		float[] tangent = { near[0] - current[0],
				   near[1] - current[1],
				   near[2] - current[2]};
		
		return tangent;
	}
	
	public static float getT(long time, Parameters parameters) {
		long current = parameters.start - parameters.end;
		long point = time - parameters.end;
		
		float t = (float)(point / (double)(current));
		
		return t;
	}
	
	//
	// precalculated mesh
	//
	private static class SpiralVertex {
		public PointF p = new PointF();
		public float time;	// time = params.start + (1-time)*(p.end-p.start)
		//public float R;
		//public float Theta;
	}
	private static Vector<SpiralDataVertex> mVertices;
	public static void genSpiralMesh(Context context)
	{
		SpiralParser parser = new SpiralParser(R.raw.spiral3); //_new);
		parser.parse(context);
//		mVertices = parser.getParsedData();
		Vector<SpiralDataVertex> parsed = parser.getParsedData();
		mVertices = new Vector<SpiralDataVertex>();
		final int tesselation = 10;
		for (int i=0;i<parsed.size()-1;i++)
		{
			SpiralDataVertex a = parsed.get(i);
			SpiralDataVertex b = parsed.get(i+1);
			for (int j=0;j<tesselation;j++)
				mVertices.add(new SpiralDataVertex(a,b,j/(float)tesselation));
		}
		
		/*long points = 10000;
		mVertices = new Vector<SpiralVertex>();
		float[] point = new float[3];
		for (int i=0;i<points;i++)
		{
			float t = i/(float)points;
			SpiralPoint sp = parametricFormulation(t, point);
			SpiralVertex vertex = new SpiralVertex();
			vertex.time = t;
			vertex.p.x = point[0];
			vertex.p.y = point[1];
			vertex.R = sp.R;
			vertex.Theta = sp.Theta;
			mVertices.add(vertex);
		}*/
	}
	
	public static class SpiralPoint {
		public float R;
		public float Theta;
		public float t;
		public PointF tangent;
		public long time;
		public PointF p;
	}
	
	private static float dist(PointF p1,PointF p2)
	{
		return (float) Math.sqrt((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y));
	}
	private static float len(PointF p)
	{
		return (float) Math.sqrt(p.x*p.x+p.y*p.y);
	}

	public static SpiralPoint fromPoint(PointF p, Parameters params)
	{
		SpiralPoint sp = new SpiralPoint();
		
		SpiralDataVertex v1 = mVertices.get(0);
		SpiralDataVertex v2 = v1;
		float closest_d = Float.MAX_VALUE;
		int closest_segment = 0;
		int n = mVertices.size();
		for (int i=0;i<n-1;i++)
		{
			v1 = v2;
			v2 = mVertices.get(i+1);
			
			float d1 = dist(v1.p,p);
			float d2 = dist(v2.p,p);
			float d = (d1+d2)*0.5f;
			if (closest_d>d)
			{
				closest_d = d;
				closest_segment = i;
			}
		}

		v1 = mVertices.get(closest_segment);
		v2 = mVertices.get(closest_segment+1);
		PointF a = new PointF(v2.p.x-v1.p.x,v2.p.y-v1.p.y);
		a.x/=len(a);
		a.y/=len(a);
		PointF b = new PointF(p.x-v1.p.x,p.y-v1.p.y);
		b.x/=len(b);
		b.y/=len(b);
		float t1t2 = a.x*b.x+a.y*b.y;
		double t = v1.time + (v2.time-v1.time)*t1t2;
		t = (t - SpiralFormula.REALTIME_SEGMENT_SIZE) / (1 - SpiralFormula.REALTIME_SEGMENT_SIZE);
		//sp.R = (v1.R+v2.R)*0.5f; // MAER Ignored
		//sp.Theta = v1.Theta; //(v1.Theta + v2.Theta)*0.5f; //MAER Ingored
		sp.t = (float)t;
//		sp.time = (long)(params.start*sp.t + (1.0-sp.t)*params.end);
		sp.time = (long)(params.start + (1.0-t)*(params.end-params.start));
		sp.p = p;

//		Log.d("Spiral", "Segment " + closest_segment + ": " + v1.time + ", " + v2.time + " = " + t + " time: " + sp.time);

		return sp;
	}
}
