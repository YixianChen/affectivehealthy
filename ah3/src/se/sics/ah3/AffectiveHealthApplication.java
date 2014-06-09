package se.sics.ah3;

import java.util.List;

import com.facebook.model.GraphUser;

import android.app.Application;

public class AffectiveHealthApplication extends Application {
	private List<GraphUser> selectedUsers;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		AHState.getInstance().init(this);
	}
  
	public List<GraphUser> getSelectedUsers() {
	    return selectedUsers;
	}
	public void setSelectedUsers(List<GraphUser> users) {
	    selectedUsers = users;
	}
}
