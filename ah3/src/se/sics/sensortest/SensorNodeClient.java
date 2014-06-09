package se.sics.sensortest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import se.sics.ah3.R;
import se.sics.sensortest.BluetoothClient.BTMessage;
import se.sics.sensortest.BluetoothClient.ClientHandler;
import se.sics.sensortest.BluetoothClient.SearchCallback;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

public class SensorNodeClient {
	public class Data {
		String data;
	}
	public interface DataListener {
		void newData(Data data);
		void dataAcc(long time, float x, float y, float z);
		void dataLoc(long time, float lon, float lat);
		void dataFake(long time, int gsr, int x, int y, int z);
		void dataSensorReady( boolean ready, String sensors );
	}

	private Context mContext;
	private DataListener mListener;

	private BluetoothClient mBT;

	public SensorNodeClient(Context context) {
		mContext = context;
		mBT = new BluetoothClient(mContext);
	}
	
	public void connect(BluetoothDevice device, DataListener listener) {
		mListener = listener;
    	mBT.connect(device, mClientHandler);
	}
	
	public void disconnect() {
		mBT.disconnect();
	}
	
	public void close() {
		mBT.close();
	}
	
	public boolean start() {
		if (mBTChannel!=null) {
			try {
				BTMessage msg = new BTMessage("START binary 2");
				msg.send(mBTChannel);
			} catch (IOException ioe) {
				Log.d("Client", "IOE sending msg");
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean mRun = true;
	private OutputStream mBTChannel = null;
	private BluetoothClient.ClientHandler mClientHandler = new ClientHandler() {
		
		@Override
		public void clientThread(InputStream is, OutputStream os) {
			Log.d("", "Connected");
			mBTChannel = os;
			String sensors = "";
			try {
				BTMessage welcome = BTMessage.read(is);
				sensors = welcome.getMessage();
			} catch (IOException ioe) {
				Log.d("SensorNodeClient", "IOE receiving welcome message");
			}
			mListener.dataSensorReady( true, sensors );
			try {
				while (mRun) {
					BTMessage incoming = BTMessage.read(is);
					if (incoming==null) {
						Log.d("Client", "Disconnected");
						break;
					}

//					Data d = new Data();
//					d.data = incoming.getMessage();
//					handleDataString(d.data);
//					handleDataString(incoming.getMessage());
					handleDataBinary(incoming.getBytes());
//					mListener.newData(d);
				}
			} catch (IOException ioe) {
				Log.d("Client", "Exception recieving msg");
			}
			mBTChannel = null;
			mListener.dataSensorReady( false, null );
		}
		
		@Override
		public void clientStatus(String status) {
//			addLine(status);
		}
	};
	
	private void handleDataString(String data) {
		String[] parts = data.split(" ");
		if (parts.length<0) return;

		if (parts[0].compareTo("acc")==0) {
			mListener.dataAcc(Long.parseLong(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]), Float.parseFloat(parts[4]));
		} else if (parts[0].compareTo("loc")==0) {
			mListener.dataLoc(Long.parseLong(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
		}
	}

	private void handleDataBinary(byte[] bytes) {
		Log.d("", "Handling binary data...");
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(bais);
		try {
			String type = dis.readUTF();
			if (type.compareTo("acc")==0) {
				mListener.dataAcc(dis.readLong(),dis.readFloat(),dis.readFloat(),dis.readFloat());
			} else if (type.compareTo("loc")==0) {
				mListener.dataLoc(dis.readLong(),dis.readFloat(),dis.readFloat());
			} else if (type.compareTo("Fake")==0) {
				mListener.dataFake(dis.readLong(),dis.readInt(),dis.readInt(),dis.readInt(),dis.readInt());
			}
		} catch (IOException ioe) {
			Log.d("SensorNodeClient", "IOE reading byte array");
		}
	}

	public void startGetDevices(SearchCallback cb) {
		mBT.startGetDevices(cb);
	}
	public Vector<BluetoothDevice> getDevices() {
		return mBT.getDevices();
	}

	public BluetoothAdapter getBluetoothAdapter() {
		// TODO Auto-generated method stub
		return mBT.mAdapter;
	}

	private class DeviceSelector implements SearchCallback {
		private ArrayAdapter<String> mAdapter;
		private AlertDialog mDialog;
		private Vector<BluetoothDevice> mDevices = new Vector<BluetoothDevice>();
		private Handler mHandler = new Handler();
		
		public DeviceSelector(Context ctx, final DataListener listener) {
			mAdapter = new ArrayAdapter<String>(ctx, R.layout.bluetoothlistentry);
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setTitle("Searching");
			builder.setAdapter(mAdapter, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	Log.d("", "CONNECTING TO DEVICE NOW!");
			         connect(mDevices.get(item),listener);
			         dialog.dismiss();
			    }
			});
			mDialog = builder.create();
			mDialog.show();
			for (BluetoothDevice dev: mBT.mAdapter.getBondedDevices()) {
				searchFound(dev);
			}
			mBT.startGetDevices(this);
		}

		@Override
		public void searchDone(Vector<BluetoothDevice> devices) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mDialog.setTitle("Select device");
				}
			});
		}

		@Override
		public void searchFound(final BluetoothDevice dev) {
			mDevices.add(dev);
			mHandler.post(new Runnable() {
				public void run() {
					mAdapter.add(dev.getName());
				}
			});
		}
	}

	public void queryAndConnect(DataListener listener) {
		new DeviceSelector(mContext, listener);
	}
}
