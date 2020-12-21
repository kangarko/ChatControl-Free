package org.mineacademy.chatcontrol;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Filter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
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
import org.mineacademy.chatcontrol.listener.ChatListener;
import org.mineacademy.chatcontrol.listener.CommandListener;
import org.mineacademy.chatcontrol.listener.PlayerListener;
import org.mineacademy.chatcontrol.rules.ChatCeaser;
import org.mineacademy.chatcontrol.settings.ConfHelper;
import org.mineacademy.chatcontrol.settings.ConfHelper.IllegalLocaleException;
import org.mineacademy.chatcontrol.settings.ConfHelper.InBuiltFileMissingException;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.GeoAPI;
import org.mineacademy.chatcontrol.util.GeoAPI.GeoResponse;
import org.mineacademy.chatcontrol.util.LagCatcher;
import org.mineacademy.chatcontrol.util.Permissions;
import org.mineacademy.chatcontrol.util.UpdateCheck;

public class ChatControl extends JavaPlugin {

	/**
	 * Variable indicating if the chat is globally muted.
	 */
	public static boolean muted = false;

	/**
	 * Instance for formatting the chat.
	 */
	public ChatFormatter formatter;

	/**
	 * Instance for checking the chat against rules.
	 */
	public ChatCeaser chatCeaser;

	/**
	 * Instance of this class for easy access from other parts of the plugin.
	 */
	private static ChatControl instance;

	// Player Name, Player Cache
	private static HashMap<String, PlayerCache> playerData = new HashMap<>();

	// Player IP, GeoResponse
	private static HashMap<String, GeoResponse> geoData = new HashMap<>();

	private int timedMessageTask;

	@Override
	public void onEnable() {
		try {
			instance = this;

			if (scanLegacyPackages()) {
				setEnabled(false);

				return;
			}

			CompatProvider.setupReflection();
			HookManager.loadDependencies();
			ConfHelper.loadAll();

			for (final Player pl : CompatProvider.getAllPlayers())
				getDataFor(pl);

			chatCeaser = new ChatCeaser();
			chatCeaser.load();

			formatter = new ChatFormatter();

			registerEvent(CompatProvider.compatChatEvent(), new ChatListener(), Settings.ListenerPriority.CHECKER);

			getServer().getPluginManager().registerEvents(new PlayerListener(), this);
			getServer().getPluginManager().registerEvents(new CommandListener(), this);

			if (Settings.Console.FILTER_ENABLED)
				try {
					Log4jFilter.init();
					Common.Debug("Console filtering now using Log4j Filter.");
				} catch (final NoClassDefFoundError err) {
					final Filter filter = new ConsoleFilter();
					for (final Plugin plugin : getServer().getPluginManager().getPlugins())
						plugin.getLogger().setFilter(filter);

					Bukkit.getLogger().setFilter(filter);
					Common.Debug("Console filtering initiated (MC 1.6.4 and lower).");
				}

			if (Settings.Packets.ENABLED)
				if (HookManager.isProtocolLibLoaded())
					HookManager.initPacketListening();
				else
					Common.LogInFrame(false, "Cannot enable packet features!", "Required plugin missing: ProtocolLib");

			if (Settings.Chat.Formatter.ENABLED)
				if (HookManager.isVaultLoaded()) {
					if (Common.doesPluginExist("ChatManager"))
						Common.LogInFrame(true, "Detected &fChatManager&c! Please copy", "settings from it to ChatControl", "and disable the plugin afterwards!");
					else {
						Common.Log("&3Starting &fformatter listener &3with " + Settings.ListenerPriority.FORMATTER + " priority");

						registerEvent(CompatProvider.compatChatEvent(), formatter, Settings.ListenerPriority.FORMATTER);
					}
				} else
					Common.LogInFrame(false, "You need Vault to enable ChatFormatter.");

			scheduleTimedMessages();

			getCommand("chatcontrol").setExecutor(new CommandsHandler());
			getServer().getScheduler().scheduleAsyncDelayedTask(this, new UpdateCheck());

			Common.addLoggingPrefix();

		} catch (final Throwable t) {
			t.printStackTrace();

			Common.Log("&4!----------------------------------------------!");
			Common.Log(" &cError loading ChatControl, plugin is disabled!");
			Common.Log(" &cRunning on " + getServer().getBukkitVersion() + " (" + Common.getServerVersion() + ") and Java " + System.getProperty("java.version"));
			Common.Log("&4!----------------------------------------------!");

			if (t instanceof InvalidConfigurationException) {
				Common.Log(" &cIt seems like your config is not a valid YAML.");
				Common.Log(" &cUse online services like");
				Common.Log(" &chttp://yaml-online-parser.appspot.com/");
				Common.Log(" &cto check for syntax errors!");

			} else if (t instanceof IllegalLocaleException)
				Common.Log(" &cChatControl doesn't have the locale: " + Settings.LOCALIZATION_SUFFIX);

			else if (t instanceof UnsupportedOperationException || t.getCause() != null && t.getCause() instanceof UnsupportedOperationException) {
				if (getServer().getBukkitVersion().startsWith("1.2.5"))
					Common.Log(" &cSorry but Minecraft 1.2.5 is no longer supported!");
				else {
					Common.Log(" &cUnable to determine server version!");
					Common.Log(" &cYour server is either too old or");
					Common.Log(" &cthe plugin broke on the new version :(");
				}
			} else if (t instanceof InBuiltFileMissingException) {
				Common.Log(" &c" + t.getMessage());
				Common.Log(" &cTo fix it, create a blank file with");
				Common.Log(" &cthe name &f" + ((InBuiltFileMissingException) t).file + " &cin plugin folder.");
				Common.Log(" &cIt will be filled with default values.");
				Common.Log(" &ePlease inform the developer about this error.");

			} else {
				String error = "Unable to get the error message, search above.";
				if (t.getMessage() != null && !t.getMessage().isEmpty() && !t.getMessage().equalsIgnoreCase("null"))
					error = t.getMessage();
				Common.Log(" &cError: " + error);
			}
			Common.Log("&4!----------------------------------------------!");

			getPluginLoader().disablePlugin(this);
		}
	}

	private final boolean scanLegacyPackages() {
		if (!getDataFolder().exists())
			return false;

		boolean found = false;

		try (Stream<Path> stream = Files.walk(getDataFolder().toPath())) {
			final List<Path> paths = stream.filter(Files::isRegularFile).collect(Collectors.toList());

			for (final Path path : paths) {
				if (path.toString().endsWith(".log"))
					continue;

				if (path.toString().contains("DS_Store"))
					continue;

				List<String> lines;

				try {
					lines = Files.readAllLines(path, StandardCharsets.UTF_8);
				} catch (final Throwable t) {
					System.out.println("**ERROR** Could not check " + path.toFile() + " for errors. It appears you saved it in the wrong encoding, see https://github.com/kangarko/chatcontrol-pro/wiki/Use-Right-Encoding");

					continue;
				}

				for (final String line : lines)
					if (line.contains("kangarko.chatcontrol")) {
						if (!found)
							System.out.println("************ WARNING ************");

						System.out.println("* Detected outdated reference to 'kangarko.chatcontrol' in " + path);
						System.out.println("* Please change the line below to contain 'org.mineacademy':");
						System.out.println("* " + line);
						System.out.println("*********************************");

						found = true;
					}
			}

		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		if (found) {
			System.out.println("** Legacy packages found. Until you update them this plugin will not function. **");
			System.out.println("*********************************");
		}

		return found;
	}

	private final void registerEvent(Class<? extends org.bukkit.event.Event> eventClass, Object listener, EventPriority priority) {
		Validate.isTrue(listener instanceof Listener && listener instanceof EventExecutor, "Class " + listener.getClass().getSimpleName() + " must implement Listener and EventExecutor");

		getServer().getPluginManager().registerEvent(eventClass, (Listener) listener, priority, (EventExecutor) listener, this, true);
	}

	@Override
	public void onDisable() {
		muted = false;
		playerData.clear();

		UpdateCheck.needsUpdate = false;
		getServer().getScheduler().cancelTasks(this);

		instance = null;
	}

	public void onReload() {
		if (getServer().getScheduler().isCurrentlyRunning(timedMessageTask))
			getServer().getScheduler().cancelTask(timedMessageTask);

		playerData.clear();

		scheduleTimedMessages();
		chatCeaser.load();
	}

	private void scheduleTimedMessages() {
		if (!Settings.Messages.TIMED_ENABLED)
			return;

		final HashMap<String, Integer> broadcasterIndexes = new HashMap<>();
		final HashMap<String, List<String>> broadcasterCache = new HashMap<>();
		final Random rand = new Random();

		final HashMap<String, List<String>> timed = Settings.Messages.TIMED;

		if (!Settings.Messages.TIMED_RANDOM_ORDER)
			for (final String world : timed.keySet())
				broadcasterIndexes.put(world, 0);

		if (Settings.Messages.TIMED_RANDOM_NO_REPEAT)
			for (final String world : timed.keySet())
				broadcasterCache.put(world, new ArrayList<>(timed.get(world)));

		if (Settings.DEBUG)
			for (final String world : timed.keySet()) {
				Common.Debug("&fMessages for: " + world);

				for (final String msg : timed.get(world))
					Common.Debug(" - " + msg);
			}

		timedMessageTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
			LagCatcher.start("timed messages");

			for (final String world : timed.keySet()) {
				final List<String> msgs = timed.get(world); // messages in world

				if (msgs.size() == 0) // no messages there, pass through
					continue;

				String msg;

				if (Settings.Messages.TIMED_RANDOM_ORDER) {
					if (Settings.Messages.TIMED_RANDOM_NO_REPEAT) {
						final List<String> worldCache = broadcasterCache.get(world);

						if (worldCache.size() == 0)
							worldCache.addAll(msgs);

						// Pull the message randomly from the cache
						msg = worldCache.remove(rand.nextInt(worldCache.size()));
					} else
						msg = msgs.get(rand.nextInt(msgs.size()));
				} else {
					int last = broadcasterIndexes.get(world);

					if (msgs.size() < last + 1)
						last = 0;

					msg = msgs.get(last);

					broadcasterIndexes.put(world, last + 1);
				}

				if (msg == null)
					continue;
				else {
					final String prefix = Settings.Messages.TIMED_PREFIX;
					final String suffix = Settings.Messages.TIMED_SUFFIX;

					msg = (!prefix.isEmpty() ? prefix + " " : "") + msg + (!suffix.isEmpty() ? " " + suffix : "");
				}

				Common.Debug(msg);

				if (world.equalsIgnoreCase("global")) {
					for (final Player online : CompatProvider.getAllPlayers())
						if (!timed.containsKey(online.getWorld().getName()) && Common.hasPerm(online, Permissions.VIEW_TIMED_MESSAGES))
							Common.tell(online, msg.replace("{world}", online.getWorld().getName()));

				} else {
					final World bukkitworld = getServer().getWorld(world);

					if (bukkitworld == null)
						Common.Warn("World \"" + world + "\" doesn't exist. No timed messages broadcast.");
					else
						for (final Player online : bukkitworld.getPlayers())
							if (Common.hasPerm(online, Permissions.VIEW_TIMED_MESSAGES))
								Common.tell(online, msg.replace("{world}", world));
				}
			}

			LagCatcher.end("timed messages");
		}, 20, 20 * Settings.Messages.TIMED_DELAY_SECONDS);
	}

	// ------------------------ static ------------------------

	public static GeoResponse getGeoFor(InetAddress ip) {
		if (!Settings.GEO_DATA || ip == null || ip.getHostAddress() == null)
			return null;

		final String host = ip.getHostAddress();
		GeoResponse geo = geoData.get(host);

		if (geo == null) {
			geo = GeoAPI.track(ip);

			geoData.put(host, geo);
		}

		return geo;
	}

	public static void removeDataFor(Player player) {
		playerData.remove(player.getName());
	}

	public static PlayerCache getDataFor(Player player) {
		final String name = player.getName();
		PlayerCache cache = playerData.get(name);

		if (cache == null) {
			cache = new PlayerCache();
			playerData.put(name, cache);
		}

		cache.onCall(player);

		return cache;
	}

	public static ChatControl instance() {
		if (instance == null)
			instance = new ChatControl();

		return instance;
	}
}
