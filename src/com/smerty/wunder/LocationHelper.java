package com.smerty.wunder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import android.app.ProgressDialog;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.widget.Toast;

import com.smerty.android.WebFetch;

public class LocationHelper implements LocationListener {

	static private final String TAG = LocationHelper.class.getSimpleName();

	private LocationManager locationManager;
	private String bestProvider;
	private Wunder wunderActivity;

	private static final String[] LOCATION_STATUS = { "Out of Service",
			"Temporarily Unavailable", "Available" };

	private AsyncTask<Location, Integer, Map<String, String>> localstationstask;

	public LocationHelper(LocationManager locationManager, Wunder wunderObj) {

		this.locationManager = locationManager;
		this.wunderActivity = wunderObj;

		final List<String> providers = locationManager.getAllProviders();
		for (String provider : providers) {
			printProvider(provider);
		}

		final Criteria criteria = new Criteria();
		bestProvider = locationManager.getBestProvider(criteria, false);
		Log.d(TAG, "BEST Provider:");
		printProvider(bestProvider);

		locationManager.requestLocationUpdates(bestProvider, 20000, 1, this);
	}

	public void onLocationChanged(Location location) {
		printLocation(location);

		if (location == null) {
			// Failed
			Toast.makeText(this.wunderActivity.getBaseContext(),
					R.string.toast_location_failure, Toast.LENGTH_SHORT).show();
			return;
		} else {

			if (this.localstationstask == null) {
				Log.d(TAG, "task was null, calling execute");
				this.localstationstask = new GetLocalStationsTask()
						.execute(location);
			} else {
				Status locationChangedStatus = this.localstationstask
						.getStatus();
				if (locationChangedStatus == Status.FINISHED) {
					Log
							.d(TAG,
									"task wasn't null, status finished, calling execute");
					this.localstationstask = new GetLocalStationsTask()
							.execute(location);
				}
			}

			locationManager.removeUpdates(this);

		}

	}

	public void onProviderDisabled(final String provider) {
		Log.d(TAG, "Provider Disabled: " + provider);
	}

	public void onProviderEnabled(final String provider) {
		Log.d(TAG, "Provider Enabled: " + provider);
	}

	public void onStatusChanged(final String provider, final int status,
			final Bundle extras) {
		Log.d(TAG, "Provider Status Changed: " + provider + ", Status="
				+ LOCATION_STATUS[status] + ", Extras=" + extras);
	}

	private void printProvider(final String provider) {
		LocationProvider info = locationManager.getProvider(provider);
		Log.d(TAG, info.toString());
	}

	private void printLocation(final Location location) {
		if (location == null) {
			Log.d(TAG, "Location[unknown]");
		} else {
			Log.d(TAG, location.toString());
		}
	}

	public Map<String, String> downloadLocalStations(final Location location) {

		final String url = "http://iphone.smerty.com/wunder/wx_near_all.php?lat="
				+ location.getLatitude() + "&lon=" + location.getLongitude();

		InputStream data;
		try {
			data = WebFetch.getInputStream(url, "java (wunder for android)",
					"UTF-8");

			Document doc = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;

			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse(data);
				// finish();
			} catch (SAXParseException e) {
				e.printStackTrace();
				Toast.makeText(this.wunderActivity.getBaseContext(),
						"SAXParseException", Toast.LENGTH_SHORT).show();
				return null;
				// finish();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				Toast.makeText(this.wunderActivity.getBaseContext(),
						"SAXException", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				return null;
				// finish();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				Toast.makeText(this.wunderActivity.getBaseContext(),
						"ParserConfigurationException", Toast.LENGTH_SHORT)
						.show();
				e.printStackTrace();
				return null;
			}

			if (doc == null) {
				// Toast.makeText(getBaseContext(), "doc is null",
				// Toast.LENGTH_SHORT).show();
				return null;
			} else {
				doc.getDocumentElement().normalize();
			}

			Map<String, String> stationMap = new HashMap<String, String>();

			try {

				int pwsStationCount = doc.getElementsByTagName("neighborhood")
						.getLength();

				for (int i = 0; i < pwsStationCount; i++) {

					String tmpPWSName;
					String tmpPWSID;

					try {
						tmpPWSName = doc.getElementsByTagName("neighborhood")
								.item(i).getChildNodes().item(0).getNodeValue()
								.replaceAll("\\s+", " ");
					} catch (NullPointerException e) {
						tmpPWSName = doc.getElementsByTagName("city").item(i)
								.getChildNodes().item(0).getNodeValue()
								.replaceAll("\\s+", " ");
					}
					tmpPWSID = doc.getElementsByTagName("id").item(i)
							.getChildNodes().item(0).getNodeValue();

					if (tmpPWSID != null && tmpPWSName != null
							& tmpPWSID.length() > 0 && tmpPWSName.length() > 0) {
						stationMap.put(tmpPWSID, tmpPWSName);
					} else {
						Log.d(TAG, "didn't put station in map");
					}
				}

			} catch (Exception e) {
				Log.d(TAG, e.toString() + " / " + e.getMessage());
				return null;
				// do nothing
			}
			return stationMap;

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

	}

	private class GetLocalStationsTask extends
			AsyncTask<Location, Integer, Map<String, String>> {

		private LocationHelper that;

		protected Map<String, String> doInBackground(
				final Location... locations) {

			this.that = LocationHelper.this;

			Map<String, String> stations = null;

			publishProgress(0);

			try {
				stations = that.downloadLocalStations(locations[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}

			publishProgress(100);

			return stations;
		}

		protected void onProgressUpdate(final Integer... progress) {
			Log.d(TAG, progress[0].toString());
			if (progress[0] == 0) {
				Wunder.progressDialog = ProgressDialog.show(
						that.wunderActivity, that.wunderActivity.getResources()
								.getText(R.string.app_name),
						that.wunderActivity.getResources().getText(
								R.string.message_download_local_progress),
						true, false);
			}
			if (progress[0] == 100) {
				Wunder.progressDialog.dismiss();
			}

		}

		protected void onPostExecute(final Map<String, String> result) {
			that.wunderActivity.processLocalStations(result);
		}
	}

}
