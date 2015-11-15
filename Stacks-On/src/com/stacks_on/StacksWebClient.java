package com.stacks_on;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class StacksWebClient extends WebViewClient {
	//private View view;
	private StacksWebConnector stacksWebConnector;
	private WebView webView;
	private AssetManager assetManager;
	
	private boolean JAVASCRIPT_ALLOW = false;
	private static final int CONNECTION_TIMEOUT = 15000; //15 secs, bit long...?
	private static final String MIMETYPE = "text/html; charset=utf-8";
	private static final String ENCODING = "UTF-8";
	private static final String STYLES_FOLDER = "styles/";
	private static final String TAG = "StacksWebClient";
	
	public StacksWebClient(AssetManager assetManager, WebView webView) {
		this.webView = webView;
		this.assetManager = assetManager;
	}
	
	public void initialRequest(String url) {
		// first request from mainActivity
		Log.d(TAG, "initialRequest.");
		stacksConnectExecute(url);
	}
	
	public void stacksConnectExecute(String url) {
		Log.d(TAG, "stacksConnectExecute");
		stacksWebConnector = new StacksWebConnector(this, url);
		stacksWebConnector.execute();	
	}
	
	public void processJsoupDoc(Document doc) {
		// called from StacksWebConnector after it has retrieved doc from url
		parseRequestedHtml(doc);
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView webView, String url) {		
		// return true means the host application handles the url
		// return false means the current WebView handles the url.

		if(Uri.parse(url).getHost().equals("dailyreview.com.au")) {
			Log.d(TAG, "should override url our host.");
			stacksConnectExecute(url);
			return false;
		}
		// experiment with always using our client
		Log.d(TAG, "should override url any, call the connectExecute.");
		stacksConnectExecute(url);
		return false;
	}
	
	@Override
	public void onLoadResource(WebView webView, String url) {
		// notify host app that we will load the url and/or data:text/html
		// here is called after jsoup parsing,
		// it draws the resource to our webView client.
		
		// need to stop this drawing to the screen, or it's associated view.loadUrl(url) drawing to screen.
		Log.d(TAG, "onLoadResource url.");
	}
	
	@Override
	public WebResourceResponse shouldInterceptRequest(WebView webView, String url) {
		return null;
	}
	
	@Override
	public void onPageFinished(WebView webView, String url) {
		Log.d(TAG, "onPageFinished url: " + url);
	}
	
	@Override
	public boolean shouldOverrideKeyEvent(WebView webView, KeyEvent event) {
		Log.d(TAG, "should override keyEvent.");
		return false;
	}
	
	
/************************************************************
* 
* 	// webview rendering methods using jsoup 
* 
*************************************************************/	
	
	private void parseRequestedHtml(Document doc) {
		// get jsoup to do some pruning here, article content, links and image(s) only
		Log.d(TAG, "parseRequestedHtml...");	
		Elements entryContent = null;
	
		if (doc != null) {	
			entryContent = doc.select("div.entry > p");
			if (entryContent.size() <=0) {
				// check for a non-daily_review page
				Log.d(TAG, "entryContent size <= 0.");
				entryContent = getTextFromDoc(doc);
			}

			if (entryContent.size() > 0 ) {
				Log.d(TAG, "entryContent size: " + entryContent.size());
				
				entryContent = makeNiceContent(entryContent);
				webView.loadData(entryContent.html(), MIMETYPE, null);
				
				Log.d(TAG, "parseReq, webview loadData.");
				// this calls onLoadResource();				
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
	
	private Elements getTextFromDoc(Document doc) {
		// get all text (visible) from a webpage with no <p> tags for instance - hi guardian.
		// some guardian pages have no p tags in their content 
		// - they are essentially links to articles
		// they have a hrefs to articles...
		Log.d(TAG, "getTextFromDoc.");
		Elements textElements = doc.getElementsByTag("p");
		
		if (textElements.size() <= 0 ) {
			Log.d(TAG, "elements tag p <= 0.");
			// assume no p tags but maybe some text, somewhere.
			//String docBodyText = Utilities.getPlainText(doc.body());
			Document parseDoc = Jsoup.parse(doc.body().html());
			String docBodyText = parseDoc.text();
			// this gets a singular element - need to <p> tag it in the Utilities...
			// this also makes a string of it all, no element tags can be found via Jsoup anymore...
			// also is a pita.
 			
			if (docBodyText == null) {
				// scream
				Log.d(TAG, "docBodyText is a null.");
				docBodyText = "it is behind you.";
			}
			else {
				textElements.add(new Element(Tag.valueOf("p"), docBodyText));
				//wow
				textElements.select("p").first().text(docBodyText);
			}
		}
		else {
			Log.d(TAG, "found tags p, size: " + textElements.size());
		}
		return textElements;
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
	
	private void debugElements(Elements elements) {
		for (Element element : elements) {
			Log.d(TAG, "e: " + element.toString());
		}		
	}
	
/********************************
 * 
 * 
 * 
 * 
 * 
 * *****************************/	

	// or add this: webView.setOnTouchListener(new View.OnTouchListener() {
	// 		@Override onTouch method then goes here...
	// }
	// as per: http://stackoverflow.com/questions/5116909/how-i-can-get-onclick-event-on-webview-in-android
	
	/*
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		final int RELEASED = 0;
		final int TOUCHED = 1;
		final int DRAGGING = 2;
		final int UNDEFINED = 3;
		int touchState = RELEASED;
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (touchState == RELEASED)
					touchState = TOUCHED;
				else
					touchState = UNDEFINED;
				break;
			case MotionEvent.ACTION_UP:
				if (touchState != DRAGGING) {
					touchState = RELEASED;
					view.performClick();
					// respond with getting link url and load
					// need to get clicked url, from jsoup?
					Log.d(TAG, "clickety-click here.");
				}
				else if (touchState == DRAGGING)
					touchState = RELEASED;
				else
					touchState = UNDEFINED;
				break;
			case MotionEvent.ACTION_MOVE:
				if (touchState == TOUCHED || touchState == DRAGGING)
					touchState = DRAGGING;
				else
					touchState = UNDEFINED;
				break;
			default:
					touchState = UNDEFINED;
		}
		return false;
	}
	*/
	
	/*
	public void scrollView(int direction) {
		//method to receive volume rocker input for scroll, or page-down equivalent
		
		// http://developer.android.com/reference/android/view/View.html#scrollBy%28int,%20int%29
		
		int x = 0; // should not be necessary
		int y= 800; // screen height
		
		view.scrollTo(x, y);
		view.scrollBy(x, y);
	}
	*/
}
