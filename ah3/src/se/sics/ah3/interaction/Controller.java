package se.sics.ah3.interaction;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import se.sics.ah3.AHState;
import se.sics.ah3.DialogActivity;
import se.sics.ah3.graphics.UIElement;
import se.sics.ah3.graphics.Camera;
import se.sics.ah3.graphics.Core;
import se.sics.ah3.graphics.CoreView;
import se.sics.ah3.graphics.Mesh20;
import se.sics.ah3.graphics.UserTagRenderer;
import se.sics.ah3.model.SpiralFormula;
import se.sics.ah3.usertags.UserTags;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class Controller implements Callback {
	private static final String TAG = "Controller";
	private ViewPort mViewPort;
	private PickHandler mPickHandler;
	private CoreView mCoreView;
	private boolean mOverview = false;

	public static long[] mZoomLevels = new long[] {1000*60,1000*60*10,1000*60*60,1000*60*60*6,1000*60*60*24,1000*60*60*24*7};
	private int mCurrentZoomLevel = 0;
	
	// state
	private int mWidth,mHeight;

	private HashMap<Integer,Pointer> mPointers = new HashMap<Integer,Pointer>();

	private boolean mUpdateRealtime = true;

	// controllers
	private boolean mIsDragging = false;
//	private EffortController mEffortController;

	// generic touch
	private int mTouchDownPointerId;
	private Parameters mTouchDownParameters;
	private SpiralFormula.SpiralPoint mTouchDownSpiralPoint;

	// scroll time
	private boolean mIsScrollingTime = false;
	// zoom time
	private boolean mIsZoomingTime = false;
	private int mTouchDownPointerId2;
//	private Parameters mTouchDownParameters2;
	private SpiralFormula.SpiralPoint mTouchDownSpiralPoint2;
	private float mZoomTimeTheta = 0.0f;
	private float mZoomTimeThetaDelta = 0.0f;

	// pan and zoom
	private boolean mIsPanZoom = false;
	private boolean mIsZooming = false;
	private int mPIDPan1;
	private int mPIDPan2;
	
	// animate zoom
	private float mAnimLeft;
	private float mAnimRight;
	private float mAnimUp;
	private float mAnimDown;
	private boolean mAnimate = false;
	
	// activity, to be able to display dialogs in a simple manner
	Handler mDialogHandler = null;
	private boolean mFirstPointerUpSinceScreenSwitch;
	private long mScrollLength = 0;
	
	protected interface Draggable {
		public PointF position();
		public boolean down(float x,float y);
		public boolean move(float x,float y);
		public boolean up(float x, float y);
		public boolean isDragging();
	}

	class MyMotionEvent {
		int pointerCount;
		Vector<PointF> positions = new Vector<PointF>();
		Vector<Integer> pointerIds = new Vector<Integer>();
		MyMotionEvent(MotionEvent me)
		{
			pointerCount = me.getPointerCount();
			for (int i=0;i<pointerCount;i++)
			{
				float x = me.getX(i);
				float y = me.getX(i);
				int id = me.getPointerId(i);
				positions.add(new PointF(x,y));
				pointerIds.add(new Integer(id));
			}
		}
	}

	class Pointer {
		boolean isDown = false;
		boolean isPicking = false;
		int mID;
		PointF mPosition;
		Draggable mDraggable = null;
		public MyMotionEvent mEvent;
		public long mTimestamp;
		public boolean isDoubleTap = false;
		Pointer(int id)
		{
			mID = id;
			mPosition = new PointF(0,0);
		}
	}
	
	public Controller(CoreView coreview, ViewPort viewport, Handler dialogHandler)
	{
		mViewPort = viewport;
		mCoreView = coreview;
		
//		mEffortController = new EffortController();
		mDialogHandler = dialogHandler;
	}

	private Pointer getPointer(int id)
	{
		Integer i = new Integer(id);
		if (!mPointers.containsKey(i))
		{
			Pointer p = new Pointer(id);
			mPointers.put(i, p);
			return p;
		}
		return mPointers.get(i);
	}

	public synchronized  boolean onTouch(View v, MotionEvent event) {
		//
		// when a pointer is pushed, then any draggable object is checked unless already dragging.
		// if no drag event, then check for other objects
		// then do zoom/pan
		//
		int pid;
		switch(event.getActionMasked()) // & MotionEvent.ACTION_MASK)
		{
		case MotionEvent.ACTION_POINTER_DOWN:
			pid = event.getPointerId(event.getActionIndex());
			pointerDown(pid, event);
			break;
		case MotionEvent.ACTION_POINTER_UP:
			pid = event.getPointerId(event.getActionIndex());
			pointerUp(pid, event);
			break;
		case MotionEvent.ACTION_UP:
			pid = event.getPointerId(0);
			pointerUp(pid, event);
			break;
		case MotionEvent.ACTION_DOWN:
			pid = event.getPointerId(0);
			pointerDown(pid, event);
			break;
		case MotionEvent.ACTION_MOVE:
			pointerMove(event);
			break;
		}
		return true;
	}

	public synchronized boolean pointerDown(int pointerId, MotionEvent event)
	{
		Log.d(TAG, "Pointer DOWN: " + pointerId + " (" + event.getX() + ", " + event.getY() + ")");
		
		mScrollLength = 0;
		// save info in pointer and postpone actual pointing...
		Pointer p = getPointer(pointerId);

		// pointer index
		int pi = event.findPointerIndex(pointerId);

		// pointer position
		float x = event.getX(pi);
		float y = event.getY(pi);
		
		if (System.currentTimeMillis()-p.mTimestamp<250) {
			p.isDoubleTap = true;
		}
		else {
			p.isDoubleTap = false;
		}

		// set pointer state
		p.isDown = true;
		p.mPosition = new PointF(x,y);
		p.mEvent = new MyMotionEvent(event);
		p.mTimestamp = System.currentTimeMillis();

		// use pick from renderer
		if (mPickHandler!=null)
		{
			p.isPicking = true;
			mPickHandler.onPick(pointerId, (int)x, (int)y);
		}
		else
		{
			postponedPointerDown(pointerId);
		}

		return true;
	}
	
	private void doubletap(Pointer pointer)
	{
		// no double tap functionality in overview
		if(mOverview) {
			return;
		}
/*		PointF modelCoord = fromScreen(pointer.mPosition);
		Parameters params = mViewPort.getParameters();
		SpiralPoint sp = SpiralFormula.fromPoint(modelCoord, params);*/
		
		Log.d(TAG, "Double tap!");

		PointF p = fromScreen(pointer.mPosition);
		Parameters params = mViewPort.getParameters();
		float dist = (float) Math.sqrt((-0.3 - p.x) * (-0.3 - p.x) + (-0.4 - p.y) * (-0.4 - p.y));
		if (dist<0.35f && !mUpdateRealtime)
		{
			mUpdateRealtime = true;
			return;
		}
		if (params.right-params.left<=(Parameters.RIGHT_BOUND-Parameters.LEFT_BOUND)/2)
		{
			zoomToNormal();
		}
		else
		{
			zoomIn(p);
		}

//		SpiralPoint sp = SpiralFormula.fromPoint(p, params);
//		AHState.getInstance().mEnergyLevels.addLevel(sp.time, 1.0f);
	}

	private void zoomIn(PointF p) {
		float w = (Parameters.RIGHT_BOUND-Parameters.LEFT_BOUND)*0.35f;
		float h = (Parameters.DOWN_BOUND-Parameters.UP_BOUND)*0.35f;
		mAnimLeft = p.x-w/2;
		mAnimRight = p.x+w/2;
		mAnimUp = p.y-h/2;
		mAnimDown = p.y+h/2;
		mAnimate = true;
		Log.d("controller", "zoomIn()");
	}

	private void zoomToNormal() {
		mAnimLeft = Parameters.LEFT_BOUND;
		mAnimRight = Parameters.RIGHT_BOUND;
		mAnimUp = Parameters.UP_BOUND;
		mAnimDown = Parameters.DOWN_BOUND;
		mAnimate = true;
		Log.d("controller", "zoomToNormal()");
	}

	private void zoomOutToOverview() {
		mAnimLeft = Parameters.LEFT_BOUND * 2.0f;
		mAnimRight = Parameters.RIGHT_BOUND * 2.0f;
		mAnimUp = Parameters.UP_BOUND * 2.0f - (Parameters.UP_BOUND - Parameters.DOWN_BOUND) * 0.5f;;
		mAnimDown = Parameters.DOWN_BOUND * 2.0f - (Parameters.UP_BOUND - Parameters.DOWN_BOUND) * 0.5f;;
		mAnimate = true;
		Log.d("controller", "zoomOutToOverview()");
	}

	private PointF fromScreen(PointF p)
	{
		float x = p.x / (float)mWidth;
		float y = p.y / (float)mHeight;

//		Parameters param = mCamera.getmParameters();
		Parameters param = mViewPort.getParameters();
		x = x*param.right + (1-x)*param.left;
		y = y*param.down + (1-y)*param.up;

		return new PointF(x,y);
	}

//	private float anglediff(SpiralPoint sp1, SpiralPoint sp2)
//	{
//		// make sure sp1.Theta<sp2.Theta
//		if (sp1.Theta>sp2.Theta){
//			SpiralPoint t = sp1;
//			sp1 = sp2;
//			sp2 = t;
//		}
//		float d = sp2.Theta-sp1.Theta; 
////		if (d>Math.PI)
////			d-= Math.PI;
//		return d;
//	}

	// mainly handles start of Gestures not based on interactive objects
	// will be called either by pickhandler callback, or directly by onDown
	private synchronized void postponedPointerDown(int pointerId)
	{
		Log.d(TAG, "Postponed pointer down " + pointerId);
		Pointer pointer = getPointer(pointerId);
		
		if (mIsDragging)
			return;
		
		// check double tap
		if (pointer.isDoubleTap) {
			doubletap(pointer);
			return;
		}
		
		MyMotionEvent event = pointer.mEvent;

		if (event.pointerCount==1)
		{
			fromScreen(pointer.mPosition);
			
			// start touch, save pointer
			mTouchDownParameters = mViewPort.getParameters();
			mTouchDownPointerId = pointerId;
			PointF modelCoord = fromScreen(pointer.mPosition);
			mTouchDownSpiralPoint = SpiralFormula.fromPoint(modelCoord, mTouchDownParameters);
			Log.d(TAG, "Start scrolling time: " + mTouchDownSpiralPoint.R + ", " + mTouchDownSpiralPoint.Theta + ": " + mTouchDownSpiralPoint.time);
			mIsScrollingTime = true;
//			mUpdateRealtime = false;
		}
		else if (event.pointerCount==2)
		{
			if (mIsScrollingTime)
			{
				// stop scrolling time!
				mIsScrollingTime = false;
			}
			// make sure there was a previous one as well
			if (mTouchDownParameters==null) {
				mTouchDownParameters = mViewPort.getParameters();
				mTouchDownPointerId = pointerId;
				
				mTouchDownSpiralPoint = SpiralFormula.fromPoint(fromScreen(pointer.mPosition), mTouchDownParameters);
			}
			// check if we are touching within an arc, and start timezooming
			mTouchDownSpiralPoint2 = SpiralFormula.fromPoint(fromScreen(pointer.mPosition), mTouchDownParameters);
			mTouchDownPointerId2 = pointerId;
			// check if we are gonna zoom in time
//			if (Math.abs(mTouchDownSpiralPoint2.R-mTouchDownSpiralPoint.R)<0.05f)
//			if (Math.abs(mTouchDownSpiralPoint2.time-mTouchDownSpiralPoint.time)<(mTouchDownParameters.end-mTouchDownParameters.start)/10
//					&& (anglediff(mTouchDownSpiralPoint2, mTouchDownSpiralPoint)<(Math.PI/2.0f))
//					|| (anglediff(mTouchDownSpiralPoint2, mTouchDownSpiralPoint)<(Math.PI) && mTouchDownSpiralPoint.R<0.25f))
			if (Math.abs(mTouchDownSpiralPoint2.time-mTouchDownSpiralPoint.time)<(mTouchDownParameters.end-mTouchDownParameters.start)/2)
			{
				mUpdateRealtime = false;
				mIsZoomingTime = true;
				mZoomTimeTheta = 1.0f;
				mZoomTimeThetaDelta = 0.0f;
				Log.d(TAG, "Zooming time");
			}

/*			if (!mIsZoomingTime)
			{
				mIsPanZoom = true;
				mPanZoomOrigParams = mViewPort.getParameters();
				mPIDPan1 = event.pointerIds.get(0); //event.getPointerId(0);
				mPIDPan2 = event.pointerIds.get(1); //event.getPointerId(1);
				Log.d(TAG, "Pan & Zoom start: " + mPIDPan1 + ", " + mPIDPan2);
			}*/
		}
	}

	public synchronized boolean pointerUp(int pointerId, MotionEvent event)
	{
		Pointer pointer = getPointer(pointerId);
		pointer.isDown = false;
		
		float x = event.getX();
		float y = event.getY();
		
		if (pointer.mDraggable!=null)	// means we are dragging
		{
			pointer.mDraggable.up(x, y);
			pointer.mDraggable = null;
			mIsDragging = false;
			return false;
		}

		// one pointer?
		if (event.getPointerCount()==1)
		{
			if (mOverview && (event.getEventTime() - event.getDownTime()) < 1500 && Math.abs(mScrollLength) < 500) {
				if(!mFirstPointerUpSinceScreenSwitch) {
					PointF p = fromScreen(pointer.mPosition);
					ViewPort vp = mCoreView.getOverview().getViewportAtPosition(p);
					if (vp!=null) {
						mUpdateRealtime = false;
						Parameters timeParams = vp.getParameters();
						Parameters params = AHState.getInstance().mViewPort.getParameters();
						params.start = timeParams.start;
						params.end = timeParams.end;
						long timespan = params.end - params.start;
						for(int i=0; i < mZoomLevels.length; i++) {
							if(mZoomLevels[i] == timespan) {
								mCurrentZoomLevel = i;
							}
						}
						AHState.getInstance().mViewPort.setParameters(params);
						mCoreView.hideOverview();
						zoomToNormal();
						mOverview = false;
						return false;
					}
				}
				// to not react on pointer up when tapping on compare button...
				if(mFirstPointerUpSinceScreenSwitch) {
					mFirstPointerUpSinceScreenSwitch = false;
				}
			}
			
			if (mIsScrollingTime)
			{
				// stom scrolling time
				mIsScrollingTime = false;
				// format date
				Date d = new Date();
				Parameters p = mViewPort.getParameters();
				d.setTime(p.end);
				Calendar cal = Calendar.getInstance();
				cal.setTime(d);
				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH);
				int day = cal.get(Calendar.DAY_OF_MONTH);
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				int minute = cal.get(Calendar.MINUTE);
				int second = cal.get(Calendar.SECOND);
				String s = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
				Log.d(TAG, "Done scrolling: " + s);
			}
			else if (mIsZoomingTime)
			{
				// stop zooming time
				mIsZoomingTime = false;
			}
		}
		if (event.getPointerCount()==2)
		{
			if (mIsScrollingTime)
			{
				// stom scrolling time
				mIsScrollingTime = false;
			}
			if (mIsZoomingTime)
			{
				int zoomLevel = mCurrentZoomLevel;
				// fix timezoom
				if (mZoomTimeThetaDelta<0.8f)	// time window should be smaller.
				{
					zoomLevel = mCurrentZoomLevel-1;
					if (zoomLevel<0) zoomLevel = 0;
				} else if (mZoomTimeThetaDelta>1.2f) {
					zoomLevel = mCurrentZoomLevel+1;
					if (zoomLevel>=mZoomLevels.length) zoomLevel = mZoomLevels.length-1;
				}
				if (mCurrentZoomLevel!=zoomLevel) {
/*					Parameters p = mViewPort.getParameters();
					long centertime = (p.start+p.end) / 2;
					float change = mZoomLevels[zoomLevel]/(float)mZoomLevels[mCurrentZoomLevel];
					long nstart = centertime - (long)((centertime - mTouchDownParameters.start)*change);
					long nend = centertime + (long)((mTouchDownParameters.end - centertime)*change);
					nstart = nend-mZoomLevels[zoomLevel];
					p.start = nstart;
					p.end = nend;
					mViewPort.setParameters(p);*/
					mCurrentZoomLevel = zoomLevel;
					Log.d(TAG, "SET ZOOMLEVEL: " + mCurrentZoomLevel);
				}
				// stop zooming time
				mIsZoomingTime = false;
				mZoomTimeTheta = 1.0f;
				mZoomTimeThetaDelta = 0.0f;
			}
		}

		if (mIsPanZoom && (mPIDPan1==pointerId || mPIDPan2==pointerId))
		{
			mIsPanZoom = false;
		}

		Log.d(TAG, "Pointer UP: " + pointerId);
		
		return false;
	}

	private float dist(float x1,float y1,float x2,float y2)
	{
		return (float) Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}
	
	private float thdiff(SpiralFormula.SpiralPoint sp1, SpiralFormula.SpiralPoint sp2)
	{
/*		float th1 = sp1.Theta;
		float th2 = sp2.Theta;
		
		float d = th2-th1;
		if (d>Math.PI) d-=Math.PI*2;
		if (d<Math.PI)d+=Math.PI*2;
		return d;*/
		Parameters parameters = mViewPort.getParameters();
		return Math.abs((float)((sp2.time-sp1.time) / (double)(parameters.end-parameters.start)));
	}
	
	public synchronized boolean pointerMove(MotionEvent event)
	{
		if (mIsDragging)
		{
			for (int i=0;i<event.getPointerCount();i++)
			{
				float x = event.getX(i);
				float y = event.getY(i);
				Pointer pointer = getPointer(event.getPointerId(i));
				// check if dragging
				if (pointer.mDraggable!=null)	// means we are dragging
				{
					pointer.mDraggable.move(x, y);
//					return true;
				}
			}
			return true;
		}

		if (event.getPointerCount()==1)
		{
			// one finger down, means index is 0
			int pointerIndex = 0; //event.findPointerIndex(pointerId);
			float x = event.getX(pointerIndex);
			float y = event.getY(pointerIndex);
			PointF point = new PointF(x,y);

			getPointer(event.getPointerId(0));

			// check if dragging
/*			if (pointer.mDraggable!=null)	// means we are dragging
			{
				pointer.mDraggable.move(x, y);
				return true;
			}*/

			if (mIsScrollingTime)
			{
				SpiralFormula.SpiralPoint sp = SpiralFormula.fromPoint(fromScreen(point), mTouchDownParameters);
				Parameters params = mViewPort.getParameters();
				long scroll = -(sp.time - mTouchDownSpiralPoint.time);
				mScrollLength += scroll;
				params.start = mTouchDownParameters.start + scroll;
				params.end = mTouchDownParameters.end + scroll;
//				Log.d(TAG, "scroll: " + scroll + " part: " + (float)(scroll/(float)(params.end-params.start)));

				// stop realtime update if starting to scroll backwards
				if (scroll<-50 && mUpdateRealtime) mUpdateRealtime = false;

				long now = new Date().getTime();
				if (params.end>now)
				{
					params.start -= params.end-now;
					params.end = now;
					mUpdateRealtime = true;
//					mIsScrollingTime = false;
				}

				mViewPort.setParameters(params);
			}
		}

		if (event.getPointerCount()==2)
		{
			if (mIsZoomingTime)
			{
				Pointer pointer1 = getPointer(mTouchDownPointerId);
				Pointer pointer2 = getPointer(mTouchDownPointerId2);
				int pointerIndex1 = event.findPointerIndex(mTouchDownPointerId);
				int pointerIndex2 = event.findPointerIndex(mTouchDownPointerId2);
				// assert that there are these pointers.
				if (pointerIndex1==-1 || pointerIndex2==-1) return false;
				PointF point1 = new PointF(event.getX(pointerIndex1),event.getY(pointerIndex1));
				PointF point2 = new PointF(event.getX(pointerIndex2),event.getY(pointerIndex2));
				SpiralFormula.fromPoint(fromScreen(point1), mTouchDownParameters);
				SpiralFormula.fromPoint(fromScreen(point2), mTouchDownParameters);
	//			mZoomTimeThetaDelta = thdiff(mTouchDownSpiralPoint, mTouchDownSpiralPoint2) / thdiff(sp1, sp2);
	//			mZoomTimeThetaDelta = dist(point1.x,point1.y,point2.x,point2.y) / dist(mTouchDownSpiralPoint.p.x,mTouchDownSpiralPoint.p.y,mTouchDownSpiralPoint2.p.x,mTouchDownSpiralPoint2.p.y);
				mZoomTimeThetaDelta = dist(pointer1.mPosition.x,pointer1.mPosition.y,pointer2.mPosition.x,pointer2.mPosition.y) / dist(point1.x,point1.y,point2.x,point2.y);

				new Date().getTime();
				long centertime = (mTouchDownSpiralPoint.time + mTouchDownSpiralPoint2.time) / 2;
				mZoomTimeTheta = mZoomTimeThetaDelta;
				double thetachange = mZoomTimeTheta;
				mZoomTimeThetaDelta = (float)thetachange;
/*				if (thetachange<0.8f)	// time window should be smaller.
				{
					int zoomLevel = mCurrentZoomLevel-1;
					if (zoomLevel<0) zoomLevel = 0;
					Log.d(TAG, "New zoom level: " + zoomLevel);
//					mCurrentZoomLevel = zoomLevel;
					thetachange = (mTouchDownParameters.end-mTouchDownParameters.start)/(double)mZoomLevels[zoomLevel];
					Log.d(TAG, "New zoom level: " + zoomLevel + " " + thetachange);
				} else if (thetachange>1.2f) {
					int zoomLevel = mCurrentZoomLevel+1;
					if (zoomLevel>=mZoomLevels.length) zoomLevel = mZoomLevels.length-1;
					Log.d(TAG, "New zoom level: " + zoomLevel + " " + thetachange);
					thetachange = (mTouchDownParameters.end-mTouchDownParameters.start)/(double)mZoomLevels[zoomLevel];
				}
				else
					thetachange = 1.0f;
				long nstart = centertime - (long)((centertime-mTouchDownParameters.start)*thetachange);
				long nend = centertime + (long)((mTouchDownParameters.end-centertime)*thetachange);
*/
				long nstart = mTouchDownParameters.start;
				long nend = mTouchDownParameters.end;
				float change = 1.0f;
				int zoomLevel=mCurrentZoomLevel;
				if (thetachange<0.8f) {
					zoomLevel = mCurrentZoomLevel-1;
					if (zoomLevel<0) zoomLevel = 0;
//					nstart = nend - mZoomLevels[zoomLevel];
					change = mZoomLevels[zoomLevel]/(float)mZoomLevels[mCurrentZoomLevel];
					Log.d(TAG, "Zoomlevel: " + zoomLevel);
				} else if (thetachange>1.2f) {
					zoomLevel = mCurrentZoomLevel+1;
					if (zoomLevel>=mZoomLevels.length) zoomLevel = mZoomLevels.length-1;
//					nstart = nend - mZoomLevels[zoomLevel];
					change = mZoomLevels[zoomLevel]/(float)mZoomLevels[mCurrentZoomLevel];
					Log.d(TAG, "Zoomlevel: " + zoomLevel);
				} else {
					thetachange = 1.0f;
				}
				nstart = centertime - (long)((centertime - mTouchDownParameters.start)*change);
				nend = centertime + (long)((mTouchDownParameters.end - centertime)*change);
				nstart = nend-mZoomLevels[zoomLevel];

//				if (nend>now)
//					nend = now;
//				if (nend<=now && nstart<nend-10*1000 && nstart>nend-5*24*60*60*1000)
				if (nstart<nend-10*1000 && nstart>nend-10*24*60*60*1000)	// max span here is 10 days
				{
					Parameters p = mViewPort.getParameters();
					p.start = nstart;
					p.end = nend;
					mViewPort.setParameters(p);
				} else {
					Log.d(TAG, "Out of bounds");
				}
//				Log.d(TAG, "center " + centertime + "start: " + nstart + " end: " + nend + " tc: " + thetachange);
			}
		}

		return false;
	}

	@Override
	public synchronized void updateFrame(int time) {
		Parameters p = mViewPort.getParameters();
		
		// anim zoom
		if (mAnimate)
		{
			if (Math.abs(p.left-mAnimLeft)<0.01f)
			{
				mAnimate = false;
				p.left = mAnimLeft;
				p.right = mAnimRight;
				p.up = mAnimUp;
				p.down = mAnimDown;
			} 
			else
			{
				p.left += (mAnimLeft - p.left) / 5.0f;
				p.right += (mAnimRight - p.right) / 5.0f;
				p.up += (mAnimUp - p.up) / 5.0f;
				p.down += (mAnimDown - p.down) / 5.0f;
			}
		}
		
		long now = new Date().getTime();
		// zoom time
/*		if (mIsZoomingTime)
		{
			long centertime = (mTouchDownSpiralPoint.time + mTouchDownSpiralPoint2.time) / 2;
//			mZoomTimeTheta += mZoomTimeThetaDelta-1;
			mZoomTimeTheta = mZoomTimeThetaDelta;
			double thetachange = mZoomTimeTheta;
//			p.start = centertime - (long)((centertime-mTouchDownParameters.start)*(1+(thetachange-1)*1));
//			p.end = centertime - (long)((centertime-mTouchDownParameters.end)*(1+(thetachange-1)*1));
//			long nstart = centertime - (long)((centertime-p.start)*(1+(thetachange-1)*0.1));
//			long nend = centertime - (long)((centertime-p.end)*(1+(thetachange-1)*0.1));
			long nstart = centertime - (long)((centertime-mTouchDownParameters.start)*thetachange);
			long nend = centertime + (long)((mTouchDownParameters.end-centertime)*thetachange);
//			if (p.end>now) p.end = now;
//			if (p.start>p.end-10*1000) p.start = p.end-10*1000;
//			if (p.start<p.end-24*60*60*1000) p.start = p.end-24*60*60*1000;
			if (nend<=now && nstart<nend-10*1000 && nstart>nend-60*60*1000)
			{
				p.start = nstart;
				p.end = nend;
			}
//			Log.d(TAG, "" + p.start + " - " + p.end + " = " + (p.end-p.start));
			Log.d(TAG, "center " + centertime + "start: " + nstart + " end: " + nend + " tc: " + thetachange);
		}*/

		// animate start / end
/*		if (mEndPulled!=0.0f)
		{
//			p.end += mEndPulled/1000.0f;
			p.end += (int)mEndPulled;
			if (p.end>now)
			{
				p.end = now;
				mEndAtRealtime = true;
			}
			if (p.end-1000*10<p.start) p.end = p.start+1000*10;
			if (mEndPulled<0) mEndAtRealtime = false;
			
			Log.d(TAG, "(" + p.start + ", " + p.end + ")" + " - (" + mStartPulled + ", " + mEndPulled + ")");
		}
		if (mStartPulled!=0.0f)
		{
//			p.start += mStartPulled/1000.0f;
			p.start += (int)mStartPulled;
			if (p.start>=p.end-1000*10) p.start = p.end-1000*10;
			
			Log.d(TAG, "(" + p.start + ", " + p.end + ")" + " - (" + mStartPulled + ", " + mEndPulled + ")");
		}*/
		if (mUpdateRealtime)
		{
			long shift = (now-500) - p.end;
			p.end += shift;
			p.start += shift;
		}

		mViewPort.setParameters(p);
	}

	@Override
	public void updateScreenSize(Camera camera, int width, int height) {
		mWidth = width;
		mHeight = height;
	}

	@Override
	public synchronized void clicked(Vector<Callback.ClickData> ids) { //String id) {
//		Log.d("Core", "Clicked object: " + id);

		for (Callback.ClickData clickData : ids)
		{
			Log.d("Core", "Clicked object: " + clickData.objectId + " from pointerid: " +clickData.clickId);
			Pointer pointer = getPointer(clickData.clickId); //mPickPointerId);
			pointer.isPicking = false;
			if (clickData.objectId == null)
			{
				postponedPointerDown(clickData.clickId);
			}
//			else if (clickData.objectId.compareTo("EffortMarker")==0)
//			{
//				dragEffortMarker(pointer);
//			}
			else if (clickData.objectId.compareTo("TMOVEMENT")==0) {
				Mesh20 mesh = (Mesh20)Core.getInstance().getRoot().getMeshInstance("Movement");
				mesh.setVisible(!mesh.isVisible());
				((UIElement)Core.getInstance().getRoot().getMeshInstance("TMOVEMENT")).setOpacity(mesh.isVisible() ? 1.0f : 0.2f);
			}
/*			else if (clickData.objectId.compareTo("TENERGY")==0) {
				Mesh20 mesh = (Mesh20)Core.getInstance().getRoot().getMeshInstance("Energy");
				mesh.setVisible(!mesh.isVisible());
				mesh = (Mesh20)Core.getInstance().getRoot().getMeshInstance("NEnergy");
				mesh.setVisible(!mesh.isVisible());
			}*/
			else if (clickData.objectId.compareTo("TGSR")==0) {
				Mesh20 mesh = (Mesh20)Core.getInstance().getRoot().getMeshInstance("Spiral");
				mesh.setVisible(!mesh.isVisible());
				((UIElement)Core.getInstance().getRoot().getMeshInstance("TGSR")).setOpacity(mesh.isVisible() ? 1.0f : 0.2f);
			} 
			else if (clickData.objectId.compareTo("TTAGS")==0) {
				Mesh20 mesh = (Mesh20)Core.getInstance().getRoot().getMeshInstance("UserTags");
				mesh.setVisible(!mesh.isVisible());
				((UIElement)Core.getInstance().getRoot().getMeshInstance("TTAGS")).setOpacity(mesh.isVisible() ? 1.0f : 0.2f);
			}
			else if (clickData.objectId.compareTo("TSCROLLP")==0) {
				mUpdateRealtime = false;
				// scroll earlier
				Parameters param = mViewPort.getParameters();
				param.start -= mZoomLevels[mCurrentZoomLevel];
				param.end -= mZoomLevels[mCurrentZoomLevel];
				mViewPort.setParameters(param);
			} else if (clickData.objectId.compareTo("TSCROLLN")==0) {
				// scroll later
				Parameters param = mViewPort.getParameters();
				param.start += mZoomLevels[mCurrentZoomLevel];
				param.end += mZoomLevels[mCurrentZoomLevel];
				// cap to realtime...
				long timeDiff = System.currentTimeMillis() - param.end;
				if (timeDiff<0) {
					param.start += timeDiff;
					param.end += timeDiff;
					mUpdateRealtime = true;
				}
				mViewPort.setParameters(param);
			}
			else if (clickData.objectId.compareTo("TOVERVIEWSCROLLP") == 0 ) {
				mCoreView.getOverview().previous();
			}
			else if (clickData.objectId.compareTo("TOVERVIEWSCROLLN") == 0 ) {
				mCoreView.getOverview().next();				
			}
			else if (clickData.objectId.compareTo("TOVERVIEW")==0) {
				if (!mOverview) {
					mCoreView.showOverview();
					mOverview = true;
					mFirstPointerUpSinceScreenSwitch = true;
					zoomOutToOverview();
				} else {
					mCoreView.hideOverview();
					zoomToNormal();
					mOverview = false;
				}
			}
			else if (clickData.objectId.contains("Status.bluetooth")) {
				mDialogHandler.sendEmptyMessage(DialogActivity.SHOW_BLUETOOTH_DIALOG);
			}
			else if (clickData.objectId.compareTo("TAGICON") == 0) {
				mDialogHandler.sendEmptyMessage(DialogActivity.SHOW_STORE_TAG_DIALOG);
			}
			else if(clickData.objectId.compareTo("UserTags") == 0) {
				((UserTagRenderer)Core.getInstance().getRoot().getMeshInstance("UserTags")).click(pointer.mPosition.x, pointer.mPosition.y, mDialogHandler);
				
			}
			else if(clickData.objectId.contains("Timespan")) {
				int zoomLevel = (mCurrentZoomLevel + 1) % mZoomLevels.length;
				Parameters p = mViewPort.getParameters();
				long centertime = (p.start+p.end) / 2;
				float change = mZoomLevels[zoomLevel]/(float)mZoomLevels[mCurrentZoomLevel];
				long nstart = centertime - (long)((centertime - p.start)*change);
				long nend = centertime + (long)((p.end - centertime)*change);
				nstart = nend-mZoomLevels[zoomLevel];
				p.start = nstart;
				p.end = nend;
				mViewPort.setParameters(p);
				mCurrentZoomLevel =  zoomLevel;
			}
		}
	}
	
//	private void dragEffortMarker(Pointer pointer)
//	{
//		mIsDragging = true;
//		pointer.mDraggable = mEffortController;
//		pointer.mDraggable.down(pointer.mPosition.x, pointer.mPosition.y);
//	}

	public void setPickHandler(PickHandler handler)
	{
		mPickHandler = handler;
	}

/*	protected class EffortController implements Draggable
	{
		private static final float ZOOM_SCALE = 0.2f;
		private static final float MAX_LEVEL_T = 0.2f;
		private static final float SLOPE_X = 0; //-1 / 1.4142135623731f;
		private static final float SLOPE_Y = 1; //1 / 1.4142135623731f;
		private boolean mIsDragging = false;
		private PointF mDownPoint;
		private UserTags.UserTag mCurrentLevel = null;

		private Parameters mParametersOnDown;

		@Override
		public PointF position() {
			return null;
		}

		@Override
		public boolean down(float x, float y) {
			mIsDragging = true;
			
			mDownPoint = new PointF(x,y);

			mCurrentLevel = AHState.getInstance().mEnergyLevels.getCurrent();

			// save old
			mParametersOnDown = mViewPort.getParameters();

			// zoom out
//			Parameters params = mViewPort.getParameters();
//			params.left = Parameters.LEFT_BOUND;
//			params.right = Parameters.RIGHT_BOUND;
//			params.up = Parameters.UP_BOUND;
//			params.down = Parameters.DOWN_BOUND;
//			mViewPort.setParameters(params);
			// animate
			mAnimate = true;
			mAnimLeft = Parameters.LEFT_BOUND*ZOOM_SCALE;
			mAnimRight = Parameters.RIGHT_BOUND*ZOOM_SCALE;
			mAnimUp = Parameters.UP_BOUND*ZOOM_SCALE;
			mAnimDown = Parameters.DOWN_BOUND*ZOOM_SCALE;

			move(x,y);
			return false;
		}

		@Override
		public boolean move(float x, float y) {
			if (!mIsDragging) return false;

			PointF p = fromScreen(new PointF(x,y));
			float nx = SLOPE_X;
			float ny = -SLOPE_Y;
			float t = p.x*nx + p.y*ny;
			t = Math.max( - MAX_LEVEL_T, Math.min(t, MAX_LEVEL_T) );
			x = nx*t; y = ny*t;

			return true;
		}

		@Override
		public boolean up(float x, float y) {
			mIsDragging = false;
			
			// calc new energy point
			PointF p = fromScreen(new PointF(x,y));
			float nx = -SLOPE_X; //1 / 1.4142135623731f;
			float ny = SLOPE_Y; // -1 / 1.4142135623731f;
			float t = -(p.x*nx + p.y*ny);
			t = Math.max( - MAX_LEVEL_T, Math.min(t, MAX_LEVEL_T) );
//			x = nx*t; y = ny*t;
			float energy = t; //dist(p.x,p.y,0,0);
			energy /= MAX_LEVEL_T; //ZOOM_SCALE;
			if (Math.abs(energy)>0.15)
			{
//				energy = p.x>0?energy:-energy;
				Log.d(TAG, "Set Energylevel to " + energy);
				
				// set the energy
				if (mCurrentLevel!=null)
				{
					mCurrentLevel.setLevel(energy);
					mCurrentLevel = null;
				}
				else
				{
					AHState.getInstance().mEnergyLevels.addLevel(energy);
				}
			}
			
			// restore zoom
			// animate
			mAnimate = true;
			mAnimLeft = mParametersOnDown.left;
			mAnimRight = mParametersOnDown.right;
			mAnimUp = mParametersOnDown.up;
			mAnimDown = mParametersOnDown.down;

			return false;
		}

		@Override
		public boolean isDragging() {
			return mIsDragging;
		}
		
	}*/
}
