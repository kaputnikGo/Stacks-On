package com.stacks_on;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends FragmentActivity {

	private WebView webView;
	private AssetManager assetManager;
	private String urlSelectorString;
	private boolean JAVASCRIPT_ALLOW = false;
	private static final String MIMETYPE = "text/html; charset=utf-8";
	private static final String STYLES_FOLDER = "styles/";
	
	private static final String TAG = "MainActivity";
	/*
	 * using jsoup 1.8.3
	 * using joda-time 2.8.2
	 * 
	 */
	// display a complete entry, not saving in db, via the StacksWebClient - don't load the article url
	
	// can get a link for an iframe if javascript is off (link has domain name as indicator)
	
	// need to be able to back button to activity_main.xml view, we do have an actionbar...
	// i hate android
	
	// xml type entity &amp; is not rendered
	
	// net connect errors/status codes returned need an internal app solution to present to user, such as for 404, 500, 
	
	// need to be able to log-in via wp backend for access to paid user type content - this would need to be a separate activity/view/settings
	// settings view based upon wp-login.php page loaded via jsoup, app then saves cookie/login credentials
	// http://v2.wp-api.org/guide/authentication/
	// https://github.com/wordpress-mobile/WordPress-Android/
	
	
	// http://jdpgrailsdev.github.io/blog/2015/01/06/email_inline_style_jsoup_cssparser.html
	// https://github.com/blipinsk/FlippableStackView
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		assetManager = getAssets();
		setContentView(R.layout.activity_main);
		urlSelectorString = "not set";
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// back key and history check
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			backButtonRequest();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				scrollUpRequest();
			}
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				scrollDownRequest();
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
* 		// stacks nav and controls 
* 
*************************************************************/	
	
	//TODO
	protected void backButtonRequest() {
		// the proper android OS back button
		Log.d(TAG, "Back button pressed");
		if (webView.canGoBack()) {
			webView.goBack();
		}
	}
	
	protected void downStacksRequest() {
		// there may not be a downwards stack and the user may be annoying...
		Log.d(TAG, "Down stack pressed");
		//setContentView(R.layout.activity_main);
	}
	
	protected void upStacksRequest() {
		Log.d(TAG, "Up stack pressed");
	}
	
	protected void settingsRequest() {
		Log.d(TAG, "Settings pressed.");
	}
	
	protected void scrollUpRequest() {
		Log.d(TAG, "Scroll up (vol up) pressed.");
	}
	
	protected void scrollDownRequest() {
		Log.d(TAG, "Scroll down (vol down) pressed.");
	}
	
/************************************************************
* 
* 		// html doc methods 
* 
*************************************************************/	

	// load pages via the StacksWebClient
	@SuppressLint("SetJavaScriptEnabled")
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
			webSettings.setJavaScriptEnabled(JAVASCRIPT_ALLOW);
				
			StacksWebClient stacksWebClient = new StacksWebClient();
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
	
	public void parseRequestedHtml(Document doc) {
		// get jsoup to do some pruning here, article content, links and image(s) only
		Log.d(TAG,"parseRequestedHtml...");	
		Elements entryContent = null;
	
		if (doc != null) {			
			entryContent = doc.select("div.entry > p");
			if (entryContent.size() > 0 ) {
				entryContent = makeNiceContent(entryContent);
				webView.loadData(entryContent.html(), MIMETYPE, null);
				//System.out.println("Debug html: \n" + entryContent.html());
			}
			else {
				// connection but page returned has length of 0
				webView.loadData("Page is empty, entryContent is zero.", MIMETYPE, null);
			}
		}
		else {
			webView.loadData("doc is null.", MIMETYPE, null);
		}		
	}
	
	private Elements makeNiceContent(Elements elements) {
		Log.d(TAG, "makeNiceContent.");
		if (elements != null) {
			// add style sheet first
			elements = makeNiceHeader(elements);
			if (JAVASCRIPT_ALLOW == false) {
				// replace with the source link in case user wants to go there
				String source = "";
				String domain = "";

				int counter = 1; // for multiples
				for (Element elem : elements) {
					Log.d(TAG, "look for iframe...");
					source = elem.getElementsByTag("iframe").attr("src");
					domain = Utilities.getDomainFromUrl(source);					
					if (source.length() > 0) {
						Log.d(TAG, "found: " + source);
						elem.html("<a href=\'" + source + "\'> "+ domain + " link " + counter + "</a>");						
						source = "";
						domain = "";
						counter++;
					}
				}
			}			
			return elements;
		}
		return null;
	}
	
	private Elements makeNiceHeader(Elements elements) {
		Log.d(TAG, "makeNiceHeader.");
		if (elements != null) {
			// load page-style-01.css from Assets folder		
			elements.first().prepend("\n<style type=\"text/css\">" + 
					Utilities.getAssetsFileContent(assetManager, STYLES_FOLDER + "page-style-01.css") + "</style>\n");
			
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
	
	protected void toggleJavascriptAllow() {
		JAVASCRIPT_ALLOW ^= JAVASCRIPT_ALLOW;
	}
	
	
	protected boolean networkConnectionPresent() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
	}	
	
	/*
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
	*/
	
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
}
