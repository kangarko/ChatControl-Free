package org.mineacademy.chatcontrol.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;

/**
 * The main utility package storing commonly used util methods.
 */
public final class Common {

	/**
	 * The console sender.
	 */
	private static final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

	/**
	 * The three digit decimal format.
	 */
	private static final DecimalFormat threeDigitFormat = new DecimalFormat("#.###");

	/**
	 * The date format we use: "dd.MM.yyyy HH:mm:ss".
	 */
	private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	/**
	 * Internal plugin prefix.
	 */
	private static String INTERNAL_PREFIX = "";

	private Common() {
	}

	/**
	 * Adds plugin prefix after the plugin has loaded.
	 *
	 * @deprecated internal call
	 */
	@Deprecated
	public static void addLoggingPrefix() {
		INTERNAL_PREFIX = "[ChatControl] ";
	}

	/**
	 * Returns a formatted date as DAY.MONTH.YEAR HOUR:MINUTES:SECONDS.
	 *
	 * @return
	 */
	public static String getFormattedDate() {
		return dateFormat.format(System.currentTimeMillis());
	}

	/**
	 * Formats the given location as useful for debugging.
	 *
	 * @param location
	 * @return
	 */
	public static String getFormattedLocation(Location location) {
		return location.getWorld().getName() + " x:" + (int) location.getX() + " y:" + (int) location.getY() + " z:" + (int) location.getZ();
	}

	/**
	 * Formats the value as #.###.
	 *
	 * @param value
	 * @return
	 */
	public static String threeDigits(double value) {
		return threeDigitFormat.format(value);
	}

	/**
	 * Basic, colorizes msg and handles {prefix}. every other method to tell player.
	 *
	 * should extend this
	 * @param sender
	 * @param message
	 */
	public static void tellColored(CommandSender sender, String message) {
		if (!message.isEmpty() && !message.equalsIgnoreCase("none"))
			for (final String part : splitNewline(message))
				sender.sendMessage(colorize(part));
	}

	/**
	 * Sends sendColoredMsg with resolved {player}.
	 * @param sender
	 * @param messages
	 */
	public static void tell(CommandSender sender, String... messages) {
		for (final String msg : messages)
			tellColored(sender, msg.replace("{player}", resolvedSender(sender)));
	}

	/**
	 * Sends sendColoredMsg with known specified {player} in last argument.
	 *
	 * @param sender
	 * @param message
	 * @param playerReplacement
	 */
	public static void tell(CommandSender sender, String message, String playerReplacement) {
		tellColored(sender, message.replace("{player}", playerReplacement));
	}

	/**
	 * Sends the sender a message with a delay in ticks.
	 *
	 * @param sender
	 * @param delayTicks
	 * @param messages
	 */
	public static void tellLater(final CommandSender sender, int delayTicks, final String... messages) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(ChatControl.getInstance(), () -> {
			tell(sender, messages);

		}, delayTicks);
	}

	/**
	 * Broadcast a message.
	 *
	 * @param message
	 */
	public static void broadcast(String message) {
		for (final String part : splitNewline(message))
			Bukkit.broadcastMessage(colorize(part));
	}

	/**
	 * Broadcast a message if the boolean is true, replacing sender variable.
	 *
	 * @param enabled
	 * @param sender
	 * @param message
	 * @param reason
	 */
	public static void broadcastIfEnabled(boolean enabled, CommandSender sender, String message, String reason) {
		if (enabled)
			broadcast((message + (reason.equals("") ? "" : " " + Localization.Parts.REASON.replace("{reason}", reason))).replace("{player}", sender == null ? "" : resolvedSender(sender)));
	}

	/**
	 * Checks if the sender has the given permission.
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean hasPermission(CommandSender sender, String permission) {
		return sender.hasPermission(permission);
	}

	/**
	 * Runs the command as the console.
	 *
	 * @param command
	 */
	public static void dispatchConsoleCommand(final String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		Bukkit.getScheduler().scheduleSyncDelayedTask(ChatControl.getInstance(), () -> {
			Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), colorize(command));
		});
	}

	/**
	 * Inserts dot at the end of the message.
	 *
	 * @param message
	 * @return
	 */
	public static String insertDot(String message) {
		final String lastChar = message.substring(message.length() - 1);
		final String[] words = message.split("\\s");
		final String lastWord = words[words.length - 1];

		if (!isDomain(lastWord) && lastChar.matches("(?i)[a-z]"))
			message = message + ".";

		return message;
	}

	/**
	 * Capitalizes first words in the message.
	 *
	 * @param message
	 * @return
	 */
	public static String capitalize(String message) {
		final String[] sentences = message.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences) {
			final String word = message.split("\\s")[0];

			if (!isDomain(word))
				sentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);

			tempMessage = tempMessage + sentence + " ";
		}
		return tempMessage.trim();
	}

	/**
	 * Colorizes and replace {prefix} in the message.
	 *
	 * @param message
	 * @return
	 */
	public static String colorize(String message) {
		if (message == null || message.isEmpty())
			return "";

		return ChatColor.translateAlternateColorCodes('&', replacePrefix(message));
	}

	/**
	 * A primitive output of ----- as decorative console line.
	 *
	 * @return
	 */
	public static String consoleLine() {
		return "&6*----------------------------------------------*";
	}

	/**
	 * Return true if the plugin exists, logs to console if it does.
	 *
	 * @param plugin
	 * @return
	 */
	public static boolean doesPluginExist(String plugin) {
		return doesPluginExist(plugin, null);
	}

	/**
	 * Return true if the plugin exists, logs to console if it does with the optional message.
	 *
	 * @param plugin
	 * @param message
	 * @return
	 */
	public static boolean doesPluginExist(String plugin, String message) {
		final boolean hooked = Bukkit.getPluginManager().getPlugin(plugin) != null;
		if (hooked)
			Common.log("&3Hooked into&8: &f" + plugin + (message != null ? " &7(" + message + ")" : ""));

		return hooked;
	}

	/**
	 * Prints message to the console if verbosing of rules or debug is enabled.
	 *
	 * @param message
	 */
	public static void verbose(String message) {
		if (Settings.VERBOSE_RULES || Settings.DEBUG)
			log(message);
	}

	/**
	 * Logs messages to the console.
	 *
	 * @param messages
	 */
	public static void log(String... messages) {
		for (final String message : messages)
			for (final String part : splitNewline(message))
				console.sendMessage(colorize(INTERNAL_PREFIX + part.replace("\n", "\n&r")));
	}

	/**
	 * Logs messages to the console in a "frame", optionally disabling the plugin.
	 *
	 * @param disable
	 * @param messages
	 */
	public static void logInFrame(boolean disable, String... messages) {
		final String old = INTERNAL_PREFIX;
		INTERNAL_PREFIX = "";

		log("&6*---------------- ChatControl -----------------*");
		for (final String msg : messages)
			log(" &c" + msg);

		if (disable) {
			Bukkit.getPluginManager().disablePlugin(ChatControl.getInstance());
			log(" &cPlugin is now disabled.");
		}

		log(consoleLine());
		INTERNAL_PREFIX = old;
	}

	/**
	 * Prints a message to the console if debug is enabled with a debug prefix.
	 *
	 * @param message
	 */
	public static void debug(String message) {
		if (Settings.DEBUG)
			console.sendMessage(colorize((INTERNAL_PREFIX.isEmpty() ? "" : "[ChatControl Debug] ") + message.replace("\n", "\n&r")));
	}

	/**
	 * Logs a warning message to the console.
	 *
	 * @param message
	 */
	public static void warn(String message) {
		Bukkit.getLogger().log(Level.WARNING, colorize(INTERNAL_PREFIX + message));
	}

	/**
	 * Strips special letters or unicodes according to anti spam settings.
	 *
	 * @param message
	 * @return
	 */
	public static String stripUnicodeOrDuplicates(String message) {
		if (Settings.AntiSpam.STRIP_SPECIAL_CHARS)
			message = message.replaceAll("[^a-zA-Z0-9\\s]", ""); // strip spec. characters EXCEPT spaces

		if (Settings.AntiSpam.STRIP_DUPLICATE_CHARS) {
			message = message.replaceAll("(.)(?=\\1\\1+)", "");
			message = message.replaceAll("(..)(?=\\1\\1+)", "");
			message = message.replaceAll("(...)(?=\\1\\1+)", "");
		}

		return stripColors(message.toLowerCase());
	}

	/**
	 * Removes colors from the message.
	 *
	 * @param message
	 * @return
	 */
	public static String stripColors(String message) {
		return ChatColor.stripColor(message);
	}

	/**
	 * Calculates uppercase letters in a row.
	 *
	 * @param message
	 * @return
	 */
	public static int[] getCapsInRows(String message) {
		final int[] editedMsg = new int[message.length()];
		final String[] parts = message.split(" ");

		for (int i = 0; i < parts.length; i++)
			for (final String whitelisted : Settings.AntiCaps.WHITELIST)
				if (whitelisted.equalsIgnoreCase(parts[i]))
					parts[i] = parts[i].toLowerCase();

		final String msg = String.join(" ", parts);

		for (int i = 0; i < msg.length(); i++)
			if (Character.isUpperCase(msg.charAt(i)) && Character.isLetter(msg.charAt(i)))
				editedMsg[i] = 1;
			else
				editedMsg[i] = 0;

		return editedMsg;
	}

	/**
	 * Returns percentage of caps in the given rows.
	 *
	 * @param caps
	 * @return
	 */
	public static int getPercentageCaps(int[] caps) {
		int sum = 0;
		for (int cap : caps)
			sum += cap;

		final double ratio = sum / caps.length;
		final int percent = (int) (100.0D * ratio);
		return percent;
	}

	/**
	 * Calculates caps row percentage.
	 *
	 * @param caps
	 * @return
	 */
	public static int getCapsInRowPercentage(int[] caps) {
		int sum = 0;
		int sumTemp = 0;

		for (final int i : caps)
			if (i == 1) {
				sumTemp++;
				sum = Math.max(sum, sumTemp);
			} else
				sumTemp = 0;
		return sum;
	}

	/**
	 * Calculates the similarity (a percentage within 0% and 100%) between two
	 * strings.
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public static int getSimilarity(String first, String second) {
		String longer = first, shorter = second;

		if (first.length() < second.length()) { // longer should always have greater length
			longer = second;
			shorter = first;
		}

		final int longerLength = longer.length();

		if (longerLength == 0)
			return 100; /* both strings are zero length */

		final double result = (longerLength - editDistance(longer, shorter)) / (double) longerLength;

		return (int) (result * 100);

	}

	/**
	 * Return true if the given pattern is found in the message.
	 *
	 * @param pattern
	 * @param message
	 * @return
	 */
	public static boolean isRegexMatch(String pattern, String message) {
		final Matcher matcher = getMatcher(pattern, message);

		try {
			return matcher.find();

		} catch (final RuntimeException ex) {
			Writer.write(Writer.ERROR_PATH, null, "Regex check timed out (bad regex?) (plugin ver. " + ChatControl.getInstance().getDescription().getVersion() + ")!\nString checked: " + message + "\nRegex: " + pattern + "");
			logInFrame(false, "Regex timed out after " + Settings.REGEX_TIMEOUT + "ms! ", "&fREG EX&c: &7" + pattern, "&fSTRING&c: &7" + message);

			return false;
		}
	}

	/**
	 * Replaces all occurences using the given pattern in the given message.
	 *
	 * @param pattern
	 * @param message
	 * @param replacement
	 * @return
	 */
	public static String replaceMatch(String pattern, String message, String replacement) {
		final Matcher matcher = getMatcher(pattern, message);

		try {
			return matcher.replaceAll(replacement);

		} catch (final RuntimeException ex) {
			return "";
		}
	}

	/**
	 * Return true if the player has the "vanished" metadata.
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanishedMeta(Player player) {
		if (player.hasMetadata("vanished"))
			for (final MetadataValue meta : player.getMetadata("vanished"))
				if (meta.asBoolean())
					return true;

		return false;
	}

	/**
	 * Throw an error if the given expression is false.
	 *
	 * @param expression
	 */
	public static void checkBoolean(final boolean expression) {
		if (!expression)
			throw new RuntimeException();
	}

	/**
	 * Throw an error with a custom message if the given expression is false.
	 *
	 * @param expression
	 * @param falseMessage
	 * @param replacements
	 */
	public static void checkBoolean(final boolean expression, final String falseMessage, final Object... replacements) {
		if (!expression) {
			String message = falseMessage;

			try {
				message = String.format(falseMessage, replacements);

			} catch (final Throwable t) {
			}

			throw new RuntimeException(message);
		}
	}

	/*
	 * Returns our own optimized matcher.
	 */
	private static Matcher getMatcher(String patternRaw, String message) {
		patternRaw = stripColors(patternRaw);
		message = stripColors(message);

		Pattern pattern = null;
		final TimedCharSequence timedMsg = new TimedCharSequence(message/* .toLowerCase() */, Settings.REGEX_TIMEOUT);

		try {
			pattern = Pattern.compile(patternRaw, Pattern.CASE_INSENSITIVE);

		} catch (final PatternSyntaxException ex) {
			ex.printStackTrace();
			logInFrame(false, "Malformed regex: \'" + patternRaw + "\'", "Use online services (like &fregex101.com&f)", "for fixing errors");
			return null;
		}

		return pattern.matcher(timedMsg);
	}

	/*
	 * A primitive way to "fix" some operating system's differences in new line operator splitting.
	 */
	private static String[] splitNewline(String innocentMessage) {
		if (!Settings.ENFORCE_NEW_LINE)
			return innocentMessage.split("\n");

		final String BUZNA_DELIMITER = "kAnGaRkO_lUbI_ZmRzLiNkU";

		final char[] chars = innocentMessage.toCharArray();
		String parts = "";

		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];

			if ('\\' == c)
				if (i + 1 < chars.length) {
					final char next = chars[i + 1];

					if ('n' == next) {
						i++;

						parts += BUZNA_DELIMITER;
						continue;
					}
				}
			parts += c;
		}

		return parts.split(BUZNA_DELIMITER);
	}

	/*
	 * Replaces {prefix} and {server}.
	 */
	private static String replacePrefix(String message) {
		return message.replace("{prefix}", Localization.Parts.PREFIX).replace("{server}", Localization.Parts.PREFIX_SERVER);
	}

	/*
	 * Returns true if the message is a domain
	 */
	private static boolean isDomain(String message) {
		return message.matches("(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}|www\\.[^\\s]+\\.[^\\s]{2,})");
	}

	/*
	 * Get the sender's name if it is a player, or fallback to localization console name.
	 */
	private static String resolvedSender(CommandSender sender) {
		if (sender instanceof Player)
			return sender.getName();

		return Localization.Parts.CONSOLE;
	}

	/*
	 * Example implementation of the Levenshtein Edit Distance
	 *
	 * See http://rosettacode.org/wiki/Levenshtein_distance#Java
	 */
	private static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		final int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++)
				if (i == 0)
					costs[j] = j;
				else if (j > 0) {
					int newValue = costs[j - 1];
					if (s1.charAt(i - 1) != s2.charAt(j - 1))
						newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
					costs[j - 1] = lastValue;
					lastValue = newValue;
				}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}
}

/**
 * An implementation of a char sequence with time limit for getting each char, preventing malformed regex from crashing the planet.
 */
final class TimedCharSequence implements CharSequence {

	private final CharSequence message;
	private final int timeoutLimit;
	private final long timeoutTime;

	public TimedCharSequence(CharSequence message, int timeoutLimit) {
		this.message = message;
		this.timeoutLimit = timeoutLimit;
		this.timeoutTime = System.currentTimeMillis() + timeoutLimit;
	}

	@Override
	public char charAt(int index) {
		if (System.currentTimeMillis() > this.timeoutTime)
			throw new RuntimeException("\'" + this.message + "\' timed out after " + this.timeoutLimit + " ms! (malformed regex?)");

		return this.message.charAt(index);
	}

	@Override
	public int length() {
		return this.message.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new TimedCharSequence(this.message.subSequence(start, end), this.timeoutLimit);
	}

	@Override
	public String toString() {
		return this.message.toString();
	}
}