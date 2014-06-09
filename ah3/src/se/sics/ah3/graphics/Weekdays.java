package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Vector;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Labels for weekdays
 * @author mareri
 *
 */

public class Weekdays extends Mesh20 {
	private final static int MAX_NUMBER_OF_ENTRIES = 6*14; // max is now 14 days... used to be 1000;
	
	int mModelViewProjectionLoc;
	int mModelViewLoc;
	int mNumberOfEntries;
	int mNumberOfVertsPerPrim;
	int mTexId, mTexSampler;
	int[] mVboTex;
	
	FloatBuffer mTextureCoordBuffer;
	FloatBuffer mOpacityBuffer;
	
	Vector<UV> uvs;
	
	private ViewPort mViewPort;
	
	public Weekdays(ViewPort viewport) {
		super("Weekdays");
		
		mViewPort = viewport;
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		
		ByteBuffer tcbb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mTextureCoordBuffer = tcbb.asFloatBuffer();
		
		ByteBuffer obb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 4);
		obb.order(ByteOrder.nativeOrder());
		mOpacityBuffer = obb.asFloatBuffer();
		
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		
		uvs = new Vector<Weekdays.UV>();
/*		OLD veckor.png
 		uvs.add(new UV(0f, 0.7f, 0.12f, 0.22f));
		uvs.add(new UV(0f, 0.7f, 0.22f, 0.32f));
		uvs.add(new UV(0f, 0.7f, 0.45f, 0.55f));
		uvs.add(new UV(0f, 0.7f, 0.57f, 0.67f));
		uvs.add(new UV(0f, 0.7f, 0.34f, 0.44f));
		uvs.add(new UV(0f, 0.7f, 0.66f, 0.76f));
		uvs.add(new UV(0f, 0.7f, 0.79f, 0.89f));*/
		// new days_heli_white
/*		uvs.add(new UV(12/101f, 89/101f, 45/315f, 0f));
		uvs.add(new UV(12/101f, 75/101f, 90/315f, 45/315f));
		uvs.add(new UV(12/101f, 92/101f, 135/315f, 90/315f));
		uvs.add(new UV(12/101f, 80/101f, 180/315f, 135/315f));
		uvs.add(new UV(12/101f, 58/101f, 225/315f, 180/315f));
		uvs.add(new UV(12/101f, 72/101f, 270/315f, 225/315f));
		uvs.add(new UV(12/101f, 81/101f, 315/315f, 270/315f));*/
		uvs.add(new UV(0,1, 45/315f, 0f));
		uvs.add(new UV(0,1, 90/315f, 45/315f));
		uvs.add(new UV(0,1, 135/315f, 90/315f));
		uvs.add(new UV(0,1, 180/315f, 135/315f));
		uvs.add(new UV(0,1, 225/315f, 180/315f));
		uvs.add(new UV(0,1, 270/315f, 225/315f));
		uvs.add(new UV(0,1, 315/315f, 270/315f));
	}
	
	void addEntry(int day, float offset) {
		//t += 0.0001f;
//		float thickness = 2f* SpiralFormula.getThickness(offset);
		float thickness = 0.15f*2;
		float opacity = 1.0f;

		if ((offset - 0.02f) < 0.05f) {
			opacity = (offset - 0.02f)/0.05f;
			opacity = Math.max(0.0f, opacity);
		}
		if (offset>0.95f) {
			opacity = (1-offset)/0.05f;
		}

		offset = SpiralFormula.rescaleTToAfterRealtimeSegment(offset);

		float[] point = { 0f, 0f, 0f };//new float[3];
		float[] normal = { 1f, 0f, 0f };//new float[3];
		float[] tangent = { 0f, 1f, 0f };//new float[3];
		
		SpiralFormula.parametricFormulation(offset, point);
		normal = SpiralFormula.getNormal(offset);
		tangent = SpiralFormula.getTangent(offset);
		
		CoreUtil.normalize(normal);
		CoreUtil.normalize(tangent);
		
		CoreUtil.scale(1f * thickness, normal);
		CoreUtil.scale(0.5f * thickness, tangent);
		
		float dimX = 0.45f;//thickness;// * 0.5f;
		float dimY = 0.45f;//thickness;// * 0.05f;
		
		// v0 -n -t
		mVertexBuffer.put(point[0] + (-normal[0] - tangent[0]) * dimX);
		mVertexBuffer.put(point[1] + (-normal[1] - tangent[1]) * dimY);
		mVertexBuffer.put(0f);
		// v1 +n -t
		mVertexBuffer.put(point[0] + (normal[0] - tangent[0]) * dimX);
		mVertexBuffer.put(point[1] + (normal[1] - tangent[1]) * dimY);
		mVertexBuffer.put(0f);
		// v2 -n +t
		mVertexBuffer.put(point[0] + (-normal[0] + tangent[0]) * dimX);
		mVertexBuffer.put(point[1] + (-normal[1] + tangent[1]) * dimY);
		mVertexBuffer.put(0f);
		// v2 -n +t
		mVertexBuffer.put(point[0] + (-normal[0] + tangent[0]) * dimX);
		mVertexBuffer.put(point[1] + (-normal[1] + tangent[1]) * dimY);
		mVertexBuffer.put(0f);
		// v1 +n -t
		mVertexBuffer.put(point[0] + (normal[0] - tangent[0]) * dimX);
		mVertexBuffer.put(point[1] + (normal[1] - tangent[1]) * dimY);
		mVertexBuffer.put(0f);
		// v3 +n +t
		mVertexBuffer.put(point[0] + (normal[0] + tangent[0]) * dimX);
		mVertexBuffer.put(point[1] + (normal[1] + tangent[1]) * dimY);
		mVertexBuffer.put(0f);

		UV uv = uvs.get(day);

		float u0 = uv.u1;//0f;
		float v0 = uv.v0;//0.12f;
		float u1 = uv.u0;//0.7f;
		float v1 = uv.v1;//0.22f;
		
/*		if(tangent[1] < 0f) {
			float switchTemp = u0;
			u0 = u1;
			u1 = switchTemp;
			
			switchTemp = v0;
			v0 = v1;
			v1 = switchTemp;
		}*/
		
		// t0
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v0);
		// t1
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v0);
		// t2
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v1);
		// t2
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v1);
		// t1
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v0);
		// t3
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v1);
		
		for (int i=0;i<6;i++) {
			mOpacityBuffer.put(opacity);
		}
		mNumberOfEntries++;
		
	}
	
	/**
	 * iterate of the visual days an add them as labels
	 */
	void setPosition() {
		// reset the buffer
		mNumberOfEntries = 0;
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		
		Parameters parameters = mViewPort.getParameters();
		
		Date currentDate = new Date(parameters.end);
		Date endDate = new Date(parameters.start);
		
		long offset = currentDate.getHours() * 60 * 60 * 1000 + currentDate.getMinutes() * 60 * 1000 + currentDate.getSeconds() * 1000 + (currentDate.getTime()%1000);
		
		long currentDay = currentDate.getTime() - offset;
		
		while(currentDay > endDate.getTime()) {
			float t = SpiralFormula.getT(currentDay, parameters);
			int day = new Date(currentDay).getDay() - 1;
			day = day == -1 ? 6 : day;
			addEntry(day, t);
			
			currentDay -= Parameters.DAY;
		}
		
		// reset the buffer and commit
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mOpacityBuffer.capacity(), mOpacityBuffer, GLES20.GL_DYNAMIC_DRAW);	
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		super.draw(camera, pick);
		
		setPosition();
		
		if(!pick) {
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
			GLES20.glEnable(GLES20.GL_BLEND);
			
			GLES20.glUniform1i(mTexSampler, 0);
	        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTexId);
	        
			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
			GLES20.glEnableVertexAttribArray(2);
			Core.glVertexAttribPointer(2, 1, GLES20.GL_FLOAT, false, 0, 0);
		}
	
		if(!pick || mPickable) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
			GLES20.glEnableVertexAttribArray(Core.VERTEX);
			Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
		
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mNumberOfVertsPerPrim * mNumberOfEntries);
			
			GLES20.glDisable(GLES20.GL_BLEND);
		}

	}
	
	@Override
	public void init() {
		super.init();
		
		mNumberOfVertsPerPrim = 6;
		
		mShader = Core.getInstance().loadProgram("weekdays", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glBindAttribLocation(mShader, 2, "opacity");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");

        mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	
//        mTexId = Core.getInstance().loadGLTexture(R.raw.veckor, new Texture2DParameters());
        mTexId = Core.getInstance().loadGLTexture(R.raw.days_helv_white, new Texture2DParameters());
	
        mVboTex = new int[2];
        GLES20.glGenBuffers(2, mVboTex, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mOpacityBuffer.capacity(), mOpacityBuffer, GLES20.GL_STATIC_DRAW);
	}
	
	private static final String kVertexShader =
        "precision mediump float; \n" +
        "attribute vec3 position; \n" +
        "attribute vec2 textureCoordinates; \n" +
        "attribute float opacity; \n" +
        "uniform mat4 worldViewProjection; \n" +
        "uniform mat4 modelView; \n" +
        "varying vec2 tc; \n" +
        "varying float visibility; \n" +
        "void main() { \n" +
        "  tc = textureCoordinates; \n" +
        "  visibility = opacity; \n" +
        "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n" +
        "}";

    private static final String kFragmentShader =
        "precision mediump float; \n" +
        "uniform sampler2D textureSampler; \n" +
        "varying vec2 tc; \n" +
        "varying float visibility; \n" +
        "void main() { \n" +
//        "  gl_FragColor = vec4(texture2D(textureSampler, tc).rgb, 1.0); \n" +
        "  gl_FragColor = texture2D(textureSampler, tc)*visibility; \n" +
        "}";
    
    class UV {
    	float u0, u1, v0, v1;
    	
    	UV(float u0, float u1, float v0, float v1) {
    		this.u0 = u0;
    		this.u1 = u1;
    		this.v0 = v0;
    		this.v1 = v1;
    	}
    }
}
