package com.stacks_on;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends FragmentActivity {

	private WebView webView;
	private String urlSelectorString;
	private static final String STYLES_FOLDER = "styles/";
	
	private static final String TAG = "MainActivity";
	/*
	 * using jsoup 1.8.3
	 * using joda-time 2.8.2
	 * 
	 */
	// display a complete entry, not saving in db, via the StacksWebClient - don't load the article url
	
	// need to be able to back button to activity_main.xml view, we do have an actionbar...
	
	// xml type entity &amp; is not rendered
	
	// need to be able to log-in via wp backend for access to paid user type content - this would need to be a separate activity/view/settings
	// settings view based upon wp-login.php page loaded via jsoup, app then saves cookie/login credentials
	// http://v2.wp-api.org/guide/authentication/
	// https://github.com/wordpress-mobile/WordPress-Android/
	
	
	// http://jdpgrailsdev.github.io/blog/2015/01/06/email_inline_style_jsoup_cssparser.html
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		urlSelectorString = "not set";
	}
	
	// load pages via the StacksWebClient
	public void loadSelectedArticle(String articleUrlString) {
		// already checked in EntryListFragment...
		if (!Utilities.checkString(articleUrlString)) {
			Log.e(TAG, "No url to load.");
			return;
		}
		// look out, needs true and false
		if (Utilities.isValidUrl(urlSelectorString) && !Utilities.checkWebsiteUp(urlSelectorString)) {
			Log.e(TAG, "Cannot connect to: " + urlSelectorString);
		}
		else {
			// assign to accessible string
			urlSelectorString = articleUrlString;
			setContentView(R.layout.web_view_layout);
			webView = (WebView) findViewById(R.id.webview);
			WebSettings webSettings = webView.getSettings();
				
			//webSettings.setJavaScriptEnabled(true);
			webSettings.setJavaScriptEnabled(false); // turn it off
				
			StacksWebClient stacksWebClient = new StacksWebClient(this);
			webView.setWebViewClient(stacksWebClient);
			
			Log.d(TAG, "loading async via jsoup...");
			if (networkConnectionPresent()) {
				StacksWebConnector stacksWebConnector = new StacksWebConnector(this, urlSelectorString);		
				stacksWebConnector.execute();
			}
			else {
				Log.e(TAG, "No network connection found.");
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// back key and history check
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		//TODO
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				// scroll page up
				// or some other useful method for StacksWeb navigation
			}
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				// scroll page up
				// or some other useful method for StacksWeb navigation
			}
			return true;
		}		
		// default
		return super.onKeyDown(keyCode, event);
	}	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	
/************************************************************
* 
* 		// html doc methods 
* 
*************************************************************/	

	public void parseRequestedHtml(Document doc) {
		// get jsoup to do some pruning here, article content, links and image(s) only
		Log.d(TAG,"parseRequestedHtml...");
		Elements entryContent = null;
		if (doc != null) {			
			entryContent = doc.select("div.entry > p");
			if (entryContent.size() > 0 ) {
				// if we have content elements, add our nice header, make a whole head...
				entryContent = makeNiceHeader(entryContent);
				webView.loadData(entryContent.html(), "text/html; charset=utf-8", null);
				//System.out.println("Debug html: \n" + entryContent.html());
			}
			else {
				webView.loadData("entryContent is zero.", "text/html; charset=utf-8", null);
			}
		}
		else {
			webView.loadData("doc is null.", "text/html; charset=utf-8", null);
		}
	}
	
	private Elements makeNiceHeader(Elements elements) {
		Log.d(TAG, "makeNiceHeader.");
		if (elements != null) {
			// load page-style-01.css from Assets folder		
			elements.first().prepend("\n<style type=\"text/css\">" + getAssetsFile("page-style-01.css") + "</style>\n");
			// since wp adds new lines for every <p> tag, we need to provide the same
			elements.append("<br /><br />");
			return elements;
		}
		return null;
	}
	
	
/************************************************************
* 
* 		// utilities 
* 
*************************************************************/	
	
	protected boolean networkConnectionPresent() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
	}	
	
	private String getAssetsFile(String filename) {
		// check the filename lives in assets/styles folder,
		// has a .css extension
		//TODO
		StringBuilder stringBuilder = new StringBuilder();
		if (!Utilities.checkString(filename)) {
			return "getAccessFile: no filename.";			
		}		
		else {
			String path = STYLES_FOLDER +  filename;				
			try {
				InputStream inputStream = getAssets().open(path);
			    if (inputStream == null) {
			    	Log.e(TAG, "Filename path inputStream is null: " + path);
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
				Log.e(TAG, "IO error accessing path: " + path);
				return "getAccessFile: access error.";
			}
		}
		return stringBuilder.toString();
	}
	
	/*
	 * keeping for archival 
	 * 
	private Elements makeNiceElements(Elements elements) {
		// resize any images to our device (site has 500x500)
		Log.d(TAG, "makeNiceElements.");
		elements.select("p").attr("style", "font-family:Georgia, serif;width:80%;margin:24px auto;");
		elements.select("img").attr("style", "display:block;width:120px;height:auto;padding: 24px 0;margin: 24px auto;");
		elements.select("a").attr("style", "display:block;border:2px solid blue;border-radius:5px;width:70%;text-align:center;padding:6px;margin:12px auto;background-color:#cccccc;");
		return elements;
	}
	*/
	
	/*
	 * keeping for archival
	 * 
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (action == KeyEvent.ACTION_DOWN) {
					// scroll page up
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == KeyEvent.ACTION_DOWN) {
					// scroll page down
				}
				return true;
			default:
				return super.dispatchKeyEvent(event);
		}
	}
	*/
			
}
