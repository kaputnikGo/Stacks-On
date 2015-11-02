package com.stacks_on;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class StacksWebClient  extends WebViewClient {
	private View view;
	private static final String TAG = "StacksWebClient";
	
	public StacksWebClient() {
		// 
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		// only this domain opens with the StacksWebClient
		if(Uri.parse(url).getHost().equals("dailyreview.com.au")) {
			return false;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		view.getContext().startActivity(intent);
		this.view = view;
		return true;
	}
	
	// or add this: webView.setOnTouchListener(new View.OnTouchListener() {
	// 		@Override onTouch method then goes here...
	// }
	// as per: http://stackoverflow.com/questions/5116909/how-i-can-get-onclick-event-on-webview-in-android
	
	public boolean onTouch(View view, MotionEvent event) {
		Log.e(TAG, "touch the webview");
		
		return false;
	}
	
	public void scrollView(int direction) {
		//method to receive volume rocker input for scroll, or page-down equivalent
		
		// http://developer.android.com/reference/android/view/View.html#scrollBy%28int,%20int%29
		
		int x = 0; // should not be necessary
		int y= 800; // screen height
		
		view.scrollTo(x, y);
		view.scrollBy(x, y);
	}
}
