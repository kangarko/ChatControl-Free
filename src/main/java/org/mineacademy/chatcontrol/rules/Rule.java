package org.mineacademy.chatcontrol.rules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.mineacademy.chatcontrol.util.Common;

import lombok.Getter;

/**
 * Represents a single rule.
 */
@Getter
public final class Rule {

	/**
	 * Required regular expression used against the checked message
	 */
	private final String match;

	/**
	 * A name/id of the rule. Used in some messages.
	 */
	private String id;

	/**
	 * Ignore following string/regular expression.
	 */
	private String ignoredMessage;

	/**
	 * Ignore player if they have specified permission.
	 */
	private String bypassPerm;

	/**
	 * Ignore events. Currently can be: chat, command or sign
	 */
	private Type ignoredEvent;

	/**
	 * Ignored gamemodes.
	 */
	private Set<GameMode> ignoredGamemodes;

	/**
	 * Required regular expression used before the message is checked to strip
	 * characters
	 */
	private String stripBefore;

	/**
	 * Required regular expression used before the message is checked to replace
	 * certain characters to another characters [0] = the regex to match what to be
	 * replaced, [1] = what to replace with
	 */
	private String[] replaceBefore;

	/**
	 * Replace the part of the checked message that matches {@link #match} with one
	 * of the string (randomly chosen)
	 */
	private String[] replacements;

	/**
	 * Rewrite the entire message to specified string chosen randomly
	 */
	private String[] rewrites;

	/**
	 * Optional commands executed as the server console divided by |
	 */
	private String[] commandsToExecute;

	/**
	 * A message to the player
	 */
	private String warnMessage;

	/**
	 * A permission required to get message {@link #customNotifyMessage}
	 */
	private String customNotifyPermission;

	/**
	 * A message broadcasted to players with {@link #customNotifyPermission}
	 */
	private String customNotifyMessage;

	/**
	 * A kick message.
	 */
	private String kickMessage;

	/**
	 * A handler that triggers when {@link #match} matches the checked message
	 */
	private Handler handler;

	/**
	 * Whenever the message should be cancelled from appearing
	 */
	private boolean cancellingEvent = false;

	/**
	 * Whenever the message should be logged and saved into a file.
	 */
	private boolean loggingEnabled = false;

	/**
	 * How much money to take from the player (Vault must be loaded)
	 */
	private Double fine;

	/**
	 * If set the rule is a {@link PacketRule};
	 */
	private PacketRule packetRule;

	/**
	 * Creates a new rule with provided regular expression
	 *
	 * @param match
	 *            the regular expression used against the checked message
	 */
	public Rule(String match) {
		this.match = match;
	}

	/**
	 * Checks specified message against regex {@link #match}
	 *
	 * @param message
	 *            The checked message
	 * @return if the message matches the {@link #match} regular expression
	 */
	public boolean matches(String message) {
		if (stripBefore != null)
			message = message.replaceAll(stripBefore, "");

		if (replaceBefore != null)
			message = message.replaceAll(replaceBefore[0], replaceBefore[1]);

		if (ignoredMessage != null && Common.isRegexMatch(ignoredMessage, message)) {
			Common.debug("&fIGNORE&c:&r " + ignoredMessage + ", message \'" + message + "\' ignored");

			message = Common.replaceMatch(ignoredMessage, message, ""); // prevent bypasses
		}

		return Common.isRegexMatch(match, message);
	}

	public void setId(String id) {
		Common.checkBoolean(this.id == null, "ID already set on: " + this);

		this.id = id;
	}

	public void setIgnoredMessage(String ignoredMessage) {
		Common.checkBoolean(this.ignoredMessage == null, "Ignored message already set on: " + this);

		this.ignoredMessage = ignoredMessage;
	}

	public void parseIgnoreEvent(String ignoreEvent) {
		Common.checkBoolean(ignoredEvent == null, "Ignored event already set on: " + this);

		ignoredEvent = parseRuleType(ignoreEvent);
	}

	public void parseIgnoredGamemodes(String line) {
		Common.checkBoolean(ignoredGamemodes == null, "Ignored gamemodes already set on: " + this);

		final HashSet<GameMode> modes = new HashSet<>();

		for (final String rawMode : line.split("\\|")) {
			GameMode gm;
			try {
				gm = GameMode.getByValue(Integer.parseInt(rawMode));
			} catch (final NumberFormatException ex) {
				gm = GameMode.valueOf(rawMode.toUpperCase());
			}

			modes.add(gm);
		}

		ignoredGamemodes = modes;
	}

	public String getBypassPerm() {
		return bypassPerm;
	}

	public void setBypassPerm(String bypassPerm) {
		Common.checkBoolean(this.bypassPerm == null, "Bypass permission already set on: " + this);

		this.bypassPerm = bypassPerm;
	}

	public void setStripBefore(String stripBefore) {
		Common.checkBoolean(this.stripBefore == null, "Strip before already set on: " + this);

		this.stripBefore = stripBefore;
	}

	// Example of the line that is parsed:
	// [aZ-01 ] with Hello World
	public void parseReplaceBefore(String line) {
		final String[] parts = line.split(" with ");
		Common.checkBoolean(parts.length == 2, "Malformed replace - must specify regex and a replacement in format: replace <regex> with <replacement>");

		Common.checkBoolean(replaceBefore == null, "Replace before already set on " + this);
		replaceBefore = parts;
	}

	public void parseReplacements(String line) {
		Common.checkBoolean(replacements == null, "Replacement already set on: " + this);

		replacements = line.split("\\|");
	}

	public void parseRewrites(String line) {
		Common.checkBoolean(rewrites == null, "Rewrite message already set on: " + this);

		rewrites = line.split("\\|");
	}

	public void parseCommandsToExecute(String line) {
		Common.checkBoolean(commandsToExecute == null, "Command to execute already set on: " + this);

		commandsToExecute = line.split("\\|");
	}

	public void setWarnMessage(String warnMessage) {
		Common.checkBoolean(this.warnMessage == null, "Warn message already set on: " + this);

		this.warnMessage = warnMessage;
	}

	public void parseCustomNotify(String raw) {
		final String[] parts = raw.split(" ");
		Common.checkBoolean(parts.length > 0, "Malformed then notify - must specify permission and a message.");

		final String permission = parts[0];

		Common.checkBoolean(customNotifyPermission == null, "Custom notify already set on " + this);
		customNotifyPermission = permission;
		customNotifyMessage = raw.replace(permission + " ", "");
	}

	public void setKickMessage(String kickMessage) {
		Common.checkBoolean(this.kickMessage == null, "Kick message already set on: " + this);

		this.kickMessage = kickMessage.isEmpty() ? "Kicked from the server" : kickMessage;
	}

	public void setHandler(Handler handler) {
		Common.checkBoolean(this.handler == null, "Handler already set on: " + this);

		this.handler = handler;
	}

	public void setCancellingEvent() {
		Common.checkBoolean(!cancellingEvent, "Message already set to be cancelled on: " + this);

		cancellingEvent = true;
	}

	public void setLoggingEnabled() {
		Common.checkBoolean(!loggingEnabled, "Rule already being logged on: " + this);

		loggingEnabled = true;
	}

	public void setFine(Double fine) {
		Common.checkBoolean(this.fine == null, "Fine already set on: " + this);

		this.fine = fine;
	}

	public void setPacketRule() {
		Common.checkBoolean(packetRule == null, "Rule is already a packet rule: " + this);

		packetRule = new PacketRule();
	}

	@Override
	public String toString() {
		return Common.stripColors(getPacketRule() != null ? getPacketRule().toString()
				: "Rule{\n" + (id != null ? "    Id = " + id + "\n" : "") + "    Match = \'" + match + "\',\n" + (stripBefore != null ? "    Strip Before Match = \'" + stripBefore + "\',\n" : "") + (bypassPerm != null ? "    Bypass With Perm = \'" + bypassPerm + "\',\n" : "") + (ignoredMessage != null ? "    Ignore Message = \'" + ignoredMessage + "\',\n" : "") + (ignoredEvent != null ? "    Ignore Event = \'" + ignoredEvent + "\',\n" : "")
						+ (replacements != null ? "    Replace With = \'" + String.join(",", replacements) + "\',\n" : "") + (rewrites != null ? "    Rewrite = \'" + rewrites + "\',\n" : "") + (commandsToExecute != null ? "    Execute Command = \'" + String.join(",", commandsToExecute) + "\',\n" : "") + (handler != null ? "    Handler = \'" + handler + "\',\n" : "") + (warnMessage != null ? "    Warn Message = \'" + warnMessage + "\',\n" : "")
						+ (cancellingEvent ? "    Deny = " + cancellingEvent + "\n" : "") + (loggingEnabled ? "    Log = " + loggingEnabled + "\n" : "") + "}");
	}

	/**
	 * Short version of {@link #toString()} that returns only regex used in this
	 * rule.
	 *
	 * @return rule's regular expression
	 */
	public String toShortString() {
		return (getPacketRule() != null ? "PacketRule" : "Rule") + " {" + (id != null ? "ID=" + id + "," : "") + "Match=\'" + match + "\'}";
	}

	private Type parseRuleType(String str) {
		Type ruleType = null;

		switch (str.toLowerCase().replace(" ", "")) {
			case "chat":
				ruleType = Type.CHAT;
				break;
			case "command":
			case "commands":
				ruleType = Type.COMMAND;
				break;
			case "sign":
			case "signs":
				ruleType = Type.SIGN;
				break;
		}

		Common.checkBoolean(ruleType != null && ruleType.canBeIgnored, "Unknown ignore event: " + str, " Valid: " + Arrays.asList(Type.values()));
		return ruleType;
	}

	/**
	 * Flags
	 */
	public enum Type {
		GLOBAL("rules.txt", false),
		PACKET("packets.txt", false),
		CHAT("chat.txt"),
		COMMAND("commands.txt"),
		SIGN("sign.txt");

		private final String fileName;
		private final boolean canBeIgnored;

		public String getFileName() {
			return fileName;
		}

		Type(String file) {
			this(file, true);
		}

		Type(String fileName, boolean canBeIgnored) {
			this.fileName = fileName;
			this.canBeIgnored = canBeIgnored;
		}
	}
}

/**
 * A special case rule used against chat packet. From normal rule uses only
 * {@link #match}
 */
@Getter
class PacketRule {

	/**
	 * Whenever the message should be cancelled from appearing.
	 */
	private boolean denyingPacket = false;

	/**
	 * A string used to replace matched part of the checked message.
	 */
	private String replacePacket;

	/**
	 * A message to replace the entire checked message.
	 */
	private String rewritePacket;

	/**
	 * Whenever the rule should not be logged into console even if Verbose is
	 * enabled.
	 */
	private boolean notVerbosing = false;

	/**
	 * A message to replace the entire checked message (custom per world).
	 *
	 */
	private HashMap<String, String> rewritePerWorld;

	public void setDenyingPacket() {
		Common.checkBoolean(!denyingPacket, "Rule is already denied: " + this);

		denyingPacket = true;
	}

	public void setReplacePacket(String replace) {
		Common.checkBoolean(this.replacePacket == null, "Replace already set on: " + this);

		this.replacePacket = replace;
	}

	public void setRewritePacket(String rewrite) {
		Common.checkBoolean(this.rewritePacket == null, "Rewrite already set on: " + this);

		this.rewritePacket = rewrite;
	}

	public void addRewriteIn(String line) {
		if (rewritePerWorld == null)
			rewritePerWorld = new HashMap<>();

		final String[] parts = line.split(" ");
		Common.checkBoolean(parts.length > 1, "Malformed rule then rewritein, please specify world and message! If you want to hide rule, set the message to \'none\'. Example: then rewritein hardcore &cCommand disabled in Hardcore world.");

		Common.checkBoolean(!rewritePerWorld.containsKey(parts[0]), "Rewrite already set in world: " + parts[0] + " to: " + rewritePerWorld.get(parts[0]) + " on: " + this);

		rewritePerWorld.put(parts[0], line.replace(parts[0] + " ", ""));
	}

	public void setNotVerbosing() {
		Common.checkBoolean(!notVerbosing, "Rule already being ignored from verbose: " + this);

		notVerbosing = true;
	}

	@Override
	public String toString() {
		return "PacketRule{\n" + (replacePacket != null ? "    Replace Word: \'" + replacePacket + "\'\n" : "") + (rewritePacket != null ? "    Rewrite With: \'" + rewritePacket + "\'\n" : "") + (rewritePerWorld != null ? "    Rewrite In Worlds: \'" + String.join(", ", rewritePerWorld.keySet()) + "\'\n" : "") + (notVerbosing ? "    Do Not Verbose: \'" + notVerbosing + "\'\n" : "") + "    Then Deny: " + denyingPacket + "\n" + "}";
	}
}