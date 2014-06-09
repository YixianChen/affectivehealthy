package se.sics.ah3.graphics;

import java.util.HashMap;
import android.os.SystemClock;
import se.sics.ah3.AHState;
import se.sics.ah3.service.AHService;

public class UIStatusElements extends MeshNode {
	
	private HashMap<String, UIElement> mElementMap;
	private float mSlowFlashFrequency = 1.0f;
	private float mFastFlashFrequency = 2.0f;
		
	private AHService.BTStatus mBTStatus = AHService.BTStatus.WAITING;
	
	private boolean mBatteryAlert = false;
	
	// flash timer stuff
	private boolean mSlowFlash = false;
	private boolean mFastFlash = false;
	
	public UIStatusElements(String id, float posX, float posY, int alignX) {
		super(id);
		mElementMap = new HashMap<String, UIElement>();
		addSymbolElements();
		this.translate(posX, posY, 0.0f);		
	}
	
	private void addSymbolElements() {
		String[] symbolNames = {"bluetooth grey", "bluetooth blue", "battery alert"};
		for(int i = 0; i < symbolNames.length; i++) {
			addSymbolElement(symbolNames[i]);
		}		
	}
	
	private void addSymbolElement(String symbolName) {
		UIElement symbolElement = new UIElement(getId() + "." + symbolName, symbolName, 0.0f, 0.0f, UIElement.ALIGN_LEFT, true);
		
		if(symbolName.compareTo("battery alert") == 0) {
			symbolElement.setPickable(false);
		}
		
		symbolElement.setPickScale(4.0f, 2.0f);
		add(symbolElement);
		mElementMap.put(symbolName, symbolElement);
	}
		
	@Override
	public void draw(Camera camera, boolean pick) {
		updateStatus();
		
		if(visible) {
			if(mBTStatus == AHService.BTStatus.CONNECTING) {
				drawSymbol(camera, pick, "bluetooth grey", 0.21f, 0.0f, mSlowFlash ? 1.0f : 0.0f);
			}
			else if(mBTStatus == AHService.BTStatus.DOWNLOADING) {
				drawSymbol(camera, pick, "bluetooth blue", 0.21f, 0.0f, mFastFlash ? 1.0f : 0.2f);
			}
			else if(mBTStatus == AHService.BTStatus.REALTIME) {
				drawSymbol(camera, pick, "bluetooth blue", 0.21f, 0.0f, 1.0f);								
			}
			else if(mBTStatus == AHService.BTStatus.ERROR || mBTStatus == AHService.BTStatus.BLUETOOTH_OFF || mBTStatus == AHService.BTStatus.NO_DEVICE_SET) {
				drawSymbol(camera, pick, "bluetooth grey", 0.21f, 0.0f, 1.0f);
			}
			
			if(mBatteryAlert) {
				drawSymbol(camera, pick, "battery alert", 0.375f, 0.0f, 1.0f);
			}
		}
	}
	
	private void updateStatus() {
		AHState ah = AHState.getInstance();
		mBTStatus = ah.mService.getStatus();
		
		if(ah.mService!=null && ah.mService.isBatteryLow()) {
			mBatteryAlert = true;
		}
		else {
			mBatteryAlert = false;
		}
		
		updateFlashTimers();
	}

	private void updateFlashTimers() {
		float time = SystemClock.uptimeMillis() * 0.001f;
		
		if( time * mSlowFlashFrequency % 1.0f > 0.5f) {
			mSlowFlash = true;
		}
		else {
			mSlowFlash = false;
		}

		if( time * mFastFlashFrequency % 1.0f  > 0.5f) {
			mFastFlash = true;
		}
		else {
			mFastFlash = false;
		}
	}

	private void drawSymbol(Camera camera, boolean pick, String symbolName, float x, float y, float opacity) {
		UIElement symbolElement = mElementMap.get(symbolName);
		symbolElement.translate(x, y, 0.0f);
		symbolElement.setOpacity(opacity);
		symbolElement.draw(camera, pick);
	}
	
}