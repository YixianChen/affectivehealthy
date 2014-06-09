package se.sics.ah3.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Mesh {
	private static int SHORT_BUFFER_BYTE_SIZE = 2;
	private static int FLOAT_BUFFER_BYTE_SIZE = 4;
	
	private int numberOfVertices;
	private int numberOfIndices;
	
	private FloatBuffer vertices;
	private ShortBuffer indices;
	private FloatBuffer colors;
	private FloatBuffer textureCoordinates;
	private int mTextureId;
	
    public float x = 0;
    public float y = 0;
    public float z = 0;
 
    public float rx = 0;
    public float ry = 0;
    public float rz = 0;
    
    public Mesh() {
    	
    }
	
	public Mesh(int numberOfVertices) {
		this.numberOfVertices = numberOfVertices;
	}
	
	void setIndices(short[] indices) {
		ByteBuffer indexBuffer = ByteBuffer.allocateDirect(indices.length * SHORT_BUFFER_BYTE_SIZE);
		indexBuffer.order(ByteOrder.nativeOrder());
		this.indices = indexBuffer.asShortBuffer();
		this.indices.put(indices);
		this.indices.position(0);
		numberOfIndices = indices.length;
	}
	
	void setVertices(float[] vertices) {
		 ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * FLOAT_BUFFER_BYTE_SIZE);
		 vertexBuffer.order(ByteOrder.nativeOrder());
		 this.vertices = vertexBuffer.asFloatBuffer();
		 this.vertices.put(vertices);
		 this.vertices.position(0);
	 }
	
	 void setTextureCoordinates(float[] textureCoords) {
		ByteBuffer textureCoordinates = ByteBuffer.allocateDirect(textureCoords.length * FLOAT_BUFFER_BYTE_SIZE);
		textureCoordinates.order(ByteOrder.nativeOrder());
		this.textureCoordinates = textureCoordinates.asFloatBuffer();
		this.textureCoordinates.put(textureCoords);
		this.textureCoordinates.position(0);
	}
	
	public void draw(GL10 gl, int primitiveType) {
		setState();
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
		if(mTextureId > 0 && textureCoordinates != null) {
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureCoordinates);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);
		}
		gl.glTranslatef(x, y, z);
	    gl.glRotatef(rx, 1, 0, 0);
	    gl.glRotatef(ry, 0, 1, 0);
	    gl.glRotatef(rz, 0, 0, 1);
		gl.glDrawElements(primitiveType, numberOfIndices,
			GL10.GL_UNSIGNED_SHORT, indices);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		if(mTextureId > 0 && textureCoordinates != null) {
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		}
    }
	
	public void draw(GL10 gl) {
		draw(gl, GL10.GL_TRIANGLES);
	}
	
	void setState() {
		
	}
	
	void setTextureId(int textureId) {
		mTextureId = textureId;
	}
}
