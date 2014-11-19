package com.wsl.contacts;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import android.os.Handler;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.text.TextUtils;

public class SipContactsLoader extends AsyncTaskLoader<ArrayList<SipContactsLoader.Result>>{
	
	private ArrayList<Result> mResults;
	
	public static class Result {
		public String name;
		public String pinyin;
		public String number;
		public int id;
		public int contactId;
		
		public String sectionIndex;
	}
	
	public static final String[] PROJECTION = new String[] {
		SipContacts._ID,
		SipContacts.NAME,
		SipContacts.NAME_PINYIN,
		SipContacts.NUMBER,
		SipContacts.CONTACT_ID
	};
	
	private DataObserver mObserver;

	public SipContactsLoader(Context context) {
		super(context);
	}
	
	@Override
    public void deliverResult(ArrayList<Result> results) {
        mResults = results;
        if (isStarted()) {
            // If the Loader is started, immediately deliver its results.
            super.deliverResult(results);
        }
    }

	@Override
    protected void onStartLoading() {
        if (mResults != null) {
            // If we currently have a result available, deliver it immediately.
            deliverResult(mResults);
        }
        
        if(mObserver == null) {
        	mObserver = new DataObserver(new Handler(), this);
        }

        if (takeContentChanged() || mResults == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }
	
	@Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }
	
	@Override
    protected void onReset() {
        super.onReset();
        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated if needed.
        mResults = null;
        
        if(mObserver != null) {
        	getContext().getContentResolver().unregisterContentObserver(mObserver);
        }
    }

	@Override
	public ArrayList<Result> loadInBackground() {
		ArrayList<Result> contacts = getContacts();
		appendSectionIndex(contacts);
		return contacts;
	}
	
	public ArrayList<Result> getContacts() {
		ContentResolver resolver = getContext().getContentResolver();
		Cursor c = resolver.query(SipContacts.CONTENT_URI, PROJECTION, null, null, SipContacts.NAME_PINYIN + " ASC");
		ArrayList<Result> results = new ArrayList<Result>();
		try {
			c.moveToPosition(-1);
			while(c.moveToNext()) {
				Result result = new Result();
				result.id = c.getInt(0);
				result.name = c.getString(1);
				result.pinyin = c.getString(2);
				result.number = c.getString(3);
				result.contactId = c.getInt(4);
				results.add(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(c != null) {
				c.close();
			}
		}
		return results;
	}
	
	private void appendSectionIndex(ArrayList<Result> items) {
		for(Result item : items) {
			String pinyin = item.pinyin;
			if(TextUtils.isEmpty(pinyin)) continue;
			String firstChar = pinyin.substring(0, 1);
			item.sectionIndex = firstChar.toUpperCase(Locale.ENGLISH);
		}
	}
	
	public static boolean ContainChinese(CharSequence str) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }
	
	public static String HanZi2PinYin(String strs) {
		 
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        char[] ch = strs.trim().toCharArray();
        StringBuffer buffer = new StringBuffer("");
 
        try {
            for (int i = 0; i < ch.length; i++) {
                // unicode，bytes应该也可以.
                if (Character.toString(ch[i]).matches("[\u4e00-\u9fa5]+")) {
                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(
                            ch[i], format);
                    buffer.append(temp[0]);
                    buffer.append(" ");
                } else {
                    buffer.append(Character.toString(ch[i]));
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
	
	private static class DataObserver extends ContentObserver {
		
		private SipContactsLoader mLoader;

		public DataObserver(Handler handler, SipContactsLoader loader) {
			super(handler);
			mLoader = loader;
			
			mLoader.getContext().getContentResolver().registerContentObserver(SipContacts.CONTENT_URI, 
					true, this);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mLoader.onContentChanged();
		}

	}

}
