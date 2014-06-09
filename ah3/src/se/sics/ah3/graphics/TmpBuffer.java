package se.sics.ah3.graphics;

import java.nio.FloatBuffer;

import se.sics.ah3.InterpretationBuffer;

public class TmpBuffer implements InterpretationBuffer {
	private FloatBuffer mBuffer;
	
	private float mMin,mMax;

	public TmpBuffer(float min,float max)
	{
		mMin = min; mMax = max;
	}

	@Override
	public void init(int length) {
		mBuffer = FloatBuffer.allocate(length);
	}

	@Override
	public FloatBuffer getBuffer() {
		return mBuffer;
	}

	@Override
	public void interpret(float v) {
		if (Float.isNaN(v))
			mBuffer.put(0);
		else
			mBuffer.put((v-mMin)*2/(mMax-mMin)-1);
	}

	@Override
	public void setPosition(int pos, int lim) {
		mBuffer.clear();
		mBuffer.position(pos);
		mBuffer.limit(lim);
	}

	@Override
	public void clear() {
		mBuffer.clear();
	}

}
