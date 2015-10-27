package com.stacks_on.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class SelectionBuilder {
	private static final String TAG = "SelectionBuilder";
	private String mTable = null;
	private Map<String, String> mProjectionMap = new HashMap<String, String>();
	private StringBuilder mSelection = new StringBuilder();
	private ArrayList<String> mSelectionArgs = new ArrayList<String>();
	
	// reset builder is better than create new
	public SelectionBuilder reset() {
		mTable = null;
		mSelection.setLength(0);
		mSelectionArgs.clear();
		return this;
	}
	
	// selectionBuilder to build a safe sql query
	public SelectionBuilder where(String selection, String... selectionArgs) {
		if (TextUtils.isEmpty(selection)) {
			if (selectionArgs != null && selectionArgs.length > 0) {
				throw new IllegalArgumentException("Valid selection required when using arguments=.");
			}
			return this;
		}
		if (mSelection.length() > 0) {
			mSelection.append(" AND ");
		}
		mSelection.append("(").append(selection).append(")");
		if (selectionArgs != null) {
			Collections.addAll(mSelectionArgs, selectionArgs);
		}
		return this;
	}
	
	// sql table
	public SelectionBuilder table(String table) {
		mTable = table;
		return this;
	}
	
	//verify
	private void assertTable() {
		if (mTable == null) {
			throw new IllegalStateException("Table not specified");
		}
	}
	
	// inner join
	public SelectionBuilder mapToTable(String column, String table) {
		mProjectionMap.put(column, table + "." + column);
		return this;
	}
	
	// new column if needed, sub-query
	public SelectionBuilder map(String fromColumn, String toClause) {
		mProjectionMap.put(fromColumn, toClause + " AS " + fromColumn);
		return this;
	}
	
	public String getSelection() {
		return mSelection.toString();
	}
	
	public String[] getSelectionArgs() {
		return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
	}
	
	//process user projection
	private void mapColumns(String[] columns) {
		for (int i = 0; i < columns.length; i++) {
			final String target = mProjectionMap.get(columns[i]);
			if (target != null) {
				columns[i] = target;
			}
		}
	}
	
	// description of state, no sql
	@Override
	public String toString() {
		return "SelectionBuilder[table=" + mTable + ", selection=" + getSelection() + ", selectionArgs=" + Arrays.toString(getSelectionArgs()) + "]";
	}
	
	// execute sql query on specified db
	public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
		return query(db, columns, null, null, orderBy, null);
	}
	
	// execute sql query on db
	public Cursor query(SQLiteDatabase db, String[] columns, String groupBy, String having, String orderBy, String limit) {
		assertTable();
		if (columns != null)
			mapColumns(columns);
		Log.v(TAG, "Query(columns=" + Arrays.toString(columns) + ") " + this);
		return db.query(mTable,  columns,  getSelection(),  getSelectionArgs(), groupBy, having, orderBy, limit);
	}
	
	// execute sql update
	public int update(SQLiteDatabase db, ContentValues values) {
		assertTable();
		Log.v(TAG, "Update() " + this);
		return db.update(mTable, values, getSelection(), getSelectionArgs());
	}
	
	// execute sql delete
	public int delete(SQLiteDatabase db) {
		assertTable();
		Log.v(TAG, "Delete() " + this);
		return db.delete(mTable, getSelection(), getSelectionArgs());
	}
}
