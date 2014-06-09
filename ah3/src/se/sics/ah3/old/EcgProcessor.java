package se.sics.ah3.old;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import android.os.Environment;
import android.util.Log;

public class EcgProcessor implements SignalProcessor {
	private int hr = 60;
	final double bpgain = 7.358264276;
	private double[] wbuffer = new double[10];
	private double[] bpxbuffer = new double [5];
	private double[] bpybuffer = new double [5];
	private double[] x_derv = new double [4];
	
	private double wIntegral = 0;
	private int bufferp = 0;
	private boolean isInit = false;
	public double _bandPassedSignal = 0;
	public double _tempSignal = 0;
	public double _windowedIntegral = 0;
	public double _derivativeSignal = 0;
	/*private File hrLogFile = null;
	private FileWriter hrWriter = null;*/
	private BufferedWriter hrOut = null;
	double _bpecg;
	double _deriv;
	double _temp;
	double _ecgIntegral;
	double _y;
	
	private void init()
	{
		wIntegral = 0;
		for(int i = 0; i < wbuffer.length; i++)
		{
			wbuffer[i] = 0;
		}
		for(int i = 0; i < bpxbuffer.length; i++)
		{
			bpxbuffer[i] = 0;
		}
		for(int i = 0; i < bpybuffer.length; i++)
		{
			bpybuffer[i] = 0;
		}
		isInit = true;
	}
	
	public int process(int ecg)
	{
		if(!isInit)
			init();
		return calcHeartEnegry(ecg);
	}
	public int process(int[] signal)
	{
		return process(signal[3]);
	}
	
	public void writeToLog(String msg)
	{
		if(hrOut == null)
		{
			try {
			    File root = Environment.getExternalStorageDirectory();
			    if (root.canWrite()){
			        File hrlogfile = new File(root, "hr.csv");
			        FileWriter hrwriter = new FileWriter(hrlogfile);
			        hrOut = new BufferedWriter(hrwriter);
			        hrOut.write(msg);
					hrOut.flush();
			    }
			} catch (IOException e) {
			    Log.e("HRLogging", "Could not write file " + e.getMessage());
			    hrOut = null;
			    return;
			}
		}
		else
		{
			try {
				hrOut.write(msg);
				hrOut.flush();
			} catch (IOException e) {
			    Log.e("HRLogging", "Could not write file " + e.getMessage());
			}
		}
		
	}

	private int calcHeartEnegry(int ecg)
	{
		_bpecg = bandpass(ecg);
		_deriv = derivative_5stencil(_bpecg);
		_temp = _deriv;//bpecg/40;
		_temp = _temp*_temp;
		_ecgIntegral = windowedIntegral(_temp);
		_bandPassedSignal = _bpecg;
		_tempSignal = _temp;
		_windowedIntegral = _ecgIntegral;
		_derivativeSignal = _deriv;
		String data = String.format("%d,%f,%f,%f,%f\n", ecg,_bandPassedSignal,_derivativeSignal,_tempSignal,_windowedIntegral);
		writeToLog(data);
		return (int)(_ecgIntegral/10);
	}
	
	private double bandpass(double signal) {
		for(int i = 0; i < bpxbuffer.length-1; i++)
		{
			bpxbuffer[i] = bpxbuffer[i+1];
		}
		bpxbuffer[bpxbuffer.length-1] = signal/bpgain;
		for(int i = 0; i < bpybuffer.length-1; i++)
		{
			bpybuffer[i] = bpybuffer[i+1];
		}
		bpybuffer[bpybuffer.length-1] = 
			bpxbuffer[0] + bpxbuffer[4] - 2*bpxbuffer[2] +
			-0.5479290571* bpybuffer[0] + -0.4486481316* bpybuffer[1] +
			-1.1188831704* bpybuffer[2] + -0.6419793587* bpybuffer[3];
		return bpybuffer[bpybuffer.length-1];
	}
	
	private double windowedIntegral(double signal)
	{
		wIntegral -= wbuffer[0];
		for(int i = 0; i < wbuffer.length-1; i++)
		{
			wbuffer[i] = wbuffer[i+1];
		}
		
		wbuffer[wbuffer.length-1] = signal;
		wIntegral += signal;
		return wIntegral;
	}
	//util
	private double derivative_5stencil(double data)
		{
			  /*y = 1/8 (2x( nT) + x( nT - T) - x( nT - 3T) - 2x( nT -
			4T))*/
			  _y = ((data *2) +  x_derv[3] - x_derv[1] - (x_derv[0]*2))/8;
			  
			  for (int i = 0; i < 3; i++)
					x_derv[i] = x_derv[i + 1];
			  x_derv[3] = data;
			  return _y;
		}

	
	public String status()
	{
		return "";
	}

	@Override
	public String calibrationStatus() {
		// TODO Auto-generated method stub
		return null;

	}
}
