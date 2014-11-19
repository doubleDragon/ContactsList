package com.wsl.contacts;

import java.util.HashSet;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class ContactsStatusService extends Service{
	
	private static final String TAG = ContactsStatusService.class.getSimpleName();
	
	private static final String SYNC_ACTION = "syncContacts";
	private static final int MSG_CONTACTS_CHANGE = 10000;
	
	private static final Uri PHONE_URI = Phone.CONTENT_URI.buildUpon()
			.appendQueryParameter("remove_duplicate_entries", "true").build();
	private static final String[] PROJECTION = new String[] {
        Phone.NUMBER,
        Phone.DISPLAY_NAME_PRIMARY,
        Phone.CONTACT_ID
    };
	
    private static final int COLUMN_NUMBER           = 0;
    private static final int COLUMN_DISPLAY_NAME     = 1;
    private static final int COLUMN_CONTACT_ID       = 2;
    
    private static final String SELECTION = Phone.NUMBER + " NOT NULL";
    
	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;
	
	private final ContentObserver mObserver = new ContentObserver(mServiceHandler) {

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Log.d(TAG, "on change");
			mServiceHandler.obtainMessage(MSG_CONTACTS_CHANGE, -1 , -1).sendToTarget();
		}
		
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private void saveServiceStatus(boolean work) {
		Constant.getInstance().setSyncServiceStatus(work);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, TAG + " onCreate--------");
		saveServiceStatus(true);
		
		HandlerThread thread = new HandlerThread(TAG);
		thread.start();
		
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		
		Log.v(TAG, "registe observer in onCreate");
		getContentResolver().registerContentObserver(ContactsContract.AUTHORITY_URI, true, mObserver);
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Temporarily removed for this duplicate message track down.
		
        Log.v(TAG, "onStart: #" + startId + " flags: " + flags + " Intent: " + intent);
        Message msg = mServiceHandler.obtainMessage();
        msg.what = MSG_CONTACTS_CHANGE;
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

	@Override
	public void onDestroy() {
		Log.v(TAG, "unregiste observer in onDestroy");
		saveServiceStatus(false);
		getContentResolver().unregisterContentObserver(mObserver);
		mServiceLooper.quit();
		super.onDestroy();
	}
	
	/**
	 * First load contacts from system contacts database
	 * Second delete locale contacts database
	 * Then insert data
	 * @param startId
	 */
	private void handleSync(int startId) {
		Log.v(TAG, "sync contacts startId: " + startId);
		HashSet<ContactBean> contacts = getContactsFromSystem();
		deleteLocalContacts();
		insertLocal(contacts);
		contacts.clear();
	}
	
	private void deleteLocalContacts() {
		ContentResolver resolver = getContentResolver();
		resolver.delete(SipContacts.CONTENT_URI, null, null);
	}
	
	private void insertLocal(HashSet<ContactBean> beans) {
		if(beans.isEmpty()) return;
		ContentResolver resolver = getContentResolver();
		ContentValues value = new ContentValues();
		for(ContactBean bean : beans) {
			value.clear();
			value.put(SipContacts.NAME, bean.name);
			value.put(SipContacts.NAME_PINYIN, bean.pinyin);
			value.put(SipContacts.NUMBER, bean.number);
			value.put(SipContacts.CONTACT_ID, bean.contactId);
			resolver.insert(SipContacts.CONTENT_URI, value);
		}
	}
	
	private HashSet<ContactBean>  getContactsFromSystem() {
		Cursor c = getContentResolver().query(PHONE_URI, PROJECTION, 
				SELECTION, null, null);
		HashSet<ContactBean> contacts = new HashSet<ContactBean>();
		String number, name;
		int contactId;
		try {
			c.moveToPosition(-1);
			while(c.moveToNext()) {
				number = c.getString(COLUMN_NUMBER);
				name = c.getString(COLUMN_DISPLAY_NAME);
				contactId = c.getInt(COLUMN_CONTACT_ID);
				ContactBean bean = new ContactBean(number, name, SipContactsLoader.HanZi2PinYin(name), contactId);
				if(contacts.contains(bean)) continue;
				contacts.add(bean);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(c != null) {
				c.close();
			}
		}
		return contacts;
	}

	private class ServiceHandler extends Handler {
		
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int what = msg.what;
			if(what != MSG_CONTACTS_CHANGE) {
				return;
			}
			int startId = msg.arg1;
			handleSync(startId);
		}
	}
	
	private static class ContactBean {
		public final String number;
		public final String name;
		public final String pinyin;
		public final int contactId;
		
		public ContactBean(String number, String name, String pinyin, int contactId) {
			this.number = number;
			this.name = name;
			this.pinyin = pinyin;
			this.contactId = contactId;
		}
		
		// Use @Override to avoid accidental overloading.
		@Override
		public boolean equals(Object o) {
			// Return true if the objects are identical.
			// (This is just an optimization, not required for correctness.)
			if (this == o) {
				return true;
			}

			// Return false if the other object has the wrong type.
			// This type may be an interface depending on the interface's
			// specification.
			if (!(o instanceof ContactBean)) {
				return false;
			}

			// Cast to the appropriate type.
			// This will succeed because of the instanceof, and lets us access
			// private fields.
			ContactBean lhs = (ContactBean) o;

			// Check each field. Primitive fields, reference fields, and
			// nullable reference
			// fields are all treated differently.
			return contactId == lhs.contactId
					&& number.equals(lhs.number)
					&& name.equals(lhs.name)
					&& pinyin.equals(lhs.pinyin);
		}
		
		@Override
		public int hashCode() {
			// Start with a non-zero constant.
			int result = 17;

			result = 31 * result + contactId;

			result = 31 * result + number.hashCode();
			result = 31 * result + name.hashCode();

			return result;
		}
	}
	
}
