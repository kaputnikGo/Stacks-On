package com.stacks_on;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


import android.util.Log;

public class Utilities {
	// main utilities for general purpose, return sensibly!
	// could be separated into package based later
	
	private static final String TAG = "Stacks-On Utilities";
	
	public static boolean checkString (final String candidate) {
		return candidate != null && !candidate.isEmpty();
	}
	
	public static boolean checkWebsiteUp(final String candidate) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			// may also need this:
			//HttpURLConnection.setInstanceFollowRedirects(false);
			HttpURLConnection urlConn = (HttpURLConnection) new URL(candidate).openConnection();
			urlConn.setRequestMethod("HEAD");
			Log.d(TAG, "responsecode not async: " + urlConn.getResponseCode());
			return (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}		
	}
	
	
	@SuppressWarnings("unused")
	public static boolean isValidUrl(final String candidate) {
		// this returns only if it appears to be a valid url string
		try {
			URI uri = new URI(candidate);
			return true;
		}
		catch (URISyntaxException e) {
			return false;
		}
	}
}
