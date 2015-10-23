package com.stacks_on.provider;

import com.stacks_on.db.SelectionBuilder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class FeedProvider extends ContentProvider {
	FeedDatabase mDatabaseHelper;
	private static final String AUTHORITY = FeedContract.CONTENT_AUTHORITY;
	// content id as uri routes
	public static final int ROUTE_ENTRIES = 1;
	public static final int ROUTE_ENTRIES_ID = 2;
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(AUTHORITY, "entries", ROUTE_ENTRIES);
		sUriMatcher.addURI(AUTHORITY, "entries/*", ROUTE_ENTRIES_ID);
	}
	
	@Override
	public boolean onCreate() {
		mDatabaseHelper = new FeedDatabase(getContext());
		return true;
	}
	
	// determine mime types
	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case ROUTE_ENTRIES:
				return FeedContract.Entry.CONTENT_TYPE;
			case ROUTE_ENTRIES_ID:
				return FeedContract.Entry.CONTENT_ITEM_TYPE;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);	
		}
	}
	
	// perform database query
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		SelectionBuilder builder = new SelectionBuilder();
		int uriMatch = sUriMatcher.match(uri);
		switch (uriMatch) {
			case ROUTE_ENTRIES_ID:
				String id = uri.getLastPathSegment();
				builder.where(FeedContract.Entry._ID + "=?", id);
			case ROUTE_ENTRIES:
				// return all known
				builder.table(FeedContract.Entry.TABLE_NAME)
					.where(selection, selectionArgs);
				Cursor c = builder.query(db, projection, sortOrder);
				// notification URI must be manually set...
				Context ctx = getContext();
				assert ctx != null;
				c.setNotificationUri(ctx.getContentResolver(), uri);
				return c;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}
	
	// insert new entry into db
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		assert db != null;
		final int match = sUriMatcher.match(uri);
		Uri result;
		switch (match) {
			case ROUTE_ENTRIES:
				long id = db.insertOrThrow(FeedContract.Entry.TABLE_NAME, null, values);
				result = Uri.parse(FeedContract.Entry.CONTENT_URI + "/" + id);
				break;
			case ROUTE_ENTRIES_ID:
				throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		// broadcast to observers, refresh UI
		Context ctx = getContext();
		assert ctx != null;
		ctx.getContentResolver().notifyChange(uri, null,false);
		return result;
	}
	
	// delete entry in db by URI
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SelectionBuilder builder = new SelectionBuilder();
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		int count;
		switch (match) {
			case ROUTE_ENTRIES:
				count = builder.table(FeedContract.Entry.TABLE_NAME)
					.where(selection, selectionArgs)
					.delete(db);
				break;
			case ROUTE_ENTRIES_ID:
				String id = uri.getLastPathSegment();
				count = builder.table(FeedContract.Entry.TABLE_NAME)
							.where(FeedContract.Entry._ID + "=?", id)
							.where(selection, selectionArgs)
							.delete(db);
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		// broadcast to observers, refresh UI
		Context ctx = getContext();
		assert ctx != null;
		ctx.getContentResolver().notifyChange(uri, null,false);
		return count;
	}
	
	//update entry in db by URI
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SelectionBuilder builder = new SelectionBuilder();
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		int count;
		switch (match) {
			case ROUTE_ENTRIES:
				count = builder.table(FeedContract.Entry.TABLE_NAME)
					.where(selection, selectionArgs)
					.update(db, values);
				break;
			case ROUTE_ENTRIES_ID:
				String id = uri.getLastPathSegment();
				count = builder.table(FeedContract.Entry.TABLE_NAME)
					.where(FeedContract.Entry._ID + "=?", id)
					.where(selection,selectionArgs)
					.update(db, values);
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		// broadcast, refresh
		Context ctx = getContext();
		assert ctx != null;
		ctx.getContentResolver().notifyChange(uri, null,false);
		return count;	
	}
	
	// sqlite backend for FeedProvider
	static class FeedDatabase extends SQLiteOpenHelper {
		//schema version
		public static final int DATABASE_VERSION = 1;
		// db filename
		public static final String DATABASE_NAME = "feed.db";
		private static final String TYPE_TEXT = " TEXT";
		private static final String TYPE_INTEGER = " INTEGER";
		private static final String COMMA_SEP = ",";
		// sql statement to create entry table
		private static final String SQL_CREATE_ENTRIES =
				"CREATE TABLE " + FeedContract.Entry.TABLE_NAME + " (" +
						FeedContract.Entry._ID + " INTEGER PRIMARY KEY," +
						FeedContract.Entry.COLUMN_NAME_ENTRY_ID + TYPE_TEXT + COMMA_SEP +
						FeedContract.Entry.COLUMN_NAME_TITLE + TYPE_TEXT + COMMA_SEP +
						FeedContract.Entry.COLUMN_NAME_LINK + TYPE_TEXT + COMMA_SEP +
						FeedContract.Entry.COLUMN_NAME_PUBLISHED + TYPE_INTEGER + ")";
		//sql statement to drop entry table
		private static final String SQL_DELETE_ENTRIES =
				"DROP TABLE IF EXISTS " + FeedContract.Entry.TABLE_NAME;
		
		public FeedDatabase(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_ENTRIES);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// this db is a cache for online data, can discard and recreate
			db.execSQL(SQL_DELETE_ENTRIES);
			onCreate(db);
		}
	}
}
