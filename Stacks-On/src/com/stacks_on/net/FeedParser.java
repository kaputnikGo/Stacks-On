package com.stacks_on.net;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class FeedParser {

	//XML element names
	private static final int TAG_ID = 1;
	private static final int TAG_TITLE = 2;
	private static final int TAG_PUBLISHED = 3;
	private static final int TAG_LINK = 4;
	// no XML namespaces
	private static final String ns = null;
	
	// parse an Atom feed for Entry objects
	public List<Entry> parse(InputStream in) throws XmlPullParserException, IOException, ParseException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return readFeed(parser);
		}
		finally {
			in.close();
		}
	}
	
	//decode an XmlPullParser feed
	private List<Entry> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		List<Entry> entries = new ArrayList<Entry>();
		// look for <feed> tags
		parser.require(XmlPullParser.START_TAG,  ns,  "feed");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// look for <entry> tag that starts each article in feed
			if (name.equals("entry")) {
				entries.add(readEntry(parser));
			}
			else {
				skip(parser);
			}
		}
		return entries;
	}
	
	// parse content of an Entry
	private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		parser.require(XmlPullParser.START_TAG, ns, "entry");
		String id = null;
		String title = null;
		String link = null;
		long publishedOn = 0;
		
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("id")) {
				// Example: <id>urn:uuid:218AC159-7F68-4CC6-873F-22AE6017390D</id>
				id = readTag(parser, TAG_ID);
			}
			else if (name.equals("title")) {
				// Example: <title>Article title</title>
				// use jsoup for entities
				title = Jsoup.parse(readTag(parser, TAG_TITLE)).select("body").html();
			}
			else if (name.equals("link")) {
				// Example: <link rel="alternate" type="text/html" href="http://example.com/article/1234"/>
				// Multiple link types can be included. readAlternateLink() will only return
                // non-null when reading an "alternate"-type link. Ignore other responses.
				String tempLink = readTag(parser, TAG_LINK);
				if (tempLink != null) {
					link = tempLink;
				}
			}
			else if (name.equals("published")) {
				// Example: <published>2003-06-27T12:00:00Z</published>
				DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
				publishedOn = formatter.parseMillis(readTag(parser, TAG_PUBLISHED));
			}
			else {
				skip(parser);
			}				
		}
		return new Entry(id, title, link, publishedOn);
	}
	
	// process tag and get value
	private String readTag(XmlPullParser parser, int tagType) throws IOException, XmlPullParserException {
		//String tag= null;
		//String endTag = null;
		
		switch (tagType) {
		case TAG_ID:
			return readBasicTag(parser, "id");
		case TAG_TITLE:
			return readBasicTag(parser, "title");
		case TAG_PUBLISHED:
			return readBasicTag(parser, "published");
		case TAG_LINK:
			return readAlternateLink(parser);
		default:
			throw new IllegalArgumentException("Unknown tag type: " + tagType);	
		}
	}
	
	// read body of basic XML tag, no nested elements should be found
	private String readBasicTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, tag);
		String result = readText(parser);
		parser.require(XmlPullParser.END_TAG,  ns,  tag);
		return result;
		
	}
	
	//process link tags in feed
	private String readAlternateLink(XmlPullParser parser) throws IOException, XmlPullParserException {
		String link = null;
		parser.require(XmlPullParser.START_TAG,  ns, "link");
		//String tag = parser.getName();
		String relType = parser.getAttributeValue(null, "rel");
		if (relType.equals("alternate")) {
			link = parser.getAttributeValue(null, "href");
		}
		while (true) {
			if (parser.nextTag() == XmlPullParser.END_TAG) 
				break;
			// intentionally break here, consume remaining sub-tags
		}
		return link;
	}
	
	// get title and summary text values
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = null;
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
	
	// skip tags we don't want, uses depth to handle nested tags
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
			}
		}
	}
	
	// Entry class for single entry(post) in feed
	public static class Entry {
		public final String id;
		public final String title;
		public final String link;
		// make this a DateTime, or a String instead?
		public final long published;
		
		Entry(String id, String title, String link, long published) {
			this.id = id;
			this.title = title;
			this.link = link;
			this.published = published;
		}
	}
}
