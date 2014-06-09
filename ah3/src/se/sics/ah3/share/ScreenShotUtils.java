package se.sics.ah3.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import se.sics.ah3.graphics.GL20Renderer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * @author yixian
 * @time 2013/11/5
 */
public class ScreenShotUtils {

	/**
	 * 
	 * @param pActivity
	 * @return bitmap
	 */


	private static Context mContext;
	public static long minSizeSDcard = 50;
	

	public static Bitmap takeScreenShot(Activity pActivity) {
		Bitmap bitmap = null;
		
		View view = pActivity.getWindow().getDecorView();
		view.getRootView();
		view.setDrawingCacheEnabled(true);

	    //view.buildDrawingCache();

		bitmap = view.getDrawingCache();

		Rect frame = new Rect();

		view.getWindowVisibleDisplayFrame(frame);
		int stautsHeight = frame.top;
		Log.d("Yixian ", "the height is :"+stautsHeight);

		int width = pActivity.getWindowManager().getDefaultDisplay().getWidth();
		int height = pActivity.getWindowManager().getDefaultDisplay()
				.getHeight();
        
		bitmap = Bitmap.createBitmap(bitmap, 0, stautsHeight, width, height
				- stautsHeight);
		view.destroyDrawingCache();
		Log.d("Yixian ", "the size of bitmap is :"+ width*(height
				- stautsHeight)/1024);
		return bitmap;
	}
   
//	
//	public static Bitmap SavePixels(int x, int y, int w, int h)
//	{  
//	     int b[]=new int[w*(y+h)];
//	     int bt[]=new int[w*h];
//	     IntBuffer ib=IntBuffer.wrap(b);
//	     ib.position(0);
//	     GLES20.glReadPixels(x, 0, w, y+h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
//
//	     for(int i=0, k=0; i<h; i++, k++)
//	     {//remember, that OpenGL bitmap is incompatible with Android bitmap
//	      //and so, some correction need.        
//	          for(int j=0; j<w; j++)
//	          {
//	               int pix=b[i*w+j];
//	               int pb=(pix>>16)&0xff;
//	               int pr=(pix<<16)&0x00ff0000;
//	               int pix1=(pix&0xff00ff00) | pr | pb;
//	               bt[(h-k-1)*w+j]=pix1;
//	          }
//	     }
//
//
//	     Bitmap sb=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
//	     ib.clear();
//	     return sb;
//	}
	
	
	
	
	private static boolean savePic(String strName){
		FileOutputStream fos = null;
		//Bitmap bmp=SavePixels(x,y,w,h);
		Bitmap bmp=null;
		while(bmp==null){
			bmp =GL20Renderer.getBitmap();
		
		if(bmp==null)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		GL20Renderer.setBitmap(null);
		try {
			if (!SDcardAavaliable(mContext)) 
				fos = mContext.getApplicationContext().openFileOutput(strName, Context.MODE_PRIVATE);
			 else
				 fos = new FileOutputStream("sdcard/"+strName);
			if (null != fos && null != bmp ) {
				bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
				fos.flush();
				fos.close();
				return true;
			}
			else 
				return false;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean SDcardAavaliable(Context context) {
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
		System.out.println("+++++++++++++++" + sdCardExist);
		if (sdCardExist) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			long sdCardSize = (availableBlocks * blockSize) / 1024;
			if (sdCardSize > minSizeSDcard) {
				System.out.println("SDcardSize:::" + minSizeSDcard + "KB");
				return true;
			} else {
				System.out.println("There is no enough space to store!!!");
			}
		} else {
			System.out.println("There is no enough space to store!!!");
		}
		return false;
	}

	public static boolean shotBitmap() {
		
		
			return savePic("temp.png"
					);
		
	}





	







}
