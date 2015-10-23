package com.stacks_on;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class StacksWebClient  extends WebViewClient {
	//possibly not needing this...
	private Activity activity = null;
	
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
		return true;
	}
}
