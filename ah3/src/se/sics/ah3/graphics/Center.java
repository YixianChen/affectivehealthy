package se.sics.ah3.graphics;

import static android.opengl.GLES10.GL_LINEAR;
import static android.opengl.GLES10.GL_NEAREST;
import static android.opengl.GLES10.GL_REPEAT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Date;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class Center extends Mesh20 {
	private final static int VERTS = 4;
	int mShadowTex;
	int mShadowSampler;
	int mCircleTex;
	int mCircleSampler;
	int mLutSampler;
	int mLutTex;
	int mTexturePath;
	int mColorLoc;
	int mMidColorLoc;
	int mShadowOpacityLoc;
	int[] vboId;
	int mModelViewProjectionLoc;
	int mModelViewLoc;

	private FloatBuffer mTextureCoordBuffer;

	float dimensions = 0.23f;
	float posX = -0.0f;
	float posY = 0.0f;
		
	float[] vertices = {
			-dimensions + posX,-dimensions + posY, 1.0f,
			dimensions + posX,-dimensions + posY, 1.0f,
			-dimensions + posX,dimensions + posY,1.0f,
			dimensions + posX,dimensions + posY, 1.0f,
	};
	
	float[] textureCoords = {
			0.0f, 0.0f,
			1.0f, 0.0f,
			0.0f,  1.0f,
			1.0f,  1.0f
	};

	/*
	 * setposition
	 * draw triangle + marker
	 * special for pick
	 */
	
	public Center() {
		super("Center2");
		
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
//		mPickable = true;
	}
		
	public void getCenterColor(float[] color) {
		float gsr = AHState.getInstance().mRTgsr;
		Spiral.gsrToColor(gsr, color, 0);
	}

	private float mLastRealtimeColor = 0.0f;
	private float mLastLevelLevel = 0.0f;
	@Override
	public void draw(Camera camera, boolean pick) {
		if(!mVisible) {
			return;
		}
		super.draw(camera, pick);
		if(!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);

			final float ANGLE = 83;
			float [] rotation = new float[16];
			Matrix.setRotateM(rotation, 0, ANGLE, 0,0,1);
			Matrix.multiplyMM(rotation, 0, camera.getmModelViewProjectionMatrix(), 0, rotation,0);

			// marker
			GLES20.glUseProgram(mShader);
			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
//			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, rotation, 0);
			GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);
			GLES20.glUniform1i(mShadowSampler, 0);
	        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mShadowTex);
	        GLES20.glUniform1i(mLutSampler, 1);
	        Core.getInstance().bindTexture(1, GLES20.GL_TEXTURE_2D, mLutTex);
			GLES20.glUniform1i(mCircleSampler, 3);
	        Core.getInstance().bindTexture(3, GLES20.GL_TEXTURE_2D, mCircleTex);

			float tval = AHState.getInstance().mRTgsr;
			boolean gettingRealtimeData = /*!Float.isNaN(tval) && */AHState.getInstance().hasRealtimeData();
	        if (gettingRealtimeData) {
		        GLES20.glUniform4f(mColorLoc, 1.0f, 0, 0, 1f);
	        } else {
		        GLES20.glUniform4f(mColorLoc, 1.0f, 0, 0, 0.2f);
	        }
	        float[] col = new float[3];
	        getCenterColor(col);
	        GLES20.glUniform3f(mMidColorLoc, col[0], col[1], col[2]);
	        
	        if (AHState.getInstance().isUpdatingRealtime()) {
		        GLES20.glUniform1f(mShadowOpacityLoc, 0.0f);
	        } else {
		        GLES20.glUniform1f(mShadowOpacityLoc, 1.0f);
		        float[] transform = new float[16];
		        Matrix.translateM(transform, 0, getTransform(), 0, -0.01f, -0.014f, 0.0f);
				GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, transform, 0);
	        }

	        //TODO: Dynaic color deactived
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
		}
		if(!pick || mPickable) {
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
		mShader = Core.getInstance().loadProgram("center", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");

        mShadowSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
        mLutSampler = GLES20.glGetUniformLocation(mShader, "lutSampler");
        mCircleSampler = GLES20.glGetUniformLocation(mShader, "circleSampler");
        mShadowTex = Core.getInstance().loadGLTexture(R.raw.center2_circle, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
        mCircleTex = Core.getInstance().loadGLTexture(R.raw.center2_circle_full, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
        mColorLoc = GLES20.glGetUniformLocation(mShader, "color");
        mMidColorLoc = GLES20.glGetUniformLocation(mShader, "midcolor");
        mShadowOpacityLoc = GLES20.glGetUniformLocation(mShader, "shadowOpacity");
        mLutTex = Core.getInstance().loadGLTexture(R.raw.lut_anna, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE, false));

        vboId = new int[3];
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
        "uniform sampler2D lutSampler; \n" +
        "uniform sampler2D circleSampler; \n" +
        "uniform float shadowOpacity; \n" +
        "uniform vec4 color; \n" +
        "uniform vec3 midcolor; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
        "  vec4 shadow = texture2D(textureSampler, tc); \n" +
        "  vec4 circle = texture2D(circleSampler, tc); \n" +
        "  vec4 lutcolor = vec4(midcolor, 1.0); //texture2D(lutSampler, color.rg); \n"+
        "  vec4 outcol = shadow*shadowOpacity + lutcolor*circle.a; \n" +
        "  outcol.a = outcol.a*color.a; \n" +
        "  gl_FragColor = outcol; \n" +
        "}";
}
