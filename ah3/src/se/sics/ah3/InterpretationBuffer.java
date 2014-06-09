package se.sics.ah3;

import java.nio.FloatBuffer;

public interface InterpretationBuffer {

	public abstract void init(int length);

	public abstract FloatBuffer getBuffer();
	
	public abstract void interpret(float v);
	
	public abstract void setPosition(int istart, int iend);

	public abstract void clear();
	
	
}