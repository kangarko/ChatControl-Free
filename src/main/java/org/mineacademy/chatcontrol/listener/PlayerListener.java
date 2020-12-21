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
import org.mineacademy.chatcontrol.util.LagCatcher;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.UpdateCheck;

public class PlayerListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPreLogin(AsyncPlayerPreLoginEvent e) {
		ChatControl.getGeoFor(e.getAddress());
	}

	@EventHandler(ignoreCancelled = true)
	public void onLogin(PlayerLoginEvent e) {
		final PlayerCache plData = ChatControl.getDataFor(e.getPlayer());
		final long difference = System.currentTimeMillis() / 1000L - plData.lastLogin;

		if (plData.lastLogin > 0 && difference < Settings.AntiBot.REJOIN_TIME) {
			final long time = Settings.AntiBot.REJOIN_TIME - difference;
			final String msg = Common.colorize(Localization.ANTIBOT_REJOIN_WAIT_MESSAGE.replace("{time}", String.valueOf(time)).replace("{seconds}", Localization.Parts.SECONDS.formatNumbers(time)));
			msg.split("\n");

			e.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER, msg);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		LagCatcher.start("Join event");

		final Player player = e.getPlayer();
		final long now = System.currentTimeMillis() / 1000L;
		final PlayerCache plData = ChatControl.getDataFor(player);

		if (!Common.hasPerm(player, Permissions.Bypasses.REJOIN))
			plData.lastLogin = now;

		plData.loginLocation = player.getLocation();

		if (player.getName().equals("kangarko"))
			Common.tellLater(player, 30, Common.consoleLine(), "&e Na serveri je nainstalovany ChatControl v" + ChatControl.instance().getDescription().getVersion() + "!", Common.consoleLine());

		if (UpdateCheck.needsUpdate && Settings.Updater.NOTIFY)
			for (final Player other : CompatProvider.getAllPlayers())
				if (Common.hasPerm(other, Permissions.Notify.UPDATE_AVAILABLE)) {
					final String sprava = Common.colorize(Localization.UPDATE_AVAILABLE).replace("{current}", ChatControl.instance().getDescription().getVersion()).replace("{new}", UpdateCheck.newVersion);
					sprava.split("\n");
					Common.tellLater(other, 4 * 20, sprava);
				}

		LagCatcher.end("Join event");

		if (Common.isVanishedMeta(player) || ChatControl.muted && Settings.Mute.SILENT_JOIN) {
			e.setJoinMessage(null);
			return;
		}

		final ChatMessage joinMessage = Settings.Messages.JOIN.getFor(plData);

		switch (joinMessage.getType()) {
			case HIDDEN:
				e.setJoinMessage(null);
				break;
			case CUSTOM:
				e.setJoinMessage(replacePlayerVariables(joinMessage.getMessage(), player));
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		handleLeaveMessage(e);

		if (Settings.RESET_CACHE_ON_QUIT)
			ChatControl.removeDataFor(e.getPlayer());
	}

	private void handleLeaveMessage(PlayerQuitEvent e) {
		final Player player = e.getPlayer();
		final PlayerCache plData = ChatControl.getDataFor(player);

		if (Settings.Messages.QUIT_ONLY_WHEN_LOGGED && !HookManager.isLogged(player)) {
			e.setQuitMessage(null);
			return;
		}

		if (Common.isVanishedMeta(player) || ChatControl.muted && Settings.Mute.SILENT_QUIT) {
			e.setQuitMessage(null);
			return;
		}

		final ChatMessage leaveMessage = Settings.Messages.QUIT.getFor(plData);

		switch (leaveMessage.getType()) {
			case HIDDEN:
				e.setQuitMessage(null);
				break;
			case CUSTOM:
				e.setQuitMessage(replacePlayerVariables(leaveMessage.getMessage(), player));
				break;
			default:
				break;
		}

	}

	@EventHandler(ignoreCancelled = true)
	public void onKick(PlayerKickEvent e) {
		final Player pl = e.getPlayer();
		final String reason = e.getReason();

		if (Common.hasPerm(pl, Permissions.Bypasses.SPAM_KICK) && (reason.equals("disconnect.spam") || reason.equals("Kicked for spamming"))) {
			e.setCancelled(true);
			return;
		}

		if (ChatControl.muted && Settings.Mute.SILENT_KICK) {
			e.setLeaveMessage(null);
			return;
		}

		final PlayerCache plData = ChatControl.getDataFor(pl);
		final ChatMessage kickMessage = Settings.Messages.KICK.getFor(plData);

		switch (kickMessage.getType()) {
			case HIDDEN:
				e.setLeaveMessage(null);
				break;
			case CUSTOM:
				e.setLeaveMessage(replacePlayerVariables(kickMessage.getMessage(), pl));
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (ChatControl.muted && Settings.Mute.SILENT_DEATHS)
			e.setDeathMessage(null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent e) {
		if (CompatProvider.getAllPlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		LagCatcher.start("Sign event");

		final Player pl = e.getPlayer();
		final PlayerCache plData = ChatControl.getDataFor(pl);
		String msg = e.getLine(0) + e.getLine(1) + e.getLine(2) + e.getLine(3);

		msg = msg.trim();

		if (Settings.Signs.DUPLICATION_CHECK && plData.lastSignText.equalsIgnoreCase(msg) && !Common.hasPerm(pl, Permissions.Bypasses.SIGN_DUPLICATION)) {
			if (Settings.Signs.DUPLICATION_ALERT_STAFF)
				for (final Player online : CompatProvider.getAllPlayers())
					if (!online.getName().equals(pl.getName()) && Common.hasPerm(online, Permissions.Notify.SIGN_DUPLICATION))
						Common.tell(online, Localization.SIGNS_DUPLICATION_STAFF.replace("{message}", msg), pl.getName());

			Common.tell(pl, Localization.SIGNS_DUPLICATION);
			e.setCancelled(true);

			if (Settings.Signs.DROP_SIGN)
				e.getBlock().breakNaturally();

			LagCatcher.end("Sign event");
			return;
		}

		if (Settings.Rules.CHECK_SIGNS && !Common.hasPerm(e.getPlayer(), Permissions.Bypasses.RULES)) {
			ChatControl.instance().chatCeaser.parseRules(e, pl, msg);

			if (e.isCancelled()) {
				Common.tellLater(pl, 2, Localization.SIGNS_BROKE); // display at the bottom
				e.setCancelled(true);

				if (Settings.Signs.DROP_SIGN)
					e.getBlock().breakNaturally();
			}
		}

		LagCatcher.end("Sign event");
	}

	private String replacePlayerVariables(String msg, Player pl) {
		msg = msg.replace("{player}", pl.getName());

		if (ChatControl.instance().formatter != null)
			msg = ChatControl.instance().formatter.replacePlayerVariables(pl, msg);

		return Common.colorize(msg);
	}
}