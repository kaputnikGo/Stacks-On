package com.stacks_on.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class FeedContract {
	private FeedContract() {
		// def
	}
	
	// content provider authority
	public static final String CONTENT_AUTHORITY = "com.stacks_on";
	// base URI
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
	// path component
	private static final String PATH_ENTRIES = "entries";
	// entry record columns
	public static class Entry implements BaseColumns {
		// MIME type for lists
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd." + CONTENT_AUTHORITY + ".entries";
		// MIME type for entries
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "vnd." + CONTENT_AUTHORITY + ".entry";
		// fully qualified URI for entry resources
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ENTRIES).build();
		// table name for entry resources
		public static final String TABLE_NAME = "entry";
		// atom id, not db primary key _ID
		public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
		// article title
		public static final String COLUMN_NAME_TITLE = "title";
		// article link (rel="alternate")
		public static final String COLUMN_NAME_LINK = "link";
		// article date
		public static final String COLUMN_NAME_PUBLISHED = "published";
	}
}
