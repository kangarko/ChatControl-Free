package org.mineacademy.chatcontrol.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.PlayerCache;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.Writer;

/**
 * Listens for command event.
 */
public final class CommandListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		if (CompatProvider.getOnlinePlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		String command = event.getMessage();
		final String[] args = command.split(" ");

		final Player player = event.getPlayer();
		final PlayerCache cache = ChatControl.getCache(player);

		muteCheck:
		if (ChatControl.isMuted()) {
			if (Common.hasPermission(player, Permissions.Bypass.MUTE))
				break muteCheck;

			if (Settings.Mute.DISABLED_CMDS_WHEN_MUTED.contains(args[0].replaceFirst("/", ""))) {
				Common.tell(player, Localization.CANNOT_COMMAND_WHILE_MUTED);
				event.setCancelled(true);
				return;
			}
		}

		timeCheck:
		{
			final long now = System.currentTimeMillis() / 1000L;
			final int commandDelay = Settings.AntiSpam.Commands.DELAY.getFor(cache);

			if (now - cache.lastCommandTime < commandDelay) {
				if (Common.hasPermission(player, Permissions.Bypass.DELAY_COMMANDS))
					break timeCheck;

				if (Settings.AntiSpam.Commands.WHITELIST_DELAY.contains(args[0].replaceFirst("/", "")))
					break timeCheck;

				final long time = commandDelay - (now - cache.lastCommandTime);

				Common.tell(player, Localization.COMMAND_WAIT_MESSAGE.replace("{time}", String.valueOf(time)).replace("{seconds}", Localization.Parts.SECONDS.formatNumbers(time)));
				event.setCancelled(true);
				return;
			} else
				cache.lastCommandTime = now;
		}

		dupeCheck:
		if (Settings.AntiSpam.Commands.SIMILARITY > 0 && Settings.AntiSpam.Commands.SIMILARITY < 100) {
			String strippedCmd = command;

			// Strip from messages like /tell <player> <msg> the player name, making the
			// check less less annoying.
			if (Settings.AntiSpam.IGNORE_FIRST_ARGUMENTS_IN_CMDS && args.length > 2)
				strippedCmd = strippedCmd.replace(args[0], "").replace(args[1], "");

			strippedCmd = Common.stripUnicodeOrDuplicates(strippedCmd);

			if (Common.getSimilarity(strippedCmd, cache.lastCommand) > Settings.AntiSpam.Commands.SIMILARITY) {
				if (Common.hasPermission(player, Permissions.Bypass.SIMILAR_COMMANDS))
					break dupeCheck;

				if (Settings.AntiSpam.Commands.WHITELIST_SIMILARITY.contains(args[0].replaceFirst("/", "")))
					break dupeCheck;

				Common.tell(player, Localization.ANTISPAM_SIMILAR_COMMAND);
				event.setCancelled(true);
				return;
			}
			cache.lastCommand = strippedCmd;
		}

		if (Settings.Rules.CHECK_COMMANDS && !Common.hasPermission(event.getPlayer(), Permissions.Bypass.RULES))
			command = ChatControl.getInstance().getChatCeaser().parseRules(event, player, command);

		if (event.isCancelled()) { // some of the rule or handler has cancelled it
			return;
		}

		if (!command.equals(event.getMessage()))
			event.setMessage(command);

		if (Settings.Writer.ENABLED && !Settings.Writer.WHITELIST_PLAYERS.contains(player.getName()))
			for (final String prikaz : Settings.Writer.INCLUDE_COMMANDS)
				if (command.toLowerCase().startsWith("/" + prikaz.toLowerCase()))
					Writer.write(Writer.CHAT_PATH, "[CMD] " + player.getName(), command);

		sound:
		if (CompatProvider.hasSounds() && Settings.SoundNotify.ENABLED_IN_COMMANDS.contains(args[0].replaceFirst("/", "")))
			if (HookManager.isEssentialsLoaded() && (command.startsWith("/r ") || command.startsWith("/reply "))) {
				final Player replyPlayer = HookManager.getReplyTo(player.getName());

				if (replyPlayer != null && Common.hasPermission(replyPlayer, Permissions.Notify.WHEN_MENTIONED))
					replyPlayer.playSound(replyPlayer.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);

			} else if (args.length > 2) {
				final Player targetPlayer = Bukkit.getPlayer(args[1]);
				if (targetPlayer == null || !targetPlayer.isOnline())
					break sound;

				targetPlayer.playSound(targetPlayer.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
			}
	}
}