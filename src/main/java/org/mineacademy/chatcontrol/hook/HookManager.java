package org.mineacademy.chatcontrol.hook;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.jsonsimple.JSONObject;
import org.mineacademy.chatcontrol.jsonsimple.JSONParser;
import org.mineacademy.chatcontrol.rules.ChatCeaser.PacketCancelledException;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.Permissions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.messaging.IMessageRecipient;
import com.massivecraft.factions.entity.MPlayer;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import fr.xephi.authme.data.auth.PlayerCache;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

/**
 * Class managing third-party integrations.
 */
public final class HookManager {

	private static AuthMeHook authMe;
	private static EssentialsHook essentials;
	private static MultiverseHook multiverse;
	private static ProtocolLibHook protocolLib;
	private static TownyHook towny;
	private static VaultHook vault;
	private static FactionsHook factions;
	private static PlaceholderAPIHook papi;

	private HookManager() {
	}

	public static void loadDependencies() {
		if (Common.doesPluginExist("AuthMe", "Country Variables"))
			authMe = new AuthMeHook();

		if (Common.doesPluginExist("Essentials"))
			if (Bukkit.getPluginManager().getPlugin("Essentials").getDescription().getAuthors().contains("Iaccidentally"))
				essentials = new EssentialsHook();

		if (Common.doesPluginExist("Multiverse-Core", "World Alias"))
			multiverse = new MultiverseHook();

		if (Common.doesPluginExist("ProtocolLib", "Packet Features"))
			protocolLib = new ProtocolLibHook();

		if (Common.doesPluginExist("Towny"))
			towny = new TownyHook();

		if (Common.doesPluginExist("Vault"))
			vault = new VaultHook();

		if (Common.doesPluginExist("Factions")) {
			if (Bukkit.getPluginManager().getPlugin("Factions").getDescription().getVersion().startsWith("1.6")) {
				Common.log("ChatControl only supports Factions the free version v2.x");
				return;
			}

			Class<?> mplayer = null;

			try {
				mplayer = Class.forName("com.massivecraft.factions.entity.MPlayer"); // only support the free version of the plugin
			} catch (final ClassNotFoundException ex) {
				Common.logInFrame(false, "Plugin only support hooking into", "the free version of Factions plugin.");
			}

			if (mplayer != null) {
				factions = new FactionsHook();
				Common.log("&3Hooked into&8: &fFactions");
			}
		}

		if (Common.doesPluginExist("PlaceholderAPI"))
			papi = new PlaceholderAPIHook();
	}

	public static boolean isAuthMeLoaded() {
		return authMe != null;
	}

	public static boolean isEssentialsLoaded() {
		return essentials != null;
	}

	public static boolean isMultiverseLoaded() {
		return multiverse != null;
	}

	public static boolean isProtocolLibLoaded() {
		return protocolLib != null;
	}

	public static boolean isTownyLoaded() {
		return towny != null;
	}

	public static boolean isVaultLoaded() {
		return vault != null;
	}

	public static boolean isFactionsLoaded() {
		return factions != null;
	}

	public static boolean isPlaceholderAPILoaded() {
		return papi != null;
	}

	public static boolean isLogged(Player player) {
		return isAuthMeLoaded() ? authMe.isLogged(player) : true;
	}

	public static boolean isAfk(String playerName) {
		return isEssentialsLoaded() ? essentials.isAfk(playerName) : false;
	}

	public static Player getReplyTo(String playerName) {
		return isEssentialsLoaded() ? essentials.getReplyTo(playerName) : null;
	}

	public static String getNick(Player player) {
		final String nick = isEssentialsLoaded() ? essentials.getNick(player.getName()) : "";

		return nick.isEmpty() ? player.getName() : nick;
	}

	public static String getWorldAlias(String world) {
		return isMultiverseLoaded() ? multiverse.getWorldAlias(world) : world;
	}

	public static void initPacketListening() {
		if (isProtocolLibLoaded())
			protocolLib.initPacketListening();
	}

	public static String getNation(Player player) {
		return isTownyLoaded() ? towny.getNation(player) : "";
	}

	public static String getTownName(Player player) {
		return isTownyLoaded() ? towny.getTownName(player) : "";
	}

	public static String getFaction(Player player) {
		return isFactionsLoaded() ? factions.getFaction(player) : "";
	}

	public static String getPlayerPrefix(Player player) {
		return isVaultLoaded() ? vault.getPlayerPrefix(player) : "";
	}

	public static String getPlayerSuffix(Player player) {
		return isVaultLoaded() ? vault.getPlayerSuffix(player) : "";
	}

	public static void takeMoney(String playerName, double amount) {
		if (isVaultLoaded())
			vault.takeMoney(playerName, amount);
	}

	public static String replacePAPIPlaceholders(Player player, String msg) {
		return isPlaceholderAPILoaded() ? papi.replacePlaceholders(player, msg) : msg;
	}
}

class AuthMeHook {

	boolean isLogged(Player player) {
		try {
			return fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player);

		} catch (final Throwable t) {

			try {
				return ((PlayerCache) fr.xephi.authme.data.auth.PlayerCache.class.getMethod("getInstance").invoke(null)).isAuthenticated(player.getName());

			} catch (final Throwable tt) {

				try {
					return (Boolean) Class.forName("fr.xephi.authme.api.API").getMethod("isAuthenticated", Player.class).invoke(null, player);

				} catch (final Throwable ttt) {
					return true;
				}
			}
		}
	}
}

class EssentialsHook {

	private final Essentials essentialsApi;

	EssentialsHook() {
		this.essentialsApi = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	boolean isAfk(String playerName) {
		final User user = this.getUser(playerName);

		return user != null ? user.isAfk() : false;
	}

	Player getReplyTo(String playerName) {
		final User user = this.getUser(playerName);
		if (user == null)
			return null;

		try {
			final IMessageRecipient recipient = user.getReplyRecipient();

			if (recipient != null) {
				final String replyPlayer = recipient.getName();
				final Player bukkitPlayer = Bukkit.getPlayer(replyPlayer);

				if (bukkitPlayer != null && bukkitPlayer.isOnline())
					return bukkitPlayer;
			}

		} catch (final NoClassDefFoundError | NoSuchMethodError ex) {
			// fallback
			try {
				final CommandSource source = (CommandSource) user.getClass().getMethod("getReplyTo").invoke(user);

				if (source != null && source.isPlayer()) {
					final Player player = source.getPlayer();

					if (player != null && player.isOnline())
						return player;
				}
			} catch (final ReflectiveOperationException ex2) {
				// ex2.printStackTrace();
			}
		}

		return null;
	}

	String getNick(String playerName) {
		final User user = this.getUser(playerName);
		if (user == null)
			return playerName;

		final String nick = user.getNickname();
		return nick != null ? nick : "";
	}

	private User getUser(String playerName) {
		return this.essentialsApi.getUserMap().getUser(playerName);
	}

}

class MultiverseHook {

	private final MultiverseCore multiVerse;

	MultiverseHook() {
		this.multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
	}

	String getWorldAlias(String world) {
		final MultiverseWorld mvWorld = this.multiVerse.getMVWorldManager().getMVWorld(world);

		if (mvWorld != null)
			return mvWorld.getColoredWorldString();

		return world;
	}
}

class TownyHook {

	String getNation(Player player) {
		try {
			final Town town = this.getTown(player);

			return town != null ? town.getNation().getName() : "";
		} catch (final Exception e) {
			return "";
		}
	}

	String getTownName(Player player) {
		final Town town = this.getTown(player);

		return town != null ? town.getName() : "";
	}

	private Town getTown(Player player) {
		try {
			final Resident resident = com.palmergames.bukkit.towny.TownyUniverse.getInstance().getResident(player.getName());

			if (resident != null)
				return resident.getTown();
		} catch (final Throwable e) {
		}

		return null;
	}
}

class ProtocolLibHook {

	private final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
	private final JSONParser parser = new JSONParser();

	void initPacketListening() {

		if (Settings.Packets.TabComplete.DISABLE)
			this.manager.addPacketListener(new PacketAdapter(ChatControl.getInstance(), PacketType.Play.Client.TAB_COMPLETE) {

				@Override
				public void onPacketReceiving(PacketEvent event) {
					if (Common.hasPermission(event.getPlayer(), Permissions.Bypass.TAB_COMPLETE))
						return;

					final String message = event.getPacket().getStrings().read(0).trim();

					if (Settings.Packets.TabComplete.DISABLE_ONLY_IN_CMDS && !message.startsWith("/"))
						return;

					if (Settings.Packets.TabComplete.ALLOW_IF_SPACE && message.contains(" "))
						return;

					if (message.length() > Settings.Packets.TabComplete.IGNORE_ABOVE_LENGTH)
						event.setCancelled(true);
				}
			});

		if (Settings.Rules.CHECK_PACKETS) {

			boolean is_1_19 = false;

			try {
				is_1_19 = MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.WILD_UPDATE);
			} catch (final Throwable t) {
				// not
			}

			if (is_1_19)
				Common.log("Parsing packet rules only works on Minecraft 1.18 and lower. Upgrade to mineacademy.org/chatcontrol-red for new MC support.");

			else
				this.manager.addPacketListener(new PacketAdapter(ChatControl.getInstance(), PacketType.Play.Server.CHAT) {

					@Override
					public void onPacketSending(PacketEvent event) {
						if (event.getPlayer() == null || !event.getPlayer().isOnline())
							return;

						final StructureModifier<WrappedChatComponent> chat = event.getPacket().getChatComponents();

						final WrappedChatComponent component = chat.read(0);
						if (component == null)
							return;

						final String raw = component.getJson();
						if (raw == null || raw.isEmpty())
							return;

						if (CompatProvider.isBungeeApiPresent())
							try {
								String unpacked = CompatProvider.unpackMessage(raw, true);
								if (unpacked == null || unpacked.isEmpty())
									return;

								final String oldUnpacked = unpacked;

								try {
									unpacked = ChatControl.getInstance().getChatCeaser().parsePacketRulesRaw(event.getPlayer(), unpacked);
								} catch (final PacketCancelledException ex) {
									event.setCancelled(true);
									return;
								}

								if (!oldUnpacked.equals(unpacked))
									chat.write(0, WrappedChatComponent.fromJson(CompatProvider.packMessage(unpacked)));

								return;

							} catch (final CompatProvider.InteractiveTextFoundException ex) {
							}

						Object parsed;

						try {
							parsed = ProtocolLibHook.this.parser.parse(raw);
						} catch (final Throwable t) {
							return;
						}

						if (!(parsed instanceof JSONObject))
							return;

						final JSONObject json = (JSONObject) parsed;
						final String origin = json.toString();

						try {
							ChatControl.getInstance().getChatCeaser().parsePacketRules(event.getPlayer(), json);
						} catch (final PacketCancelledException ex) {
							event.setCancelled(true);
							return;
						}

						if (!json.toString().equals(origin))
							chat.write(0, WrappedChatComponent.fromJson(json.toString()));
					}
				});
		}
	}
}

class VaultHook {

	private Chat chat;
	private Economy economy;

	VaultHook() {
		final ServicesManager services = Bukkit.getServicesManager();

		final RegisteredServiceProvider<Economy> economyProvider = services.getRegistration(Economy.class);

		if (economyProvider != null)
			this.economy = economyProvider.getProvider();

		final RegisteredServiceProvider<Chat> chatProvider = services.getRegistration(Chat.class);

		if (chatProvider != null)
			this.chat = chatProvider.getProvider();

		else if (Settings.Chat.Formatter.ENABLED)
			Common.logInFrame(true,
					"You have enabled chat formatter, but no",
					"permissions nor chat plugins were found.",
					"Run /vault-info and check what is missing");
	}

	String getPlayerPrefix(Player player) {
		if (this.chat == null)
			return "";

		return this.chat.getPlayerPrefix(player);
	}

	String getPlayerSuffix(Player player) {
		if (this.chat == null)
			return "";

		return this.chat.getPlayerSuffix(player);
	}

	void takeMoney(String player, double amount) {
		if (this.economy != null)
			this.economy.withdrawPlayer(player, amount);
	}
}

class FactionsHook {

	FactionsHook() {
	}

	String getFaction(Player player) {
		return MPlayer.get(player.getUniqueId()).getFactionName();
	}
}

class PlaceholderAPIHook {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[%]([^%]+)[%]");
	private static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	String replacePlaceholders(Player player, String msg) {
		try {
			return this.setBracketPlaceholders(player, msg);

		} catch (final Throwable t) {
			Common.log(
					"PlaceholderAPI failed to replace variables!", "Player: " + player.getName(), "Message: " + msg, "Error: {error}");
			t.printStackTrace();

			return msg;
		}
	}

	private String setBracketPlaceholders(Player player, String text) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

		if (hooks.isEmpty())
			return text;

		text = this.setBracketPlaceholders(player, text, PLACEHOLDER_PATTERN.matcher(text), hooks);
		text = this.setBracketPlaceholders(player, text, BRACKET_PLACEHOLDER_PATTERN.matcher(text), hooks);

		return text;
	}

	private String setBracketPlaceholders(Player player, String text, Matcher matcher, Map<String, PlaceholderHook> hooks) {
		final String oldText = text;

		while (matcher.find()) {

			String format = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (format.startsWith("+")) {
				frontSpace = true;

				format = format.substring(1);
			}

			if (format.endsWith("+")) {
				backSpace = true;

				format = format.substring(0, format.length() - 1);
			}

			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index);
			final String params = format.substring(index + 1);
			final String finalFormat = format;

			if (hooks.containsKey(identifier)) {

				// Wait 0.5 seconds then kill the thread to prevent server
				// crashing on PlaceholderAPI variables hanging up on the main thread
				final Thread currentThread = Thread.currentThread();
				final boolean main = Bukkit.isPrimaryThread();
				final BukkitTask watchDog = new BukkitRunnable() {

					@Override
					public void run() {
						Common.logInFrame(false,
								"IMPORTANT: PREVENTED SERVER CRASH FROM PLACEHOLDERAPI",
								"",
								"Replacing PlaceholderAPI variable took over " + (main ? "1.5" : "4") + " sec",
								"and was interrupted to prevent hanging the server.",
								"",
								"This is typically caused when a variable sends",
								"blocking HTTP request, such as checking stuff on",
								"the Internet or resolving offline player names.",
								"This is NOT error in ChatControl, you need",
								"to contact placeholder expansion author instead.",
								"",
								"Variable: " + finalFormat,
								"Text: " + oldText,
								"Player: " + (player == null ? "none" : player.getName()));

						currentThread.stop();
					}
				}.runTaskLater(ChatControl.getInstance(), main ? 30 : 80);

				String value = hooks.get(identifier).onRequest(player, params);

				// Indicate we no longer have to kill the thread
				watchDog.cancel();

				if (value != null) {
					value = Matcher.quoteReplacement(Common.colorize(value));

					text = text.replaceAll(Pattern.quote(matcher.group()), value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : ""));
				}
			}
		}

		return text;
	}

}