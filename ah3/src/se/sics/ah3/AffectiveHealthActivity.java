package se.sics.ah3;

import se.sics.ah3.graphics.Core;
import se.sics.ah3.graphics.CoreUtil;
import se.sics.ah3.graphics.CoreView;
import se.sics.ah3.graphics.GL20Renderer;
import se.sics.ah3.sensors.Accelerometer;
import se.sics.ah3.sensors.Microphone;
import se.sics.ah3.service.AHService;
import se.sics.ah3.settings.AHSettings;
import se.sics.ah3.share.LoginActivity;
import se.sics.ah3.share.ScreenShotUtils;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class AffectiveHealthActivity extends DialogActivity { //implements BluetoothListener {
    /** Called when the activity is first created. */

    View.OnTouchListener gestureListener;
    
    private AHService mService = null;
    private Accelerometer mAccelerometer;
    private Microphone mMicrophone;
    private static Context mContext;
    private int width,height;
    private CoreView view;
//    SensorNodeClient mSensorNode;
//    BluetoothClient mBluetoothClient;
//    boolean mGettingData = false;
//    String mBluetoothStatus = "";
    TextView mOutput;
    ProgressBar mProgressBar;
    private boolean mBatteryWarning = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // fade in animation
        mContext=this;
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        Log.d("AH3", "Activity: onCreate called");
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        width =getWindowManager().getDefaultDisplay().getWidth();
        height = getWindowManager().getDefaultDisplay().getHeight();
//        AHState state = AHState.getInstance();
//        state.init(this);

//        enableBluetooth();

//   		if (mBluetoothClient==null)
//   		{
//	   		mAccelerometer = new Accelerometer(this, state.mAccColumn, 500);
//	   		mMicrophone = new Microphone(this, state.mGsrColumn, 500);
//   		}

        view = new CoreView(this, this.mHandler);
   		//setContentView(view);
        setContentView(R.layout.main);
        
        TextView tv = (TextView)findViewById(R.id.output);
        FrameLayout layout = (FrameLayout)findViewById(R.id.framelayout);
        layout.addView(view, 0);
        mOutput = tv;
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        tv.setVisibility(View.INVISIBLE);

//        startUpdater();
//        LayoutParams layoutparams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
//        view.setLayoutParams(layoutparams);
//   		initApp();

/*        mAdapter = new ArrayAdapter<String>(this, R.layout.bluetoothlistentry); //, new String[] {"Hej"}); 
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Make your selection");
		builder.setAdapter(mAdapter, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
//		         dialog.dismiss();
//		         mBluetoothClient.chooseDevice(item);
//		         Toast.makeText(AffectiveHealthActivity.this, "hej " + item, Toast.LENGTH_LONG).show();
		    	bDialog = false;
		    }
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		new Thread(new Runnable() {
			public void run() {
				while(bDialog) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ie) { }
					mOutput.post(new Runnable() {
						public void run() {
							mAdapter.add("tjo");
						}
					});
				}
			}
		}).start();*/
//        mSensorNode = new SensorNodeClient(this);
//        mSensorNode.queryAndConnect(mDataListener);
        
        Intent i = new Intent(this, AHService.class);
        startService(i);
	}
//	boolean bDialog = true;
//	ArrayAdapter<String> mAdapter;
	
	private Thread mUpdateThread = null;
	private void startUpdater() {
		mUpdateThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(200);
						updateInfo();
					} catch (InterruptedException ie) {
						break;
					}
				}
			}
		});
		mUpdateThread.start();
	}
	private void stopUpdater() {
		mUpdateThread.interrupt();
		mUpdateThread = null;
	}
	
	private void updateInfo() {
		mOutput.post(new Runnable() {
			@Override
			public void run() {
				AHState ah = AHState.getInstance();
				String dataTime = "Recieving data: " + CoreUtil.formatTimeString(ah.mRTtime);
//				String str = mBluetoothStatus + " (" + ah.mDataRate + ")" + "\n" + (mGettingData?dataTime:"");
				String str = mService!=null?mService.getStatusString():"Waiting for service";
				if (mService!=null && mService.isUploading()) str += "\nUploading...";

				mOutput.setText(str);
				
				if (ah.hasRealtimeData()) {
					mProgressBar.setVisibility(View.INVISIBLE);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection

	    try {
			switch (item.getItemId()) {
			case R.id.bluetooth:
				System.out.println("RSTART BT");
//				AHState.getInstance().getDtiLauncher().new RestartBluetooth().execute();
/*				if (mBluetoothClient==null)
				{
					enableBluetooth();
				}
				if (mBluetoothClient!=null)
				{
					mBluetoothClient.startSearchAndConnect();
				}
				else
				{
					Toast.makeText(this, "Using mockup data. Bluetooth not available.", Toast.LENGTH_LONG).show();
					mBluetoothStatus = "Using mockup data. Bluetooth not available.";
				}*/
				if (mService.scanAndConnect(true)) {
					// is establishing connection.
				} else {
					// already connected... which is good!
				}

			    return true;
/*			case R.id.btconnect:
				Intent i = new Intent(this, BluetoothActivity.class);
				startActivity(i);
			    return true;*/
			case R.id.mockdata:
				disableBluetooth();
				enablePhoneSensors();
				startPhoneSensors();
				return true;
			//case R.id.upload:
				//startUploader();
				//return true;
			case R.id.share:
				
				GL20Renderer.setFlagScreeshot(true);
				boolean result = ScreenShotUtils.shotBitmap();  
		        if(result)  
		        {  
		            Toast.makeText(AffectiveHealthActivity.this, "screenshot successfully", Toast.LENGTH_SHORT).show();  
		        }else {  
		            Toast.makeText(AffectiveHealthActivity.this, "screenshot get failed.Please try it again!!!", Toast.LENGTH_LONG).show();  
		        }  
		        if(GL20Renderer.getScreenshot()){
		        	GL20Renderer.setFlagScreeshot(false);
		        }
		        
		         Intent intent = new Intent(this, LoginActivity.class);   
			     intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
			     startActivity(intent);
			     return true;
			     
			       
			case R.id.settings:
				startActivityForResult(new Intent(this,AHSettings.class), 0);
				return true;
			case R.id.reprocess:
				AHState.getInstance().reprocessGSRFilter();
				return true;
			default:
			    return super.onOptionsItemSelected(item);
			}
		} catch (Exception e) {
			Log.e("AH3", "Exception", e);
			return true;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode==0 && resultCode==RESULT_OK) {
			// tell service that we have a change of settings.

			// load new settings
			SharedPreferences prefs = getSharedPreferences(AHSettings.PREFS_NAME, 0);
			String preferreddevice = prefs.getString(AHSettings.PREF_PREFERRED_DEVICE_ADDR, "");

			BluetoothDevice currentDevice = mService.getConnectedDevice();
			if (currentDevice!=null) {
				if (currentDevice.getAddress().compareTo(preferreddevice)!=0) {
					mService.disconnect();
					mService.scanAndConnect(true);
				}
			}
			
			mService.setConnectInterval(prefs.getInt(AHSettings.PREF_CONNECT_INTERVAL, 30));
		}
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

//		mSensorNode.close();
		
		// clear graphic caches
		Core.getInstance().clearCaches();

/*		if (mBluetoothClient!=null) {
			mBluetoothClient.stopAndDisconnect();
		} else {
			stopPhoneSensors();
		}*/
		stopPhoneSensors();
		mService.setStream(false);
		
		stopUpdater();

		unbindService(mServiceConnection);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

//		if (mBluetoothClient!=null) {
//			mBluetoothClient.startSearchAndConnect();
//		} else {
//			// start signal recorder
//			// Accelerometer
//			startPhoneSensors();
//		}
//		if (mSensorNode==null)
//			mSensorNode = new SensorNodeClient(this);

        Intent i = new Intent(this, AHService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

		startUpdater();
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			AHState.getInstance().mService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((AHService.AHBinder)service).getService();
			mService.scanAndConnect(true);
			AHState.getInstance().mService = mService;
			updateInfo();
		}
	};

	private void makeToastInUI(final String message)
	{
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(AffectiveHealthActivity.this, message, Toast.LENGTH_LONG).show();
//				mBluetoothStatus = message;
			}
		});
	}

/*	@Override
	public void bluetoothStatus(Status status, String message) {
		switch(status)
		{
		case NONE:			// either we havnt started, something has gone wrong, or we are done...
//			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			makeToastInUI(message);
			mGettingData = false;
			break;
		case SEARCHING:		// searching for devices
//			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			makeToastInUI(message);
			break;
		case CONNECTING:	// connecting to the device
			break;
		case CONNECTED:		// we are connected!
			break;
		case SENDINGTIMESTAMP:	// currently busy sending timestamp data
			makeToastInUI(message);
			break;
		case RECEIVINGDATA:		// receiving data
//			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			makeToastInUI(message);
			mGettingData = true;
			mProgressBar.post(new Runnable() { public void run() {
				mProgressBar.setVisibility(View.VISIBLE);
			} } );
			break;
		case BATTERY_LOW:
			if (!mBatteryWarning) {
				mBatteryWarning = true;
				makeToastInUI("Low on battery");

				mOutput.post(new Runnable() { public void run() {
					mOutput.setBackgroundColor((255<<24) + (192<<16));
				} } );
			}
			break;
		}
	}*/

/*	@Override
	protected Dialog onCreateDialog(int id) {
//		String[] items = new String[]{"hej"};
		String[] items = mDialogItems;
		
		Log.d("Bluetooth", "Displaying dialog: " + items.length);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Make your selection");
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		         dialog.dismiss();
		         mBluetoothClient.chooseDevice(item);
//		         Toast.makeText(AffectiveHealthActivity.this, "hej " + item, Toast.LENGTH_LONG).show();
		    }
		});
		AlertDialog alert = builder.create();
		return alert;

//		return super.onCreateDialog(id);
	}*/

	private String[] mDialogItems;

/*	@Override
	public void bluetoothDevices(Vector<BluetoothDevice> dtiDevices) {
		mOutput.post(new Runnable() {

			@Override
			public void run() {
				if (mDevicesDialog!=null)
					mDevicesDialog.setTitle("Done");
			}
		});
	}*/
	
	private void disablePhoneSensors()
	{
		if (mAccelerometer!=null)
		{
			mAccelerometer.stop();
			mAccelerometer = null; 
		}
		if (mMicrophone!=null)
		{
			mMicrophone.stop();
			mMicrophone = null;
		}
	}

	private void disableBluetooth()
	{
//		if (mBluetoothClient!=null)
//		{
//			mBluetoothClient.stopAndDisconnect();
//			mBluetoothClient = null;
//		}
		if (mService!=null) {
			mService.disconnect();
		}
	}

/*	private void enableBluetooth()
	{
		disablePhoneSensors();
		if (mBluetoothClient==null)
		{
			mBluetoothClient = new BluetoothClient(this);
	   		mBluetoothClient.setListener(this);
		}
	}*/
	
	private void enablePhoneSensors()
	{
		if (mAccelerometer==null)
			mAccelerometer = new Accelerometer(this, AHState.getInstance().mAccColumn, 500);
		if (mMicrophone==null)
			mMicrophone = new Microphone(this, AHState.getInstance().mGsrColumn, 500);
	}

	private void startPhoneSensors()
	{
		if (mAccelerometer!=null)
			mAccelerometer.start();
		if (mMicrophone!=null)
			mMicrophone.start();
	}

	private void stopPhoneSensors()
	{
		if (mAccelerometer!=null)
			mAccelerometer.stop();
		if (mMicrophone!=null)
			mMicrophone.stop();
	}

//	private void startUploader() {
//		mService.startDataUpload();
//	}
	
/*		boolean stream = false;	// just upload old data and end
		final DataUploader uploader = new DataUploader(this,AHState.getInstance().mDatabaseTable, stream);
		uploader.start();
		// create and show upload text view
		Log.d("Uploader", "Starting to upload");
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					if (uploader.mState==DataUploader.STATE_UPLOAD) {
						Log.d("Uploader", ""+uploader.status_progress + "/" + uploader.status_toupload);
					} else if (uploader.mState==DataUploader.STATE_DONE) {
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
					}
				}
				// destroy upload text view
				Log.d("Uploader", "Upload done");
			}
		}).start();
	}*/

//	AlertDialog mDevicesDialog = null;
//	ArrayAdapter<String> mDevicesAdapter = null;

/*	@Override
	public void bluetoothDevice(BluetoothDevice device) {
		if (mDevicesDialog==null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(AffectiveHealthActivity.this);
			builder.setTitle("Searching...");
			builder.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					 makeToastInUI("No device selected");
			         mDevicesDialog = null;
				}
			});
			mDevicesAdapter = new ArrayAdapter<String>(this, R.layout.listitem);
			builder.setAdapter(mDevicesAdapter, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			         dialog.dismiss();
			         mBluetoothClient.chooseDevice(item);
			         mDevicesDialog = null;
			    }
			});

			mDevicesDialog = builder.create();
			mDevicesDialog.show();
		}

		mDevicesAdapter.add(device.getName());
	}*/

}
