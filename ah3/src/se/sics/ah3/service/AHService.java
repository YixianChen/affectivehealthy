package se.sics.ah3.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import se.sics.ah3.BluetoothClient;
import se.sics.ah3.BluetoothClient.Status;
import se.sics.ah3.AHState;
import se.sics.ah3.DataUploader;
import se.sics.ah3.R;
import se.sics.ah3.settings.AHSettings;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AHService extends Service implements
		BluetoothClient.BluetoothListener {
	private final static String TAG = "AHService";
	private final static String START_DATA_ACTION = "se.sics.ah3.data";

	public class AHBinder extends Binder {
		public AHService getService() {
			return AHService.this;
		}
	}

	private AHBinder mBinder = new AHBinder();

	// state settings
	private boolean mStream = false; // get realtime data or disconnect when we
										// are getting realtime data.
	private boolean mGettingRealtimeData = false;
	private String mDeviceName = "";
	private boolean mBatteryWarning = false;

	private boolean mIsSearching = false;

	private BluetoothDevice mConnectedDevice = null;

	public String getStatusString() {
		return (mBluetoothClient.isConnected() ? "Connected to "
				+ mDeviceName
				+ "\n"
				+ (mGettingRealtimeData ? "Realtime data "
						: "Download old data ")
				: ("Not connected \n" + (mIsSearching ? "Searching" : mError)));
	}

	public enum BTStatus {
		WAITING, CONNECTING, ERROR, DOWNLOADING, REALTIME, BLUETOOTH_OFF, NO_DEVICE_SET
	}

	public BTStatus getStatus() {
		AHState ah = AHState.getInstance();
		if (ah.hasRealtimeData()) {
			return BTStatus.REALTIME;
		} else {
			if (mBluetoothClient.getStatus() == BluetoothClient.Status.BLUETOOTH_OFF) {
				return BTStatus.BLUETOOTH_OFF;
			} 
			else if(!mBluetoothClient.hasPreferredDevice()) {
				return BTStatus.NO_DEVICE_SET;
			}
			else {
				if (ah.mService != null) {
					if (!ah.mService.isConnected()
							&& !ah.mService.isSearching()) {
						return BTStatus.ERROR;
					} else if (!ah.mService.isConnected()) {
						return BTStatus.CONNECTING;
					} else {
						return BTStatus.DOWNLOADING;
					}
				} else {
					return BTStatus.WAITING;
				}
			}
		}
	}

	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private int mNotificationID = 0;
	private PendingIntent mPendingIntent;

	// Alarm
	private PendingIntent mPendingService;

	// engine stuff

	BluetoothClient mBluetoothClient;
	// BluetoothClient.BluetoothListener mBluetoothListener;

	private DataUploader mDataUploader = null;

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		// mNotificationManager.cancel(mNotificationID);
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "onUnbind");

		if (mBluetoothClient.isConnected()) {
			// mNotification.setLatestEventInfo(this, "AH running",
			// "Bluetooth connection active", mPendingIntent);
			// mNotificationManager.notify(mNotificationID,mNotification);
		}
		/*
		 * else if (mDataUploader!=null) { // only do this if we are not
		 * receiving data from bluetooth. should be seen as a warning...
		 * mNotification.setLatestEventInfo(this, "HRV uploader active",
		 * "Upload thread is running. Might as well turn it off if not receiving new data as we are not connected to the bluetooth device."
		 * , mPendingIntent);
		 * mNotificationManager.notify(mNotificationID,mNotification); }
		 * 
		 * if (!mBluetoothClient.isConnected() && mDataUploader==null) {
		 * stopSelf(); }
		 */
		return true; // super.onUnbind(intent);
	}

	// public void setActivity(UIListener listener) {
	// mUIListener = listener;
	// }

	// first thing run
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		// TODO Auto-generated method stub
		super.onCreate();

		// mData = new HRVData(this);

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotification = new Notification(R.drawable.icon, "AH Service",
				System.currentTimeMillis());

		Intent notificationIntent = new Intent(this,
				se.sics.ah3.AffectiveHealthActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		mPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				Intent.FLAG_ACTIVITY_NEW_TASK);
		mNotification.setLatestEventInfo(this, "AH scheduler running",
				"Taking care of your bracelet so you don't have to.",
				mPendingIntent);
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;

		startForeground(mNotificationID, mNotification);

		// mNotificationManager.notify(mNotificationID, mNotification);

		mGettingRealtimeData = false;
		mBluetoothClient = new BluetoothClient(this);
		mBluetoothClient.setListener(this);

		// setup alarm
		SharedPreferences prefs = getSharedPreferences(AHSettings.PREFS_NAME, 0);
		Intent serviceIntent = new Intent(this, AHService.class);
		serviceIntent.setAction(START_DATA_ACTION);
		mPendingService = PendingIntent.getService(this, 0, serviceIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		setConnectInterval(prefs.getInt(AHSettings.PREF_CONNECT_INTERVAL, 30));
	}

	public void setConnectInterval(int minutes) {
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		long interval = 1000 * 60 * minutes;
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ interval, interval, mPendingService);
	}

	// last thing run
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();

		mNotificationManager.cancelAll();

		mBluetoothClient.stopAndDisconnect();
	}

	// each time started by activity
	@Override
	public void onStart(Intent intent, int startId) {
		// this method wont be called as we have START_STICKY in onStartCommand
		Log.d(TAG, "onStart");
		super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		String action = intent != null ? intent.getAction() : null;
		if (action != null && action.compareTo(START_DATA_ACTION) == 0)
			if (!mBluetoothClient.isConnected())
				scanAndConnect(mStream);
		return START_STICKY;
	}

	public boolean scanAndConnect(boolean stream) {
		// mStream = stream;
		setStream(stream);
		if (!mBluetoothClient.hasPreferredDevice()) {
			setError("No device set");
			return false;
		}
		if (!mBluetoothClient.isConnected()) {
			Log.d(TAG, "Calling start serach and connect");
			mIsSearching = mBluetoothClient.startSearchAndConnect();
		} else {
			Log.d(TAG, "Already connected");
			return false;
		}
		return true;
	}

	public void disconnect() {
		mBluetoothClient.stopAndDisconnect();
	}

	@Override
	public void bluetoothDevices(Vector<BluetoothDevice> dtiDevices) {
		// if (mUIListener!=null) {
		// mUIListener.bluetoothDevices(dtiDevices);
		// }
		mIsSearching = false;
	}

	@Override
	public void bluetoothStatus(Status status, String message) {
		Log.d(TAG, "Bluetooth Status: " + message);
		switch (status) {
		case LOST_CONNECTION:
			if (mStream) {
				// only notify if the streaming setting is set to true.
				// otherwise we do not want to be connected... unless we are
				// actually transferring old data...
				mNotification.setLatestEventInfo(this, "Lost connection",
						"Connection with bluetooth node ended", mPendingIntent);
				long[] vibrate = { 0, 100, 200, 300, 100, 300, 100, 300 };
				mNotification.vibrate = vibrate;
				mNotificationManager.notify(mNotificationID, mNotification);
			}
			mConnectedDevice = null;
			break;
		case BATTERY_LOW:
			if (!mBatteryWarning) {
				mBatteryWarning = true;
				// only notify if the streaming setting is set to true.
				// otherwise we do not want to be connected... unless we are
				// actually transferring old data...
				mNotification.setLatestEventInfo(this, "Bracelet battery",
						"Battery in bracelet is low", mPendingIntent);
				long[] vibrate = { 0, 200, 200, 200, 200, 200 };
				mNotification.vibrate = vibrate;
				mNotificationManager.notify(mNotificationID, mNotification);
			}
			break;
		}
		// if (mUIListener!=null) {
		// mUIListener.output(message);
		// } else {
		// }
	}

	@Override
	public void bluetoothDevice(BluetoothDevice device) {
		// if (mUIListener!=null) {
		// mUIListener.bluetoothDevice(device);
		// }
	}

	private void output(String msg) {
		// if (mUIListener!=null) {
		// mUIListener.output(msg);
		// }
		Log.d(TAG, "output: " + msg);
	}

	private String mError = "";

	private void setError(String msg) {
		Log.d(TAG, "SetError: " + msg);
		mError = msg;
	}

	public boolean isConnected() {
		return mBluetoothClient.isConnected();
	}

	public boolean isSearching() {
		return mIsSearching;
	}

	@Override
	public void bluetoothDeviceConnected(BluetoothDevice device) {
		Log.d(TAG, "Connected");
		mConnectedDevice = device;
		mDeviceName = device.getName();
		mIsSearching = false;
		mUnableToConnectCounter = 0;
		mGettingRealtimeData = false;
	}

	@Override
	public void bluetoothDeviceNotFound() {
		setError("Device not found");
		mIsSearching = false;
	}

	private int mUnableToConnectCounter = 0;

	@Override
	public void bluetoothUnableToConnect() {
		// Log.d(TAG, "Unable to connect");
		setError("Unable to connect to device");
		mUnableToConnectCounter++;
		mIsSearching = false;
		if (mUnableToConnectCounter <= 3) {
			Log.d(TAG, "Retrying to connect");
			scanAndConnect(mStream);
		} else {
		}
	}

	@Override
	public void bluetoothRealtimeDataStarted() {
		Log.d(TAG, "We are getting streamed data");
		if (!mStream) {
			mBluetoothClient.stopAndDisconnect();
		} else {
			mGettingRealtimeData = true;
		}
	}

	private Timer mStopStreamTimer = null;

	public void setStream(boolean value) {
		mStream = value;
		if (mStopStreamTimer != null) {
			mStopStreamTimer.cancel();
			mStopStreamTimer = null;
		}
		if (value == false && mBluetoothClient.isConnected()
				&& mGettingRealtimeData) {
			mStopStreamTimer = new Timer();
			mStopStreamTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					mBluetoothClient.stopAndDisconnect();
					mGettingRealtimeData = false;
				}
			}, 1000 * 30); // disconnect after 30 seconds...
		}
	}

	public BluetoothDevice getConnectedDevice() {
		return mConnectedDevice;
	}

	public boolean isUploading() {
		return mDataUploader != null;
	}

	public boolean isBatteryLow() {
		return mBatteryWarning;
	}

	public void startDataUpload(String username) {
		if (mDataUploader != null) {
			mDataUploader.stop();
			return;
		}

		output("Starting upload");

		mDataUploader = new DataUploader(this,
				AHState.getInstance().mDatabaseTable, true, username);
		mDataUploader.start(new DataUploader.Listener() {

			@Override
			public void uploaderStopped(String reason) {
				mDataUploader = null;
				output("Data upload stopped");
			}

			@Override
			public void uploaderIssue(String msg) {
				/*
				 * if (mUIListener!=null) { output(msg); } else {
				 * mNotification.setLatestEventInfo(HRVService.this,
				 * "Uploading Issue", msg, mPendingIntent); }
				 */
			}
		});
	}

	public String getUploadStatusString() {
		if (!isUploading()) {
			return "Not uploading";
		}
		return mDataUploader.getStatusString();
	}
	public int getStatusProgress(){
		return mDataUploader.getProgress();
	}
	public int getMStatus(){
		return mDataUploader.getMStatus();
	}
	
	public void clearAllData(String confirm) {
		if (confirm!=null && confirm.compareTo("yes")==0) {
			AHState.getInstance().mDatabaseTable.deleteAll();
		}
	}
}
