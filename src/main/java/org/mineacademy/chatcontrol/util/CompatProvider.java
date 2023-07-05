package org.mineacademy.chatcontrol.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

/**
 * A utility class to help with cross-version compatibility.
 */
public final class CompatProvider {

	/*
	 * Cache reflection method to increase performance.
	 */
	private static Method getPlayersMethod;
	private static Method getHealthMethod;

	/*
	 * Cache flags for maximum performance.
	 */
	private static boolean isGetPlayersCollection = false;
	private static boolean isGetHealthDouble = false;
	private static boolean bungeeApiPresent = true;
	private static boolean hasSounds = true;
	private static boolean newSoundClass = true;

	private CompatProvider() {
	}

	/**
	 * Initialize the reflection class.
	 */
	public static void setupReflection() {
		try {

			// Minecraft 1.2.5 has no sound class
			try {
				Class.forName("org.bukkit.Sound");

			} catch (final Throwable ex) {
				hasSounds = false;
			}

			getPlayersMethod = Bukkit.class.getMethod("getOnlinePlayers");
			isGetPlayersCollection = getPlayersMethod.getReturnType() == Collection.class;
			getHealthMethod = Player.class.getMethod("getHealth");
			isGetHealthDouble = getHealthMethod.getReturnType() == double.class;

			try {
				Class.forName("net.md_5.bungee.api.chat.BaseComponent");
			} catch (final ClassNotFoundException ex) {
				bungeeApiPresent = false;
			}

			try {
				Sound.valueOf("BLOCK_END_GATEWAY_SPAWN").ordinal();
			} catch (final Throwable t) {
				newSoundClass = false;
			}

		} catch (final ReflectiveOperationException ex) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Attempts to load yaml configuration from the given input stream.
	 *
	 * @param stream
	 * @return
	 */
	public static YamlConfiguration loadConfiguration(InputStream stream) {
		try {
			return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));

		} catch (final NoSuchMethodError ex) {
			try {
				return (YamlConfiguration) YamlConfiguration.class.getMethod("loadConfiguration", InputStream.class).invoke(null, stream);

			} catch (final ReflectiveOperationException e) {
				throw new Error("Failed to load configuration from input stream", e);
			}
		}
	}

	/**
	 * Get the health of the player.
	 *
	 * @param player
	 * @return
	 */
	public static int getHealth(Player player) {
		return isGetHealthDouble ? (int) player.getHealth() : getHealhLegacy(player);
	}

	/**
	 * Get all online players.
	 *
	 * @return
	 */
	public static Collection<? extends Player> getOnlinePlayers() {
		return isGetPlayersCollection ? Bukkit.getOnlinePlayers() : Arrays.asList(getPlayersLegacy());
	}

	/**
	 * Converts chat message in JSON (IChatBaseComponent) to one lined old style
	 * message with color codes. e.g. {text:"Hello world",color="red"} converts to
	 * &cHello world.
	 *
	 * @param json
	 * @param denyEvents if an exception should be thrown if hover/click event is found.
	 * @return
	 *
	 * @throws InteractiveTextFoundException if click/hover event are found.
	 *                                       Such events would be removed, and therefore message
	 *                                       containing them shall not be unpacked.
	 */
	public static String unpackMessage(String json, boolean denyEvents) throws InteractiveTextFoundException {
		Common.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");
		String text = "";

		try {
			for (final BaseComponent component : ComponentSerializer.parse(json)) {
				if ((component.getHoverEvent() != null || component.getClickEvent() != null) && denyEvents)
					throw new InteractiveTextFoundException();

				text += component.toLegacyText();
			}
		} catch (final Throwable t) {
			Common.debug("Unable to parse JSON message. Got " + t.getMessage());
		}

		return text;
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}.
	 *
	 * @param message
	 * @return
	 */
	public static String packMessage(String message) {
		Common.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		return ComponentSerializer.toString(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
	}

	/**
	 * Looks up the {@link Sound} bukkit class from the given sound name, falls back to note bass if not found.
	 *
	 * @param name
	 * @return
	 */
	public static Sound parseSound(String name) {
		try {
			return Sound.valueOf(name);

		} catch (final Throwable t) {
			Common.log("Warning: Your settings.yml or another file had invalid sound '" + name + "', using note bass as default");

			try {
				return Sound.valueOf("NOTE_BASS");

			} catch (final Throwable tt) {
				return Sound.valueOf("BLOCK_NOTE_BLOCK_BASS");
			}
		}
	}

	/**
	 * Retrieves a compatible chat event.
	 *
	 * @return
	 */
	public static final Class<? extends Event> compatChatEvent() {
		try {
			return (Class<? extends Event>) Class.forName("org.bukkit.event.player.AsyncPlayerChatEvent");
		} catch (final ClassNotFoundException ex) {
			try {
				return (Class<? extends Event>) Class.forName("org.bukkit.event.player.PlayerChatEvent");

			} catch (final ClassNotFoundException tt) {
				throw new RuntimeException("ChatControl is incompatible with your MC version!");
			}
		}
	}

	/**
	 * Return true if we have 1.9+ sound names.
	 *
	 * @return
	 */
	public static boolean hasNewSoundNames() {
		return newSoundClass;
	}

	/**
	 * Return true if we have 1.3.2+ sound class.
	 *
	 * @return
	 */
	public static boolean hasSounds() {
		return hasSounds;
	}

	/*
	 * Helper method to get players as srray.
	 */
	private static Player[] getPlayersLegacy() {
		try {
			return (Player[]) getPlayersMethod.invoke(null);
		} catch (final ReflectiveOperationException ex) {
			throw new RuntimeException("Reflection malfunction", ex);
		}
	}

	/*
	 * Helper method to get a player's health.
	 */
	private static int getHealhLegacy(Player player) {
		try {
			return (int) getHealthMethod.invoke(player);
		} catch (final ReflectiveOperationException ex) {
			throw new RuntimeException("Reflection malfunction", ex);
		}
	}

	/**
	 * A special exception used in rules to stop executing when click/hover effects are found.
	 */
	public static class InteractiveTextFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InteractiveTextFoundException() {
		}
	}
}