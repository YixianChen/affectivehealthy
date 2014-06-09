package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.sics.ah3.bitMapThread;
import se.sics.ah3.interaction.Callback;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.interaction.PickHandler;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.share.ScreenShotUtils;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

public class GL20Renderer implements GLSurfaceView.Renderer, PickHandler {
	private FrameTimer mTicker;
	private int mFrameTime;
	private ViewPort mViewPort;
	private Callback mInteractionCallback;
	public Camera mCamera;
	
	private int[] mScreenSize;
	private MeshNode mRoot;
	private Vector<PickItem> mPickList;
	private CoreView mCoreView;
	static Bitmap bitmap = null;
	private static boolean screenShot = false;
	
	public GL20Renderer(MeshNode rootNode, ViewPort viewPort, Callback interactionCallback, CoreView coreView) {
		mTicker = new FrameTimer();
		mScreenSize = new int[2];
		mCamera = new Camera();
		mRoot = rootNode;
		mViewPort = viewPort;
		mInteractionCallback = interactionCallback;
		mCoreView = coreView;
		mPickList = new Vector<GL20Renderer.PickItem>();
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		// proceed with logic
		mFrameTime = mTicker.tick();
		// update interaction
		mInteractionCallback.updateFrame(mFrameTime);
		// clear
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		// get new camera data
		updateCamera();
		Vector<PickItem> pickList = new Vector<PickItem>();
		int n1,n2;
		synchronized(mPickList)
		{
			n1 = mPickList.size();
//			pickList = (Vector<PickItem>)mPickList.clone();
			for(PickItem item : mPickList)
				pickList.add(item);
			n2 = pickList.size();
			mPickList.clear();
		}
		// picking
		if(!pickList.isEmpty()) {
			Log.d("RENDERER", "PickLists: " + n1 + ", " + n2);
			// prepare scene graph
			Core.getInstance().startPicking();
			// build buffer
			mRoot.draw(mCamera, true);
			// list of interaction
			Vector<Callback.ClickData> v = new Vector<Callback.ClickData>();
			// create picked data
			for(PickItem item : pickList) {
				String objectId = Core.getInstance().getPickedObjectName(pick(item.mX, item.mY));
				Callback.ClickData id = new Callback.ClickData();
				id.clickId = item.mId;
				id.objectId = objectId;
				v.add(id);
			}
			// call back interaction
			mInteractionCallback.clicked(v);
			// clear the queue for next frame
//			mPickList.clear();
		}
		// render world!
		mRoot.draw(mCamera, false);
		//bitMapThread t = new bitMapThread(mScreenSize[0],mScreenSize[1]);
		//t.start();
		//bitmap =t.getBitmap();
		if(screenShot){
		bitmap= SavePixels(0, 0, mScreenSize[0], mScreenSize[1]);
		setFlagScreeshot(false);
		}
	}
	public static Bitmap SavePixels(int x, int y, int w, int h)
	{  
	     int b[]=new int[w*(y+h)];
	     int bt[]=new int[w*h];
	     IntBuffer ib=IntBuffer.wrap(b);
	     ib.position(0);
	     GLES20.glReadPixels(x, 0, w, y+h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

	     for(int i=0, k=0; i<h; i++, k++)
	     {//remember, that OpenGL bitmap is incompatible with Android bitmap
	      //and so, some correction need.        
	          for(int j=0; j<w; j++)
	          {
	               int pix=b[i*w+j];
	               int pb=(pix>>16)&0xff;
	               int pr=(pix<<16)&0x00ff0000;
	               int pix1=(pix&0xff00ff00) | pr | pb;
	               bt[(h-k-1)*w+j]=pix1;
	          }
	     }


	     Bitmap sb=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
	     ib.clear();
	     return sb;
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mScreenSize[0] = width;
		mScreenSize[1] = height;
		GLES20.glViewport(0, 0, width, height);
		mInteractionCallback.updateScreenSize(mCamera, width, height);

		// order important, coreview uses SCREEN_SPACE_CAMERA
		Core.SCREEN_SPACE_CAMERA.updateUICamera(width, height);
		mCoreView.setScreenSize(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        //GLES20.glEnable(GLES20.GL_CULL_FACE);
        //GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
		Symbols.getInstance().init();
		mRoot.init();
	}
	
	/**
	 * Picking at the current frame buffer
	 * @return the id at the (x, y)
	 */
	private int pick(int x, int y) {
		ByteBuffer pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
		GLES20.glReadPixels(x, mScreenSize[1] - y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel);
		int result = (int)(pixel.get(0) & 0xff) << 16 | (int)(pixel.get(1) & 0xff) << 8 | (int)(pixel.get(2) & 0xff);
//		Log.v(Core.TAG, "Pick: " + result);
		return result;
	}
	
	private void updateCamera() {
		float[] mvMatrix = new float[16]; 
		float[] mvpMatrix = new float[16];
		float[] pMat = new float[16];

		Matrix.setIdentityM(mvMatrix, 0);
//		Matrix.scaleM(mvMatrix, 0, mvMatrix, 0, 1.20f,1,1);
//		Matrix.translateM(mvMatrix, 0,mvMatrix, 0, -0.095f, -0.11625f*2, -4f);
		Matrix.translateM(mvMatrix, 0,mvMatrix, 0, 0,0, -4f);
		mCamera.setmModelViewMatrix(mvMatrix);

		Parameters parameters = mViewPort.getParameters();
		mCamera.setmParameters(parameters);
		Matrix.setIdentityM(pMat, 0);
		Matrix.orthoM(pMat, 0, parameters.getLeft(), parameters.getRight(), parameters.getDown(), parameters.getUp(), 0.1f, 100f);
		mCamera.setmProjectionMatrix(pMat);
		
		//TODO: Refactor this so this is handled inside the camera class
		Matrix.multiplyMM(mvpMatrix, 0, mCamera.getmProjectionMatrix(), 0, mCamera.getmModelViewMatrix(), 0);
		mCamera.setmModelViewProjectionMatrix(mvpMatrix);
		
		mCamera.setmStartTime(parameters.getStart());
		mCamera.setmEndTime(parameters.getEnd());
	}
	
	@Override
	public void onPick(int id, int x, int y) {
		Log.i(Core.TAG, "onPick : " + x + ":" + y + " id: " + id);
		synchronized(mPickList)
		{
			mPickList.add(new PickItem(id, x, y));
		}
	}
	
	// holder for queue data for picking
	private class PickItem {
		int mX, mY, mId;
		
		PickItem(int id, int x, int y) {
			mId = id;
			mX = x;
			mY = y;
		}
	}
	public static void setBitmap(Bitmap bmp){
		bitmap=bmp;
	}
	public static Bitmap getBitmap(){
		return bitmap;
	}
    public static void setFlagScreeshot(boolean screenshot){
    	screenShot = screenshot;
    }	
	public static boolean getScreenshot()throws Exception{
		return screenShot;
	}
	
	
}
