package org.mineacademy.chatcontrol.util;

/**
 * This class holds all permissions used by plugin.
 *
 * If you are not familiar with Java and want to use these permissions, just add
 * a player permission from the brackets which looks like:
 *
 * public static final String MUTE = "chatcontrol.commands.mute";
 *
 * Here the permission is: chatcontrol.commands.mute
 */
public class Permissions {

	private Permissions() {
	}

	public class Commands {
		// Permission muting game chat with "/chc mute" command.
		public static final String MUTE = "chatcontrol.commands.mute";

		// Muting without broadcasting any message with "/chc mute -silent" command.
		public static final String MUTE_SILENT = "chatcontrol.commands.mute.silent";

		// Muting without broadcasting player who muted the chat with "/chc mute
		// -anonymous" command.
		public static final String MUTE_ANONYMOUS = "chatcontrol.commands.mute.anonymous";

		// Clearing game chat with "/chc clear" command.
		public static final String CLEAR = "chatcontrol.commands.clear";

		// Chat clear without broadcasting any message with "/chc clear -silent"
		// command.
		public static final String CLEAR_SILENT = "chatcontrol.commands.clear.silent";

		// Chat clear without broadcasting player who cleared the chat with "/chc clear
		// -anonymous" command.
		public static final String CLEAR_ANONYMOUS = "chatcontrol.commands.clear.anonymous";

		// Clearing the console with "/chc clear -console" command.
		public static final String CLEAR_CONSOLE = "chatcontrol.commands.clear.console";

		// Reloading the configuration with "/chc reload" command.
		public static final String RELOAD = "chatcontrol.commands.reload";

		// Displaying help for the plugin with "/chc list" command.
		public static final String LIST = "chatcontrol.commands.list";

		// Broadcasting fake join/kick/leave messages with "/chc fake" command.
		public static final String FAKE = "chatcontrol.commands.fake";
	}

	public class Bypasses {
		// Allow player to chat while the chat is muted.
		public static final String MUTE = "chatcontrol.bypass.mute";

		// Bypass command message delay.
		public static final String DELAY_COMMANDS = "chatcontrol.bypass.time.commands";

		// Bypass chat message delay.
		public static final String DELAY_CHAT = "chatcontrol.bypass.time.chat";

		// Allow a player to send duplicate/similar commands.
		public static final String SIMILAR_COMMANDS = "chatcontrol.bypass.dupe.commands";

		// Allow a player to send duplicate/similar messages.
		public static final String SIMILAR_CHAT = "chatcontrol.bypass.dupe.chat";

		// Allow player to chat even when they did not moved since joining the server.
		// Not recommended due to spam bots.
		public static final String MOVE = "chatcontrol.bypass.move";

		// Bypass check for CAPS.
		public static final String CAPS = "chatcontrol.bypass.caps";

		// Bypass rejoin check. Not recommended due to spam bots.
		public static final String REJOIN = "chatcontrol.bypass.rejoin";

		// Allow player to tab complete. (Notice: If you are using Spigot it is highly
		// recommended to disable this feature and use the inbuilt one in spigot.yml)
		public static final String TAB_COMPLETE = "chatcontrol.bypass.tabcomplete";

		// Player's chat don't clears, they receive just one message instead.
		public static final String CHAT_CLEARING = "chatcontrol.bypass.clear";

		// Player's messages will not be replaced from replace lists in Chat.yml.
		public static final String CHARACTER_REPLACE = "chatcontrol.bypass.replace";

		// Player's messages will not be capitalized.
		public static final String CAPITALIZE = "chatcontrol.bypass.capitalize";

		// No dot will not be inserted after player's messages.
		public static final String PUNCTUATE = "chatcontrol.bypass.punctuate"; // TODO NB: Permission changed in v5.0.0 from insertdot to
																				// punctuate.

		// Allow player to make multiple signs with same text.
		public static final String SIGN_DUPLICATION = "chatcontrol.bypass.signduplication";

		// Allow player to bypass custom rules and handlers.
		public static final String RULES = "chatcontrol.bypass.rules";

		// Allow player to bypass vanilla's "disconnect.spam" kick when sending messages
		// too quickly.
		public static final String SPAM_KICK = "chatcontrol.bypass.spamkick";
	}

	public class Notify {
		// Receive a warning when a player makes multiple signs with the same text.
		public static final String SIGN_DUPLICATION = "chatcontrol.notify.signduplication";

		// Receive a warning when a new version of plugin is available.
		public static final String UPDATE_AVAILABLE = "chatcontrol.notify.update";

		// Receive a sound warning when somebody mentions you in the chat. (Highly
		// configurable.)
		public static final String WHEN_MENTIONED = "chatcontrol.notify.whenmentioned";
	}

	public class Formatter {
		// Permission to use colors 1-9 and a-f with the '&' character.
		public static final String COLOR = "chatcontrol.chat.format.color";

		// Permission to use '&k' magic character.
		public static final String MAGIC = "chatcontrol.chat.format.magic";

		// Permission to use '&l' character to make messages bold.
		public static final String BOLD = "chatcontrol.chat.format.bold";

		// Permission to use '&m' character to strikethrough the messages.
		public static final String STRIKETHROUGH = "chatcontrol.chat.format.strikethrough";

		// Permission to use '&n' character to underline the messages.
		public static final String UNDERLINE = "chatcontrol.chat.format.underline";

		// Permission to use '&o' character make the messages italics.
		public static final String ITALIC = "chatcontrol.chat.format.italic";

		// Permission to speak in global chat when chat range is enabled and message
		// starts with '!'.
		public static final String GLOBAL_CHAT = "chatcontrol.chat.global";

		// Permission to receive everyone's messages when ranged mode is enabled.
		public static final String SPY = "chatcontrol.chat.spy";

		// Permission speak for all players in the entire world when ranged mode is
		// enabled.
		public static final String OVERRIDE_RANGED_WORLD = "chatcontrol.chat.overrideranged";
	}

	public static final String VIEW_TIMED_MESSAGES = "chatcontrol.broadcaster.view";
}