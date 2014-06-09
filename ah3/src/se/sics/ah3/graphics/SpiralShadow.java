package se.sics.ah3.graphics;

import static android.opengl.GLES10.GL_LINEAR;
import static android.opengl.GLES10.GL_NEAREST;
import static android.opengl.GLES10.GL_REPEAT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import se.sics.ah3.R;
import se.sics.ah3.interaction.Parameters;
import android.opengl.GLES20;

public class SpiralShadow extends Mesh20 {
	private final static int VERTS = 4;
	int mTexId;
	int mTexSampler;
	int[] vboId;
	int mModelViewProjectionLoc;
	int mModelViewLoc;

	private FloatBuffer mTextureCoordBuffer;
	
	boolean mActive = false;	//	whether this is being set or not.

//	float x0 = Parameters.LEFT_BOUND, y0 = -Parameters.UP_BOUND;
//	float x1 = Parameters.RIGHT_BOUND, y1 =  -Parameters.DOWN_BOUND;
	float x0 = -1f, y0 = 1.6f+0.2f;
	float x1 = 1f, y1 =  -1.6f+0.2f;
	float[] vertices = {
			x0,y0,1.0f,
			x1,y0,1.0f,
			x0,y1,1.0f,
			x1,y1,1.0f,
	};
	
	float[] textureCoords = {
			0.0f, 1.0f,
			1.0f, 1.0f,
			0.0f,  0.0f,
			1.0f,  0.0f
	};
	
	/*
	 * setposition
	 * draw triangle + marker
	 * special for pick
	 */

	public SpiralShadow() {
		super("SpiralShadow");
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		
		for(int i = 0; i < VERTS * 3; i++) {
			mVertexBuffer.put(vertices[i]);
		}

		ByteBuffer tcbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mTextureCoordBuffer = tcbb.asFloatBuffer();
		
		for(int i = 0; i < VERTS * 2; i++) {
			mTextureCoordBuffer.put(textureCoords[i]);
		}
		
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		super.draw(camera, pick);
		if(!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);
			
			// marker
			GLES20.glUseProgram(mShader);
			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);
			GLES20.glUniform1i(mTexSampler, 0);
	        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTexId);
	        
	        // tex buffer
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
		}
		if(!pick || mPickable) {
			// vertex buffer
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
			GLES20.glEnableVertexAttribArray(Core.VERTEX);
			Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
		
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTS);
        	GLES20.glDisable(GLES20.GL_BLEND);
        }
	}
	
	@Override
	public void init() {
		super.init();
		mShader = Core.getInstance().loadProgram("spiralShadow", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");

        mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
        mTexId = Core.getInstance().loadGLTexture(R.raw.background_shadow2, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));

        vboId = new int[2];
        GLES20.glGenBuffers(2, vboId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_STATIC_DRAW);
	
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_STATIC_DRAW);
	}
	
	private static final String kVertexShader =
        "precision mediump float; \n" +
        "uniform mat4 worldViewProjection; \n" +
        "uniform mat4 modelView; \n" +
        "attribute vec3 position; \n" +
        "attribute vec2 textureCoordinates; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
        "  tc = textureCoordinates; \n" +
        "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n" +
        "}";

    private static final String kFragmentShader =
        "precision mediump float; \n" +
        "uniform sampler2D textureSampler; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
        "  vec4 col = texture2D(textureSampler, tc); \n"+
        "  gl_FragColor = col; \n" +
        "}";
}
