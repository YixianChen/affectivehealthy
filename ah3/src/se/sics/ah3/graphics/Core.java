package se.sics.ah3.graphics;

import static android.opengl.GLES10.GL_TEXTURE_2D;
import static android.opengl.GLES10.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_T;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Core singleton
 * initialize by setting renderer and application context
 * @author mareri
 *
 */

public class Core {
	private static Core core = null;
	public static String TAG = "Core";
	private HashMap<Integer, Integer> cachedTextures;
	private Context applicationContext;
	
	// state variables
	private static int bindTextureTexture = 0;
	
	// constants
	// attrib buffer
	public final static int VERTEX = 0;
	public final static int TEXTURE = 1;
	// buffer size
	public final static int VERTEX_DIMENSIONS = 3;
	public final static int UV_DIMENSIONS = 2;
	public final static int T_DIMENSIONS = 1;
	
	// screen space effects
	public static Camera SCREEN_SPACE_CAMERA = new Camera();
	
	// picking
	HashMap<Integer, String> mPickList;
	private int mPickId;
	
	// scene graph
	MeshNode mRoot;
	
	// cached programs
	HashMap<String, Integer> mProgramManager;
	
	// fix for API level 8
	static {
        System.loadLibrary("gles20fix");
    }

	private Core() {
		Log.d(TAG, "Created new Core instance");
		cachedTextures = new HashMap<Integer, Integer>();
		mProgramManager = new HashMap<String, Integer>();
		core = this;
		mPickId = 0;
		mPickList = new HashMap<Integer, String>();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Cannot clone a singleton");
	}
	
	public static Core getInstance() {
		return core != null ? core : new Core();
	}
	
	public void clearCaches() {
		cachedTextures.clear();
		mProgramManager.clear();
	}
	
	/**
	 * Cache and load textures
	 * @param pathToResource
	 * @return the opengl texture object
	 */
	public int loadGLTexture(int pathToResource, Texture2DParameters textureParameters) {
		if(cachedTextures.containsKey(new Integer(pathToResource))) {
			Log.d(TAG, "Cached textured fetched");
			return cachedTextures.get(new Integer(pathToResource));
		} else {
			int[] textureId = new int[1];
		    GLES20.glGenTextures(1, textureId, 0);
		    if(textureId[0] < 1) {
		    	Log.e(TAG, "Error: Generating texture object");
		    	return -1;
		    }
		    
		    GLES20.glBindTexture(GL_TEXTURE_2D, textureId[0]);
		    GLES20.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
		            textureParameters.getmMinFilter());
		    GLES20.glTexParameterf(GL_TEXTURE_2D,
		            GL_TEXTURE_MAG_FILTER,
		            textureParameters.getmMagFilter());
		    GLES20.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
		    		textureParameters.getmWrapS());
		    GLES20.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
		    		textureParameters.getmWrapT());
		    
		    
		    //TODO: Add support internal resources
		    InputStream is = applicationContext.getResources().openRawResource(pathToResource);
			Bitmap bitmap;
			try {
				bitmap = BitmapFactory.decodeStream(is);
			} finally {
				try {
					is.close();
				} catch (IOException e) {}
			}

			GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
			bitmap.recycle();
			
			// add mip mapping
		    if(textureParameters.getMipMap()) {
		    	GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		    	Log.d(TAG, "Generated mipmap");
		    }
		    
			// store key in cache
			//cachedTextures.put(pathToResource, textureId[0]);
			//Log.d(TAG, "Loaded and cached a texture");
			
			return textureId[0];
		}
	}
	
	// assumption is that it'll only be 2d textures for now
	public void bindTexture(int channel, int target, int texture) {
		if(texture != bindTextureTexture) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + channel);
			bindTextureTexture = texture;
			GLES20.glBindTexture(target, texture);
		}
	}
	
	// native calls to come around bug in API Level 8
	native public static void glDrawElements(
            int mode, int count, int type, int offset);
    native public static void glVertexAttribPointer(
            int index, int size, int type,
            boolean normalized, int stride, int offset);
    
	public MeshNode getRoot() {
		return mRoot;
	}

	public void setRoot(MeshNode root) {
		mRoot = root;
	}
	
	public Context getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(Context applicationContext) {
		Log.d(TAG, "Application context set");
		this.applicationContext = applicationContext;
	}
	
	public void glUseProgram(int program) {
		GLES20.glUseProgram(program);
	}
	
	public void glBindBuffer(int type, int buffer) {
		GLES20.glBindBuffer(type, buffer);
	}
	
	/**
	 * another missing method..
	 */
	public static void perspectiveM(float[] m, float angle, float aspect, float near, float far) {
        float f = (float)Math.tan(0.5 * (Math.PI - angle));
        float range = near - far;

        m[0] = f / aspect;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;

        m[4] = 0;
        m[5] = f;
        m[6] = 0;
        m[7] = 0;

        m[8] = 0;
        m[9] = 0; 
        m[10] = far / range;
        m[11] = -1;

        m[12] = 0;
        m[13] = 0;
        m[14] = near * far / range;
        m[15] = 0;
    }
    
    /**
     * Picking
     */
    public void startPicking() {
    	mPickList.clear();
    	mPickId = 0;
    }
    
    public int getPickId(String name) {
    	mPickId += 10;
    	mPickList.put(mPickId, name);
    	return mPickId;
    }
    
    public String getPickedObjectName(int id) {
    	return mPickList.get(id);
    }
    
    private static int getShader(String source, int type) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) return 0;
        
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = { 0 };
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(Core.TAG, GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public int loadProgram(String name, String vertexShader, String fragmentShader) {
    	if(mProgramManager.containsKey(name)) {
    		return mProgramManager.get(name);
    	}
        int vs = getShader(vertexShader, GLES20.GL_VERTEX_SHADER);
        int fs = getShader(fragmentShader, GLES20.GL_FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return 0;

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        int[] linked = { 0 };
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(Core.TAG, GLES20.glGetProgramInfoLog(program));
            return 0;
        }
        mProgramManager.put(name, program);
        return program;
    } 
}
