package com.stacks_on;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.os.AsyncTask;
import android.util.Log;

public class StacksWebConnector extends AsyncTask<Void, Void, Void> {
	//TODO - this prob not best way, MainActivity...
	private MainActivity mainActivity;
	private String urlString;
	private Document doc;
	
	private static final String TAG = "StacksWebConnector";

	public StacksWebConnector(MainActivity mainActivity, String urlString) {
		this.mainActivity = mainActivity;
		this.urlString = urlString;
	}
	

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	@Override
	protected Void doInBackground(Void... params) {		
		try {
			int status = getWebsiteStatus(urlString);
			
			if (websiteStatusProceed(status)) {
				doc = Jsoup.connect(urlString).get();
			}
			else {
				Log.e(TAG, "Error, website status: " + status);
			}
		}
		catch (IOException ex) {
			Log.e(TAG, "Error opening " + urlString + " with: " + ex.toString());
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		mainActivity.parseRequestedHtml(doc);
	}
	
	private int getWebsiteStatus(final String candidate) {
		// return a website status code based upon candidate string for url
		Connection.Response response = null;	
		try {
			response = Jsoup.connect(candidate).execute();
			return response.statusCode();
		}
		catch (IOException ex) {
			Log.e(TAG, "GetWebsiteStatus connection error: " + ex);
			return 0;
		}
	}
	
	private boolean websiteStatusProceed(int status) {
		// can mode this to allow certain conditions, like MSIE webzones
		switch (status) {
		//successful cases
			case 200: //OK
			case 201: //CREATED
			case 202: //ACCEPTED
			case 203: //NO-INFO
				return true;
			case 204: //NO CONTENT
			case 205: //RESET
			case 206: //PARTIAL CONTENT
				return false;
		// redirection cases
			case 300: //MULTIPLE CHOICES
			case 301: //MOVED PERMANENTLY
			case 302: //FOUND, temp diff uri
			case 303: //OTHER
			case 304: //NOT MODIFIED
			case 305: //USE PROXY
			case 306: //UNUSED
			case 307: //TEMP REDIRECT
				return false;
		// client error cases
			case 400: //BAD REQUEST
			case 401: //UNUTHORIZED
			case 402: //PAYMENT REQUIRED - unused
			case 403: //FORBIDDEN
			case 404: //NOT FOUND
			case 405: //NOT ALLOWED
			case 406: //NOT ACCPETABLE
			case 407: //PROXY AUTH REQUIRED	
			case 408: //REQUEST TIMEOUT
			case 409: //CONFLICT
			case 410: //GONE
			case 411: //CONTENT_LENGTH REQUIRED
			case 412: //PRE-CONDITION FAILED
			case 413: //REQUEST ENTITY TOO LARGE
			case 414: //REQUEST URI TOO LONG
			case 415: //UNSUPPORTED MEDIA TYPE
			case 416: //REQUESTED RANGE NOT SATISFIABLE
			case 417: //EXPECTATION FAILED
				return false;
		// server error cases
			case 500: //INTERNAL SERVER ERROR
			case 501: //NOT IMPLEMENTED
			case 502: //BAD GATEWAY
			case 503: //SERVICE UNAVAILABLE
			case 504: //GATEWAY TIMEOUT
			case 505: //HTTP VERSION NOT SUPPORTED
				return false;
			default:
				return false;				
		}
	}
}
