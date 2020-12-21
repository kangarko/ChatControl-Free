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
import org.mineacademy.chatcontrol.util.LagCatcher;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.Writer;

public class CommandListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
		if (CompatProvider.getAllPlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		LagCatcher.start("Command event");

		String command = e.getMessage();
		final String[] args = command.split(" ");

		final Player pl = e.getPlayer();
		final PlayerCache plData = ChatControl.getDataFor(pl);

		muteCheck: if (ChatControl.muted) {
			if (Common.hasPerm(pl, Permissions.Bypasses.MUTE))
				break muteCheck;

			if (Settings.Mute.DISABLED_CMDS_WHEN_MUTED.contains(args[0].replaceFirst("/", ""))) {
				Common.tell(pl, Localization.CANNOT_COMMAND_WHILE_MUTED);
				e.setCancelled(true);
				LagCatcher.end("Command event");
				return;
			}
		}

		timeCheck: {
			final long now = System.currentTimeMillis() / 1000L;
			final int commandDelay = Settings.AntiSpam.Commands.DELAY.getFor(plData);

			if (now - plData.lastCommandTime < commandDelay) {
				if (Common.hasPerm(pl, Permissions.Bypasses.DELAY_COMMANDS))
					break timeCheck;

				if (Settings.AntiSpam.Commands.WHITELIST_DELAY.contains(args[0].replaceFirst("/", "")))
					break timeCheck;

				final long time = commandDelay - (now - plData.lastCommandTime);

				Common.tell(pl, Localization.COMMAND_WAIT_MESSAGE.replace("{time}", String.valueOf(time)).replace("{seconds}", Localization.Parts.SECONDS.formatNumbers(time)));
				e.setCancelled(true);
				LagCatcher.end("Command event");
				return;
			} else
				plData.lastCommandTime = now;
		}

		dupeCheck: if (Settings.AntiSpam.Commands.SIMILARITY > 0 && Settings.AntiSpam.Commands.SIMILARITY < 100) {
			String strippedCmd = command;

			// Strip from messages like /tell <player> <msg> the player name, making the
			// check less less annoying.
			if (Settings.AntiSpam.IGNORE_FIRST_ARGUMENTS_IN_CMDS && args.length > 2)
				strippedCmd = strippedCmd.replace(args[0], "").replace(args[1], "");

			strippedCmd = Common.prepareForSimilarityCheck(strippedCmd);

			if (Common.similarity(strippedCmd, plData.lastCommand) > Settings.AntiSpam.Commands.SIMILARITY) {
				if (Common.hasPerm(pl, Permissions.Bypasses.SIMILAR_COMMANDS))
					break dupeCheck;

				if (Settings.AntiSpam.Commands.WHITELIST_SIMILARITY.contains(args[0].replaceFirst("/", "")))
					break dupeCheck;

				Common.tell(pl, Localization.ANTISPAM_SIMILAR_COMMAND);
				e.setCancelled(true);
				LagCatcher.end("Command event");
				return;
			}
			plData.lastCommand = strippedCmd;
		}

		if (Settings.Rules.CHECK_COMMANDS && !Common.hasPerm(e.getPlayer(), Permissions.Bypasses.RULES))
			command = ChatControl.instance().chatCeaser.parseRules(e, pl, command);

		if (e.isCancelled()) { // some of the rule or handler has cancelled it
			LagCatcher.end("Command event");
			return;
		}

		if (!command.equals(e.getMessage()))
			e.setMessage(command);

		if (Settings.Writer.ENABLED && !Settings.Writer.WHITELIST_PLAYERS.contains(pl.getName()))
			for (final String prikaz : Settings.Writer.INCLUDE_COMMANDS)
				if (command.toLowerCase().startsWith("/" + prikaz.toLowerCase()))
					Writer.Write(Writer.CHAT_PATH, "[CMD] " + pl.getName(), command);

		sound: if (CompatProvider.hasSounds() && Settings.SoundNotify.ENABLED_IN_COMMANDS.contains(args[0].replaceFirst("/", "")))
			if (HookManager.isEssentialsLoaded() && (command.startsWith("/r ") || command.startsWith("/reply "))) {
				final Player reply = HookManager.getReplyTo(pl.getName());

				if (reply != null && Common.hasPerm(reply, Permissions.Notify.WHEN_MENTIONED))
					reply.playSound(reply.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
			} else if (args.length > 2) {
				final Player player = Bukkit.getPlayer(args[1]);
				if (player == null || !player.isOnline())
					break sound;

				player.playSound(player.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
			}

		LagCatcher.end("Command event");
	}
}