package com.stacks_on;

import com.stacks_on.accounts.GenericAccountService;
import com.stacks_on.provider.FeedContract;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SyncUtils {
	private static final long SYNC_FREQUENCY = 60 * 60; // 1 hour in seconds
	private static final String CONTENT_AUTHORITY = FeedContract.CONTENT_AUTHORITY;
	private static final String PREF_SETUP_COMPLETE = "setup_complete";
	// value must match value in res/xml/syncadapter.xml
	public static final String ACCOUNT_TYPE = "com.stacks_on.account";
	
	// if not present - create an entry for this app in system account list
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static void CreateSyncAccount(Context context) {
		boolean newAccount = false;
		boolean setupComplete = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);
		// create account if new or deleted
		Account account = GenericAccountService.GetAccount(ACCOUNT_TYPE);
		AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
		
		if (accountManager.addAccountExplicitly(account, null, null)) {
			// tell system that account supports sync
			ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
			// can auto-sync
			ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
			// schedule pref, system can override
			ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, new Bundle(), SYNC_FREQUENCY);
			newAccount = true;
		}
		
		// schedule initial sync, check for problems
		if (newAccount || !setupComplete) {
			TriggerRefresh();
			PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_SETUP_COMPLETE, true).commit();
		}
	}
	
	// method to manually trigger sync via refresh button
	public static void TriggerRefresh() {
		Bundle b = new Bundle();
		// disable some default settings to allow immediate sync
		b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
		ContentResolver.requestSync(
				GenericAccountService.GetAccount(ACCOUNT_TYPE),
				FeedContract.CONTENT_AUTHORITY,
				b);
	}
}
