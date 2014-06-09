/**
 * 
 */
package se.sics.ah3.dti;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;



public class SendTimeMessage extends DtiMessage{
	
	private static final int[] FORMAT_SEND_TIME = new int[]{1,2,2,2,3,4,1}; //,2};
	private static final int[] HEADER_SEND_TIME = new int[]{0xae};

	enum Field{
		date,
		time,
		dummy
	}
	public SendTimeMessage(int serial, int count) {		
		super(
				HEADER_SEND_TIME,
				new int[]{1,18,count,serial,0,0,0},
				FORMAT_SEND_TIME
		);
	}
	 
	
	private int getIndex(Field f){
		int offset = DtiMessage.Field.values().length-1;
		int i = f.ordinal()+offset;
		return i;
	}
	private static int build(int[] arr){
		int ret=0;
		int n=arr.length;
		for (int i = 0; i < n; i++) {
			ret |= arr[i]<<((n-i-1)*8);
		}
		return ret;
	}  
	public void write(OutputStream os) throws IOException{
		int itime = getIndex(Field.time);
		int idate = getIndex(Field.date);
		
		Calendar now = Calendar.getInstance();
		int date = build(new int[]{
				now.get(Calendar.DAY_OF_MONTH),
				now.get(Calendar.MONTH)+1,
				now.get(Calendar.YEAR)-2000
		});

		msg[idate]=date;
//		now = Calendar.getInstance();
		int time = build(new int[]{
				(int)(now.getTimeInMillis()%1000)/10,
				now.get(Calendar.SECOND),
				now.get(Calendar.MINUTE),
				now.get(Calendar.HOUR_OF_DAY)
		});
		msg[itime]=time;
		
		super.write(os);
	}
}