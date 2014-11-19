package com.wsl.contacts.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.wsl.contacts.SipContacts;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.provider.CallLog;
import android.support.v4.database.DatabaseUtilsCompat;
import android.text.TextUtils;

public class ContactsListProvider extends ContentProvider{
	
	private static final String UNKNOWN_URI_LOG = "Unknown URI ";
	
	private static final int CONTACTS = 1, CONTACTS_ID = 2;
	
	private static final String[] CONTACT_PROJECTIONS = new String[] {
		SipContacts._ID,
		SipContacts.NAME,
		SipContacts.NAME_PINYIN,
		SipContacts.NUMBER,
		SipContacts.CONTACT_ID
	};
	
	/**
	 * A UriMatcher instance
	 */
	private static final UriMatcher URI_MATCHER;
	static {
		// Create and initialize URI matcher.
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		
		URI_MATCHER.addURI(SipContacts.AUTHORITY, SipContacts.CONTACTS_TABLE_NAME, CONTACTS);
		URI_MATCHER.addURI(SipContacts.AUTHORITY, SipContacts.CONTACTS_TABLE_NAME + "/#", CONTACTS_ID);
	}
	
	private ContactsDatabase mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new ContactsDatabase(getContext());
		return true;
	}
	
	private static List<String> getPossibleFieldsForType(int type) {
		List<String> possibles = null;
		switch (type) {
		case CONTACTS:
		case CONTACTS_ID:
			possibles = Arrays.asList(CONTACT_PROJECTIONS);
			break;
		default:
		}
		return possibles;
	}

	private static void checkSelection(List<String> possibles, String selection) {
		if (selection != null) {
			String cleanSelection = selection.toLowerCase();
			for (String field : possibles) {
				cleanSelection = cleanSelection.replace(field, "");
			}
			cleanSelection = cleanSelection.replaceAll("like", "");
			
			cleanSelection = cleanSelection.replaceAll(" in \\([0-9 ,]+\\)", "");
			cleanSelection = cleanSelection.replaceAll(" and ", "");
			cleanSelection = cleanSelection.replaceAll(" or ", "");
			cleanSelection = cleanSelection.replaceAll("[0-9]+", "");
			cleanSelection = cleanSelection.replaceAll("[=? ]", "");
			
			if (cleanSelection.length() > 0) {
				throw new SecurityException("You are selecting wrong thing " + cleanSelection);
			}
		}
	}

	private static void checkProjection(List<String> possibles, String[] projection) {
		if (projection != null) {
			// Ensure projection is valid
			for (String proj : projection) {
				proj = proj.replaceAll(" AS [a-zA-Z0-9_]+$", "");
				if (!possibles.contains(proj)) {
					throw new SecurityException("You are asking wrong values " + proj);
				}
			}
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Constructs a new query builder and sets its table name
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String finalSortOrder = sortOrder;
		String[] finalSelectionArgs = selectionArgs;
		String finalGrouping = null;
		String finalHaving = null;
		int type = URI_MATCHER.match(uri);

		Uri regUri = uri;

		// Security check to avoid data retrieval from outside
		int remoteUid = Binder.getCallingUid();
		int selfUid = android.os.Process.myUid();
		if (remoteUid != selfUid) {
//			if (type == ACCOUNTS || type == ACCOUNTS_ID) {
//				for (String proj : projection) {
//					if (proj.toLowerCase().contains(SipProfile.FIELD_DATA)
//							|| proj.toLowerCase().contains("*")) {
//						throw new SecurityException(
//								"Password not readable from external apps");
//					}
//				}
//			}
		}
		// Security check to avoid project of invalid fields or lazy projection
		List<String> possibles = getPossibleFieldsForType(type);
		if (possibles == null) {
			throw new SecurityException("You are asking wrong values " + type);
		}
		checkProjection(possibles, projection);
		checkSelection(possibles, selection);

		Cursor c;
		long id;
		switch (type) {
		case CONTACTS:
			qb.setTables(SipContacts.CONTACTS_TABLE_NAME);
			if(sortOrder == null) {
				finalSortOrder = SipContacts._ID + " DESC";
			}
			break;
		case CONTACTS_ID:
			qb.setTables(SipContacts.CONTACTS_TABLE_NAME);
			qb.appendWhere(SipContacts._ID + "=?");
			finalSelectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
					new String[] { uri.getLastPathSegment() });
			break;
		default:
			throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
		}
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		c = qb.query(db, projection, selection, finalSelectionArgs, finalGrouping, finalHaving, finalSortOrder);

		c.setNotificationUri(getContext().getContentResolver(), regUri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case CONTACTS:
			return SipContacts.DIR_TYPE;
		case CONTACTS_ID:
			return SipContacts.ITEM_TYPE;
		default:
			throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		int matched = URI_MATCHER.match(uri);
		String matchedTable = null;
		Uri baseInsertedUri = null;

		switch (matched) {
		case CONTACTS:
		case CONTACTS_ID:
			matchedTable = SipContacts.CONTACTS_TABLE_NAME;
			baseInsertedUri = SipContacts.CONTENT_ID_URI;
			break;
		default:
			break;
		}
		if (matchedTable == null) {
			throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
		}

		ContentValues values;

		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		long rowId = db.insert(matchedTable, null, values);

		// If the insert succeeded, the row ID exists.
		if (rowId >= 0) {
			// TODO : for inserted account register it here

			Uri retUri = ContentUris.withAppendedId(baseInsertedUri, rowId);
			getContext().getContentResolver().notifyChange(retUri, null);

			return retUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		String finalWhere;
		int count = 0;
		int matched = URI_MATCHER.match(uri);
		Uri regUri = uri;

		List<String> possibles = getPossibleFieldsForType(matched);
		checkSelection(possibles, where);

		ArrayList<Long> oldRegistrationsAccounts = null;

		switch (matched) {
		case CONTACTS:
			count = db.delete(SipContacts.CONTACTS_TABLE_NAME, where, whereArgs);
			break;
		case CONTACTS_ID:
			finalWhere = DatabaseUtilsCompat.concatenateWhere(SipContacts._ID + " = " + ContentUris.parseId(uri),
					where);
			count = db.delete(SipContacts.CONTACTS_TABLE_NAME, finalWhere, whereArgs);
			break;
		default:
			throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
		}

		getContext().getContentResolver().notifyChange(regUri, null);

		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		String finalWhere;
		int matched = URI_MATCHER.match(uri);

		List<String> possibles = getPossibleFieldsForType(matched);
		checkSelection(possibles, where);

		switch (matched) {
		case CONTACTS:
			count = db.update(SipContacts.CONTACTS_TABLE_NAME, values, where, whereArgs);
			break;
		case CONTACTS_ID:
			finalWhere = DatabaseUtilsCompat.concatenateWhere(SipContacts._ID + " = " + ContentUris.parseId(uri),
					where);
			count = db.update(SipContacts.CONTACTS_TABLE_NAME, values, finalWhere, whereArgs);
			break;
		default:
			throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

}
