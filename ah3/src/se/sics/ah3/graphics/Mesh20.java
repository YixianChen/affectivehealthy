package se.sics.ah3.graphics;

import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class Mesh20 {
	// transformation
	private float[] mTranslate,mScale;
	private float[] mTransform;
	
	private MeshNode mParent = null;
	
	// id, package visibility
	String name;
	
	// shader
	int mShader;
	int mPickShader;
	int mPickModelViewProjectionLoc;
	int mPickModelViewLoc;	
	int mPickIdLoc;
	int[] mVboGeometry;
	
	// geometry
	FloatBuffer mVertexBuffer;
	
	// texture
	FloatBuffer[] mTextureBuffer;
	
	// visibility
	boolean mVisible;
	boolean mPickable;

	private void makeTransform() {
		Matrix.setIdentityM(mTransform, 0);
		Matrix.translateM(mTransform, 0, mTranslate[0],mTranslate[1],mTranslate[2]);
		Matrix.scaleM(mTransform, 0, mScale[0],mScale[1],mScale[2]);
	}
	public void translate(float x, float y, float z) {
		mTranslate[0] = x; mTranslate[1] = y; mTranslate[2] = z;
//		Matrix.translateM(mTransform,0,x,y,z);
		makeTransform();
	}
	
	public float[] getTranslation() {
		return mTranslate;
	}
	
	public void scale(float x, float y, float z) {
		mScale[0] = x; mScale[1] = y; mScale[2] = z;
		makeTransform();
	}
	
	public final float[] getTransform() {
		if (mParent!=null) {
			float[] result = new float[16];
			Matrix.multiplyMM(result, 0, mParent.traverseTransform(), 0, mTransform, 0);
			return result;
		} else {
			return mTransform;
		}
	}

	public MeshNode getParent() {
		return mParent;
	}
	
	public void setParent(MeshNode node) {
		mParent = node;
	}

	public boolean isPickable() {
		return mPickable;
	}
	
	public void setPickable(boolean pickable) {
		mPickable = pickable;
	}
	
	public boolean isVisible() {
		return mVisible;
	}

	public void setVisible(boolean visible) {
		mVisible = visible;
	}

	public Mesh20() {
		name = new String();
		mVisible = true;
		mTransform = new float[16];
		Matrix.setIdentityM(mTransform, 0);
		mTranslate = new float[3];
		mScale = new float[3];
		mScale[0] = mScale[1] = mScale[2] = 1f;
	}
	
	public Mesh20(String id) {
		name = id;
		mVisible = true;
		mTransform = new float[16];
		Matrix.setIdentityM(mTransform, 0);
		mTranslate = new float[3];
		mScale = new float[3];
		mScale[0] = mScale[1] = mScale[2] = 1f;
	}
	
	public void setId(String id) {
		name = id;
	}
	
	public void draw(Camera camera, boolean pick) {
		if(mVisible) {
			if(!pick || !mPickable) {
				GLES20.glUseProgram(mShader);
			} else {
				GLES20.glUseProgram(mPickShader);
				int pickId = Core.getInstance().getPickId(name);
				float b = (pickId & 0xff) / 255.0f;
				float g = ((pickId & 0xff00) >> 8) / 255.0f;
				float r = ((pickId & 0xff0000) >> 16) / 255.0f;
				GLES20.glUniform4f(mPickIdLoc, r, g, b, 1f);
				GLES20.glUniformMatrix4fv(mPickModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
		        GLES20.glUniformMatrix4fv(mPickModelViewLoc, 1, false, getPickTransform(),0);
			}
		}
	}
	
	public float[] getPickTransform() {
		return getTransform();
	}
	
	public void init() {
		mPickShader = Core.getInstance().loadProgram("Mesh20PickShader", kpickVertexShader, kpickFragmentShader);
        GLES20.glBindAttribLocation(mPickShader, 0, "position");
        GLES20.glLinkProgram(mPickShader);
        mPickIdLoc = GLES20.glGetUniformLocation(mPickShader, "id");
        
        mPickModelViewProjectionLoc = GLES20.glGetUniformLocation(mPickShader, "worldViewProjection");
		mPickModelViewLoc = GLES20.glGetUniformLocation(mPickShader, "modelView");

        mVboGeometry = new int[1];
        GLES20.glGenBuffers(1, mVboGeometry, 0);
	}
	
	public String getName() {
		return name;
	}
	
	private static final String kpickVertexShader =
        "precision mediump float; \n" +
        "uniform mat4 worldViewProjection; \n" +
		"uniform mat4 modelView; \n" +
        "attribute vec3 position; \n" +
        "void main() { \n" +
        "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n" +
        "}";

    private static final String kpickFragmentShader =
        "precision mediump float; \n" +
        "uniform vec4 id; \n" +
        "void main() { \n" +
        "  gl_FragColor = id; \n" +
        "}";
}
