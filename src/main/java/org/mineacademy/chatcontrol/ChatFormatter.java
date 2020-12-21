package org.mineacademy.chatcontrol;

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
import org.mineacademy.chatcontrol.util.GeoAPI.GeoResponse;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.Writer;

public class ChatFormatter implements Listener, EventExecutor {

	private final Pattern COLOR_REGEX = Pattern.compile("(?i)&([0-9A-F])");
	private final Pattern MAGIC_REGEN = Pattern.compile("(?i)&([K])");
	private final Pattern BOLD_REGEX = Pattern.compile("(?i)&([L])");
	private final Pattern STRIKETHROUGH_REGEX = Pattern.compile("(?i)&([M])");
	private final Pattern UNDERLINE_REGEX = Pattern.compile("(?i)&([N])");
	private final Pattern ITALIC_REGEX = Pattern.compile("(?i)&([O])");
	private final Pattern RESET_REGEX = Pattern.compile("(?i)&([R])");

	@Override
	public void execute(Listener listener, Event e) throws EventException {
		final CompatPlayerChatEvent event = new CompatPlayerChatEvent(e);

		onChatFormat(event);
		event.install(e);
	}

	private void onChatFormat(CompatPlayerChatEvent e) {
		final Player pl = e.getPlayer();
		String msg = e.getMessage();

		String format = Settings.Chat.Formatter.FORMAT;
		boolean rangedChat = Settings.Chat.Formatter.RANGED_MODE;

		if (rangedChat && msg.startsWith("!") && Common.hasPerm(pl, Permissions.Formatter.GLOBAL_CHAT)) {
			rangedChat = false;
			msg = msg.substring(1);

			format = Settings.Chat.Formatter.GLOBAL_FORMAT;
		}

		msg = formatColor(msg, pl);

		format = format.replace("{display_name}", pl.getName());
		format = replaceAllVariables(pl, format);
		format = format.replace("{message}", msg);

		try {
			e.setFormat(format.replace("%", "%%"));
		} catch (final UnknownFormatConversionException ex) {
			Common.LogInFrame(false, "Chat format contains illegal characters!", "Applied format: " + format, "Correct it at &eChat.Formatter.Message_Format &cin settings.yml", "Error: " + ex);

			ex.printStackTrace();
		}

		e.setMessage(msg);

		if (rangedChat) {
			e.getRecipients().clear();
			e.getRecipients().addAll(getLocalRecipients(pl, msg, Settings.Chat.Formatter.RANGE));
		}
	}

	private String replaceAllVariables(Player pl, String msg) {
		msg = replacePlayerVariables(pl, msg);
		msg = formatColor(msg);
		msg = replaceTime(msg);

		return msg;
	}

	public String replacePlayerVariables(Player pl, String msg) {
		if (HookManager.isPlaceholderAPILoaded())
			msg = HookManager.replacePAPIPlaceholders(pl, msg);

		if (msg.contains("{country_code}") || msg.contains("{country_name}") || msg.contains("{region_name}") || msg.contains("{isp}")) {
			final GeoResponse geo = ChatControl.getGeoFor(pl.getAddress().getAddress());

			msg = msg.replace("{country_code}", geo != null ? geo.getCountryCode() : "").replace("{country_name}", geo != null ? geo.getCountryName() : "").replace("{region_name}", geo != null ? geo.getRegionName() : "").replace("{isp}", geo != null ? geo.getIsp() : "");
		}

		msg = msg.replace("{pl_prefix}", formatColor(HookManager.getPlayerPrefix(pl))).replace("{pl_suffix}", formatColor(HookManager.getPlayerSuffix(pl)))
				.replace("{player}", pl.getName()).replace("{display_name}", pl.getDisplayName()).replace("{tab_name}", pl.getPlayerListName()).replace("{nick}", HookManager.getNick(pl))
				.replace("{world}", HookManager.getWorldAlias(pl.getWorld().getName())).replace("{health}", formatHealth(pl) + ChatColor.RESET)
				.replace("{town}", HookManager.getTownName(pl)).replace("{nation}", HookManager.getNation(pl))
				.replace("{faction}", HookManager.getFaction(pl));

		return msg;
	}

	private String replaceTime(String msg) {
		final Calendar c = Calendar.getInstance();

		if (msg.contains("{h"))
			msg = msg.replace("{h}", String.format("%02d", c.get(Calendar.HOUR)));

		if (msg.contains("{H"))
			msg = msg.replace("{H}", String.format("%02d", c.get(Calendar.HOUR_OF_DAY)));

		if (msg.contains("{g"))
			msg = msg.replace("{g}", Integer.toString(c.get(Calendar.HOUR)));

		if (msg.contains("{G"))
			msg = msg.replace("{G}", Integer.toString(c.get(Calendar.HOUR_OF_DAY)));

		if (msg.contains("{i"))
			msg = msg.replace("{i}", String.format("%02d", c.get(Calendar.MINUTE)));

		if (msg.contains("{s"))
			msg = msg.replace("{s}", String.format("%02d", c.get(Calendar.SECOND)));

		if (msg.contains("{a"))
			msg = msg.replace("{a}", c.get(Calendar.AM_PM) == 0 ? "am" : "pm");

		if (msg.contains("{A"))
			msg = msg.replace("{A}", c.get(Calendar.AM_PM) == 0 ? "AM" : "PM");

		return msg;
	}

	private String formatColor(String msg) {
		return Common.colorize(msg);
	}

	private String formatColor(String msg, Player pl) {
		if (msg == null)
			return "";

		boolean canReset = false;

		if (Common.hasPerm(pl, Permissions.Formatter.COLOR)) {
			msg = COLOR_REGEX.matcher(msg).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPerm(pl, Permissions.Formatter.MAGIC)) {
			msg = MAGIC_REGEN.matcher(msg).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPerm(pl, Permissions.Formatter.BOLD)) {
			msg = BOLD_REGEX.matcher(msg).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPerm(pl, Permissions.Formatter.STRIKETHROUGH)) {
			msg = STRIKETHROUGH_REGEX.matcher(msg).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPerm(pl, Permissions.Formatter.UNDERLINE)) {
			msg = UNDERLINE_REGEX.matcher(msg).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (Common.hasPerm(pl, Permissions.Formatter.ITALIC)) {
			msg = ITALIC_REGEX.matcher(msg).replaceAll("\u00A7$1");
			canReset = true;
		}

		if (canReset) {
			msg = RESET_REGEX.matcher(msg).replaceAll("\u00A7$1");
		}

		return msg;
	}

	private String formatHealth(Player pl) {
		final int health = CompatProvider.getHealth(pl);

		return (health > 10 ? ChatColor.DARK_GREEN : health > 5 ? ChatColor.GOLD : ChatColor.RED) + "" + health;
	}

	private List<Player> getLocalRecipients(Player pl, String msg, double range) {
		final List<Player> recipients = new LinkedList<>();

		try {
			final Location playerLocation = pl.getLocation();
			final double squaredDistance = Math.pow(range, 2.0D);

			for (final Player receiver : CompatProvider.getAllPlayers()) {
				if (receiver.getWorld().getName().equals(pl.getWorld().getName()))
					if (Common.hasPerm(pl, Permissions.Formatter.OVERRIDE_RANGED_WORLD) || playerLocation.distanceSquared(receiver.getLocation()) <= squaredDistance) {
						recipients.add(receiver);
						continue;
					}

				if (Common.hasPerm(receiver, Permissions.Formatter.SPY))
					Common.tell(receiver, replaceAllVariables(pl, Settings.Chat.Formatter.SPY_FORMAT.replace("{message}", msg)));
			}

			return recipients;
		} catch (final ArrayIndexOutOfBoundsException ex) {
			Common.Log("(Range Chat) Got " + ex.getMessage() + ", trying (limited) backup.");
			Writer.Write(Writer.ERROR_PATH, "Range Chat", pl.getName() + ": \'" + msg + "\' Resulted in error: " + ex.getMessage());

			if (Common.hasPerm(pl, Permissions.Formatter.OVERRIDE_RANGED_WORLD)) {
				for (final Player recipient : CompatProvider.getAllPlayers())
					if (recipient.getWorld().equals(pl.getWorld()))
						recipients.add(recipient);

			} else
				for (final Entity en : pl.getNearbyEntities(range, range, range))
					if (en.getType() == EntityType.PLAYER)
						recipients.add((Player) en);
		}

		return recipients;
	}
}