package org.mineacademy.chatcontrol.listener;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UnknownFormatConversionException;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatPlayerChatEvent;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.GeoAPI;
import org.mineacademy.chatcontrol.util.GeoAPI.GeoResponse;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.Writer;

/**
 * A listener managing chat formatting.
 */
public final class ChatFormatListener implements Listener, EventExecutor {

	private final Pattern COLOR_REGEX = Pattern.compile("(?i)&([0-9A-F])");
	private final Pattern MAGIC_REGEN = Pattern.compile("(?i)&([K])");
	private final Pattern BOLD_REGEX = Pattern.compile("(?i)&([L])");
	private final Pattern STRIKETHROUGH_REGEX = Pattern.compile("(?i)&([M])");
	private final Pattern UNDERLINE_REGEX = Pattern.compile("(?i)&([N])");
	private final Pattern ITALIC_REGEX = Pattern.compile("(?i)&([O])");
	private final Pattern RESET_REGEX = Pattern.compile("(?i)&([R])");

	@Override
	public void execute(Listener listener, Event event) throws EventException {
		final CompatPlayerChatEvent compatibleEvent = new CompatPlayerChatEvent(event);

		onChatFormat(compatibleEvent);
		compatibleEvent.install(event);
	}

	private void onChatFormat(CompatPlayerChatEvent event) {
		final Player player = event.getPlayer();
		String message = event.getMessage();

		String format = Settings.Chat.Formatter.FORMAT;
		boolean rangedChat = Settings.Chat.Formatter.RANGED_MODE;

		if (rangedChat && message.startsWith("!") && Common.hasPermission(player, Permissions.Formatter.GLOBAL_CHAT)) {
			rangedChat = false;
			message = message.substring(1);

			format = Settings.Chat.Formatter.GLOBAL_FORMAT;
		}

		message = formatColor(message, player);

		format = format.replace("{display_name}", player.getName());
		format = replaceAllVariables(player, format);
		format = format.replace("{message}", message);

		try {
			event.setFormat(format.replace("%", "%%"));
		} catch (final UnknownFormatConversionException ex) {
			Common.logInFrame(false, "Chat format contains illegal characters!", "Applied format: " + format, "Correct it at &eChat.Formatter.Message_Format &cin settings.yml", "Error: " + ex);

			ex.printStackTrace();
		}

		event.setMessage(message);

		if (rangedChat) {
			event.getRecipients().clear();
			event.getRecipients().addAll(getLocalRecipients(player, message, Settings.Chat.Formatter.RANGE));
		}
	}

	private String replaceAllVariables(Player player, String message) {
		message = replacePlayerVariables(player, message);
		message = formatColor(message);
		message = replaceTime(message);

		return message;
	}

	public String replacePlayerVariables(Player player, String message) {
		if (HookManager.isPlaceholderAPILoaded())
			message = HookManager.replacePAPIPlaceholders(player, message);

		if (message.contains("{country_code}") || message.contains("{country_name}") || message.contains("{region_name}") || message.contains("{isp}")) {
			final GeoResponse geo = GeoAPI.getResponse(player.getAddress().getAddress());

			message = message.replace("{country_code}", geo != null ? geo.getCountryCode() : "").replace("{country_name}", geo != null ? geo.getCountryName() : "").replace("{region_name}", geo != null ? geo.getRegionName() : "").replace("{isp}", geo != null ? geo.getIsp() : "");
		}

		String prefix = formatColor(HookManager.getPlayerPrefix(player));
		String suffix = formatColor(HookManager.getPlayerSuffix(player));

		message = message
				.replace("{player_prefix}", prefix).replace("{player_suffix}", suffix)
				.replace("{pl_prefix}", prefix).replace("{pl_suffix}", suffix)
				.replace("{player}", player.getName()).replace("{display_name}", player.getDisplayName()).replace("{tab_name}", player.getPlayerListName()).replace("{nick}", HookManager.getNick(player))
				.replace("{world}", HookManager.getWorldAlias(player.getWorld().getName())).replace("{health}", formatHealth(player) + ChatColor.RESET)
				.replace("{town}", HookManager.getTownName(player)).replace("{nation}", HookManager.getNation(player))
				.replace("{faction}", HookManager.getFaction(player));

		return message;
	}

	private String replaceTime(String message) {
		final Calendar c = Calendar.getInstance();

		if (message.contains("{h"))
			message = message.replace("{h}", String.format("%02d", c.get(Calendar.HOUR)));

		if (message.contains("{H"))
			message = message.replace("{H}", String.format("%02d", c.get(Calendar.HOUR_OF_DAY)));

		if (message.contains("{g"))
			message = message.replace("{g}", Integer.toString(c.get(Calendar.HOUR)));

		if (message.contains("{G"))
			message = message.replace("{G}", Integer.toString(c.get(Calendar.HOUR_OF_DAY)));

		if (message.contains("{i"))
			message = message.replace("{i}", String.format("%02d", c.get(Calendar.MINUTE)));

		if (message.contains("{s"))
			message = message.replace("{s}", String.format("%02d", c.get(Calendar.SECOND)));

		if (message.contains("{a"))
			message = message.replace("{a}", c.get(Calendar.AM_PM) == 0 ? "am" : "pm");

		if (message.contains("{A"))
			message = message.replace("{A}", c.get(Calendar.AM_PM) == 0 ? "AM" : "PM");

		return message;
	}

	private String formatColor(String message) {
		return Common.colorize(message);
	}

	private String formatColor(String message, Player player) {
		if (message == null)
			return "";

		boolean canReset = false;

		if (Common.hasPermission(player, Permissions.Formatter.COLOR)) {
			message = COLOR_REGEX.matcher(message).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPermission(player, Permissions.Formatter.MAGIC)) {
			message = MAGIC_REGEN.matcher(message).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPermission(player, Permissions.Formatter.BOLD)) {
			message = BOLD_REGEX.matcher(message).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPermission(player, Permissions.Formatter.STRIKETHROUGH)) {
			message = STRIKETHROUGH_REGEX.matcher(message).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPermission(player, Permissions.Formatter.UNDERLINE)) {
			message = UNDERLINE_REGEX.matcher(message).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPermission(player, Permissions.Formatter.ITALIC)) {
			message = ITALIC_REGEX.matcher(message).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (canReset) {
			message = RESET_REGEX.matcher(message).replaceAll("\u00A7$1");
		}

		return message;
	}

	private String formatHealth(Player player) {
		final int health = CompatProvider.getHealth(player);

		return (health > 10 ? ChatColor.DARK_GREEN : health > 5 ? ChatColor.GOLD : ChatColor.RED) + "" + health;
	}

	private List<Player> getLocalRecipients(Player player, String message, double range) {
		final List<Player> recipients = new LinkedList<>();

		try {
			final Location playerLocation = player.getLocation();
			final double squaredDistance = Math.pow(range, 2.0D);

			for (final Player receiver : CompatProvider.getOnlinePlayers()) {
				if (receiver.getWorld().getName().equals(player.getWorld().getName()))
					if (Common.hasPermission(player, Permissions.Formatter.OVERRIDE_RANGED_WORLD) || playerLocation.distanceSquared(receiver.getLocation()) <= squaredDistance) {
						recipients.add(receiver);
						continue;
					}

				if (Common.hasPermission(receiver, Permissions.Formatter.SPY))
					Common.tell(receiver, replaceAllVariables(player, Settings.Chat.Formatter.SPY_FORMAT.replace("{message}", message)));
			}

			return recipients;
		} catch (final ArrayIndexOutOfBoundsException ex) {
			Common.log("(Range Chat) Got " + ex.getMessage() + ", trying (limited) backup.");
			Writer.write(Writer.ERROR_PATH, "Range Chat", player.getName() + ": \'" + message + "\' Resulted in error: " + ex.getMessage());

			if (Common.hasPermission(player, Permissions.Formatter.OVERRIDE_RANGED_WORLD)) {
				for (final Player recipient : CompatProvider.getOnlinePlayers())
					if (recipient.getWorld().equals(player.getWorld()))
						recipients.add(recipient);

			} else
				for (final Entity en : player.getNearbyEntities(range, range, range))
					if (en.getType() == EntityType.PLAYER)
						recipients.add((Player) en);
		}

		return recipients;
	}
}