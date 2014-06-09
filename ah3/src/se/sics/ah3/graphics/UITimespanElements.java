package se.sics.ah3.graphics;

import java.util.Date;
import java.util.HashMap;

import se.sics.ah3.AHState;
import se.sics.ah3.interaction.Parameters;

public class UITimespanElements extends MeshNode {
	private static final String[] mSymbolNames = {"1 week", "24 hours", "6 hours", "1 hour", "10 min", "1 min"};
	
	private HashMap<String, UIElement> mElementMap;
	private float mPosX;
	private float mPosY;
	private int mAlignX;
	
	public UITimespanElements(String id, float posX, float posY, int alignX) {
		super(id);
		mPosX = posX;
		mPosY = posY;
		mAlignX = alignX;
		mElementMap = new HashMap<String, UIElement>();

		for(int i = 0; i < mSymbolNames.length; i++) {
			String symbolName = mSymbolNames[i];
			UIElement symbolElement = new UIElement(getId() + "." + symbolName, symbolName, mPosX, mPosY, mAlignX, true);
			this.add(symbolElement);
			
			mElementMap.put(symbolName, symbolElement);
		}
		hideSymbolElements();
		mElementMap.get("1 min").setVisible(true);
	}
	
	private void hideSymbolElements() {
		for (UIElement symbolElement : mElementMap.values()) {
		    symbolElement.setVisible(false);
		}
	}
	
	@Override
	public void draw(Camera camera, boolean pick) {
		updateSpanAndPickSymbol();	
		super.draw(camera, pick);
	}
	
	private void updateSpanAndPickSymbol() {
		Parameters parameters = AHState.getInstance().mViewPort.getParameters();

		Date currentDate = new Date(parameters.end);
		Date endDate = new Date(parameters.start);

		long timeLine = currentDate.getTime() - endDate.getTime();

		double days = timeLine / (double) Parameters.DAY;
		double hours = timeLine / (double) Parameters.HOUR;
		double minutes = timeLine / (double) Parameters.MINUTES;
		
		hideSymbolElements();
		if(compareWithEpsilon(minutes, 1.0)) {
			mElementMap.get("1 min").setVisible(true);
		}
		else if(compareWithEpsilon(minutes, 10.0)) {
			mElementMap.get("10 min").setVisible(true);
		}
		else if(compareWithEpsilon(hours, 1.0)) {
			mElementMap.get("1 hour").setVisible(true);
		}
		else if(compareWithEpsilon(hours, 6.0)) {
			mElementMap.get("6 hours").setVisible(true);
		}
		else if(compareWithEpsilon(hours, 24.0)) {
			mElementMap.get("24 hours").setVisible(true);
		}
		else if(compareWithEpsilon(days, 7.0)) {
			mElementMap.get("1 week").setVisible(true);
		}
	}
		
	private Boolean compareWithEpsilon(double var1, double var2) {
		float epsilonRatio = 0.001f;
		if(var1 > (var2 * (1 - epsilonRatio)) && var1 < (var2 * (1 + epsilonRatio))) {
			return true;
		}
		return false;
	}
}