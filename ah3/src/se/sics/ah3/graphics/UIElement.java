package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class UIElement extends Mesh20 {
	private final static int VERTS = 4;

	// test geometry
	int mTexSampler;
	int[] vboId;
	int mColorLoc;
	int mModelViewProjectionLoc;
	int mModelViewLoc;
	int mOpacityLoc;

	float[] color;
	private float mOpacity = 1.0f;

	float mPosX;
	float mPosY;

	float mXOffset;

	// 480/800
	// 0.9375/0.78125

	String mSymbolName;
	private FloatBuffer mTextureCoordBuffer;

	public static final int ALIGN_LEFT = 1;
	public static final int ALIGN_CENTER = 0;
	public static final int ALIGN_RIGHT = -1;

	private int mAlignX = ALIGN_CENTER;

	private float mPickScaleX = 1.0f;
	private float mPickScaleY = 2.0f;
	
	private boolean mUseScreenSpaceCamera = true;
	
	public UIElement(String id, String symbolName, float posX, float posY,
			int alignX, boolean useScreenSpaceCamera) {
		super(id);
		setPickable(true);
		mSymbolName = symbolName;
		mPosX = posX;
		mPosY = posY;
		mXOffset = 0.0f;
		mAlignX = alignX;
		mUseScreenSpaceCamera = useScreenSpaceCamera;
	}

	public void setColor(float r, float g, float b, float a) {
		color[0] = r;
		color[1] = g;
		color[2] = b;
		color[3] = a;
	}

	public void setOpacity(float opacity) {
		mOpacity = opacity;
	}

	float[] getColor() {
		return color;
	}

	@Override
	public void draw(Camera camera, boolean pick) {

		if (!mVisible) {
			return;
		}
		
		if(mUseScreenSpaceCamera) {
			camera = Core.SCREEN_SPACE_CAMERA;
		}
		
		super.draw(camera, pick);

		if (!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
					GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);

			GLES20.glUniform1i(mTexSampler, 0);
			Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D,
					Symbols.getInstance().getTextureId());

			GLES20.glUniform4f(mColorLoc, color[0], color[1], color[2],
					color[3]);
			GLES20.glUniform1f(mOpacityLoc, mOpacity);

			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false,
					camera.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(),
					0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
		}

		if (!pick || mPickable) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
			GLES20.glEnableVertexAttribArray(Core.VERTEX);
			Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false,
					0, 0);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTS);

			GLES20.glDisable(GLES20.GL_BLEND);
		}
	}

	public void setPickScale(float x, float y) {
		mPickScaleX = x;
		mPickScaleY = y;
	}

	// let's oversize the pick area
	@Override
	public float[] getPickTransform() {
		float[] transform = getTransform().clone();
		Matrix.scaleM(transform, 0, mPickScaleX, mPickScaleY, 1.0f);
		return transform;
	}

	@Override
	public void translate(float x, float y, float z) {
		super.translate(mPosX + mXOffset + x, y + mPosY, z);
	}

	@Override
	public void init() {
		super.init();

		Symbols.Symbol symbol = Symbols.getInstance().getSymbol(mSymbolName);
		mShader = Core.getInstance().loadProgram("UIElement", kVertexShader, kFragmentShader);

		mXOffset = symbol.width * 0.5f * mAlignX;

		float[] vertices = { -symbol.width * 0.5f, -symbol.height * 0.5f, 0.0f,
				+symbol.width * 0.5f, -symbol.height * 0.5f, 0.0f,
				-symbol.width * 0.5f, +symbol.height * 0.5f, 0.0f,
				+symbol.width * 0.5f, +symbol.height * 0.5f, 0.0f };

		super.translate(mPosX + mXOffset, mPosY, 1.0f);

		float[] textureCoords = { symbol.u0, symbol.v0, symbol.u1, symbol.v0,
				symbol.u0, symbol.v1, symbol.u1, symbol.v1 };

		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();

		for (int i = 0; i < VERTS * 3; i++) {
			mVertexBuffer.put(vertices[i]);
		}

		ByteBuffer tcbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mTextureCoordBuffer = tcbb.asFloatBuffer();

		for (int i = 0; i < VERTS * 2; i++) {
			mTextureCoordBuffer.put(textureCoords[i]);
		}

		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);

		color = new float[4];
		setColor(1f, 1f, 1f, 1f);

		GLES20.glBindAttribLocation(mShader, 0, "position");
		GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
		GLES20.glLinkProgram(mShader);
		mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
		mColorLoc = GLES20.glGetUniformLocation(mShader, "color");
		mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
		mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");
		mOpacityLoc = GLES20.glGetUniformLocation(mShader, "opacity");

		vboId = new int[2];
		GLES20.glGenBuffers(2, vboId, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mVertexBuffer.capacity(), mVertexBuffer,
				GLES20.GL_STATIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId[1]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer,
				GLES20.GL_STATIC_DRAW);
	}

	private static final String kVertexShader = "precision mediump float; \n"
			+ "attribute vec3 position; \n"
			+ "attribute vec2 textureCoordinates; \n"
			+ "uniform mat4 worldViewProjection; \n"
			+ "uniform mat4 modelView; \n"
			+ "uniform float opacity; \n"
			+ "varying vec2 tc; \n"
			+ "void main() { \n"
			+ "  tc = textureCoordinates; \n"
			+ "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n"
			+ "}";

	private static final String kFragmentShader = "precision mediump float; \n"
			+ "uniform sampler2D textureSampler; \n" + "uniform vec4 color; \n"
			+ "uniform float opacity; \n" + "varying vec2 tc; \n"
			+ "void main() { \n"
			+ "  vec4 col = texture2D(textureSampler, tc); \n"
			+ "  col.a = col.a * opacity; \n" + "  gl_FragColor = col; \n"
			+ "}";
}
