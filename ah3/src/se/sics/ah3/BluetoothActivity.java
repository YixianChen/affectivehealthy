package se.sics.ah3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;

import se.sics.ah3.dti.SendTimeMessage;
import se.sics.ah3.dti.Util;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BluetoothActivity extends ListActivity { //implements DtiBluetoothClient.Listener {
	private ArrayAdapter<String> mListAdapter;
	
	private BluetoothClient mBluetoothClient;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		mBluetoothClient = new BluetoothClient(this);

		// adapter
		mListAdapter = new ArrayAdapter<String>(this, R.layout.bluetoothlistentry);

		setContentView(R.layout.bluetoothlist);
		setListAdapter(mListAdapter);

//		AHState.getInstance().getDtiLauncher().getDtiClient().setListener(this);

		fillList();
	}

	private void fillList()
	{
		mListAdapter.add("Hej");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Scan");
		menu.add(0, 1, 0, "Stop");
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection

	    try {
			switch (item.getItemId()) {
			case 0:
				System.out.println("RSTART BT");
//				AHState.getInstance().getDtiLauncher().new RestartBluetooth().execute();
//				btDetectStart();
//				startSearchAndConnect();
				mBluetoothClient.startSearchAndConnect();
			    return true;
			case 1:
				System.out.println("Disconnect and stop");
//				stopAndDisconnect();
				mBluetoothClient.stopAndDisconnect();
				return true;
			default:
			    return super.onOptionsItemSelected(item);
			}
		} catch (Exception e) {
			Log.e("AH3", "Exception", e);
			return true;
		}
	}

//	@Override
//	public void btDetect(String name) {
//		mListAdapter.add(name);
//		setListAdapter(mListAdapter);
//	}
//
//	@Override
//	public void btDetectDone() {
//		mListAdapter.add("DONE");
//		setListAdapter(mListAdapter);
//	}
//
//	@Override
//	public void btDetectStart() {
//		mListAdapter.clear();
//		mListAdapter.add("Starting...");
//		setListAdapter(mListAdapter);
//	}
	
    private static final String TAG = "Bluetooth";
    
//    private void stopAndDisconnect()
//    {
//    	mKeepGettingData = false;
//    	try {
//    		mSocket.close();
//    	} catch(IOException ioe) {
//    		//
//    	}
//    }
//
//    private void startSearchAndConnect()
//	{
//		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//
//		if (mReceiver==null)
//		{
//            mReceiver = new MyBroadcastReceiver();
//            registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
//            registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
//		}
//		mReceiver.dtiDevice = null;
//		btAdapter.startDiscovery();
//	}
//    
//    private BluetoothSocket mSocket;
//    private ConnectionThread mThread;
//   
//    private void connectTo(BluetoothDevice dev)
//    {
//		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//
//		Log.v(TAG,"Connecting to "+dev.getName() + " " + dev.getAddress());
//		mListAdapter.add("Connecting");
//
//		BluetoothSocket socket = null;
//		try {
//			Method m = dev.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
//			socket = (BluetoothSocket) m.invoke(dev, 1); //Integer.valueOf(1));
//		} catch (NoSuchMethodException nsme) {
//			//
//		} catch( Exception e) {
//			//
//		}
//
//		if (socket == null)
//		{
//			Log.d(TAG, "No socket");
//		}
//		else
//		{
//			try {
//				socket.connect();
//				mSocket = socket;
//			} catch(Exception e)
//			{
//				Log.d(TAG, "Exception connecting to socket");
//				mSocket = null;
//			}
//		}
//		
//		if (mSocket!=null)
//		{
//			mThread = new ConnectionThread();
//			mThread.start();
//		}
//		else
//		{
//			Log.d(TAG, "No socket open");
//		}
//    }
//    
//	private static void sendCmdStart(OutputStream os, int serial) throws IOException
//	{
//		Date now = new Date();
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		baos.write(0xd0);
//		baos.write(0x01);
//		baos.write(0x12); baos.write(0x00);
//		baos.write(0x01); baos.write(0x00);
//		baos.write(serial&0xff); baos.write((serial>>8)&0xff);
//		baos.write(12); baos.write(0x02); baos.write(27); // date
//		baos.write(0x0);
//		baos.write(now.getHours());
//			baos.write(now.getMinutes());
//			baos.write(now.getSeconds());
//			baos.write((int)(now.getTime()%1000)/10);
//		byte[] b = baos.toByteArray();
//		int crc = Util.CRC16_CCITT(b);
//		baos.write(crc&0xff); baos.write((crc>>8)&0xff);
//		os.write(baos.toByteArray());
//	}
//	
//	static class DataMessage {
//		public enum DataFields {
//			date,
//			time,
//			event,
//			vccBattery,
//			dummy,
//			skinTemperature,
//			skinConductance,
//			accX,
//			accY,
//			accZ,
//			envTemperature,  
//			envLight		
//		}
//		public static void write(int[] ints, int[] format, OutputStream os ) throws IOException{
//	         for (int i = 0; i < ints.length; i++) {
//	        	 for (int k =0; k < format[i]; k++) {
//	         		int b=((ints[i]>>k*8) & 0xFF);
//	         		os.write(b);
//	         	}
//	         }
//		}
//
//		public static boolean send(OutputStream os, int cmd, int[] format, int[] data) throws IOException
//		{
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			// write cmd
//			baos.write(cmd);
//			// write data
//			write(data,format, baos);
//			// compute crc
//			byte[] b = baos.toByteArray();
//			int crc = Util.CRC16_CCITT(b);
//			// write crc to payload
//			baos.write(crc&0xff); baos.write((crc>>8)&0xff);
//			// send data
//			os.write(baos.toByteArray());
//			return true;
//		}
//		
//		public static int[] recv(InputStream is, int[] format) throws IOException
//		{
//			int[] data = Util.read(is, format, null);
//			return data;
//		}
//	}
//
//	private boolean mKeepGettingData = true;
//	class ConnectionThread extends Thread {
//    	public void run()
//    	{
//    		int msgCount = 0;
//    		boolean timeSent = false;
//    		boolean sendAndWait = false;
//    		int serial = 0;
//
//    		Log.d(TAG, "Starting communication");
//			try {
//				InputStream is = mSocket.getInputStream();
//				OutputStream os = mSocket.getOutputStream();
//				
//				mKeepGettingData = true;
//				while(mKeepGettingData)
//				{
//					// get cmd header (decides which cmd it is)
//					Log.d(TAG, "Waiting for command");
//					int dataByte = is.read();
//					if (dataByte==-1)
//					{
//						throw new MyException("Socket closed");
//					}
//					int cmd = dataByte;
//					switch(cmd)
//					{
//					case 0xb0: // data message
//						Log.d(TAG, "RECIEVED CMD: Low battery!");
////						byte[] batteryData = new byte[19];
////						is.read(batteryData);	// just read the rest
//						int[] batteryData = DataMessage.recv(is, new int[] {1,2,2,2,3,1,4,2,2});
//						break;
//					case 0xe9: // data message
////						Log.d(TAG, "RECIEVED CMD: Data!");
////						byte[] dataData = new byte[33];
////						is.read(dataData);	// just read the rest
////						int[] dataData = DataMessage.recv(is, new int[] {1,2,2,2,3,4,1,1,1,2,2,2,2,2,2,2,2});
////						Log.d(TAG, "time: "+dataData[5]);
//						se.sics.ah3.dti.DataMessage dataMessage = new se.sics.ah3.dti.DataMessage();
//						dataMessage.read(is);
//						{
//							int gsr = dataMessage.getField(se.sics.ah3.dti.DataMessage.Field.skinConductance);
//							int ax = dataMessage.getField(se.sics.ah3.dti.DataMessage.Field.accX);
//							int ay = dataMessage.getField(se.sics.ah3.dti.DataMessage.Field.accY);
//							int az = dataMessage.getField(se.sics.ah3.dti.DataMessage.Field.accZ);
//							Log.d(TAG, "Data: " + gsr + ", " + ax + "," + ay + "," + az);
//						}
//						break;
//					case 0xea: // ask for time
//						Log.d(TAG, "RECIEVED CMD: Ask for timestamp");
////						byte[] askData = new byte[13]; 
////						is.read(askData);	// just read the rest
//						int[] askData = DataMessage.recv(is, new int[] {1,2,2,2,1,1,1,1,2});
////						serial = ((int)askData[5]) + ((int)askData[6]<<8);
//						serial = askData[3];
//						Log.d(TAG, "Serial: " + serial + " (len: " + askData[1] + ")");
//
//						SendTimeMessage sendTimeMessage = new SendTimeMessage(serial, ++msgCount);
//						sendTimeMessage.write(os);
//						// crc not included in format, but sent.
////						DataMessage.send(os, 0xae, new int[] {1,2,2,2,3,1,4}, new int[] {1,18,++msgCount, serial, 0,0,0});
//
//						sendCmdStart(os, serial);
//
////						timeSent = true;
//						break;
//					default:
//						Log.d(TAG, "Unknown cmd: " + cmd);
//					}
//
///*					if (timeSent && !sendAndWait)
//					{
//						Log.d(TAG, "Sending cmd to start and wait");
//						sendCmdStart(os, serial);
//						sendAndWait = true;
//					}*/
//				}
//			} catch (IOException ioe) {
//				Log.d(TAG, "IOException: " + ioe.getMessage());
//			} catch (MyException mye) {
//				Log.d(TAG, "Exception: " + mye.getMessage());
//			}
//			finally {
//				Log.d(TAG, "Thread ended");
//				try {
//					mSocket.close();
//				} catch (IOException ioe) {
//					Log.d(TAG, "Error closing socket");
//				}
//			}
//    	}
//    }
//    
//    class MyException extends Exception {
//    	public MyException(String s)
//    	{
//    		super(s);
//    	}
//    }
//
//	class MyBroadcastReceiver extends BroadcastReceiver{
//		BluetoothDevice dtiDevice = null;
//		public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            // When discovery finds a device  
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                Log.v(TAG,device.getName() + "\n" + device.getAddress());
//                if(device!=null && device.getName()!=null && device.getName().startsWith("DTI2_")){
//                	Log.v(TAG,"FOUND");
//                	dtiDevice=device;
//                }
////            	detected(device.getName());
//                btDetect(device.getName());
//            }  else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//            	Log.v(TAG,"Finished discovery");
//            	btDetectDone();
//            	if(dtiDevice!=null){
//            		BluetoothActivity.this.runOnUiThread(new Runnable() {
//						
//						@Override
//						public void run() {
//							connectTo(dtiDevice);
//						}
//					});
///*            		try {
//						synchronized (DtiBluetoothClient.this) {
//							mSocket = initConnection(dtiDevice);
//							DtiBluetoothClient.this.notify();
//							Log.d(TAG, "Notified");
//						}
//					} catch (Exception e) {
//						e.printStackTrace(); 
//					}*/
//            	}            		
//            }
//        }
//	}
//
//	private MyBroadcastReceiver mReceiver = null;
}
