package se.sics.ah3.old;

public interface SignalProcessor {
	public int process(int signal);
	public int process(int[] signal);

	public String status();

	public String calibrationStatus();

}
