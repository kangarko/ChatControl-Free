package org.mineacademy.chatcontrol.settings;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.util.CompatProvider;

@SuppressWarnings("unused")
public class Localization extends ConfHelper {

	private Localization() {
	}

	protected static void load() throws Exception {
		// try if the user has his modified version of localization inside the plugin
		// folder
		file = new File(ChatControl.instance().getDataFolder(), "localization/" + Settings.LOCALIZATION);

		if (file.exists())
			cfg = YamlConfiguration.loadConfiguration(file);
		else {
			file = null;

			final InputStream is = ChatControl.class.getResourceAsStream("/localization/" + Settings.LOCALIZATION);
			Objects.requireNonNull(is, "Unknown locale: " + Settings.LOCALIZATION_SUFFIX + " (Possible causes: plugin does not have it or was reloaded)");

			try {
				cfg = CompatProvider.loadConfiguration(is);
			} catch (final NullPointerException ex) {
				throw new IllegalLocaleException();
			}
		}
		loadValues(Localization.class);
	}

	public static class Parts {
		public static String JOIN;
		public static String QUIT;
		public static String KICK;
		public static String PREFIX = "";
		public static String PREFIX_SERVER = "";
		public static String CONSOLE;
		public static String REASON;
		public static String SIGN;
		public static String COMMAND;
		public static CasusHelper SECONDS;

		private static final void init() {
			pathPrefix("General");

			JOIN = getString("Player_Join", "player join");
			QUIT = getString("Player_Quit", "player quit");
			KICK = getString("Player_Kick", "player kick");
			PREFIX = getString("Prefix", "&8[&3ChatControl&8]&7");
			PREFIX_SERVER = getString("Server_Prefix", "&8[&4Server&8]&c");
			CONSOLE = getString("Console", "&cserver");
			REASON = getString("Reason", "&7Reason: &f{reason}");
			SIGN = getString("Sign", "SIGN");
			COMMAND = getString("Command", "COMMAND");
			SECONDS = new CasusHelper(getString("Seconds", "second, seconds"));
		}
	}

	public static String WRONG_PARAMETERS;
	public static String WRONG_ARGUMENTS;

	public static String CANNOT_BROADCAST_EMPTY_MESSAGE;
	public static String CANNOT_CHAT_WHILE_MUTED;
	public static String CANNOT_COMMAND_WHILE_MUTED;
	public static String CANNOT_CHAT_UNTIL_MOVED;

	public static String ANTISPAM_SIMILAR_MESSAGE;
	public static String ANTISPAM_SIMILAR_COMMAND;
	public static String ANTISPAM_CAPS_MESSAGE;

	public static String COMMAND_WAIT_MESSAGE;
	public static String CHAT_WAIT_MESSAGE;

	public static String ANTIBOT_REJOIN_WAIT_MESSAGE;

	public static String MUTE_BROADCAST;
	public static String MUTE_UNMUTE_BROADCAST;
	public static String MUTE_ANON_BROADCAST;
	public static String MUTE_ANON_UNMUTE_BROADCAST;
	public static String MUTE_SUCCESS;
	public static String MUTE_UNMUTE_SUCCESS;

	public static String CLEAR_BROADCAST;
	public static String CLEAR_ANON_BROADCAST;
	public static String CLEAR_CONSOLE;
	public static String CLEAR_CONSOLE_MSG;
	public static String CLEAR_STAFF;

	public static String SIGNS_DUPLICATION;
	public static String SIGNS_DUPLICATION_STAFF;
	public static String SIGNS_BROKE;

	public static String USAGE_FAKE_CMD;
	public static String UPDATE_AVAILABLE;
	public static String NO_PERMISSION;
	public static String RELOAD_COMPLETE;
	public static String RELOAD_FAILED;

	private static final void init() {
		pathPrefix("Cannot");
		CANNOT_BROADCAST_EMPTY_MESSAGE = getString("Broadcast_Empty_Message", "&cMessage on {event} is empty. No broadcast.");
		CANNOT_CHAT_WHILE_MUTED = getString("Chat_While_Muted", "&7You cannot chat while the chat is muted!");
		CANNOT_COMMAND_WHILE_MUTED = getString("Command_While_Muted", "&7You cannot use this command while the chat is muted!");
		CANNOT_CHAT_UNTIL_MOVED = getString("Chat_Until_Moved", "&7You cannot chat until you move!"); // TODO radius?

		pathPrefix("Anti_Bot");
		ANTIBOT_REJOIN_WAIT_MESSAGE = getString("Rejoin_Message", "{prefix}\\n\\n&6Please wait &7{time} {seconds}&6 before logging in again.");

		pathPrefix("Anti_Spam");
		ANTISPAM_SIMILAR_MESSAGE = getString("Similar_Message", "&cPlease do not repeat the same (or similar) message.");
		ANTISPAM_SIMILAR_COMMAND = getString("Similar_Command", "&cPlease do not repeat the same (or similar) command.");
		ANTISPAM_CAPS_MESSAGE = getString("Too_Much_Caps", "&cDo not use so much CAPS LOCK!");
		COMMAND_WAIT_MESSAGE = getString("Command_Wait_Message", "&cPlease wait {time} {seconds} before your next command.");
		CHAT_WAIT_MESSAGE = getString("Chat_Wait_Message", "&cPlease wait {time} {seconds} before your next message.");

		pathPrefix("Chat_Mute");
		MUTE_SUCCESS = getString("Mute", "&7Chat was successfully muted.");
		MUTE_UNMUTE_SUCCESS = getString("Unmute", "&7Chat is no longer muted.");

		MUTE_BROADCAST = getString("Mute_Broadcast", "{server} &6{player} has muted the chat.");
		MUTE_UNMUTE_BROADCAST = getString("Unmute_Broadcast", "{server} &6{player} has unmuted the chat.");
		MUTE_ANON_BROADCAST = getString("Anonymous_Mute_Broadcast", "{server} Initiated global chat mute.");
		MUTE_ANON_UNMUTE_BROADCAST = getString("Anonymous_Unmute_Broadcast", "{server} Global chat mute cancelled.");

		pathPrefix("Chat_Clear");
		CLEAR_BROADCAST = getString("Broadcast", "{server} &6{player} cleared the chat.");
		CLEAR_ANON_BROADCAST = getString("Anonymous_Broadcast", "{server} The game chat was cleared.");
		CLEAR_CONSOLE = getString("Console_Player_Message", "{prefix} &7Console was successfully cleared.");
		CLEAR_CONSOLE_MSG = getString("Console_Message", "&7Console was cleared by {player}");
		CLEAR_STAFF = getString("Chat_Staff_Message", "&7^----- [ == &fChat was cleared by {player} &7== ] -----^");

		pathPrefix("Signs");
		SIGNS_DUPLICATION = getString("Duplicate_Text", "{server} You cannot make multiple signs with the same text!");
		SIGNS_DUPLICATION_STAFF = getString("Duplicate_Text_Staff", "&c{player} created signs with same text: {message}");
		SIGNS_BROKE = getString("Broke_When_Violated_A_Rule", "Your sign broke, there must be something wrong with it!");

		pathPrefix("Usage");
		USAGE_FAKE_CMD = getString("Fake_Command", "{prefix} Usage: /chatcontrol fake <&bjoin&f/&aleave&f/&ekick&f>");

		pathPrefix(null);
		UPDATE_AVAILABLE = getString("Update_Available", "&2A new version of &3ChatControl&2 is available.\\n&2Current version: &f{current}&2; New version: &f{new}\\n&2You can disable this notification in the config.");
		NO_PERMISSION = getString("No_Permission", "&cInsufficient permission ({permission}).");
		RELOAD_COMPLETE = getString("Reload_Complete", "{prefix} &2Configuration reloaded successfuly.");
		RELOAD_FAILED = getString("Reload_Failed", "{prefix} &cReloading configuration failed, check console. The error was: &4{error}");

		WRONG_PARAMETERS = getString("Wrong_Parameters", "&cWrong parameters or missing permission.");
		WRONG_ARGUMENTS = getString("Wrong_Arguments", "&cWrong arguments. Type &6/chc list&c for command list.");
	}
}
