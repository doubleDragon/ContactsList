package com.wsl.contacts;

import java.util.ArrayList;

import com.wsl.contacts.R;
import com.wsl.contacts.ContactsListAdapter.ContactListItemViewCache;
import com.wsl.contacts.widgets.AutoScrollListView;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class ContactsListFragment extends Fragment implements OnFocusChangeListener,
		OnTouchListener,LoaderManager.LoaderCallbacks<ArrayList<SipContactsLoader.Result>>{
	
	final private Handler mHandler = new Handler();
	final private Runnable mRequestFocus = new Runnable() {
        public void run() {
        	mListView.focusableViewAvailable(mListView);
        }
    };
	
	private Context mContext;
	
	private TextView mEmptyView;
	private AutoScrollListView mListView;
	
	private ContactsListAdapter mListAdapter;
	private View mProgressContainer;
	private View mListContainer;
	
	private boolean mListShown;
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Display some text if there is no data.

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mListAdapter = new ContactsListAdapter(getActivity());
        setListAdapter();

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	private void ensureViews() {
		if(mListView != null) return;
		View root = getView();
		if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        } 

		mEmptyView = (TextView) root.findViewById(R.id.empty);
		mEmptyView.setVisibility(View.GONE);
		mProgressContainer = root.findViewById(R.id.progressContainer);
		mListContainer = root.findViewById(R.id.listContainer);
		mListView = (AutoScrollListView) root.findViewById(android.R.id.list);
		mListView.setOnFocusChangeListener(this);
        mListView.setOnTouchListener(this);
        mListView.setFastScrollEnabled(true);
        mListView.setFastScrollAlwaysVisible(true);
		mListView.setEmptyView(mEmptyView);
		mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContactListItemViewCache groupListItem = (ContactListItemViewCache) view.getTag();
                if (groupListItem != null) {
                    viewContact(groupListItem.getContactid());
                }
            }
        });
		
		mListShown = true;
		if (mListAdapter != null) {
            setListAdapter();
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
	}
	
	/**
     * Provide the cursor for the list view.
     */
    private void setListAdapter() {
        boolean hadAdapter = mListAdapter != null;
        if (mListView != null) {
        	mListView.setAdapter(mListAdapter);
            if (!mListShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setListShown(true, getView().getWindowToken() != null);
            }
        }
    }
    
    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     * 
     * <p>Applications do not normally need to use this themselves.  The default
     * behavior of ListFragment is to start with the list not being shown, only
     * showing it once an adapter is given with {@link #setListAdapter(ListAdapter)}.
     * If the list at that point had not been shown, when it does get shown
     * it will be do without the user ever seeing the hidden state.
     * 
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    private void setListShown(boolean shown) {
        setListShown(shown, true);
    }
    
    /**
     * Like {@link #setListShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    private void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }
    
    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     * 
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        ensureViews();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ensureViews();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.contacts_list_fragment, container, false);
	}
	
	@Override
	public void onDestroy() {
		mHandler.removeCallbacks(mRequestFocus);
        mListView = null;
        mListShown = false;
        mEmptyView = null;
        mProgressContainer = mListContainer = null;
		super.onDestroy();
	}

	private void viewContact(long contactId) {
		//Intent to system contacts
	}
	
	private void hideSoftKeyboard() {
        if (mContext == null) {
            return;
        }
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }
	
	/**
     * Dismisses the soft keyboard when the list takes focus.
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mListView && hasFocus) {
            hideSoftKeyboard();
        }
    }

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (view == mListView) {
            hideSoftKeyboard();
        }
        return false;
	}

	@Override
	public Loader<ArrayList<SipContactsLoader.Result>> onCreateLoader(int id, Bundle args) {
		mProgressContainer.setVisibility(View.VISIBLE);
		return new SipContactsLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<SipContactsLoader.Result>> loader, 
			ArrayList<SipContactsLoader.Result> data) {
		if (mListAdapter != null) {
            mListAdapter.notifyDataSetInvalidated();
        }
		
		mListAdapter.setData(data);
		// The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
		
		// Hide the progress indicator
        mProgressContainer.setVisibility(View.GONE);
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<SipContactsLoader.Result>> loader) {
		mListAdapter.setData(null);
	}
	
}
