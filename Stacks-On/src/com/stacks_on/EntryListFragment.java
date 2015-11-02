package com.stacks_on;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.stacks_on.accounts.GenericAccountService;
import com.stacks_on.provider.FeedContract;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class EntryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "EntryListFragment";
	
	// cursor adapter for listview results
	private SimpleCursorAdapter mAdapter;
	
	//handle to syncObserver for progressbar element
	private Object mSyncObserverHandle;
	
	// menu for action bar
	private Menu mOptionsMenu;
	
	//projection for content provider
	private static final String[] PROJECTION = new String[] {
		FeedContract.Entry._ID,
		FeedContract.Entry.COLUMN_NAME_TITLE,
		FeedContract.Entry.COLUMN_NAME_LINK,
		FeedContract.Entry.COLUMN_NAME_PUBLISHED
	};
	//column indexes, cursor index is same as projection index
	//private static final int COLUMN_ID = 0;
	//private static final int COLUMN_TITLE = 1;
	private static final int COLUMN_URL_STRING = 2;
	private static final int COLUMN_PUBLISHED = 3;
	//columns to read from cursor for listView
	private static final String[] FROM_COLUMNS = new String[] {
		FeedContract.Entry.COLUMN_NAME_TITLE,
		FeedContract.Entry.COLUMN_NAME_PUBLISHED
	};
	// views list for cursor data
	// change these to style
	private static final int[] TO_FIELDS = new int[] {
		android.R.id.text1,
		android.R.id.text2
	};

	public EntryListFragment() {
		// default empty constructor
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	// create sync account at launch if needed
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// create if needed
		SyncUtils.CreateSyncAccount(activity);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view,  savedInstanceState);
		mAdapter = new SimpleCursorAdapter(
				getActivity(),
				android.R.layout.simple_list_item_activated_2,
				null,
				FROM_COLUMNS,
				TO_FIELDS,
				0
		);
		
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int i) {
				if (i == COLUMN_PUBLISHED) {
					// convert timestamp into human readable
					// still need to account for 24 hour clock or AM/PM
					DateTimeFormatter formatter = DateTimeFormat.forPattern("d/MM/yyyy, h:mm");
					DateTime datetime = new DateTime(cursor.getLong(i));
					((TextView) view).setText(datetime.toString(formatter));
					((TextView) view).setTextColor(Color.parseColor("#ff0000"));
					((TextView) view).setTextSize(12);
					return true;
				}
				else {
					// let the simple cursor adapter handle it
					((TextView) view).setTextColor(Color.parseColor("#0000ff"));
					((TextView) view).setTextSize(14);
					return false;
				}
			}
		});
		setListAdapter(mAdapter);
		setEmptyText("Waiting for sync...");
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		// look for state changes
		final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE; 
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}
	
	// query content provider for data, in background
	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		// only one loader, value 0
		return new CursorLoader(getActivity(),
				FeedContract.Entry.CONTENT_URI,
				PROJECTION,
				null,
				null,
				FeedContract.Entry.COLUMN_NAME_PUBLISHED + " desc"); //sort
		
	}
	
	// move the Cursor into the ListView, to refresh the UI
	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}
	
	// when data changed, reset then reload loader
	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		mAdapter.changeCursor(null);
	}
	
	//create actionBar
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		mOptionsMenu = menu;
		inflater.inflate(R.menu.main, menu);
	}
	
	// user touches actionBar
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				SyncUtils.TriggerRefresh();
				return true;
			case R.id.down_stack:
				((MainActivity)getActivity()).downStacksRequest();
				return true;
			case R.id.up_stack:
				((MainActivity)getActivity()).upStacksRequest();
				return true;
			case R.id.action_settings:
				((MainActivity)getActivity()).settingsRequest();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// load article that user selects via the StacksWebClient if its within our domain
	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);
		// get the selection
		Cursor c = (Cursor) mAdapter.getItem(position);
		String articleUrlString = c.getString(COLUMN_URL_STRING);
		
		if (articleUrlString == null) {
			Log.e(TAG, "Launch entry with null link.");
			return;
		}
		Log.i(TAG, "Opening url: " + articleUrlString);
		((MainActivity)getActivity()).loadSelectedArticle(articleUrlString);
	}
	
	// set the state of the refresh button
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setRefreshActionButtonState(boolean refreshing) {
		Log.e(TAG, "refreshing: " + refreshing);
		if (mOptionsMenu == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			return;
		}
		
		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
			}
			else {
				refreshItem.setActionView(null);
			}
		}
	}
	
	// syncStatusObserver updates refresh button
	private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		@Override
		public void onStatusChanged(int which) {
			getActivity().runOnUiThread(new Runnable() {
				// syncAdapter is background thread, UI on UI thread
				@Override
				public void run() {
					Account account = GenericAccountService.GetAccount(SyncUtils.ACCOUNT_TYPE);
					if (account == null) {
						// getAccount should not return invalid value, but...
						setRefreshActionButtonState(false);
						return;
					}
					Log.d(TAG, "run...");
					// test some things
					boolean syncActive = ContentResolver.isSyncActive(
							account, FeedContract.CONTENT_AUTHORITY);
					if (syncActive) {
						setRefreshActionButtonState(true);
					}
					else {
						setRefreshActionButtonState(false);
					}
					
					
				}
			});
		}
	};
}
