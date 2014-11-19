package com.wsl.contacts;

import android.content.ContentResolver;
import android.net.Uri;

public class SipContacts {
	
	public static final String AUTHORITY = "com.wsl.contacts";
	public static final String CONTACTS_TABLE_NAME = "contacts";
	
	public static final String _ID = "_id";
	public static final String NAME = "name";
	public static final String NAME_PINYIN = "pinyin";
	public static final String NUMBER = "number";
	public static final String CONTACT_ID = "contact_id";
	
	public static final Uri CONTENT_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"+ AUTHORITY +"/"
			+ CONTACTS_TABLE_NAME);
	public static final Uri CONTENT_ID_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/"
			+ CONTACTS_TABLE_NAME + "/");
	
	/**
	 * Base content type for csipsimple objects.
	 */
	public static final String DIR_TYPE = "vnd.android.cursor.dir/vnd.contacts";
	/**
	 * Base item content type for csipsimple objects.
	 */
	public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.contacts";

}
