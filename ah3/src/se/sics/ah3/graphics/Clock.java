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

public class Clock extends Mesh20 {
	int mModelViewProjectionLoc;
	int mModelViewLoc;
	int mOpacityLoc;
	int mTexId, mTexSampler;
	int[] mVboTex;
	int[] mVboDays;

	// square buffers
	private FloatBuffer mSquareVertexBuffer;
	private FloatBuffer mSquareTextureBuffer;

	// textures
	int mTexNumbers;
//	int mTexDays;

	// chars
	class Char {
		int texId;
		char c;
		float u0, v0, u1, v1;
		float width;

		public Char(int tid, char c, float width, float u0, float v0, float u1,
				float v1) {
			texId = tid;
			this.c = c;
			this.width = width;
			this.u0 = u0;
			this.v0 = v0;
			this.u1 = u1;
			this.v1 = v1;
		}
	}

	HashMap<String, Char> mCharMappings = new HashMap<String, Char>();

	Vector<UV> uvs;
	Vector<UV> uvsDay;
	float[] normal = { 0f, 1f, 0f };
	float[] tangent = { 1f, 0f, 0f };

	float textWidth = 0.07f; // 0.15f/3.0f;
	float textHeight = 0.05f; // 0.1f/3.0f;

	public Clock() {
		super("Timespan");

		// square bufers
		ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mSquareVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer tcbb = ByteBuffer.allocateDirect(4 * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mSquareTextureBuffer = tcbb.asFloatBuffer();

		// texture atlas coordinates
		uvs = new Vector<Clock.UV>();
		uvs.add(new UV(0f, 0.25f, 0.0f, 0.25f)); // 0
		uvs.add(new UV(0.25f, 0.5f, 0.0f, 0.25f)); // 1
		uvs.add(new UV(0.5f, 0.75f, 0.0f, 0.25f)); // 2
		uvs.add(new UV(0.75f, 1.0f, 0.0f, 0.25f)); // 3
		uvs.add(new UV(0f, 0.25f, 0.25f, 0.5f)); // 4
		uvs.add(new UV(0.25f, 0.5f, 0.25f, 0.5f)); // 5
		uvs.add(new UV(0.5f, 0.75f, 0.25f, 0.5f)); // 6
		uvs.add(new UV(0.75f, 1.0f, 0.25f, 0.5f)); // 7
		uvs.add(new UV(0f, 0.25f, 0.5f, 0.75f)); // 8
		uvs.add(new UV(0.25f, 0.5f, 0.5f, 0.75f)); // 9
		uvs.add(new UV(0.5f, 0.75f, 0.5f, 0.75f)); // .
		uvs.add(new UV(0.75f, 1.0f, 0.5f, 0.75f)); // -
		uvs.add(new UV(0f, 0.25f, 0.75f, 1.0f)); // blank

		uvsDay = new Vector<Clock.UV>();
		uvsDay.add(new UV(-0.05f, 0.5f, 0.0f, 0.135f)); // Mon
		uvsDay.add(new UV(-0.05f, 0.5f, 0.12f, 0.265f)); // Tue
		uvsDay.add(new UV(-0.05f, 0.55f, 0.25f, 0.39f)); // Wed
		uvsDay.add(new UV(-0.05f, 0.5f, 0.38f, 0.525f)); // Thu
		uvsDay.add(new UV(-0.05f, 0.5f, 0.51f, 0.65f)); // Fri
		uvsDay.add(new UV(-0.05f, 0.5f, 0.64f, 0.78f)); // Sat
		uvsDay.add(new UV(-0.05f, 0.5f, 0.77f, 0.905f)); // Sun

		CoreUtil.scale(textHeight, normal);
		CoreUtil.scale(textWidth, tangent);
	}

	private String zeroPadTime(int v) {
		return v < 10 ? "0" + v : "" + v;
	}


	@Override
	public void draw(Camera camera, boolean pick) {
		super.draw(Core.SCREEN_SPACE_CAMERA, pick);

		if (!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
					GLES20.GL_ONE_MINUS_SRC_ALPHA);
			// GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE);
			// GLES20.glBlendEquation(GLES20.GL_FUNC_SUBTRACT);
			GLES20.glEnable(GLES20.GL_BLEND);

			GLES20.glUniform1i(mTexSampler, 0);
			Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTexId);

			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);
			GLES20.glUniform1f(mOpacityLoc, 1);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
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
		texturecoords.put(v2);

		vertexcoords.put(x + w);
		vertexcoords.put(y);
		vertexcoords.put(0);
		texturecoords.put(u2);
		texturecoords.put(v2);

		vertexcoords.put(x + w);
		vertexcoords.put(y + h);
		vertexcoords.put(0);
		texturecoords.put(u2);
		texturecoords.put(v1);

		vertexcoords.put(x);
		vertexcoords.put(y + h);
		vertexcoords.put(0);
		texturecoords.put(u1);
		texturecoords.put(v1);

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

	private float getNumCharWidth(String ch) {
		return mCharMappings.containsKey(ch) ? (mCharMappings.get(ch).width / 20f) * textWidth : (textWidth * 0.25f);
	}

	private void charOut(String ch, float x, float y, float heightFactor, float opacity) {
		Char c = mCharMappings.get(ch);
		if (c == null) return;
		float width = heightFactor * (c.width / 20f) * textWidth;
		renderSquare(c.texId, x, y, width, textHeight * heightFactor, c.u0, c.v0, c.u1, c.v1, opacity);
	}

	private String getMonthString(int m) {
		String[] ms = new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
		return ms[m % ms.length];
	}
	private String getSpecialText(char c) {
		switch (c) {
		case '&': return "and";
		case 'Y': return "year";
		}
		return " ";
	}

	private void print(String s, float x, float y, float heightFactor, float opacity) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			String ch = "" + c;
			float kLeft = 0f;
			float kRight = 0f;
			if (c == '%') { // months
				i++;
				c = s.charAt(i);
				ch = getMonthString(c - 48);
			} else if (c=='$') {
				i++;
				c = s.charAt(i);
				ch = getSpecialText(c);
			} else { // kern certain chars
				if (c=='/') { kLeft = 0.2f; kRight = 0.2f; }
				else if (c=='.') { kLeft = 0f; kRight = 0.05f; }
			}
			float width = getNumCharWidth(ch) * heightFactor;
			charOut(ch, x-kLeft*textWidth*heightFactor, y, heightFactor, opacity);
			x += width-kLeft*textWidth*heightFactor-kRight*textWidth*heightFactor;
		}
	}

	private void renderStuff() {
		// create vertex buffer
		// renderSquare(mTexId, -0.9f, 0.0f, 1.0f, 1.0f, 0, 1, 1, 0);

		Date date = new Date();
		float textOpacity = 0.8f;

		float lineHeight = textHeight*1.2f;

		String s = ""
				+ zeroPadTime(date.getHours()) + ":" + zeroPadTime(date.getMinutes());

		print(s, -0.2f, 0.0f, 1.0f, 0.7f);
	}

	@Override
	public void init() {
		super.init();

		mShader = Core.getInstance().loadProgram("clock", kVertexShader, kFragmentShader);
		GLES20.glBindAttribLocation(mShader, 0, "position");
		GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
		GLES20.glLinkProgram(mShader);
		mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
		mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");
		mOpacityLoc = GLES20.glGetUniformLocation(mShader, "opacity");
		mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");

/*		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mVertexBuffer.capacity(), mVertexBuffer,
				GLES20.GL_DYNAMIC_DRAW);*/

		mTexId = Core.getInstance().loadGLTexture(R.raw.numbers, new Texture2DParameters());
//		mTexDays = Core.getInstance().loadGLTexture(R.raw.days,
//				new Texture2DParameters());

		mTexNumbers = Core.getInstance().loadGLTexture(R.raw.numbers_helv, new Texture2DParameters());

		mCharMappings.put("0", new Char(mTexNumbers, '0', 15, 185 / 200f, 0.5f,
				1, 0));
		mCharMappings.put("1", new Char(mTexNumbers, '1', 12, 5 / 200f, 0.5f,
				17 / 200f, 0));
		mCharMappings.put("2", new Char(mTexNumbers, '2', 17, 22 / 200f, 0.5f,
				39 / 200f, 0));
		mCharMappings.put("3", new Char(mTexNumbers, '3', 17, 42 / 200f, 0.5f,
				59 / 200f, 0));
		mCharMappings.put("4", new Char(mTexNumbers, '4', 15, 63 / 200f, 0.5f,
				78 / 200f, 0));
		mCharMappings.put("5", new Char(mTexNumbers, '5', 16, 83 / 200f, 0.5f,
				99 / 200f, 0));
		mCharMappings.put("6", new Char(mTexNumbers, '6', 16, 103 / 200f, 0.5f,
				119 / 200f, 0));
		mCharMappings.put("7", new Char(mTexNumbers, '7', 14, 126 / 200f, 0.5f,
				140 / 200f, 0));
		mCharMappings.put("8", new Char(mTexNumbers, '8', 14, 145 / 200f, 0.5f,
				159 / 200f, 0));
		mCharMappings.put("9", new Char(mTexNumbers, '9', 14, 165 / 200f, 0.5f,
				179 / 200f, 0));
		mCharMappings.put(":", new Char(mTexNumbers, ':', 4, 22 / 200f, 1,
				26 / 200f, 0.5f));
		mCharMappings.put(".", new Char(mTexNumbers, '.', 4, 22 / 200f, 1,
				26 / 200f, 0.5f));
		mCharMappings.put("/", new Char(mTexNumbers, '/', 13, 5 / 200f, 1,
				18 / 200f, 0.5f));

		mVboTex = new int[1];
		GLES20.glGenBuffers(1, mVboTex, 0);
	}

	private static final String kVertexShader = "precision mediump float; \n"
			+ "attribute vec3 position; \n"
			+ "attribute vec2 textureCoordinates; \n"
			+ "uniform mat4 worldViewProjection; \n"
			+ "uniform mat4 modelView; \n"
			+ "uniform float opacity; \n" + "varying vec2 tc; \n"
			+ "void main() { \n"
			+ "  tc = textureCoordinates; \n"
			+ "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n"
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
