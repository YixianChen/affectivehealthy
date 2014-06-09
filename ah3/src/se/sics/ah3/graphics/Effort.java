package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import se.sics.ah3.AHState;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import se.sics.ah3.usertags.UserTags;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Effort shape
 * 
 * @version 0.2
 * @author mareri
 *
 */

public class Effort extends Mesh20 {
	private int numberOfVertices = 1000;
	
	// test geometry
	int mModelViewProjectionLoc;
	int mIntensityLoc;
	float mIntensity;
	
	public Effort() {
		super("Effort");
		ByteBuffer vbb = ByteBuffer.allocateDirect(numberOfVertices * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
	}
	
	public void setIntensity(float intensity) {
		mIntensity = intensity;
		GLES20.glUseProgram(mShader);
		GLES20.glUniform1f(mIntensityLoc, mIntensity);
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		if(!mVisible || (pick && !mPickable)) {
			return;
		}
		super.draw(camera, pick);
		if(!pick) {
			GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);
			
			GLES20.glUseProgram(mShader);
	        GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
		}
	        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glEnableVertexAttribArray(Core.VERTEX); 
        Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, numberOfVertices);
        
        if(!pick) {
        	GLES20.glDisable(GLES20.GL_BLEND);
        }
        
        updateData();
	}
	
	void updateData() {
		Parameters p = AHState.getInstance().mViewPort.getParameters();
		Vector<UserTags.UserTag> energyLevels = AHState.getInstance().mUserTags.getUserTags(p.start, p.end);

	}
	
	float step(float cx, float cy, float px, float py) {
		float d = (float) Math.sqrt((cx - px) * (cx - px) + (cy - py) * (cy - py));
		//Log.d("Spiral", "D: " + d);
		return d;
	}
	
	float stepInterpolation(float y0, float y1, float t, float l) {
		if(t < l) {
			return y0;
		} else {
			return y1;
		}
	}
	
	float smoothStepInterpolation(float t) {
		return t * t * (3f - 2f * t);
	}
	
	boolean close(float a, float b) {
		float EPSILON = 0.001f;
		return Math.sqrt((a - b) * (a - b)) < EPSILON;
	}
	
	float distanceToCenter(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z *z);
	}
	
	void normalize(float[] v) {
		float l = 1f / (float)(Math.sqrt(v[0] * v[0] + v[1] * v[1]));
		v[0] *= l;
		v[1] *= l;
	}
	
	private void createData() {
		float[] coords = new float[numberOfVertices * 3];
		float measure = 0f;
		float stepLength;
		float[] outCoords = new float[3];
		float[] normal = new float[2];
		float[] prevCoord = new float[3];
		prevCoord[0] = 0f;
		prevCoord[1] = 0f;
		
		float startSpiralT = 0.075f;
		float endSpiralT = 1f - startSpiralT;
		SpiralFormula.parametricFormulation(startSpiralT - 0.001f, prevCoord);
		
		for(int i = 0; i < numberOfVertices * 0.5f; i++) {
			float t = startSpiralT + endSpiralT * (float)i / (numberOfVertices * 0.5f - 1);
			SpiralFormula.parametricFormulation(t, outCoords); 
			float d = 1f / distanceToCenter(outCoords[0], outCoords[1], outCoords[2]);
			float size = SpiralFormula.getThickness(t) * (float)(Math.random() > 0.5 ? Math.random() : 0.0);// (float)(Math.sin((1f - t) * Math.PI * 0.5)) * 0.075f;//0.05f - t * t * 0.04f;
			
			normal[0] = prevCoord[1] - outCoords[1];
			normal[1] = -prevCoord[0] + outCoords[0];
			normalize(normal);
			prevCoord[0] = outCoords[0];
			prevCoord[1] = outCoords[1];
			
			coords[i * 6]     = outCoords[0];// + normal[0] * size;
			coords[i * 6 + 1] = outCoords[1];// + normal[1] * size;
			//coords[i * 6 + 2] = outCoords[2] + outCoords[2] * d * size;
			coords[i * 6 + 3] = outCoords[0] - normal[0] * size;
			coords[i * 6 + 4] = outCoords[1] - normal[1] * size;
			//coords[i * 6 + 5] = outCoords[2] - outCoords[2] * d * size;
			if(i > 0) {
				measure += step(coords[i * 3], coords[i * 3 + 1], coords[(i-1) * 3], coords[(i-1) * 3  + 1]);
			}
		}
		
		Log.d("Spiral", "Length: " + measure);
		stepLength = measure / numberOfVertices;
		Log.d("Spiral", "Step length: " + stepLength);
		
		for (int i = 0; i < numberOfVertices; i++) {
			for (int j = 0; j < 3; j++) {
				mVertexBuffer.put(coords[i * 3 + j]);
			}
		}

		mVertexBuffer.position(0);
	}
	
	public void init() {
		super.init();
        mShader = Core.getInstance().loadProgram("effort", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc =
            GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        
        mIntensityLoc = GLES20.glGetUniformLocation(mShader, "intensity");
        
		createData();
		
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_STATIC_DRAW);
        
        setIntensity(0.25f);
	}
	
	 private static final String kVertexShader =
         "precision mediump float; \n" +
         "uniform mat4 worldViewProjection; \n" +
         "attribute vec3 position; \n" +
         "void main() { \n" +
         "  gl_Position = worldViewProjection * vec4(position, 1.0); \n" +
         "}";
 
     private static final String kFragmentShader =
         "precision mediump float; \n" +
         "uniform float intensity; \n" +
         "void main() { \n" +
         "  gl_FragColor = vec4(vec3(0.0), intensity); \n" +
         "}";
}


