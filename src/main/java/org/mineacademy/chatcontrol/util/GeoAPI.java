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

public class GeoAPI {

	private static final HashMap<String, GeoResponse> cache = new HashMap<>();

	public static final GeoResponse track(InetAddress ip) {
		GeoResponse response = new GeoResponse("", "", "", "");

		if (ip == null)
			return response;

		if (ip.getHostAddress().equals("127.0.0.1") || ip.getHostAddress().equals("0.0.0.0"))
			return new GeoResponse("local", "-", "local", "-");

		if (cache.containsKey(ip.toString()))
			return cache.get(ip.toString());

		try {
			final URL url = new URL("http://ip-api.com/json/" + ip.getHostAddress() + "?fields=country,countryCode,regionName,isp");
			final URLConnection con = url.openConnection();

			con.setConnectTimeout(2000);
			con.setReadTimeout(2000);

			try (final BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String page = "";
				String input;

				while ((input = r.readLine()) != null)
					page += input;

				response = new GeoResponse(getJson(page, "country"), getJson(page, "countryCode"), getJson(page, "regionName"), getJson(page, "isp"));
				cache.put(ip.toString(), response);
			}

		} catch (final NoRouteToHostException ex) {
			// Firewall or internet access denied

		} catch (final SocketTimeoutException ex) {
		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		return response;
	}

	private static final String getJson(String page, String element) {
		return page.contains("\"" + element + "\":\"") ? page.split("\"" + element + "\":\"")[1].split("\",")[0] : "";
	}

	public static final class GeoResponse {
		private final String countryName, countryCode, regionName, isp;

		public GeoResponse(String countryName, String countryCode, String regionName, String isp) {
			this.countryName = countryName;
			this.countryCode = countryCode;
			this.regionName = regionName;
			this.isp = isp;
		}

		public String getCountryName() {
			return countryName;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public String getRegionName() {
			return regionName;
		}

		public String getIsp() {
			return isp;
		}

		@Override
		public String toString() {
			return "GeoResponse{country=" + countryName + ", isp=" + isp + "}";
		}
	}
}
