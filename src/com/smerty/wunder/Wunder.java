package com.smerty.wunder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Document;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.LocationManager;
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

import com.smerty.android.DocumentHelper;
import com.smerty.android.WebFetch;

public class Wunder extends Activity {

	static private final String TAG = Wunder.class.getSimpleName();

	private static final String PREFS_NAME = "WunderPrefs";
	private static final String DEF_STATION_ID = "KCALIVER14";
	private LocationHelper locationHelper;
	transient private String selectedID = "";

	transient private ScrollView scrollView;
	transient private TableLayout tableLayout;

	transient private AsyncTask<Wunder, Integer, Integer> updatetask;
	public static ProgressDialog progressDialog;

	transient private WeatherReport conds;

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
			Log.d(TAG, "task was null, calling execute");
			this.updatetask = new UpdateFeedTask().execute(this);
		} else {
			final Status updateTaskStatus = this.updatetask.getStatus();
			if (updateTaskStatus == Status.FINISHED) {
				Log
						.d(TAG,
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
			Log.d(TAG, progress[0].toString());
			if (progress[0] == 0) {
				Wunder.progressDialog = ProgressDialog.show(that, that
						.getResources().getText(R.string.app_name), that
						.getResources().getText(
								R.string.message_download_weather_progress),
						true, false);
			}
			if (progress[0] == 100) {
				Wunder.progressDialog.dismiss();
			}

		}

		protected void onPostExecute(final Integer result) {
			// Log.d("onPostExecute", that.getApplicationInfo().packageName);
			that.allTogether();

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
		} else {
			if (conds.temperature != null && conds.temperature.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.temperature = String
							.valueOf(Math
									.floor((Double
											.parseDouble(conds.temperature) - 32) * 5 / 9 * 10) / 10);
					text.setText(that.getResources().getText(
							R.string.temperature)
							+ ": \t\t\t\t\t" + conds.temperature + "C");
				} else {
					text.setText(that.getResources().getText(
							R.string.temperature)
							+ ": \t\t\t\t\t" + conds.temperature + "F");
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
					text.setText(that.getResources().getText(R.string.dewpoint)
							+ ": \t\t\t\t\t\t" + conds.dewpoint + "C");
				} else {
					text.setText(that.getResources().getText(R.string.dewpoint)
							+ ": \t\t\t\t\t\t" + conds.dewpoint + "F");
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
				text.setText(that.getResources().getText(
						R.string.relative_humidity)
						+ ": \t\t\t" + conds.humidity + "%");
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
				text.setText(that.getResources().getText(
						R.string.wind_direction)
						+ ": \t\t\t\t" + conds.winddirection);
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
					text.setText(that.getResources().getText(
							R.string.wind_speed)
							+ ": \t\t\t\t\t" + conds.windspeed + " km/h");
				} else {
					text.setText(that.getResources().getText(
							R.string.wind_speed)
							+ ": \t\t\t\t\t" + conds.windspeed + " mph");
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
					text.setText(that.getResources().getText(
							R.string.barometric_pressure)
							+ ": \t\t" + conds.pressure + " mb");
				} else {
					text.setText(that.getResources().getText(
							R.string.barometric_pressure)
							+ ": \t\t" + conds.pressure + " in");
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
				text.setText(that.getResources().getText(
						R.string.solar_radiation)
						+ ": \t\t\t\t" + conds.solarradiation + " w/m^2");
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.uvIndex != null && conds.uvIndex.length() > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				text.setText(that.getResources().getText(R.string.uv_index)
						+ ": \t\t\t\t\t\t" + conds.uvIndex);
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
					text.setText(that.getResources().getText(
							R.string.precipitation_1hr)
							+ ": \t\t" + conds.precipitation1hr + " mm");
				} else {
					text.setText(that.getResources().getText(
							R.string.precipitation_1hr)
							+ ": \t\t" + conds.precipitation1hr + " in");
				}
				text.setTextSize(18);
				row.setPadding(3, 3, 3, 3);
				row.setBackgroundColor(Color.argb(200, 51, 51, 51));
				row.addView(text);
				tableLayout.addView(row);
			}

			if (conds.precipToday != null && conds.precipToday.length() > 0
					&& Double.valueOf(conds.precipToday) > 0) {
				row = new TableRow(that);
				text = new TextView(that);
				if (usemetric) {
					conds.precipToday = String.valueOf(Math.floor((Double
							.parseDouble(conds.precipToday)) * 25.4 * 10) / 10);
					text.setText(that.getResources().getText(
							R.string.precipitation_today)
							+ ": \t\t" + conds.precipToday + " mm");
				} else {
					text.setText(that.getResources().getText(
							R.string.precipitation_today)
							+ ": \t\t" + conds.precipToday + " in");
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
			break;
		case MENU_GEO:
			geoHelper();
			break;
		case MENU_REFRESH:
			this.onCreate(null);
			break;
		case MENU_SETTINGS:

			final AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.pws_station_id);

			final EditText input = new EditText(this);

			final SharedPreferences settings = getSharedPreferences(PREFS_NAME,
					0);
			final String pwsid = settings
					.getString("stationID", DEF_STATION_ID);
			final boolean useMetric = settings.getBoolean("useMetric", false);

			input.setSingleLine(true);

			final InputFilter filter = new InputFilter() {
				public CharSequence filter(final CharSequence source,
						final int start, final int end, final Spanned dest,
						final int dstart, final int dend) {
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
							final String value = input.getText().toString()
									.trim().toUpperCase();

							final SharedPreferences settings = getSharedPreferences(
									PREFS_NAME, 0);
							final String pwsid = settings.getString(
									"stationID", DEF_STATION_ID);

							final SharedPreferences.Editor editor = settings
									.edit();
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

			break;
		case MENU_QUIT:
			finish();
			break;
		default:
			return false;
		}
		return true;
	}

	public WeatherReport getWeather() {

		WeatherReport retval = new WeatherReport();

		InputStream data;

		String pwsid = null;

		try {

			final SharedPreferences settings = getSharedPreferences(PREFS_NAME,
					0);
			pwsid = settings.getString("stationID", DEF_STATION_ID);

			data = WebFetch.getInputStream(
					"http://api.wunderground.com/weatherstation/WXCurrentObXML.asp?ID="
							+ pwsid, "java", "UTF-8");

			final Document doc = DocumentHelper.getDocument(data);

			try {
				retval.solarradiation = doc.getElementsByTagName(
						"solar_radiation").item(0).getChildNodes().item(0)
						.getNodeValue();
				retval.uvIndex = doc.getElementsByTagName("UV").item(0)
						.getChildNodes().item(0).getNodeValue();
			} catch (Exception e) {
				// do nothing
			}

			try {
				retval.precipitation1hr = doc.getElementsByTagName(
						"precip_1hr_in").item(0).getChildNodes().item(0)
						.getNodeValue();
				retval.precipToday = doc
						.getElementsByTagName("precip_today_in").item(0)
						.getChildNodes().item(0).getNodeValue();
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
				Log.d(TAG, e.getMessage(), e);
				retval = null;
			}
		} catch (IOException e) {
			Log.d(TAG, e.getMessage(), e);
			Toast.makeText(getBaseContext(), R.string.toast_network_failure,
					Toast.LENGTH_SHORT).show();
			retval = null;
		}

		return retval;
	}

	public void geoHelper() {
		locationHelper = new LocationHelper((LocationManager) this
				.getSystemService(LOCATION_SERVICE), this);
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

	public void processLocalStations(final Map<String, String> stations) {

		if (stations == null || stations.isEmpty()) {
			Toast.makeText(getBaseContext(), R.string.toast_no_nearby,
					Toast.LENGTH_LONG).show();
			return;
		}

		final AlertDialog.Builder alertPWSList = new AlertDialog.Builder(this);

		alertPWSList.setTitle(R.string.menu_nearby);

		final String PWSName[] = stations.values().toArray(new String[0]);
		final String PWSid[] = stations.keySet().toArray(new String[0]);

		final Wunder thatPWSList = this;

		alertPWSList.setSingleChoiceItems(PWSName, -1,
				new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog,
							final int item) {

						thatPWSList.selectedID = PWSid[item];

					}

				});

		alertPWSList.setPositiveButton(R.string.button_set,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final String value = thatPWSList.selectedID
								.toUpperCase(Locale.ENGLISH);

						final SharedPreferences settings = getSharedPreferences(
								PREFS_NAME, 0);
						final String pwsid = settings.getString("stationID",
								DEF_STATION_ID);

						final SharedPreferences.Editor editor = settings.edit();
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
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						// Canceled.
					}
				});

		alertPWSList.setIcon(R.drawable.icon);

		alertPWSList.show();

	}
	
	public LocationHelper getLocationHelper() {
		return locationHelper;
	}
	
	public void setLocationHelper(LocationHelper locationHelper) {
		this.locationHelper = locationHelper;
	}

}