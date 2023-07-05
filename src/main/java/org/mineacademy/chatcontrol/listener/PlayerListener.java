package org.mineacademy.chatcontrol.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.PlayerCache;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.settings.ConfHelper.ChatMessage;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.GeoAPI;
import org.mineacademy.chatcontrol.util.Permissions;

/**
 * Listens for player-related events.
 */
public final class PlayerListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPreLogin(AsyncPlayerPreLoginEvent event) {
		GeoAPI.getResponse(event.getAddress());
	}

	@EventHandler(ignoreCancelled = true)
	public void onLogin(PlayerLoginEvent event) {
		final PlayerCache cache = ChatControl.getCache(event.getPlayer());
		final long difference = System.currentTimeMillis() / 1000L - cache.lastLogin;

		if (cache.lastLogin > 0 && difference < Settings.AntiBot.REJOIN_TIME) {
			final long time = Settings.AntiBot.REJOIN_TIME - difference;
			final String message = Common.colorize(Localization.ANTIBOT_REJOIN_WAIT_MESSAGE.replace("{time}", String.valueOf(time)).replace("{seconds}", Localization.Parts.SECONDS.formatNumbers(time)));

			message.split("\n");
			event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER, message);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final long now = System.currentTimeMillis() / 1000L;
		final PlayerCache cache = ChatControl.getCache(player);

		if (!Common.hasPermission(player, Permissions.Bypass.REJOIN))
			cache.lastLogin = now;

		cache.loginLocation = player.getLocation();

		// Developer easter egg
		if (player.getName().equals("kangarko"))
			Common.tellLater(player, 30, Common.consoleLine(), "&e Na serveri je nainstalovany ChatControl v" + ChatControl.getInstance().getDescription().getVersion() + "!", Common.consoleLine());

		if (Common.isVanishedMeta(player) || ChatControl.isMuted() && Settings.Mute.SILENT_JOIN) {
			event.setJoinMessage(null);
			return;
		}

		final ChatMessage joinMessage = Settings.Messages.JOIN.getFor(cache);

		switch (joinMessage.getType()) {
			case HIDDEN:
				event.setJoinMessage(null);
				break;
			case CUSTOM:
				event.setJoinMessage(this.replacePlayerVariables(joinMessage.getMessage(), player));
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.handleLeaveMessage(event);

		if (Settings.RESET_CACHE_ON_QUIT)
			ChatControl.removeCache(event.getPlayer());
	}

	private void handleLeaveMessage(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final PlayerCache cache = ChatControl.getCache(player);

		if (Settings.Messages.QUIT_ONLY_WHEN_LOGGED && !HookManager.isLogged(player)) {
			event.setQuitMessage(null);
			return;
		}

		if (Common.isVanishedMeta(player) || ChatControl.isMuted() && Settings.Mute.SILENT_QUIT) {
			event.setQuitMessage(null);
			return;
		}

		final ChatMessage leaveMessage = Settings.Messages.QUIT.getFor(cache);

		switch (leaveMessage.getType()) {
			case HIDDEN:
				event.setQuitMessage(null);
				break;
			case CUSTOM:
				event.setQuitMessage(this.replacePlayerVariables(leaveMessage.getMessage(), player));
				break;
			default:
				break;
		}

	}

	@EventHandler(ignoreCancelled = true)
	public void onKick(PlayerKickEvent event) {
		final Player player = event.getPlayer();
		final String reason = event.getReason();

		if (Common.hasPermission(player, Permissions.Bypass.SPAM_KICK) && (reason.equals("disconnect.spam") || reason.equals("Kicked for spamming"))) {
			event.setCancelled(true);
			return;
		}

		if (ChatControl.isMuted() && Settings.Mute.SILENT_KICK) {
			event.setLeaveMessage(null);
			return;
		}

		final PlayerCache cache = ChatControl.getCache(player);
		final ChatMessage kickMessage = Settings.Messages.KICK.getFor(cache);

		switch (kickMessage.getType()) {
			case HIDDEN:

				try {
					event.setLeaveMessage(null);

				} catch (final Throwable t) {
					// MC 1.16 on Paper with Adventure
					event.setLeaveMessage("");
				}

				break;
			case CUSTOM:
				event.setLeaveMessage(this.replacePlayerVariables(kickMessage.getMessage(), player));
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (ChatControl.isMuted() && Settings.Mute.SILENT_DEATHS)
			event.setDeathMessage(null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		if (CompatProvider.getOnlinePlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		final Player player = event.getPlayer();
		final PlayerCache cache = ChatControl.getCache(player);
		String message = event.getLine(0) + event.getLine(1) + event.getLine(2) + event.getLine(3);

		message = message.trim();

		if (Settings.Signs.DUPLICATION_CHECK && cache.lastSignText.equalsIgnoreCase(message) && !Common.hasPermission(player, Permissions.Bypass.SIGN_DUPLICATION)) {
			if (Settings.Signs.DUPLICATION_ALERT_STAFF)
				for (final Player online : CompatProvider.getOnlinePlayers())
					if (!online.getName().equals(player.getName()) && Common.hasPermission(online, Permissions.Notify.SIGN_DUPLICATION))
						Common.tell(online, Localization.SIGNS_DUPLICATION_STAFF.replace("{message}", message), player.getName());

			Common.tell(player, Localization.SIGNS_DUPLICATION);
			event.setCancelled(true);

			if (Settings.Signs.DROP_SIGN)
				event.getBlock().breakNaturally();

			return;
		}

		if (Settings.Rules.CHECK_SIGNS && !Common.hasPermission(event.getPlayer(), Permissions.Bypass.RULES)) {
			ChatControl.getInstance().getChatCeaser().parseRules(event, player, message);

			if (event.isCancelled()) {
				Common.tellLater(player, 2, Localization.SIGNS_BROKE); // display at the bottom
				event.setCancelled(true);

				if (Settings.Signs.DROP_SIGN)
					event.getBlock().breakNaturally();
			}
		}
	}

	private String replacePlayerVariables(String message, Player player) {
		message = message.replace("{player}", player.getName());

		if (ChatControl.getInstance().getFormatter() != null)
			message = ChatControl.getInstance().getFormatter().replacePlayerVariables(player, message);

		return Common.colorize(message);
	}
}