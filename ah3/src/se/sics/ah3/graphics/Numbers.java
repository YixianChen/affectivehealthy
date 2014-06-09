package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Vector;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import android.opengl.GLES20;

/**
 * Labels for hours and minutes
 * @author mareri
 *
 */

public class Numbers extends Mesh20 { 
	private final static int MAX_NUMBER_OF_ENTRIES = 4000;
	
	int mModelViewProjectionLoc;
	int mModelViewLoc;
	int mNumberOfEntries;
	int mNumberOfVertsPerPrim;
	int mTexId, mTexSampler;
	int[] mVboTex;
	
	FloatBuffer mTextureCoordBuffer;
	FloatBuffer mOpacityBuffer;
	
	Vector<UV> uvs;
	
	private ViewPort mViewPort;
	
	public Numbers(ViewPort viewport) {
		super("Numbers");
		
		mViewPort = viewport;
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		
		ByteBuffer tcbb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 2 * 4);
		tcbb.order(ByteOrder.nativeOrder());
		mTextureCoordBuffer = tcbb.asFloatBuffer();
		
		ByteBuffer obb = ByteBuffer.allocateDirect(MAX_NUMBER_OF_ENTRIES * 4);
		obb.order(ByteOrder.nativeOrder());
		mOpacityBuffer = obb.asFloatBuffer();
		
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		
		// texture atlas coordinates
		uvs = new Vector<Numbers.UV>();
		uvs.add(new UV(0f,    0.25f,  0.0f, 0.25f)); // 0
		uvs.add(new UV(0.25f, 0.5f,  0.0f,  0.25f)); // 1
		uvs.add(new UV(0.5f,  0.75f,  0.0f, 0.25f)); // 2
		uvs.add(new UV(0.75f, 1.0f,  0.0f,  0.25f)); // 3
		uvs.add(new UV(0f, 	  0.25f, 0.25f, 0.5f)); // 4
		uvs.add(new UV(0.25f, 0.5f, 0.25f,  0.5f)); // 5
		uvs.add(new UV(0.5f,  0.75f, 0.25f, 0.5f)); // 6
		uvs.add(new UV(0.75f, 1.0f, 0.25f,  0.5f)); // 7
		uvs.add(new UV(0f,    0.25f,  0.5f, 0.75f)); // 8
		uvs.add(new UV(0.25f, 0.5f,  0.5f,  0.75f)); // 9
		uvs.add(new UV(0.5f,  0.75f,  0.5f, 0.75f)); // .
		uvs.add(new UV(0f,  0.25f,  0.75f, 1.0f)); // blank
	}
	
	void submitPrimitive(float[] point, float[] normal, float[] tangent, int texture, float opacity) {
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

		float u1 = uv.u0;
		float u0 = uv.u1;
		float v1 = uv.v0;
		float v0 = uv.v1;
		
/*		if(tangent[1] < 0f) {
			float switchTemp = u0;
			u0 = u1;
			u1 = switchTemp;
			
			switchTemp = v0;
			v0 = v1;
			v1 = switchTemp;
		}*/
		
		// t0
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v1);
		// t1
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v0);
		// t2
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v1);
		// t2
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v1);
		// t1
		mTextureCoordBuffer.put(u0);
		mTextureCoordBuffer.put(v0);
		// t3
		mTextureCoordBuffer.put(u1);
		mTextureCoordBuffer.put(v0);

		for (int i=0;i<6;i++) {
			mOpacityBuffer.put(opacity);
		}

		mNumberOfEntries++;
	}
	
	int convertToUVIndex(char c) {
		switch(c) {
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
				return (int)(c - 48);
			default:
				return 11;
		}
	}
	
	// assumes the numbers are in the form 1234 where a dot will be put at 12.34
	void addEntry(String number, float offset) {
		float thickness = 0.15f; //SpiralFormula.getThickness(offset);
		float opacity = 1.0f;
		
		if (offset<0.05f) {
			opacity = offset/0.05f;
		}
		if (offset>0.95f) {
			opacity = (1-offset)/0.05f;
		}

		offset = SpiralFormula.rescaleTToAfterRealtimeSegment(offset);

		float[] point = { 0f, 0f, 0f };
		float[] normal = { 1f, 0f, 0f };
		float[] tangent = { 0f, 1f, 0f };
		
		float widthFactor = 0.25f;
		
		SpiralFormula.parametricFormulation(offset, point);
		normal = SpiralFormula.getNormal(offset);
		tangent = SpiralFormula.getTangent(offset);
		
		CoreUtil.normalize(normal);
		CoreUtil.normalize(tangent);
		
		float[] tmp = {0,0,0};
		CoreUtil.scale(-SpiralFormula.getThickness(offset) - 0.04f, normal, tmp);
		point = CoreUtil.add(point, tmp);
		
		CoreUtil.scale(widthFactor * thickness, normal);
		CoreUtil.scale(widthFactor * thickness, tangent);
		
		// reverse the string for readability when upside down
		String output = number;
		output = new StringBuffer(number).reverse().toString();
		if(tangent[1] < 0f) {
//			output = new StringBuffer(number).reverse().toString();
		}
		
		float sizeFactor = 0.25f / widthFactor;
		float pos = sizeFactor * (float)(-output.length() / 2);
		if(output.length() % 2 == 0) {
			pos += sizeFactor * 0.5f;
		}
		float[] shiftedPosition = new float[3];
		for(int i = 0; i < output.length(); i++) {
//			CoreUtil.scale(pos, normal, shiftedPosition);
			CoreUtil.scale(pos, tangent, shiftedPosition);
			submitPrimitive(CoreUtil.add(point, shiftedPosition), normal, tangent, convertToUVIndex(output.charAt(i)), opacity);
			pos += sizeFactor;
		}
	}
	
	private String zeroPadTime(int v)
	{
		return v<10?"0"+v:""+v;
	}
	private String getSecondsNthsString(long time, int n)
	{
		Date currentDate = new Date(time);
		int second = currentDate.getSeconds();
		return zeroPadTime(second);
	}
	
	private String getMinutesNthsString(long time, int n)
	{
		Date currentDate = new Date(time);
		int minute = currentDate.getMinutes();
		return zeroPadTime(minute);
	}

	private String getHourString(long time)
	{
		Date currentDate = new Date(time);
		int hour = currentDate.getHours();
		return zeroPadTime(hour);
	}
	
	void setPosition() {
		// reset the buffer
		mNumberOfEntries = 0;
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		
		Parameters parameters = mViewPort.getParameters();
		
		Date currentDate = new Date(parameters.end);
		Date endDate = new Date((parameters.start));

		long timeLine = currentDate.getTime() - endDate.getTime();

		double hours = timeLine / (double)Parameters.HOUR;
		double minutes = timeLine / (double)Parameters.MINUTES;
		
		int everySecond = 0;
		int everyMinute = 0;
		int everyHour = 0;
		int days = 1;

		if (minutes<2) {
			everySecond = 5;
		} else if (minutes<=10) {
//			everySecond = 30;
			everyMinute = 1;
		} else if (hours<2) {
			everyMinute = 5;
		} else if (hours<=6) {
			everyHour = 1;
			everyMinute = 30;
		} else if (hours<=24) {
			everyHour = 2;
			days = 1;
		}

		// let's put up a few time stamps here.
		if (everySecond>0) {	// let's put out a few seconds
			long offset = (currentDate.getSeconds()%everySecond) * 1000 + (currentDate.getTime()%1000);
			long currentSecond = currentDate.getTime() - offset;
			
			while(currentSecond>endDate.getTime()) {
				if ((currentSecond/1000)%60!=0 || everyMinute==0)	// skip even minutes
				{
					float t = SpiralFormula.getT(currentSecond, parameters);
					String timeString = "   " + getSecondsNthsString(currentSecond,everySecond);
					addEntry(timeString, t);
				}
				currentSecond -= 1000*everySecond;
			}
		}
		if (everyMinute>0) {
			long offset = (currentDate.getMinutes()%everyMinute) * 60 * 1000 + currentDate.getSeconds() * 1000 + (currentDate.getTime()%1000);
			long currentMinute = currentDate.getTime() - offset;

			while(currentMinute>endDate.getTime()) {
				int h = new Date(currentMinute).getHours();
				if ((currentMinute/Parameters.MINUTES)%60!=0 || (everyHour==0 && h%24!=0)) // skip even hours
				{
					String hour = getHourString(currentMinute);
					float t = SpiralFormula.getT(currentMinute, parameters);
					String timeString = hour + " " + getMinutesNthsString(currentMinute,everyMinute);
					addEntry(timeString, t);
				}
				currentMinute -= 1000*60*everyMinute;
			}
		}
		if (everyHour>0) {
			long offset = (currentDate.getHours()%everyHour) * 60 * 60 * 1000 + currentDate.getMinutes() * 60 * 1000 + currentDate.getSeconds() * 1000 + (currentDate.getTime()%1000);
			long currentHour = currentDate.getTime() - offset;

			while(currentHour>endDate.getTime()) {
				int h = new Date(currentHour).getHours();
				if (days==0 || (h%24)!=0) // skip even days
				{
					String hour = getHourString(currentHour);
					float t = SpiralFormula.getT(currentHour, parameters);
					String timeString = hour + " 00";
					addEntry(timeString, t);
				}
				currentHour -= 1000*60*60*everyHour;
			}
		}

		// reset the buffer and commit
		mVertexBuffer.position(0);
		mTextureCoordBuffer.position(0);
		mOpacityBuffer.position(0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mOpacityBuffer.capacity(), mOpacityBuffer, GLES20.GL_DYNAMIC_DRAW);
	}

	@Override
	public void draw(Camera camera, boolean pick) {
		super.draw(camera, pick);

		setPosition();

		if(!pick) {
//			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_BLEND);
			
			GLES20.glUniform1i(mTexSampler, 0);
	        Core.getInstance().bindTexture(0, GLES20.GL_TEXTURE_2D, mTexId);
	        
			GLES20.glUniformMatrix4fv(mModelViewProjectionLoc, 1, false, camera.getmModelViewProjectionMatrix(), 0);
			GLES20.glUniformMatrix4fv(mModelViewLoc, 1, false, getTransform(), 0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
			GLES20.glEnableVertexAttribArray(1);
			Core.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
			GLES20.glEnableVertexAttribArray(2);
			Core.glVertexAttribPointer(2, 1, GLES20.GL_FLOAT, false, 0, 0);
		}
	
		if(!pick || mPickable) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
			GLES20.glEnableVertexAttribArray(Core.VERTEX);
			Core.glVertexAttribPointer(Core.VERTEX, 3, GLES20.GL_FLOAT, false, 0, 0);
		
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mNumberOfVertsPerPrim * mNumberOfEntries);
			
			GLES20.glDisable(GLES20.GL_BLEND);
		}

	}
	
	@Override
	public void init() {
		super.init();
		
		mNumberOfVertsPerPrim = 6;
		
		mShader = Core.getInstance().loadProgram("numbers", kVertexShader, kFragmentShader);
        GLES20.glBindAttribLocation(mShader, 0, "position");
        GLES20.glBindAttribLocation(mShader, 1, "textureCoordinates");
        GLES20.glBindAttribLocation(mShader, 2, "opacity");
        GLES20.glLinkProgram(mShader);
        mModelViewProjectionLoc = GLES20.glGetUniformLocation(mShader, "worldViewProjection");
        mModelViewLoc = GLES20.glGetUniformLocation(mShader, "modelView");
        mTexSampler = GLES20.glGetUniformLocation(mShader, "textureSampler");
        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboGeometry[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mVertexBuffer.capacity(), mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
	
        mTexId = Core.getInstance().loadGLTexture(R.raw.numbers_thin_alt, new Texture2DParameters());
	
        mVboTex = new int[2];
        GLES20.glGenBuffers(2, mVboTex, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mTextureCoordBuffer.capacity(), mTextureCoordBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboTex[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * mOpacityBuffer.capacity(), mOpacityBuffer, GLES20.GL_STATIC_DRAW);
	}

	private static final String kVertexShader =
        "precision mediump float; \n" +
        "attribute vec3 position; \n" +
        "attribute vec2 textureCoordinates; \n" +
        "attribute float opacity; \n" +
        "uniform mat4 worldViewProjection; \n" +
        "uniform mat4 modelView; \n" +
        "varying vec2 tc; \n" +
        "varying float visibility; \n" +
        "void main() { \n" +
        "  visibility = opacity; \n" +
        "  tc = textureCoordinates; \n" +
        "  gl_Position = worldViewProjection * modelView * vec4(position, 1.0); \n" +
        "}";

    private static final String kFragmentShader =
        "precision mediump float; \n" +
        "uniform sampler2D textureSampler; \n" +
        "varying vec2 tc; \n" +
        "varying float visibility; \n" +
        "void main() { \n" +
        "  vec4 color = texture2D(textureSampler, tc)*visibility; \n" +
//        "  color.a = color.a*visibility; \n" +
        "  gl_FragColor = vec4(0.0,0.0,0.0,color.r); \n" +
        "}";

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
