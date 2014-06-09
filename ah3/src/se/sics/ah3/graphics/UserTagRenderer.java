package se.sics.ah3.graphics;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import se.sics.ah3.graphics.Symbols.Symbol;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import se.sics.ah3.usertags.UserTags;
import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class UserTagRenderer extends Mesh20 {
	private final static int MAX_NUMBER_OF_ENTRIES = 1000;
	
	int mModelViewProjectionLoc;
	int mModelViewLoc;
	int mDrawStyle;
	int mNumberOfEntries;
	int mNumberOfVertsPerPrim;
	Symbol mSymbol;

	private FloatBuffer mTextureCoordBuffer;	
	private FloatBuffer mOpacityBuffer;
	
	private float[] mPositions;
	
	int mTexSampler;
	int[] vboId;
	
	protected ViewPort mViewPort;

	private int[] mVboTex;

	private String[] mTags;

	public UserTagRenderer(ViewPort viewport) {
		super("UserTags");
		setPickable(true);
		
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

		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		mVertexBuffer.position(0);
		
		mPositions = new float[MAX_NUMBER_OF_ENTRIES * 2];
		mTags = new String[MAX_NUMBER_OF_ENTRIES];
		
		mNumberOfEntries = 0;
		
		mDrawStyle = GLES20.GL_TRIANGLES;
		mNumberOfVertsPerPrim = 3;		
	}
	
	public void click(float x, float y, Handler dialogHandler) {
		float nearestDistance = Float.MAX_VALUE;
		int nearestIndex = -1;
		for(int i = 0; i < mNumberOfEntries; i++) {
			//Log.e("click", "tag at " + mPositions[i*2] + ", " + mPositions[i*2+1]);
			float xDiff = x - mPositions[i*2];
			float yDiff = y - mPositions[i*2+1];
			float dist = xDiff*xDiff + yDiff*yDiff;
			if(dist < nearestDistance) {
				nearestIndex = i;
				nearestDistance = dist;
			}
		}
		final String tag = mTags[nearestIndex];
		
		dialogHandler.post(new Runnable() {
			@Override
			public void run() {
				Context context = Core.getInstance().getApplicationContext();
				int duration = Toast.LENGTH_SHORT;
				CharSequence text = tag;
				if(tag.compareTo("") == 0) {
					text = "No tag text entered";
				}
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();				
			}
		});
	}
	
		
	void addEntry(long time, String tag, Camera camera) {		
		float[] point = new float[3];
		float[] normal = new float[3];
		float[] tangent = new float[3];
		
		float t = SpiralFormula.getT(time, mViewPort.getParameters());
		float opacity = 1.0f;
		if(t < 0.01f) {
			opacity = t / 0.01f;
		}
		else if (t > 0.9f) {
			opacity = 1 - (t - 0.9f) / 0.1f;
		}
		t = SpiralFormula.rescaleTToAfterRealtimeSegment(t);
				
		float size = 1.0f;
		float height = mSymbol.height * size;
		float width = mSymbol.width * size;
		
		SpiralFormula.parametricFormulation(t, point);
		normal = SpiralFormula.getNormal(t);
		tangent = SpiralFormula.getTangent(t);
		float thickness = SpiralFormula.getThickness(t);
		
		CoreUtil.normalize(normal);
		CoreUtil.normalize(tangent);

		float[] edge = normal.clone();
		
		CoreUtil.scale(width*0.5f, tangent);
		CoreUtil.scale(height, normal);
		CoreUtil.scale(-thickness, edge);
				
		// v0 -n -t
		mVertexBuffer.put(point[0] + edge[0] + (-normal[0] - tangent[0]));
		mVertexBuffer.put(point[1] + edge[1] + (-normal[1] - tangent[1]));
		mVertexBuffer.put(0f);
		// v1 +n -t
		mVertexBuffer.put(point[0] + edge[0] + (-tangent[0]));
		mVertexBuffer.put(point[1] + edge[1] + (-tangent[1]));
		mVertexBuffer.put(0f);
		// v2 -n +t
		mVertexBuffer.put(point[0] + edge[0] + (-normal[0] + tangent[0]));
		mVertexBuffer.put(point[1] + edge[1] + (-normal[1] + tangent[1]));
		mVertexBuffer.put(0f);
		// v2 -n +t
		mVertexBuffer.put(point[0] + edge[0] + (-normal[0] + tangent[0]));
		mVertexBuffer.put(point[1] + edge[1] + (-normal[1] + tangent[1]));
		mVertexBuffer.put(0f);
		// v1 +n -t
		mVertexBuffer.put(point[0] + edge[0] + (-tangent[0]));
		mVertexBuffer.put(point[1] + edge[1] + (-tangent[1]));
		mVertexBuffer.put(0f);
		// v3 +n +t
		mVertexBuffer.put(point[0] + edge[0] + (tangent[0]));
		mVertexBuffer.put(point[1] + edge[1] + (tangent[1]));
		mVertexBuffer.put(0f);
		
		// get screen coordinates and store them + tags for click handling
		float[] position = {point[0] + edge[0] - normal[0] * 0.5f, point[1] + edge[1] - normal[1] * 0.5f, 0.0f, 0.0f};
		float[] modelViewProjectionMatrix = camera.getmModelViewProjectionMatrix();
		float[] modelMatrix = getTransform();
		
		android.opengl.Matrix.multiplyMM(modelMatrix, 0, modelViewProjectionMatrix, 0, modelMatrix, 0);
		android.opengl.Matrix.multiplyMV(position, 0, modelMatrix, 0, position, 0);
		
		mPositions[mNumberOfEntries * 2 + 0] = (position[0] * 0.5f + 0.5f) * Core.SCREEN_SPACE_CAMERA.getScreenWidth();
		mPositions[mNumberOfEntries * 2 + 1] = (position[1] * -0.5f + 0.5f) * Core.SCREEN_SPACE_CAMERA.getScreenHeight();
		mTags[mNumberOfEntries] = tag;
		
		
		float[] textureCoords = {
				mSymbol.u0, mSymbol.v0,
				mSymbol.u0, mSymbol.v1,
				mSymbol.u1, mSymbol.v0,
				
				mSymbol.u1, mSymbol.v0,
				mSymbol.u0, mSymbol.v1,
				mSymbol.u1, mSymbol.v1
		};
		
		
		for(int i = 0; i < textureCoords.length; i++) {
			mTextureCoordBuffer.put(textureCoords[i]);
		}
		
		for (int i=0;i<6;i++) {
			mOpacityBuffer.put(opacity);
		}
		
		mNumberOfEntries++;
	}
	
	void setPosition(Camera camera) {
		// reset the buffer
		mNumberOfEntries = 0;
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		
		// add data
		Parameters p = mViewPort.getParameters();
		Vector<UserTags.UserTag> userTags = AHState.getInstance().mUserTags.getUserTags(p.start, p.end);

		for(int i = 0; i < userTags.size(); i++) {
			addEntry(userTags.get(i).getTime(), userTags.get(i).getTag(), camera);
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
		if(!mVisible) {
			return;
		}

		super.draw(camera, pick);

		setPosition(camera);
		
		if(!pick) {
//			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);
			
			GLES20.glUniform1i(mTexSampler, 0);
	        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, Symbols.getInstance().getTextureId());
	        
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
		mSymbol = Symbols.getInstance().getSymbol("tag spiral icon");
		
		mShader = Core.getInstance().loadProgram("userTagRenderer", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glBindAttribLocation(mShader, 2, "opacity");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");
        mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
		
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
        "  visibility = opacity; \n" +
        "  tc = textureCoordinates; \n" +
        "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n" +
        "}";

    private static final String kFragmentShader =
        "precision mediump float; \n" +
        "uniform sampler2D textureSampler; \n" +
        "varying vec2 tc; \n" +
        "varying float visibility; \n" +
        "void main() { \n" +
        "  gl_FragColor = texture2D(textureSampler, tc)*visibility;; \n" +
        "}";

}