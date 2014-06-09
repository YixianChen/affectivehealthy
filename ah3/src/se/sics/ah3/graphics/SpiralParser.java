package se.sics.ah3.graphics;

import java.util.Vector;

import android.content.Context;
import android.graphics.PointF;

/**
 * Parser for the spiral format
 * Format is <x, y, radius>
 * @author mareri
 *
 */

public class SpiralParser {
	private int mId;
	Vector<SpiralDataVertex> parsedData;
	float mScale;
	
	public Vector<SpiralDataVertex> getParsedData() {
		return parsedData;
	}

	public int getNumberOfElements() {
		return parsedData.size();
	}

	public SpiralParser(int idToFile) {
		mId = idToFile;
		parsedData = new Vector<SpiralDataVertex>();
		mScale = 1.0f; //2.2f;
	}
	
	public void parse(Context context) {
		String output = CoreUtil.parseRawText(context, mId);
		String[] stringFragments = output.split("\\s+");
		//parsedData = new float[stringFragments.length];
		//int counter = 0;
		//for(String fragment : stringFragments) {
			//parsedData[counter] = Float.parseFloat(fragment);
			//counter++;
		//}

		/*		float[] centerfix = new float[]{
		0.18958333333333f,
		0.42916666666667f + 0.01f,
		0
};*/
		float[] centerfix = new float[]{
				0,
				0,
				0
		};
		// load the data
		float[] points = new float[stringFragments.length];
		for (int i=0;i<stringFragments.length;i+=3) {
			points[i+0] = Float.parseFloat(stringFragments[i+0])+centerfix[0];
			points[i+1] = Float.parseFloat(stringFragments[i+1])+centerfix[1];
			points[i+2] = Float.parseFloat(stringFragments[i + 2]);
		}
		
		int numPoints = stringFragments.length/3;
		float[] smoothpoints = new float[stringFragments.length];
		// smooth the data
		for (int i=0;i<numPoints;i++) {
			float x=0, y=0;
			float r = 0;
			for (int j=-4;j<5;j++) {
				int idx = Math.max(0, Math.min(numPoints-1, i+j));
				x += points[idx*3+0];
				y += points[idx*3+1];
				r += points[idx*3+2];
			}
			x/=10f; y/=10f; r/=10f;
			smoothpoints[i*3+0] = x*2;//*1.2f;
			smoothpoints[i*3+1] = y*2*1.6f;//*1.2f;
			smoothpoints[i*3+2] = r*2;
		}
		
		// save the data
		float t;
		for(int i = 0; i < stringFragments.length; i+=3) {
			t = i / (float)(stringFragments.length - 1);
			parsedData.add(new SpiralDataVertex(mScale,
//					Float.parseFloat(stringFragments[i])+0.18958333333333f,
//					Float.parseFloat(stringFragments[i + 1])+0.42916666666667f + 0.01f,
//					Float.parseFloat(stringFragments[i + 2]),
//					points[i+0],
//					points[i+1],
//					points[i+2],
					smoothpoints[i+0],
					smoothpoints[i+1],
					smoothpoints[i+2],
					t));
		}
	}
	
	public static class SpiralDataVertex {
		public SpiralDataVertex(float scale, float x, float y, float r, float t) {
			p = new PointF();
			p.x = scale * x;
			p.y = scale * y;
			this.r = scale * r;
			this.time = t;
		}
		public SpiralDataVertex(SpiralDataVertex a, SpiralDataVertex b, float s) {
			// creates a vertex that's basically a*(1-s) + b*s
			float s2 = 1.0f-s;
			p = new PointF();
			p.x = a.p.x*s2+b.p.x*s;
			p.y = a.p.y*s2+b.p.y*s;
			r = a.r*s2+b.r*s;
			time = a.time*s2+b.time*s;
		}
		public PointF p;
		public float r;
		public float time;
	}
}
