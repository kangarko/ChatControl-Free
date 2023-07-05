package org.mineacademy.chatcontrol.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.mineacademy.chatcontrol.settings.Settings;

import lombok.Data;

/**
 * A simple helper to retrieve geographical information from IP address.
 */
public final class GeoAPI {

	/*
	 * Cache to minimize the amount of blocking requests
	 */
	private static final HashMap<String, GeoResponse> cache = new HashMap<>();

	/**
	 * Retrieve geographical information about the IP address.
	 *
	 * @param ip
	 * @return
	 */
	public static GeoResponse getResponse(InetAddress ip) {
		GeoResponse response = new GeoResponse("", "", "", "");

		if (!Settings.GEO_DATA || ip == null || ip.getHostAddress() == null)
			return response;

		if (ip.getHostAddress().equals("127.0.0.1") || ip.getHostAddress().equals("0.0.0.0"))
			return new GeoResponse("local", "-", "local", "-");

		synchronized (cache) {
			if (cache.containsKey(ip.toString()))
				return cache.get(ip.toString());
		}

		try {
			final URL url = new URL("http://ip-api.com/json/" + ip.getHostAddress() + "?fields=country,countryCode,regionName,isp");
			final URLConnection con = url.openConnection();

			con.setConnectTimeout(2000);
			con.setReadTimeout(2000);

			BufferedReader reader = null;

			try {
				reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

				StringBuilder page = new StringBuilder();
				String input;

				while ((input = reader.readLine()) != null)
					page.append(input);

				response = new GeoResponse(getJson(page.toString(), "country"), getJson(page.toString(), "countryCode"), getJson(page.toString(), "regionName"), getJson(page.toString(), "isp"));

				synchronized (cache) {
					cache.put(ip.toString(), response);
				}

			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
			}

		} catch (NoRouteToHostException ex) {
			// Firewall or internet access denied

		} catch (SocketTimeoutException ex) {
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return response;
	}

	/*
	 * A primitive helper method to get data from json response
	 */
	private static String getJson(String page, String element) {
		return page.contains("\"" + element + "\":\"") ? page.split("\"" + element + "\":\"")[1].split("\",")[0] : "";
	}

	/**
	 * A class storing the geo response from the track() method above.
	 */
	@Data
	public static final class GeoResponse {
		private final String countryName;
		private final String countryCode;
		private final String regionName;
		private final String isp;
	}
}
