package com.stacks_on;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;





import android.content.res.AssetManager;
import android.util.Log;

public class Utilities {
	// main utilities for general purpose, return sensibly!
	// could be separated into package based later
	
	private static final String TAG = "Stacks-On Utilities";
	
	public static boolean checkString (final String candidate) {
		return candidate != null && !candidate.isEmpty();
	}
	
	public static String getAssetsFileContent(AssetManager assetManager, String filename) {
		// generic function, assume the filename has proper location in assets folder
		// and includes file extension
		StringBuilder stringBuilder = new StringBuilder();
		if (!Utilities.checkString(filename)) {
			return "getAccessFile: no filename.";			
		}		
		else {				
			try {
				InputStream inputStream = assetManager.open(filename);
			    if (inputStream == null) {
			    	Log.e(TAG, "Filename path inputStream is null: " + filename);
			    	return "getAssetsFile: file not found.";
			    }
			    else {
				    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
					String inputLine;
				    while ((inputLine = in.readLine()) != null) {
				    	stringBuilder.append(inputLine);
				    }
				    in.close();	
			    }
			}
			catch (IOException e) {
				Log.e(TAG, "IO error accessing path: " + filename);
				return "getAccessFile: access error.";
			}
		}
		return stringBuilder.toString();
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
	
	public static String getDomainFromUrl(String original) {
		try {
			URL url = new URL(original);
			return url.getHost();
		}
		catch (IOException ex) {
			return "no domain";
		}
	}
}
