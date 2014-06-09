/**
 * 
 */
package se.sics.ah3.dti;

import java.util.Calendar;

import se.sics.ah3.dti.DtiMessage.IncomingMessage;



public class DataMessage extends IncomingMessage{
	private static final int[] FORMAT_DATA_MESSAGE = new int[]{1,2,2,2,3,4,1,1,1,2,2,2,2,2,2,2,2 };
	private static final int[] HEADER_DATA_MESSAGE = new int[]{0xe9};
	private static final int[] isSigned = new int[]{0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0};
	
	public enum Field{
		date,
		time,
		event,
		vccBattery,
		dummy,
		skinTemperature,
		skinConductance,
		accX,
		accY,
		accZ,
		envTemperature,  
		envLight		
	}
	public DataMessage() {
		super(HEADER_DATA_MESSAGE,FORMAT_DATA_MESSAGE);
	}

	private int getIndex(Field f){
		int offset = DtiMessage.Field.values().length-1;
		int i = f.ordinal()+offset;
		return i;
	}
	
	public int getField(Field f){
		int i= getIndex(f);
		
		int size= FORMAT_DATA_MESSAGE[i];
		int val = msg[i];
		boolean signed = ((1<<(size*8-1)) & val) >0;		
		int ret =  (isSigned[i]==1 && signed) ?val-(1<<(size*8)) : val;
		return ret;
	}

	private int get(int v, int n){
		return (v>>(8*n))&0xff;
	}
	public int getHour(){
		int v = getField(Field.time);
		return get(v,0);
	}
	public int getMinutes(){
		int v = getField(Field.time);
		return get(v,1);		
	}
	public int getSeconds(){
		int v = getField(Field.time);
		return get(v,2);		
	}
	public int getHundreds(){
		int v = getField(Field.time);
		return get(v,3);
	}
	
	public int getYear(){
		int v = getField(Field.date);
		return get(v,0);		
	}  
	public int getMonth(){
		int v = getField(Field.date);
		return get(v,1);		
	}
	public int getDay(){
		int v = getField(Field.date);
		return get(v,2);		
	}
	
	public long getTimestamp() {
		Calendar cal = Calendar.getInstance();
		cal.set(2000+getYear(), getMonth()-1, getDay(), getHour(), getMinutes(), getSeconds());
		cal.set(Calendar.MILLISECOND, getHundreds()*10);

		long timestamp = cal.getTimeInMillis();
		
		return timestamp;
	}
}