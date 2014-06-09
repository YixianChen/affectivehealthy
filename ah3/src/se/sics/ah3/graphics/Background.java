package se.sics.ah3.graphics;

import static android.opengl.GLES10.GL_LINEAR;
import static android.opengl.GLES10.GL_NEAREST;
import static android.opengl.GLES10.GL_REPEAT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import se.sics.ah3.R;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class Background extends Mesh20 {
	private final static int VERTS = 4;

	float parallaxScale = 0.1f;

	// square buffers
	private FloatBuffer mSquareVertexBuffer;
	private FloatBuffer mSquareTextureBuffer;

	// test geometry
	int mTex;
	int mTexBars;
	int mTexSampler;
	int[] vboId;
	int mParallaxLoc;

	// 480/800
	// 0.9375/0.78125
	
	private FloatBuffer mTextureCoordBuffer;
	
	public Background() {
		super("background");
		
		float w = 1f;
		float h = 1f;
		float[] vertices = {
				// X, Y, Z
				/*-1.5f, -1.8f, 1.0f,
				 1.5f, -1.8f, 1.0f,
				-1.5f,  1.8f, 1.0f,
				 1.5f,  1.8f, 1.0f*/
				-w, -h, 1.0f,
				 w, -h, 1.0f,
				-w,  h, 1.0f,
				 w,  h, 1.0f};
		
		/*float[] textureCoords = {
				// X, Y, Z
				0.0f, 0.0f,
				 0.9375f, 0.0f,
				0.0f,  0.78125f,
				 0.9375f,  0.78125f };*/
		
		// mock for non parallax texture
		float[] textureCoords = {
				// u,v
				0,1,
				 1,1,
				0,0,
				1,0
				/*0.05f, 0.75f,
				 0.95f, 0.75f,
				0.05f,  0.025f,
				0.95f,  0.025f*/
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

		// square buffers
		vbb = ByteBuffer.allocateDirect(4 * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mSquareVertexBuffer = vbb.asFloatBuffer();

		tcbb = ByteBuffer.allocateDirect(4 * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mSquareTextureBuffer = tcbb.asFloatBuffer();
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		if(!mVisible || (pick && !mPickable)) {
			return;
		}
		GLES20.glUseProgram(mShader);
		
		// parallax effect
		float px = 0f;//camera.getmParameters().left + camera.getmParameters().right;
		float py = 0f;//camera.getmParameters().up + camera.getmParameters().down;
		//GLES20.glUniform2f(mParallaxLoc, parallaxScale * px, -parallaxScale * py);
		
		GLES20.glUniform1i(mTexSampler, 0);
        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTex);
        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[0]);
		GLES20.glEnableVertexAttribArray(Core.VERTEX);
		Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
		GLES20.glEnableVertexAttribArray(1);
		Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
	
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTS);
		
		// rectangles for top and bottom.
		//renderSquare(mTex, -1,-1,2,0.8f,0,0.5f,1,0);
		//renderSquare(mTexBars, -1,-1,2,0.8f,0,0.5f,1,0);
	}
	
	private void renderSquare(int texture, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
		FloatBuffer vertexcoords = mSquareVertexBuffer;
		FloatBuffer texturecoords = mSquareTextureBuffer;

		vertexcoords.put(x);
		vertexcoords.put(y);
		vertexcoords.put(0);
		texturecoords.put(u1);
		texturecoords.put(v1);

		vertexcoords.put(x + w);
		vertexcoords.put(y);
		vertexcoords.put(0);
		texturecoords.put(u2);
		texturecoords.put(v1);

		vertexcoords.put(x + w);
		vertexcoords.put(y + h);
		vertexcoords.put(0);
		texturecoords.put(u2);
		texturecoords.put(v2);

		vertexcoords.put(x);
		vertexcoords.put(y + h);
		vertexcoords.put(0);
		texturecoords.put(u1);
		texturecoords.put(v2);

		vertexcoords.position(0);
		texturecoords.position(0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * vertexcoords.capacity(), vertexcoords, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glEnableVertexAttribArray(0); // vertex
		Core.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * texturecoords.capacity(), texturecoords, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glEnableVertexAttribArray(1);	// texture coords
		Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

		Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, texture);
//		GLES20.glUniform1f(mOpacityLoc, opacity);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
	}
	
	@Override
	public void init() {
        mShader = Core.getInstance().loadProgram("background", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glLinkProgram(mShader);
        mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
//        mTex = Core.getInstance().loadGLTexture(R.raw.background_new_test, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
//        mTex = Core.getInstance().loadGLTexture(R.raw.background, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
        mTex = Core.getInstance().loadGLTexture(R.raw.background_shadow, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
        mTexBars = Core.getInstance().loadGLTexture(R.raw.background_bars, new Texture2DParameters(GL_NEAREST, GL_LINEAR, GL_REPEAT, GL_REPEAT, false));
        mParallaxLoc =
            GLES20.glGetUniformLocation(mShader, "parallax");
        
        vboId = new int[2];
        GLES20.glGenBuffers(2, vboId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_DYNAMIC_DRAW);
	
	}
	
	private static final String kVertexShader =
        "precision mediump float; \n" +
        "uniform vec2 parallax; \n" +
        "attribute vec3 position; \n" +
        "attribute vec2 textureCoordinates; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
        "  tc = textureCoordinates; \n" +
        "  gl_Position = vec4(position + vec3(parallax, 0.0), 1.0); \n" +
        "}";

    private static final String kFragmentShader =
        "precision mediump float; \n" +
        "uniform sampler2D textureSampler; \n" +
        "varying vec2 tc; \n" +
        "void main() { \n" +
//        "  gl_FragColor = texture2D(textureSampler, tc); \n" +
//		"  gl_FragColor = vec4(0.6,0.6,0.6,1.0); \n" +
		"  gl_FragColor = vec4(1.0,1.0,1.0,1.0); \n" +
        "}";
}


