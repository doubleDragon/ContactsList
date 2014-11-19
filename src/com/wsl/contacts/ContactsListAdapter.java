package com.wsl.contacts;

import java.util.ArrayList;
import java.util.Arrays;

import com.wsl.contacts.R;
import com.wsl.contacts.SipContactsLoader.Result;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class ContactsListAdapter extends ArrayAdapter<SipContactsLoader.Result>
		implements SectionIndexer, AbsListView.RecyclerListener {
	
	private final LayoutInflater mInflater;
    private String[] mSections;
    private int[] mPositions;

	public ContactsListAdapter(Context context) {
		super(context, R.layout.contact_list_item);
		
		mInflater = LayoutInflater.from(context);
	}
	
	public void setData(ArrayList<SipContactsLoader.Result> items) {
		clear();
		
		if (items == null) return;
		addAll(items);
		
		//Generate sections and positions
		ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        String lastSectionIndex = null;
        boolean hasSections = true;

        for (int i = 0; i < items.size(); i++) {
        	SipContactsLoader.Result item = items.get(i);
            String index;

            index = item.sectionIndex;

            if (index == null) {
                hasSections = false;
                break;
            }

            if (!TextUtils.equals(index, lastSectionIndex)) {
                sections.add(index);
                positions.add(i);
                lastSectionIndex = index;
            }
        }

        if (!hasSections) {
            sections.clear();
            sections.add("");
            positions.clear();
            positions.add(0);
        }

        int count = sections.size();
        mSections = new String[count];
        mPositions = new int[count];
        for (int i = 0; i < count; i++) {
            mSections[i] = sections.get(i);
            mPositions[i] = positions.get(i);
        }
	}
	
	@Override
	public Result getItem(int position) {
		return super.getItem(position);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		Result item = getItem(position);
        View result;
        ContactListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (ContactListItemViewCache) result.getTag();
        } else {
            result = mInflater.inflate(R.layout.contact_list_item, parent, false);
            viewCache = new ContactListItemViewCache(result);
            result.setTag(viewCache);
        }
        viewCache.setContactId(item.contactId);

        boolean showHeader = Arrays.binarySearch(mPositions, position) >= 0;
        if (showHeader) {
            String index = item.sectionIndex;
            viewCache.header.setVisibility(View.VISIBLE);
            viewCache.separator.setText(index != null ? index.toUpperCase() : "");
        } else {
        	viewCache.header.setVisibility(View.GONE);
        }
        viewCache.nameView.setText(item.name);
        
        return result;
    }

	
	@Override
    public void onMovedToScrapHeap(View view) {
//        unbindView(view);
    }
	
	@Override
    public int getPositionForSection(int section) {
        if (section < 0 || section >= mSections.length) {
            return -1;
        }

        return mPositions[section];
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return -1;
        }

        int index = Arrays.binarySearch(mPositions, position);

        /*
         * Consider this example: section positions are 0, 3, 5; the supplied
         * position is 4. The section corresponding to position 4 starts at
         * position 3, so the expected return value is 1. Binary search will not
         * find 4 in the array and thus will return -insertPosition-1, i.e. -3.
         * To get from that number to the expected value of 1 we need to negate
         * and subtract 2.
         */
        return index >= 0 ? index : -index - 2;
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    /**
     * Cache of the children views of a contact detail entry represented by a
     */
    public static class ContactListItemViewCache {

        public final View header;
        public final TextView nameView;
        public final TextView separator;
        private long mContactId;

        public ContactListItemViewCache(View view) {
        	header = view.findViewById(R.id.header);
        	nameView = (TextView) view.findViewById(R.id.name);
        	separator = (TextView) view.findViewById(R.id.separator);
        }

        public void setContactId(long contactId) {
        	mContactId = contactId;
        }

        public long getContactid() {
            return mContactId;
        }
    }
}
