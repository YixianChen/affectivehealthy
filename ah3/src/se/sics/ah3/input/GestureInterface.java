package se.sics.ah3.input;

import android.view.MotionEvent;

/**
 * Interface for gestures. Meant so that the activity propagates its touch event to an implementation of
 * GestureInterface which calls back the handler.
 * 
 * @version 1
 * @author mareri
 *
 */

public interface GestureInterface {
	public boolean onTouchEvent(final MotionEvent event);
	//public void setGestureHandler(GestureHandler gestureHandler);
}
