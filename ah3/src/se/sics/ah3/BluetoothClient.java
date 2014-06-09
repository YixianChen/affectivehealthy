package se.sics.ah3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

import se.sics.ah3.dti.DataMessage;
import se.sics.ah3.dti.DtiMessage;
import se.sics.ah3.dti.SendTimeMessage;
import se.sics.ah3.dti.Util;
import se.sics.ah3.dti.signal.AccSignalConsumer;
import se.sics.ah3.dti.signal.GsrSignalConsumer;
import se.sics.ah3.graphics.CoreUtil;
import se.sics.ah3.settings.AHSettings;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

public class BluetoothClient {
    private static final String TAG = "Bluetooth";
    
    private boolean mUsingBridge = false;

    public enum Status {
    	NONE,				// nothing is going on
    	SEARCHING,			// searching for devices
    	CONNECTING,			// connecting to device
    	CONNECTED,			// connected to device but waiting for command
    	SENDINGTIMESTAMP,	// the device is asking for timestamp and has not yet sent any data
    	RECEIVINGDATA,		// the device has started to send data!!!
    	BATTERY_LOW,		// low on battery
    	LOST_CONNECTION,
    	BLUETOOTH_OFF,
    	NO_DEVICE_SET
    }

//    private static final UUID MY_UUID_INSECURE =
//            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = 
    		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface BluetoothListener {
    	public void bluetoothStatus(Status status, String message);
		public void bluetoothDevices(Vector<BluetoothDevice> dtiDevices);
		public void bluetoothDevice(BluetoothDevice device);
		public void bluetoothDeviceConnected(BluetoothDevice device);
		public void bluetoothDeviceNotFound();
		public void bluetoothUnableToConnect();
		public void bluetoothRealtimeDataStarted();
//		public void bluetoothConnected();
    }
    
//    public interface ScanAndConnectResult {
//    	public void connected();
//    	public void deviceNotFound();
//    	public void unableToConnect();
//    }

    private Status mStatus = Status.NONE;
    private BluetoothListener mListener = null;

//    private float mInstantaneausDataRate = 0;

	private Context mContext;
	private ArrayList<SignalConsumer> mConsumers = new ArrayList<SignalConsumer>();
	
	private String mPreferredDevice = "";	// bluetooth address (mac address) of preferred device set by last successfully connected device.

    public BluetoothClient(Context context)
    {
    	mContext = context;
		AccSignalConsumer accConsumer = new AccSignalConsumer(AHState.getInstance().mAccColumn,
				new SignalConsumerListener() {
					AHState ah = AHState.getInstance();
					@Override
					public void signalConsumed(long time, float val) {
						ah.mRTtime = time;
						if (time>System.currentTimeMillis()-1000) {
							ah.mRTacc = val;
						} else {
							ah.mRTacc = Float.NaN;
						}
					}
				});
		GsrSignalConsumer gsrConsumer = new GsrSignalConsumer(AHState.getInstance().mGsrColumn,
				new SignalConsumerListener() {
					AHState ah = AHState.getInstance();
					@Override
					public void signalConsumed(long time, float val) {
						ah.mRTtime = time;
/*						if (time>System.currentTimeMillis()-2000) {
							ah.mRTgsr = val;
						} else {
							ah.mRTgsr = Float.NaN;
						}*/
					}
				});
//		BatterySignalConsumer batConsumer = new BatterySignalConsumer(handler);
		addConsumer(accConsumer);
		addConsumer(gsrConsumer);
//		dtiClient.addConsumer(batConsumer);

		getPreferredDevice();
    }
    
    public boolean hasPreferredDevice() {
    	String s = getPreferredDevice();
    	return s.compareTo("")!=0;
    }
    
    private String getPreferredDevice() {
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		mPreferredDevice = prefs.getString(AHSettings.PREF_PREFERRED_DEVICE_ADDR, "");
		
		return mPreferredDevice;
    }

    public void setPreferredDevice(BluetoothDevice device) {
    	mPreferredDevice = device.getAddress();
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(AHSettings.PREF_PREFERRED_DEVICE_ADDR, mPreferredDevice);
		editor.putString(AHSettings.PREF_PREFERRED_DEVICE_NAME, device.getName());
		editor.apply();
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter.isDiscovering())
			btAdapter.cancelDiscovery();
		Log.d(TAG, "Preferred device set: " + device.getName() + " (" + mPreferredDevice + ")");
    }
    
    private void unsetPreferredDevice() {
    	mPreferredDevice = "";
		SharedPreferences prefs = mContext.getSharedPreferences(AHSettings.PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(AHSettings.PREF_PREFERRED_DEVICE_ADDR);
		editor.remove(AHSettings.PREF_PREFERRED_DEVICE_NAME);
		editor.apply();
    }

    public void setListener(BluetoothListener listener)
    {
    	mListener = listener;
    }

    public void stopAndDisconnect()
    {
    	mKeepGettingData = false;
    	if (mSocket!=null)
    	{
	    	try {
	    		mSocket.close();
	    		mSocket = null;
	    	} catch(IOException ioe) {
	    		//
	    	}
    	}
    	if (mIsSearching)
    	{
    		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    		if (btAdapter.isDiscovering())
    			btAdapter.cancelDiscovery();
    		mIsSearching = false;
    	}
    	if (mReceiver!=null)
    	{
    		mContext.unregisterReceiver(mReceiver);
    		mReceiver = null;
    	}
    }
    
    private boolean mIsSearching = false;

    private void stopDiscovering() {
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
    }

    public boolean isConnected() {
    	return mSocket!=null;
    }

    private boolean mConnectOnDiscovery = false;

    public boolean startSearchAndConnect()
	{
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
				
		if (mSocket!=null || mIsSearching) { Log.d(TAG, "We might be connected, or we are searching still. Not trying to search or connect"); return false;	} // do not search if already connected or already searching...

		getPreferredDevice();	// sets mPreferredDevice...
		mConnectOnDiscovery = true;

		if (mReceiver==null)
		{
            mReceiver = new MyBroadcastReceiver();
            mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		}
		mReceiver.clearDevs();
/*		for (BluetoothDevice dev : btAdapter.getBondedDevices()) {
			mReceiver.dtiDevices.add(dev);
		}*/
		if(btAdapter.getState() == BluetoothAdapter.STATE_OFF) {
			status(Status.BLUETOOTH_OFF, "bluetooth is off");
			return true;
		}
		else if(!hasPreferredDevice()) {
			status(Status.NO_DEVICE_SET, "no preferred device set");
			return true;			
		}
		else {
			mIsSearching = true;
			status(Status.SEARCHING, "Starting to search for devices");
			btAdapter.startDiscovery();
			return true;			
		}
	}

    public void startSearch()
	{
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mSocket!=null || mIsSearching) return;	// do not search if already connected or already searching...

		getPreferredDevice();	// sets mPreferredDevice...
		mConnectOnDiscovery = false;

		if (mReceiver==null)
		{
            mReceiver = new MyBroadcastReceiver();
            mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		}
		mReceiver.clearDevs();
/*		for (BluetoothDevice dev : btAdapter.getBondedDevices()) {
			mReceiver.dtiDevices.add(dev);
		}*/
		mIsSearching = true;
		status(Status.SEARCHING, "Starting to search for devices");
		btAdapter.startDiscovery();
	}
    
	public void addConsumer(SignalConsumer consumer){		
		mConsumers.add(consumer);
	}

	private BluetoothSocket mSocket;
    private ConnectionThread mThread;

    private void connectTo(BluetoothDevice dev)
    {
		stopDiscovering();

		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

		Log.v(TAG,"Connecting to "+dev.getName() + " " + dev.getAddress());
		//mListAdapter.add("Connecting");
		status(Status.CONNECTING,"Connecting");

		BluetoothSocket socket = null;
		if (false && !mUsingBridge) {
			try {
				Method m = dev.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
				socket = (BluetoothSocket) m.invoke(dev, 1); //Integer.valueOf(1));
			} catch (NoSuchMethodException nsme) {
				//
			} catch( Exception e) {
				//
			}
		} else {
			try {
				socket = dev.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
			} catch (IOException ioe) {
				Log.d(TAG, "Unable to create insecure rfcomm socket");
			}
		}

		if (socket == null)
		{
			Log.d(TAG, "No socket");
			status(Status.NONE, "Unable to open socket");
		}
		else
		{
			try {
				socket.connect();
				mSocket = socket;
			} catch(Exception e)
			{
				Log.d(TAG, "Exception connecting to socket");
				mSocket = null;
				status(Status.NONE,"Unable to connect to socket");
				mListener.bluetoothUnableToConnect();

//				if (dev.getAddress().compareTo(mPreferredDevice)==0) {
//					unsetPreferredDevice();
//				}

			}
		}
		
		if (mSocket!=null)
		{
			status(Status.CONNECTED, "We are connected");
			mListener.bluetoothDeviceConnected(dev);
			mThread = new ConnectionThread();
			mThread.start();
			
			setPreferredDevice(dev);
		}
		else
		{
			Log.d(TAG, "No socket open");
		}
    }
    
	private static void sendCmdStart(OutputStream os, int serial, int msgCount) throws IOException
	{
		Calendar now = Calendar.getInstance();
/*		int date = build(new int[]{
				now.get(Calendar.DAY_OF_MONTH),
				now.get(Calendar.MONTH)+1,
				now.get(Calendar.YEAR)-2000
		});

//		now = Calendar.getInstance();
		int time = build(new int[]{
				(int)(now.getTimeInMillis()%1000)/10,
				now.get(Calendar.SECOND),
				now.get(Calendar.MINUTE),
				now.get(Calendar.HOUR_OF_DAY)*/

		int month = now.get(Calendar.MONTH)+1;
		int day = now.get(Calendar.DAY_OF_MONTH);
		int year = now.get(Calendar.YEAR)-2000;
		int hour = now.get(Calendar.HOUR_OF_DAY);
		int minute = now.get(Calendar.MINUTE);
		int second = now.get(Calendar.SECOND);
		int hundreds = (int)(now.getTimeInMillis()%1000)/10;

//		Date now = new Date();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(0xd0);
		baos.write(0x01);
		baos.write(0x12); baos.write(0x00);		// length
		baos.write(msgCount&0xff); baos.write((msgCount>>8)&0xff);		// msg count
		baos.write(serial&0xff); baos.write((serial>>8)&0xff);			// serial
//		baos.write(now.getYear()-100); baos.write(now.getMonth()+1); baos.write(now.getDate());	// date
		baos.write(year); baos.write(month); baos.write(day);	// date
		baos.write(0x0);	// dummy
//		baos.write(now.getHours());
//		baos.write(now.getMinutes());
//		baos.write(now.getSeconds());
//		baos.write((int)(now.getTime()%1000)/10);
		baos.write(hour);
		baos.write(minute);
		baos.write(second);
		baos.write(hundreds);
		byte[] b = baos.toByteArray();
		int crc = Util.CRC16_CCITT(b);
		baos.write(crc&0xff); baos.write((crc>>8)&0xff);
		os.write(baos.toByteArray());
		os.flush();
		DtiMessage.printBytes("DATA >>> ", baos.toByteArray());

		Log.d(TAG, "Start cmd sent");
	}
	
	static class BluetoothMessage {
		public static void write(int[] ints, int[] format, OutputStream os ) throws IOException{
	         for (int i = 0; i < ints.length; i++) {
	        	 for (int k =0; k < format[i]; k++) {
	         		int b=((ints[i]>>k*8) & 0xFF);
	         		os.write(b);
	         	}
	         }
		}

		public static boolean send(OutputStream os, int cmd, int[] format, int[] data) throws IOException
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// write cmd
			baos.write(cmd);
			// write data
			write(data,format, baos);
			// compute crc
			byte[] b = baos.toByteArray();
			int crc = Util.CRC16_CCITT(b);
			// write crc to payload
			baos.write(crc&0xff); baos.write((crc>>8)&0xff);
			// send data
			os.write(baos.toByteArray());
			os.flush();
			DtiMessage.printBytes("DATA >>> ", baos.toByteArray());
			return true;
		}
		
		public static int[] recv(InputStream is, int[] format) throws IOException
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int[] data = Util.read(is, format, baos);
			DtiMessage.printBytes("DATA <<< ", baos.toByteArray());
			return data;
		}
	}

	final int[] mAckFormat = new int[]{1,2,2,2};
	final int[] mAckData = new int[]{0x01,10,0/*msgcount*/, 0/*serial*/};
	private void sendAck(OutputStream os, int msgCount, int serial) throws IOException {
		mAckData[2] = msgCount; mAckData[3] = serial;
		BluetoothMessage.send(os, 0xa0, mAckFormat, mAckData);
	}

	private boolean mKeepGettingData = true;
	class ConnectionThread extends Thread {
    	public void run()
    	{
    		int msgCount = 0;
    		int serial = 0;
    		boolean startSent = false;

    		Log.d(TAG, "Starting communication");
			try {
				InputStream is = mSocket.getInputStream();
				OutputStream os = mSocket.getOutputStream();

				long lastDataTimestamp = 0;
				AHState ahstate = AHState.getInstance();

				boolean gettingRealtimeData = false;
				DataMessage dataMessage = new DataMessage();

				mKeepGettingData = true;
				while(mKeepGettingData)
				{
					// get cmd header (decides which cmd it is)
//					Log.d(TAG, "Waiting for command");
					int dataByte = is.read();
					if (dataByte==-1)
					{
						throw new MyException("Socket closed");
					}
					int cmd = dataByte;
					switch(cmd)
					{
					case 0xb0: // low battery warning
						Log.d(TAG, "RECIEVED CMD: Low battery!");
						mListener.bluetoothStatus(Status.BATTERY_LOW, "Battery Low");
						int[] batteryData = BluetoothMessage.recv(is, new int[] {1,2,2,2,3,1,4,2,2});
						// send MACK
//						BluetoothMessage.send(os, 0xa0, new int[]{1,2,2,2}, new int[]{0x01,10,++msgCount, serial});
						sendAck(os, ++msgCount, serial);
						break;
					case 0xe9: // data message
						if (mStatus!=Status.RECEIVINGDATA)
							status(Status.RECEIVINGDATA, "We are receiving data!");
						dataMessage.read(is);
						{
							long ts = System.currentTimeMillis(); //dataMessage.getTimestamp();
							ahstate.mDataRate = 1000/(float)(ts-lastDataTimestamp);
							lastDataTimestamp = ts;

//							long t1 = System.currentTimeMillis();
							handleDataMessage(dataMessage);
//							long t2 = System.currentTimeMillis();
//							Log.d("PROFILE", "PROFILE: " + (t2-t1) + " " + ahstate.mDataRate);
							
							if (System.currentTimeMillis()-dataMessage.getTimestamp()<2000 && !gettingRealtimeData) {
								gettingRealtimeData = true;
								mListener.bluetoothRealtimeDataStarted();
							}
						}
						// send MACK
						if (!mUsingBridge)
							sendAck(os, ++msgCount, serial);
						break;
					case 0xea: // ask for time
						Log.d(TAG, "RECIEVED CMD: Ask for timestamp");
						if (mStatus!=Status.SENDINGTIMESTAMP)
							status(Status.SENDINGTIMESTAMP, "Sending timestamp");
						int[] askData = BluetoothMessage.recv(is, new int[] {1,2,2,2,1,1,1,1,2});
						serial = askData[3];
						msgCount = askData[2];
						Log.d(TAG, "Serial: " + serial + " (len: " + askData[1] + ")");

						SendTimeMessage sendTimeMessage = new SendTimeMessage(serial, ++msgCount);
						sendTimeMessage.write(os);

						Log.d(TAG, "Timestamp sent.");
//						if (!startSent)
//						{
//							sendCmdStart(os, serial, ++msgCount);
//							startSent = true;
//						}

						// immediately start to receive data
//						sendCmdStart(os, serial, ++msgCount);
//						startSent = true;
						break;
					case 0xa0:	// mack
						// immediately start to receive data
//						Log.d(TAG, "RECIEVED CMD: Ack!");
						BluetoothMessage.recv(is, new int[] {1,2,2,2,2});
						if (!startSent)
						{
							sendCmdStart(os, serial, ++msgCount);
							startSent = true;
						}
						break;
					default:
						Log.d(TAG, "Unknown cmd: " + cmd);
					}
				}
			} catch (IOException ioe) {
				Log.d(TAG, "IOException: " + ioe.getMessage());
			} catch (MyException mye) {
				Log.d(TAG, "Exception: " + mye.getMessage());
			}
			finally {
				Log.d(TAG, "Thread ended");
				status(Status.LOST_CONNECTION, "Communication ended");
				try {
					if (mSocket!=null)
						mSocket.close();
				} catch (IOException ioe) {
					Log.d(TAG, "Error closing socket");
				}
				mSocket = null;
			}
    	}
    }
	
	private synchronized void status(Status status, String string)
	{
		mStatus = status;
		Log.d(TAG, "Status: " + string);
		if (mListener!=null)
			mListener.bluetoothStatus(status, string);
	}
	
	public synchronized Status getStatus()
	{
		return mStatus;
	}
	
	private static int[] v = new int[5]; // 5 interests at a time;
	private void handleDataMessage(DataMessage msg)
	{
//		long now = System.currentTimeMillis();
		
//		Calendar cal = Calendar.getInstance();
//		cal.set(2000+msg.getYear(), msg.getMonth()-1, msg.getDay(), msg.getHour(), msg.getMinutes(), msg.getSeconds());
//		cal.set(Calendar.MILLISECOND, msg.getHundreds()*10);

//		Date d = new Date();
//		d.setTime(System.currentTimeMillis());
//		Calendar cal2 = Calendar.getInstance();
//		Log.d("DATA", "Minute: " + msg.getMinutes() + " (" + cal2.get(Calendar.MINUTE) + ", " + d.getMinutes() + ")");

		long timestamp = msg.getTimestamp(); //cal.getTimeInMillis();
		
		Log.d(TAG, "Timestamp: " + CoreUtil.getFormattedDate("yyyy-MM-dd HH:mm:ss", new Date(timestamp)));

		DataMessage.Field[] fields = DataMessage.Field.values();
		for(SignalConsumer c : mConsumers) {
			int[] intr = c.getInterest();
//			int[] v = new int[intr.length];
			for (int i = 0; i < intr.length; i++) {
				v[i] = msg.getField(fields[intr[i]]);
			}
			c.consumeSignal(timestamp, v);
		}
	}
    
    class MyException extends Exception {
    	public MyException(String s)
    	{
    		super(s);
    	}
    }
    
    public BluetoothDevice getDevice(int item) {
    	if (mReceiver==null) return null;
    	return item<=mReceiver.dtiDevices.size()?mReceiver.dtiDevices.get(item):null;
    }

    public void chooseDevice(final int index) {
    	new Thread(new Runnable() {

			@Override
			public void run() {
		    	Log.d(TAG, "Choosed device: " + index + " (out of " + mReceiver.dtiDevices.size() + ")");
		    	if (mReceiver!=null && mReceiver.dtiDevices.size()>index) {
		    		BluetoothDevice dev = mReceiver.dtiDevices.get(index);
		    		mFirstDevice = dev.getAddress();
		    		connectTo(dev);
		    	}
			}
		}).start();
    }

    private String mFirstDevice = null;	// Address

	class MyBroadcastReceiver extends BroadcastReceiver {
//		BluetoothDevice dtiDevice = null;
		private Object lock = new Object();
		private Vector<BluetoothDevice> dtiDevices = new Vector<BluetoothDevice>();
		private boolean mConnect = false;

		public void clearDevs() {
			synchronized (lock) {
				dtiDevices.clear();
				mConnect = false;
			}
		}
		
		public void addDevice(BluetoothDevice dev) {
			synchronized (lock) {
				dtiDevices.add(dev);
			}
		}

		public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device  
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                Log.v(TAG,name + "\n" + device.getAddress());
                if(device!=null && name!=null && name.startsWith("DTI2_")){
                	Log.v(TAG,"FOUND");
                	if (mConnectOnDiscovery && device.getAddress().compareTo(mPreferredDevice)==0) {
                		stopDiscovering();
                		mIsSearching = false;
                		connectTo(device);
                		mConnect = true;
                	} else {
                    	addDevice(device);
                    	mListener.bluetoothDevice(device);
                	}
                }
            }  else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            	Log.v(TAG,"Finished discovery");
            	synchronized(lock) {
	            	if(dtiDevices.size()>0) {
	            		boolean found = false;
	            		if (!found && mListener!=null)
	            		{
	            			Log.d(TAG, "Devs: " + dtiDevices.size());
	            			mListener.bluetoothDevices(dtiDevices);
	            		}
	            	} else {
	            		// no device found
	            		if (!mConnect)
	            			status(Status.NONE, "No device found");
	            	}
            	}
            	
            	if (mConnectOnDiscovery&&!mConnect) {
            		mListener.bluetoothDeviceNotFound();
            	}
            	mIsSearching = false;
        		mContext.unregisterReceiver(mReceiver);
        		mReceiver = null;
            }
        }
	}

	private MyBroadcastReceiver mReceiver = null;
}
