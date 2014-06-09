package se.sics.ah3.graphics;

import java.io.InputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;

public class CoreUtil {
	/**
	 * return the 2D tangent related to the normal sent in
	 * @param normal
	 * @return
	 */
	public static float[] tangent(float[] normal) {
		float[] t = { normal[1], -normal[0], 0f};
		return t;
	}
	
	public static float linear(float t, float a, float b) {
//		if (Float.isNaN(a) || Float.isNaN(b)) return Float.NaN;
		if (Float.isNaN(a) && Float.isNaN(b)) return -0.9f;
		if (Float.isNaN(a)) return b;
		if (Float.isNaN(b)) return a;
		return (1f-t) * a + t * b;
	}
	
	public static void linear(float t, float[] a, float[] b, float[] out) {
		out[0] = (1 - t) * a[0] + t * b[0];
		out[1] = (1 - t) * a[1] + t * b[1];
		out[2] = (1 - t) * a[2] + t * b[2];
	}
	
	public static float clamp(float t, float min, float max) {
		return Math.max(min, Math.min(t, max));
	}
	
	public static double clamp(double t, double min, double max) {
		return Math.max(min, Math.min(t, max));
	}
	
	public static float fract(float a) {
		float b = Math.abs(a);
		int c = (int)Math.abs(a);
		return Math.signum(a) * (b - c);
	}
	
	public static double fract(double a) {
		double b = Math.abs(a);
		long c = (long)Math.abs(a);
		return Math.signum(a) * (b - c);
	}
	
	public static void normalize(float[] v) {
		float l = 1f / (float)(Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
		v[0] *= l;
		v[1] *= l;
		v[2] *= l;
	}
	
	public static float[] add(float[] a, float[] b) {
		return new float[]{a[0] + b[0],
				a[1] + b[1],
				a[2] + b[2]};
	}
	
	public static float[] sub(float[] a, float[] b) {
		return new float[]{a[0] - b[0],
				a[1] - b[1],
				a[2] - b[2]};
	}
	
	/**
	 * Scale three component vector in place
	 * @param t - scale factor
	 * @param vec - the vector
	 */
	public static void scale(float t, float[] vec) {
		vec[0] *= t;
		vec[1] *= t;
		vec[2] *= t;
	}
	
	/**
	 * Scale three component vector and store as output
	 * @param t - scale factor
	 * @param in - in vector
	 * @param out - out vector
	 */
	public static void scale(float t, float[] in, float[] out) {
		out[0] = in[0] * t;
		out[1] = in[1] * t;
		out[2] = in[2] * t;
	}
	
	/**
	 * Help function for parsing text file stored as a resource
	 * @param context
	 * @param id
	 * @return String of the raw resource
	 */
	public static String parseRawText(Context context, int id) {
		Resources resources = context.getResources();
        InputStream inputStream = resources.openRawResource(id);

        byte[] bytesArray = null;
        try {
        	bytesArray = new byte[inputStream.available()];
	        inputStream.read(bytesArray);
        } catch(Exception e) {
        	// Pokemon exception
        	e.printStackTrace();
        }
        return new String(bytesArray);
	}
	
	public static String formatTimeString(long time) {
		if (time==0) return "";
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		return "" + 
				cal.get(Calendar.YEAR) + "-" + 
				(cal.get(Calendar.MONTH)+1) + "-" + 
				cal.get(Calendar.DAY_OF_MONTH) + " " +
				cal.get(Calendar.HOUR_OF_DAY) + ":" + 
				cal.get(Calendar.MINUTE) + ":" + 
				cal.get(Calendar.SECOND) + "." + 
				cal.get(Calendar.MILLISECOND);
	}
	
	// stefans implementation
	public static String getFormattedDate(String formatString, Date date) {
		Format formatter = new SimpleDateFormat(formatString, Locale.US);
		return formatter.format(date).toLowerCase();
	}	
}
