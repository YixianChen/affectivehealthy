package se.sics.ah3.interaction;

import java.util.Vector;

import se.sics.ah3.graphics.Camera;

public interface Callback {
	public class ClickData { public int clickId; public String objectId; }
	public void updateFrame(int time);
	public void updateScreenSize(Camera camera, int width, int height);
	public void clicked(Vector<ClickData> ids);
}
