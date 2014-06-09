package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Iterator;

import se.sics.ah3.AHState;
import se.sics.ah3.DataProxy;
import se.sics.ah3.database.Column.TimeData;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.model.SpiralFormula;
import android.content.Context;
import android.opengl.GLES20;
import android.util.FloatMath;

/**
 * Movement visualisation
 * 
 * @version 0.1
 * @author mareri
 *
 */

public class Movement extends Mesh20 {
	private int mNumberOfVertices = 1000;
	
	private DataProxy mDataProxy;
	private long mLastStart=0, mLastEnd=0;

	private ByteBuffer mRawVertexBuffer;
	private int mModelViewProjectionLoc;
	private int mModelViewLoc;

	// bounds for opacity
//	private final static float MIN_OPACITY = 0.1f;
//	private final static float MAX_OPACITY = 0.8f;
//	
//	private final static boolean DEBUG = false;
	
	private ViewPort mViewPort;
	
	private Context mContext = null;

	public Movement(ViewPort viewport, String name, Context context) {
		super(name);
		mViewPort = viewport;
		mContext = context;
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		if(!mVisible || (pick && !mPickable)) {
			return;
		}
		super.draw(camera, pick);
//		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
//		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
        GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);
        
        if(!pick) {
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
			GLES20.glEnable(GLES20.GL_BLEND);
        }
        
    	updateData();

    	GLES20.glEnableVertexAttribArray(Core.VERTEX); 
        Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mNumberOfVertices);
        
        if(!pick) {
        	GLES20.glDisable(GLES20.GL_BLEND);
        }
	}
	
	/**
	 * Read data from the database
	 */
	void updateData() {
	    // recreate the data
	    //TODO: Refactor
	    createData();

	    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 3 * mNumberOfVertices, mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	}

	private void normalize(float[] v) {
		float l = 1f / (FloatMath.sqrt(v[0] * v[0] + v[1] * v[1]));
		v[0] *= l;
		v[1] *= l;
	}

	private void pushCoord(float t, float[] outCoords, float[] normal, int i, float opacity, float value) {
		SpiralFormula.parametricFormulation(t, outCoords);
		float thickness = SpiralFormula.getThickness(t);
		
		if(t <= SpiralFormula.REALTIME_SEGMENT_SIZE) {
			opacity = 0.0f;
			value = 0.0f;
		}

		float size = thickness;
		size *= value; //td.value;

		SpiralFormula.getNormal(t, normal);
		normalize(normal);
//		prevCoord[0] = outCoords[0];
//		prevCoord[1] = outCoords[1];
		mVertexBuffer.put( outCoords[0] + normal[0] * size + normal[0] * thickness );
		mVertexBuffer.put( outCoords[1] + normal[1] * size + normal[1] * thickness );
		mVertexBuffer.put( opacity );
		mVertexBuffer.put( outCoords[0] - normal[0] * size + normal[0] * thickness );
		mVertexBuffer.put( outCoords[1] - normal[1] * size + normal[1] * thickness );
		mVertexBuffer.put( opacity );
	}

	private void createData() {
		Parameters p = mViewPort.getParameters();
		long start = p.start;
		long end = p.end;
		float timespan = end-start;

		// only update data if there is new data
		if (mLastStart==start && mLastEnd==end && !mDataProxy.isDirty()) return;
		mLastStart = start;
		mLastEnd = end;

//		Collection<TimeData> data = getData(start,end); //mDataBufferer.get(start, end); //AHState.getInstance().mGsrColumn.getTimeData(start, end);
		Collection<TimeData> data = mDataProxy.getData(start,end); //mDataBufferer.get(start, end); //AHState.getInstance().mGsrColumn.getTimeData(start, end);

		int numberOfPoints = data.size();
		int numActualVertices = 0;
		mNumberOfVertices = data.size()*6;
//		Log.d("Movement2", "Number of vertices: " + numberOfVertices);
		if (numberOfPoints==0) return;
		
		if (mRawVertexBuffer==null || mRawVertexBuffer.capacity()<mNumberOfVertices * 4 * 3) {
			mRawVertexBuffer = ByteBuffer.allocateDirect(mNumberOfVertices * 4 * 3);
			mRawVertexBuffer.order(ByteOrder.nativeOrder());
			mVertexBuffer = mRawVertexBuffer.asFloatBuffer();
		}

//		float[] coords = new float[mNumberOfVertices * 3 * 2];
		float[] outCoords = new float[3];
		float[] normal = new float[3];
//		float[] prevCoord = new float[3];
//		prevCoord[0] = 0f;
//		prevCoord[1] = 0f;

		Iterator<TimeData> iterator = data.iterator();
		TimeData td = iterator.next();	// get first
		TimeData tdnext = td;	// just to make sure tdnext != null
		long resolution = mDataProxy.getDataResolution(start, end);
		float resDiff = (float)(resolution / (double)(end-start));
		long prevTime = end;

		int i;
		for (i=0;tdnext!=null;i++) {
			tdnext = iterator.hasNext()?iterator.next():null;

			float t = (end-td.time) / timespan;
			if (t<0) t = 0.0f;
			
			
			t = SpiralFormula.rescaleTToAfterRealtimeSegment(t);

			float opacity = 0.5f;
			if (prevTime-td.time>resolution) {
				pushCoord(t-resDiff, outCoords, normal, i, opacity, 0.0f);
				i++;
			}
			pushCoord(t, outCoords, normal, i, opacity, td.value/32.0f);
			if (tdnext==null || td.time-tdnext.time>resolution) {
				i++;
				pushCoord(t+resDiff, outCoords, normal, i, opacity, 0.0f);
			}

			prevTime = td.time;
			td = tdnext;
		}
		
		numActualVertices = i*2;
//		Log.d("Movement2", "Actual vertices: " + numActualVertices + " vertices: " + mNumberOfVertices);

		//Log.d("Spiral", "Length: " + measure);
		//Log.d("Spiral", "Step length: " + stepLength);
		
//		for (i = 0; i < numActualVertices; i++) {
//			for (int j = 0; j < 3; j++) {
//				mVertexBuffer.put(coords[i * 3 + j]);
//			}
//		}

		mNumberOfVertices = numActualVertices;
		mVertexBuffer.position(0);
	}

	public void init() {
		super.init();

		mDataProxy = new DataProxy("movement", AHState.getInstance().mAccColumn, DataProxy.METHOD_MEAN, mContext);

        mShader = Core.getInstance().loadProgram("movement", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");

//		createData();

//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	}
	
	 private static final String kVertexShader =
         "precision mediump float; \n" +
         "uniform mat4 worldViewProjection; \n" +
         "uniform mat4 modelView; \n" +
         "attribute vec3 position; \n" +
         "varying float intensity; \n" +
         "void main() { \n" +
         "  intensity = position.z; \n" +
         "  gl_Position = worldViewProjection * modelView * vec4(position.xy, vec2(1.0)); \n" +
         "}";
 
     private static final String kFragmentShader =
         "precision mediump float; \n" +
         "varying float intensity; \n" +
         "void main() { \n" +
         "  gl_FragColor = vec4(intensity); \n" +
         "}";
}


