package org.mineacademy.chatcontrol.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.GameMode;
import org.mineacademy.chatcontrol.util.Common;

/**
 * Represents a single rule
 * 
 * @author kangarko
 */
public class Rule {

	/**
	 * Flags
	 */
	public static enum Type {
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
	private String[] commandToExecute;

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
	private boolean cancel = false;

	/**
	 * Whenever the message should be logged and saved into a file.
	 */
	private boolean log = false;

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

		if (ignoredMessage != null && Common.regExMatch(ignoredMessage, message)) {
			Common.Debug("&fIGNORE&c:&r " + ignoredMessage + ", message \'" + message + "\' ignored");

			message = Common.replaceMatch(ignoredMessage, message, ""); // prevent bypasses
		}

		return Common.regExMatch(match, message);
	}

	public String getMatch() {
		return match;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		Validate.isTrue(this.id == null, "ID already set on: " + this);

		this.id = id;
	}

	public void setIgnoredMessage(String ignoredMessage) {
		Validate.isTrue(this.ignoredMessage == null, "Ignored message already set on: " + this);

		this.ignoredMessage = ignoredMessage;
	}

	public Type getIgnoredEvent() {
		return ignoredEvent;
	}

	public void parseIgnoreEvent(String ignoreEvent) {
		Validate.isTrue(ignoredEvent == null, "Ignored event already set on: " + this);

		ignoredEvent = parseRuleType(ignoreEvent);
	}

	public Set<GameMode> getIgnoredGamemodes() {
		return ignoredGamemodes;
	}

	public void parseIgnoredGamemodes(String line) {
		Validate.isTrue(ignoredGamemodes == null, "Ignored gamemodes already set on: " + this);

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
		Validate.isTrue(this.bypassPerm == null, "Bypass permission already set on: " + this);

		this.bypassPerm = bypassPerm;
	}

	public void setStripBefore(String stripBefore) {
		Validate.isTrue(this.stripBefore == null, "Strip before already set on: " + this);

		this.stripBefore = stripBefore;
	}

	// Example of the line that is parsed:
	// [aZ-01 ] with Hello World
	public void parseReplaceBefore(String line) {
		final String[] parts = line.split(" with ");
		Validate.isTrue(parts.length == 2, "Malformed replace - must specify regex and a replacement in format: replace <regex> with <replacement>");

		Validate.isTrue(replaceBefore == null, "Replace before already set on " + this);
		replaceBefore = parts;
	}

	public String[] getReplacements() {
		return replacements;
	}

	public void parseReplacements(String line) {
		Validate.isTrue(replacements == null, "Replacement already set on: " + this);

		replacements = line.split("\\|");
	}

	public String[] getRewrites() {
		return rewrites;
	}

	public void parseRewrites(String line) {
		Validate.isTrue(rewrites == null, "Rewrite message already set on: " + this);

		rewrites = line.split("\\|");
	}

	public String[] getCommandsToExecute() {
		return commandToExecute;
	}

	public void parseCommandsToExecute(String line) {
		Validate.isTrue(commandToExecute == null, "Command to execute already set on: " + this);

		commandToExecute = line.split("\\|");
	}

	public String getWarnMessage() {
		return warnMessage;
	}

	public void setWarnMessage(String warnMessage) {
		Validate.isTrue(this.warnMessage == null, "Warn message already set on: " + this);

		this.warnMessage = warnMessage;
	}

	public void parseCustomNotify(String raw) {
		final String[] parts = raw.split(" ");
		Validate.isTrue(parts.length > 0, "Malformed then notify - must specify permission and a message.");

		final String permission = parts[0];

		Validate.isTrue(customNotifyPermission == null, "Custom notify already set on " + this);
		customNotifyPermission = permission;
		customNotifyMessage = raw.replace(permission + " ", "");
	}

	public String getCustomNotifyMessage() {
		return customNotifyMessage;
	}

	public String getCustomNotifyPermission() {
		return customNotifyPermission;
	}

	public String getKickMessage() {
		return kickMessage;
	}

	public void setKickMessage(String kickMessage) {
		Validate.isTrue(this.kickMessage == null, "Kick message already set on: " + this);

		this.kickMessage = kickMessage.isEmpty() ? "Kicked from the server" : kickMessage;
	}

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		Validate.isTrue(this.handler == null, "Handler already set on: " + this);

		this.handler = handler;
	}

	public boolean cancelEvent() {
		return cancel;
	}

	public void setCancelEvent() {
		Validate.isTrue(!cancel, "Message already set to be cancelled on: " + this);

		cancel = true;
	}

	public boolean log() {
		return log;
	}

	public void setLog() {
		Validate.isTrue(!log, "Rule already being logged on: " + this);

		log = true;
	}

	public Double getFine() {
		return fine;
	}

	public void setFine(Double fine) {
		Validate.isTrue(this.fine == null, "Fine already set on: " + this);

		this.fine = fine;
	}

	public void setPacketRule() {
		Validate.isTrue(packetRule == null, "Rule is already a packet rule: " + this);

		packetRule = new PacketRule();
	}

	public PacketRule getPacketRule() {
		return packetRule;
	}

	@Override
	public String toString() {
		return Common.stripColors(getPacketRule() != null ? getPacketRule().toString() : "Rule{\n" + (id != null ? "    Id = " + id + "\n" : "") + "    Match = \'" + match + "\',\n" + (stripBefore != null ? "    Strip Before Match = \'" + stripBefore + "\',\n" : "") + (bypassPerm != null ? "    Bypass With Perm = \'" + bypassPerm + "\',\n" : "") + (ignoredMessage != null ? "    Ignore Message = \'" + ignoredMessage + "\',\n" : "") + (ignoredEvent != null ? "    Ignore Event = \'" + ignoredEvent + "\',\n" : "") + (replacements != null ? "    Replace With = \'" + StringUtils.join(replacements, ",") + "\',\n" : "") + (rewrites != null ? "    Rewrite = \'" + rewrites + "\',\n" : "") + (commandToExecute != null ? "    Execute Command = \'" + StringUtils.join(commandToExecute, ",") + "\',\n" : "") + (handler != null ? "    Handler = \'" + handler + "\',\n" : "") + (warnMessage != null ? "    Warn Message = \'" + warnMessage + "\',\n" : "") + (cancel ? "    Deny = " + cancel + "\n" : "") + (log ? "    Log = " + log + "\n" : "") + "}");
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

		Validate.isTrue(ruleType != null && ruleType.canBeIgnored, "Unknown ignore event: " + str, " Valid: " + StringUtils.join(Type.values(), ", "));
		return ruleType;
	}
}

/**
 * A special case rule used against chat packet. From normal rule uses only
 * {@link #match}
 */
class PacketRule {

	/**
	 * Whenever the message should be cancelled from appearing.
	 */
	private boolean deny = false;

	/**
	 * A string used to replace matched part of the checked message.
	 */
	private String replace;

	/**
	 * A message to replace the entire checked message.
	 */
	private String rewrite;

	/**
	 * Whenever the rule should not be logged into console even if Verbose is
	 * enabled.
	 */
	private boolean doNotVerbose = false;

	/**
	 * A message to replace the entire checked message (custom per world).
	 *
	 */
	private HashMap<String, String> rewritePerWorld;

	public void setDeny() {
		Validate.isTrue(!deny, "Rule is already denied: " + this);

		deny = true;
	}

	public boolean deny() {
		return deny;
	}

	public void setReplacePacket(String replace) {
		Validate.isTrue(this.replace == null, "Replace already set on: " + this);

		this.replace = replace;
	}

	public String getReplacePacket() {
		return replace;
	}

	public void setRewritePacket(String rewrite) {
		Validate.isTrue(this.rewrite == null, "Rewrite already set on: " + this);

		this.rewrite = rewrite;
	}

	public String getRewritePacket() {
		return rewrite;
	}

	public void addRewriteIn(String line) {
		if (rewritePerWorld == null)
			rewritePerWorld = new HashMap<>();

		final String[] parts = line.split(" ");
		Validate.isTrue(parts.length > 1, "Malformed rule then rewritein, please specify world and message! If you want to hide rule, set the message to \'none\'. Example: then rewritein hardcore &cCommand disabled in Hardcore world.");

		Validate.isTrue(!rewritePerWorld.containsKey(parts[0]), "Rewrite already set in world: " + parts[0] + " to: " + rewritePerWorld.get(parts[0]) + " on: " + this);

		rewritePerWorld.put(parts[0], line.replace(parts[0] + " ", ""));
	}

	public HashMap<String, String> getRewritePerWorld() {
		return rewritePerWorld;
	}

	public boolean doNotVerboe() {
		return doNotVerbose;
	}

	public void setDoNotVerbose() {
		Validate.isTrue(!doNotVerbose, "Rule already being ignored from verbose: " + this);

		doNotVerbose = true;
	}

	@Override
	public String toString() {
		return "PacketRule{\n" + (replace != null ? "    Replace Word: \'" + replace + "\'\n" : "") + (rewrite != null ? "    Rewrite With: \'" + rewrite + "\'\n" : "") + (rewritePerWorld != null ? "    Rewrite In Worlds: \'" + StringUtils.join(rewritePerWorld.keySet().toArray(), ", ") + "\'\n" : "") + (doNotVerbose ? "    Do Not Verbose: \'" + doNotVerbose + "\'\n" : "") + "    Then Deny: " + deny + "\n" + "}";
	}
}