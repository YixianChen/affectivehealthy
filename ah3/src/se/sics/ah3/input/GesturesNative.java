package se.sics.ah3.input;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class GesturesNative implements GestureInterface, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {
	private GestureHandler mHandler;
	private float mLastSpan;
    private long mLastNonTapTouchEventTimeNS;
    
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;
    
    // constants
    private static final int SWIPE_STRAIGHT_PATH_HORIZONTAL = 100;
    private static final int SWIPE_STRAIGHT_PATH_VERTICAL = 100;
    private static final int SWIPE_DISTANCE_HORIZONTAL = 100;
    private static final int SWIPE_DISTANCE_VERTICAL = 100;
    private static final int SWIPE_VELOCITY_HORIZONTAL = 100;
    private static final int SWIPE_VELOCITY_VERTICAL = 100;
	
	public GesturesNative(Context context, GestureHandler gestureHandler) {
		mHandler = gestureHandler;
		mLastSpan = 0;
		mLastNonTapTouchEventTimeNS = 0;
		
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setIsLongpressEnabled(false);
		mScaleDetector = new ScaleGestureDetector(context, this);
	}
	
	public boolean onTouchEvent(final MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		mScaleDetector.onTouchEvent(event);
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		int direction = 0;
		float dx = e1.getX() - e2.getX();
		float dy = e1.getY() - e2.getY();
		
		if (Math.abs(dy) < SWIPE_STRAIGHT_PATH_HORIZONTAL && Math.abs(velocityX) > SWIPE_VELOCITY_HORIZONTAL && Math.abs(dx) > SWIPE_DISTANCE_HORIZONTAL) {
			direction |= dx > 0 ? GestureHandler.DIRECTION_RIGHT : GestureHandler.DIRECTION_LEFT;
		}
		if (Math.abs(dx) < SWIPE_STRAIGHT_PATH_VERTICAL) {
			return false;
		}
		
		if(direction != 0) {
			mHandler.fling(direction, 0f);
		}
		return direction == 0 ? false : true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, final float distanceX,
			final float distanceY) {
		mHandler.drag(distanceX, distanceY);
		mLastNonTapTouchEventTimeNS = System.nanoTime();
		return true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		final float amount = detector.getCurrentSpan() - mLastSpan;
		mHandler.zoom(amount);
		mLastSpan = detector.getCurrentSpan();
        mLastNonTapTouchEventTimeNS = System.nanoTime();
        return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		mLastSpan = detector.getCurrentSpan();
		return true;
	}

	// unused input methods
	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {}
	@Override
	public void onShowPress(MotionEvent e) {}
	@Override
	public void onLongPress(MotionEvent e) {}
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	/*@Override
	public void setGestureHandler(GestureHandler gestureHandler) {
		mHandler = gestureHandler;
	}*/
}
