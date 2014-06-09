package se.sics.ah3.graphics;

import android.graphics.Color;

public class ColorMapper {
	private static ColorMapper _instance;
	public static ColorMapper getInstance()
	{
		if(_instance == null)
			_instance = new ColorMapper();
		return _instance;
	}
	private int iArousalStatesNum = 8;
	TAHArousalState[] iStates = new TAHArousalState[iArousalStatesNum];
	int[] iBorderColor = new int[2];
	
	public ColorMapper()
	{
		InitStates();
	}
	public int getErrorMapping(int rawValue)
	{
	switch(rawValue)
		{
		case Error.NO_SIGNAL:
			return TRgb(128,128,128,255);
			
		case Error.CONNECTION_LOSS:
			return TRgb(128,128,128,255);
			
		case Error.SENSOR_LAG:
			return TRgb(128,128,128,255);
			
		case Error.NO_DATA:
			return TRgb(0,0,0,50);
			
		case Error.FUTURE_DATA:
			return TRgb(255,255,255,50);
			
		case Error.UNKNOWN_ERROR:
		default:
			return TRgb(0,0,128,0);
		}
	}
	
	public int mapToColor(int aRawValue) {
		// TODO Auto-generated method stub		
		int oldRawValue = aRawValue;
		if(aRawValue < 0)
			return 0;
		if( aRawValue >=4000)
			return TRgb(255,0,0);//this should not happen
		if(iStates != null)
			{
			//find two nearest colors, interpolate
			//for now just return first slot
			int i = 0;
			while(iStates[i].iValue < aRawValue)
			{
				if(i == iArousalStatesNum)
					break;
				i++;
			}		
			//iterpolate
			double low, high;
			int lowColor, highColor;
			if( 0 == i)
				{
				low = 0;
				lowColor = iBorderColor[0];
				}
			else
				{
				low = iStates[i-1].iValue;
				lowColor = iStates[i-1].iColor;
				}
			if( i == 8)
				{
				high = 4000;
				highColor = iBorderColor[1];
				}
			else
				{
				high = iStates[i].iValue;
				highColor = iStates[i].iColor;
				}
			
			double delta = high - low;
			double inter  = 0;
			if(delta != 0)
				inter = (aRawValue - low)/delta;
			int r,g,b,a;
			r = (int)(Color.red(lowColor)*(1-inter) + Color.red(highColor)*(inter));
			g = (int)(Color.green(lowColor)*(1-inter) + Color.green(highColor)*(inter));
			b = (int)(Color.blue(lowColor)*(1-inter) + Color.blue(highColor)*(inter));
			a = (int)(Color.alpha(lowColor)*(1-inter) +Color.alpha(highColor)*(inter));
			return TRgb(r,g,b,a);
			}
		return TRgb(0,0,0,255);
	}
	private int TRgb(int r, int g, int b) {
		return Color.rgb(r, g, b);
	}
	private int TRgb(int r, int g, int b, int a) {
		// TODO Auto-generated method stub
		return Color.argb(a, r, g, b);
	}
	
	
	
	private void InitStates()
	{
	iBorderColor[0] = TRgb(0,50,50);
	
	iStates[0] = new TAHArousalState(20, TRgb(41,98,114));
	iStates[1] = new TAHArousalState(400, TRgb(56,105,92));
	iStates[2] = new TAHArousalState(700, TRgb(114,143,86));
	iStates[3] = new TAHArousalState(1100, TRgb(151,158,76));
	iStates[4] = new TAHArousalState(2000, TRgb(211,206,63));
	iStates[5] = new TAHArousalState(3000, TRgb(195,103,66));
	iStates[6] = new TAHArousalState(3800, TRgb(147,61,62));
	iStates[7] = new TAHArousalState(4000, TRgb(116,61,75));
		
	iBorderColor[1]= TRgb(102,40,78);
	}
	
	private class TAHArousalState
	{
		int iColor;
		int iValue;
		
		public TAHArousalState(int aValue, int aRgb)
		{
			iColor = aRgb;
			iValue = aValue;
		}
	};
}
