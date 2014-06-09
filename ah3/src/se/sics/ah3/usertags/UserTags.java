package se.sics.ah3.usertags;

import java.util.Vector;

import android.content.Context;
import android.database.Cursor;

public class UserTags {
	public class UserTag {
		private long mID;
		private long mTime;
		private String mTag;
		
		public UserTag(long id, long time, String tag)
		{
			mID = id;
			mTime = time;
			mTag = tag;
		}
		
		public UserTag(Cursor c)
		{
			mID = c.getLong(0);
			mTime = c.getLong(1);
			mTag = c.getString(2);
		}

		public long getID() { return mID; }
		public long getTime() { return mTime; }
		public String getTag() { return mTag; }
		
		public void setTag(String tag) {
			mDB.setTag(mID, tag);
		}
	}

	private UserTagsDB mDB;

	public UserTags(Context context)
	{
		mDB = new UserTagsDB(context);
		mDB.create();
	}

	public Vector<UserTag> getUserTags(long startTime, long endTime)
	{
		Vector<UserTag> ret = new Vector<UserTag>();

		Cursor c = mDB.get(startTime, endTime);

		while(c.moveToNext())
		{
			UserTag el = new UserTag(c);
			
			ret.add(el);
		}
		return ret;
	}

	public UserTag addTag(String tag)
	{
		// insert into database. return tag
		long time = System.currentTimeMillis();
		long id = mDB.insert(time, tag);
		return new UserTag(id,time,tag);
	}

	public UserTag addTag(long time, String tag)
	{
		// insert into database. return EnergyLevel
		long id = mDB.insert(time, tag);
		return new UserTag(id,time,tag);
	}

	// returns null if no last levels found...
	public UserTag getCurrent()
	{
		// get all levels in the last seconds
		long date = System.currentTimeMillis();
		Cursor c = mDB.get(date-4*1000,date);
		if (c.getCount()==0)
		{
			return null;
		}
		c.moveToNext();
		return new UserTag(c);
	}
}
