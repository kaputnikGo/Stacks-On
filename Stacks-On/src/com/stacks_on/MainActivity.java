package com.stacks_on;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends FragmentActivity {

	private WebView webView;
	private String urlSelectorString;
	private String htmlSelectorString;
	private static final String TAG = "MainActivity";
	/*
	 * using jsoup
	 * using joda-time
	 * 
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		urlSelectorString = "not set";
		htmlSelectorString = "empty";
	}
	
	// load pages via the StacksWebClient
	public void loadSelectedArticle(String articleUrlString) {
		// already checked in EntryListFragment...
		if (articleUrlString == null) {
			return;
		}
		else {
			urlSelectorString = articleUrlString;
			setContentView(R.layout.web_view_layout);
			webView = (WebView) findViewById(R.id.webview);
			WebSettings webSettings = webView.getSettings();
			webSettings.setJavaScriptEnabled(true);
			
			StacksWebClient stacksWebClient = new StacksWebClient(this);
			webView.setWebViewClient(stacksWebClient);
			Log.d(TAG, "loading via jsoup...");
			GetURL getUrl = new GetURL();
			getUrl.execute();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// back key and history check
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		// default
		return super.onKeyDown(keyCode, event);
	}
	
	// don't load on the main thread...
	private class GetURL extends AsyncTask<Void, Void, Void> {
		String htmlString = "nothing";
		Document doc;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				doc = Jsoup.connect(urlSelectorString).get();
				htmlString = doc.html();
			}
			catch (IOException ex) {
				Log.e(TAG, "Error opening " + urlSelectorString + " with: " + ex.toString());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			webView.loadData(htmlString,  "text/html; charset=utf-8", null);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	*/
}
