package se.sics.ah3.dti;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {
	public static void write(int[] ints, int[] n, OutputStream os ) throws IOException{
         for (int i = 0; i < ints.length; i++) {
        	 for (int k =0; k < n[i]; k++) {
         		int b=((ints[i]>>k*8) & 0xFF);
         		os.write(b);
         	}
         }
	}
		
	public static int CRC16_CCITT(byte[] bytes){
		return CRC16_CCITT(bytes, 0, bytes.length);
	}
	
	public static int CRC16_CCITT(byte[] bytes, int offset, int len){
		int crc = 0xFFFF; 
		int polynomial = 0x1021;
		
		for (int j = offset; j < len; j++) {
			byte b = bytes[j];
			for (int i = 0; i < 8; i++) {
				boolean bit = ((b   >> (7-i) & 1) == 1);
				boolean c15 = ((crc >> 15    & 1) == 1);
				crc <<= 1;
				if (c15 ^ bit) crc ^= polynomial;
			}
		}
		
		crc &= 0xffff;
		return crc;
	}
	
	public static int[] read(InputStream is, int[] n,ByteArrayOutputStream baos) throws IOException {
    	int[] ret = new int[n.length];

    	for (int j = 0; j < n.length; j++) {    		
    		for (int i = 0; i < n[j]; i++) {
    			
    			int b = is.read();
    			if(b<0)
    				throw new IOException();

    			if(baos!=null)
    				baos.write(b);
    			ret[j] |= b<<i*8;
    		}
    	}
    	return ret;
    }

	public static String printBytes(byte[] bytes, int[]n) {
		String s = "";
		int k=0;
		int next=n[k++];
		for (int i = 0; i < bytes.length; i++) {
			String tmp = Integer.toHexString(0xFF & bytes[i]);
			while(tmp.length()<2)
				tmp="0"+tmp;
			if(i==next){
				s+=" ";
				next=next+n[k++];
			}
			s+=tmp;
			
		}
		return s;
	}
	
	public static String printBytes(int i, int n){
		String s="";
		for (int j = 0; j < n; j++){
			int v = (i >> (j*8));
			String tmp = Integer.toHexString(0xFF & v);
			while(tmp.length()<2)
				tmp="0"+tmp;
			s=tmp+s;
		}
		return s;
	}
	
	public static String printBytes(int[] ints, int[]n){
		String s = "";
		for (int i = 0; i < ints.length; i++) {			
			String tmp = printBytes(ints[i],n[i]);
			while(tmp.length()<2*n[i])
				tmp="0"+tmp;
			s+=tmp+" ";
		}
		return s;
	}
	
    
}
