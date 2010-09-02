package com.smerty.wunder;

public class WeatherReport {
	public String stationID, neighborhood, updated, temperature, humidity,
			winddirection, windspeed, pressure, dewpoint, heatindex,
			windchill, solarradiation, uvIndex, precipitation1hr, precipToday;
	
	public WeatherReport() {
		// TODO Auto-generated constructor stub
	}

	public WeatherReport(final String stationID, final String neighborhood, final String updated,
			final String temperature, final String humidity, final String winddirection,
			final String windspeed, final String pressure, final String dewpoint,
			final String heatindex, final String windchill, final String solarradiation,
			final String uvIndex, final String precipitation1hr, final String precipToday) {
		super();
		this.stationID = stationID;
		this.neighborhood = neighborhood;
		this.updated = updated;
		this.temperature = temperature;
		this.humidity = humidity;
		this.winddirection = winddirection;
		this.windspeed = windspeed;
		this.pressure = pressure;
		this.dewpoint = dewpoint;
		this.heatindex = heatindex;
		this.windchill = windchill;
		this.solarradiation = solarradiation;
		this.uvIndex = uvIndex;
		this.precipitation1hr = precipitation1hr;
		this.precipToday = precipToday;
	}
	
	
}