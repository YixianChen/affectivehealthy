package se.sics.ah3.graphics;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.Matrix;
import android.util.Log;
import se.sics.ah3.AHState;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.interaction.ViewPort;

public class Overview extends MeshNode {
	
	private ArrayList<SpiralInfo> mSpirals = new ArrayList<SpiralInfo>();
	private SpiralInfo mMainSpiral = null;
	private int mGridWidth = 3;
	private int mGridHeight = 2;
	private float mScale = 0.6f;
	
	private Camera mCamera = null; // for picking
	private Context mContext;
	
	public Overview(String id, Context context) {
		super(id);
		mContext = context;
	}

	int mId = 0;
	class SpiralInfo {
		ViewPort viewport;
		MeshNode node;
		UITimeEndElements timeEndElements = null;
		int id;
	}
	
	private SpiralInfo makeSpiral(long start, long end, float x, float y, float scale, boolean isMain) {
		ViewPort vp = null;
		if(isMain) {
			vp = AHState.getInstance().mViewPort;
		}
		else {
			vp = new ViewPort();
			Parameters params = vp.getParameters();
			params.start = start;
			params.end = end;
			vp.setParameters(params);			
		}
		
		MeshNode spiralNode = new MeshNode("SpiralNode...");
		
		SpiralShadowDynamic shadow = new SpiralShadowDynamic(vp);
		Spiral spiral = new Spiral(vp, "Overview.Spiral."+mId, mContext);
		shadow.setDrawRealtimeSegment(false);
		spiral.setDrawRealtimeSegment(false);
		Mesh20 movement = new Movement(vp, "Overview.Movement."+mId, mContext);
		SpiralInfo si = new SpiralInfo();
		if(!isMain) {
			UITimeEndElements timeEndElements = new UITimeEndElements("SpiralInfo.timeend." + mId, 0, 0, UIElement.ALIGN_CENTER, false);
			timeEndElements.translate(-0.9f, -0.5f, 0.0f);
			timeEndElements.scale(3.0f, 3.0f, 1.0f);
			timeEndElements.setParameters(vp.getParameters());			
			spiralNode.add(timeEndElements);
			si.timeEndElements = timeEndElements;
		}
		
		spiralNode.add(shadow);
		spiralNode.add(spiral);
		spiralNode.add(movement);
//		spiralNode.add(new Weekdays(vp));
//		spiralNode.add(new Numbers(vp));
		
		spiralNode.translate(x, y, 0);
		spiralNode.scale(scale,scale,1.0f);

		si.viewport = vp;
		si.node = spiralNode;
		si.id = mId++;

		return si;
	}
	
	@Override
	public void draw(Camera camera, boolean pick) {
		mCamera = camera;
		super.draw(camera, pick);
	}
	
	public void create() {
		// initialize it
		clear();
		mSpirals.clear();

		Parameters parameters = AHState.getInstance().mViewPort.getParameters();

		long granularity = parameters.end-parameters.start;

		float scale = mScale;
		float width = 0.6f;		// half width
		float height = 0.6f;	// half height
								// dimensions end up being width*2+scale
		
		mScale = Math.min(0.9f / mGridWidth, 0.9f / mGridHeight);

		long time = parameters.end; //System.currentTimeMillis();
		for (int i=0;i<mGridHeight;i++) {
			for (int j=0;j<mGridWidth;j++) {
				int k = i*mGridWidth+j;
				float x = 1-j/((mGridWidth-1)/2f); //1-(j-(mGridWidth-1)/2f)/((mGridWidth-1)/2f);
				float y = 1-i/((mGridHeight-1)/2f); //1-(i-(mGridHeight-1)/2f)/((mGridHeight-1)/2f);
				SpiralInfo si = makeSpiral(time-granularity*k, time-granularity*(k-1), x*width,-y*height+0.25f, scale, false);
				mSpirals.add(si);
				add(si.node);
			}
		}

		//mMainSpiral = makeSpiral(parameters.start, parameters.end, 0.0f, -0.75f, 0.5f, true);
		//add(mMainSpiral.node);

		float margin = 0.2f;
		UIElement arrowLeft = new UIElement("TOVERVIEWSCROLLP", "arrow left", -2.0f + margin, 1.95f, UIElement.ALIGN_LEFT, false);
		UIElement arrowRight = new UIElement("TOVERVIEWSCROLLN", "arrow right", 2.0f - margin, 3.2f, UIElement.ALIGN_RIGHT, false);
		arrowLeft.scale(2.0f, 2.0f, 1.0f);
		arrowRight.scale(2.0f, 2.0f, 1.0f);
		add(arrowLeft);
		add(arrowRight);
		
		setPositions();
		
		//init();
	}
	
	public void updateParameters() {
		Parameters parameters = AHState.getInstance().mViewPort.getParameters();
		long start = parameters.start;
		long end = parameters.end;
		// default is to space spirals a day apart
		long granularity = 24*60*60*1000;
		// unless granularity of viewport is higher, then use that
		if((end - start) > granularity) {
			granularity = end - start;
		}
		for (int i=0;i<mGridHeight;i++) {
			for (int j=0;j<mGridWidth;j++) {
				int k = i*mGridWidth+j;
				parameters.start = start - granularity * (k + 1);
				parameters.end = end - granularity * (k + 1);
				SpiralInfo si = mSpirals.get(k);
				si.viewport.setParameters(parameters);
				si.timeEndElements.setParameters(si.viewport.getParameters());
			}
		}		
	}
	
	// updates the positions of the spirals based on their locations in the array
	private void setPositions() {
//		float scale = 0.25f;
		float width = 1.0f;		// half width
		float height = 1.2f;	// half height
								// dimensions end up being width*2+scale
		for (int l=0;l<mSpirals.size();l++) {
			SpiralInfo si = mSpirals.get(l);
			int i = mGridHeight-1-l/mGridWidth;
			int j = l%mGridWidth;
			float x = 1-j/((mGridWidth-1)/2f);
			float y = 1-i/((mGridHeight)/2f);
			si.node.translate(x*width, -y*height+3.05f,0);
		}
	}
	
	public ViewPort getViewportAtPosition(PointF pos) {
		int i=-1;
		
		// translate pos into objects world
		float[] clickPos = transformClickIntoOurSpace(pos);
		float minDistance = Float.MAX_VALUE;
		
		// check for shortest xy-distance
		for(int j = 0; j < mSpirals.size(); j++) {
			float distance = distanceToSpiral(j, clickPos);
			if(distance < minDistance) {
				i = j;
				minDistance = distance;
			}
		}
		
		if(minDistance < 0.5f) {
			return mSpirals.get(i).viewport;
		}
		
		if(distanceToMainSpiral(clickPos) < 1.0f) {
			return AHState.getInstance().mViewPort;
		}
		
		return null;
	}

	private float[] transformClickIntoOurSpace(PointF pos) {
		float[] clickPos = {pos.x, pos.y, 0.0f, 0.0f};
		float[] transform = traverseTransform();
		Matrix.multiplyMM(transform, 0, mCamera.getmModelViewMatrix(), 0, transform, 0);
		Matrix.multiplyMV(clickPos, 0, transform, 0, clickPos, 0);
		return clickPos;
	}

	private float distanceToSpiral(int spiralIndex, float[] fromPoint) {
		SpiralInfo si = mSpirals.get(spiralIndex);
		float[] spiralPos = si.node.getTranslation();
		float distance = (float)Math.pow((fromPoint[0] - spiralPos[0]) * (fromPoint[0] - spiralPos[0]) + (fromPoint[1] - spiralPos[1]) * (fromPoint[1] - spiralPos[1]), 0.5);
		return distance;
	}

	private float distanceToMainSpiral(float[] fromPoint) {
		float[] spiralPos = Core.getInstance().getRoot().getMeshInstance("Spiral").getTranslation();
		float distance = (float)Math.pow((fromPoint[0] - spiralPos[0]) * (fromPoint[0] - spiralPos[0]) + (fromPoint[1] - spiralPos[1]) * (fromPoint[1] - spiralPos[1]), 0.5);
		return distance;
	}
	
	public void next() {
		SpiralInfo siLast = mSpirals.get(mSpirals.size()-1);
		mSpirals.remove(mSpirals.size()-1);
		SpiralInfo siFirst = mSpirals.get(0);

		Parameters parameters = siFirst.viewport.getParameters();
		// default is to space spirals a day apart
		long granularity = 24*60*60*1000;
		// unless granularity of viewport is higher, then use that
		if((parameters.end - parameters.start) > granularity) {
			granularity = parameters.end - parameters.start;
		}
		parameters.start += granularity;
		parameters.end += granularity;

		siLast.viewport.setParameters(parameters);
		siLast.timeEndElements.setParameters(siLast.viewport.getParameters());
		mSpirals.add(0, siLast);
		
		setPositions();	// updates positions of the spirals
	}

	public void previous() {
		SpiralInfo siFirst = mSpirals.get(0);
		mSpirals.remove(0);
		SpiralInfo siLast = mSpirals.get(mSpirals.size()-1);

		Parameters parameters = siLast.viewport.getParameters();
		long granularity = 24*60*60*1000;
		// unless granularity of viewport is higher, then use that
		if((parameters.end - parameters.start) > granularity) {
			granularity = parameters.end - parameters.start;
		}
		parameters.start -= granularity;
		parameters.end -= granularity;

		siFirst.viewport.setParameters(parameters);
		siFirst.timeEndElements.setParameters(siFirst.viewport.getParameters());
		mSpirals.add(siFirst);

		setPositions();	// updates positions of the spirals
	}
}
