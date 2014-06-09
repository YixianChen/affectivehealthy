package se.sics.ah3.dti.signal;

import se.sics.ah3.database.Column;

public class FakeSignalConsumer extends Thread{
	
	int sleep = 500;

	float max,min;
	
	Column column;
	
	float val=0f;
	
	
	public FakeSignalConsumer(int sleep, float min, float max, Column column) {
		super();
		this.sleep = sleep;
		this.max = max;
		this.min = min;
		this.column = column;
	}



	@Override
	public void run() {
		float t = 0;
		while(true){
			
			//float rnd = (float) (0.5-Math.random())*.05f;
		//	System.out.println(val+" + "+rnd+" = "+(val+rnd));
			//val = Math.max(min, Math.min(max, val+rnd));
			t+=0.01+Math.random();
//			val+=1;
//			if (val>500) val = 0;
			val = (max+min)*0.5f + (max-min)*0.5f*(float)Math.sin(t);
		//	System.out.println("VAL: "+val);
			column.insert(System.currentTimeMillis(), val);
			
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
