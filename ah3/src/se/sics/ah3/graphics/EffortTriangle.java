package se.sics.ah3.graphics;

import static android.opengl.GLES10.GL_LINEAR;
import static android.opengl.GLES10.GL_NEAREST;
import static android.opengl.GLES10.GL_REPEAT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import se.sics.ah3.R;
import android.graphics.PointF;
import android.opengl.GLES20;

public class EffortTriangle extends Mesh20 {
	private final static int VERTS = 4;
	
	// test geometry
	int mTex;
	int mTexSampler;
	int[] vboId;
	int mColorLoc;
	int mModelViewProjectionLoc;
	float[] color;
	
	int mTexturePath;
	int mDrawStyle;
	
	// 480/800
	// 0.9375/0.78125

	private FloatBuffer mTextureCoordBuffer;
	
	public EffortTriangle() {
		super("EffortTriangle");
		mTexturePath = R.raw.triangle; //R.raw.lut;

		float centerPosX = 0.0f;
		float centerPosY = 0.0f;
		float width = 0.5f;
		float height = 0.15f;
//		PointF tangent = new PointF(0.5000000000000001f, 0.8660254037844386f); //-1/1.4142135623731f,1/1.4142135623731f);
		PointF tangent = new PointF(1.0f, 0.0f); //-1/1.4142135623731f,1/1.4142135623731f);
		
		float[] vertices = {
				centerPosX - width * 0.5f, centerPosY, 1.0f,
				centerPosX - height * 0.5f*tangent.x, centerPosY - height * 0.5f*tangent.y, 1.0f,
				centerPosX - width * 0.5f, centerPosY, 1.0f,
				centerPosX + height * 0.5f*tangent.x, centerPosY + height * 0.5f*tangent.y, 1.0f 
		};
		
		float[] textureCoords = {
				0.5f, 0.0f,
				0.0f, 1.0f,
				0.5f,  0.0f,
				1.0f,  1.0f
		};
		
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
		
		color = new float[4];
		//setColor(0.5f, 0.7f, 0.6f, 1f);
		setColor(0.0f,0.0f,0.0f,1.0f);
	}
	
	void setPosition(float x, float y) {
		mDrawStyle = x < 0f ? GLES20.GL_LINE_LOOP : GLES20.GL_TRIANGLE_STRIP;
		mVertexBuffer.put(0, x);
		mVertexBuffer.put(1, y);
		mVertexBuffer.put(6, x);
		mVertexBuffer.put(7, y);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_STATIC_DRAW);
	}
	
	public void setColor(float r, float g, float b, float a) {
		color[0] = r;
		color[1] = g;
		color[2] = b;
		color[3] = a;
	}
	
	float[] getColor() {
		return color;
	}
	
	@Override
	public void draw(Camera camera, boolean pick) {
		if(!mVisible) {
			return;
		}
		super.draw(camera, pick);
		
		if(!pick) {
			if (mDrawStyle==GLES20.GL_TRIANGLE_STRIP) {
				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
				GLES20.glEnable(GLES20.GL_BLEND);
			}

			GLES20.glUseProgram(mShader);
			GLES20.glUniform1i(mTexSampler, 0);
	        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTex);
	        GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
			
	        GLES20.glUniform4f(mColorLoc, color[0], color[1], color[2], color[3]);
	        
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
		}
	
		if(!pick || mPickable) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
			GLES20.glEnableVertexAttribArray(Core.VERTEX);
			Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);

			GLES20.glDrawArrays(mDrawStyle, 0, VERTS);
		}
		if (mDrawStyle==GLES20.GL_TRIANGLE_STRIP) {
			GLES20.glDisable(GLES20.GL_BLEND);
		}
	}
	
	@Override
	public void init() {
		super.init();
        mShader = Core.getInstance().loadProgram("effortTriangle", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glLinkProgram(mShader);
        mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
        mTex = Core.getInstance().loadGLTexture(mTexturePath, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
        mColorLoc = GLES20.glGetUniformLocation(mShader, "color");
        mModelViewProjectionLoc =
            GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        
        vboId = new int[2];
        GLES20.glGenBuffers(2, vboId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_STATIC_DRAW);
	
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_STATIC_DRAW);
        mDrawStyle = GLES20.GL_TRIANGLE_STRIP;
	}
	
	private static final String kVertexShader =
        "precision mediump float; \n" +
        "attribute vec3 position; \n" +
        "attribute vec2 textureCoordinates; \n" +
        "uniform mat4 worldViewProjection; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
        "  tc = textureCoordinates; \n" +
        "  gl_Position = worldViewProjection * vec4(position, 1.0); \n" +
        "}";

    private static final String kFragmentShader =
        "precision mediump float; \n" +
        "uniform sampler2D textureSampler; \n" +
        "uniform vec4 color; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
        "  gl_FragColor = texture2D(textureSampler, tc); \n" +
        "}";
}


