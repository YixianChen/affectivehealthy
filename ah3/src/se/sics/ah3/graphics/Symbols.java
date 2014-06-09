package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;
import java.util.HashMap;

import se.sics.ah3.R;

import android.opengl.GLES20;
import android.util.Log;

public class Symbols {

	class Symbol {
		float u0, v0, u1, v1;
		float width;
		float height;
		final static float textureWidth = 512.0f;
		final static float textureHeight = 512.0f;
		
		public Symbol(float u0, float v0, float u1, float v1, float scale) {
			// magicScale is to map from symbols.png to something
			// similar on screen...
			float magicScale = 2.7f * scale;
			this.width = (u1 - u0) * magicScale / textureWidth;
			this.height = (v1 - v0) * magicScale / textureHeight;
			this.u0 = u0 / textureWidth;
			this.v0 = v0 / textureHeight;
			this.u1 = u1 / textureWidth;
			this.v1 = v1 / textureHeight;
		}
	}

	HashMap<String, Symbol> mSymbols = new HashMap<String, Symbol>();
	int mTextureId = 0;
	
	static Symbols mInstance = null;
	
	public static Symbols getInstance() {
		if(mInstance == null) {
			mInstance = new Symbols();
		}
		return mInstance;
	}
	
	private Symbols() {
	}
	
	public void init() {
		// load texture
		mTextureId = Core.getInstance().loadGLTexture(R.raw.symbols, new Texture2DParameters());
				
		// set up all mappings
		loadButtonTextsAndDivider(1.0f);		
		loadTimespanTexts(0.875f);
		loadArrows(1.0f);
		loadDateAndTimeSymbols(1.0f);
		loadStatusSymbols(1.0f);
		loadTaggingSymbols(1.0f);
	}

	private void loadDateAndTimeSymbols(float scale) {
		float step = 11.0f;
		float height = 13.0f;
		// first 0 through 9
		for(int i=0; i<10; i++) {
			mSymbols.put(""+i, new Symbol(20 + step * i, 394, 20 + step * (i + 1), 394 + height, scale));			
		}
		
		// months
		step = 30.0f;
		String[] names = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
		for(int i=0; i<names.length; i++) {
			mSymbols.put(names[i], new Symbol(34 + step * i, 372, 34 + step * (i + 1), 372 + height, scale));
		}
		
		// other symbols
		mSymbols.put(":", new Symbol(129, 394, 136, 394 + height, scale));
		mSymbols.put(".", new Symbol(136, 394, 143, 394 + height, scale));
		// reusing . as ' because it's not in the png...
		mSymbols.put("'", new Symbol(136, 404, 143, 404 + height, scale));
	}

	private void loadArrows(float scale) {
		mSymbols.put("arrow left", 	new Symbol(18, 229, 48, 268, scale));
		mSymbols.put("arrow right", new Symbol(170, 229, 200, 268, scale));
	}

	private void loadTimespanTexts(float scale) {
		float step = 39.0f;
		mSymbols.put("1 week", 		new Symbol(290, 87 + step * 0, 511, 87 + step * 1, scale));
		mSymbols.put("24 hours", 	new Symbol(304, 87 + step * 1, 511, 87 + step * 2, scale));
		mSymbols.put("6 hours", 	new Symbol(328, 87 + step * 2, 511, 87 + step * 3, scale));
		mSymbols.put("1 hour", 		new Symbol(358, 87 + step * 3, 511, 87 + step * 4, scale));
		mSymbols.put("10 min", 		new Symbol(372, 87 + step * 4, 511, 87 + step * 5, scale));
		mSymbols.put("1 min", 		new Symbol(396, 87 + step * 5, 511, 87 + step * 6, scale));
	}

	private void loadButtonTextsAndDivider(float scale) {
		mSymbols.put("skin conductance", new Symbol(375, 330, 511, 344, scale));
		mSymbols.put("user tagging", new Symbol(214, 309, 311, 323, scale));
		mSymbols.put("movement", new Symbol(132, 330, 207, 344, scale));
		mSymbols.put("compare", new Symbol(316, 394, 380, 408, scale));
		
		mSymbols.put("affective health", new Symbol(386, 394, 511, 408, scale));
		mSymbols.put("divider", new Symbol(10, 428, 420, 429, scale));
	}

	
	private void loadStatusSymbols(float scale) {
		mSymbols.put("bluetooth grey", new Symbol(81, 285, 101, 318, scale));
		mSymbols.put("bluetooth blue", new Symbol(102, 285, 122, 318, scale));
		mSymbols.put("battery alert", new Symbol(150, 285, 170, 318, scale));
	}
	
	private void loadTaggingSymbols(float scale) {
		mSymbols.put("tag icon", new Symbol(47, 285, 79, 318, scale));
		mSymbols.put("tag text", new Symbol(4, 329, 31, 344, scale));
		mSymbols.put("tag spiral icon", new Symbol(125, 282, 139, 321, scale));
	}
	
	public int getTextureId() {
		return mTextureId;
	}
	
	public Symbol getSymbol(String symbolName) {
		Symbol symbol = mSymbols.get(symbolName);
		if(symbol == null) {
			throw new RuntimeException("no such symbol: " + symbolName);
		}
		return symbol;
	}
}
