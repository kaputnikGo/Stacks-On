package com.stacks_on;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.stacks_on.net.FeedParser;
import com.stacks_on.provider.FeedContract;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

class SyncAdapter extends AbstractThreadedSyncAdapter {
	public static final String TAG = "SyncAdapter";
	// unique url for feed fetching
	private static final String FEED_URL = "http://dailyreview.com.au/feed/atom/";
	// 15 secs
	private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;
	// 10 secs
	private static final int NET_READ_TIMEOUT_MILLIS = 10000;
	// content resolver for db
	private final ContentResolver mContentResolver;
	// projection of known fields
	private static final String[] PROJECTION = new String[] {
		FeedContract.Entry._ID,
		FeedContract.Entry.COLUMN_NAME_ENTRY_ID,
		FeedContract.Entry.COLUMN_NAME_TITLE,
		FeedContract.Entry.COLUMN_NAME_LINK,
		FeedContract.Entry.COLUMN_NAME_PUBLISHED
	};
	// column positions in projection
	public static final int COLUMN_ID = 0;
	public static final int COLUMN_ENTRY_ID = 1;
	public static final int COLUMN_TITLE = 2;
	public static final int COLUMN_LINK = 3;
	public static final int COLUMN_PUBLISHED = 4;
	
	// constructors
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContentResolver = context.getContentResolver();
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
		mContentResolver = context.getContentResolver();
	}
	
	// called by android system after request to run sync adapter
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.i(TAG, "Beginning network sync...");
		try {
			final URL location = new URL(FEED_URL);
			InputStream stream = null;
			
			try {
				Log.i(TAG, "Streaming data from network: " + location);
				stream = downloadUrl(location);
				updateLocalFeedData(stream, syncResult);
			}
			finally {
				if (stream != null) {
					stream.close();
				}
			}
		}
		catch (MalformedURLException e) {
			Log.e(TAG, "Feed URL is malformed: " + e);
			syncResult.stats.numParseExceptions++;
			return;
		}
		catch (IOException e) {
			Log.e(TAG, "Error reading from network: " + e.toString());
			syncResult.stats.numIoExceptions++;
			return;
		}
		catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing feed: " + e.toString());
			syncResult.stats.numParseExceptions++;
			return;
		}
		catch (ParseException e) {
			Log.e(TAG, "Error parsing feed: " + e.toString());
			syncResult.stats.numParseExceptions++;
			return;			
		}
		catch (RemoteException e) {
			Log.e(TAG, "Error updating database: " + e.toString());
			syncResult.databaseError = true;
			return;
		}
		catch (OperationApplicationException e) {
			Log.e(TAG, "Error updating database: " + e.toString());
			syncResult.databaseError = true;
			return;
		}
		Log.i(TAG, "Network sync complete.");
	}
	
	// read XML from input stream, check for existing, merge via batch
	public void updateLocalFeedData(final InputStream stream, final SyncResult syncResult)
		throws IOException, XmlPullParserException, RemoteException, OperationApplicationException, ParseException {
		final FeedParser feedParser = new FeedParser();
		final ContentResolver contentResolver = getContext().getContentResolver();
		Log.i(TAG, "Parsing stream as Atom feed.");
		final List<FeedParser.Entry> entries = feedParser.parse(stream);
		Log.i(TAG, "Parsing complete, found " + entries.size() + " entries.");
				
		ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
		// hash table of entries
		HashMap<String, FeedParser.Entry> entryMap = new HashMap<String, FeedParser.Entry>();
		for (FeedParser.Entry e : entries) {
			entryMap.put(e.id, e);
		}
		
		Log.i(TAG, "Fetching local entries for merge.");
		Uri uri = FeedContract.Entry.CONTENT_URI;
		Cursor c = contentResolver.query(uri,  PROJECTION,  null,  null,  null);
		assert c != null;
		Log.i(TAG, "Found " + c.getCount() + " local entries, computing merge solution...");
		
		// find stale data
		int id;
		String entryId;
		String title;
		String link;
		long published;
		while (c.moveToNext()) {
			syncResult.stats.numEntries++;
			id = c.getInt(COLUMN_ID);
			entryId = c.getString(COLUMN_ENTRY_ID);
			title = c.getString(COLUMN_TITLE);
			link = c.getString(COLUMN_LINK);
			published = c.getLong(COLUMN_PUBLISHED);			
			FeedParser.Entry match = entryMap.get(entryId);
			if (match != null) {
				entryMap.remove(entryId);
				// check if entry needs updating
				Uri existingUri = FeedContract.Entry.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
				if ((match.title != null && !match.title.equals(title)) ||
						(match.link != null && !match.link.equals(link)) ||
						(match.published != published)) {
					// can update existing record
					Log.i(TAG, "Scheduling update: " + existingUri);
					batch.add(ContentProviderOperation.newUpdate(existingUri)
							.withValue(FeedContract.Entry.COLUMN_NAME_TITLE,  match.title)
							.withValue(FeedContract.Entry.COLUMN_NAME_LINK, match.link)
							.withValue(FeedContract.Entry.COLUMN_NAME_PUBLISHED,  match.published)
							.build());
					syncResult.stats.numUpdates++;
				}
				else {
					Log.i(TAG, "No action: " + existingUri);
				}
			}
			else {
				// entry not exist, remove from db
				Uri deleteUri = FeedContract.Entry.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
				Log.i(TAG, "Scheduling delete: " + deleteUri);
				batch.add(ContentProviderOperation.newDelete(deleteUri).build());
				syncResult.stats.numDeletes++;
			}
		}
		c.close();
		
		// add new items
		for (FeedParser.Entry e : entryMap.values()) {
			Log.i(TAG, "Scheduling insert, entry_id: " + e.id);
			batch.add(ContentProviderOperation.newInsert(FeedContract.Entry.CONTENT_URI)
					.withValue(FeedContract.Entry.COLUMN_NAME_ENTRY_ID,  e.id)
					.withValue(FeedContract.Entry.COLUMN_NAME_TITLE,  e.title)
					.withValue(FeedContract.Entry.COLUMN_NAME_LINK,  e.link)
					.withValue(FeedContract.Entry.COLUMN_NAME_PUBLISHED,  e.published)
					.build());
			syncResult.stats.numInserts++;
		}
		Log.i(TAG, "MErge solution ready, apply batch.");
		mContentResolver.applyBatch(FeedContract.CONTENT_AUTHORITY, batch);
		mContentResolver.notifyChange(
				FeedContract.Entry.CONTENT_URI,
				null,
				false);
	}
	
	// get the stream from a connection
	private InputStream downloadUrl(final URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS);
		conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.connect();
		return conn.getInputStream();
	}
}
