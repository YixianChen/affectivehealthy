package se.sics.ah3.random;

import se.sics.ah3.old.IDataSource;
import se.sics.ah3.old.SignalProcessor;

public class RandomSmoothSignal implements SignalProcessor , IDataSource{
	int oldArousal = 0;
	int oldMovement = 0;
	double factor;
	@Override
	public int process(int signal) {
		// TODO Auto-generated method stub
		double raw = Math.random();
		factor = (signal - 3048) / 4096.0;
		factor = factor * factor;
		if(raw > 0.5+factor)
		{
			signal = (int)(raw*2048 + signal/2);
		}
		else
		{
			signal += factor * 50;
		}
		return signal;
	}

	@Override
	public int process(int[] signal) {
		return process(0);
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

	@Override
	public int processArousal(SignalProcessor processor) {
		oldArousal = process(oldArousal);
		return oldArousal;
	}

	@Override
	public int processMovement(SignalProcessor processor) {
		oldMovement = process(oldMovement);
		return oldMovement;
	}

	@Override
	public int processPulse(SignalProcessor processor) {
		// TODO Auto-generated method stub
		return 0;
	}

}
