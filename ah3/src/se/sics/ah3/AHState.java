package se.sics.ah3;

import se.sics.ah3.database.AccDatabaseTable;
import se.sics.ah3.database.Column;
import se.sics.ah3.database.DataBase;
import se.sics.ah3.database.DataBaseBuffer;
import se.sics.ah3.database.SignalDatabaseTable;
import se.sics.ah3.graphics.AccBuffer;
import se.sics.ah3.graphics.GsrBuffer;
import se.sics.ah3.interaction.ViewPort;
import se.sics.ah3.interaction.Parameters;
import se.sics.ah3.model.SpiralFormula;
import se.sics.ah3.service.AHService;
import se.sics.ah3.usertags.UserTags;
import android.content.Context;

public class AHState
{
	private static AHState _instance = null;
	
	private AHState()
	{
		_instance = this;
	}
	
	public static AHState getInstance()
	{
		return _instance != null ? _instance : new AHState();
	}
	
	// stuff

	public ViewPort mViewPort;

	public SignalDatabaseTable mDatabaseTable = null;
	public AccDatabaseTable mAccDatabaseTable = null;
	public Column mAccColumn = null;
	public Column mGsrColumn = null;
	public Column mGsrFilteredColumn = null;
//	public LongBuffer mTimestampColumn = null;
	public float mRTgsr = Float.NaN;
	public float mRTacc = Float.NaN;
	public long mRTtime = 0;

	public float mDataRate = 0;

	public UserTags mUserTags = null;

	public AHService mService = null;

	public void init(Context context)
	{
		if (mDatabaseTable!=null) return;

		DataBase db = new DataBase(context, "AH3DB", null, 11);
		mAccDatabaseTable = new AccDatabaseTable(context, null, 11);
		mDatabaseTable = db.createTable("MYVALS");
		DataBaseBuffer dbbuf = new DataBaseBuffer(mDatabaseTable,60*60*24*10,1000);
		mAccColumn = dbbuf.addBuffer(new AccBuffer(0.04f,1,0,1),"MOVEMENT");
		mGsrColumn = dbbuf.addBuffer(new GsrBuffer(400,100,1,0),"GSR");
		mGsrFilteredColumn = dbbuf.addBuffer(new GsrBuffer(400,100,1,0),"FILTERED_GSR");

		db.create();

		SpiralFormula.genSpiralMesh(context);

		mUserTags = new UserTags(context);

		mViewPort = new ViewPort();

//		mDtiLauncher = new DtiLauncher(mApp,mAccColumn,mGsrColumn);
//		mDtiLauncher.init();
//		new FakeSignalConsumer(500, -200, 500, mAccColumn).start();
//		new FakeSignalConsumer(500, -16, 16, mGsrColumn).start();
	}

	public void reprocessGSRFilter() {
		mDatabaseTable.processGSRFilter("GSR", "FILTERED_GSR");
	}
	
	public boolean hasRealtimeData() {
		return Math.abs(System.currentTimeMillis() - mRTtime)<4000;
	}
	
	public boolean isUpdatingRealtime() {
		Parameters p = mViewPort.getParameters();
		return System.currentTimeMillis()-p.end<1000;
	}
}
