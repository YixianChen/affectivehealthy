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
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Movement visualisation
 * 
 * @version 0.1
 * @author mareri
 *
 */

public class SpiralShadowDynamic extends Mesh20 {
	private int mNumberOfVertices = 1000;
	
	int mModelViewProjectionLoc;
	int mModelViewLoc;
	float[] mMovementData;
	
	int[] mVboTex;
	FloatBuffer mTextureCoords;
	int mTex;
	int mTextureSamplerLoc;
	int mTLoc;
	int mLowOpacityTThresholdLoc;

	FloatBuffer mTBuffer;
	
	private boolean mDrawRealtimeSegment = true;
	
	private ViewPort mViewPort;

	public SpiralShadowDynamic(ViewPort viewPort) {
		super("ShadowDynamic");
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
		super.draw(camera, pick);

		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

		GLES20.glBlendFunc(GLES20. GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
        GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);
        
		Parameters p = mViewPort.getParameters();		        
		float threshold = (float)((double)(System.currentTimeMillis() - p.start) / (double)(p.end - p.start));
		threshold = Math.min(1.0f / (1.0f - SpiralFormula.REALTIME_SEGMENT_SIZE), Math.max(0.0f, threshold));
		
		if(AHState.getInstance().isUpdatingRealtime() && p.end < System.currentTimeMillis()) {
			threshold = 2.0f; // outside of spiral
		}
		
		GLES20.glUniform1f(mLowOpacityTThresholdLoc, threshold);

        if(!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);
        }
        
    	updateData();

        GLES20.glUniform1i(mTextureSamplerLoc, 0);
        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTex);

	    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glEnableVertexAttribArray(Core.VERTEX);
        Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);

	    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
        GLES20.glEnableVertexAttribArray(1);
        Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
        GLES20.glEnableVertexAttribArray(2);
        Core.glVertexAttribPointer(2, 1, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mNumberOfVertices);
        
        GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
	}
	
	/**
	 * Read data from the database
	 */
	void updateData() {
	    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 3 * mNumberOfVertices, mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 2 * mNumberOfVertices, mTextureCoords, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 1 * mNumberOfVertices, mTBuffer, GLES20.GL_DYNAMIC_DRAW);
	}

	private void normalize(float[] v) {
		float l = 1f / (float)(Math.sqrt(v[0] * v[0] + v[1] * v[1]));
		v[0] *= l;
		v[1] *= l;
	}
	
	private void pushCoord(float t, float[] outCoords, float[] prevCoord, float[] normal, float[] coords, int i, float opacity, float value) {
		SpiralFormula.parametricFormulation(t, outCoords);
		float thickness = SpiralFormula.getThickness(t);
		float size = thickness;

		size *= value; //td.value;

		SpiralFormula.getNormal(t, normal);
		normalize(normal);
		prevCoord[0] = outCoords[0];
		prevCoord[1] = outCoords[1];
		coords[i * 6]     = outCoords[0] + normal[0] * size;// + normal[0] * thickness;
		coords[i * 6 + 1] = outCoords[1] + normal[1] * size;// + normal[1] * thickness;
		coords[i * 6 + 2] = opacity;
		coords[i * 6 + 3] = outCoords[0] - normal[0] * size;// + normal[0] * thickness;
		coords[i * 6 + 4] = outCoords[1] - normal[1] * size;// + normal[1] * thickness;
		coords[i * 6 + 5] = opacity;
	}

	private void createData() {
		int numberOfPoints = 500; //data.size();
		int numActualVertices = 0;
		mNumberOfVertices = numberOfPoints*6; //data.size()*6;
//		Log.d("Movement2", "Number of vertices: " + numberOfVertices);
		if (numberOfPoints==0) return;
		
		// create data buffers
		ByteBuffer vbb = ByteBuffer.allocateDirect(mNumberOfVertices * 4 * 3);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		ByteBuffer tcb = ByteBuffer.allocateDirect(mNumberOfVertices * 4 * 2);
		tcb.order(ByteOrder.nativeOrder());
		mTextureCoords = tcb.asFloatBuffer();

		ByteBuffer tb = ByteBuffer.allocateDirect(mNumberOfVertices * 4 );
		tb.order(ByteOrder.nativeOrder());
		mTBuffer = tb.asFloatBuffer();
		
		float[] coords = new float[mNumberOfVertices * 3 * 2];
		float[] outCoords = new float[3];
		float[] normal = new float[3];
		float[] prevCoord = new float[3];
		prevCoord[0] = 0f;
		prevCoord[1] = 0f;

		int i;
		for (i=2;i<numberOfPoints;i++) {
			float t = i / (float)numberOfPoints;
			if(!mDrawRealtimeSegment) {
				t = SpiralFormula.rescaleTToAfterRealtimeSegment(t);
			}
			pushCoord(t, outCoords, prevCoord, normal, coords, i-2, 1.0f, 1.5f);
		}
		
		numActualVertices = (i-2)*2;
		
		for (i = 0; i < numActualVertices; i++) {
			for (int j = 0; j < 3; j++) {
				mVertexBuffer.put(coords[i * 3 + j]);
			}
			mTextureCoords.put(i%2==0?0:1);
			mTextureCoords.put(i<40?i/80f:0.5f);
			mTBuffer.put( ( 1.0f - i / (float)numActualVertices) / (1.0f - SpiralFormula.REALTIME_SEGMENT_SIZE) );
		}

		mNumberOfVertices = numActualVertices;
		mVertexBuffer.position(0);
		mTextureCoords.position(0);
		mTBuffer.position(0);
	}

	public void init() {
		super.init();

        mShader = Core.getInstance().loadProgram("spiralShadowDynamic", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "texturecoords");
        GLES20.glBindAttribLocation(mShader, 2, "t");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");
        
        mTextureSamplerLoc = GLES20.glGetUniformLocation(mShader, "texture");
        mTex = Core.getInstance().loadGLTexture(R.raw.shadow_feather_nt2, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE, true));

        mLowOpacityTThresholdLoc = GLES20.glGetUniformLocation(mShader, "lowOpacityTThreshold");
        
        mVboTex = new int[2];
        GLES20.glGenBuffers(2, mVboTex, 0);
//		createData();

//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);

	    createData();
	}
	
	 private static final String kVertexShader =
         "precision mediump float; \n" +
         "uniform mat4 worldViewProjection; \n" +
         "uniform mat4 modelView; \n" +
         "uniform float lowOpacityTThreshold; \n" +
         "attribute vec3 position; \n" +
         "attribute vec2 texturecoords; \n" +
         "attribute float t; \n" +
         "varying vec2 vTC; \n" +
         "varying float tVal; \n" +
         "void main() { \n" +
         "  vTC = texturecoords; \n" +
         "  tVal = t; \n" + 
         "  gl_Position = worldViewProjection * modelView * vec4(position.xy, vec2(1.0)); \n" +
         "}";
 
     private static final String kFragmentShader =
         "precision mediump float; \n" +
         "uniform sampler2D texture; \n" +
         "uniform float lowOpacityTThreshold; \n" +
         "varying vec2 vTC; \n" +
         "varying float tVal; \n" +
         "void main() { \n" +
         "  vec4 col = texture2D(texture, vTC); \n" +
         "  if(tVal > lowOpacityTThreshold) {" +
         "    col = col * vec4(1.0, 1.0, 1.0, 0.5);\n" +
         "  }\n" +
         "  gl_FragColor = col;\n" +
         "}";

}


