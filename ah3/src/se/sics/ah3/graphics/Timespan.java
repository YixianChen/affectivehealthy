package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Vector;
import java.util.HashMap;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import android.opengl.GLES20;

/**
 * Labels for hours and minutes
 * 
 * @author mareri
 * 
 */

public class Timespan extends Mesh20 {
	private final static int MAX_NUMBER_OF_ENTRIES = 4000;

	int mModelViewProjectionLoc;
	int mOpacityLoc;
	int mNumberOfEntries;
	int mNumberOfVertsPerPrim;
	int mTexId, mTexSampler;
	int[] mVboTex;
	int[] mVboDays;
//	int mTexDays;
	int mNumberOfDayEntries;
	FloatBuffer mDaysGeomBuffer;
	FloatBuffer mDaysTcBuffer;

	FloatBuffer mTextureCoordBuffer;

	// square buffers
	private FloatBuffer mSquareVertexBuffer;
	private FloatBuffer mSquareTextureBuffer;

	// textures
	int mTexShowing;
	int mTexGranularities;
	int mTexNumbers;
	int mTexDays;
	int mTexTexts;
	int mTexTexts2;
	int mTexTextsTo;

	// chars
	class Char {
		int texId;
		char c;
		float u0, v0, u1, v1;
		float width;

		public Char(int tid, char c, float width, float u0, float v0, float u1,
				float v1) {
			texId = tid;
			this.c = c;
			this.width = width;
			this.u0 = u0;
			this.v0 = v0;
			this.u1 = u1;
			this.v1 = v1;
		}
	}

	HashMap<String, Char> mCharMappings = new HashMap<String, Char>();

	Vector<UV> uvs;
	Vector<UV> uvsDay;
	float[] cursorPosition;
	float[] normal = { 0f, 1f, 0f };
	float[] tangent = { 1f, 0f, 0f };

	float textWidth = 0.07f; // 0.15f/3.0f;
	float textHeight = 0.07f; // 0.1f/3.0f;

	public Timespan() {
		super("Timespan");

		// square bufers
		ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mSquareVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer tcbb = ByteBuffer.allocateDirect(4 * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mSquareTextureBuffer = tcbb.asFloatBuffer();

		// other buffers

		vbb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();

		tcbb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mTextureCoordBuffer = tcbb.asFloatBuffer();

		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);

		ByteBuffer dvbb = ByteBuffer
				.allocateDirect(MAX_NUMBER_OF_ENTRIES * 3 * 4);
		dvbb.order(ByteOrder.nativeOrder());
		mDaysGeomBuffer = dvbb.asFloatBuffer();

		ByteBuffer dtcbb = ByteBuffer
				.allocateDirect(MAX_NUMBER_OF_ENTRIES * 2 * 4);
		dtcbb.order(ByteOrder.nativeOrder());
		mDaysTcBuffer = dtcbb.asFloatBuffer();

		mDaysGeomBuffer.position(0);
		mDaysTcBuffer.position(0);

		// texture atlas coordinates
		uvs = new Vector<Timespan.UV>();
		uvs.add(new UV(0f, 0.25f, 0.0f, 0.25f)); // 0
		uvs.add(new UV(0.25f, 0.5f, 0.0f, 0.25f)); // 1
		uvs.add(new UV(0.5f, 0.75f, 0.0f, 0.25f)); // 2
		uvs.add(new UV(0.75f, 1.0f, 0.0f, 0.25f)); // 3
		uvs.add(new UV(0f, 0.25f, 0.25f, 0.5f)); // 4
		uvs.add(new UV(0.25f, 0.5f, 0.25f, 0.5f)); // 5
		uvs.add(new UV(0.5f, 0.75f, 0.25f, 0.5f)); // 6
		uvs.add(new UV(0.75f, 1.0f, 0.25f, 0.5f)); // 7
		uvs.add(new UV(0f, 0.25f, 0.5f, 0.75f)); // 8
		uvs.add(new UV(0.25f, 0.5f, 0.5f, 0.75f)); // 9
		uvs.add(new UV(0.5f, 0.75f, 0.5f, 0.75f)); // .
		uvs.add(new UV(0.75f, 1.0f, 0.5f, 0.75f)); // -
		uvs.add(new UV(0f, 0.25f, 0.75f, 1.0f)); // blank

		uvsDay = new Vector<Timespan.UV>();
		uvsDay.add(new UV(-0.05f, 0.5f, 0.0f, 0.135f)); // Mon
		uvsDay.add(new UV(-0.05f, 0.5f, 0.12f, 0.265f)); // Tue
		uvsDay.add(new UV(-0.05f, 0.55f, 0.25f, 0.39f)); // Wed
		uvsDay.add(new UV(-0.05f, 0.5f, 0.38f, 0.525f)); // Thu
		uvsDay.add(new UV(-0.05f, 0.5f, 0.51f, 0.65f)); // Fri
		uvsDay.add(new UV(-0.05f, 0.5f, 0.64f, 0.78f)); // Sat
		uvsDay.add(new UV(-0.05f, 0.5f, 0.77f, 0.905f)); // Sun

		cursorPosition = new float[3];
		setCursorPosition(-0.9f, 0.9f);

		CoreUtil.scale(textHeight, normal);
		CoreUtil.scale(textWidth, tangent);
	}

	private void setCursorPosition(float x, float y) {
		cursorPosition[0] = x;
		cursorPosition[1] = y;
		cursorPosition[2] = 0f;
	}

	private void submitPrimitive(float[] point, float[] normal, float[] tangent,
			int texture) {
		// v0 -n -t
		mVertexBuffer.put(point[0] + (-normal[0] - tangent[0]));
		mVertexBuffer.put(point[1] + (-normal[1] - tangent[1]));
		mVertexBuffer.put(0f);
		// v1 +n -t
		mVertexBuffer.put(point[0] + (normal[0] - tangent[0]));
		mVertexBuffer.put(point[1] + (normal[1] - tangent[1]));
		mVertexBuffer.put(0f);
		// v2 -n +t
		mVertexBuffer.put(point[0] + (-normal[0] + tangent[0]));
		mVertexBuffer.put(point[1] + (-normal[1] + tangent[1]));
		mVertexBuffer.put(0f);
		// v2 -n +t
		mVertexBuffer.put(point[0] + (-normal[0] + tangent[0]));
		mVertexBuffer.put(point[1] + (-normal[1] + tangent[1]));
		mVertexBuffer.put(0f);
		// v1 +n -t
		mVertexBuffer.put(point[0] + (normal[0] - tangent[0]));
		mVertexBuffer.put(point[1] + (normal[1] - tangent[1]));
		mVertexBuffer.put(0f);
		// v3 +n +t
		mVertexBuffer.put(point[0] + (normal[0] + tangent[0]));
		mVertexBuffer.put(point[1] + (normal[1] + tangent[1]));
		mVertexBuffer.put(0f);

		UV uv = uvs.get(texture);

		float u0 = uv.u0;
		float u1 = uv.u1;
		float v0 = uv.v0;
		float v1 = uv.v1;

		// t2
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v1);
		// t0
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v0);
		// t3
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v1);
		// t3
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v1);
		// t0
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v0);
		// t1
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v0);

		mNumberOfEntries++;
	}

	private void submitPrimitiveDay(float[] point, float[] normal, float[] tangent,
			int texture, float width) {
		// v0 -n -t
		mDaysGeomBuffer.put(point[0] + (-normal[0] - tangent[0] * width));
		mDaysGeomBuffer.put(point[1] + (-normal[1] - tangent[1] * width));
		mDaysGeomBuffer.put(0f);
		// v1 +n -t
		mDaysGeomBuffer.put(point[0] + (normal[0] - tangent[0] * width));
		mDaysGeomBuffer.put(point[1] + (normal[1] - tangent[1] * width));
		mDaysGeomBuffer.put(0f);
		// v2 -n +t
		mDaysGeomBuffer.put(point[0] + (-normal[0] + tangent[0] * width));
		mDaysGeomBuffer.put(point[1] + (-normal[1] + tangent[1] * width));
		mDaysGeomBuffer.put(0f);
		// v2 -n +t
		mDaysGeomBuffer.put(point[0] + (-normal[0] + tangent[0] * width));
		mDaysGeomBuffer.put(point[1] + (-normal[1] + tangent[1] * width));
		mDaysGeomBuffer.put(0f);
		// v1 +n -t
		mDaysGeomBuffer.put(point[0] + (normal[0] - tangent[0] * width));
		mDaysGeomBuffer.put(point[1] + (normal[1] - tangent[1] * width));
		mDaysGeomBuffer.put(0f);
		// v3 +n +t
		mDaysGeomBuffer.put(point[0] + (normal[0] + tangent[0] * width));
		mDaysGeomBuffer.put(point[1] + (normal[1] + tangent[1] * width));
		mDaysGeomBuffer.put(0f);

		UV uv = uvsDay.get(texture);

		float u0 = uv.u0;
		float u1 = uv.u1;
		float v0 = uv.v0;
		float v1 = uv.v1;

		// t2
		mDaysTcBuffer.put(u0);
		mDaysTcBuffer.put(v1);
		// t0
		mDaysTcBuffer.put(u0);
		mDaysTcBuffer.put(v0);
		// t3
		mDaysTcBuffer.put(u1);
		mDaysTcBuffer.put(v1);
		// t3
		mDaysTcBuffer.put(u1);
		mDaysTcBuffer.put(v1);
		// t0
		mDaysTcBuffer.put(u0);
		mDaysTcBuffer.put(v0);
		// t1
		mDaysTcBuffer.put(u1);
		mDaysTcBuffer.put(v0);

		mNumberOfDayEntries++;
	}

	private int convertToUVIndex(char c) {
		switch (c) {
		case 45:
			return 11;
		case 46:
			return 10;
		case 48:
		case 49:
		case 50:
		case 51:
		case 52:
		case 53:
		case 54:
		case 55:
		case 56:
		case 57:
			return (int) (c - 48);
		default:
			return 12;
		}
	}

	private void addEntry(String number, float offset) {
		float thickness = SpiralFormula.getThickness(offset);

		float[] point = { 0f, 0f, 0f };
		float[] normal = { 1f, 0f, 0f };
		float[] tangent = { 0f, 1f, 0f };

		float widthFactor = 0.25f;

		SpiralFormula.parametricFormulation(offset, point);
		normal = SpiralFormula.getNormal(offset);
		tangent = SpiralFormula.getTangent(offset);

		CoreUtil.normalize(normal);
		CoreUtil.normalize(tangent);

		CoreUtil.scale(widthFactor * thickness, normal);
		CoreUtil.scale(widthFactor * thickness, tangent);

		// reverse the string for readability when upside down
		String output = number;
		if (tangent[1] < 0f) {
			output = new StringBuffer(number).reverse().toString();
		}

		float sizeFactor = 0.25f / widthFactor;
		float pos = sizeFactor * (float) (-output.length() / 2);
		if (output.length() % 2 == 0) {
			pos += sizeFactor * 0.5f;
		}
		float[] shiftedPosition = new float[3];
		for (int i = 0; i < output.length(); i++) {
			CoreUtil.scale(pos, normal, shiftedPosition);
			submitPrimitive(CoreUtil.add(point, shiftedPosition), normal,
					tangent, convertToUVIndex(output.charAt(i)));
			pos += sizeFactor;
		}
	}

	private String zeroPadTime(int v) {
		return v < 10 ? "0" + v : "" + v;
	}

	private String getSecondsNthsString(long time, int n) {
		Date currentDate = new Date(time);
		int second = currentDate.getSeconds();
		return zeroPadTime(second);
	}

	private String getMinutesNthsString(long time, int n) {
		Date currentDate = new Date(time);
		int minute = currentDate.getMinutes();
		return zeroPadTime(minute);
	}

	private String getHourString(long time) {
		Date currentDate = new Date(time);
		int hour = currentDate.getHours();
		return zeroPadTime(hour);
	}

	void setPosition2() {
		// reset the buffer
		mNumberOfEntries = 0;
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);

		Parameters parameters = AHState.getInstance().mViewPort.getParameters();

		Date currentDate = new Date(parameters.end);
		Date endDate = new Date(parameters.start);

		long timeLine = currentDate.getTime() - endDate.getTime();

		double hours = timeLine / (double) Parameters.HOUR;

		double minutes = timeLine / (double) Parameters.MINUTES;

		// seconds
		// if (timeLine<Parameters.MINUTES*20 && timeLine>Parameters.MINUTES/2)
		{
			/*
			 * int n = 10; if (timeLine<Parameters.MINUTES) n = 1; else if
			 * (timeLine<Parameters.MINUTES*2) n = 2; else if
			 * (timeLine<Parameters.MINUTES*4) n = 5; else if
			 * (timeLine<Parameters.MINUTES*20) n = 10;
			 */
			int seconds = (int) (timeLine / 1000);
			int n = seconds / 36;
			// n = 60, 45, 30, 15, ...
			if (n > 60)
				n = 60;
			else if (n > 30)
				n = 30;
			else if (n > 15)
				n = 15;
			else if (n > 10)
				n = 10;
			else if (n > 5)
				n = 5;
			else if (n > 2)
				n = 2;
			else if (n < 1)
				n = 1;
			if (n > 0 && n < 60) {
				long offset = (currentDate.getSeconds() % n) * 1000
						+ (currentDate.getTime() % 1000);

				long currentMinute = currentDate.getTime() - offset;

				while (currentMinute > endDate.getTime()) {
					if ((currentMinute / 1000) % 60 != 0) // skip even minutes
					{
						float t = SpiralFormula.getT(currentMinute, parameters);
						// String timeString =
						// getMinutesNthsString(currentMinute, 1) + "." +
						// getSecondsNthsString(currentMinute,n);
						String timeString = "   "
								+ getSecondsNthsString(currentMinute, n);
						addEntry(timeString, t);
					}
					currentMinute -= 1000 * n;
				}
			}
		}

		// minutes
		// if (timeLine<Parameters.HOUR*6 && timeLine>Parameters.MINUTES*20)
		{
			/*
			 * int n = 10; if (timeLine<Parameters.HOUR) n = 1; else if
			 * (timeLine<Parameters.HOUR*2) n = 2; else if
			 * (timeLine<Parameters.HOUR*4) n = 5; else if
			 * (timeLine<Parameters.HOUR*6) n = 10;
			 */
			int n = (int) minutes / 36;
			if (n > 60)
				n = 60;
			else if (n > 30)
				n = 30;
			else if (n > 15)
				n = 15;
			else if (n > 10)
				n = 10;
			else if (n > 5)
				n = 5;
			else if (n > 2)
				n = 2;
			else if (n < 1)
				n = 1;
			if (n > 0 && n < 60) {
				long offset = (currentDate.getMinutes() % n) * 60 * 1000
						+ currentDate.getSeconds() * 1000
						+ (currentDate.getTime() % 1000);

				long currentMinute = currentDate.getTime() - offset;

				while (currentMinute > endDate.getTime()) {
					if ((currentMinute / Parameters.MINUTES) % 60 != 0) // skip
																		// even
																		// hours
					{
						float t = SpiralFormula.getT(currentMinute, parameters);
						String timeString = getHourString(currentMinute) + "."
								+ getMinutesNthsString(currentMinute, n);
						addEntry(timeString, t);
					}
					currentMinute -= Parameters.MINUTES * n;
				}
			}
		}

		// hours
		if (hours >= 6.0) {
			long offset = currentDate.getMinutes() * 60 * 1000
					+ currentDate.getSeconds() * 1000
					+ (currentDate.getTime() % 1000);

			long currentHour = currentDate.getTime() - offset;

			while (currentHour > endDate.getTime()) {
				float t = SpiralFormula.getT(currentHour, parameters);
				int timeStamp = new Date(currentHour).getHours();
				String timeString = new String();
				if (timeStamp < 10) {
					timeString = "0";
				}
				timeString += timeStamp + ".00";
				addEntry(timeString, t);
				currentHour -= Parameters.HOUR;
			}
		} /*
		 * else if(minutes >= 1.0 ) { long offset = currentDate.getSeconds() *
		 * 1000 + (currentDate.getTime()%1000);
		 * 
		 * long currentMinute = currentDate.getTime() - offset;
		 * 
		 * while(currentMinute > endDate.getTime()) { float t =
		 * SpiralFormula.getT(currentMinute, parameters); int timeStamp = new
		 * Date(currentMinute).getMinutes(); String timeString = new String();
		 * if(timeStamp < 10) { timeString = "0"; } timeString += new
		 * Integer(timeStamp).toString(); addEntry(timeString, t); currentMinute
		 * -= Parameters.MINUTES; } }
		 */

		batchGraphics();
	}

	char dayAsCharCode(int day) {
		day = (day + 6) % 7;
		return (char) (97 + day);
	}

	float offset(char c) {
		float texWidthOffset = textWidth / 0.1f;
		switch (c) {
		case 45:
			return 0.1f * texWidthOffset;
		case 46:
			return 0.075f * texWidthOffset;
		case 48:
		case 49:
		case 50:
		case 51:
		case 52:
		case 53:
		case 54:
		case 55:
		case 56:
		case 57:
			return 0.1f * texWidthOffset;
		case 97:
			return 0.3f * texWidthOffset;
		case 98:
			return 0.25f * texWidthOffset;
		case 99:
			return 0.3f * texWidthOffset;
		case 100:
			return 0.25f * texWidthOffset;
		case 101:
			return 0.15f * texWidthOffset;
		case 102:
			return 0.25f * texWidthOffset;
		case 103:
			return 0.3f * texWidthOffset;
		default:
			return 0.1f * texWidthOffset;
		}
	}

	void setPosition() {
		// reset the buffer
		mNumberOfEntries = 0;
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);

		// days
		mNumberOfDayEntries = 0;
		mDaysGeomBuffer.position(0);
		mDaysTcBuffer.position(0);

		Parameters parameters = AHState.getInstance().mViewPort.getParameters();

		Date currentDate = new Date(parameters.end);
		Date endDate = new Date(parameters.start);

		long timeLine = currentDate.getTime() - endDate.getTime();

		double days = timeLine / (double) Parameters.DAY;
		double hours = timeLine / (double) Parameters.HOUR;
		double minutes = timeLine / (double) Parameters.MINUTES;

		// setCursorPosition(-0.9f, 0.9f);

		String output = new String();

		/*
		 * if(days > 1) { // days
		 * 
		 * } else
		 */
		/*
		 * if(hours > 1) { // hours output +=
		 * dayAsCharCode(currentDate.getDay()) +
		 * zeroPadTime(currentDate.getHours()) + ".00-" +
		 * dayAsCharCode(endDate.getDay())+ zeroPadTime(endDate.getHours()) +
		 * ".00"; } else if(minutes > 1) { // minutes output +=
		 * zeroPadTime(currentDate.getHours()) + "." +
		 * zeroPadTime(currentDate.getMinutes()) + "-" +
		 * zeroPadTime(endDate.getHours()) + "." +
		 * zeroPadTime(endDate.getMinutes()); } else { // seconds output +=
		 * zeroPadTime(currentDate.getHours()) + "." +
		 * zeroPadTime(currentDate.getMinutes()) + "." +
		 * zeroPadTime(currentDate.getSeconds()) + "-" +
		 * zeroPadTime(endDate.getHours()) + "." +
		 * zeroPadTime(endDate.getMinutes()) + "." +
		 * zeroPadTime(endDate.getSeconds()); }
		 * 
		 * textOut(output,-0.9f,0.9f);
		 */

		if (timeLine < 1000 * 60 * 60 * 2) { // less than 2 hours 1h
			//
			String startString = dayAsCharCode(currentDate.getDay()) + " "
					+ zeroPadTime(currentDate.getDate()) + "/"
					+ zeroPadTime(currentDate.getMonth() + 1) + " "
					+ zeroPadTime(currentDate.getHours()) + "."
					+ zeroPadTime(currentDate.getMinutes());
			String endString = dayAsCharCode(endDate.getDay()) + " "
					+ zeroPadTime(endDate.getDate()) + "/"
					+ zeroPadTime(endDate.getMonth() + 1) + " "
					+ zeroPadTime(endDate.getHours()) + "."
					+ zeroPadTime(endDate.getMinutes());

			// textOut(startString, -0.2f, -0.75f, 0.5f);
			// textOut(endString, -0.2f, -0.80f, 0.5f);
		} else {
			String startString = dayAsCharCode(currentDate.getDay()) + " "
					+ zeroPadTime(currentDate.getDate()) + "/"
					+ zeroPadTime(currentDate.getMonth() + 1) + " "
					+ zeroPadTime(currentDate.getHours()) + "."
					+ zeroPadTime(currentDate.getMinutes()) + "."
					+ zeroPadTime(currentDate.getSeconds());
			String endString = dayAsCharCode(endDate.getDay()) + " "
					+ zeroPadTime(endDate.getDate()) + "/"
					+ zeroPadTime(endDate.getMonth() + 1) + " "
					+ zeroPadTime(endDate.getHours()) + "."
					+ zeroPadTime(endDate.getMinutes()) + "."
					+ zeroPadTime(endDate.getSeconds());

			// textOut(startString, -0.9f,-0.7f, 1.0f);
			// textOut(endString, -0.9f,-0.8f, 0.75f);
		}

		batchGraphics();
	}

	private void textOut(String output, float x, float y, float size) {
		setCursorPosition(x, y);

		normal[0] = 0.0f;
		normal[1] = 1.0f;
		normal[2] = 0.0f;
		tangent[0] = 1f;
		tangent[1] = 0f;
		tangent[2] = 0f;
		CoreUtil.scale(textHeight * size, normal);
		CoreUtil.scale(textWidth * size, tangent);

		for (int i = 0; i < output.length(); i++) {
			float stepSize = offset(output.charAt(i)) * size;
			if (output.charAt(i) == '.') {
				setCursorPosition(cursorPosition[0] - 0.025f, cursorPosition[1]);
			}
			if (output.charAt(i) >= 97) {
				setCursorPosition(cursorPosition[0] + 0.1f, cursorPosition[1]);
				submitPrimitiveDay(cursorPosition, normal, tangent,
						output.charAt(i) - 97, 2f);
			} else {
				submitPrimitive(cursorPosition, normal, tangent,
						convertToUVIndex(output.charAt(i)));
			}
			setCursorPosition(cursorPosition[0] + stepSize, cursorPosition[1]);
		}
	}

	void batchGraphics() {
		// reset the buffer and commit
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mVertexBuffer.capacity(), mVertexBuffer,
				GLES20.GL_DYNAMIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer,
				GLES20.GL_DYNAMIC_DRAW);

		mDaysGeomBuffer.position(0);
		mDaysTcBuffer.position(0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboDays[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mDaysGeomBuffer.capacity(), mDaysGeomBuffer,
				GLES20.GL_DYNAMIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboDays[1]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mDaysTcBuffer.capacity(), mDaysTcBuffer,
				GLES20.GL_DYNAMIC_DRAW);

	}

	@Override
	public void draw(Camera camera, boolean pick) {
		super.draw(Core.SCREEN_SPACE_CAMERA, pick);

//		setPosition();

		if (!pick) {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
					GLES20.GL_ONE_MINUS_SRC_ALPHA);
			// GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE);
			// GLES20.glBlendEquation(GLES20.GL_FUNC_SUBTRACT);
			GLES20.glEnable(GLES20.GL_BLEND);

			GLES20.glUniform1i(mTexSampler, 0);
			Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTexId);

			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false,
					Core.SCREEN_SPACE_CAMERA.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniform1f(mOpacityLoc, 1);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
		}

		if (!pick || mPickable) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
			GLES20.glEnableVertexAttribArray(Core.VERTEX);
			Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false,
					0, 0);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mNumberOfVertsPerPrim
					* mNumberOfEntries);

/*			if (mNumberOfDayEntries > 0) {
				GLES20.glUniform1i(mTexSampler, 0);
				Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D,
						mTexDays);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboDays[1]);
				GLES20.glEnableVertexAttribArray(1);
				Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboDays[0]);
				GLES20.glEnableVertexAttribArray(Core.VERTEX);
				Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT,
						false, 0, 0);

				GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
						mNumberOfVertsPerPrim * mNumberOfDayEntries);
			}*/

			// draw screen
			renderStuff();

			GLES20.glDisable(GLES20.GL_BLEND);
		}

	}

	private void renderSquare(int texture, float x, float y, float w, float h, float u1, float v1, float u2, float v2, float opacity) {
		FloatBuffer vertexcoords = mSquareVertexBuffer;
		FloatBuffer texturecoords = mSquareTextureBuffer;

		vertexcoords.put(x);
		vertexcoords.put(y);
		vertexcoords.put(0);
		texturecoords.put(u1);
		texturecoords.put(v1);

		vertexcoords.put(x + w);
		vertexcoords.put(y);
		vertexcoords.put(0);
		texturecoords.put(u2);
		texturecoords.put(v1);

		vertexcoords.put(x + w);
		vertexcoords.put(y + h);
		vertexcoords.put(0);
		texturecoords.put(u2);
		texturecoords.put(v2);

		vertexcoords.put(x);
		vertexcoords.put(y + h);
		vertexcoords.put(0);
		texturecoords.put(u1);
		texturecoords.put(v2);

		vertexcoords.position(0);
		texturecoords.position(0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * vertexcoords.capacity(), vertexcoords, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glEnableVertexAttribArray(0); // vertex
		Core.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * texturecoords.capacity(), texturecoords, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glEnableVertexAttribArray(1);	// texture coords
		Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

		Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, texture);

		GLES20.glUniform1f(mOpacityLoc, opacity);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
	}

	private float getNumCharWidth(String ch) {
		return mCharMappings.containsKey(ch) ? (mCharMappings.get(ch).width / 20f) * textWidth : (textWidth * 0.25f);
	}

	private void charOut(String ch, float x, float y, float heightFactor, float opacity) {
		Char c = mCharMappings.get(ch);
		if (c == null) return;
		float width = heightFactor * (c.width / 20f) * textWidth;
		renderSquare(c.texId, x, y, width, textHeight * heightFactor * 1.78f, c.u0, c.v0, c.u1, c.v1, opacity);
	}

	// used through %n where n is num
	private String getDayString(int m) {
		String[] ms = new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
		return ms[m % ms.length];
	}
	private String getMonthString(char m) {
		switch (m) {
		case '1': return "Jan";
		case '2': return "Feb";
		case '3': return "Mar";
		case '4': return "Apr";
		case '5': return "May";
		case '6': return "Jun";
		case '7': return "Jul";
		case '8': return "Aug";
		case '9': return "Sep";
		case 'A':
		case 'a': return "Oct";
		case 'B':
		case 'b': return "Nov";
		case 'C':
		case 'c': return "Dec";
		}
		return "Jan";
	}
	/*	private String getMonthString(int m) {
	String[] ms = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	return ms[m % ms.length];
	}*/
	private char getMonthChar(int m) {
		char[] ms = new char[] { '1','2','3','4','5','6','7','8','9','a','b','c' };
		return ms[m % ms.length];
	}
	private String getSpecialText(char c) {
		switch (c) {
		case '&': return "and";
		case 'Y': return "year";
		case 'T': return "to";
		}
		return " ";
	}

	private void print(String s, float x, float y, float heightFactor, float opacity) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			String ch = "" + c;
			float kLeft = 0f;
			float kRight = 0f;
			if (c == '%') { // days
				i++;
				c = s.charAt(i);
				ch = getDayString(c - 48);
			} else if (c == '#') { // month
				i++;
				c = s.charAt(i);
				ch = getMonthString(c);
			} else if (c=='$') {
				i++;
				c = s.charAt(i);
				ch = getSpecialText(c);
			} else { // kern certain chars
				if (c=='/') { kLeft = 0.2f; kRight = 0.2f; }
				else if (c=='.') { kLeft = 0f; kRight = 0.05f; }
			}
			float width = getNumCharWidth(ch) * heightFactor;
			charOut(ch, x-kLeft*textWidth*heightFactor, Core.SCREEN_SPACE_CAMERA.getHeightScaledCoordinate(y), heightFactor, opacity);
			x += width-kLeft*textWidth*heightFactor-kRight*textWidth*heightFactor;
		}
	}

	private void renderStuff() {
		// create vertex buffer
		// renderSquare(mTexId, -0.9f, 0.0f, 1.0f, 1.0f, 0, 1, 1, 0);

		Parameters parameters = AHState.getInstance().mViewPort.getParameters();
		long timespan = parameters.end - parameters.start;
		Date startDate = new Date(parameters.start);
		Date endDate = new Date(parameters.end);

//		GLES20.glUniform1f(mOpacityLoc, 0.5f);
		float textOpacity = 0.8f;

		float ystart = -0.72f;
		float lineHeight = textHeight; //*1.2f;

		float gran_vh = 100 / 600f;
		if (timespan <= 1000 * 61) { // 1 minute
			renderSquare(mTexGranularities, -1.0f, -1 + 0.11f, 2.0f, 0.25f, 0, 1, 1, gran_vh * 5,1.0f);

/*			String s1 = "" 
					+ zeroPadTime(startDate.getHours()) + ":"
					+ zeroPadTime(startDate.getMinutes()) + "."
					+ zeroPadTime(startDate.getSeconds())
					+ " $& "
					+ zeroPadTime(endDate.getHours()) + ":"
					+ zeroPadTime(endDate.getMinutes()) + "."
					+ zeroPadTime(endDate.getSeconds());*/
/*			String s2 = ""
					+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1) + " "
					+ " $Y " + (startDate.getYear() + 1900);*/
			String s1 = ""
					+ "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + " "
					+ "#" + getMonthChar(startDate.getMonth()) + " "
					+ (startDate.getYear() + 1900);
			String s2 = ""
					+ zeroPadTime(startDate.getHours()) + ":"
					+ zeroPadTime(startDate.getMinutes()) + "."
					+ zeroPadTime(startDate.getSeconds());

			print(s1, -0.2f, ystart - lineHeight, 1.0f, textOpacity);
			print(s2, -0.2f, ystart - lineHeight*2, 1.0f, textOpacity);
		} else if (timespan <= 1000 * 60 * 11) { // 10 minutes
			renderSquare(mTexGranularities, -1.0f, -1 + 0.11f, 2.0f, 0.25f, 0, gran_vh * 5, 1, gran_vh * 4, 1.0f);

/*			String s1 = ""
					+ zeroPadTime(startDate.getHours()) + "." + zeroPadTime(startDate.getMinutes())
					+ " $& "
					+ zeroPadTime(endDate.getHours()) + "." + zeroPadTime(endDate.getMinutes());
			String s2 = "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1)
					+ " $Y " + (startDate.getYear() + 1900);*/
			
			String s1 = ""
					+ "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + " "
					+ "#" + getMonthChar(startDate.getMonth()) + " "
					+ (startDate.getYear() + 1900);
			String s2 = ""
					+ zeroPadTime(startDate.getHours()) + ":"
					+ zeroPadTime(startDate.getMinutes())
					+ " $T "
					+ zeroPadTime(endDate.getHours()) + ":"
					+ zeroPadTime(endDate.getMinutes());

			print(s1, -0.2f, ystart - lineHeight, 1.0f, textOpacity);
			print(s2, -0.2f, ystart - lineHeight*2, 1.0f, textOpacity);
		} else if (timespan <= 1000 * 60 * 61) { // 1h
			renderSquare(mTexGranularities, -1.0f, -1 + 0.11f, 2.0f, 0.25f, 0, gran_vh * 4, 1, gran_vh * 3, 1.0f);

/*			if (true && startDate.getDay()==endDate.getDay()) {
				String s1 = 
						zeroPadTime(startDate.getHours()) + "." + zeroPadTime(startDate.getMinutes())
						+ " $& "
						+ zeroPadTime(endDate.getHours()) + "." + zeroPadTime(endDate.getMinutes());
				String s2 = "%" + ((startDate.getDay() + 6) % 7) + " "
						+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1) + " "
						+ "$Y " + (startDate.getYear() + 1900);
				
				print(s1, -0.2f, -0.72f - lineHeight, 1.0f, textOpacity);
				print(s2, -0.2f, -0.72f - lineHeight*2, 1.0f, textOpacity);
			} else {
				String s1 = "%" + ((startDate.getDay() + 6) % 7) + " "
						+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1) + " "
						+ zeroPadTime(startDate.getHours()) + "." + zeroPadTime(startDate.getMinutes())
						+ " $& ";
				String s2 = "%" + ((endDate.getDay() + 6) % 7) + " "
						+ (endDate.getDate()) + "/" + (endDate.getMonth() + 1) + " "
						+ zeroPadTime(endDate.getHours()) + "." + zeroPadTime(endDate.getMinutes());
				String s3 = "$Y " + (startDate.getYear() + 1900);
				
				print(s1, -0.2f, -0.70f - lineHeight, 1.0f, textOpacity);
				print(s2, -0.2f, -0.70f - lineHeight*2, 1.0f, textOpacity);
				print(s3, -0.2f, -0.70f - lineHeight*3, 1.0f, textOpacity);
			}*/

			String s1 = ""
					+ "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + " "
					+ "#" + getMonthChar(startDate.getMonth()) + " "
					+ (startDate.getYear() + 1900);
			String s2 = ""
					+ zeroPadTime(startDate.getHours()) + ":"
					+ zeroPadTime(startDate.getMinutes())
					+ " $T "
					+ zeroPadTime(endDate.getHours()) + ":"
					+ zeroPadTime(endDate.getMinutes());

			print(s1, -0.2f, ystart - lineHeight, 1.0f, textOpacity);
			print(s2, -0.2f, ystart - lineHeight*2, 1.0f, textOpacity);
		} else if (timespan <= 1000 * 60 * 60 * 7) { // 6h
			renderSquare(mTexGranularities, -1.0f, -1 + 0.11f, 2.0f, 0.25f, 0, gran_vh * 3, 1, gran_vh * 2, 1.0f);

/*			String s1 = "%" + ((startDate.getDay() + 6) % 7) + " "
					+ zeroPadTime(startDate.getHours()) + "." + zeroPadTime(startDate.getMinutes())
					+ " $& "
					+ zeroPadTime(endDate.getHours()) + "." + zeroPadTime(endDate.getMinutes());
			String s2 = ""
					+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1)
					+ " $Y " + (startDate.getYear() + 1900);*/
			
			String s1 = ""
					+ "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + " "
					+ "#" + getMonthChar(startDate.getMonth()) + " "
					+ (startDate.getYear() + 1900);
			String s2 = ""
					+ zeroPadTime(startDate.getHours()) + ":"
					+ zeroPadTime(startDate.getMinutes())
					+ " $T "
					+ zeroPadTime(endDate.getHours()) + ":"
					+ zeroPadTime(endDate.getMinutes());

			print(s1, -0.2f, ystart - lineHeight, 1.0f, textOpacity);
			print(s2, -0.2f, ystart - lineHeight*2, 1.0f, textOpacity);
		} else if (timespan <= 1000 * 60 * 60 * 25) { // 24h
			renderSquare(mTexGranularities, -1.0f, -1 + 0.11f, 2.0f, 0.25f, 0, gran_vh * 2, 1, gran_vh * 1, 1.0f);
/*			String s1 = ""
					+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1)
					+ " $& "
					+ (endDate.getDate()) + "/" + (endDate.getMonth() + 1);
			String s2 = "$Y " + (startDate.getYear() + 1900);*/
			
			String s1 = ""
					+ "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + " "
					+ "#" + getMonthChar(startDate.getMonth()) + " "
					+ (startDate.getYear() + 1900);
			String s2 = ""
					+ zeroPadTime(startDate.getHours()) + ":"
					+ zeroPadTime(startDate.getMinutes())
					+ " $T "
					+ zeroPadTime(endDate.getHours()) + ":"
					+ zeroPadTime(endDate.getMinutes());

			print(s1, -0.2f, ystart - lineHeight, 1.0f, textOpacity);
			print(s2, -0.2f, ystart - lineHeight*2, 1.0f, textOpacity);
		} else { // week
			renderSquare(mTexGranularities, -1.0f, -1f + 0.11f, 2.0f, 0.25f, 0, gran_vh * 1, 1, 0, 1.0f);

/*			String s1 = "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + "/" + (startDate.getMonth() + 1)
					+ " $& " + "%" + ((endDate.getDay() + 6) % 7) + " "
					+ (endDate.getDate()) + "/" + (endDate.getMonth() + 1);
			String s2 = "$Y " + (startDate.getYear() + 1900);*/
			
			String s1 = ""
					+ "%" + ((startDate.getDay() + 6) % 7) + " "
					+ (startDate.getDate()) + " "
					+ "#" + getMonthChar(startDate.getMonth())
					+ " $T "
					+ "%" + ((endDate.getDay() + 6) % 7) + " "
					+ (endDate.getDate()) + " "
					+ "#" + getMonthChar(endDate.getMonth());
			String s2 = ""
					+ (startDate.getYear() + 1900);

			print(s1, -0.2f, ystart - lineHeight, 1.0f, textOpacity);
			print(s2, -0.2f, ystart - lineHeight*2, 1.0f, textOpacity);
		}

		// "Showing your biodata between
//		renderSquare(mTexShowing, -0.2f, ystart, 0.8f, textHeight, 0, 1, 1, 0, textOpacity);
		charOut("showing", -0.2f, ystart, 1.0f, textOpacity);
//		renderSquare(mTexShowing, -0.2f, ystart, 0.8f, textHeight, 0, 1, 1, 0, textOpacity);

//		print("%0 %6 #1 #c 1/2.", -0.8f, -0.5f, 1.0f, 1.0f);
	}

	@Override
	public void init() {
		super.init();

		mNumberOfVertsPerPrim = 6;

		mShader = Core.getInstance().loadProgram("timespan", kVertexShader, kFragmentShader);
		GLES20.glBindAttribLocation(mShader, 0, "position");
		GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
		GLES20.glLinkProgram(mShader);
		mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader,
				"worldViewProjection");
		mOpacityLoc = GLES20.glGetUniformLocation(mShader, "opacity");
		mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mVertexBuffer.capacity(), mVertexBuffer,
				GLES20.GL_DYNAMIC_DRAW);

		// mTexId = Core.getInstance().loadGLTexture(R.raw.numbers_thin, new
		// Texture2DParameters());
		mTexId = Core.getInstance().loadGLTexture(R.raw.numbers,
				new Texture2DParameters());
//		mTexDays = Core.getInstance().loadGLTexture(R.raw.days,
//				new Texture2DParameters());

		mTexShowing = Core.getInstance().loadGLTexture(R.raw.showingbiodata, new Texture2DParameters());
		mTexGranularities = Core.getInstance().loadGLTexture(R.raw.granularities, new Texture2DParameters());
		mTexNumbers = Core.getInstance().loadGLTexture(R.raw.numbers_helv, new Texture2DParameters());
		mTexDays = Core.getInstance().loadGLTexture(R.raw.days_helv_italic, new Texture2DParameters());
		mTexTexts = Core.getInstance().loadGLTexture(R.raw.text_and_year, new Texture2DParameters());
		mTexTextsTo = Core.getInstance().loadGLTexture(R.raw.text_to, new Texture2DParameters());
		mTexTexts2 = Core.getInstance().loadGLTexture(R.raw.texts, new Texture2DParameters());

/*		mCharMappings.put("0", new Char(mTexNumbers, '0', 15, 185 / 200f, 0.5f, 1, 0));
		mCharMappings.put("1", new Char(mTexNumbers, '1', 12, 5 / 200f, 0.5f, 17 / 200f, 0));
		mCharMappings.put("2", new Char(mTexNumbers, '2', 17, 22 / 200f, 0.5f, 39 / 200f, 0));
		mCharMappings.put("3", new Char(mTexNumbers, '3', 17, 42 / 200f, 0.5f, 59 / 200f, 0));
		mCharMappings.put("4", new Char(mTexNumbers, '4', 15, 63 / 200f, 0.5f, 78 / 200f, 0));
		mCharMappings.put("5", new Char(mTexNumbers, '5', 16, 83 / 200f, 0.5f, 99 / 200f, 0));
		mCharMappings.put("6", new Char(mTexNumbers, '6', 16, 103 / 200f, 0.5f, 119 / 200f, 0));
		mCharMappings.put("7", new Char(mTexNumbers, '7', 14, 126 / 200f, 0.5f, 140 / 200f, 0));
		mCharMappings.put("8", new Char(mTexNumbers, '8', 14, 145 / 200f, 0.5f, 159 / 200f, 0));
		mCharMappings.put("9", new Char(mTexNumbers, '9', 14, 165 / 200f, 0.5f, 179 / 200f, 0));
		mCharMappings.put(":", new Char(mTexNumbers, ':', 4, 22 / 200f, 1, 26 / 200f, 0.5f));
		mCharMappings.put(".", new Char(mTexNumbers, '.', 4, 22 / 200f, 1, 26 / 200f, 0.5f));
		mCharMappings.put("/", new Char(mTexNumbers, '/', 13, 5 / 200f, 1, 18 / 200f, 0.5f));*/

/*		mCharMappings.put("Mon", new Char(mTexDays, ' ', 50, 0, 23 / 210f, 50 / 55f, 0 / 210f));
		mCharMappings.put("Tue", new Char(mTexDays, ' ', 41, 0, 53 / 210f, 41 / 55f, 30 / 210f));
		mCharMappings.put("Wed", new Char(mTexDays, ' ', 53, 0, 83 / 210f, 53 / 55f, 60 / 210f));
		mCharMappings.put("Thu", new Char(mTexDays, ' ', 42, 0, 113 / 210f, 42 / 55f, 90 / 210f));
		mCharMappings.put("Fri", new Char(mTexDays, ' ', 30, 0, 143 / 210f, 30 / 55f, 120 / 210f));
		mCharMappings.put("Sat", new Char(mTexDays, ' ', 39, 0, 173 / 210f, 39 / 55f, 150 / 210f));
		mCharMappings.put("Sun", new Char(mTexDays, ' ', 44, 0, 203 / 210f, 44 / 55f, 180 / 210f));*/

		mCharMappings.put("and", new Char(mTexTexts, ' ', 26, 0, 23 / 46f, 26 / 30f, 0 / 46f));
		mCharMappings.put("year", new Char(mTexTexts, ' ', 30, 0, 46 / 46f, 30 / 30f, 23 / 46f));
//		mCharMappings.put("to", new Char(mTexTextsTo, ' ', 17, 0,1,1,0));

		float lh = 32f/512f;
		mCharMappings.put("Jan", new Char(mTexTexts2, ' ', 48, 0,lh*1, 48/512f, lh*0));
		mCharMappings.put("Feb", new Char(mTexTexts2, ' ', 51, 0,lh*2, 51/512f, lh*1));
		mCharMappings.put("Mar", new Char(mTexTexts2, ' ', 51, 0,lh*3, 51/512f, lh*2));
		mCharMappings.put("Apr", new Char(mTexTexts2, ' ', 50, 0,lh*4, 50/512f, lh*3));
		mCharMappings.put("May", new Char(mTexTexts2, ' ', 53, 0,lh*5, 53/512f, lh*4));
		mCharMappings.put("Jun", new Char(mTexTexts2, ' ', 60, 0,lh*6, 60/512f, lh*5));
		mCharMappings.put("Jul", new Char(mTexTexts2, ' ', 52, 0,lh*7, 52/512f, lh*6));
		mCharMappings.put("Aug", new Char(mTexTexts2, ' ', 51, 0,lh*8, 51/512f, lh*7));
		mCharMappings.put("Sep", new Char(mTexTexts2, ' ', 59, 0,lh*9, 59/512f, lh*8));
		mCharMappings.put("Oct", new Char(mTexTexts2, ' ', 47, 0,lh*10, 47/512f, lh*9));
		mCharMappings.put("Nov", new Char(mTexTexts2, ' ', 52, 0,lh*11, 52/512f, lh*10));
		mCharMappings.put("Dec", new Char(mTexTexts2, ' ', 51, 0,lh*12, 51/512f, lh*11));
		
		mCharMappings.put("Mon", new Char(mTexTexts2, ' ', 54, 311/512f, lh*1, 365/512f, lh*0));
		mCharMappings.put("Tue", new Char(mTexTexts2, ' ', 57, 311/512f, lh*2, 367/512f, lh*1));
		mCharMappings.put("Wed", new Char(mTexTexts2, ' ', 58, 311/512f, lh*3, 369/512f, lh*2));
		mCharMappings.put("Thu", new Char(mTexTexts2, ' ', 56, 311/512f, lh*4, 367/512f, lh*3));
		mCharMappings.put("Fri", new Char(mTexTexts2, ' ', 33, 311/512f, lh*5, 344/512f, lh*4));
		mCharMappings.put("Sat", new Char(mTexTexts2, ' ', 42, 311/512f, lh*6, 353/512f, lh*5));
		mCharMappings.put("Sun", new Char(mTexTexts2, ' ', 48, 311/512f, lh*7, 359/512f, lh*6));

		mCharMappings.put("0", new Char(mTexTexts2, '0', 17, 1/512f,lh*14,18/512f,lh*13));
		mCharMappings.put("1", new Char(mTexTexts2, '1', 11, 27/512f,lh*14,38/512f,lh*13));
		mCharMappings.put("2", new Char(mTexTexts2, '2', 19, 44/512f,lh*14,63/512f,lh*13));
		mCharMappings.put("3", new Char(mTexTexts2, '3', 17, 67/512f,lh*14,84/512f,lh*13));
		mCharMappings.put("4", new Char(mTexTexts2, '4', 16, 90/512f,lh*14,106/512f,lh*13));
		mCharMappings.put("5", new Char(mTexTexts2, '5', 17, 112/512f,lh*14,129/512f,lh*13));
		mCharMappings.put("6", new Char(mTexTexts2, '6', 18, 135/512f,lh*14,152/512f,lh*13));
		mCharMappings.put("7", new Char(mTexTexts2, '7', 15, 159/512f,lh*14,174/512f,lh*13));
		mCharMappings.put("8", new Char(mTexTexts2, '8', 17, 178/512f,lh*14,195/512f,lh*13));
		mCharMappings.put("9", new Char(mTexTexts2, '9', 17, 200/512f,lh*14,217/512f,lh*13));

		mCharMappings.put("/", new Char(mTexTexts2, '/', 15, 220/512f,lh*14,235/512f,lh*13));
		mCharMappings.put(".", new Char(mTexTexts2, '.', 7, 237/512f,lh*14,244/512f,lh*13));
		mCharMappings.put(":", new Char(mTexTexts2, ':', 7, 237/512f,lh*14,244/512f,lh*13));

		mCharMappings.put("showing", new Char(mTexTexts2, ' ', 174, 0/512f,lh*15,174/512f,lh*14));
		mCharMappings.put("to", new Char(mTexTexts2, ' ', 18, 0/512f,lh*16,18/512f,lh*15));

		mVboTex = new int[1];
		GLES20.glGenBuffers(1, mVboTex, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer,
				GLES20.GL_STATIC_DRAW);

		mVboDays = new int[2];
		GLES20.glGenBuffers(2, mVboDays, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboDays[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mDaysGeomBuffer.capacity(), mDaysGeomBuffer,
				GLES20.GL_STATIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboDays[1]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				4 * mDaysTcBuffer.capacity(), mDaysTcBuffer,
				GLES20.GL_STATIC_DRAW);

	}

	private static final String kVertexShader = "precision mediump float; \n"
			+ "attribute vec3 position; \n"
			+ "attribute vec2 textureCoordinates; \n"
			+ "uniform mat4 worldViewProjection; \n"
			+ "uniform float opacity; \n" + "varying vec2 tc; \n"
			+ "void main() { \n" + "  tc = textureCoordinates; \n"
			+ "  gl_Position = worldViewProjection * vec4(position, 1.0); \n"
			+ "}";

	private static final String kFragmentShader = "precision mediump float; \n"
			+ "uniform sampler2D textureSampler; \n"
			+ "uniform float opacity; \n"
			+ "varying vec2 tc; \n"
			+ "void main() { \n"
			+
			// "  float t = texture2D(textureSampler, tc).r; \n" +
			"  vec4 col = texture2D(textureSampler, tc); \n"
			+ "  col.a = col.a*opacity; \n"
			+ "  gl_FragColor = col; //vec4(0,0,0,t); \n" + "}";

	class UV {
		float u0, u1, v0, v1;

		UV(float u0, float u1, float v0, float v1) {
			this.u0 = u0;
			this.u1 = u1;
			this.v0 = v0;
			this.v1 = v1;
		}
	}
}
