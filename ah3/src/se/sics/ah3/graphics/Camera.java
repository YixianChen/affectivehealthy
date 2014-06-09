package se.sics.ah3.graphics;

import android.opengl.Matrix;
import se.sics.ah3.interaction.Parameters;

public class Camera {
	// camera
	private float[] mProjectionMatrix;
	private float[] mModelViewMatrix;
	private float[] mModelViewProjectionMatrix;
	private float mStartTime;
	private float mEndTime;
	private Parameters mParameters;
	
	private float screenWidth;
	private float screenHeight;
	
	public Camera() {
		mProjectionMatrix = new float[16];
		mModelViewMatrix = new float[16];
		mModelViewProjectionMatrix = new float[16];
		Matrix.setIdentityM(mProjectionMatrix, 0);
		Matrix.setIdentityM(mModelViewMatrix, 0);
		Matrix.setIdentityM(mModelViewProjectionMatrix, 0);
	}

	public void updateUICamera(int width, int height) {
		screenWidth = width;
		screenHeight = height;
		Matrix.setIdentityM(mModelViewProjectionMatrix, 0);
		Matrix.orthoM(mModelViewProjectionMatrix, 0, -1, 1, (float)height/(float)width, -(float)height/(float)width, -1, 1);
	}
			
	public float getHeightScaledCoordinate(float yCoord) {
		return yCoord * screenHeight / screenWidth;
	}
	
	public Parameters getmParameters() {
		return mParameters;
	}

	public void setmParameters(Parameters mParameters) {
		this.mParameters = mParameters;
	}

	public float getmStartTime() {
		return mStartTime;
	}

	public void setmStartTime(float mStartTime) {
		this.mStartTime = mStartTime;
	}

	public float getmEndTime() {
		return mEndTime;
	}

	public void setmEndTime(float mEndTime) {
		this.mEndTime = mEndTime;
	}

	public void updateFrame(Parameters parameters) {
		
	}

	public float[] getmProjectionMatrix() {
		return mProjectionMatrix;
	}

	public void setmProjectionMatrix(float[] mProjectionMatrix) {
		this.mProjectionMatrix = mProjectionMatrix;
	}

	public float[] getmModelViewMatrix() {
		return mModelViewMatrix;
	}

	public void setmModelViewMatrix(float[] mModelViewMatrix) {
		this.mModelViewMatrix = mModelViewMatrix;
	}

	public float[] getmModelViewProjectionMatrix() {
		return mModelViewProjectionMatrix;
	}

	public void setmModelViewProjectionMatrix(float[] mModelViewProjectionMatrix) {
		this.mModelViewProjectionMatrix = mModelViewProjectionMatrix;
	}
	
	public float getScreenWidth() {
		return screenWidth;
	}
	
	public float getScreenHeight() {
		return screenHeight;
	}
}
