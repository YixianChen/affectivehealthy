package se.sics.ah3;

import java.util.Date;

import se.sics.ah3.graphics.CoreUtil;
import se.sics.ah3.service.AHService;
import se.sics.ah3.settings.AHSettings;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class DialogActivity extends Activity {
	
	private static final int BT_SEARCHING_DIALOG = 0;
	private static final int BT_DOWNLOADING_DIALOG = 1;
	private static final int BT_ERROR_DIALOG = 2;
	private static final int BT_STATUS_DIALOG = 3;
	private static final int TAG_STORE_DIALOG = 4;
	private static final int BT_OFF_DIALOG = 5;
	private static final int BT_NO_DEVICE_SET_DIALOG = 6;
	
	
	public static final int SHOW_BLUETOOTH_DIALOG = 0;
	public static final int SHOW_STORE_TAG_DIALOG = 1;

	private ProgressDialog mProgressDialog = null;
	
	protected Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == SHOW_BLUETOOTH_DIALOG) {
				displayBluetoothDialog();
			}
			else if(msg.what == SHOW_STORE_TAG_DIALOG) {
				displayStoreTagDialog();
			}
		}
	};
	
	public Handler getHandler() {
		return mHandler;
	}
	
	private void displayStoreTagDialog() {
		showDialog(TAG_STORE_DIALOG);
	}
	
	private void displayBluetoothDialog() {		
		// add logic for checking bt status and then pick dialog
		switch(AHState.getInstance().mService.getStatus()) {
		case CONNECTING:
			showDialog(BT_SEARCHING_DIALOG);
			break;
		case ERROR:
			showDialog(BT_ERROR_DIALOG);
			break;
		case DOWNLOADING:
			showDialog(BT_DOWNLOADING_DIALOG);
			break;
		case BLUETOOTH_OFF:
			showDialog(BT_OFF_DIALOG);
			break;
		case NO_DEVICE_SET:
			showDialog(BT_NO_DEVICE_SET_DIALOG);
			break;
		default:
			showDialog(BT_STATUS_DIALOG);
			break;
		}
	}
	
	@Override
	public Dialog onCreateDialog(int which) {
		switch (which) {
		case BT_SEARCHING_DIALOG:
			return createSearchingDialog();
		case BT_DOWNLOADING_DIALOG:
			return createDownloadingDialog();
		case BT_ERROR_DIALOG:
			return createErrorDialog();
		case BT_STATUS_DIALOG:
			return createStatusDialog();
		case TAG_STORE_DIALOG:
			return createStoreTagDialog();
		case BT_OFF_DIALOG:
			return createBluetoothOffDialog();
		case BT_NO_DEVICE_SET_DIALOG:
			return createBluetoothNoDeviceSetDialog();
		default:
			break;
		}
		return null;
	}
	
	private Dialog createStoreTagDialog() {
		final View dialogView = getLayoutInflater().inflate(R.layout.store_tag_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
			.setTitle("Store a new tag")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					EditText tagText = (EditText) dialogView.findViewById(R.id.tagText);
					String tag = tagText.getText().toString();
					Log.e("tag", tag);
					AHState.getInstance().mUserTags.addTag(tag);
					dialog.dismiss();
				}
			})
			.setNegativeButton("Cancel",  new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			}).create();
		dialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface onShowDialog) {
				EditText tagText = (EditText) dialogView.findViewById(R.id.tagText);
				tagText.setText("");
			}
		});
		return dialog;
	}
	
	private Dialog createSearchingDialog() {
		final View dialogView = getLayoutInflater().inflate(R.layout.search_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
			.setTitle("Searching for bluetooth device")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			}).create();
		return dialog;
	}	

	private Runnable updateDownloadingMessage = new Runnable() {
	    @Override
	    public void run() {
	        //Log.v(TAG, strCharacters);
	    	if(mProgressDialog != null) {
	    		mProgressDialog.setMessage(AHState.getInstance().mService.getStatusString());	    		
	    	}
	    }
	};	
	
	private Dialog createDownloadingDialog() {
		final ProgressDialog dialog;
		dialog = new ProgressDialog(this);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMessage("Loading data");
		dialog.setCancelable(true);
		
		dialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface onShowDialog) {
			    DialogActivity.this.runOnUiThread(new Runnable() {
			    	private boolean bFirstRun = true;
			        public void run() {
			        	if (AHState.getInstance().mService.getStatus() != AHService.BTStatus.DOWNLOADING) {
			        		dialog.dismiss();
			        		return;
			        	}
			        	if(bFirstRun || dialog.isShowing()) {
			        		dialog.setMessage("Downloading data from " + CoreUtil.getFormattedDate("yyyy-MM-dd HH:mm:ss", new Date(AHState.getInstance().mRTtime)));
			        		mHandler.postDelayed(this, 100);

			        		if (dialog.isShowing()) bFirstRun = false;
			        	}
			        }
			    });
			}
		});

		return dialog;
	}
	
	private Dialog createErrorDialog() {
		final View dialogView = getLayoutInflater().inflate(R.layout.error_dialog, null);
		final TextView errorTextView = (TextView)dialogView.findViewById(R.id.errorTextView);
		errorTextView.setText(AHState.getInstance().mService.getStatusString());
		
		AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
			.setTitle("Bluetooth error")
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			})
			.setPositiveButton("Reconnect", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					AHState.getInstance().mService.scanAndConnect(true);
				}
			}).create();
		dialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface onShowDialog) {
			    DialogActivity.this.runOnUiThread(new Runnable() {
			        public void run() {
			        	errorTextView.setText(AHState.getInstance().mService.getStatusString());
		        	}
			    });
			}
		});
		return dialog;
	}
	
	private Dialog createStatusDialog() {
		final View dialogView = getLayoutInflater().inflate(R.layout.status_dialog, null);
		TextView statusTextView = (TextView)dialogView.findViewById(R.id.statusTextView);
		statusTextView.setText("Connected to " + AHState.getInstance().mService.getConnectedDevice().getName());
		
		AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
			.setTitle("Bluetooth status")
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			}).create();
		return dialog;
	}
	
	private Dialog createBluetoothOffDialog() {
		final View dialogView = getLayoutInflater().inflate(R.layout.error_dialog, null);
		TextView errorTextView = (TextView)dialogView.findViewById(R.id.errorTextView);
		errorTextView.setText("Bluetooth is turned off, do you want to go to settings and turn it on?");
		
		AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
			.setTitle("Bluetooth error")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				    Intent intentBluetooth = new Intent();
				    intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
				    startActivity(intentBluetooth);     
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			}).create();
		return dialog;
	}
	
	private Dialog createBluetoothNoDeviceSetDialog() {
		final View dialogView = getLayoutInflater().inflate(R.layout.no_device_set_dialog, null);
		TextView errorTextView = (TextView)dialogView.findViewById(R.id.errorTextView);
		errorTextView.setText("You have yet to select a bracelet to connect to, do you want to do it now?");
		
		AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
			.setTitle("Bluetooth error")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
					startActivityForResult(new Intent((AffectiveHealthActivity)DialogActivity.this, AHSettings.class), 0);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			}).create();
		return dialog;
	}	
}
