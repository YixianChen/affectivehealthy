/**
 * 
 */
package se.sics.ah3.dti;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import android.util.Log;

public class DtiMessage{
	
	
	private static final int[] FORMAT_HEADER= new int[]{1};	

	
	int[] n; //includes the crc
	int[] msg; //msg does not include the first header byte
	int crc; //crc is always calculated from raw
	byte[] raw; //includes everything
	int[] header; //header is the first byte indicating the message id
	
	enum Field{
		id,
		version,
		length,
		count,
		serial 
	}
	
	public DtiMessage(int[] header, int n[]) {
		this(header, null, n);
	}
	
	public DtiMessage(int header[], int msg[], int n[]) {
		this.header = header;
		this.msg=msg;
		this.n=n; 		
	}
	
	public int getField(Field f){
		int i = f == Field.id ? 0: f.ordinal()-1;
		return msg[i];
	}
	
	
	public static class IncomingMessage extends DtiMessage{
		public IncomingMessage(int[] header, int[] n) {
			super(header,n);
		}		
	}
	public static class AskTimeMessage extends IncomingMessage{
		private static final int[] FORMAT_ASK_TIME = new int[]{1,2,2,2,1,1,1,1,2};//IMPORTANT: the last 1-byte is just a zero, this also included in CRC
		private static final int[] HEADER_ASK_TIME = new int[]{0xea};

		public AskTimeMessage() {
			super(HEADER_ASK_TIME,FORMAT_ASK_TIME);
		}
		
	}
	static class Factory{
		Class<? extends IncomingMessage> c;
		public Factory(Class<? extends IncomingMessage> c) {
			this.c = c;
		}
		public DtiMessage create() throws IllegalAccessException, InstantiationException{
			return  c.newInstance();			
		}
	}
	static HashMap<Integer,Factory> map = new HashMap<Integer, Factory>();
	static{	
		map.put(0xe9, new Factory(DataMessage.class));
		map.put(0xea, new Factory(AskTimeMessage.class));
	}
	
	public static DtiMessage receive(InputStream is) throws IOException, IllegalAccessException, InstantiationException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int[] tmp =Util.read(is,  new int[]{1},baos);
		DtiMessage ret = map.get(tmp[0]).create();
		ret.read(is);
    	return ret;
	}
	
	public void createRaw() throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		Util.write(header, FORMAT_HEADER, baos);
		Util.write(msg, n, baos);
		byte[] data = baos.toByteArray();
		crc = Util.CRC16_CCITT(data);
		Util.write(new int[]{crc}, new int[]{2}, baos);		
    	raw = baos.toByteArray(); 
	}
	
	private static String hexChar(int b)
	{
		return ""+Character.forDigit(b, 16);
	}

	private static String hex(byte b)
	{
		short s = (short)b;
		return hexChar((s>>4)&0x0f) + hexChar(s&0x0f);
	}

	private static String hex(int b)
	{
		short s = (short)b;
		return hexChar((s>>4)&0x0f) + hexChar(s&0x0f);
	}

	public static void printBytes(String head, final byte[] bytes)
	{
		String s = head;
		for (int i=0;i<bytes.length;i++)
		{
			  s += hex(bytes[i]) + " ";
		}
//		Log.d("DATA", s);
	}

	public static void printBytes(String head, final int[] bytes)
	{
		String s = head;
		for (int i=0;i<bytes.length;i++)
		{
			  s += hex(bytes[i]) + " ";
		}
//		Log.d("DATA", s);
	}

	public void write(OutputStream os) throws IOException{
		createRaw();
    	os.write(raw);
    	os.flush();
    	printBytes("Data >>> ", raw);
	}
	
	public void read(InputStream is) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Util.write(header, FORMAT_HEADER, baos);
    	int[] tmp =Util.read(is, n,baos);
    	raw=baos.toByteArray();
    	int m = tmp.length-1;
    	crc=Util.CRC16_CCITT(raw,0,raw.length-2);
    	msg=new int[m];
    	System.arraycopy(tmp, 0, msg, 0, m);
    	printBytes("Data <<< ", raw);
	}
	
	public boolean check(){
		int tmp = ((0xff & raw[raw.length-1])<<8) | (0xff & raw[raw.length-2]);
		return tmp==crc;
	}
	
	@Override
	public String toString() {
		String s = getClass().getSimpleName()+":";
		s+="\n\tmsg: "+Util.printBytes(msg,n);
		s+="\n\tcrc: "+Util.printBytes(new int[]{crc},new int[]{2});
		int[] tmp = new int[raw.length];
		System.arraycopy(n, 0, tmp, 1, n.length);
		tmp[0]=1;
		s+="\n\traw: "+Util.printBytes(raw,tmp);
		s+="\n\tcrc: "+(check() ? "OK" : "NOT OK");
		return s;
	}
	
	public void print(){
//		Log.v("Bluetooth",toString());
	}
	
	
}