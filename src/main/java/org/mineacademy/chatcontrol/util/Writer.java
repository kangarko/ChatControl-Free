package org.mineacademy.chatcontrol.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import javax.annotation.Nullable;

import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.settings.Settings;

/**
 * A utility class capable of extracting, or writing to files.
 */
public final class Writer {

	/**
	 * The path to the file where errors are logged.
	 */
	public static final String ERROR_PATH = "errors/errors.txt";

	/**
	 * The path to the file where chat message are logged.
	 */
	public static final String CHAT_PATH = "logs/chat.txt";

	/**
	 * The path to the file where rule output is logged.
	 */
	public static final String RULES_PATH = "logs/rules.txt";

	private Writer() {
	}

	/**
	 * Write a line to file with optional prefix which can be null.
	 *
	 * @param toPath path to the file inside the plugin folder
	 * @param prefix optional prefix, can be null
	 * @param message line, is split by \n
	 */
	public static void write(String toPath, @Nullable String prefix, String message) {
		final int lastIndex = toPath.lastIndexOf('/');
		final File dir = new File(ChatControl.instance().getDataFolder(), toPath.substring(0, lastIndex >= 0 ? lastIndex : 0));

		if (!dir.exists())
			dir.mkdirs();

		final File file = new File(ChatControl.instance().getDataFolder(), toPath);

		if (Settings.Writer.STRIP_COLORS)
			message = Common.stripColors(message);

		try (FileWriter writer = new FileWriter(file, true)) {
			for (final String line : message.trim().split("\n"))
				writer.write("[" + Common.getFormattedDate() + "] " + (prefix != null ? prefix + ": " : "") + line + System.lineSeparator());

		} catch (final Exception ex) {
			ex.printStackTrace();

			Common.logInFrame(false,
					"Error writing to: " + toPath,
					"Error: " + ex.getMessage());
		}
	}

	/**
	 * Copy file from plugins jar to destination.
	 *
	 * @param internalPath the path to the file inside the plugin
	 * @return the extracted file
	 */
	public static File extract(String internalPath) {
		return extract(internalPath, internalPath);
	}

	/**
	 * Copy file from plugins jar to destination - customizable destination file
	 * name.
	 *
	 * @param internalPath the path to the file inside the plugin
	 * @param diskPath the path where the file will be copyed inside the plugin folder
	 *
	 * @return the extracted file
	 */
	public static File extract(String internalPath, String diskPath) {
		final File datafolder = ChatControl.instance().getDataFolder();
		final File destination = new File(datafolder, diskPath);

		if (destination.exists())
			return destination;

		final int lastIndex = diskPath.lastIndexOf('/');
		final File dir = new File(datafolder, diskPath.substring(0, lastIndex >= 0 ? lastIndex : 0));

		if (!dir.exists())
			dir.mkdirs();

		final InputStream input = ChatControl.class.getResourceAsStream("/" + internalPath);
		Objects.requireNonNull(input, "Inbuilt resource not found: " + internalPath);

		try {
			Files.copy(input, Paths.get(destination.toURI()), StandardCopyOption.REPLACE_EXISTING);

		} catch (final IOException ex) {
			throw new RuntimeException("Error copying: " + internalPath + " to: " + diskPath, ex);
		}

		Common.log("&fCreated default file: " + destination.getName());
		return destination;
	}
}
