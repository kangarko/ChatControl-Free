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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.settings.Localization;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.LagCatcher;
import org.mineacademy.chatcontrol.util.Writer;

/**
 * Custom rule engine. Reads a set of rules from a file
 *
 * @author kangarko
 * @since 5.0.0
 */
public final class ChatCeaser {

	/**
	 * Stored rules by file. Can only be modified in {@link #load()} method.
	 */
	private final HashMap<Rule.Type, List<Rule>> rulesMap = new HashMap<>();

	private final Random rand = new Random();

	/**
	 * Clears {@link #rules} and load them .
	 */
	public void load() {
		rulesMap.clear();

		loadRules(GLOBAL, CHAT, COMMAND, SIGN, PACKET);
	}

	/**
	 * Fill {@link #rules} with rules in specified file paths.
	 *
	 * @param filePaths
	 *            the paths for every rule file
	 */
	private void loadRules(Rule.Type... filePaths) {
		for (final Rule.Type ruleType : filePaths) {
			final File file = Writer.Extract("rules/" + ruleType.getFileName());
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
								Validate.isTrue(!createdRules.contains(rule), ruleType.getFileName() + " already contains rule where match is: " + line);
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
									rule.getPacketRule().setDeny();
								else if ("dont verbose".equals(line))
									rule.getPacketRule().setDoNotVerbose();
								else if (line.startsWith("then replace "))
									rule.getPacketRule().setReplacePacket(line.replaceFirst("then replace ", ""));
								else if (line.startsWith("then rewrite "))
									rule.getPacketRule().setRewritePacket(line.replaceFirst("then rewrite ", ""));
								else if (line.startsWith("then rewritein "))
									rule.getPacketRule().addRewriteIn(line.replaceFirst("then rewritein ", ""));
								else
									throw new NullPointerException("Unknown packet rule operator: " + line);
							} else if ("then deny".equals(line))
								rule.setCancelEvent();

							else if ("then log".equals(line))
								rule.setLog();

							// TODO remove
							else if (line.startsWith("strip ")) {
								Common.Warn("Operator 'strip' was deprecated and replaced by 'before strip'. Please edit rule '" + rule + "' in " + ruleType.getFileName());
								rule.setStripBefore(line.replaceFirst("strip ", ""));
							}

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

			Validate.isTrue(!rulesMap.containsKey(ruleType), "Rules map already contains rules from: " + ruleType.getFileName() + "!");
			rulesMap.put(ruleType, createdRules);
		}

		if (Settings.DEBUG)
			for (final Rule.Type ruleType : rulesMap.keySet()) {
				Common.Debug("&e" + Common.consoleLine());
				Common.Debug("&eDisplaying rules from: " + ruleType.getFileName());

				for (final Rule rule : rulesMap.get(ruleType))
					Common.Debug("Loaded rule:\n" + rule);
			}

		if (!Settings.SILENT_STARTUP)
			for (final Rule.Type ruleType : rulesMap.keySet())
				Common.Log("&fLoaded " + rulesMap.get(ruleType).size() + " Rules in " + ruleType.getFileName());
	}

	/**
	 * Check the message against all rules. Can cancel the event or return modified
	 * message.
	 *
	 * @param event
	 *            the event - must be cancellable
	 * @param pl
	 *            the player that triggered filtering
	 * @param msg
	 *            the message that is being checked
	 * @return the message that was initially put, might be changed
	 */
	public <T extends Cancellable> String parseRules(T event, Player pl, String msg) {
		Rule.Type ruleType = Rule.Type.CHAT;

		if (event instanceof PlayerCommandPreprocessEvent)
			ruleType = Rule.Type.COMMAND;
		else if (event instanceof SignChangeEvent)
			ruleType = Rule.Type.SIGN;

		LagCatcher.start("Rule parse");

		final String origin = msg;

		// First iterate over all rules.
		List<Rule> rules = rulesMap.get(GLOBAL);

		Common.Debug("Checking " + rules.size() + " global rules");

		LagCatcher.start("Rule parse: global");
		msg = iterateStandardRules(rules, event, pl, msg, ruleType, true);
		LagCatcher.end("Rule parse: global");

		// Then iterate over rules for the given event
		rules = rulesMap.get(ruleType);

		Common.Debug("Checking " + rules.size() + " rules for " + ruleType + " (" + ruleType.getFileName() + ")");

		// Then iterate over specific rules for events.
		LagCatcher.start("Rule parse from: " + event.getClass().getSimpleName());
		msg = iterateStandardRules(rules, event, pl, msg, ruleType, false);
		LagCatcher.end("Rule parse from: " + event.getClass().getSimpleName());

		if (event.isCancelled())
			Common.Verbose("&fOriginal message &ccancelled&f.");
		else if (!origin.equals(msg))
			Common.Verbose("&fFINAL&a: &r" + msg);

		LagCatcher.end("Rule parse");

		return msg;
	}

	/**
	 * Internal method, {@link #parseRules(Cancellable, Player, String)}
	 */
	private <T extends Cancellable> String iterateStandardRules(List<Rule> rules, T e, Player pl, String msg, Rule.Type type, boolean global) {
		for (final Rule rule : rules) {
			if (/*!global && */rule.getIgnoredEvent() != null && rule.getIgnoredEvent() == type)
				continue;

			if (rule.getIgnoredGamemodes() != null && rule.getIgnoredGamemodes().contains(pl.getGameMode()))
				continue;

			if (rule.getBypassPerm() != null)
				if (Common.hasPerm(pl, rule.getBypassPerm()))
					continue;

			if (rule.matches(msg)) {

				Common.Verbose("&f*--------- ChatControl rule match on " + pl.getName() + " --------- ID " + (rule.getId() != null ? rule.getId() : "UNSET"));
				Common.Verbose("&fMATCH&b: &r" + (Settings.DEBUG ? rule : rule.getMatch()));
				Common.Verbose("&fCATCH&b: &r" + msg);

				if (rule.log()) {
					if (!Settings.VERBOSE_RULES)
						Common.Log("&4" + (type == Rule.Type.SIGN ? "[" + Localization.Parts.SIGN + " - " + Common.shortLocation(pl.getLocation()) + "] " : "") + pl.getName() + " violated " + rule.toShortString() + " with message: &f" + msg);

					Writer.Write(Writer.RULES_PATH, pl.getName(), (type == Rule.Type.SIGN ? "[" + Localization.Parts.SIGN + " - " + Common.shortLocation(pl.getLocation()) + "] " : "") + rule.toShortString() + " caught message: " + msg);
				}

				if (rule.getCustomNotifyMessage() != null) {
					Objects.requireNonNull(rule.getCustomNotifyPermission(), "Custom alert permission cannot be null!");

					for (final Player online : CompatProvider.getAllPlayers())
						if (Common.hasPerm(online, rule.getCustomNotifyPermission()))
							Common.tellLater(online, 1, replaceVariables(pl, rule, rule.getCustomNotifyMessage(), msg));
				}

				if (rule.getHandler() != null)
					msg = handle(e, pl, msg, rule, type);

				if (e.isCancelled())
					return msg; // The message will not appear in the chat, no need to continue.

				if (rule.getRewrites() != null && rule.getRewrites().length > 0)
					msg = getRandomString(pl, rule, rule.getRewrites(), msg);

				if (rule.getReplacements() != null && rule.getReplacements().length > 0)
					msg = msg.replaceAll("(?i)" + rule.getMatch(), getRandomString(pl, rule, rule.getReplacements(), msg));

				if (rule.getCommandsToExecute() != null)
					for (String command : rule.getCommandsToExecute()) {
						command = replaceVariables(pl, rule, command, msg);
						Common.customAction(pl, command, msg);
					}

				if (rule.getWarnMessage() != null)
					if (rule.cancelEvent()) // if not blocked, display after player's message
						Common.tell(pl, replaceVariables(pl, rule, rule.getWarnMessage(), msg));
					else
						Common.tellLater(pl, 1, replaceVariables(pl, rule, rule.getWarnMessage(), msg));

				if (rule.getFine() != null)
					HookManager.takeMoney(pl.getName(), rule.getFine());

				if (rule.getKickMessage() != null) {
					final Player Pl = pl;
					final Rule Rule = rule;

					Bukkit.getScheduler().scheduleSyncDelayedTask(ChatControl.instance(), () -> {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + Pl.getName() + " " + Common.colorize(Rule.getKickMessage()));
					});
				}

				if (rule.cancelEvent()) {
					System.out.println("Cancelled the message");

					e.setCancelled(true);
					return msg; // The message will not appear in the chat, no need to continue.
				}
			}
		}

		HandlerCache.reset();
		return msg;
	}

	/**
	 * Handlers a custom handler. Returns the original message (can be modified) Can
	 * cancel the event.
	 */
	private <T extends Cancellable> String handle(T e, Player pl, String msg, Rule rule, Rule.Type type) {
		final Handler handler = rule.getHandler();

		if (handler.getBypassPermission() != null && Common.hasPerm(pl, handler.getBypassPermission()))
			return msg;

		if (type == Rule.Type.COMMAND && handler.getIgnoredInCommands() != null)
			for (final String ignored : handler.getIgnoredInCommands())
				if (msg.startsWith(ignored))
					return msg;

		final String warnMessage = handler.getPlayerWarnMsg();

		if (warnMessage != null && !HandlerCache.lastWarnMsg.equals(warnMessage)) {

			if (handler.blockMessage()) // if not blocked, display after player's message
				Common.tell(pl, replaceVariables(pl, handler, warnMessage, msg));
			else
				Common.tellLater(pl, 1, replaceVariables(pl, handler, warnMessage, msg));

			HandlerCache.lastWarnMsg = warnMessage;
		}

		final String broadcastMessage = handler.getBroadcastMsg();

		if (broadcastMessage != null && !HandlerCache.lastBroadcastMsg.equals(broadcastMessage)) {
			Common.broadcastWithPlayer(replaceVariables(pl, handler, broadcastMessage, msg), pl.getName());
			HandlerCache.lastBroadcastMsg = broadcastMessage;
		}

		if (handler.getStaffAlertMsg() != null) {
			Objects.requireNonNull(handler.getStaffAlertPermission(), "Staff alert permission is null for: " + this);

			for (final Player online : CompatProvider.getAllPlayers())
				if (Common.hasPerm(online, handler.getStaffAlertPermission()))
					Common.tell(online, (type == Rule.Type.SIGN ? "[" + Localization.Parts.SIGN + " - " + Common.shortLocation(pl.getLocation()) + "] " : "") + replaceVariables(pl, handler, handler.getStaffAlertMsg(), msg), pl.getName());
		}

		if (handler.getConsoleMsg() != null)
			Common.Log(replaceVariables(pl, handler, handler.getConsoleMsg(), msg));

		if (handler.getCommandsToExecute() != null)
			for (final String cmd : handler.getCommandsToExecute())
				Common.customAction(pl, replaceVariables(pl, handler, cmd, msg), msg);

		if (handler.getWriteToFileName() != null)
			Writer.Write(handler.getWriteToFileName(), pl.getName(), replaceVariables(pl, handler, "[Handler={handler}, Rule ID={ruleID}] ", msg) + msg);

		if (handler.blockMessage() || type == Rule.Type.SIGN && Settings.Signs.BLOCK_WHEN_VIOLATES_RULE)
			e.setCancelled(true);

		else if (handler.getMsgReplacement() != null)
			// return msg = Common.colorize( Common.replaceMatch(standardrule.getMatch(),
			// msg, rule.getReplacePacket()));
			return Common.replaceMatch(rule.getMatch(), msg, Common.colorize(replaceVariables(pl, handler, handler.getMsgReplacement(), msg)));

		else if (handler.getRewriteTo() != null)
			return Common.colorize(replaceVariables(pl, handler, handler.getRewriteTo(), msg));

		return msg;
	}

	/**
	 * Parses the JSON chat message and check it against packet rules
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
			Common.Debug("Skipping unknown object: " + input.getClass().getTypeName());
	}

	public String parsePacketRulesRaw(Player player, String msg) throws PacketCancelledException {
		if (msg == null || msg.isEmpty())
			return "";

		Common.Debug("Checking Packet rules against: " + msg);

		for (final Rule standardrule : rulesMap.get(PACKET))
			if (standardrule.matches(Common.stripColors(msg.toLowerCase()))) {
				final PacketRule rule = standardrule.getPacketRule();
				Objects.requireNonNull(rule, "Malformed rule - must be a packet rule: " + standardrule);

				if (!rule.doNotVerboe()) {
					Common.Verbose("&f*--------- ChatControl rule match: chat packet ---------");
					Common.Verbose("&fMATCH&b: &r" + (Settings.DEBUG ? rule : standardrule.getMatch()));
					Common.Verbose("&fCATCH&b: &r" + msg);
				}

				final String origin = msg;
				final String world = player.getWorld().getName();

				if (rule.deny()) {
					if (!rule.doNotVerboe())
						Common.Verbose("&fPacket sending &ccancelled&f.");
					throw new PacketCancelledException();
				}

				else if (rule.getRewritePerWorld() != null && rule.getRewritePerWorld().get(world) != null) {
					msg = Common.colorize(replaceVariables(player, standardrule, rule.getRewritePerWorld().get(world), msg));

					if (msg.equalsIgnoreCase("none") || msg.equalsIgnoreCase("hidden")) {
						if (!rule.doNotVerboe())
							Common.Verbose("&fPacket sending &ccancelled&f.");
						throw new PacketCancelledException();
					}
				}

				else if (rule.getRewritePacket() != null)
					msg = Common.colorize(replaceVariables(player, standardrule, rule.getRewritePacket(), msg));

				else if (rule.getReplacePacket() != null)
					msg = Common.colorize(Common.replaceMatch(standardrule.getMatch(), msg, rule.getReplacePacket()));

				// msg = msg.replaceAll(standardrule.getMatch(),
				// Common.colorize(rule.getReplacePacket()));

				if (!origin.equals(msg) && !rule.doNotVerboe())
					Common.Verbose("&fFINAL&a: &r" + msg);
			}

		return msg;
	}

	/**
	 * Replaces rule ID (if set) and handler name (if set) in the message and player
	 * and world name.
	 */
	private String replaceVariables(Player player, Handler handler, String message, String msgReplacement) {
		return replaceVariables0(player, handler.getRuleID(), message, msgReplacement).replace("{handler}", handler.getName());
	}

	/**
	 * Replaces rule ID (if set) in the message and player and world name.
	 */
	private String replaceVariables(Player player, Rule rule, String message, String msgReplacement) {
		return replaceVariables0(player, rule.getId(), message, msgReplacement);
	}

	private String replaceVariables0(Player pl, String ruleId, String message, String msgReplacement) {
		return message.replace("{ruleID}", ruleId != null ? ruleId : "UNSET").replace("{player}", pl.getName()).replace("{world}", pl.getWorld().getName()).replace("{message}", msgReplacement);
	}

	/**
	 * Get one colorized string with replaced rule variables from a list.
	 *
	 * @param rule
	 *            the rule variables will be taken from
	 * @param messages
	 *            the messages to choose from
	 * @return a colorized string with replaced variables randomly chosen from
	 *         strings
	 */
	private String getRandomString(Player player, Rule rule, String[] messages, String msgReplacement) {
		Validate.isTrue(messages.length > 0, "Got empty message '" + StringUtils.join(messages) + "'");

		final String randomMsg = messages[rand.nextInt(messages.length)];
		return Common.colorize(replaceVariables(player, rule, randomMsg, msgReplacement));
	}

	public static class PacketCancelledException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}

class HandlerLoader {

	private static YamlConfiguration cfg;
	private static String sectionName;

	static Handler loadHandler(String name, String ruleID) {
		final File file = Writer.Extract("handlers.yml");
		cfg = YamlConfiguration.loadConfiguration(file);

		if (!cfg.isConfigurationSection(name))
			throw new NullPointerException("Unknown handler: " + name);

		sectionName = cfg.getConfigurationSection(name).getName();

		final Handler handler = new Handler(sectionName, ruleID);
		String message;

		message = getString("Bypass_With_Permission");
		if (isValid(message))
			handler.setBypassPermission(message);

		message = getString("Player_Warn_Message");
		if (isValid(message))
			handler.setPlayerWarnMsg(message);

		message = getString("Broadcast_Message");
		if (isValid(message))
			handler.setBroadcastMsg(message);

		message = getString("Staff_Alert_Message");
		if (isValid(message))
			handler.setStaffAlertMsg(message);

		message = getString("Staff_Alert_Permission");
		if (isValid(message))
			handler.setStaffAlertPermission(message);

		message = getString("Console_Message");
		if (isValid(message))
			handler.setConsoleMsg(message);

		message = getString("Write_To_File");
		if (isValid(message))
			handler.setWriteToFileName(message);

		final Boolean block = cfg.getBoolean(sectionName + ".Block_Message");
		if (block != null && block)
			handler.setBlockMessage();

		message = getString("Replace_Word");
		if (isValid(message))
			handler.setMsgReplacement(message);

		message = getString("Replace_Whole");
		if (isValid(message))
			handler.setRewriteTo(message);

		List<String> list;
		if (cfg.isSet(sectionName + ".Execute_Commands")) {
			list = cfg.getStringList(sectionName + ".Execute_Commands");
			handler.setCommandsToExecute(list);
		}

		if (cfg.isSet(sectionName + ".Ignored_In_Commands")) {
			list = cfg.getStringList(sectionName + ".Ignored_In_Commands");
			handler.setIgnoredInCommands(list);
		}

		sectionName = null;

		return handler;
	}

	private static boolean isValid(String msg) {
		return msg != null && !msg.isEmpty() && !msg.equalsIgnoreCase("none");
	}

	private static String getString(String path) {
		final String msg = cfg.getString(sectionName + "." + path);

		return msg != null && !msg.isEmpty() && !msg.equalsIgnoreCase("none") ? msg : null;
	}
}

/**
 * Caches last messages displayed to the player and broadcasted to everyone,
 * prevents duplicate displaying when multiple rules are violated at once.
 */
class HandlerCache {
	static String lastWarnMsg = "";
	static String lastBroadcastMsg = "";

	static void reset() {
		lastWarnMsg = "";
		lastBroadcastMsg = "";
	}
}