package com.smerty.wunder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class Wunder extends Activity implements LocationListener {

	private static final String PREFS_NAME = "WunderPrefs";
	private static final String DEF_STATION_ID = "KCALIVER14";
	private LocationManager locationManager;
	private String bestProvider;
	private static final String[] LOCATION_STATUS = { "Out of Service",
			"Temporarily Unavailable", "Available" };
	private String selectedID = "";

	private ScrollView scrollView;
	private TableLayout tableLayout;

	private AsyncTask<Wunder, Integer, Integer> updatetask;
	private AsyncTask<Location, Integer, Map<String, String>> localstationstask;
	public ProgressDialog progressDialog;

	private WeatherReport conds;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// might be a memory leak on rotation since onCreate will be called
		// again
		scrollView = new ScrollView(this);
		tableLayout = new TableLayout(this);

		tableLayout.setShrinkAllColumns(true);

		if (this.updatetask == null) {
			Log.d("startDownloading", "task was null, calling execute");
			this.updatetask = new UpdateFeedTask().execute(this);
		} else {
			final Status updateTaskStatus = this.updatetask.getStatus();
			if (updateTaskStatus == Status.FINISHED) {
				Log.d("updatetask",
						"task wasn't null, status finished, calling execute");
				this.updatetask = new UpdateFeedTask().execute(this);
			}
		}

		scrollView.addView(tableLayout);
		setContentView(scrollView);
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (scrollView != null) {
			setContentView(scrollView);
		}
	}

	private class UpdateFeedTask extends AsyncTask<Wunder, Integer, Integer> {

		private Wunder that;

		protected Integer doInBackground(final Wunder... thats) {

			if (that == null) {
				this.that = thats[0];
			}

			publishProgress(0);

			try {
				that.conds = that.getWeather();
			} catch (Exception e) {
				that.conds = null;
			}

			publishProgress(100);

			return 0;
		}

		protected void onProgressUpdate(final Integer... progress) {
			Log.d("onProgressUpdate", progress[0].toString());
			if (progress[0] == 0) {
				that.progressDialog = ProgressDialog.show(that, that
						.getResources().getText(R.string.app_name), that
						.getResources().getText(
								R.string.message_download_weather_progress),
						true, false);
			}
			if (progress[0] == 100) {
				that.progressDialog.dismiss();
			}

		}

		protected void onPostExecute(final Integer result) {
			// Log.d("onPostExecute", that.getApplicationInfo().packageName);
			that.allTogether();

		}
	}

	private class GetLocalStationsTask extends
			AsyncTask<Location, Integer, Map<String, String>> {

		private Wunder that;

		protected Map<String, String> doInBackground(final Location... locations) {

			this.that = Wunder.this;

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
			Log.d("onProgressUpdate", progress[0].toString());
			if (progress[0] == 0) {
				that.progressDialog = ProgressDialog.show(that, that
						.getResources().getText(R.string.app_name), that
						.getResources().getText(
								R.string.message_download_local_progress),
						true, false);
			}
			if (progress[0] == 100) {
				that.progressDialog.dismiss();
			}

		}

		protected void onPostExecute(final Map<String, String> result) {
			// Log.d("onPostExecute", that.getApplicationInfo().packageName);
			that.processLocalStations(result);
		}
	}

	public void allTogether() {

		final Wunder that = this;

		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		final boolean usemetric = settings.getBoolean("useMetric", false);

		TableRow row = new TableRow(that);
		TextView text = new TextView(that);
		if (conds == null) {
			text.setText(R.string.message_no_weather);
		} else {
			text.setText(conds.neighborhood);
		}
		text.setTextSize(24);
		row.setPadding(3, 3, 3, 3);
		row.setBackgroundColor(Color.argb(200, 51, 51, 51));
		row.addView(text);
		tableLayout.addView(row);

		row = new TableRow(that);
		row.setPadding(3, 1, 3, 1);
		row.setBackgroundColor(Color.argb(200, 80, 80, 80));
		tableLayout.addView(row);

		if (conds == null) {
				row = new TableRow(that);
				text = new TextView(that);
				text.setText(R.string.message_data_not_available);
				text.setTextSize(14);
				text.setGravity(1);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
		}
		else {
			if (conds.temperature != null && conds.temperature.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.temperature = String
							.valueOf(Math
									.floor((Double
											.parseDouble(conds.temperature) - 32) * 5 / 9 * 10) / 10);
					text.setText(that.getResources().getText(R.string.temperature) + ": \t\t\t\t\t" + conds.temperature
							+ "C");
				} else {
					text.setText(that.getResources().getText(R.string.temperature) + ": \t\t\t\t\t" + conds.temperature
							+ "F");
				}
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.dewpoint != null && conds.dewpoint.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.dewpoint = String
							.valueOf(Math
									.floor((Double.parseDouble(conds.dewpoint) - 32) * 5 / 9 * 10) / 10);
					text.setText(that.getResources().getText(R.string.dewpoint) + ": \t\t\t\t\t\t" + conds.dewpoint
							+ "C");
				} else {
					text.setText(that.getResources().getText(R.string.dewpoint) + ": \t\t\t\t\t\t" + conds.dewpoint
							+ "F");
				}
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.humidity != null && conds.humidity.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				text
						.setText(that.getResources().getText(R.string.relative_humidity) + ": \t\t\t" + conds.humidity
								+ "%");
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.winddirection != null && conds.winddirection.length() > 0
					&& conds.windspeed != null && conds.windspeed.length() > 0
					&& Double.valueOf(conds.windspeed) > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				text.setText(that.getResources().getText(R.string.wind_direction) + ": \t\t\t\t" + conds.winddirection);
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.windspeed != null && conds.windspeed.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.windspeed = String
							.valueOf(Math
									.floor((Double.parseDouble(conds.windspeed)) * 1.609344 * 10) / 10);
					text.setText(that.getResources().getText(R.string.wind_speed) + ": \t\t\t\t\t" + conds.windspeed
							+ " km/h");
				} else {
					text.setText(that.getResources().getText(R.string.wind_speed) + ": \t\t\t\t\t" + conds.windspeed
							+ " mph");
				}
				// }
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.pressure != null && conds.pressure.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.pressure = String
							.valueOf(Math
									.floor((Double.parseDouble(conds.pressure)) * 33.86389 * 10) / 10);
					text.setText(that.getResources().getText(R.string.barometric_pressure) + ": \t\t" + conds.pressure
							+ " mb");
				} else {
					text.setText(that.getResources().getText(R.string.barometric_pressure) + ": \t\t" + conds.pressure
							+ " in");
				}
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.solarradiation != null
					&& conds.solarradiation.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				text.setText(that.getResources().getText(R.string.solar_radiation) + ": \t\t\t\t" + conds.solarradiation
						+ " w/m^2");
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.uv != null && conds.uv.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				text.setText(that.getResources().getText(R.string.uv_index) + ": \t\t\t\t\t\t" + conds.uv);
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.precipitation1hr != null
					&& conds.precipitation1hr.length() > 0
					&& Double.valueOf(conds.precipitation1hr) > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.precipitation1hr = String
							.valueOf(Math
									.floor((Double
											.parseDouble(conds.precipitation1hr)) * 25.4 * 10) / 10);
					text.setText(that.getResources().getText(R.string.precipitation_1hr) + ": \t\t"
							+ conds.precipitation1hr + " mm");
				} else {
					text.setText(that.getResources().getText(R.string.precipitation_1hr) + ": \t\t"
							+ conds.precipitation1hr + " in");
				}
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.precipToday != null
					&& conds.precipToday.length() > 0
					&& Double.valueOf(conds.precipToday) > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.precipToday = String
							.valueOf(Math
									.floor((Double
											.parseDouble(conds.precipToday)) * 25.4 * 10) / 10);
					text.setText(that.getResources().getText(R.string.precipitation_today) + ": \t\t"
							+ conds.precipToday + " mm");
				} else {
					text.setText(that.getResources().getText(R.string.precipitation_today) + ": \t\t"
							+ conds.precipToday + " in");
				}
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			row = new TableRow(that);
			row.setPadding(3, 3, 3, 3);
			row.setBackgroundColor(Color.argb(200, 51, 51, 51));
			tableLayout.addView(row);

			row = new TableRow(that);
			row.setPadding(3, 1, 3, 1);
			row.setBackgroundColor(Color.argb(200, 80, 80, 80));
			tableLayout.addView(row);

			row = new TableRow(that);
			text = new TextView(that);
			text.setText(conds.updated);
			text.setTextSize(14);
			text.setGravity(2);
			row.setPadding(3, 3, 3, 3);
			row.addView(text);
			tableLayout.addView(row);

			row = new TableRow(that);
			text = new TextView(that);
			text.setText(R.string.weathersource);
			text.setTextSize(14);
			text.setGravity(2);
			row.setPadding(3, 3, 3, 3);
			row.addView(text);
			tableLayout.addView(row);

		}

	}

	public static final int MENU_ABOUT = 10;
	public static final int MENU_QUIT = 11;
	public static final int MENU_REFRESH = 12;
	public static final int MENU_SETTINGS = 13;
	public static final int MENU_GEO = 14;

	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh);
		menu.add(0, MENU_GEO, 0, R.string.menu_nearby);
		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings);
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		menu.add(0, MENU_QUIT, 0, R.string.menu_quit);
		return true;
	}

	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ABOUT:
			Toast.makeText(getBaseContext(), R.string.developed_by,
					Toast.LENGTH_LONG).show();
			return true;
		case MENU_GEO:
			geoHelper();
			return true;
		case MENU_REFRESH:
			this.onCreate(null);
			return true;
		case MENU_SETTINGS:

			final AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.pws_station_id);

			final EditText input = new EditText(this);

			final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			final String pwsid = settings.getString("stationID", DEF_STATION_ID);
			final boolean useMetric = settings.getBoolean("useMetric", false);

			input.setSingleLine(true);

			final InputFilter filter = new InputFilter() {
				public CharSequence filter(final CharSequence source, final int start,
						final int end, final Spanned dest, final int dstart, final int dend) {
					for (int i = start; i < end; i++) {
						if (!Character.isLetterOrDigit(source.charAt(i))) {
							return "";
						}
					}
					return null;
				}
			};

			input.setText(pwsid);
			input.setFilters(new InputFilter[] { filter });

			final LinearLayout alertLayout = new LinearLayout(this);

			alertLayout.setOrientation(1);

			final LinearLayout metricCheckLayout = new LinearLayout(this);
			metricCheckLayout.setOrientation(0);

			final CheckBox metricCheck = new CheckBox(this);
			metricCheck.setChecked(useMetric);

			metricCheckLayout.addView(metricCheck);
			final TextView metricText = new TextView(this);

			metricText.setText(R.string.use_metric);
			metricText.setTextSize(14);
			metricText.setGravity(2);

			metricCheckLayout.addView(metricText);

			alertLayout.addView(input);
			alertLayout.addView(metricCheckLayout);

			alert.setView(alertLayout);

			final Wunder that = this;

			alert.setPositiveButton(R.string.button_set,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							final String value = input.getText().toString().trim()
									.toUpperCase();

							final SharedPreferences settings = getSharedPreferences(
									PREFS_NAME, 0);
							final String pwsid = settings.getString("stationID",
									DEF_STATION_ID);

							final SharedPreferences.Editor editor = settings.edit();
							editor.putString("stationID", value);
							editor.putBoolean("useMetric", metricCheck
									.isChecked());
							editor.commit();

							if (!pwsid.equalsIgnoreCase(value)) {
								Toast.makeText(getBaseContext(),
										"Changed: " + value, Toast.LENGTH_LONG)
										.show();

							}

							that.onCreate(null);

						}
					});

			alert.setNegativeButton(R.string.button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int whichButton) {
							// canceled
						}
					});

			alert.setIcon(R.drawable.icon);

			alert.show();

			return true;
		case MENU_QUIT:
			finish();
			return true;
		default:
			return false;
		}
	}

	public class WeatherReport {
		public String stationID, neighborhood, updated, temperature, humidity,
				winddirection, windspeed, pressure, dewpoint, heatindex,
				windchill, solarradiation, uv, precipitation1hr,
				precipToday;
	}

	public WeatherReport getWeather() {

		WeatherReport retval = new WeatherReport();

		try {

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, "UTF-8");
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
			HttpProtocolParams.setUserAgent(params, "java");

			final DefaultHttpClient client = new DefaultHttpClient(params);

			InputStream data = null;

			String pwsid = null;

			try {

				final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				pwsid = settings.getString("stationID", DEF_STATION_ID);

				HttpGet method = new HttpGet(
						"http://api.wunderground.com/weatherstation/WXCurrentObXML.asp?ID="
								+ pwsid);
				HttpResponse res = client.execute(method);
				data = res.getEntity().getContent();

			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(getBaseContext(), R.string.toast_network_failure,
						Toast.LENGTH_SHORT).show();

				return null;
			}

			Document doc = null;
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;

			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse(data);
				// finish();
			} catch (SAXParseException e) {
				e.printStackTrace();
				Toast.makeText(getBaseContext(), "SAXParseException",
						Toast.LENGTH_SHORT).show();
				return null;
				// finish();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				Toast.makeText(getBaseContext(), "SAXException",
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				return null;
				// finish();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				Toast.makeText(getBaseContext(),
						"ParserConfigurationException", Toast.LENGTH_SHORT)
						.show();
				e.printStackTrace();
				return null;
			}

			doc.getDocumentElement().normalize();

			try {
				retval.solarradiation = doc.getElementsByTagName(
						"solar_radiation").item(0).getChildNodes().item(0)
						.getNodeValue();
				retval.uv = doc.getElementsByTagName("UV").item(0)
						.getChildNodes().item(0).getNodeValue();
			} catch (Exception e) {
				// do nothing
			}

			try {
				retval.precipitation1hr = doc.getElementsByTagName(
						"precip_1hr_in").item(0).getChildNodes().item(0)
						.getNodeValue();
				retval.precipToday = doc.getElementsByTagName(
						"precip_today_in").item(0).getChildNodes().item(0)
						.getNodeValue();
			} catch (Exception e) {
				// do nothing
			}

			try {
				if (doc.getElementsByTagName("neighborhood").item(0)
						.hasChildNodes()) {
					retval.neighborhood = doc.getElementsByTagName(
							"neighborhood").item(0).getChildNodes().item(0)
							.getNodeValue();
				} else {
					if (doc.getElementsByTagName("city").item(0)
							.hasChildNodes()) {
						retval.neighborhood = doc.getElementsByTagName("city")
								.item(0).getChildNodes().item(0).getNodeValue();
						if (doc.getElementsByTagName("state").item(0)
								.hasChildNodes()) {
							retval.neighborhood += ", "
									+ doc.getElementsByTagName("state").item(0)
											.getChildNodes().item(0)
											.getNodeValue();
						}
					} else {
						retval.neighborhood = pwsid;
					}
				}
			} catch (Exception e) {
				retval.neighborhood = pwsid;
			}

			try {
				retval.updated = doc.getElementsByTagName("observation_time")
						.item(0).getChildNodes().item(0).getNodeValue();
				retval.temperature = doc.getElementsByTagName("temp_f").item(0)
						.getChildNodes().item(0).getNodeValue();
				retval.humidity = doc.getElementsByTagName("relative_humidity")
						.item(0).getChildNodes().item(0).getNodeValue();
				retval.winddirection = doc.getElementsByTagName("wind_dir")
						.item(0).getChildNodes().item(0).getNodeValue();
				retval.windspeed = doc.getElementsByTagName("wind_mph").item(0)
						.getChildNodes().item(0).getNodeValue();
				retval.dewpoint = doc.getElementsByTagName("dewpoint_f")
						.item(0).getChildNodes().item(0).getNodeValue();
				retval.pressure = doc.getElementsByTagName("pressure_in").item(
						0).getChildNodes().item(0).getNodeValue();
			} catch (Exception e) {
				return null;
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return retval;
	}

	public void geoHelper() {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		final List<String> providers = locationManager.getAllProviders();
		for (String provider : providers) {
			printProvider(provider);
		}

		final Criteria criteria = new Criteria();
		bestProvider = locationManager.getBestProvider(criteria, false);
		Log.d("WUNDERLOC", "BEST Provider:");
		printProvider(bestProvider);

		locationManager.requestLocationUpdates(bestProvider, 20000, 1, this);
	}

	/** Register for the updates when Activity is in foreground */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
	}

	public Map<String, String> downloadLocalStations(final Location location) {

		try {
			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, "UTF-8");
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
			HttpProtocolParams
					.setUserAgent(params, "java (wunder for android)");

			DefaultHttpClient client = new DefaultHttpClient(params);

			InputStream data = null;

			try {

				String url = "http://iphone.smerty.com/wunder/wx_near_all.php?lat="
					+ location.getLatitude() + "&lon="
					+ location.getLongitude();
				
				Log.d("downloadLocalStations", "Fetching Url: " + url);
				
				HttpGet method = new HttpGet(url);
				HttpResponse res = client.execute(method);
				data = res.getEntity().getContent();

			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(getBaseContext(), R.string.toast_network_failure,
						Toast.LENGTH_SHORT).show();

			}
			Document doc = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;

			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse(data);
				// finish();
			} catch (SAXParseException e) {
				e.printStackTrace();
				Toast.makeText(getBaseContext(), "SAXParseException",
						Toast.LENGTH_SHORT).show();
				return null;
				// finish();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				Toast.makeText(getBaseContext(), "SAXException",
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				return null;
				// finish();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				Toast.makeText(getBaseContext(),
						"ParserConfigurationException", Toast.LENGTH_SHORT)
						.show();
				e.printStackTrace();
				return null;
			}

			if (doc == null) {
				Toast.makeText(getBaseContext(), "doc is null",
						Toast.LENGTH_SHORT).show();
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
					}
					else {
						Log.d("downloadLocalStations", "didn't put station in map");
					}
				}

			} catch (Exception e) {
				Log.d("onLocation Changed", e.toString() + " / "
						+ e.getMessage());
				return null;
				// do nothing
			}

			return stationMap;

		} catch (IOException e) {
			Toast.makeText(getBaseContext(), e.toString() + ": " + e.getMessage(),
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return null;
		}
	}

	public void processLocalStations(final Map<String, String> stations) {

		if (stations == null || stations.isEmpty()) {
			Toast.makeText(getBaseContext(), R.string.toast_no_nearby,
					Toast.LENGTH_LONG).show();
			return;
		}

		AlertDialog.Builder alertPWSList = new AlertDialog.Builder(this);

		alertPWSList.setTitle(R.string.menu_nearby);

		final String PWSName[] = stations.values().toArray(new String[0]);
		final String PWSid[] = stations.keySet().toArray(new String[0]);

		final Wunder thatPWSList = this;

		alertPWSList.setSingleChoiceItems(PWSName, -1,
				new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int item) {

						thatPWSList.selectedID = PWSid[item];

					}

				});

		alertPWSList.setPositiveButton(R.string.button_set,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int whichButton) {
						String value = thatPWSList.selectedID.toUpperCase(Locale.ENGLISH);

						SharedPreferences settings = getSharedPreferences(
								PREFS_NAME, 0);
						String pwsid = settings.getString("stationID",
								DEF_STATION_ID);

						SharedPreferences.Editor editor = settings.edit();
						editor.putString("stationID", value);
						editor.commit();

						if (!pwsid.equalsIgnoreCase(value)) {
							Toast.makeText(getBaseContext(),
									"Changed: " + value, Toast.LENGTH_LONG)
									.show();

						}

						thatPWSList.onCreate(null);

					}
				});

		alertPWSList.setNegativeButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int whichButton) {
						// Canceled.
					}
				});

		alertPWSList.setIcon(R.drawable.icon);

		alertPWSList.show();

	}

	public void onLocationChanged(Location location) {
		printLocation(location);

		if (location == null) {
			// Failed
			Toast.makeText(getBaseContext(), R.string.toast_location_failure,
					Toast.LENGTH_SHORT).show();
			return;
		} else {

			if (this.localstationstask == null) {
				Log.d("localstationstask", "task was null, calling execute");
				this.localstationstask = new GetLocalStationsTask()
						.execute(location);
			} else {
				Status locationChangedStatus = this.localstationstask.getStatus();
				if (locationChangedStatus == Status.FINISHED) {
					Log
							.d("localstationstask",
									"task wasn't null, status finished, calling execute");
					this.localstationstask = new GetLocalStationsTask()
							.execute(location);
				}
			}

			locationManager.removeUpdates(this);

		}

	}

	public void onProviderDisabled(final String provider) {
		Log.d("WUNDERLOC", "Provider Disabled: " + provider);
	}

	public void onProviderEnabled(final String provider) {
		Log.d("WUNDERLOC", "Provider Enabled: " + provider);
	}

	public void onStatusChanged(final String provider, final int status, final Bundle extras) {
		Log.d("WUNDERLOC", "Provider Status Changed: " + provider + ", Status="
				+ LOCATION_STATUS[status] + ", Extras=" + extras);
	}

	private void printProvider(final String provider) {
		LocationProvider info = locationManager.getProvider(provider);
		Log.d("WUNDERLOC", info.toString());
	}

	private void printLocation(final Location location) {
		if (location == null) {
			Log.d("WUNDERLOC", "Location[unknown]");
		} else {
			Log.d("WUNDERLOC", location.toString());
		}
	}

}