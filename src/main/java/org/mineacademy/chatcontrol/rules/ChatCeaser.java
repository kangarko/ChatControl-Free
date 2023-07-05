package org.mineacademy.chatcontrol.rules;

import static org.mineacademy.chatcontrol.rules.Rule.Type.CHAT;
import static org.mineacademy.chatcontrol.rules.Rule.Type.COMMAND;
import static org.mineacademy.chatcontrol.rules.Rule.Type.GLOBAL;
import static org.mineacademy.chatcontrol.rules.Rule.Type.PACKET;
import static org.mineacademy.chatcontrol.rules.Rule.Type.SIGN;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.jsonsimple.JSONArray;
import org.mineacademy.chatcontrol.jsonsimple.JSONObject;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.Writer;

/**
 * Custom rule engine. Reads a set of rules from a file.
 *
 * @since 5.0.0
 */
public final class ChatCeaser {

	/**
	 * Stored rules by file. Can only be modified in {@link #load()} method.
	 */
	private final HashMap<Rule.Type, List<Rule>> rulesMap = new HashMap<>();

	/**
	 * Random instance.
	 */
	private final Random random = new Random();

	/**
	 * Clears rules and load them .
	 */
	public void load() {
		rulesMap.clear();

		loadRules(GLOBAL, CHAT, COMMAND, SIGN, PACKET);
	}

	/**
	 * Fill {@link #rules} with rules in specified file paths.
	 *
	 * @param filePaths the paths for every rule file
	 */
	private void loadRules(Rule.Type... filePaths) {
		for (final Rule.Type ruleType : filePaths) {
			final File file = Writer.extract("rules/" + ruleType.getFileName());
			final List<Rule> createdRules = new ArrayList<>();

			try {
				Rule rule = null; // The rule being created.
				String previousRuleName = null;
				final boolean packetRule = ruleType == PACKET;

				final List<String> rawLines = Files.readAllLines(Paths.get(file.toURI()), StandardCharsets.UTF_8);

				for (int i = 0; i < rawLines.size(); i++) {
					final String line = rawLines.get(i).trim();

					if (!line.isEmpty() && !line.startsWith("#"))
						// If a line starts with 'match ' then assume a new rule is found and start
						// creating it. This makes a new instance of 'rule' variable.
						if (line.startsWith("match ")) {
							if (rule != null) { // Found another match, assuming previous rule is finished creating.
								Common.checkBoolean(!createdRules.contains(rule), ruleType.getFileName() + " already contains rule where match is: " + line);
								createdRules.add(rule);
							}

							rule = new Rule(line.replaceFirst("match ", ""));
							previousRuleName = rule.toShortString();

							if (packetRule)
								rule.setPacketRule();
						} else {
							Objects.requireNonNull(rule, "Cannot define an operator when no rule is being created! File: \'" + ruleType.getFileName() + "\' Line: \'" + line + "\' Previous rule: \'" + previousRuleName + "\'");
							// If a rule is being created then attempt to parse operators.

							if (packetRule) {
								if ("then deny".equals(line))
									rule.getPacketRule().setDenyingPacket();
								else if ("dont verbose".equals(line))
									rule.getPacketRule().setNotVerbosing();
								else if (line.startsWith("then replace "))
									rule.getPacketRule().setReplacePacket(line.replaceFirst("then replace ", ""));
								else if (line.startsWith("then rewrite "))
									rule.getPacketRule().setRewritePacket(line.replaceFirst("then rewrite ", ""));
								else if (line.startsWith("then rewritein "))
									rule.getPacketRule().addRewriteIn(line.replaceFirst("then rewritein ", ""));
								else
									throw new NullPointerException("Unknown packet rule operator: " + line);
							} else if ("then deny".equals(line))
								rule.setCancellingEvent();

							else if ("then log".equals(line))
								rule.setLoggingEnabled();

							else if (line.startsWith("before strip "))
								rule.setStripBefore(line.replaceFirst("before strip ", ""));

							else if (line.startsWith("before replace "))
								rule.parseReplaceBefore(line.replaceFirst("before replace ", ""));

							else if (line.startsWith("id "))
								rule.setId(line.replaceFirst("id ", ""));

							else if (line.startsWith("ignore string "))
								rule.setIgnoredMessage(line.replaceFirst("ignore string ", ""));

							else if (line.startsWith("ignore event "))
								rule.parseIgnoreEvent(line.replaceFirst("ignore event ", ""));

							else if (line.startsWith("ignore perm "))
								rule.setBypassPerm(line.replaceFirst("ignore perm ", ""));

							else if (line.startsWith("then rewrite "))
								rule.parseRewrites(line.replaceFirst("then rewrite ", ""));

							else if (line.startsWith("ignore gamemode "))
								rule.parseIgnoredGamemodes(line.replaceFirst("ignore gamemode ", ""));

							// workaround
							else if (line.startsWith("then replace "))
								rule.parseReplacements(line.replaceFirst("then replace ", ""));

							else if (line.startsWith("then replace"))
								rule.parseReplacements(line.replaceFirst("then replace", ""));
							//

							else if (line.startsWith("then console "))
								rule.parseCommandsToExecute(line.replaceFirst("then console ", ""));

							else if (line.startsWith("then warn "))
								rule.setWarnMessage(line.replaceFirst("then warn ", ""));

							else if (line.startsWith("then notify "))
								rule.parseCustomNotify(line.replaceFirst("then notify ", ""));

							else if (line.startsWith("then fine "))
								rule.setFine(Double.parseDouble(line.replaceFirst("then fine ", "")));

							//
							else if (line.startsWith("then kick "))
								rule.setKickMessage(line.replaceFirst("then kick ", ""));

							else if (line.startsWith("then kick"))
								rule.setKickMessage(line.replaceFirst("then kick", ""));
							//

							else if (line.startsWith("handle as "))
								rule.setHandler(HandlerLoader.loadHandler(line.replaceFirst("handle as ", ""), rule.getId()));

							else
								throw new NullPointerException("Unknown operator: '" + line + "'" + " in " + ruleType.getFileName());
						}

					if (i + 1 == rawLines.size() && rule != null) // Reached end of the file but a rule is being created, finishing it
						createdRules.add(rule);
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
			}

			Common.checkBoolean(!rulesMap.containsKey(ruleType), "Rules map already contains rules from: " + ruleType.getFileName() + "!");
			rulesMap.put(ruleType, createdRules);
		}

		if (Settings.DEBUG)
			for (final Rule.Type ruleType : rulesMap.keySet()) {
				Common.debug("&e" + Common.consoleLine());
				Common.debug("&eDisplaying rules from: " + ruleType.getFileName());

				for (final Rule rule : rulesMap.get(ruleType))
					Common.debug("Loaded rule:\n" + rule);
			}

		if (!Settings.SILENT_STARTUP)
			for (final Rule.Type ruleType : rulesMap.keySet())
				Common.log("&fLoaded " + rulesMap.get(ruleType).size() + " Rules in " + ruleType.getFileName());
	}

	/**
	 * Check the message against all rules. Can cancel the event or return modified
	 * message.
	 *
	 * @param event the event - must be cancellable
	 * @param player the player that triggered filtering
	 * @param message the message that is being checked
	 * @return the message that was initially put, might be changed
	 */
	public <T extends Cancellable> String parseRules(T event, Player player, String message) {
		Rule.Type ruleType = Rule.Type.CHAT;

		if (event instanceof PlayerCommandPreprocessEvent)
			ruleType = Rule.Type.COMMAND;
		else if (event instanceof SignChangeEvent)
			ruleType = Rule.Type.SIGN;

		final String origin = message;

		// First iterate over all rules.
		List<Rule> rules = rulesMap.get(GLOBAL);

		Common.debug("Checking " + rules.size() + " global rules");

		message = iterateStandardRules(rules, event, player, message, ruleType, true);

		// Then iterate over rules for the given event
		rules = rulesMap.get(ruleType);

		Common.debug("Checking " + rules.size() + " rules for " + ruleType + " (" + ruleType.getFileName() + ")");

		// Then iterate over specific rules for events.
		message = iterateStandardRules(rules, event, player, message, ruleType, false);

		if (event.isCancelled())
			Common.verbose("&fOriginal message &ccancelled&f.");
		else if (!origin.equals(message))
			Common.verbose("&fFINAL&a: &r" + message);

		return message;
	}

	/**
	 * Internal method, {@link #parseRules(Cancellable, Player, String)}
	 */
	private <T extends Cancellable> String iterateStandardRules(List<Rule> rules, T event, Player player, String message, Rule.Type type, boolean global) {
		for (final Rule rule : rules) {
			if (rule.getIgnoredEvent() != null && rule.getIgnoredEvent() == type)
				continue;

			if (rule.getIgnoredGamemodes() != null && rule.getIgnoredGamemodes().contains(player.getGameMode()))
				continue;

			if (rule.getBypassPerm() != null)
				if (Common.hasPermission(player, rule.getBypassPerm()))
					continue;

			if (rule.matches(message)) {

				Common.verbose("&f*--------- ChatControl rule match on " + player.getName() + " --------- ID " + (rule.getId() != null ? rule.getId() : "UNSET"));
				Common.verbose("&fMATCH&b: &r" + (Settings.DEBUG ? rule : rule.getMatch()));
				Common.verbose("&fCATCH&b: &r" + message);

				if (rule.isLoggingEnabled()) {
					if (!Settings.VERBOSE_RULES)
						Common.log("&4" + (type == Rule.Type.SIGN ? "[" + Localization.Parts.SIGN + " - " + Common.getFormattedLocation(player.getLocation()) + "] " : "") + player.getName() + " violated " + rule.toShortString() + " with message: &f" + message);

					Writer.write(Writer.RULES_PATH, player.getName(), (type == Rule.Type.SIGN ? "[" + Localization.Parts.SIGN + " - " + Common.getFormattedLocation(player.getLocation()) + "] " : "") + rule.toShortString() + " caught message: " + message);
				}

				if (rule.getCustomNotifyMessage() != null) {
					Objects.requireNonNull(rule.getCustomNotifyPermission(), "Custom alert permission cannot be null!");

					for (final Player online : CompatProvider.getOnlinePlayers())
						if (Common.hasPermission(online, rule.getCustomNotifyPermission()))
							Common.tellLater(online, 1, replaceVariables(player, rule, rule.getCustomNotifyMessage(), message));
				}

				if (rule.getHandler() != null)
					message = handle(event, player, message, rule, type);

				if (event.isCancelled())
					return message; // The message will not appear in the chat, no need to continue.

				if (rule.getRewrites() != null && rule.getRewrites().length > 0)
					message = getRandomString(player, rule, rule.getRewrites(), message);

				if (rule.getReplacements() != null && rule.getReplacements().length > 0)
					message = message.replaceAll("(?i)" + rule.getMatch(), getRandomString(player, rule, rule.getReplacements(), message));

				if (rule.getCommandsToExecute() != null)
					for (String command : rule.getCommandsToExecute()) {
						command = replaceVariables(player, rule, command, message);
						Common.dispatchConsoleCommand(command);
					}

				if (rule.getWarnMessage() != null)
					if (rule.isCancellingEvent()) // if not blocked, display after player's message
						Common.tell(player, replaceVariables(player, rule, rule.getWarnMessage(), message));
					else
						Common.tellLater(player, 1, replaceVariables(player, rule, rule.getWarnMessage(), message));

				if (rule.getFine() != null)
					HookManager.takeMoney(player.getName(), rule.getFine());

				if (rule.getKickMessage() != null) {
					final Player finalPlayer = player;
					final Rule finalRule = rule;

					Bukkit.getScheduler().scheduleSyncDelayedTask(ChatControl.getInstance(), () -> {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + finalPlayer.getName() + " " + Common.colorize(finalRule.getKickMessage()));
					});
				}

				if (rule.isCancellingEvent()) {
					event.setCancelled(true);

					return message; // The message will not appear in the chat, no need to continue.
				}
			}
		}

		HandlerCache.reset();
		return message;
	}

	/*
	 * Handlers a custom handler. Returns the original message (can be modified) Can
	 * cancel the event.
	 */
	private <T extends Cancellable> String handle(T event, Player player, String message, Rule rule, Rule.Type type) {
		final Handler handler = rule.getHandler();

		if (handler.getBypassPermission() != null && Common.hasPermission(player, handler.getBypassPermission()))
			return message;

		if (type == Rule.Type.COMMAND && handler.getIgnoredInCommands() != null)
			for (final String ignored : handler.getIgnoredInCommands())
				if (message.startsWith(ignored))
					return message;

		final String warnMessage = handler.getPlayerWarningMessage();

		if (warnMessage != null && !HandlerCache.lastWarnMessage.equals(warnMessage)) {

			if (handler.isMessageBlocked()) // if not blocked, display after player's message
				Common.tell(player, replaceVariables(player, handler, warnMessage, message));
			else
				Common.tellLater(player, 1, replaceVariables(player, handler, warnMessage, message));

			HandlerCache.lastWarnMessage = warnMessage;
		}

		final String broadcastMessage = handler.getBroadcastMessage();

		if (broadcastMessage != null && !HandlerCache.lastBroadcastMessage.equals(broadcastMessage)) {
			Common.broadcast(replaceVariables(player, handler, broadcastMessage, message));
			HandlerCache.lastBroadcastMessage = broadcastMessage;
		}

		if (handler.getStaffAlertMessage() != null) {
			Objects.requireNonNull(handler.getStaffAlertPermission(), "Staff alert permission is null for: " + this);

			for (final Player online : CompatProvider.getOnlinePlayers())
				if (Common.hasPermission(online, handler.getStaffAlertPermission()))
					Common.tell(online, (type == Rule.Type.SIGN ? "[" + Localization.Parts.SIGN + " - " + Common.getFormattedLocation(player.getLocation()) + "] " : "") + replaceVariables(player, handler, handler.getStaffAlertMessage(), message), player.getName());
		}

		if (handler.getConsoleMessage() != null)
			Common.log(replaceVariables(player, handler, handler.getConsoleMessage(), message));

		if (handler.getCommandsToExecute() != null)
			for (final String cmd : handler.getCommandsToExecute())
				Common.dispatchConsoleCommand(replaceVariables(player, handler, cmd, message));

		if (handler.getWriteToFileName() != null)
			Writer.write(handler.getWriteToFileName(), player.getName(), replaceVariables(player, handler, "[Handler={handler}, Rule ID={ruleID}] ", message) + message);

		if (handler.isMessageBlocked() || type == Rule.Type.SIGN && Settings.Signs.BLOCK_WHEN_VIOLATES_RULE)
			event.setCancelled(true);

		else if (handler.getMessageReplacement() != null)
			// return msg = Common.colorize( Common.replaceMatch(standardrule.getMatch(),
			// msg, rule.getReplacePacket()));
			return Common.replaceMatch(rule.getMatch(), message, Common.colorize(replaceVariables(player, handler, handler.getMessageReplacement(), message)));

		else if (handler.getRewriteTo() != null)
			return Common.colorize(replaceVariables(player, handler, handler.getRewriteTo(), message));

		return message;
	}

	/**
	 * Parses the JSON chat message and check it against packet rules
	 * @param player
	 *
	 * @param input
	 *            the JSON chat message object
	 * @throws PacketCancelledException
	 *             if the packet should be cancelled
	 */
	public void parsePacketRules(Player player, Object input) throws PacketCancelledException {
		if (input instanceof JSONObject) {
			final JSONObject objects = (JSONObject) input;

			for (final Object key : objects.keySet()) {
				final Object value = objects.get(key);

				if (value instanceof JSONObject)
					parsePacketRules(player, value);

				else if (value instanceof JSONArray)
					parsePacketRules(player, value);

				else if (value instanceof String) {
					final String result = parsePacketRulesRaw(player, value.toString());
					objects.put(key, result);
				}
			}

		} else if (input instanceof JSONArray) {
			final JSONArray array = (JSONArray) input;

			for (int i = 0; i < array.size(); i++) {
				final Object value = array.get(i);

				if (value instanceof JSONObject)
					parsePacketRules(player, value);

				else if (value instanceof JSONArray)
					parsePacketRules(player, value);

				else if (value instanceof String) {
					final String result = parsePacketRulesRaw(player, value.toString());
					array.set(i, result);
				}
			}
		} else
			Common.debug("Skipping unknown object: " + input.getClass().getTypeName());
	}

	public String parsePacketRulesRaw(Player player, String message) throws PacketCancelledException {
		if (message == null || message.isEmpty())
			return "";

		Common.debug("Checking packet rules against: " + message);

		for (final Rule standardrule : rulesMap.get(PACKET))
			if (standardrule.matches(Common.stripColors(message.toLowerCase()))) {
				final PacketRule rule = standardrule.getPacketRule();
				Objects.requireNonNull(rule, "Malformed rule - must be a packet rule: " + standardrule);

				if (!rule.isNotVerbosing()) {
					Common.verbose("&f*--------- ChatControl rule match: chat packet ---------");
					Common.verbose("&fMATCH&b: &r" + (Settings.DEBUG ? rule : standardrule.getMatch()));
					Common.verbose("&fCATCH&b: &r" + message);
				}

				final String origin = message;
				final String world = player.getWorld().getName();

				if (rule.isDenyingPacket()) {
					if (!rule.isNotVerbosing())
						Common.verbose("&fPacket sending &ccancelled&f.");
					throw new PacketCancelledException();
				}

				else if (rule.getRewritePerWorld() != null && rule.getRewritePerWorld().get(world) != null) {
					message = Common.colorize(replaceVariables(player, standardrule, rule.getRewritePerWorld().get(world), message));

					if (message.equalsIgnoreCase("none") || message.equalsIgnoreCase("hidden")) {
						if (!rule.isNotVerbosing())
							Common.verbose("&fPacket sending &ccancelled&f.");
						throw new PacketCancelledException();
					}
				}

				else if (rule.getRewritePacket() != null)
					message = Common.colorize(replaceVariables(player, standardrule, rule.getRewritePacket(), message));

				else if (rule.getReplacePacket() != null)
					message = Common.colorize(Common.replaceMatch(standardrule.getMatch(), message, rule.getReplacePacket()));

				// msg = msg.replaceAll(standardrule.getMatch(),
				// Common.colorize(rule.getReplacePacket()));

				if (!origin.equals(message) && !rule.isNotVerbosing())
					Common.verbose("&fFINAL&a: &r" + message);
			}

		return message;
	}

	/**
	 * Replaces rule ID (if set) and handler name (if set) in the message and player
	 * and world name.
	 */
	private String replaceVariables(Player player, Handler handler, String message, String messageReplacement) {
		return replaceVariables0(player, handler.getRuleId(), message, messageReplacement).replace("{handler}", handler.getName());
	}

	/**
	 * Replaces rule ID (if set) in the message and player and world name.
	 */
	private String replaceVariables(Player player, Rule rule, String message, String messageReplacement) {
		return replaceVariables0(player, rule.getId(), message, messageReplacement);
	}

	private String replaceVariables0(Player player, String ruleId, String message, String messageReplacement) {
		return message.replace("{ruleID}", ruleId != null ? ruleId : "UNSET").replace("{player}", player.getName()).replace("{world}", player.getWorld().getName()).replace("{message}", messageReplacement);
	}

	/**
	 * Get one colorized string with replaced rule variables from a list.
	 *
	 * @param rule the rule variables will be taken from
	 * @param messages the messages to choose from
	 * @return a colorized string with replaced variables randomly chosen from strings
	 */
	private String getRandomString(Player player, Rule rule, String[] messages, String msgReplacement) {
		Common.checkBoolean(messages.length > 0, "Got empty message '" + String.join(", ", messages) + "'");

		final String randomMsg = messages[random.nextInt(messages.length)];
		return Common.colorize(replaceVariables(player, rule, randomMsg, msgReplacement));
	}

	public static class PacketCancelledException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}

final class HandlerLoader {

	private static YamlConfiguration config;
	private static String sectionName;

	static Handler loadHandler(String name, String ruleId) {
		final File file = Writer.extract("handlers.yml");
		config = YamlConfiguration.loadConfiguration(file);

		if (!config.isConfigurationSection(name))
			throw new NullPointerException("Unknown handler: " + name);

		sectionName = config.getConfigurationSection(name).getName();

		final Handler handler = new Handler(sectionName, ruleId);
		String message;

		message = getString("Bypass_With_Permission");
		if (isValid(message))
			handler.setBypassPermission(message);

		message = getString("Player_Warn_Message");
		if (isValid(message))
			handler.setPlayerWarningMessage(message);

		message = getString("Broadcast_Message");
		if (isValid(message))
			handler.setBroadcastMessage(message);

		message = getString("Staff_Alert_Message");
		if (isValid(message))
			handler.setStaffAlertMessage(message);

		message = getString("Staff_Alert_Permission");
		if (isValid(message))
			handler.setStaffAlertPermission(message);

		message = getString("Console_Message");
		if (isValid(message))
			handler.setConsoleMessage(message);

		message = getString("Write_To_File");
		if (isValid(message))
			handler.setWriteToFileName(message);

		final Boolean block = config.getBoolean(sectionName + ".Block_Message");
		if (block != null && block)
			handler.setMessageBlocked();

		message = getString("Replace_Word");
		if (isValid(message))
			handler.setMessageReplacement(message);

		message = getString("Replace_Whole");
		if (isValid(message))
			handler.setRewriteTo(message);

		List<String> list;
		if (config.isSet(sectionName + ".Execute_Commands")) {
			list = config.getStringList(sectionName + ".Execute_Commands");
			handler.setCommandsToExecute(list);
		}

		if (config.isSet(sectionName + ".Ignored_In_Commands")) {
			list = config.getStringList(sectionName + ".Ignored_In_Commands");
			handler.setIgnoredInCommands(list);
		}

		sectionName = null;

		return handler;
	}

	private static boolean isValid(String message) {
		return message != null && !message.isEmpty() && !message.equalsIgnoreCase("none");
	}

	private static String getString(String path) {
		final String message = config.getString(sectionName + "." + path);

		return message != null && !message.isEmpty() && !message.equalsIgnoreCase("none") ? message : null;
	}
}

/**
 * Caches last messages displayed to the player and broadcasted to everyone,
 * prevents duplicate displaying when multiple rules are violated at once.
 */
final class HandlerCache {
	static String lastWarnMessage = "";
	static String lastBroadcastMessage = "";

	static void reset() {
		lastWarnMessage = "";
		lastBroadcastMessage = "";
	}
}