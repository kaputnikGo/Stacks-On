package com.stacks_on;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class StacksWebClient  extends WebViewClient {
	//possibly not needing this...
	private Activity activity = null;
	private View view;
	
	public StacksWebClient(Activity activity) {
		this.activity = activity;
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
	
	public void scrollView(int direction) {
		//method to receive volume rocker input for scroll, or page-down equivalent
		
		// http://developer.android.com/reference/android/view/View.html#scrollBy%28int,%20int%29
		
		int x = 0; // should not be necessary
		int y= 800; // screen height
		
		view.scrollTo(x, y);
		view.scrollBy(x, y);
	}
}
