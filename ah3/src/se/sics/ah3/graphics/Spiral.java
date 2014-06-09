package se.sics.ah3.graphics;

import static android.opengl.GLES10.GL_LINEAR;
import static android.opengl.GLES10.GL_NEAREST;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Iterator;

import se.sics.ah3.AHState;
import se.sics.ah3.DataProxy;
import se.sics.ah3.R;
import se.sics.ah3.database.Column.TimeData;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.model.SpiralFormula;
import android.content.Context;
import android.opengl.GLES20;

/**
 * New spiral shape
 * 
 * @version 0.1
 * @author mareri
 *
 */

public class Spiral extends Mesh20 {
	private int numberOfVertices = 1000;
	private FloatBuffer mArousalBuffer;
	private FloatBuffer mColorBuffer;
	private FloatBuffer mOpacityBuffer;
	private boolean mDrawRealtimeSegment = true;

	private ByteBuffer mRawVertexBuffer = null;
	private ByteBuffer mRawArousalBuffer = null;
	private ByteBuffer mRawOpacityBuffer = null;
	private ByteBuffer mRawColorBuffer = null;

	private DataProxy mDataProxy;
	private long mLastStart=0, mLastEnd=0;

	// test geometry
	 int mTex;
	 int mTexSampler;
	 int mModelViewProjectionLoc;
	 int mModelViewLoc;
	 int mSpiralShader;
	 boolean mShaderAndTextureLoaded = false;
	
	int[] vboId;
	//
	private Context mContext = null;
	
	private ViewPort mViewPort;
	
	public Spiral(ViewPort viewPort, String name, Context context) {
		super(name);
		mContext = context;
		mViewPort = viewPort;
	}
	
	public void setViewPort(ViewPort viewPort) {
		mViewPort = viewPort;
	}
	
	public void setDrawRealtimeSegment(boolean drawRealtimeSegment) {
		mDrawRealtimeSegment = drawRealtimeSegment;
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		if(!mVisible || (pick && !mPickable)) {
			return;
		}

		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

		GLES20.glBlendFunc(GLES20. GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		
		GLES20.glUseProgram(mShader);
        GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);

        GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(),0);

        GLES20.glUniform1i(mTexSampler, 0);
        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTex);

        // update buffers
		updateData();

        // bind buffers
		// vertex
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[0]);
        GLES20.glEnableVertexAttribArray(Core.VERTEX); 
        Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
        // arousal
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
        GLES20.glEnableVertexAttribArray(1);
        Core.glVertexAttribPointer(1, 1, GLES20.GL_FLOAT, false, 0, 0);
        // opacity
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[2]);
        GLES20.glEnableVertexAttribArray(2);
        Core.glVertexAttribPointer(2, 1, GLES20.GL_FLOAT, false, 0, 0);
        // color
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[3]);
        GLES20.glEnableVertexAttribArray(3);
        Core.glVertexAttribPointer(3, 3, GLES20.GL_FLOAT, false, 0, 0);

//        GLES20.glDrawArrays(GLES20.GL_LINES, 0, numberOfVertices);
        if (numberOfVertices>0)
        	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, numberOfVertices);
        GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
	}

	public static float ifNaN(float value)
	{
		return Float.isNaN(value)?-0.9f:value;
	}
	
	public static float gsrToIndex(float gsr) {
//		float value = gsr / 700.0f;
//    	value = CoreUtil.clamp(value, 0f, 1.0f);
//		return value;
		if (gsr<500) return 0.2f*gsr/500.0f;
		if (gsr<3000) return 0.2f + (gsr-500)*0.2f/2500f;
		if (gsr<20000) return 0.4f + (gsr-3000)*0.2f/17000f;
		if (gsr<70000) return 0.6f + (gsr-20000)*0.2f/50000f;
		return CoreUtil.clamp(0.8f+(gsr-70000)/100000.0f,0f,1.0f);
	}

	public static void gsrToColor(float gsr, float[] color, int index) {
		final int[] gsrValues = new int[] {
//				0, 5, 6, 22, 35, 40, 45, 55, 65, 85, 120, 180, 500, 1000, 3000, 20000, 40000, 1000000
//				0, 4000, 32000, 100000
//				0, 3, 12, 20, 250, 500, 800, 1000, 2500, 5000, 9000, 15000, 20000, 28000, 30000, 40000, 65535, 1000000
				0, 3, 25, 150, 800, 1500, 2700, 4000, 5500, 7000, 9000, 15000, 20000, 28000, 30000, 40000, 65535, 1000000
		};
		final float[] gsrColors = new float[] {
//				0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,
//				1,0,0,
//				1,1,0,
//				0,1,0,
//				0.9568627450980392f,0.5529411764705883f,0.18823529411764706f,
////				0.38823529411764707f,0.4980392156862745f,0.17647058823529413f,
////				0.48627450980392156f,0.49019607843137253f,0.1843137254901961f,
//				0.5019607843137255f,0.3215686274509804f,0.2901960784313726f,
////				0.5490196078431373f,0.23921568627450981f,0.40784313725490196f,
//				0.043137254901960784f,0.7607843137254902f,0.5686274509803921f
//
////				0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,0f,0.23921568627450981f,0.4470588235294118f,0.2235294117647059f,0.3568627450980392f,0.5529411764705883f,0.18823529411764706f,0.38823529411764707f,0.4980392156862745f,0.17647058823529413f,0.48627450980392156f,0.49019607843137253f,0.1843137254901961f,0.5019607843137255f,0.3215686274509804f,0.2901960784313726f,0.5490196078431373f,0.23921568627450981f,0.40784313725490196f,0.5764705882352941f,0.2196078431372549f,0.47058823529411764f,0.7058823529411765f,0.23529411764705882f,0.6588235294117647f,0.7607843137254902f,0.17647058823529413f,0.8352941176470589f,0.7568627450980392f,0.043137254901960784f,0.7607843137254902f,0.5686274509803921f,0.23137254901960785f,0.7176470588235294f,0.403921568627451f,0.21176470588235294f,0.6823529411764706f,0.3137254901960784f,0.21568627450980393f,0.6078431372549019f,0.2f,0.17254901960784313f,0.4392156862745098f,0.16862745098039217f,0.19607843137254902f,0.4392156862745098f,0.16862745098039217f,0.19607843137254902f

//				0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,0f,0.23921568627450981f,0.4470588235294118f,0.2235294117647059f,0.3568627450980392f,0.5529411764705883f,0.18823529411764706f,0.38823529411764707f,0.4980392156862745f,0.17647058823529413f,0.48627450980392156f,0.49019607843137253f,0.1843137254901961f,0.5019607843137255f,0.3215686274509804f,0.2901960784313726f,0.5490196078431373f,0.23921568627450981f,0.40784313725490196f,0.5764705882352941f,0.2196078431372549f,0.47058823529411764f,0.7058823529411765f,0.23529411764705882f,0.6588235294117647f,0.7607843137254902f,0.17647058823529413f,0.8352941176470589f,0.7568627450980392f,0.043137254901960784f,0.7607843137254902f,0.5686274509803921f,0.23137254901960785f,0.7176470588235294f,0.403921568627451f,0.21176470588235294f,0.6823529411764706f,0.3137254901960784f,0.21568627450980393f,0.6078431372549019f,0.2f,0.17254901960784313f,0.4392156862745098f,0.16862745098039217f,0.19607843137254902f,0.4392156862745098f,0.16862745098039217f,0.19607843137254902f
			0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,0.08627450980392157f,0.1568627450980392f,0.3137254901960784f,0f,0.23921568627450981f,0.4470588235294118f,0.2235294117647059f,0.3568627450980392f,0.5529411764705883f,0.18823529411764706f,0.38823529411764707f,0.4980392156862745f,0.17647058823529413f,0.48627450980392156f,0.49019607843137253f,0.1843137254901961f,0.5019607843137255f,0.3215686274509804f,0.2901960784313726f,0.5490196078431373f,0.23921568627450981f,0.40784313725490196f,0.5764705882352941f,0.2196078431372549f,0.47058823529411764f,0.7058823529411765f,0.23529411764705882f,0.6588235294117647f,0.7607843137254902f,0.17647058823529413f,0.8352941176470589f,0.7568627450980392f,0.043137254901960784f,0.7607843137254902f,0.5686274509803921f,0.23137254901960785f,0.7176470588235294f,0.403921568627451f,0.21176470588235294f,0.6823529411764706f,0.3137254901960784f,0.21568627450980393f,0.6078431372549019f,0.2f,0.17254901960784313f,0.4392156862745098f,0.16862745098039217f,0.19607843137254902f,0.4392156862745098f,0.16862745098039217f,0.19607843137254902f
		};

		int i=0;
		while(gsrValues[i+1]<gsr && i<gsrValues.length-1) i++;
		i++;
		float s2 = (gsr-gsrValues[i-1])/(float)(gsrValues[i]-gsrValues[i-1]);
		float s1 = 1-s2; //(gsrValues[i]-gsr)/(float)(gsrValues[i]-gsrValues[i-1]);

		color[index+0] = gsrColors[i*3+0]*s2+gsrColors[(i-1)*3+0]*s1;
		color[index+1] = gsrColors[i*3+1]*s2+gsrColors[(i-1)*3+1]*s1;
		color[index+2] = gsrColors[i*3+2]*s2+gsrColors[(i-1)*3+2]*s1;

//		float value = gsr / 700.0f;
//    	value = CoreUtil.clamp(value, 0f, 1.0f);
//		return value;
/*		if (gsr<500) return 0.2f*gsr/500.0f;
		if (gsr<3000) return 0.2f + (gsr-500)*0.2f/2500f;
		if (gsr<20000) return 0.4f + (gsr-3000)*0.2f/17000f;
		if (gsr<70000) return 0.6f + (gsr-20000)*0.2f/50000f;
		return CoreUtil.clamp(0.8f+(gsr-70000)/100000.0f,0f,1.0f);*/
	}

	private void pushCoord(float t, float[] outCoords, float[] normal, float[] colorbuffer, int i, float opacity, float arousal, float width, float light, float z) {
		SpiralFormula.parametricFormulation(t, outCoords);
		float size = SpiralFormula.getThickness(t) * width;

		SpiralFormula.getNormal(t, normal);
		normalize(normal);

		// outer cirle
		/*		coordsbuffer[i * 6]     = outCoords[0] + normal[0] * size;
		coordsbuffer[i * 6 + 1] = outCoords[1] + normal[1] * size;
		coordsbuffer[i * 6 + 2] = z; //outCoords[2] + outCoords[2] * d * size;
		// inner circle
		coordsbuffer[i * 6 + 3] = outCoords[0] - normal[0] * size;
		coordsbuffer[i * 6 + 4] = outCoords[1] - normal[1] * size;
		coordsbuffer[i * 6 + 5] = z;//outCoords[2] - outCoords[2] * d * size;*/
		mVertexBuffer.put( outCoords[0] + normal[0] * size );
		mVertexBuffer.put( outCoords[1] + normal[1] * size );
		mVertexBuffer.put( z ); //outCoords[2] + outCoords[2] * d * size;
		// inner circle
		mVertexBuffer.put( outCoords[0] - normal[0] * size );
		mVertexBuffer.put( outCoords[1] - normal[1] * size );
		mVertexBuffer.put( z );//outCoords[2] - outCoords[2] * d * size;

//		opacitybuffer[i*2] = opacitybuffer[i*2+1] = opacity;
//		arousalbuffer[i*2] = arousalbuffer[i*2+1] = light;
		mOpacityBuffer.put(opacity);
		mOpacityBuffer.put(opacity);
		mArousalBuffer.put(light);
		mArousalBuffer.put(light);
		
//		gsrToColor(arousal, colorbuffer, i*2*3);
//		gsrToColor(arousal, colorbuffer, i*2*3+3);
		gsrToColor(arousal,colorbuffer,0);
		mColorBuffer.put(colorbuffer);
		mColorBuffer.put(colorbuffer);
	}

	void updateData() {
		Parameters p = mViewPort.getParameters();
		long start = p.start;
		long end = p.end;
		float timespan = end-start;

		// only update data if there is new data
		if (mLastStart==start && mLastEnd==end && !mDataProxy.isDirty()) return;
		mLastStart = start;
		mLastEnd = end;

//		Collection<TimeData> data = getData(start,end); //mDataBufferer.get(start, end); //AHState.getInstance().mGsrColumn.getTimeData(start, end);
		Collection<TimeData> data = mDataProxy.getData(start,end); //mDataBufferer.get(start, end); //AHState.getInstance().mGsrColumn.getTimeData(start, end);

		int numActualVertices = 0;
		numberOfVertices = 16*2 + data.size()*2 * (2 + 2); // first 16 for realtime segment

		if (mRawVertexBuffer==null || mRawVertexBuffer.capacity()<numberOfVertices * Core.VERTEX_DIMENSIONS * 4) {
			mRawVertexBuffer = ByteBuffer.allocateDirect(numberOfVertices * Core.VERTEX_DIMENSIONS * 4);
			mRawVertexBuffer.order(ByteOrder.nativeOrder());
			mVertexBuffer = mRawVertexBuffer.asFloatBuffer();
		}
		if (mRawArousalBuffer==null || mRawArousalBuffer.capacity()<numberOfVertices * Core.T_DIMENSIONS * 4) {
			mRawArousalBuffer = ByteBuffer.allocateDirect(numberOfVertices * Core.T_DIMENSIONS * 4);
			mRawArousalBuffer.order(ByteOrder.nativeOrder());
			mArousalBuffer = mRawArousalBuffer.asFloatBuffer();
		}
		if (mRawOpacityBuffer==null || mRawOpacityBuffer.capacity()<numberOfVertices * Core.T_DIMENSIONS * 4) {
			mRawOpacityBuffer = ByteBuffer.allocateDirect(numberOfVertices * Core.T_DIMENSIONS * 4);
			mRawOpacityBuffer.order(ByteOrder.nativeOrder());
			mOpacityBuffer = mRawOpacityBuffer.asFloatBuffer();
		}
		if (mRawColorBuffer==null || mRawColorBuffer.capacity()<numberOfVertices * 4 * 3) {
			mRawColorBuffer = ByteBuffer.allocateDirect(numberOfVertices * 4 * 3);
			mRawColorBuffer.order(ByteOrder.nativeOrder());
			mColorBuffer = mRawColorBuffer.asFloatBuffer();
		}
		
		// set data in buffers
		mVertexBuffer.position(0);
		mArousalBuffer.position(0);
		mOpacityBuffer.position(0);
		mColorBuffer.position(0);
		
		float[] outCoords = new float[3];
		float[] normal = new float[3];
		float[] prevCoord = new float[3];
		float[] colors = new float[3];
		prevCoord[0] = 0f;
		prevCoord[1] = 0f;

		Iterator<TimeData> iterator = data.iterator();
		TimeData td = iterator.hasNext()?iterator.next():null;	// get first
		TimeData tdnext = td;	// just to make sure tdnext != null when we got a td
		long resolution = mDataProxy.getDataResolution(start, end);
		float datawidth = (float)(resolution/ (double)(end-start));
		float smooth = 0.4f;

		float width = 1.0f;
		long prevTime = end;

		tdnext = iterator.hasNext()?iterator.next():null;
		int i = 0;//
		if(mDrawRealtimeSegment) {
			i = createRealtimeSegment(colors, outCoords, normal, prevTime, resolution, td, tdnext, start, end);
		}
		
		for (;tdnext!=null;) {
			float opacityvalue = 1.0f;
			float t = (end-td.time) / timespan;
			if(t < -0.1f) {
				t = -0.1f;
			}

			t = SpiralFormula.rescaleTToAfterRealtimeSegment(t);
			float z = 1.0f;

			if (prevTime-td.time>resolution*2) {
				pushCoord(t, outCoords, normal, colors, i++, 0.0f, (td.value), width, 1.0f, 0.5f);
			}
			pushCoord(t+datawidth*smooth, outCoords, normal, colors, i++, opacityvalue, (td.value), width, 1.0f, z);
			pushCoord(t+datawidth*(1-smooth), outCoords, normal, colors, i++, opacityvalue, (td.value), width, 1.0f, z);
			if (tdnext!=null && td.time-tdnext.time>resolution) {
				pushCoord(t+datawidth, outCoords, normal, colors, i++, 0.0f, (td.value), width, 1.0f, 0.5f);
			}

			prevTime = td.time;
			td = tdnext;
			tdnext = iterator.hasNext()?iterator.next():null;
		}
		numActualVertices = i*2;

		// upload buffers
        mVertexBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 3 * numActualVertices, mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
        mArousalBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * numActualVertices, mArousalBuffer, GLES20.GL_DYNAMIC_DRAW);
        mOpacityBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[2]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * numActualVertices, mOpacityBuffer, GLES20.GL_DYNAMIC_DRAW);
        mColorBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[3]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 3 * numActualVertices, mColorBuffer, GLES20.GL_DYNAMIC_DRAW);

        numberOfVertices = numActualVertices;
	}
	
	private int createRealtimeSegment(float[] colors, float[] outCoords, float[] normal, long prevTime, long resolution, TimeData td, TimeData tdnext, long start, long end) {
		int i = 0;
		float width = 1.0f;
		float z = 2.0f;
		float opacity_value = AHState.getInstance().hasRealtimeData()?1.0f:0.2f;
		if(!AHState.getInstance().isUpdatingRealtime()) {
			width = 1.1f;
		}
		pushCoord(0.001f, outCoords, normal, colors, i, opacity_value, AHState.getInstance().mRTgsr, width, 1.0f, z);
		for(i = 1; i<= 10; i++) {
			pushCoord(i * SpiralFormula.REALTIME_SEGMENT_SIZE * 0.95f / 10, outCoords, normal, colors, i, opacity_value, AHState.getInstance().mRTgsr, width, 1.0f, z);
		}
		width = 1.0f;
		float tdValue = 0.0f;
		if(td!=null) {
			tdValue = td.value;
		}
		if(!AHState.getInstance().isUpdatingRealtime()) {
			// add a black "shadow" data point to end realtime segment
			pushCoord(SpiralFormula.REALTIME_SEGMENT_SIZE*0.95f, outCoords, normal, colors, i++, opacity_value, AHState.getInstance().mRTgsr, width, 0.0f, z);
			pushCoord(SpiralFormula.REALTIME_SEGMENT_SIZE, outCoords, normal, colors, i++, opacity_value, tdValue, width, 0.0f, z);
			
			// This isn't entirely right if the data STARTS with a disconnect...
			if(td!=null && (prevTime - td.time) > resolution) {						
				pushCoord(SpiralFormula.REALTIME_SEGMENT_SIZE, outCoords, normal, colors, i++, 0.0f, tdValue, width, 0.0f, 0.5f);
			}
			else {
				pushCoord(SpiralFormula.REALTIME_SEGMENT_SIZE, outCoords, normal, colors, i++, opacity_value, tdValue, width, 1.0f, z);					
			}
		}
		else {
			if( td!=null && (end-td.time > resolution * 2)) {
			     pushCoord(SpiralFormula.REALTIME_SEGMENT_SIZE*0.95f, outCoords, normal, colors, i++, 0.0f, tdValue, width, 0.0f, 0.5f);
			     pushCoord(SpiralFormula.REALTIME_SEGMENT_SIZE, outCoords, normal, colors, i++, 0.0f, tdValue, width, 0.0f, 0.5f);
		    }
		}
		return i;
	}
		
	private void normalize(float[] v) {
		float l = 1f / (float)(Math.sqrt(v[0] * v[0] + v[1] * v[1]));
		v[0] *= l;
		v[1] *= l;
	}
	
	public void init() {
		
		// share shader and texture loading between instances, make them static!
		
		mDataProxy = new DataProxy("gsr", AHState.getInstance().mGsrFilteredColumn, DataProxy.METHOD_MAX, mContext);

		if(!mShaderAndTextureLoaded) {
			mShaderAndTextureLoaded = true;
			mSpiralShader = Core.getInstance().loadProgram("spiral", kVertexShader, kFragmentShader);
	        GLES20.glBindAttribLocation(mSpiralShader, 0, "position");
	        GLES20.glBindAttribLocation(mSpiralShader, 1, "arousal");
	        GLES20.glBindAttribLocation(mSpiralShader, 2, "opacity");
	        GLES20.glBindAttribLocation(mSpiralShader, 3, "color");
	        GLES20.glLinkProgram(mSpiralShader);
	        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mSpiralShader, "worldViewProjection");
	        mModelViewLoc = GLES20.glGetUniformLocation(mSpiralShader, "modelView");
	
	        mTexSampler = GLES20.glGetUniformLocation(mSpiralShader, "textureSampler");
	
	        mTex = Core.getInstance().loadGLTexture(R.raw.lut_anna, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE, true));
		}
		
		mShader = mSpiralShader;

//		createData();

		int buffers = 4;
		vboId = new int[buffers];
        GLES20.glGenBuffers(buffers, vboId, 0);
	}
	
	 private static final String kVertexShader =
         "precision mediump float; \n" +
         "uniform mat4 worldViewProjection; \n" +
         "uniform mat4 modelView; \n" +
         "attribute vec3 position; \n" +
         "attribute float arousal; \n" +
         "attribute float opacity; \n" +
         "attribute vec3 color; \n" +
         "varying float visibility; \n" +
         "varying float gsr; \n" +
         "varying vec3 vColor; \n" +
         "void main() { \n" +
/*         "	textureCoordinate = arousal; \n" +
         "  tc = vec2(temporalOffset + temporal.x * temporalScale, temporal.y); \n" +*/
         "  visibility = opacity; \n" +
         "  gsr = arousal; \n" +
         "  vColor = color; \n" +
         "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n" +
         "}";
 
     private static final String kFragmentShader =
         "precision mediump float; \n" +
         "uniform sampler2D textureSampler; \n" +
         "varying float visibility; \n" +
         "varying float gsr; \n" +
         "varying vec3 vColor; \n" +
         "void main() { \n" +
         "  vec4 color = vec4(vColor,1.0); //texture2D(textureSampler, vec2(gsr)); \n" + 
         "  gl_FragColor = vec4(color.rgb*gsr,visibility); //vec4(1.0,0.0,0.0,1.0); //vec4(color.rgb, 1.0); \n" +
         "}";
}


