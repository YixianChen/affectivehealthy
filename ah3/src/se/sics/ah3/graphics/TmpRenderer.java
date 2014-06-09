package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.sics.ah3.AHState;
import se.sics.ah3.interaction.Callback;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.interaction.PickHandler;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.usertags.UserTags;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

public class TmpRenderer implements GLSurfaceView.Renderer, PickHandler {
	// config
	private ViewPort mViewPort;
	private Callback mController;

	// state
	private int mWidth, mHeight;

	// shaders
	private int mShaderProgram;
	private int maPosition;
	private int muColor;
	private int muMVPMatrix;
	
	// render data
	private int mNumPoints = 1000;
	private float[] mCoords;
	private MeshData mMesh;	// will become a nice little VarMeshData
	private MeshData mTriangle = new MeshData();
	private MeshData mSquare = new SquareMesh();

	public class MeshData {
		private FloatBuffer vertexBuffer;

		public MeshData()
		{
			final float triangleCoords[] = {
				-0.5f, -0.25f, 0,
				0.5f, -0.25f, 0,
				0, 0.559f, 0
			};
			setVertexBuffer(triangleCoords);
		}
		
		public void setVertexBuffer(final float[] coords)
		{
			ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * (Float.SIZE/8));
			bb.order(ByteOrder.nativeOrder());
			vertexBuffer = bb.asFloatBuffer();
			vertexBuffer.put(coords);
			vertexBuffer.position(0);
		}
		
		public void updateVertices(final float[] coords)
		{
			vertexBuffer.position(0);
			vertexBuffer.clear();
			vertexBuffer.put(coords);
			vertexBuffer.position(0);
		}
	}

	public class SquareMesh extends MeshData {
		public SquareMesh()
		{
			final float squareCoords[] = {
				1.0f, 1.0f, 0,
				-1.0f, 1.0f, 0,
				-1.0f, -1.0f, 0,
				1.0f, -1.0f, 0,
				1.0f, 1.0f, 0		/* to make lines */
			};
			setVertexBuffer(squareCoords);
		}
	}

	public class VarMeshData extends MeshData {
		public VarMeshData(int size)
		{
			float[] data = new float[size*3];
			for (int i=0;i<size*3;i++) data[i] = 0.0f;
			setVertexBuffer(data);
		}
	}

	public TmpRenderer(ViewPort viewport, Callback controller)
	{
		mViewPort = viewport;
		mController = controller;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glClearColor(0.8f, 0.2f, 0.2f, 1.0f);

		// setup shaders
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode);
		mShaderProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mShaderProgram, vertexShader);
		GLES20.glAttachShader(mShaderProgram, fragmentShader);
		GLES20.glLinkProgram(mShaderProgram);
		// get attributes
		maPosition = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
		muColor = GLES20.glGetUniformLocation(mShaderProgram, "uColor");
		muMVPMatrix = GLES20.glGetUniformLocation(mShaderProgram, "uMVPMatrix");
		
		// generate line
		mMesh = new VarMeshData(mNumPoints);
		mCoords = new float[mNumPoints*3];
		for (int i=0;i<mNumPoints;i++)
		{
			mCoords[i*3+0] = -1+(float)i*2/(float)(mNumPoints-1);
			mCoords[i*3+1] = 0;
			mCoords[i*3+2] = 0;
		}
		mMesh.updateVertices(mCoords);

		GLES20.glEnableVertexAttribArray(maPosition);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		mWidth = width;
		mHeight = height;
		mController.updateScreenSize(null, width, height);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		float t = SystemClock.currentThreadTimeMillis() / 1000.0f;
		Parameters p = mViewPort.getParameters();

		mController.updateFrame(0);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		GLES20.glUniform4f(muColor, 1.0f,1.0f,1.0f,1.0f);	// white
		float[] m = new float[16];
		Matrix.setIdentityM(m, 0);
		Matrix.orthoM(m, 0, p.left, p.right, p.down, p.up, -3, 7);
		GLES20.glUniformMatrix4fv(muMVPMatrix, 1, false, m, 0);
		
//		AHState.getInstance().mAccColumn.getBuffer(p.start,p.end);
//		long timeoffset = AHState.getInstance().mAccColumn.getTimeOffset(p.end);
//		long deltatime = p.end-timeoffset;
//		while(deltatime>1000) deltatime-=1000;
//		while(deltatime<0) deltatime+=1000;
//
//		GLES20.glUniform4f(muColor, 1.0f,1.0f,1.0f,1.0f);	// white
//		drawSpiral(new ValueBufferHelper(-1), p, 0);			// render negative boundary
//		drawSpiral(new ValueBufferHelper(+1), p, 0);			// render posivite boundary
//		GLES20.glUniform4f(muColor, 0.0f,1.0f,0.5f,1.0f);	// light green
//		drawSpiral(new TmpBufferHelper(AHState.getInstance().mAccColumn.getBuffer(p.start,p.end)), p, deltatime);
//		GLES20.glUniform4f(muColor, 0.0f,0.5f,1.0f,1.0f);	// light blue
//		drawSpiral(new GsrBufferHelper(AHState.getInstance().mGsrColumn.getBuffer(p.start,p.end)), p, deltatime);
		
		// render energy level stuff
		drawEnergy(m,p);

		GLES20.glUniform4f(muColor, 1.0f,1.0f,1.0f,1.0f);	// white
		long now = new Date().getTime();
		if (p.end>now-500)
		{
			Matrix.scaleM(m, 0, 0.1f,0.1f,0.1f);
			GLES20.glUniformMatrix4fv(muMVPMatrix, 1, false, m, 0);

			GLES20.glVertexAttribPointer(maPosition, 3, GLES20.GL_FLOAT, false, 12, mTriangle.vertexBuffer);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
		}
	}
	
	private void drawEnergy(float[] m,Parameters p)
	{
		Vector<UserTags.UserTag> userTags = AHState.getInstance().mUserTags.getUserTags(p.start, p.end);

		float[] m2 = m.clone();
		for (UserTags.UserTag ut : userTags)
		{
			m2 = m.clone();
			// calc point on spiral
			float t = 1.0f-(ut.getTime()-p.start)/(float)(p.end-p.start);
			float r_base = t*0.8f;
			float r = r_base+0.1f;
			float o = (float)((1-t)*Math.PI*2*4);
			float x = (float)(r*Math.sin(o));
			float y = (float)(r*Math.cos(o));
			
			// render
			GLES20.glUniform4f(muColor, 1.0f,1.0f,1.0f,1.0f);	// white
			String tag = ut.getTag();
			float scale = 1.0f;// Math.abs(level);
			Matrix.translateM(m2, 0, x,y, 0);
			Matrix.scaleM(m2, 0, 0.05f*scale,0.05f*scale,1.0f);
			GLES20.glUniformMatrix4fv(muMVPMatrix, 1, false, m2, 0);

			GLES20.glVertexAttribPointer(maPosition, 3, GLES20.GL_FLOAT, false, 12, mSquare.vertexBuffer);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
		}
	}
	
	private class BufferHelper {
		private int floatsPerValue;
		protected FloatBuffer buffer;
		public BufferHelper(FloatBuffer buffer, int floatsPerValue)
		{this.buffer=buffer; this.floatsPerValue = floatsPerValue;}
		public int remaining() { return buffer.remaining() / floatsPerValue; }
		public void ditch(int n)
		{
			buffer.get(new float[n*floatsPerValue]);
		}
		public float get()
		{
			float v = buffer.get();
			if (floatsPerValue>1)
			{
				buffer.get(new float[floatsPerValue-1]);
			}
			return v;
		}
	}
	
	private class GsrBufferHelper extends BufferHelper {
		public GsrBufferHelper(FloatBuffer b){super(b, 1);}
		public float get()
		{
			return super.get();
		}
	}

	private class TmpBufferHelper extends BufferHelper {
		public TmpBufferHelper(FloatBuffer b){super(b, 1);}
	}
	
	private class ValueBufferHelper extends BufferHelper {
		public ValueBufferHelper(float value)
		{
			super(FloatBuffer.allocate(2), 1);
			buffer.clear();
			buffer.put(value); buffer.put(value);
			buffer.position(0);
		}
	}

	private void drawSpiral(BufferHelper data, Parameters p, long timedelta) {
		BufferHelper b = data;
		int n = b.remaining();
		// render the points we get, to the entire screen. i.e. render the data towards all vertices
//		double timeperpoint = (p.end-p.start) / mNumPoints;
//		double pointspertime = mNumPoints / (double)(p.end-p.start);
		double pointspersample = mNumPoints / (double)n;
		double dbvalspertime = 2 / 1000.0;
		int oldj = (int)(timedelta*dbvalspertime*pointspersample);
		Log.d("TmpRenderer", "" + timedelta + " :: " + oldj);
//		float value = b.get();
//		b.get(new float[3]); // and skip 3
//		int n = b.remaining()/4;
		float value = 0; //b.get();
		for (int i=mNumPoints-1;i>=0;i--)
		{
			int j = n-(int)(i*n/(float)(mNumPoints-1));
			if (j>oldj)
			{
				if (j-oldj>1)
				{
//					b.get(new float[4*(j-oldj-1)]);
					b.ditch(j-oldj-1);
				}
//				value = b.get();
//				b.get(new float[3]); // and skip 3
				value = b.get();
				oldj = j;
			}

			float r_base = (mNumPoints-1-i)*0.8f / (float)(mNumPoints-1);
			float r = r_base + 0.1f+value*0.08f;
			float o = (float)(i*Math.PI*2*4/(float)(mNumPoints-1));

//			mCoords[i*3+0] = -1+(float)i*2/(float)(mNumPoints-1);
//			mCoords[i*3+1] = value;
			mCoords[i*3+0] = (float)(r*Math.sin(o));
			mCoords[i*3+1] = (float)(r*Math.cos(o));
		}
		mMesh.updateVertices(mCoords);

		GLES20.glUseProgram(mShaderProgram);
		GLES20.glVertexAttribPointer(maPosition, 3, GLES20.GL_FLOAT, false, 12, mMesh.vertexBuffer);
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mMesh.vertexBuffer.capacity()/3);
	}

	// shaders
	
	private int loadShader(int type, String code)
	{
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);
		return shader;
	}
	
	private final String mVertexShaderCode = 
	        "attribute vec4 vPosition; \n" +
	        "uniform vec4 uColor;      \n" +
	        "uniform mat4 uMVPMatrix;      \n" +
	        "void main(){              \n" +
	        " gl_Position = uMVPMatrix * vPosition; \n" +
	        "}                         \n";

    private final String mFragmentShaderCode = 
        "precision mediump float;  \n" +
        "uniform vec4 uColor;      \n" +
        "void main(){              \n" +
        " gl_FragColor = uColor; \n" +
        "}                         \n";

	@Override
	public void onPick(int id, int x, int y) {
/*		if (x<240 && y>700) mController.clicked("buttonLeft");
		else if (x>240 && y>700) mController.clicked("buttonRight");
		else mController.clicked(null);*/
	}
}
