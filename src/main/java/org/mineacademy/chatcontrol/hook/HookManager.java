package org.mineacademy.chatcontrol.hook;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mineacademy.chatcontrol.ChatControl;
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
import com.palmergames.bukkit.towny.object.TownyUniverse;

import fr.xephi.authme.data.auth.PlayerCache;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

public class HookManager {

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
				Common.Log("ChatControl only supports Factions the free version v2.x");
				return;
			}

			Class<?> mplayer = null;

			try {
				mplayer = Class.forName("com.massivecraft.factions.entity.MPlayer"); // only support the free version of the plugin
			} catch (final ClassNotFoundException ex) {
				Common.LogInFrame(false, "Plugin only support hooking into", "the free version of Factions plugin.");
			}

			if (mplayer != null) {
				factions = new FactionsHook();
				Common.Log("&3Hooked into&8: &fFactions");
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

	// ------------------ delegate methods, reason it's here = prevent errors when
	// class loads but plugin is missing

	/*
	 * public static String getCountryCode(Player pl) { return isAuthMeLoaded() ?
	 * authMe.getCountryCode(pl) : ""; }
	 *
	 * public static String getCountryName(Player pl) { return isAuthMeLoaded() ?
	 * authMe.getCountryName(pl) : ""; }
	 */

	public static boolean isLogged(Player pl) {
		return isAuthMeLoaded() ? authMe.isLogged(pl) : true;
	}

	public static boolean isAfk(String pl) {
		return isEssentialsLoaded() ? essentials.isAfk(pl) : false;
	}

	public static Player getReplyTo(String pl) {
		return isEssentialsLoaded() ? essentials.getReplyTo(pl) : null;
	}

	public static String getNick(Player pl) {
		final String nick = isEssentialsLoaded() ? essentials.getNick(pl.getName()) : "";

		return nick.isEmpty() ? pl.getName() : nick;
	}

	public static String getWorldAlias(String world) {
		return isMultiverseLoaded() ? multiverse.getWorldAlias(world) : world;
	}

	public static void initPacketListening() {
		if (isProtocolLibLoaded())
			protocolLib.initPacketListening();
	}

	public static String getNation(Player pl) {
		return isTownyLoaded() ? towny.getNation(pl) : "";
	}

	public static String getTownName(Player pl) {
		return isTownyLoaded() ? towny.getTownName(pl) : "";
	}

	public static String getFaction(Player pl) {
		return isFactionsLoaded() ? factions.getFaction(pl) : "";
	}

	public static String getPlayerPrefix(Player pl) {
		return isVaultLoaded() ? vault.getPlayerPrefix(pl) : "";
	}

	public static String getPlayerSuffix(Player pl) {
		return isVaultLoaded() ? vault.getPlayerSuffix(pl) : "";
	}

	public static void takeMoney(String player, double amount) {
		if (isVaultLoaded())
			vault.takeMoney(player, amount);
	}

	public static String replacePAPIPlaceholders(Player pl, String msg) {
		return isPlaceholderAPILoaded() ? papi.replacePlaceholders(pl, msg) : msg;
	}
}

class AuthMeHook {

	boolean isLogged(Player pl) {
		try {
			return fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(pl);

		} catch (final Throwable t) {

			try {
				return ((PlayerCache) fr.xephi.authme.data.auth.PlayerCache.class.getMethod("getInstance").invoke(null)).isAuthenticated(pl.getName());

			} catch (final Throwable tt) {

				try {
					return (Boolean) Class.forName("fr.xephi.authme.api.API").getMethod("isAuthenticated", Player.class).invoke(null, pl);

				} catch (final Throwable ttt) {
					return true;
				}
			}
		}
	}
}

class EssentialsHook {

	private final Essentials ess;

	EssentialsHook() {
		ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	boolean isAfk(String pl) {
		final User user = getUser(pl);

		return user != null ? user.isAfk() : false;
	}

	Player getReplyTo(String pl) {
		final User user = getUser(pl);
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

	String getNick(String pl) {
		final User user = getUser(pl);
		if (user == null)
			return pl;

		final String nick = user.getNickname();
		return nick != null ? nick : "";
	}

	private User getUser(String pl) {
		return ess.getUserMap().getUser(pl);
	}

}

class MultiverseHook {

	private final MultiverseCore multiVerse;

	MultiverseHook() {
		multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
	}

	String getWorldAlias(String world) {
		final MultiverseWorld mvWorld = multiVerse.getMVWorldManager().getMVWorld(world);

		if (mvWorld != null)
			return mvWorld.getColoredWorldString();

		return world;
	}
}

class TownyHook {

	String getNation(Player pl) {
		try {
			final Town t = getTown(pl);

			return t != null ? t.getNation().getName() : "";
		} catch (final Exception e) {
			return "";
		}
	}

	String getTownName(Player pl) {
		final Town t = getTown(pl);

		return t != null ? t.getName() : "";
	}

	private Town getTown(Player pl) {
		try {
			final Resident res = TownyUniverse.getDataSource().getResident(pl.getName());

			if (res != null)
				return res.getTown();
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
			manager.addPacketListener(new PacketAdapter(ChatControl.instance(), PacketType.Play.Client.TAB_COMPLETE) {

				@Override
				public void onPacketReceiving(PacketEvent e) {
					if (Common.hasPerm(e.getPlayer(), Permissions.Bypasses.TAB_COMPLETE))
						return;

					final String msg = e.getPacket().getStrings().read(0).trim();

					if (Settings.Packets.TabComplete.DISABLE_ONLY_IN_CMDS && !msg.startsWith("/"))
						return;

					if (Settings.Packets.TabComplete.ALLOW_IF_SPACE && msg.contains(" "))
						return;

					if (msg.length() > Settings.Packets.TabComplete.IGNORE_ABOVE_LENGTH)
						e.setCancelled(true);
				}
			});

		if (Settings.Rules.CHECK_PACKETS)
			manager.addPacketListener(new PacketAdapter(ChatControl.instance(), PacketType.Play.Server.CHAT) {

				@Override
				public void onPacketSending(PacketEvent e) {
					if (e.getPlayer() == null || !e.getPlayer().isOnline())
						return;

					final StructureModifier<WrappedChatComponent> chat = e.getPacket().getChatComponents();

					final WrappedChatComponent comp = chat.read(0);
					if (comp == null)
						return;

					final String raw = comp.getJson();
					if (raw == null || raw.isEmpty())
						return;

					if (Settings.Rules.UNPACK_PACKET_MESSAGE)
						try {
							String unpacked = CompatProvider.unpackMessage(raw, true);
							if (unpacked == null || unpacked.isEmpty())
								return;

							final String oldUnpacked = unpacked;

							try {
								unpacked = ChatControl.instance().chatCeaser.parsePacketRulesRaw(e.getPlayer(), unpacked);
							} catch (final PacketCancelledException ex) {
								e.setCancelled(true);
								return;
							}

							if (!oldUnpacked.equals(unpacked))
								chat.write(0, WrappedChatComponent.fromJson(CompatProvider.packMessage(unpacked)));

							return;

						} catch (final CompatProvider.InteractiveTextFoundException ex) {
						}

					Object parsed;

					try {
						parsed = parser.parse(raw);
					} catch (final Throwable t) {
						return;
					}

					if (!(parsed instanceof JSONObject))
						return;

					final JSONObject json = (JSONObject) parsed;
					final String origin = json.toJSONString();

					try {
						ChatControl.instance().chatCeaser.parsePacketRules(e.getPlayer(), json);
					} catch (final PacketCancelledException ex) {
						e.setCancelled(true);
						return;
					}

					if (!json.toJSONString().equals(origin))
						chat.write(0, WrappedChatComponent.fromJson(json.toJSONString()));
				}
			});
	}
}

class VaultHook {

	private Chat chat;
	private Economy economy;

	VaultHook() {
		final ServicesManager services = Bukkit.getServicesManager();

		final RegisteredServiceProvider<Economy> economyProvider = services.getRegistration(Economy.class);

		if (economyProvider != null)
			economy = economyProvider.getProvider();
		else
			Common.Log("&cEconomy plugin not found");

		final RegisteredServiceProvider<Chat> chatProvider = services.getRegistration(Chat.class);

		if (chatProvider != null)
			chat = chatProvider.getProvider();
		else if (Settings.Chat.Formatter.ENABLED)
			Common.LogInFrame(true, "You have enabled chat formatter", "but no permissions and chat", "plugin was found!", "Run /vault-info and check what is missing");
	}

	String getPlayerPrefix(Player pl) {
		if (chat == null)
			return "";

		return chat.getPlayerPrefix(pl);
	}

	String getPlayerSuffix(Player pl) {
		if (chat == null)
			return "";

		return chat.getPlayerSuffix(pl);
	}

	void takeMoney(String player, double amount) {
		if (economy != null)
			economy.withdrawPlayer(player, amount);
	}
}

class FactionsHook {

	FactionsHook() {
	}

	String getFaction(Player pl) {
		return MPlayer.get(pl.getUniqueId()).getFactionName();
	}
}

class PlaceholderAPIHook {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[%]([^%]+)[%]");

	String replacePlaceholders(Player pl, String msg) {
		try {
			return setBracketPlaceholders(pl, msg);

		} catch (final Throwable t) {
			Common.Log(
					"PlaceholderAPI failed to replace variables!", "Player: " + pl.getName(), "Message: " + msg, "Error: {error}");
			t.printStackTrace();

			return msg;
		}
	}

	private String setBracketPlaceholders(Player player, String text) throws Throwable {
		final Map<String, PlaceholderHook> placeholders = PlaceholderAPI.getPlaceholders();
		final Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

		while (matcher.find()) {

			final String format = matcher.group(1);
			final int index = format.indexOf("_");

			if (index > 0 && index < format.length()) {

				final String identifier = format.substring(0, index).toLowerCase();
				final String params = format.substring(index + 1);

				Common.Debug("Placeholders: " + placeholders);

				if (placeholders.containsKey(identifier)) {
					final String value = placeholders.get(identifier).onRequest(player, params);

					Common.Debug("Replacing {" + identifier + "_" + params + "} with '" + value + "'");

					if (value != null)
						text = text.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(value));
				}
			}
		}

		return Common.colorize(text);
	}

}