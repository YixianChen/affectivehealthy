package se.sics.ah3.graphics;

import java.util.Vector;

import android.opengl.Matrix;

/**
 * MeshNode consist of subtrees of nodes and has a list of meshes
 * @author mareri
 *
 */

public class MeshNode {
	private String name;
	private Vector<Mesh20> children;
	protected Vector<MeshNode> nodes;
	protected boolean visible;
	private MeshNode mParent = null;
	// transformation
	private float[] mTranslate,mScale;
	private float[] mTransform;

	public MeshNode(String id) {
		name = id;
		visible = true;
		mTransform = new float[16];
		Matrix.setIdentityM(mTransform, 0);
		mTranslate = new float[3];
		mScale = new float[3];
		mScale[0] = mScale[1] = mScale[2] = 1f;
		mTranslate[0] = mTranslate[1] = mTranslate[2] = 0.0f;
	}
	
	/**
	 * fetch specific mesh in the subtree
	 * @param id
	 * @return
	 */
	public Mesh20 getMeshInstance(String id) {
		if(children == null) {
			return null;
		}
		for(Mesh20 child : children) {
			if(child.getName().compareTo(id) == 0) {
				return child;
			}
		}
		if(nodes == null) {
			return null;
		}
		for(MeshNode node : nodes) {
			Mesh20 target = node.getMeshInstance(id);
			if(target != null) {
				return target;
			}
		}
		return null;
	}
	
	public MeshNode getParent() {
		return mParent;
	}
	
	public void setParent(MeshNode node) {
		mParent = node;
	}
	
	public final float[] traverseTransform() {
		if (mParent!=null) {
			float[] result = new float[16];
			Matrix.multiplyMM(result, 0, mParent.traverseTransform(), 0, mTransform, 0);
			return result;
		} else {
			return mTransform;
		}
	}
	
	private void makeTransform() {
		Matrix.setIdentityM(mTransform, 0);
		Matrix.translateM(mTransform, 0, mTranslate[0],mTranslate[1],mTranslate[2]);
		Matrix.scaleM(mTransform, 0, mScale[0],mScale[1],mScale[2]);
	}
	public void translate(float x, float y, float z) {
		mTranslate[0] = x; mTranslate[1] = y; mTranslate[2] = z;
//		Matrix.translateM(mTransform,0,x,y,z);
		makeTransform();
	}
	
	public float[] getTranslation() {
		return mTranslate;
	}
		
	public void scale(float x, float y, float z) {
		mScale[0] = x; mScale[1] = y; mScale[2] = z;
		makeTransform();
	}

	/**
	 * fetch a node by name
	 * @param id
	 * @return
	 */
	public MeshNode getNode(String id) {
		if(name.compareTo(id) == 0) {
			return this;
		}
		for(MeshNode node : nodes) {
			if(node.getId().compareTo(id) == 0) {
				return node;
			}
		}
		return null;
	}
	
	public void clear() {
		if (nodes!=null)
			nodes.clear();
		if (children!=null)
			children.clear();
	}

	/**
	 * draw all nodes and meshes in the subtree
	 * @param camera
	 */
	public void draw(Camera camera, boolean pick) {
		if(visible) {
			if(children != null) {
				for(Mesh20 child : children) {
					child.draw(camera, pick);
				}
			}
			if(nodes != null) {
				for(MeshNode node : nodes) {
					node.draw(camera, pick);
				}
			}
		}
	}

	/**
	 * called after graphic context is setup
	 */
	public void init() {
		if(children != null) {
			for(Mesh20 child : children) {
				child.init();
			}
		}
		if(nodes != null) {
			for(MeshNode node : nodes) {
				node.init();
			}
		}
	}
	
	/**
	 * reference mesh
	 * @param mesh
	 */
	public void add(Mesh20 mesh) {
		if(children == null) {
			children = new Vector<Mesh20>();
		}
		children.add(mesh);
		mesh.setParent(this);
	}
	
	/**
	 * reference node
	 * @param meshNode
	 */
	public void add(MeshNode meshNode) {
		if(nodes == null) {
			nodes = new Vector<MeshNode>();
		}
		nodes.add(meshNode);
		meshNode.setParent(this);
	}
	
	public boolean remove(MeshNode meshNode) {
		if (nodes==null) {
			return false;
		}
		return nodes.remove(meshNode);
	}
	
	/**
	 * id of the node
	 * @return
	 */
	public String getId() {
		return name;
	}
	
	/**
	 * 
	 * @param visibility
	 */
	public void setVisible(boolean visibility) {
		visible = visibility;
	}
}
