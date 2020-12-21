package org.mineacademy.chatcontrol.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
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

public class Common {

	private static final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

	private static final DecimalFormat digitsFormat = new DecimalFormat("#.###");
	private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private Common() {
	}

	private static String INTERNAL_PREFIX = "";

	public static void addLoggingPrefix() {
		INTERNAL_PREFIX = "[ChatControl] ";
	}

	/**
	 * DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 */
	public static String getFormattedDate() {
		return dateFormat.format(System.currentTimeMillis());
	}

	/**
	 * #.###
	 */
	public static String threeDigits(double value) {
		return digitsFormat.format(value);
	}

	/**
	 * Basic, colorizes msg and handles {prefix}. every other method to tell player
	 * should extend this
	 */
	public static void tellColored(CommandSender sender, String msg) {
		if (!msg.isEmpty() && !msg.equalsIgnoreCase("none"))
			for (final String part : splitNewline(msg))
				sender.sendMessage(colorize(part));
	}

	/**
	 * Sends sendColoredMsg with resolved {player}.
	 */
	public static void tell(CommandSender sender, String... messages) {
		for (final String msg : messages)
			tellColored(sender, msg.replace("{player}", resolvedSender(sender)));
	}

	/**
	 * Sends sendColoredMsg with known specified {player} in last argument.
	 */
	public static void tell(CommandSender pl, String msg, String plReplacement) {
		tellColored(pl, msg.replace("{player}", plReplacement));
	}

	public static void tellLater(final CommandSender pl, int delayTicks, final String... msgs) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(ChatControl.instance(), () -> {
			tell(pl, msgs);

		}, delayTicks);
	}

	public static void broadcast(String message) {
		for (final String part : splitNewline(message))
			Bukkit.broadcastMessage(colorize(part));
	}

	public static void broadcastWithPlayer(String message, String playerReplacement) {
		broadcast(message.replace("{player}", playerReplacement));
	}

	public static void broadcastIfEnabled(boolean enabled, CommandSender plReplace, String msg, String reason) {
		if (enabled)
			broadcastWithPlayer(msg + (reason.equals("") ? "" : " " + Localization.Parts.REASON.replace("{reason}", reason)), plReplace == null ? "" : resolvedSender(plReplace));
	}

	public static boolean hasPerm(CommandSender sender, String perm) {
		return sender.hasPermission(perm);
	}

	public static void customAction(final Player pl, final String action, final String msg) {
		if (action.isEmpty() || action.equalsIgnoreCase("none"))
			return;

		Bukkit.getScheduler().scheduleSyncDelayedTask(ChatControl.instance(), () -> {
			Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), colorize(action.replace("{player}", pl.getName()).replace("{message}", msg)));

		});
	}

	public static String insertDot(String msg) {
		if (!Settings.Chat.Grammar.INSERT_DOT || msg.length() < Settings.Chat.Grammar.INSERT_DOT_MSG_LENGTH)
			return msg;

		final String lastChar = msg.substring(msg.length() - 1);
		final String[] words = msg.split("\\s");
		final String lastWord = words[words.length - 1];

		if (!isDomain(lastWord) && lastChar.matches("(?i)[a-z]"))
			msg = msg + ".";

		return msg;
	}

	public static String capitalize(String msg) {
		if (!Settings.Chat.Grammar.CAPITALIZE || msg.length() < Settings.Chat.Grammar.CAPITALIZE_MSG_LENGTH)
			return msg;

		final String[] sentences = msg.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences) {
			final String word = msg.split("\\s")[0];

			if (!isDomain(word))
				sentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);

			tempMessage = tempMessage + sentence + " ";
		}
		return tempMessage.trim();
	}

	public static String colorize(String str) {
		if (str == null || str.isEmpty())
			return "";

		return ChatColor.translateAlternateColorCodes('&', setPrefix(str));
	}

	public static String consoleLine() {
		return "&6*----------------------------------------------*";
	}

	public static boolean doesPluginExist(String plugin) {
		return doesPluginExist(plugin, null);
	}

	public static boolean doesPluginExist(String plugin, String message) {
		final boolean hooked = Bukkit.getPluginManager().getPlugin(plugin) != null;
		if (hooked)
			Common.Log("&3Hooked into&8: &f" + plugin + (message != null ? " &7(" + message + ")" : ""));

		return hooked;
	}

	// ---------------------------- PRIVATE --------------------------------------

	private static String setPrefix(String str) {
		return str.replace("{prefix}", Localization.Parts.PREFIX).replace("{server}", Localization.Parts.PREFIX_SERVER);
	}

	private static boolean isDomain(String str) {
		return str.matches("(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}|www\\.[^\\s]+\\.[^\\s]{2,})");
	}

	private static String resolvedSender(CommandSender sender) {
		if (sender instanceof Player)
			return sender.getName();

		return Localization.Parts.CONSOLE;
	}

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
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

	// -------------------------------------------------------------------

	public static void Log(String... messages) {
		for (final String message : messages)
			for (final String part : splitNewline(message))
				console.sendMessage(colorize(INTERNAL_PREFIX + part.replace("\n", "\n&r")));
	}

	public static void LogFromParser(String str) {
		console.sendMessage(colorize(str.replace("{", "&6{&7").replace("}", "&6}&7").replace("=", " &d=&7 ").replace("[", "&b[&7").replace("]", "&b]&7")));
	}

	public static void LogInFrame(boolean disable, String... messages) {
		final String old = INTERNAL_PREFIX;
		INTERNAL_PREFIX = "";

		Log("&6*---------------- ChatControl -----------------*");
		for (final String msg : messages)
			Log(" &c" + msg);

		if (disable) {
			Bukkit.getPluginManager().disablePlugin(ChatControl.instance());
			Log(" &cPlugin is now disabled.");
		}

		Log(consoleLine());
		INTERNAL_PREFIX = old;
	}

	public static void Warn(String str) {
		Bukkit.getLogger().log(Level.WARNING, colorize(INTERNAL_PREFIX + str));
	}

	public static void Debug(String str) {
		if (Settings.DEBUG)
			console.sendMessage(colorize((INTERNAL_PREFIX.isEmpty() ? "" : "[ChatControl Debug] ") + str.replace("\n", "\n&r")));
	}

	public static void Verbose(String str) {
		if (Settings.VERBOSE_RULES || Settings.DEBUG)
			console.sendMessage(colorize(str.replace("\n", "\n&r")));
	}

	public static void Error(String str, Throwable ex) {
		Bukkit.getLogger().log(Level.SEVERE, "[ChatControl " + ChatControl.instance().getDescription().getVersion() + "] " + str, ex);
	}

	public static void Error(String str) {
		Bukkit.getLogger().log(Level.SEVERE, "[ChatControl " + ChatControl.instance().getDescription().getVersion() + "] " + str);
	}

	// -------------------------------------------------------------------

	public static String prepareForSimilarityCheck(String str) {
		if (Settings.AntiSpam.STRIP_SPECIAL_CHARS)
			str = str.replaceAll("[^a-zA-Z0-9\\s]", ""); // strip spec. characters EXCEPT spaces

		if (Settings.AntiSpam.STRIP_DUPLICATE_CHARS) {
			str = str.replaceAll("(.)(?=\\1\\1+)", "");
			str = str.replaceAll("(..)(?=\\1\\1+)", "");
			str = str.replaceAll("(...)(?=\\1\\1+)", "");
		}

		return stripColors(str.toLowerCase());
	}

	public static String stripDuplicate(String str) {
		str = str.replaceAll("(.)\\1+", "$1"); // hardcore duplicate strip
		return str;
	}

	public static String stripColors(String str) {
		// Own method -> str.replaceAll("(" + ChatColor.COLOR_CHAR +
		// "|&)([0-9a-fk-or])", "");
		return ChatColor.stripColor(str);
	}

	public static int[] checkCaps(String message) {
		final int[] editedMsg = new int[message.length()];
		final String[] parts = message.split(" ");

		for (int i = 0; i < parts.length; i++)
			for (final String whitelisted : Settings.AntiCaps.WHITELIST)
				if (whitelisted.equalsIgnoreCase(parts[i]))
					parts[i] = parts[i].toLowerCase();

		final String msg = StringUtils.join(parts, " ");

		for (int i = 0; i < msg.length(); i++)
			if (Character.isUpperCase(msg.charAt(i)) && Character.isLetter(msg.charAt(i)))
				editedMsg[i] = 1;
			else
				editedMsg[i] = 0;
		return editedMsg;
	}

	public static int percentageCaps(int[] caps) {
		int sum = 0;
		for (int i = 0; i < caps.length; i++)
			sum += caps[i];

		final double ratio = sum / caps.length;
		final int percent = (int) (100.0D * ratio);
		return percent;
	}

	public static int checkCapsInRow(int[] caps) {
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

	private static Matcher getMatcher(String regex, String plain_msg) {
		regex = stripColors(regex);
		plain_msg = stripColors(plain_msg);

		Pattern pattern = null;
		final TimedCharSequence timedMsg = new TimedCharSequence(plain_msg/* .toLowerCase() */, Settings.REGEX_TIMEOUT);

		try {
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		} catch (final PatternSyntaxException ex) {
			ex.printStackTrace();
			LogInFrame(false, "Malformed regex: \'" + regex + "\'", "Use online services (like &fregex101.com&f)", "for fixing errors");
			return null;
		}

		return pattern.matcher(timedMsg);
	}

	public static boolean regExMatch(String regex, String plain_msg) {
		final Matcher matcher = getMatcher(regex, plain_msg);

		try {
			return matcher.find();
		} catch (final RuntimeException ex) {
			Writer.Write(Writer.ERROR_PATH, null, "Regex check timed out (bad regex?) (plugin ver. " + ChatControl.instance().getDescription().getVersion() + ")!\nString checked: " + plain_msg + "\nRegex: " + regex + "");
			LogInFrame(false, "Regex timed out after " + Settings.REGEX_TIMEOUT + "ms! ", "&fREG EX&c: &7" + regex, "&fSTRING&c: &7" + plain_msg);

			return false;
		}
	}

	public static String replaceMatch(String regex, String plain_msg, String replacement) {
		final Matcher matcher = getMatcher(regex, plain_msg);

		try {
			return matcher.replaceAll(replacement);

		} catch (final RuntimeException ex) {
			return "";
		}
	}

	/**
	 * Calculates the similarity (a percentage within 0% and 100%) between two
	 * strings.
	 */
	public static int similarity(String s1, String s2) {
		String longer = s1, shorter = s2;

		if (s1.length() < s2.length()) { // longer should always have greater length
			longer = s2;
			shorter = s1;
		}

		final int longerLength = longer.length();

		if (longerLength == 0)
			return 100; /* both strings are zero length */

		final double result = (longerLength - editDistance(longer, shorter)) / (double) longerLength;

		return (int) (result * 100);

	}

	public static String shortLocation(Location loc) {
		return loc.getWorld().getName() + " x:" + (int) loc.getX() + " y:" + (int) loc.getY() + " z:" + (int) loc.getZ();
	}

	public static String getServerVersion() {
		final String packageName = Bukkit.getServer().getClass().getPackage().getName();

		return packageName.substring(packageName.lastIndexOf('.') + 1);
	}

	public static String lastColor(String msg) {
		return msg.substring(msg.lastIndexOf('&'), msg.length());
	}

	private static String[] splitNewline(String innocentMessage) {
		if (!Settings.ENFORCE_NEW_LINE)
			return innocentMessage.split("\n");

		final String GAY_DELIMITER = "kAnGaRkO_lUbI_ZmRzLiNkU";

		final char[] chars = innocentMessage.toCharArray();
		String parts = "";

		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];

			if ('\\' == c)
				if (i + 1 < chars.length) {
					final char next = chars[i + 1];

					if ('n' == next) {
						i++;

						parts += GAY_DELIMITER;
						continue;
					}
				}
			parts += c;
		}

		return parts.split(GAY_DELIMITER);
	}

	public static boolean isVanishedMeta(Player player) {
		if (player.hasMetadata("vanished"))
			for (final MetadataValue meta : player.getMetadata("vanished"))
				if (meta.asBoolean())
					return true;

		return false;
	}
}

class TimedCharSequence implements CharSequence {

	private final CharSequence message;
	private final int timeoutLimit;
	private final long timeoutTime;

	public TimedCharSequence(CharSequence message, int timeoutLimit) {
		this.message = message;
		this.timeoutLimit = timeoutLimit;
		timeoutTime = System.currentTimeMillis() + timeoutLimit;
	}

	@Override
	public char charAt(int index) {
		if (System.currentTimeMillis() > timeoutTime)
			throw new RuntimeException("\'" + message + "\' timed out after " + timeoutLimit + " ms! (malformed regex?)");

		return message.charAt(index);
	}

	@Override
	public int length() {
		return message.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new TimedCharSequence(message.subSequence(start, end), timeoutLimit);
	}

	@Override
	public String toString() {
		return message.toString();
	}
}