package se.sics.ah3.graphics;

import java.util.Date;
import java.util.HashMap;
import se.sics.ah3.AHState;
import se.sics.ah3.interaction.Parameters;

public class UITimeEndElements extends MeshNode {
	
	private HashMap<String, UIElement> mElementMap;
	// these are our "cursors"
	float mCursorX = 0.0f;
	float mCursorY = 0.0f;
	
	private Parameters mParameters = null;
	private boolean mUseScreenSpaceCamera;
	public UITimeEndElements(String id, float posX, float posY, int alignX, boolean useScreenSpaceCamera) {
		super(id);
		mElementMap = new HashMap<String, UIElement>();
		addSymbolElements();
		this.translate(posX, posY, 0.0f);
		mUseScreenSpaceCamera = useScreenSpaceCamera;
	}
	
	public void setParameters(Parameters parameters) {
		mParameters = parameters;
	}
	
	private void addSymbolElements() {
		String[] symbolNames = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", 
				"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
				":", ".", "'"};
		for(int i = 0; i < symbolNames.length; i++) {
			addSymbolElement(symbolNames[i]);
		}		
	}
	
	private void addSymbolElement(String symbolName) {
		UIElement symbolElement = new UIElement(getId() + "." + symbolName, symbolName, 0.0f, 0.0f, UIElement.ALIGN_LEFT, mUseScreenSpaceCamera);
		add(symbolElement);
		mElementMap.put(symbolName, symbolElement);
	}
		
	@Override
	public void draw(Camera camera, boolean pick) {
		if(visible) {
			if(mUseScreenSpaceCamera) {
				camera = Core.SCREEN_SPACE_CAMERA;
			}
			
			Parameters parameters = mParameters != null ? mParameters : AHState.getInstance().mViewPort.getParameters();

			Date startDate = new Date(parameters.end);
			
			mCursorX = mCursorY = 0.0f;
			
			drawDate(camera, pick, startDate);
			mCursorX = 0.0f;
			mCursorY = 0.1f;
			drawTime(camera, pick, startDate);
		}
	}

	private void drawDate(Camera camera, boolean pick, Date startDate) {
		String day = CoreUtil.getFormattedDate("dd", startDate);
		String month = CoreUtil.getFormattedDate("MMM", startDate);
		String year = CoreUtil.getFormattedDate("yy", startDate);
		drawNumbers(camera, pick, day, 0.7f);
		drawSymbol(camera, pick, month, 1.0f);
		mCursorX += 0.02f;
		drawSymbol(camera, pick, "'", 0.7f);
		drawNumbers(camera, pick, year, 0.7f);
	}

	private void drawTime(Camera camera, boolean pick, Date startDate) {
		String hours = CoreUtil.getFormattedDate("HH", startDate);
		String minutes = CoreUtil.getFormattedDate("mm", startDate);
		String seconds = CoreUtil.getFormattedDate("ss", startDate);
		drawNumbers(camera, pick, hours, 0.7f);
		drawSymbol(camera, pick, ":", 0.8f);
		drawNumbers(camera, pick, minutes, 0.7f);
		drawSymbol(camera, pick, ":", 0.8f);
		drawNumbers(camera, pick, seconds, 0.7f);
	}
	
	private void drawNumbers(Camera camera, boolean pick, String numbers, float kerningScale) {
		for(int i=0; i<numbers.length(); i++) {
			String symbolName = ""+numbers.charAt(i);
			drawSymbol(camera, pick, symbolName, kerningScale);
		}
	}

	private void drawSymbol(Camera camera, boolean pick, String symbolName, float kerningScale) {
		UIElement symbolElement = mElementMap.get(symbolName);
		symbolElement.translate(mCursorX, mCursorY, 0.0f);
		symbolElement.draw(camera, pick);
		mCursorX += Symbols.getInstance().getSymbol(symbolName).width * kerningScale;
	}
}