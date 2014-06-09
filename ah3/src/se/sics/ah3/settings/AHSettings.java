package se.sics.ah3.settings;

import java.util.Date;
import java.util.Vector;

import se.sics.ah3.AHState;
import se.sics.ah3.BluetoothClient;
import se.sics.ah3.DialogActivity;
import se.sics.ah3.PromptDialog;
import se.sics.ah3.BluetoothClient.Status;
import se.sics.ah3.graphics.CoreUtil;
import se.sics.ah3.service.AHService;
import se.sics.ah3.share.LoginActivity;
import se.sics.ah3.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnShowListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class AHSettings extends Activity implements BluetoothClient.BluetoothListener {
	public final static String PREFS_NAME = "AffectiveHealth";
	public final static String PREF_PREFERRED_DEVICE_ADDR = "preferreddevice";
	public final static String PREF_PREFERRED_DEVICE_NAME = "preferreddevice_name";
	public final static String PREF_CONNECT_INTERVAL = "connectinterval";
	public final static String PREF_USERNAME = "username";

	private BluetoothClient mBluetoothClient;
	private BluetoothDevice mPreferredDevice = null;
	
	private TextView mDeviceName;
	private TextView mConnectInterval;
	
	private TextView mStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		mBluetoothClient = new BluetoothClient(this);
		mBluetoothClient.setListener(this);

		// load current settings
		SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);
//		String preferreddevice = prefs.getString(PREF_PREFERRED_DEVICE_ADDR, "");
		String preferreddevice_name = prefs.getString(PREF_PREFERRED_DEVICE_NAME, "");
		int interval = prefs.getInt(PREF_CONNECT_INTERVAL, 30);
		
		mDeviceName = (TextView)findViewById(R.id.device_name);
		mConnectInterval = (TextView)findViewById(R.id.connect_interval);
		
		mStatus = (TextView)findViewById(R.id.settings_status);

		mDeviceName.setText(preferreddevice_name);
		mConnectInterval.setText(""+interval);

		Button scan = (Button)findViewById(R.id.scan_bt);
		scan.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBluetoothClient.startSearch();
			}
		});
		Button ok = (Button)findViewById(R.id.settings_ok);
		ok.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// save
				if (mPreferredDevice!=null) {
					mBluetoothClient.setPreferredDevice(mPreferredDevice);
				}

				SharedPreferences prefs = AHSettings.this.getSharedPreferences(PREFS_NAME, 0);
				Editor edit = prefs.edit();
//				edit.putString("preferreddevice", (String)mDeviceName.getText());
				edit.putInt(AHSettings.PREF_CONNECT_INTERVAL, Integer.parseInt(mConnectInterval.getText().toString()));
				edit.commit();

				// end
				setResult(RESULT_OK);
				finish();
			}
		});
		
//		if (AHState.getInstance().mService!=null && AHState.getInstance().mService.isUploading()) {
//			new Thread(new Runnable() {
//				
//				@Override
//				public void run() {
//					while(AHState.getInstance().mService.isUploading()) {
//						mStatus.post(new Runnable() {
//							
//							@Override
//							public void run() {
//								mStatus.setText(AHState.getInstance().mService.getUploadStatusString());
//							}
//						});
//						try {
//							Thread.sleep(1000);
//						} catch (InterruptedException ie) {
//							//
//						}
//					}
//				}
//			}).start();
//		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		setResult(RESULT_CANCELED);
		finishActivity(0);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// to remove broadcast receivers that might be put in place for searching for devices.
		mBluetoothClient.stopAndDisconnect();
	}

	@Override
	public void bluetoothStatus(Status status, String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bluetoothDevices(Vector<BluetoothDevice> dtiDevices) {
		// TODO Auto-generated method stub
		if (mDevicesDialog!=null)
			mDevicesDialog.setTitle("Done");
	}

	private AlertDialog mDevicesDialog = null;
	private ArrayAdapter<String> mDevicesAdapter = null;

	@Override
	public void bluetoothDevice(BluetoothDevice device) {
		if (mDevicesDialog==null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(AHSettings.this);
			builder.setTitle("Searching...");
			builder.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
//					 makeToastInUI("No device selected");
			         mDevicesDialog = null;
				}
			});
			mDevicesAdapter = new ArrayAdapter<String>(this, R.layout.listitem);
			builder.setAdapter(mDevicesAdapter, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			         dialog.dismiss();
//			         mBluetoothClient.chooseDevice(item);
			         BluetoothDevice dev = mBluetoothClient.getDevice(item);
			         mDeviceName.setText(dev.getName());
			         mPreferredDevice = dev;
			         mDevicesDialog = null;
			    }
			});

			mDevicesDialog = builder.create();
			mDevicesDialog.show();
		}

		mDevicesAdapter.add(device.getName());
	}

	@Override
	public void bluetoothDeviceConnected(BluetoothDevice device) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bluetoothDeviceNotFound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bluetoothUnableToConnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bluetoothRealtimeDataStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.settings_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection

	    try {
			switch (item.getItemId()) {
			case R.id.settings_clear:
				PromptDialog confirm = new PromptDialog(this, "Clear all data?", "Confirm by typing 'yes'", "no") {
					
					@Override
					public boolean onOkClicked(String input) {
						if (input.compareTo("yes")==0) {
							// do the upload as the user.
							AHState.getInstance().mService.clearAllData("yes");
						}
						return false;
					}
				};
				confirm.show();
				break;
			case R.id.settings_reprocess:
				AHState.getInstance().reprocessGSRFilter();
				return true;
			case R.id.settings_upload:
			
				Intent intent = new Intent(this, LoginActivity.class); 
				Bundle bl = new Bundle();
				bl.putBoolean("upload", true);
				intent.putExtras(bl);
			    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
			    startActivity(intent);
			   
//				PromptDialog prompt = new PromptDialog(this, "Username", "Enter username", "") {
//					
//					@Override
//					public boolean onOkClicked(String input) {
//						if (input.compareTo("")!=0) {
//							// do the upload as the user.
//							AHState.getInstance().mService.startDataUpload(input);
//
//							new Thread(new Runnable() {
//
//								@Override
//								public void run() {
//									while(AHState.getInstance().mService.isUploading()) {
//										mStatus.post(new Runnable() {
//
//											@Override
//											public void run() {
//												mStatus.setText(AHState.getInstance().mService.getUploadStatusString());
//											}
//										});
//										try {
//											Thread.sleep(1000);
//										} catch (InterruptedException ie) {
//											//
//										}
//									}
//								}
//							}).start();
//						}
//						return false;
//					}
//				};
//
//				prompt.show();
				break;
			}
	    } catch (Exception e) {
	    	
	    }
	    return true;
	}
}
