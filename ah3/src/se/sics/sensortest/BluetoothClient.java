package se.sics.sensortest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothClient {
	private static final String TAG = "BLUETOOTH";
	
	public static final int BT_STATUS = 1;
	public static final int BT_STOPPEDLISTENING = 3;

    // Name for the SDP record when creating server socket
//    private static final String NAME_SECURE = "BluetoothChatSecure";
//    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
//    private static final UUID MY_UUID_SECURE =
//        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public interface ClientHandler {
    	void clientThread(InputStream is, OutputStream os);
    	void clientStatus(String status);
    }
    public interface SearchCallback {
		void searchDone(Vector<BluetoothDevice> devices);
		void searchFound(BluetoothDevice dev);
	}
	private Context mContext;
//	private Handler mHandler;
	private SearchCallback mCallback = null;

	public BluetoothAdapter mAdapter;

	public BluetoothClient(Context ctx)
	{
		mContext = ctx;
//		mHandler = handler;
		mAdapter = BluetoothAdapter.getDefaultAdapter();

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        ctx.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        ctx.registerReceiver(mReceiver, filter);
	}

	private ClientHandler mClientHandler = null;
	private ClientConnection mClientConnection = null;
	public ClientConnection connect(BluetoothDevice device, ClientHandler handler)
	{
		mClientHandler = handler;
		mClientConnection = new ClientConnection(device);
		mClientConnection.start();
		return mClientConnection;
	}

	public void disconnect()
	{
		if (mClientConnection!=null)
		{
			mClientConnection.disconnect();
			mClientConnection = null;
		}
	}
	
	public void close() {
		disconnect();
		mContext.unregisterReceiver(mReceiver);
	}
	
	public boolean isConnected() {
		return mClientConnection!=null && mClientConnection.isConnected();
	}
	
	public enum Command { STOP, START, STATUS };
	
	public class ClientConnection extends Thread {
		private BluetoothDevice mDevice;
		private BluetoothSocket mClientSocket;
//		private InputStream mInputStream;
//		private OutputStream mOutputStream;
//		private boolean mRun = true;
		private boolean mIsConnected = false;

		public ClientConnection(BluetoothDevice device) {
			mDevice = device;
		}
		public void run() {
			try {
				BluetoothSocket socket = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
				
				socket.connect();
				mIsConnected = true;
				mClientSocket = socket;
				
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
//				mInputStream = is;
//				mOutputStream = os;

				mClientHandler.clientThread(is, os);

				socket.close();
			} catch (IOException ioe) {
				Log.d(TAG, "Exception while communicating: " + ioe.getMessage());
			}
			mIsConnected = false;
			mClientHandler.clientStatus("Disconnected");
//			mSocket = null;
		}
		
		public boolean isConnected() {
			return mIsConnected;
		}
		
		public void disconnect()
		{
			try {
				if (mClientSocket!=null)
					mClientSocket.close();
				mClientSocket = null;
			} catch (IOException ioe) {
				//done
			}
//			mRun = false;
//			try {
//				if (mSocket!=null)
//					mSocket.close();
//				mSocket = null;
//			} catch (IOException ioe) {
//				Log.d(TAG, "Disconnect exception: " + ioe.getMessage());
//			}
		}
		
/*		public void sendCmd(String msgText)
		{
			BTMessage msg = new BTMessage(msgText);
			try {
				msg.send(mOutputStream);
			} catch (IOException ioe) {
				Log.d(TAG, "Exception: " + ioe.getMessage());
			}
		}
		public void sendCmd(Command cmd)
		{
			BTMessage msg;
			switch(cmd)
			{
			case START:
				msg = new BTMessage("start");
				break;
			case STOP:
				msg = new BTMessage("stop");
				break;
			case STATUS:
				msg = new BTMessage("status");
				break;
			default:
				msg = new BTMessage("hello");
				break;
			}
			try {
				msg.send(mOutputStream);
			} catch (IOException ioe) {
				Log.d(TAG, "Exception: " + ioe.getMessage());
			}
		}*/
	}
	
//	private void sendUIMessage(String msg, String devicename)
//	{
//		Message uimsg = mHandler.obtainMessage(Bluetooth.BT_STATUS);
//		Bundle bundle = new Bundle();
//		bundle.putString("DEVICENAME", devicename);
//		bundle.putString("MESSAGE", "UI " + msg);
//		uimsg.setData(bundle);
//		mHandler.sendMessage(uimsg);
//		Log.d(TAG, "sent msg to handler: " + msg);
//	}

	public void startGetDevices(SearchCallback cb)
	{
		mReceiver.mFoundDevices.clear();
		mCallback = cb;
		if (mAdapter.isDiscovering())
		{
			mAdapter.cancelDiscovery();
		}
		for (BluetoothDevice dev : mAdapter.getBondedDevices()) {
			mReceiver.mFoundDevices.add(dev);
		}
		mAdapter.startDiscovery();
	}
	
	public Vector<BluetoothDevice> getDevices() {
		return mReceiver.mFoundDevices;
	}

	static protected class BTMessage {
		private int mType = 0;	// 0 is text msg
		private String mMsg = "";
		private final byte[] mBytes;
		BTMessage(String msg) {
			mMsg = msg;
			mBytes = null;
		}
		BTMessage(final byte[] data) {
			mBytes = data;
		}
		public static BTMessage read(InputStream is) throws IOException {
			int length = 0;
			for (int i=0;i<4;i++) { int c = is.read(); if (c<0) return null; length |= c<<(i*8); }
			byte[] b = new byte[length];
			int n=0;
			while(n<length) {
				int bytesRead = is.read(b, n, length-n);
				if (bytesRead<0) return null;
				n += bytesRead;
			}
			Log.d(TAG, "Read BTMessage (" + length + ")");
			return new BTMessage(b);
		}
		public void send(OutputStream os) throws IOException {
			if (mBytes!=null) sendBinary(os, mBytes);
			sendString(os);
		}
		public void sendString(OutputStream os) throws IOException {
			sendBinary(os, mMsg.getBytes());
		}
		private void sendBinary(OutputStream os, final byte[] bytes) throws IOException {
			int length = bytes.length;
			for (int i=0;i<4;i++)
				os.write((length>>(i*8))&0xff);
			os.write(bytes);
		}
		public String getMessage() { return mBytes!=null?new String(mBytes):mMsg; }
		public byte[] getBytes() { return mBytes!=null?mBytes:mMsg.getBytes(); }
	}

	// The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final MyBroadcastReceiver mReceiver = new MyBroadcastReceiver();

    private class MyBroadcastReceiver extends BroadcastReceiver {
    	public Vector<BluetoothDevice> mFoundDevices = new Vector<BluetoothDevice>();
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	mCallback.searchFound(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            	mCallback.searchDone(mFoundDevices);
            }
        }
    };
}
