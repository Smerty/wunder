package com.smerty.android;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;

final public class WebFetch {

	static private final String TAG = WebFetch.class.getSimpleName();

	private WebFetch() {
		// TODO Auto-generated constructor stub
	}

	static public InputStream getInputStream(final String url) throws IOException {
		return getInputStream(url, null, "UTF-8");
	}

	static public InputStream getInputStream(final String url, final String agent, final String charset) throws IOException {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, charset);
		HttpProtocolParams.setUseExpectContinue(params, true);
		HttpProtocolParams.setHttpElementCharset(params, charset);
		if (agent != null && agent.length() > 0) {
			HttpProtocolParams.setUserAgent(params, agent);
		}
		return getInputStream(url, params);
	}

	static public InputStream getInputStream(final String url, final HttpParams params) throws IOException {
		Log.d(TAG, "Fetching Url: " + url);
		final DefaultHttpClient client = new DefaultHttpClient(params);
		final HttpGet method = new HttpGet(url);
		final HttpResponse res = client.execute(method);
		return res.getEntity().getContent();
	}

}
