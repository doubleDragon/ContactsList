package com.wsl.contacts.db;

import com.wsl.contacts.SipContacts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class ContactsDatabase extends SQLiteOpenHelper{
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "contacts";
	
	private final static String TABLE_CONTACTS_CREATE = "CREATE TABLE IF NOT EXISTS " +
			SipContacts.CONTACTS_TABLE_NAME + " (" +
			SipContacts._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			SipContacts.NAME + " TEXT NOT NULL," +
			SipContacts.NAME_PINYIN + " TEXT NOT NULL," +
			SipContacts.NUMBER + " TEXT NOT NULL," +
			SipContacts.CONTACT_ID + " INTEGER NOT NULL DEFAULT -1" +
			")";

	public ContactsDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CONTACTS_CREATE);//by wsl
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
