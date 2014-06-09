package se.sics.ah3.graphics;

import se.sics.ah3.AHState;
import se.sics.ah3.interaction.Controller;
import se.sics.ah3.interaction.ViewPort;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

/**
 * CoreView.java
 * 
 * @author mareri
 *
 */

public class CoreView extends GLSurfaceView implements View.OnTouchListener{
	GL20Renderer mRenderer;
//	ViewPort mViewPort;
	Controller mController;
	MeshNode mRoot;	
	MeshNode mSpiralNode;
	MeshNode mTopUiNode;
	MeshNode mBottomUiNode;
	Overview mOverviewNode;
	SpiralParser mSpiralParser;
	
	public CoreView(Context context, Handler dialogHandler) {
		super(context);
		Core.getInstance().setApplicationContext(context);
		setEGLContextClientVersion(2);
		setEGLConfigChooser(8, 8, 8, 0, 16, 0);
//		mViewPort = new ViewPort();
//		AHState.getInstance().mViewPort = mViewPort;
		mController = new Controller(this, AHState.getInstance().mViewPort, dialogHandler);
		buildSceneGraph();	
		Core.getInstance().setRoot(mRoot);
		mRenderer = new GL20Renderer(mRoot, AHState.getInstance().mViewPort, mController, this);
		mController.setPickHandler(mRenderer);
		setRenderer(mRenderer);

		setOnTouchListener(this);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return mController.onTouch(v, event);
	}
	
	public void setScreenSize(float width, float height) {
		mTopUiNode.translate(0.0f, Core.SCREEN_SPACE_CAMERA.getHeightScaledCoordinate(-1.0f), 0.0f);
		mBottomUiNode.translate(0.0f, Core.SCREEN_SPACE_CAMERA.getHeightScaledCoordinate(1.0f), 0.0f);
	}

	private void buildSceneGraph() {
		mRoot = new MeshNode("root");
		mRoot.add(new Background());
//		mRoot.add(new Backdrop());

		buildAndAddOverviewNode();
		buildAndAddSpiralNode();
		buildAndAddTopUiNode();
		buildAndAddBottomUiNode();
	}

	private void buildAndAddOverviewNode() {
		mOverviewNode = new Overview("OverviewNode", getContext());
		mOverviewNode.create();
		mOverviewNode.setVisible(false);
		mRoot.add(mOverviewNode);
	}

	private void buildAndAddBottomUiNode() {
		mBottomUiNode = new MeshNode("BottompUiNode");
		
		mBottomUiNode.add(new UIStatusElements("Status", -1.0f, -0.15f, UIElement.ALIGN_LEFT));
		UIElement tagIcon = new UIElement("TAGICON", "tag icon", 402.0f*2.0f/480.0f - 1.0f, -0.15f, UIElement.ALIGN_LEFT, true);
		UIElement tagText = new UIElement("TAGTEXT", "tag text", 356.0f*2.0f/480.0f - 1.0f, -0.15f, UIElement.ALIGN_LEFT, true);
		tagIcon.setPickScale(3.0f, 3.0f);
		tagText.setPickable(false);
		
		mBottomUiNode.add(tagText);
		mBottomUiNode.add(tagIcon);

		mRoot.add(mBottomUiNode);
	}

	private void buildAndAddTopUiNode() {
		mTopUiNode = new MeshNode("TopUiNode");
		float margin = 26.0f*2.0f/480.0f;
		
		mTopUiNode.add(new UIElement("TSCROLLP", "arrow left", -1.0f + margin, 0.15f, UIElement.ALIGN_LEFT, true));
		mTopUiNode.add(new UIElement("TSCROLLN", "arrow right", 204.0f*2.0f/480.0f - 1.0f, 0.15f, UIElement.ALIGN_RIGHT, true));
		mTopUiNode.add(new UITimeEndElements("TimeEnd", -0.71f, 0.1f, UIElement.ALIGN_LEFT, true));

		mTopUiNode.add(new UITimespanElements("Timespan", 1.0f - margin, 0.15f, UIElement.ALIGN_RIGHT));
		
		UIElement divider = new UIElement("Divider", "divider", 0.0f, 0.3f, UIElement.ALIGN_CENTER, true);
		divider.scale(0.835f, 1.0f, 1.0f);
		divider.setPickable(false);
		divider.setOpacity(0.3f);
		mTopUiNode.add(divider);
		
		UIElement affectiveHealth = new UIElement("Affective Health", "affective health", 1.0f - margin, 0.4f, UIElement.ALIGN_RIGHT, true);
		affectiveHealth.setOpacity(0.3f);
		affectiveHealth.setPickable(false);
		mTopUiNode.add(affectiveHealth);
		
		// toggle buttons
		float linespacing = 0.17f;
		float buttonStart = 0.58f;
		mTopUiNode.add(new UIElement("TGSR", "skin conductance", 1.0f - margin, buttonStart, UIElement.ALIGN_RIGHT, true));
		mTopUiNode.add(new UIElement("TTAGS", "user tagging", 1.0f - margin, buttonStart + linespacing * 1, UIElement.ALIGN_RIGHT, true));
		mTopUiNode.add(new UIElement("TMOVEMENT", "movement", 1.0f - margin, buttonStart + linespacing * 2, UIElement.ALIGN_RIGHT, true));
		
		mTopUiNode.add(new UIElement("TOVERVIEW", "compare", 1.0f - margin, buttonStart + linespacing * 3.5f, UIElement.ALIGN_RIGHT, true));
		mRoot.add(mTopUiNode);
	}

	private void buildAndAddSpiralNode() {
		ViewPort masterViewPort = AHState.getInstance().mViewPort;

		SpiralShadowDynamic shadow = new SpiralShadowDynamic(masterViewPort);
		Spiral spiral = new Spiral(masterViewPort, "Spiral", getContext());
		Mesh20 movement = new Movement(masterViewPort, "Movement", getContext());
		Mesh20 tags = new UserTagRenderer(masterViewPort);
		
		
		mSpiralNode = new MeshNode("SpiralNode");
		mSpiralNode.add(shadow);
		mSpiralNode.add(spiral);
		mSpiralNode.add(movement);
		mSpiralNode.add(new Weekdays(masterViewPort));
		mSpiralNode.add(new Numbers(masterViewPort));
		mSpiralNode.add(tags);

		mSpiralNode.translate(-0.05f, 0.1f, 0);
		mSpiralNode.scale(1.0f,1.0f,1.0f);
		mRoot.add(mSpiralNode);
	}

	public void showOverview() {
		//mSpiralNode.setVisible(false);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TGSR")).setVisible(false);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TTAGS")).setVisible(false);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TMOVEMENT")).setVisible(false);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TOVERVIEW")).setVisible(false);
		mOverviewNode.updateParameters();
		mOverviewNode.setVisible(true);
		
		
/*		mOverviewNode.clear();

		Parameters parameters = AHState.getInstance().mViewPort.getParameters();

		long granularity = parameters.end-parameters.start;

		long time = System.currentTimeMillis();
		for (int i=0;i<3;i++) {
			for (int j=0;j<3;j++) {
				int k = i*3+j;
				SpiralInfo si = makeSpiral(time-granularity*k, time-granularity*(k-1), (1-j)*0.6f,-(1-i)*0.6f+0.25f, 0.25f);
				mOverviewNode.add(si.node);
			}
		}
		
		mOverviewNode.init();*/
	}

	public void hideOverview() {
		//mSpiralNode.setVisible(true);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TGSR")).setVisible(true);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TTAGS")).setVisible(true);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TMOVEMENT")).setVisible(true);
		((UIElement)Core.getInstance().getRoot().getMeshInstance("TOVERVIEW")).setVisible(true);
		mOverviewNode.setVisible(false);
	}
	
	public Overview getOverview() {
		return mOverviewNode;
	}
	
}
