package org.mineacademy.chatcontrol.listener;

import java.util.List;

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

/**
 * Listens for chat event managing checks.
 */
public final class ChatCheckListener implements Listener, EventExecutor {

	public ChatCheckListener() {
		Common.log("&3Starting &fchecker listener &3with " + Settings.ListenerPriority.CHECKER + " priority");
	}

	@Override
	public void execute(Listener listener, Event event) throws EventException {
		final CompatPlayerChatEvent compatibleEvent = new CompatPlayerChatEvent(event);

		onPlayerChat(compatibleEvent);
		compatibleEvent.install(event);
	}

	private void onPlayerChat(CompatPlayerChatEvent event) {
		if (CompatProvider.getOnlinePlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		final Player player = event.getPlayer();
		final PlayerCache cache = ChatControl.getCache(player);
		String message = event.getMessage();

		if (Settings.AntiBot.BLOCK_CHAT_UNTIL_MOVED && cache.loginLocation != null)
			if (player.getLocation().equals(cache.loginLocation) && !Common.hasPermission(player, Permissions.Bypass.MOVE)) {
				Common.tell(player, Localization.CANNOT_CHAT_UNTIL_MOVED);
				event.setCancelled(true);
				return;
			}

		if (ChatControl.isMuted() && !Common.hasPermission(player, Permissions.Bypass.MUTE)) {
			Common.tell(player, Localization.CANNOT_CHAT_WHILE_MUTED);
			event.setCancelled(true);
			return;
		}

		timeCheck:
		{
			final long now = System.currentTimeMillis() / 1000L;
			final int messageDelay = Settings.AntiSpam.Messages.DELAY.getFor(cache);

			if (now - cache.lastMessageTime < messageDelay) {
				if (Common.hasPermission(player, Permissions.Bypass.DELAY_CHAT) || isWhitelisted(message, Settings.AntiSpam.Messages.WHITELIST_DELAY))
					break timeCheck;

				final long time = messageDelay - (now - cache.lastMessageTime);

				Common.tell(player, Localization.CHAT_WAIT_MESSAGE.replace("{time}", String.valueOf(time)).replace("{seconds}", Localization.Parts.SECONDS.formatNumbers(time)));
				event.setCancelled(true);
				return;

			}
			cache.lastMessageTime = now;
		}

		dupeCheck:
		if (Settings.AntiSpam.Messages.SIMILARITY > 0 && Settings.AntiSpam.Messages.SIMILARITY < 100) {
			if (Common.hasPermission(player, Permissions.Bypass.SIMILAR_CHAT) || isWhitelisted(message, Settings.AntiSpam.Messages.WHITELIST_SIMILARITY))
				break dupeCheck;

			final String strippedMsg = Common.stripUnicodeOrDuplicates(message);

			if (Common.getSimilarity(strippedMsg, cache.lastMessage) > Settings.AntiSpam.Messages.SIMILARITY) {
				Common.tell(player, Localization.ANTISPAM_SIMILAR_MESSAGE);
				event.setCancelled(true);
				return;
			}
			cache.lastMessage = strippedMsg;
		}

		if (Settings.Rules.CHECK_CHAT && !Common.hasPermission(event.getPlayer(), Permissions.Bypass.RULES))
			message = ChatControl.getInstance().getChatCeaser().parseRules(event, player, message);

		if (event.isCancelled()) // cancelled from chat ceaser
			return;

		if (Settings.AntiCaps.ENABLED && !Common.hasPermission(player, Permissions.Bypass.CAPS))
			if (message.length() >= Settings.AntiCaps.MIN_MESSAGE_LENGTH) {
				final String msgBefore = message;
				final int[] newMessage = Common.getCapsInRows(message);
				if (Common.getPercentageCaps(newMessage) >= Settings.AntiCaps.MIN_CAPS_PERCENTAGE || Common.getCapsInRowPercentage(newMessage) >= Settings.AntiCaps.MIN_CAPS_IN_A_ROW) {

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
							for (final Player online : CompatProvider.getOnlinePlayers())
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

					message = String.join(" ", parts);

					if (!msgBefore.equals(message) && Settings.AntiCaps.WARN_PLAYER)
						Common.tellLater(player, 1, Localization.ANTISPAM_CAPS_MESSAGE);
				}
			}

		if (!Common.hasPermission(player, Permissions.Bypass.CAPITALIZE) && Settings.Chat.Grammar.CAPITALIZE && message.length() >= Settings.Chat.Grammar.CAPITALIZE_MSG_LENGTH)
			message = Common.capitalize(message);

		if (!Common.hasPermission(player, Permissions.Bypass.PUNCTUATE) && Settings.Chat.Grammar.INSERT_DOT && message.length() >= Settings.Chat.Grammar.INSERT_DOT_MSG_LENGTH)
			message = Common.insertDot(message);

		if (!message.equals(event.getMessage()))
			event.setMessage(message);

		if (Settings.Writer.ENABLED && !Settings.Writer.WHITELIST_PLAYERS.contains(player.getName().toLowerCase()))
			Writer.write(Writer.CHAT_PATH, player.getName(), message);

		if (CompatProvider.hasSounds() && Settings.SoundNotify.ENABLED)
			if (Settings.SoundNotify.CHAT_PREFIX.equalsIgnoreCase("none")) {
				for (final Player online : CompatProvider.getOnlinePlayers())
					if (message.toLowerCase().contains(online.getName().toLowerCase()) && canSoundNotify(online.getName()) && Common.hasPermission(online, Permissions.Notify.WHEN_MENTIONED))
						online.playSound(online.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);

			} else
				for (final Player online : CompatProvider.getOnlinePlayers())
					if (message.toLowerCase().contains(Settings.SoundNotify.CHAT_PREFIX + online.getName().toLowerCase()) && canSoundNotify(online.getName()) && Common.hasPermission(online, Permissions.Notify.WHEN_MENTIONED))
						online.playSound(online.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
	}

	private boolean canSoundNotify(String player) {
		if (!Settings.SoundNotify.ONLY_WHEN_AFK)
			return true;

		return HookManager.isAfk(player);
	}

	private boolean isWhitelisted(String message, List<String> whitelist) {
		final boolean useRegex = Settings.AntiSpam.Messages.REGEX_IN_WHITELIST;

		for (final String whitelisted : whitelist)
			if (useRegex && Common.isRegexMatch(whitelisted, message))
				return true;
			else if (message.startsWith(whitelisted))
				return true;

		return false;
	}
}