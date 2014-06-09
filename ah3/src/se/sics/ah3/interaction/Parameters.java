package se.sics.ah3.interaction;

import java.util.Date;

public class Parameters {
	public final static float LEFT_BOUND 	= -1; //-1.12f*0.95f*(457/518f);
	public final static float RIGHT_BOUND 	=  1; //1.12f*0.95f*(457/518f);
	public final static float UP_BOUND 		= -1.6f; //-1.60f+0.2f*(605/619f) + 0.2f;
	public final static float DOWN_BOUND 	=  1.6f; //1.60f+0.2f*(605/619f) + 0.2f;
	/*public final static float LEFT_BOUND 	= -1.12f*0.95f;
	public final static float RIGHT_BOUND 	=  1.12f*0.95f;
	public final static float UP_BOUND 		= -1.60f+0.2f;
	public final static float DOWN_BOUND 	=  1.60f+0.2f;*/
	public float up, down, left, right;
	public long start, end;
	
	public final static long MINUTES = 1000 * 60;
	public final static long HOUR = MINUTES * 60;
	public final static long DAY = HOUR * 24;
	public final static long WEEK = DAY * 7;

	public Parameters() {
		long now = new Date().getTime();
		start = now-Controller.mZoomLevels[0]; //3*MINUTES;
		end = now;

		up = UP_BOUND;
		down = DOWN_BOUND;
		left = LEFT_BOUND;
		right = RIGHT_BOUND;
	}

	public float getUp() {
		return up;
	}

	public void setUp(float up) {
		this.up = up;
	}

	public float getDown() {
		return down;
	}

	public void setDown(float down) {
		this.down = down;
	}

	public float getLeft() {
		return left;
	}

	public void setLeft(float left) {
		this.left = left;
	}

	public float getRight() {
		return right;
	}

	public void setRight(float right) {
		this.right = right;
	}

	public float getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public float getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}
	
	public Parameters(Parameters p)
	{
		up = p.up;
		down = p.down;
		left = p.left;
		right = p.right;
		start = p.start;
		end = p.end;
	}
}
