package com.stacks_on;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;











import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

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
	
/* 
 * 
 * parse Jsoup doc into plaintext when we can't get our preferred elements  
 * thanks to jsoup geezer:
 * https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java 
 * 
 */	
	
	public static String getPlainText(Element element) {
		FormattingVisitor formatter = new FormattingVisitor();
		NodeTraversor traversor = new NodeTraversor(formatter);
		// walk the DOM, and call .head() and .tail() for each node
		traversor.traverse(element);
		
		return formatter.toString();
	}
	
	private static class FormattingVisitor implements NodeVisitor {
		private static final int maxWidth = 80;
		private int width = 0;
		private StringBuilder accum = new StringBuilder();
		
		// for first node
		public void head(Node node, int depth) {
			String name = node.nodeName();
			if (node instanceof TextNode)
				append(((TextNode) node).text());

			else if (name.equals("li"))
				append("\n * ");
			else if (name.equals("dt"))
				append("  ");
			else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
				append("\n");

		}
		// for last after all visited
		public void tail(Node node, int depth) {
			String name = node.nodeName();
			if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
				append("\n");
			else if (name.equals("a"))
				append(String.format(" <%s>", node.absUrl("href")));
		}
		
		
		// appends for StringBuilder
		private void append(String text) {		
			if (text.startsWith("\n"))
				width = 0;
			if (text.equals(" ") &&
					(accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
				return;
			
			if (text.length() + width > maxWidth) {
				String words[] = text.split("\\s+");
				for (int i = 0; i < words.length; i++) {
					String word = words[i];
					boolean last = i == words.length - 1;
					if (!last)
						word = word + " ";
					if (word.length() + width > maxWidth) {
						accum.append("\n").append(word);
						width = word.length();
					}
					else {
						accum.append(word);
						width += word.length();
					}
				}
			}
			else {
				accum.append(text);
				width += text.length();
			}
		}
		
		@Override
		public String toString() {
			return accum.toString();
		}
	}
}
