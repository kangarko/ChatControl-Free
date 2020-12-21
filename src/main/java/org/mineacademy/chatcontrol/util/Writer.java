package org.mineacademy.chatcontrol.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.settings.Settings;

public class Writer {

	public static final String ERROR_PATH = "errors/errors.txt";
	public static final String CHAT_PATH = "logs/chat.txt";
	public static final String RULES_PATH = "logs/rules.txt";

	private Writer() {
	}

	/**
	 * Write a line to file with optional prefix which can be null.
	 *
	 * @param to
	 *            path to the file inside the plugin folder
	 * @param prefix
	 *            optional prefix, can be null
	 * @param msg
	 *            line, is split by \n
	 */
	public static void Write(String to, String prefix, String msg) {
		final int lastIndex = to.lastIndexOf('/');
		final File dir = new File(ChatControl.instance().getDataFolder(), to.substring(0, lastIndex >= 0 ? lastIndex : 0));
		if (!dir.exists())
			dir.mkdirs();

		final File file = new File(ChatControl.instance().getDataFolder(), to);

		if (Settings.Writer.STRIP_COLORS)
			msg = Common.stripColors(msg);

		try (FileWriter bw = new FileWriter(file, true)) {
			for (final String line : msg.trim().split("\n"))
				bw.write("[" + Common.getFormattedDate() + "] " + (prefix != null ? prefix + ": " : "") + line + System.lineSeparator());
		} catch (final Exception ex) {
			ex.printStackTrace();
			Common.LogInFrame(false, "Error writing to: " + to, "Error: " + ex.getMessage());
		}
	}

	/**
	 * Copy file from plugins jar to destination.
	 *
	 * @param path
	 *            the path to the file inside the plugin
	 * @return the extracted file
	 */
	public static File Extract(String path) {
		return Extract(path, path);
	}

	/**
	 * Copy file from plugins jar to destination - customizable destination file
	 * name.
	 *
	 * @param from
	 *            the path to the file inside the plugin
	 * @param to
	 *            the path where the file will be copyed inside the plugin folder
	 * @return the extracted file
	 */
	
	public static File Extract(String from, String to) {
		final File datafolder = ChatControl.instance().getDataFolder();
		final File destination = new File(datafolder, to);

		if (destination.exists())
			return destination;

		final int lastIndex = to.lastIndexOf('/');
		final File dir = new File(datafolder, to.substring(0, lastIndex >= 0 ? lastIndex : 0));

		if (!dir.exists())
			dir.mkdirs();

		final InputStream is = ChatControl.class.getResourceAsStream("/" + from);
		Objects.requireNonNull(is, "Inbuilt resource not found: " + from);

		try {
			Files.copy(is, Paths.get(destination.toURI()), StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException ex) {
			throw new RuntimeException("Error copying: " + from + " to: " + to, ex);
		}

		Common.Log("&fCreated default file: " + destination.getName());
		return destination;
	}
}
