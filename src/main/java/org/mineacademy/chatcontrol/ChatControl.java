package org.mineacademy.chatcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Filter;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.chatcontrol.filter.ConsoleFilter;
import org.mineacademy.chatcontrol.filter.Log4jFilter;
import org.mineacademy.chatcontrol.hook.HookManager;
import org.mineacademy.chatcontrol.listener.ChatCheckListener;
import org.mineacademy.chatcontrol.listener.ChatFormatListener;
import org.mineacademy.chatcontrol.listener.CommandListener;
import org.mineacademy.chatcontrol.listener.PlayerListener;
import org.mineacademy.chatcontrol.rules.ChatCeaser;
import org.mineacademy.chatcontrol.settings.ConfHelper;
import org.mineacademy.chatcontrol.settings.ConfHelper.IllegalLocaleException;
import org.mineacademy.chatcontrol.settings.ConfHelper.InBuiltFileMissingException;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.Permissions;

import lombok.Getter;
import lombok.Setter;

public final class ChatControl extends JavaPlugin {

	/**
	 * Instance of this class for easy access from other parts of the plugin.
	 */
	private static ChatControl instance;

	/**
	 * Stores player related data
	 */
	private static HashMap<String, PlayerCache> playerCacheMap = new HashMap<>();

	/**
	 * Variable indicating if the chat is globally muted.
	 */
	@Getter
	@Setter
	private static boolean muted = false;

	/**
	 * Instance for formatting the chat.
	 */
	@Getter
	private ChatFormatListener formatter;

	/**
	 * Instance for checking the chat against rules.
	 */
	@Getter
	private ChatCeaser chatCeaser;

	/**
	 * The timed message task ID.
	 */
	private int timedMessageTask;

	@Override
	public void onEnable() {
		try {
			instance = this;

			CompatProvider.setupReflection();
			HookManager.loadDependencies();
			ConfHelper.loadAll();

			for (final Player onlinePlayer : CompatProvider.getOnlinePlayers())
				getCache(onlinePlayer);

			chatCeaser = new ChatCeaser();
			chatCeaser.load();

			formatter = new ChatFormatListener();

			registerEvent(CompatProvider.compatChatEvent(), new ChatCheckListener(), Settings.ListenerPriority.CHECKER);

			getServer().getPluginManager().registerEvents(new PlayerListener(), this);
			getServer().getPluginManager().registerEvents(new CommandListener(), this);

			if (Settings.Console.FILTER_ENABLED)
				try {
					Log4jFilter.init();
					Common.debug("Console filtering now using Log4j Filter.");
				} catch (final NoClassDefFoundError err) {
					final Filter filter = new ConsoleFilter();
					for (final Plugin plugin : getServer().getPluginManager().getPlugins())
						plugin.getLogger().setFilter(filter);

					Bukkit.getLogger().setFilter(filter);
					Common.debug("Console filtering initiated (MC 1.6.4 and lower).");
				}

			if (Settings.Packets.ENABLED)
				if (HookManager.isProtocolLibLoaded())
					HookManager.initPacketListening();
				else
					Common.logInFrame(false, "Cannot enable packet features!", "Required plugin missing: ProtocolLib");

			if (Settings.Chat.Formatter.ENABLED)
				if (HookManager.isVaultLoaded()) {
					if (Common.doesPluginExist("ChatManager"))
						Common.logInFrame(true, "Detected &fChatManager&c! Please copy", "settings from it to ChatControl", "and disable the plugin afterwards!");
					else {
						Common.log("&3Starting &fformatter listener &3with " + Settings.ListenerPriority.FORMATTER + " priority");

						registerEvent(CompatProvider.compatChatEvent(), formatter, Settings.ListenerPriority.FORMATTER);
					}
				} else
					Common.logInFrame(false, "You need Vault to enable ChatFormatter.");

			scheduleTimedMessages();

			getCommand("chatcontrol").setExecutor(new CommandsHandler());

			Common.addLoggingPrefix();

		} catch (final Throwable t) {
			t.printStackTrace();

			Common.log("&4!----------------------------------------------!");
			Common.log(" &cError loading ChatControl, plugin is disabled!");
			Common.log(" &cRunning on " + getServer().getBukkitVersion() + " and Java " + System.getProperty("java.version"));
			Common.log("&4!----------------------------------------------!");

			if (t instanceof InvalidConfigurationException) {
				Common.log(" &cIt seems like your config is not a valid YAML.");
				Common.log(" &cUse online services like");
				Common.log(" &chttp://yaml-online-parser.appspot.com/");
				Common.log(" &cto check for syntax errors!");

			} else if (t instanceof IllegalLocaleException)
				Common.log(" &cChatControl doesn't have the locale: " + Settings.LOCALIZATION_SUFFIX);

			else if (t instanceof UnsupportedOperationException || t.getCause() != null && t.getCause() instanceof UnsupportedOperationException) {
				if (getServer().getBukkitVersion().startsWith("1.2.5"))
					Common.log(" &cSorry but Minecraft 1.2.5 is no longer supported!");
				else {
					Common.log(" &cUnable to determine server version!");
					Common.log(" &cYour server is either too old or");
					Common.log(" &cthe plugin broke on the new version :(");
				}
			} else if (t instanceof InBuiltFileMissingException) {
				Common.log(" &c" + t.getMessage());
				Common.log(" &cTo fix it, create a blank file with");
				Common.log(" &cthe name &f" + ((InBuiltFileMissingException) t).file + " &cin plugin folder.");
				Common.log(" &cIt will be filled with default values.");
				Common.log(" &ePlease inform the developer about this error.");

			} else {
				String error = "Unable to get the error message, search above.";
				if (t.getMessage() != null && !t.getMessage().isEmpty() && !t.getMessage().equalsIgnoreCase("null"))
					error = t.getMessage();
				Common.log(" &cError: " + error);
			}
			Common.log("&4!----------------------------------------------!");

			getPluginLoader().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		muted = false;
		playerCacheMap.clear();

		getServer().getScheduler().cancelTasks(this);

		instance = null;
	}

	public void onReload() {
		if (getServer().getScheduler().isCurrentlyRunning(timedMessageTask))
			getServer().getScheduler().cancelTask(timedMessageTask);

		playerCacheMap.clear();

		scheduleTimedMessages();
		chatCeaser.load();
	}

	private void scheduleTimedMessages() {
		if (!Settings.Messages.TIMED_ENABLED)
			return;

		final HashMap<String, Integer> broadcasterIndexes = new HashMap<>();
		final HashMap<String, List<String>> broadcasterCache = new HashMap<>();
		final Random random = new Random();

		final HashMap<String, List<String>> timed = Settings.Messages.TIMED;

		if (!Settings.Messages.TIMED_RANDOM_ORDER)
			for (final String world : timed.keySet())
				broadcasterIndexes.put(world, 0);

		if (Settings.Messages.TIMED_RANDOM_NO_REPEAT)
			for (final String world : timed.keySet())
				broadcasterCache.put(world, new ArrayList<>(timed.get(world)));

		if (Settings.DEBUG)
			for (final String world : timed.keySet()) {
				Common.debug("&fMessages for: " + world);

				for (final String msg : timed.get(world))
					Common.debug(" - " + msg);
			}

		timedMessageTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (final String world : timed.keySet()) {
				final List<String> msgs = timed.get(world); // messages in world

				if (msgs.size() == 0) // no messages there, pass through
					continue;

				String message;

				if (Settings.Messages.TIMED_RANDOM_ORDER) {
					if (Settings.Messages.TIMED_RANDOM_NO_REPEAT) {
						final List<String> worldCache = broadcasterCache.get(world);

						if (worldCache.size() == 0)
							worldCache.addAll(msgs);

						// Pull the message randomly from the cache
						message = worldCache.remove(random.nextInt(worldCache.size()));
					} else
						message = msgs.get(random.nextInt(msgs.size()));
				} else {
					int last = broadcasterIndexes.get(world);

					if (msgs.size() < last + 1)
						last = 0;

					message = msgs.get(last);

					broadcasterIndexes.put(world, last + 1);
				}

				if (message == null)
					continue;
				else {
					final String prefix = Settings.Messages.TIMED_PREFIX;
					final String suffix = Settings.Messages.TIMED_SUFFIX;

					message = (!prefix.isEmpty() ? prefix + " " : "") + message + (!suffix.isEmpty() ? " " + suffix : "");
				}

				Common.debug(message);

				if (world.equalsIgnoreCase("global")) {
					for (final Player online : CompatProvider.getOnlinePlayers())
						if (!timed.containsKey(online.getWorld().getName()) && Common.hasPermission(online, Permissions.VIEW_TIMED_MESSAGES))
							Common.tell(online, message.replace("{world}", online.getWorld().getName()));

				} else {
					final World bukkitworld = getServer().getWorld(world);

					if (bukkitworld == null)
						Common.warn("World \"" + world + "\" doesn't exist. No timed messages broadcast.");
					else
						for (final Player online : bukkitworld.getPlayers())
							if (Common.hasPermission(online, Permissions.VIEW_TIMED_MESSAGES))
								Common.tell(online, message.replace("{world}", world));
				}
			}

		}, 20, 20 * Settings.Messages.TIMED_DELAY_SECONDS);
	}

	private final void registerEvent(Class<? extends org.bukkit.event.Event> eventClass, Object listener, EventPriority priority) {
		getServer().getPluginManager().registerEvent(eventClass, (Listener) listener, priority, (EventExecutor) listener, this, true);
	}

	public static void removeCache(Player player) {
		playerCacheMap.remove(player.getName());
	}

	public static PlayerCache getCache(Player player) {
		final String name = player.getName();
		PlayerCache cache = playerCacheMap.get(name);

		if (cache == null) {
			cache = new PlayerCache();
			playerCacheMap.put(name, cache);
		}

		cache.assignGroups(player);

		return cache;
	}

	public static ChatControl getInstance() {
		return instance;
	}
}
