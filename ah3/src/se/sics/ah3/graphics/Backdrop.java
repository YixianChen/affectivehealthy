package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Vector;
import java.util.HashMap;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Labels for hours and minutes
 * 
 * @author mareri
 * 
 */

public class Backdrop extends Mesh20 {
	private int mModelViewProjectionLoc;
	private int mOpacityLoc;
	private int mTexSampler;

	int[] mVboTex;

	// square buffers
	private FloatBuffer mSquareVertexBuffer;
	private FloatBuffer mSquareTextureBuffer;

	// textures
	private int mTexBars;

	public Backdrop() {
		super("Backdrop");

		// square bufers
		ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mSquareVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer tcbb = ByteBuffer.allocateDirect(4 * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mSquareTextureBuffer = tcbb.asFloatBuffer();
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		super.draw(Core.SCREEN_SPACE_CAMERA, pick);

		if (!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			// GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE);
			// GLES20.glBlendEquation(GLES20.GL_FUNC_SUBTRACT);
			GLES20.glEnable(GLES20.GL_BLEND);

			GLES20.glUniform1i(mTexSampler, 0);

			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, Core.SCREEN_SPACE_CAMERA.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniform1f(mOpacityLoc, 1);
		}

		if (!pick || mPickable) {
			// draw screen
			renderStuff();

			GLES20.glDisable(GLES20.GL_BLEND);
		}

	}

	private void renderSquare(int texture, float x, float y, float w, float h, float u1, float v1, float u2, float v2, float opacity) {
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

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * vertexcoords.capacity(), vertexcoords, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glEnableVertexAttribArray(0); // vertex
		Core.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * texturecoords.capacity(), texturecoords, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glEnableVertexAttribArray(1);	// texture coords
		Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

		Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, texture);

		GLES20.glUniform1f(mOpacityLoc, opacity);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
	}

	private void renderStuff() {
//		renderSquare(mTexBars, -1,1, 2.0f, 0.8f, 0, 0.5f, 1, 0, 1.0f);
		renderSquare(mTexBars, -1,1, 2.0f, -0.7f, 0, 0, 1, 0.5f, 1.0f);
		renderSquare(mTexBars, -1,-1+0.7f, 2.0f, -0.7f, 0, 0.5f, 1, 1.0f, 1.0f);
	}

	@Override
	public void init() {
		super.init();

		mShader = Core.getInstance().loadProgram("backdrop", kVertexShader, kFragmentShader);
		GLES20.glBindAttribLocation(mShader, 0, "position");
		GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
		GLES20.glLinkProgram(mShader);
		mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
		mOpacityLoc = GLES20.glGetUniformLocation(mShader, "opacity");
		mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");

		mTexBars = Core.getInstance().loadGLTexture(R.raw.background_bars, new Texture2DParameters());

		mVboTex = new int[1];
		GLES20.glGenBuffers(1, mVboTex, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
	}

	private static final String kVertexShader = "precision mediump float; \n"
			+ "attribute vec3 position; \n"
			+ "attribute vec2 textureCoordinates; \n"
			+ "uniform mat4 worldViewProjection; \n"
			+ "uniform float opacity; \n" + "varying vec2 tc; \n"
			+ "void main() { \n" + "  tc = textureCoordinates; \n"
			+ "  gl_Position = vec4(position,1.0); //worldViewProjection * vec4(position, 1.0); \n"
			+ "}";

	private static final String kFragmentShader = "precision mediump float; \n"
			+ "uniform sampler2D textureSampler; \n"
			+ "uniform float opacity; \n"
			+ "varying vec2 tc; \n"
			+ "void main() { \n"
			+
			// "  float t = texture2D(textureSampler, tc).r; \n" +
			"  vec4 col = texture2D(textureSampler, tc); \n"
			+ "  col.a = col.a*opacity; \n"
			+ "  gl_FragColor = col; //vec4(0,0,0,t); \n" + "}";
}
