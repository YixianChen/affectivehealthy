package se.sics.ah3;

import java.nio.IntBuffer;

import android.graphics.Bitmap;
import android.opengl.GLES20;

public class bitMapThread extends Thread{
	private int w , h;
	private Bitmap bitmap;
	public bitMapThread(int w,int h){
		this.w=w;
		this.h=h;
	}
	
public void run(){
	
	int b[]=new int[w*h];
    int bt[]=new int[w*h];
    IntBuffer ib=IntBuffer.wrap(b);
    ib.position(0);
    GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

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


    bitmap=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
    //ib.clear();
     
	
}
public Bitmap getBitmap(){
	return bitmap;
}
}
