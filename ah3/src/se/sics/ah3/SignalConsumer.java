package se.sics.ah3;

public interface SignalConsumer {
	public void consumeSignal(long time, int... value);
	public int[] getInterest();
}
