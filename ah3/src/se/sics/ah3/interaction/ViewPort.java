package se.sics.ah3.interaction;

public class ViewPort {
	private Parameters mParameters;

	public ViewPort() {
		mParameters = new Parameters();
	}
	
	public Parameters getParameters()
	{
		return new Parameters(mParameters);
	}
	
	public void setParameters(Parameters parameters)
	{
		mParameters = new Parameters(parameters);
	}
}
