package org.mineacademy.chatcontrol.rules;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.mineacademy.chatcontrol.util.Common;

/**
 * Custom handler that handles message caught by {@link Rule}
 *
 * @author kangarko
 */
public class Handler {

	/**
	 * The name of the handler. It's automatically set from the handler name in
	 * handlers file.
	 */
	private final String name;

	/**
	 * The id/name of the rule associated with this handler.
	 */
	private String ruleID = "UNSET";

	/**
	 * A permission that makes player bypass the checks.
	 */
	private String bypassPermission;

	/**
	 * List of commands that will be ignored from the check.
	 */
	private List<String> ignoredInCommands;

	/**
	 * A message displayed to the player that triggered the handler. Set to 'none'
	 * to disable.
	 */
	private String playerWarnMsg;

	/**
	 * A message broadcasted to everyone. Set to 'none' to disable.
	 */
	private String broadcastMsg;

	/**
	 * People with specified permission will recieve {@link #staffAlertMsg}.
	 * Functional only when {@link #staffAlertMsg} is 'none'.
	 */
	private String staffAlertPermission;

	/**
	 * A message broadcasted to staff with {@link #staffAlertPermission}. Set to
	 * 'none' to disable.
	 */
	private String staffAlertMsg;

	/**
	 * A message logged in the server's console. Set to 'none' to disable.
	 */
	private String consoleMsg;

	/**
	 * A list of commands to be executed. Variables: {player} {message} Can be empty.
	 */
	private List<String> commandsToExecute;

	/**
	 * A name of a file that message will be writed in. Variables: {time} {player}
	 * {message} Set to 'none' to disable writing.
	 */
	private String writeToFileName;

	/**
	 * Should the message be blocked from appearing? This cancels for instance
	 * player chat event or command event.
	 */
	private boolean blockMessage = false;

	/**
	 * Economy. How much money to take from player? Requires Vault.
	 */
	private Double fine;

	/**
	 * A replacement that replaces only part of the message caught by a rule.
	 */
	private String msgReplacement;

	/**
	 * A message that replaces the entire message caught by a rule.
	 */
	private String rewriteTo;

	/**
	 * Creates a new handler instance.
	 *
	 * @param name
	 *            the name of this handler
	 */
	public Handler(String name, String ruleID) {
		this.name = name;

		if (ruleID != null)
			this.ruleID = ruleID;
	}

	public String getName() {
		return name;
	}

	public String getRuleID() {
		return ruleID;
	}

	public void setBypassPermission(String bypassPermission) {
		Validate.isTrue(this.bypassPermission == null, "Bypass permission already set for: " + this);

		this.bypassPermission = bypassPermission;
	}

	public String getBypassPermission() {
		return bypassPermission;
	}

	public List<String> getIgnoredInCommands() {
		return ignoredInCommands;
	}

	public void setIgnoredInCommands(List<String> ignoredInCommands) {
		Validate.isTrue(this.ignoredInCommands == null, "Ignored commands already set for: " + this);

		this.ignoredInCommands = ignoredInCommands;
	}

	public void setPlayerWarnMsg(String playerWarnMsg) {
		Validate.isTrue(this.playerWarnMsg == null, "Player warn message already set for: " + this);

		this.playerWarnMsg = playerWarnMsg;
	}

	public String getPlayerWarnMsg() {
		return playerWarnMsg;
	}

	public void setBroadcastMsg(String broadcastMsg) {
		Validate.isTrue(this.broadcastMsg == null, "Broadcast message already set for: " + this);

		this.broadcastMsg = broadcastMsg;
	}

	public String getBroadcastMsg() {
		return broadcastMsg;
	}

	public void setStaffAlertMsg(String staffAlertMsg) {
		Validate.isTrue(this.staffAlertMsg == null, "Staff alert message already set for: " + this);

		this.staffAlertMsg = staffAlertMsg;
	}

	public String getStaffAlertMsg() {
		return staffAlertMsg;
	}

	public void setStaffAlertPermission(String staffAlertPermission) {
		Objects.requireNonNull(staffAlertMsg, "Staff alert message is null, cannot get staff permission! Handler: " + this);

		this.staffAlertPermission = staffAlertPermission;
	}

	public String getStaffAlertPermission() {
		return staffAlertPermission;
	}

	public void setConsoleMsg(String consoleMsg) {
		Validate.isTrue(this.consoleMsg == null, "Console message already set for: " + this);

		this.consoleMsg = consoleMsg;
	}

	public String getConsoleMsg() {
		return consoleMsg;
	}

	public void setCommandsToExecute(List<String> commandsToExecute) {
		Validate.isTrue(this.commandsToExecute == null, "Commands to execute already set for: " + this);

		this.commandsToExecute = commandsToExecute;
	}

	public List<String> getCommandsToExecute() {
		return commandsToExecute;
	}

	public void setWriteToFileName(String writeToFileName) {
		Validate.isTrue(this.writeToFileName == null, "Write to file path already set for: " + this);

		this.writeToFileName = writeToFileName;
	}

	public String getWriteToFileName() {
		return writeToFileName;
	}

	public void setBlockMessage() {
		Validate.isTrue(!blockMessage, "Message is already blocked for: " + this);

		blockMessage = true;
	}

	public boolean blockMessage() {
		return blockMessage;
	}

	public Double getFine() {
		return fine;
	}

	public void parseFine(String line) {
		Validate.isTrue(fine == null, "Fine already set on: " + this);

		try {
			final double fine = Double.parseDouble(line);
			this.fine = fine;
		} catch (final NumberFormatException ex) {
			throw new RuntimeException("Corrupted fine value, expected double, got: " + line);
		}
	}

	public void setMsgReplacement(String msgReplacement) {
		Validate.isTrue(!blockMessage, "Replacement cannot be defined when the message is blocked: " + this);
		Validate.isTrue(rewriteTo == null, "Whole message replacement already defined for: " + this);

		this.msgReplacement = msgReplacement;
	}

	public String getMsgReplacement() {
		return msgReplacement;
	}

	public void setRewriteTo(String wholeMsgReplacement) {
		Validate.isTrue(!blockMessage, "Whole replacement cannot be defined when the message is blocked: " + this);
		Validate.isTrue(msgReplacement == null, "Part message replacement already defined for: " + this);

		rewriteTo = wholeMsgReplacement;
	}

	public String getRewriteTo() {
		return rewriteTo;
	}

	private String printCommands() {
		String commands = "(" + commandsToExecute.size() + ")";
		for (final String command : commandsToExecute)
			commands += command;

		return commands;
	}

	@Override
	public String toString() {
		return Common.stripColors("    Handler{\n" + "        Name: \'" + name + "\'\n" + (ruleID != null ? "        Rule ID: " + ruleID + "\n" : "") + (ignoredInCommands != null ? "        Ignored In Commands: " + ignoredInCommands + "\n" : "") + (bypassPermission != null ? "        Bypass Permission: \'" + bypassPermission + "\'\n" : "") + (playerWarnMsg != null ? "        Player Warn Msg: \'" + playerWarnMsg + "\'\n" : "")
				+ (broadcastMsg != null ? "        Broadcast Msg: \'" + broadcastMsg + "\'" : "") + (staffAlertPermission != null ? "        Staff Alert Permission: \'" + staffAlertPermission + "\'\n" : "") + (staffAlertMsg != null ? "        Staff Alert Msg: \'" + staffAlertMsg + "\'\n" : "") + (consoleMsg != null ? "        Console Msg: \'" + consoleMsg + "\'\n" : "") + (commandsToExecute != null ? "        Commands To Execute: \'" + printCommands() + "\'\n" : "")
				+ (writeToFileName != null ? "        Write To File Name: \'" + writeToFileName + "\'\n" : "") + (blockMessage ? "        Block Message: \'" + blockMessage + "\'\n" : "") + (msgReplacement != null ? "        Replace Part With: \'" + msgReplacement + "\'\n" : "") + (rewriteTo != null ? "        Replace Whole With: \'" + rewriteTo + "\'\n" : "") + "    }");
	}
}