package com.smerty.android;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import android.util.Log;

public class DocumentHelper {
	
	static private final String TAG = DocumentHelper.class.getSimpleName();

	public static Document getDocument(final InputStream inStream) {
		Document doc = null;
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder documentBuilder;

		try {
			documentBuilder = dbFactory.newDocumentBuilder();
			doc = documentBuilder.parse(inStream);
			doc.getDocumentElement().normalize();
		} catch (SAXParseException e) {
			Log.d(TAG, e.getMessage(), e);
			//doc = null;
		} catch (SAXException e) {
			Log.d(TAG, e.getMessage(), e);
			//doc = null;
		} catch (ParserConfigurationException e) {
			Log.d(TAG, e.getMessage(), e);
			//doc = null;
		} catch (IOException e) {
			Log.d(TAG, e.getMessage(), e);
			//doc = null;
		}
		return doc;
		
	}
	
}
