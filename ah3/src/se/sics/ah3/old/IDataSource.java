package se.sics.ah3.old;


public interface IDataSource {	
	public int processArousal(SignalProcessor mArousalProcessor);
	public int processMovement(SignalProcessor processor);
	public int processPulse(SignalProcessor processor);

}
