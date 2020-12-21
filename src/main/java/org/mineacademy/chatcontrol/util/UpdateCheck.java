package org.mineacademy.chatcontrol.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;

public class UpdateCheck implements Runnable {

	public static boolean needsUpdate = false;
	public static String newVersion;

	@Override
	public void run() {
		final String currentversion = ChatControl.instance().getDescription().getVersion();

		try {
			if (Settings.Updater.CHECK_NOTES) {
				final YamlConfiguration file = openExternalYaml("http://kangarko.6f.sk/chatcontrol/note.txt");

				if (file != null) {
					String note = file.getString("message");

					if (note == null)
						note = file.getString(String.valueOf(toNumber(currentversion)));

					if (note != null && !note.isEmpty())
						Common.Log(note);
				}
			}
		} catch (final IOException ex) {
		}

		if (!Settings.Updater.ENABLED)
			return;

		if (currentversion.contains("SNAPSHOT") || currentversion.contains("DEV"))
			return;

		final String rawUrl = "https://api.spigotmc.org/legacy/update.php?resource=271";

		try {
			final URL url = new URL(rawUrl);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))) {
				final String newversion = reader.readLine();

				if (newversion == null) {
					Common.Debug("Could not retrieve version from the update file.");

					return;
				}

				if (newversion.contains("SNAPSHOT") || newversion.contains("DEV"))
					return;

				if (toNumber(newversion) > toNumber(currentversion)) {
					needsUpdate = true;
					newVersion = newversion;

					Common.Log(Localization.UPDATE_AVAILABLE.replace("{current}", currentversion).replace("{new}", newversion));
				}
			}

		} catch (UnknownHostException | MalformedURLException ex) {
			Common.Warn("Update check failed, could not connect to: " + rawUrl);

			if (Settings.DEBUG)
				ex.printStackTrace();

		} catch (final NumberFormatException ex) {
			Common.Warn("Update check failed, malformed version string: " + ex.getMessage());

		} catch (final IOException ex) {
			if (ex.getMessage().equals("Permission denied: connect"))
				Common.Warn("Unable to connect to the update site, check your internet/firewall.");
			else {
				Common.Warn("Error while checking for update from: " + rawUrl + " (" + ex.getMessage() + ")");

				if (Settings.DEBUG)
					ex.printStackTrace();
			}
		}
	}

	private YamlConfiguration openExternalYaml(String path) throws IOException {
		final InputStream is = new URL(path).openStream();

		if (is != null)
			return CompatProvider.loadConfiguration(is);

		return null;
	}

	private int toNumber(String s) {
		return Integer.valueOf(s.replace(".", "").replace("-BETA", "").replace("-ALPHA", ""));
	}
}