package se.sics.ah3.old;

import android.util.Log;

public class AccelProcessor implements SignalProcessor {
	final int frequency = 200;//200 sps;
	final int windowSize = frequency/4; //part of a second - window
	final int calibSize = 1000;
	private double[] wndw = new double[windowSize];
	private int[] xBuf = new int[calibSize];
	private int[] yBuf = new int[calibSize];
	private int[] zBuf = new int[calibSize];
	private int xSum = 0;
	private int ySum = 0;
	private int zSum = 0;
	private int bufPos = 0;
	private double integral = 0;
	private int stabilityMargin = 50; 
	private int stabilityCounter = 0; 
	private boolean xIsCalibrated = false;
	private boolean yIsCalibrated = false;
	private boolean zIsCalibrated = false;
	private boolean[] positiveGCalibrated; 
	private double[] positiveG;
	private double[] negativeG;
	private boolean[] negativeGCalibrated;
	private int[] zeroG = new int[3];
	private double[] x_derv = new double [4];
	
	double _xMean;
	double _yMean;
	double _zMean;
	double _xDiff;
	double _yDiff;
	double _zDiff;
	double _xOff;
	double _yOff;
	double _zOff;
	int _oldZeroG;
	double _absAcc;
	double _m;
	double _dif;
	double _temp;
	double _accDeriv;
	double _accIntegral;
	double _d5sy;
	
	public AccelProcessor()
	{
		positiveGCalibrated = new boolean[3];
		negativeGCalibrated = new boolean[3];
		for(int i = 0; i < 3; i++)
		{
			positiveGCalibrated[i] = false;
			negativeGCalibrated[i] = false;
		}
		positiveG = new double[3];
		negativeG = new double[3];
	}
	
	public int process(int signal) {
		int[] signalA = new int[1];
		signalA[0] = signal;
		return process(signalA);
	}

	
	public int process(int[] signal) {
		//expect 3 axis on signal[0,1 and 2]
		if(signal.length < 3)
			return -1;
		if(!isCalibrated())
		{
			calibrate(signal);
		//	Log.i("AccSensor", "Accelerometer is calibrating");
		}
		else
		{
			//Log.d("SensorAcc","calibrated");
		}
		
		_absAcc = 0;
		for(int i = 0; i < 3; i++)
		{
			_dif = (positiveG[i] - negativeG[i])/ 2.0;
			_m = 1;
			if(_dif != 0)
				_m = 1024/_dif;  
			
			_temp = _m*(signal[i] - zeroG[i]);
			_temp = _temp*_temp;
			_absAcc += _temp;
		}
		_absAcc = Math.sqrt(_absAcc);
		_accDeriv = Math.abs(derivative_5stencil(_absAcc));
		
		_accIntegral = windowedIntegral(_accDeriv);// should be g if node is perfectly still
		return (int)(_accIntegral+0.5);
	}
	
	private double windowedIntegral(double signal)
	{
		integral -= wndw[0];
		for(int i = 0; i < wndw.length-1; i++)
		{
			wndw[i] = wndw[i+1];
		}
		
		wndw[wndw.length-1] = signal;
		integral += signal;
		return integral/(double)wndw.length;
	}
	private boolean isCalibrated()
	{
		//boolean done = true;
		/*for(int i = 0; i < 3; i++)
		{
			if(!positiveGCalibrated[i] )
			{
				return false;
			}
			if(!negativeGCalibrated[i] )
			{
				return false;
			}
		}*/
	
		return (xIsCalibrated&&yIsCalibrated&&zIsCalibrated);
	}
	
	private boolean calibrate(int[] signal)
	{
		int x,y,z;
		x = signal[0];
		y = signal[1];
		z = signal[2];
		if(bufPos >= calibSize)
			bufPos = 0;
		xSum -= xBuf[bufPos];
		ySum -= yBuf[bufPos];
		zSum -= zBuf[bufPos];
		xBuf[bufPos] = x;
		yBuf[bufPos] = y;
		zBuf[bufPos] = z;
		xSum += xBuf[bufPos];
		ySum += yBuf[bufPos];
		zSum += zBuf[bufPos];
		bufPos++;
		_xMean = xSum/(double)calibSize;
		_yMean = ySum/(double)calibSize;
		_zMean = zSum/(double)calibSize;
		_xDiff = _xMean - x;
		_yDiff = _yMean - y;
		_zDiff = _zMean - z;
		if(Math.abs(_xDiff)<stabilityMargin && Math.abs(_yDiff)<stabilityMargin && Math.abs(_zDiff)<stabilityMargin)
			stabilityCounter++;
		else
			stabilityCounter = 0;
		if(stabilityCounter > calibSize)
		{
			//stable position reached
			_xOff = _xMean - 2048;
			_yOff = _yMean - 2048;
			_zOff = _zMean - 2048;
			//which one is influenced by g
			if(_xOff*_xOff > _yOff*_yOff && _xOff*_xOff > _zOff*_zOff)
			{
				//x is influenced by g
				if(_xOff>0)
				{
					positiveG[0] = _xMean;
					positiveGCalibrated[0] = true;
				}
				else
				{
					negativeG[0] = _xMean;
					negativeGCalibrated[0] = true;
				}
				_oldZeroG = zeroG[1];
				if(_oldZeroG == 0)
					zeroG[1] = (int)(_yMean+0.5);
				else
				{
					zeroG[1] = (int)(((_yMean+_oldZeroG)/2.0)+0.5);
				}
				yIsCalibrated = true;
				
				_oldZeroG = zeroG[2];
				if(_oldZeroG == 0)
					zeroG[2] = (int)(_zMean+0.5);
				else
				{
					zeroG[2] = (int)(((_zMean+_oldZeroG)/2.0)+0.5);
				}
				zIsCalibrated = true;	
			}
			
			if(_zOff*_zOff > _yOff*_yOff && _xOff*_xOff < _zOff*_zOff)
			{
				//z is influenced by g
				if(_zOff>0)
				{
					positiveG[2] = _zMean;
					positiveGCalibrated[2] = true;
				}
				else
				{
					negativeG[2] = _zMean;
					negativeGCalibrated[2] = true;
				}
				
				int oldZeroG;
				
				oldZeroG = zeroG[1];
				if(oldZeroG == 0)
					zeroG[1] = (int)(_yMean+0.5);
				else
				{
					zeroG[1] = (int)(((_yMean+oldZeroG)/2.0)+0.5);
				}
				yIsCalibrated = true;
				
				
				oldZeroG = zeroG[0];
				if(oldZeroG == 0)
					zeroG[0] = (int)(_zMean+0.5);
				else
				{
					zeroG[0] = (int)(((_xMean+oldZeroG)/2.0)+0.5);
				}
				xIsCalibrated = true;
			}
			
			if(_xOff*_xOff < _yOff*_yOff && _yOff*_yOff > _zOff*_zOff)
			{
				//y is influenced by g
				if(_yOff>0)
				{
					positiveG[1] = _yMean;
					positiveGCalibrated[1] = true;
				}
				else
				{
					negativeG[1] = _yMean;
					negativeGCalibrated[1] = true;
				}
				
				int oldZeroG;
				
				oldZeroG = zeroG[0];
				if(oldZeroG == 0)
					zeroG[0] = (int)(_xMean+0.5);
				else
				{
					zeroG[0] = (int)(((_xMean+oldZeroG)/2.0)+0.5);
				}
				xIsCalibrated = true;
				
				oldZeroG = zeroG[2];
				if(oldZeroG == 0)
					zeroG[2] = (int)(_zMean+0.5);
				else
				{
					zeroG[2] = (int)(((_zMean+oldZeroG)/2.0)+0.5);
				}
				zIsCalibrated = true;
			}
		}
		return false;
	}
	
	private double derivative_5stencil(double data)
	{
		  /*y = 1/8 (2x( nT) + x( nT - T) - x( nT - 3T) - 2x( nT -
		4T))*/
		  _d5sy = ((data *2) +  x_derv[3] - x_derv[1] - (x_derv[0]*2))/8;
		  
		  for (int i = 0; i < 3; i++)
				x_derv[i] = x_derv[i + 1];
		  x_derv[3] = data;
		  return _d5sy;
	}
	
	public String status()
	{
		String msg = "";
		if(xIsCalibrated)
			msg += "x";
		if(yIsCalibrated)
			msg += "y";
		if(zIsCalibrated)
			msg += "z";
		for(int i = 0; i < 3; i++)
		{
			msg += i+":";
			if(positiveGCalibrated[i])
				msg += "+";
			if(negativeGCalibrated[i])
				msg += "-";
		}
		return msg;
	}

	@Override
	public String calibrationStatus() {
		String calibStr = "";
		if(xIsCalibrated)
			calibStr += "X:";
		else
			calibStr += "!X:";
		
		if(yIsCalibrated)
			calibStr += "Y:";
		else
			calibStr += "!Y:";
		
		if(zIsCalibrated)
			calibStr += "Z:";
		else
			calibStr += "!Z:";
		// TODO Auto-generated method stub
		return calibStr;
	}

}
