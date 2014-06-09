package se.sics.ah3.old;

import android.util.Log;

public class GSRProcessor implements SignalProcessor {

	int iPeakValue;
	int iTonicValue;
	int _baseline;
	double _baseSum;

	int downsampleCounter=-1;
	double iBaseline = -1;
	int baselineCounter = 0;

	int offsetPeakHigh = 1;
	int offsetPeakLow = 1;
	int offsetLevelHigh = 8;
	int offsetLevelLow=8;

	double smoothedSignal;
	double highPeak;
	double lowPeak;
	double highLevel;
	double lowLevel;
	double peakValue, levelValue;
	int outWindowHighPeriod;
	int outWindowLowPeriod;
	int errorCounter=0;
	private int DOWNSAMPLE_FACTOR = 100;
	private int NO_SIGNAL = -1;

	int m_n;
	int m_oldM, m_newM, m_oldS, m_newS;

	double[] xv = new double[2];
	double[] yv = new double[2];
	
	double[] tvxv = new double[2];
	double[] tvyv = new double[2];
	
	double[] twxv = new double[2];
	double[] twyv = new double[2];
	
	double[] thxv = new double[2];
	double[] thyv = new double[2];
	
	double[] tlxv = new double[2];
	double[] tlyv = new double[2];
	
	double gain, coef,lastcutoff;
	private double MIN_GSR_VALUE = 20;
	private int factor;

	double normalized;
	double tonicValueRaw;
	
	int windowTonic;
	int diff;
	private double[] lpxv = new double[2];
	private double[] lpyv = new double[2];

	// second phase constructor, anything that may leave must be constructed here
	//MatlabSetUpFileL();
	public GSRProcessor()
	{
		iPeakValue = -1;
		iTonicValue = -1;
	}
	
	@Override
	public int process(int signal) {
		// TODO Auto-generated method stub
		/*if(ah2Process((short) signal) == 0)
		{
			Log.d("GSRProces:", "Arousal:"+iPeakValue +"Tonic: "+ iTonicValue +" Baseline: "+ iBaseline);
		}*/
		ah2Process((short) signal);
		return iPeakValue;
	}

	@Override
	public int process(int[] signal) {
		// TODO Auto-generated method stub
		ah2Process((short) signal[0]);
		return iPeakValue;
	}

	@Override
	public String status() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String calibrationStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	int getResult(int type){

		switch(type){
		case 0: return iPeakValue;
		case 1: return iTonicValue;
		}
		return -1;

	}
	int calculateBaseLine(short in)
	{
		_baseSum = iBaseline*baselineCounter+in;
		baselineCounter++;
		iBaseline = _baseSum/baselineCounter;
		return (int)iBaseline;
	}

	int Process(short data1,short data2,short data3)
	{
		return ah2Process(data1);
	}


	//TODO: output noise
	int ah2Process(short data)
	{
		_baseline = calculateBaseLine(data);
		//return ProcessOld(data);
		smoothedSignal = lowpassOld(data-_baseline);
		
		downsampleCounter++;
		downsampleCounter %= DOWNSAMPLE_FACTOR;
		if(downsampleCounter != 0){
			//MatlabWriteToFileL(0,0,0,0,0,0);
			return downsampleCounter;
		}
		else{
			//MatlabWriteToFileL(1000,1,0,0,0,0);
		}

		if((smoothedSignal+_baseline)<MIN_GSR_VALUE){
			iPeakValue = NO_SIGNAL;
			iTonicValue = NO_SIGNAL;
			errorCounter = 10; //2 seconds
			return downsampleCounter;
		}

		if(errorCounter > 0) 
		{
			errorCounter--;
			return downsampleCounter;
		}

		tonicValueRaw = tonicValue(smoothedSignal,0.1);

		windowTonic = (int) tonicWindow(smoothedSignal);
		highLevel = windowTonic + 9*offsetLevelHigh;
		lowLevel = windowTonic - 1*offsetLevelLow;

		highPeak = tonicHigh(smoothedSignal,0.25)+9*offsetPeakHigh;
		lowPeak = tonicLow(smoothedSignal,0.25)-1*offsetPeakLow;

		//		just in case they cross, because it happened
		//		TODO: check for stability of algorithm and make sure they never cross, thus preventing this check

		if(lowPeak>=highPeak) 
		{
			diff = (int) (lowPeak-highPeak);
			lowPeak += 10+diff;
			highPeak -= 10+diff;
		}

		if(smoothedSignal<lowPeak){
			peakValue =lowPeak;
			offsetPeakLow++;
			//			outWindowLowPeriod = Min(10,outWindowLowPeriod+10);
			//			outWindowHighPeriod = Max(0,outWindowHighPeriod-1);
		}
		else if(smoothedSignal>highPeak){
			peakValue =highPeak;
			offsetPeakHigh++;
			//			outWindowLowPeriod = Max(0,outWindowLowPeriod-1);
			//			outWindowHighPeriod = Min(10,outWindowHighPeriod+10);
		}
		else{
			peakValue =smoothedSignal;
			//			outWindowLowPeriod = Max(0,outWindowLowPeriod-1);
			//			outWindowHighPeriod = Max(0,outWindowHighPeriod-1);
		}

		if(lowLevel>=highLevel) 
		{
			diff = (int) (lowLevel-highLevel);
			lowLevel += 10+diff;
			highLevel -= 10+diff;
		}

		if(tonicValueRaw<lowLevel){
			levelValue =lowLevel;
			offsetLevelLow++;
			//		outWindowLowPeriod = Min(10,outWindowLowPeriod+10);
			//		outWindowHighPeriod = Max(0,outWindowHighPeriod-1);
		}
		else if(tonicValueRaw>highLevel){
			levelValue =highLevel;
			offsetLevelHigh++;			
			//		outWindowLowPeriod = Max(0,outWindowLowPeriod-1);
			//		outWindowHighPeriod = Min(10,outWindowHighPeriod+10);
		}
		else{
			levelValue =tonicValueRaw;
			//		outWindowLowPeriod = Max(0,outWindowLowPeriod-1);
			//		outWindowHighPeriod = Max(0,outWindowHighPeriod-1);
		}

		normalized = (peakValue-lowPeak)/(highPeak-lowPeak); //it is between 0 and 1

		iPeakValue = (int)Math.max(MIN_GSR_VALUE,(normalized*4000));

		normalized= (levelValue-lowLevel)/(highLevel-lowLevel); //it is between 0 and 1

		iTonicValue = (int)Math.max(MIN_GSR_VALUE,(normalized*4000));

		return downsampleCounter;
	}

	int variance(int x)
	{			
		m_n++;

		// See Knuth TAOCP vol 2, 3rd edition, page 232
		if (m_n == 1)
		{
			m_oldM = m_newM = x;
			m_oldS = 0;

			return 1; //should be zero but we want to avoid size 0;
		}
		else
		{
			m_newM = m_oldM + (x - m_oldM)/m_n;
			m_newS = m_oldS + (x - m_oldM)*(x - m_newM);

			// set up for next iteration
			m_oldM = m_newM; 
			m_oldS = m_newS;

			return m_newS/(m_n - 1);
		}
	}

	//works at 10Hz, implemented with chebychev ripple -30Hz
	double tonicValue(double data, double cutoff)
	{	
		if(cutoff==4.0){
			gain=11.26972425;
			coef = 0.8225333686;
		}
		else if(cutoff==0.1){
			gain = 1006.749815;
			coef = 0.9980134091;
		}
		else if(cutoff==0.5){
			gain=200.5584995;
			coef = 0.9900278472;
		}
		else if(cutoff==3.0){
			gain=23.96380154;
			coef = 0.9165407877;
		}

		else if(cutoff==2.0){
			gain=44.50325004;
			coef = 0.9550594620;
		}
		else if(cutoff==1.0){
			gain=98.27622433;
			coef = 0.9796491978;	
		}

		else if(cutoff==0.25){
			gain = 402.6045208;
			coef = 0.9950323459;
		}
		else if(cutoff==0.1){
			gain = 1006.749815;
			coef = 0.998013409;
		}

		tvxv[0] = tvxv[1]; 
		tvxv[1] = ((double)data) / gain;
		tvyv[0] = tvyv[1]; 
		tvyv[1] =   (tvxv[0] + tvxv[1])
		+ (  coef * tvyv[0]);

		lastcutoff=cutoff;
		return (double) tvyv[1];
	}

	//works at 10Hz, implemented with chebychev ripple -30Hz, cutoff 0.001Hz
	double tonicWindow(double data)
	{
		//for 0.001 of cutoff
		gain = 100609.0791;
		coef = 0.9999801211;

		//for 0.0002 of cutoff
		//gain = 503041.4114; 
		//coef = 0.9999960242;

		twxv[0] = twxv[1]; 
		twxv[1] = ((double)data) / gain;
		twyv[0] = twyv[1]; 
		twyv[1] =   (twxv[0] + twxv[1])
		+ (  coef * twyv[0]);

		return (double) twyv[1];
	}


	//works at 10Hz, implemented with chebychev ripple -30Hz
	double tonicLow(double data, double cutoff)
	{
		if(cutoff==4.0){
			gain=11.26972425;
			coef = 0.8225333686;
		}
		else if(cutoff==0.1){
			gain = 1006.749815;
			coef = 0.9980134091;
		}
		else if(cutoff==0.5){
			gain=200.5584995;
			coef = 0.9900278472;
		}
		else if(cutoff==3.0){
			gain=23.96380154;
			coef = 0.9165407877;
		}

		else if(cutoff==2.0){
			gain=44.50325004;
			coef = 0.9550594620;
		}
		else if(cutoff==1.0){
			gain=98.27622433;
			coef = 0.9796491978;	
		}

		else if(cutoff==0.25){
			gain = 402.6045208;
			coef = 0.9950323459;
		}

		tlxv[0] = tlxv[1]; 
		tlxv[1] = ((double)data) / gain;
		tlyv[0] = tlyv[1]; 
		tlyv[1] =   (tlxv[0] + tlxv[1])
		+ (  coef * tlyv[0]);

		lastcutoff=cutoff;
		return (double) tlyv[1];
	}

	//works at 10Hz, implemented with chebychev ripple -30Hz, copy of tonicLow
	double tonicHigh(double data, double cutoff)
	{

		if(cutoff==4.0){
			gain=11.26972425;
			coef = 0.8225333686;
		}
		else if(cutoff==0.1){
			gain = 1006.749815;
			coef = 0.9980134091;
		}
		else if(cutoff==0.5){
			gain=200.5584995;
			coef = 0.9900278472;
		}
		else if(cutoff==3.0){
			gain=23.96380154;
			coef = 0.9165407877;
		}

		else if(cutoff==2.0){
			gain=44.50325004;
			coef = 0.9550594620;
		}
		else if(cutoff==1.0){
			gain=98.27622433;
			coef = 0.9796491978;	
		}
		else if(cutoff==0.25){
			gain = 402.6045208;
			coef = 0.9950323459;
		}

		thxv[0] = thxv[1]; 
		thxv[1] = ((double)data) / gain;
		thyv[0] = thyv[1]; 
		thyv[1] =   (thxv[0] + thxv[1])
		+ (  coef * thyv[0]);
		return (double) thyv[1];
	}

	double lowpass(int data)
	{
		//filtertype 	 = 	 Chebyshev
		//passtype 	= 	Lowpass
		//ripple 	= 	-20
		//order 	= 	1
		//samplerate 	= 	1000
		//corner1 	= 	10
		//corner2 	= 	
		//adzero 	= 	
		//logmin 	= 	

		
		lpxv[0] = lpxv[1]; 
		lpxv[1] = ((double)data) / 317.6101362;
		lpyv[0] = lpyv[1]; 
		lpyv[1] =   (lpxv[0] + lpxv[1])
		+ (  0.9937029717 * lpyv[0]);
		return (int) lpyv[1];
	}

	//here be dragons	

	//chebychev 0.5hz ripple -40db
	public int tonicOld(int data, int factor)
	{
		xv[0] = xv[1]; 
		xv[1] = data / (6.365974170e+04/factor);
		yv[0] = yv[1]; 
		yv[1] =   (xv[0] + xv[1])
		+ (  0.9999685830 * yv[0]);
		return (int) yv[1];
	}

	//???
	public double lowpassOld(int data)
	{
		lpxv[0] = lpxv[1]; 
		lpxv[1] = ((float)data) / 3354.503450;
		lpyv[0] = lpyv[1]; 
		lpyv[1] =   (lpxv[0] + lpxv[1])
		+ (  0.9994037866 * lpyv[0]);
		return  lpyv[1];
	}
}





