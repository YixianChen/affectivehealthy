package se.sics.ah3.graphics;

import android.opengl.GLES20;

/**
 * Container object for creation of texture objects
 * @author mareri
 *
 */

public class Texture2DParameters {
	private int mMinFilter;
	private int mMagFilter;
	private int mWrapS;
	private int mWrapT;
	private boolean mMipMap;
	
	public Texture2DParameters() {
		mMinFilter = GLES20.GL_LINEAR;
		mMagFilter = GLES20.GL_LINEAR;
		mWrapS = GLES20.GL_CLAMP_TO_EDGE;
		mWrapT = GLES20.GL_CLAMP_TO_EDGE;
		mMipMap = false;
	}
	
	public Texture2DParameters(int minFilter, int magFilter, int wrapS, int wrapT, boolean mipMap) {
		mMinFilter = minFilter;
		mMagFilter = magFilter;
		mWrapS = wrapS;
		mWrapT = wrapT;
		mMipMap = mipMap;
	}

	public int getmMinFilter() {
		return mMinFilter;
	}

	public int getmMagFilter() {
		return mMagFilter;
	}

	public int getmWrapS() {
		return mWrapS;
	}

	public int getmWrapT() {
		return mWrapT;
	}
	
	public boolean getMipMap() {
		return mMipMap;
	}
}
