package se.sics.ah3.input;

/**
 * Callback for GestureInterface
 * @author mareri
 *
 */

public interface GestureHandler {
	// constants for direction
	public static int DIRECTION_UP = 0;
	public static int DIRECTION_DOWN = 1;
	public static int DIRECTION_LEFT = 2;
	public static int DIRECTION_RIGHT = 3;
	// public methods
	public void drag(float dx, float dy);
	public void zoom(float amount);
	public void fling(int direction, float magnitude);
}
