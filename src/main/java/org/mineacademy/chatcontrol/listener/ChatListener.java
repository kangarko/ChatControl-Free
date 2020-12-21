package org.mineacademy.chatcontrol.listener;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.PlayerCache;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatPlayerChatEvent;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.Writer;

public class ChatListener implements Listener, EventExecutor {

	public ChatListener() {
		Common.Log("&3Starting &fchecker listener &3with " + Settings.ListenerPriority.CHECKER + " priority");
	}

	@Override
	public void execute(Listener listener, Event e) throws EventException {
		final CompatPlayerChatEvent event = new CompatPlayerChatEvent(e);

		onPlayerChat(event);
		event.install(e);
	}

	private void onPlayerChat(CompatPlayerChatEvent e) {
		if (CompatProvider.getAllPlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		final Player pl = e.getPlayer();
		final PlayerCache plData = ChatControl.getDataFor(pl);
		String message = e.getMessage();

		if (Settings.AntiBot.BLOCK_CHAT_UNTIL_MOVED && plData.loginLocation != null)
			if (pl.getLocation().equals(plData.loginLocation) && !Common.hasPerm(pl, Permissions.Bypasses.MOVE)) {
				Common.tell(pl, Localization.CANNOT_CHAT_UNTIL_MOVED);
				e.setCancelled(true);
				return;
			}

		if (ChatControl.muted && !Common.hasPerm(pl, Permissions.Bypasses.MUTE)) {
			Common.tell(pl, Localization.CANNOT_CHAT_WHILE_MUTED);
			e.setCancelled(true);
			return;
		}

		timeCheck: {
			final long now = System.currentTimeMillis() / 1000L;
			final int messageDelay = Settings.AntiSpam.Messages.DELAY.getFor(plData);

			if (now - plData.lastMessageTime < messageDelay) {
				if (Common.hasPerm(pl, Permissions.Bypasses.DELAY_CHAT) || isWhitelisted(message, Settings.AntiSpam.Messages.WHITELIST_DELAY))
					break timeCheck;

				final long time = messageDelay - (now - plData.lastMessageTime);

				Common.tell(pl, Localization.CHAT_WAIT_MESSAGE.replace("{time}", String.valueOf(time)).replace("{seconds}", Localization.Parts.SECONDS.formatNumbers(time)));
				e.setCancelled(true);
				return;

			}
			plData.lastMessageTime = now;
		}

		dupeCheck: if (Settings.AntiSpam.Messages.SIMILARITY > 0 && Settings.AntiSpam.Messages.SIMILARITY < 100) {
			if (Common.hasPerm(pl, Permissions.Bypasses.SIMILAR_CHAT) || isWhitelisted(message, Settings.AntiSpam.Messages.WHITELIST_SIMILARITY))
				break dupeCheck;

			final String strippedMsg = Common.prepareForSimilarityCheck(message);

			if (Common.similarity(strippedMsg, plData.lastMessage) > Settings.AntiSpam.Messages.SIMILARITY) {
				Common.tell(pl, Localization.ANTISPAM_SIMILAR_MESSAGE);
				e.setCancelled(true);
				return;
			}
			plData.lastMessage = strippedMsg;
		}

		if (Settings.Rules.CHECK_CHAT && !Common.hasPerm(e.getPlayer(), Permissions.Bypasses.RULES))
			message = ChatControl.instance().chatCeaser.parseRules(e, pl, message);

		if (e.isCancelled()) // cancelled from chat ceaser
			return;

		if (Settings.AntiCaps.ENABLED && !Common.hasPerm(pl, Permissions.Bypasses.CAPS))
			if (message.length() >= Settings.AntiCaps.MIN_MESSAGE_LENGTH) {
				final String msgBefore = message;
				final int[] newMessage = Common.checkCaps(message);
				if (Common.percentageCaps(newMessage) >= Settings.AntiCaps.MIN_CAPS_PERCENTAGE || Common.checkCapsInRow(newMessage) >= Settings.AntiCaps.MIN_CAPS_IN_A_ROW) {

					final String[] parts = message.split(" ");
					boolean capsAllowed = false;
					boolean whitelisted = false;

					for (int i = 0; i < parts.length; i++) {
						for (final String whitelist : Settings.AntiCaps.WHITELIST)
							if (whitelist.equalsIgnoreCase(parts[i])) {
								whitelisted = true;
								capsAllowed = true;
								continue;
							}

						if (Settings.AntiCaps.IGNORE_USERNAMES)
							for (final Player online : CompatProvider.getAllPlayers())
								if (online.getName().equalsIgnoreCase(parts[i])) {
									whitelisted = true;
									capsAllowed = true;
									continue;
								}

						if (!whitelisted) {
							if (!capsAllowed) {
								final char firstChar = parts[i].charAt(0);
								parts[i] = firstChar + parts[i].toLowerCase().substring(1);
							} else
								parts[i] = parts[i].toLowerCase();

							capsAllowed = !parts[i].endsWith(".") && !parts[i].endsWith("!");
						}

						whitelisted = false;
					}

					message = StringUtils.join(parts, " ");

					if (!msgBefore.equals(message) && Settings.AntiCaps.WARN_PLAYER)
						Common.tellLater(pl, 1, Localization.ANTISPAM_CAPS_MESSAGE);
				}
			}

		if (!Common.hasPerm(pl, Permissions.Bypasses.CAPITALIZE))
			message = Common.capitalize(message);
		if (!Common.hasPerm(pl, Permissions.Bypasses.PUNCTUATE))
			message = Common.insertDot(message);

		if (!message.equals(e.getMessage()))
			e.setMessage(message);

		if (Settings.Writer.ENABLED && !Settings.Writer.WHITELIST_PLAYERS.contains(pl.getName().toLowerCase()))
			Writer.Write(Writer.CHAT_PATH, pl.getName(), message);

		if (CompatProvider.hasSounds() && Settings.SoundNotify.ENABLED)
			if (Settings.SoundNotify.CHAT_PREFIX.equalsIgnoreCase("none")) {
				for (final Player online : CompatProvider.getAllPlayers())
					if (message.toLowerCase().contains(online.getName().toLowerCase()) && canSoundNotify(online.getName()) && Common.hasPerm(online, Permissions.Notify.WHEN_MENTIONED))
						online.playSound(online.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);

			} else
				for (final Player online : CompatProvider.getAllPlayers())
					if (message.toLowerCase().contains(Settings.SoundNotify.CHAT_PREFIX + online.getName().toLowerCase()) && canSoundNotify(online.getName()) && Common.hasPerm(online, Permissions.Notify.WHEN_MENTIONED))
						online.playSound(online.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
	}

	private boolean canSoundNotify(String pl) {
		if (!Settings.SoundNotify.ONLY_WHEN_AFK)
			return true;

		return HookManager.isAfk(pl);
	}

	private boolean isWhitelisted(String message, List<String> whitelist) {
		final boolean useRegex = Settings.AntiSpam.Messages.REGEX_IN_WHITELIST;

		for (final String whitelisted : whitelist)
			if (useRegex && Common.regExMatch(whitelisted, message))
				return true;
			else if (message.startsWith(whitelisted))
				return true;

		return false;
	}
}