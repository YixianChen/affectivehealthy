package se.sics.ah3.settings;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

import se.sics.ah3.AHState;
import se.sics.ah3.R;
import se.sics.ah3.share.LoginActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class UploadDataFragment extends Fragment {

	private TextView mStatus;
	private Button uploadButton;
	private String userName;
	private String userId;
	private String userName_Id="sss";
	private ProgressBar pb;
	private static final int REAUTH_ACTIVITY_CODE = 100;
	private static final int UPLOAD_DONE = 2;
	private ProfilePictureView profilePictureView;
	private TextView userNameView;

	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(final Session session, final SessionState state,
				final Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.transmission, container, false);
		uploadButton = (Button) view.findViewById(R.id.btnUpload);
		// pb = (ProgressBar)view.findViewById(R.id.progressbar);
		mStatus = (TextView) view.findViewById(R.id.tv);
		profilePictureView = (ProfilePictureView) view.findViewById(R.id.upload_profile_pic);
		profilePictureView.setCropped(true);

		// Find the user's name view
		userNameView = (TextView) view.findViewById(R.id.upload_user_name);
		Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			// Get the user's data
			makeMeRequest(session);
		}
		uploadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Do nothing for now
				// do the upload as the user.
				System.out.println(userName_Id);
				AHState.getInstance().mService.startDataUpload(userName_Id);

				new Thread(new Runnable() {

					@Override
					public void run() {
						while (AHState.getInstance().mService.isUploading()) {
							mStatus.post(new Runnable() {

								@Override
								public void run() {
									mStatus.setText(AHState.getInstance().mService
											.getUploadStatusString());
								}
							});
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ie) {
								//
							}
						}
						//Intent intent = new Intent(getActivity(), AHSettings.class);
						//intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						//startActivity(intent);
					}
					
					
				}).start();
//				if (AHState.getInstance().mService.getMStatus() == UPLOAD_DONE) {
//					Intent intent = new Intent(getActivity(), AHSettings.class);
//					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//					startActivity(intent);
//
//				}

				// UpLoadData upTask = new UpLoadData();
				// upTask.execute(100);
			}
		});
//		if (AHState.getInstance().mService != null
//				&& AHState.getInstance().mService.isUploading()) {
//			new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					while (AHState.getInstance().mService.isUploading()) {
//						mStatus.post(new Runnable() {
//
//							@Override
//							public void run() {
//								mStatus.setText(AHState.getInstance().mService
//										.getUploadStatusString());
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

	
		
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiHelper = new UiLifecycleHelper(getActivity(), callback);
		uiHelper.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REAUTH_ACTIVITY_CODE) {
			uiHelper.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		uiHelper.onSaveInstanceState(bundle);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	private void onSessionStateChange(final Session session,
			SessionState state, Exception exception) {
		if (session != null && session.isOpened()) {
			// Get the user's data.
			makeMeRequest(session);
		}
	}

	private void makeMeRequest(final Session session) {
		// Make an API call to get user data and define a
		// new callback to handle the response.
		Request request = Request.newMeRequest(session,
				new Request.GraphUserCallback() {
					@Override
					public void onCompleted(GraphUser user, Response response) {
						// If the response is successful
						if (session == Session.getActiveSession()) {
							if (user != null) {
								// Set the id for the ProfilePictureView
								// view that in turn displays the profile
								// picture.
								userId = user.getId();
								profilePictureView.setProfileId(userId);
								// Set the Textview's text to the user's name.
								userName = user.getName();
								userNameView.setText(userName);
								userName_Id = userName + "_" + userId;
								System.out
										.println("............" + userName_Id);
							}
						}
						if (response.getError() != null) {
							// Handle errors, will do so later.

						}
					}
				});
		request.executeAsync();
	}

	public class UpLoadData extends AsyncTask<Integer, Integer, String> {
		ProgressDialog progressDialog;

		// declare other objects as per your need
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(getActivity(),
					"Progress Dialog Title Text", "Process Description Text",
					true);

			// do initialization of required objects objects here
			super.onPreExecute();

		};

		@Override
		protected String doInBackground(Integer... params) {

			// do loading operation here
			if (AHState.getInstance().mService != null
					&& AHState.getInstance().mService.isUploading()) {
				AHState.getInstance().mService.startDataUpload(userName_Id);
				new Thread(new Runnable() {

					@Override
					public void run() {
						while (AHState.getInstance().mService.isUploading()) {
							mStatus.post(new Runnable() {

								@Override
								public void run() {

									publishProgress(AHState.getInstance().mService
											.getStatusProgress());
									System.out.println(AHState.getInstance().mService
											.getStatusProgress());

								}

								// mStatus.setText(AHState.getInstance().mService.getUploadStatusString());

							});
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ie) {
								//
							}
						}
					}
				}).start();
			}

			return "finish";
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			pb.setProgress(AHState.getInstance().mService.getStatusProgress());
			mStatus.setText(progress[0] + "%");
			super.onProgressUpdate(progress);
		}

		@Override
		protected void onPostExecute(String result) {
			// setTitle(result);
			super.onPostExecute(result);
			progressDialog.dismiss();
		};
	}

}
