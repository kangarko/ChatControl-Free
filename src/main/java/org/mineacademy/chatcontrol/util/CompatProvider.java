package org.mineacademy.chatcontrol.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class CompatProvider {

	private static Method getPlayersMethod;
	private static Method getHealthMethod;

	private static boolean isGetPlayersCollection = false;
	private static boolean isGetHealthDouble = false;

	private static boolean bungeeApiPresent = true;

	private static boolean hasSounds = true;
	private static boolean newSoundClass = true;

	private CompatProvider() {
	}

	public static void setupReflection() {
		try {
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

	public static YamlConfiguration loadConfiguration(InputStream is) {
		try {
			return YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));

		} catch (final NoSuchMethodError ex) {
			try {
				return (YamlConfiguration) YamlConfiguration.class.getMethod("loadConfiguration", InputStream.class).invoke(null, is);

			} catch (final ReflectiveOperationException e) {
				throw new Error("Failed to load configuration from input stream", e);
			}
		}
	}

	public static int getHealth(Player pl) {
		return isGetHealthDouble ? (int) pl.getHealth() : getHealhLegacy(pl);
	}

	public static Collection<? extends Player> getAllPlayers() {
		return isGetPlayersCollection ? Bukkit.getOnlinePlayers() : Arrays.asList(getPlayersLegacy());
	}

	/**
	 * Converts chat message in JSON (IChatBaseComponent) to one lined old style
	 * message with color codes. e.g. {text:"Hello world",color="red"} converts to
	 * &cHello world
	 *
	 * @throws InteractiveTextFoundException
	 *             if click/hover event are found. Such events would be removed, and
	 *             therefore message containing them shall not be unpacked
	 *
	 * @param denyEvents
	 *            if an exception should be thrown if hover/click event is found.
	 */
	public static String unpackMessage(String json, boolean denyEvents) throws InteractiveTextFoundException {
		Validate.isTrue(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");
		String text = "";

		try {
			for (final BaseComponent comp : ComponentSerializer.parse(json)) {
				if ((comp.getHoverEvent() != null || comp.getClickEvent() != null) && denyEvents)
					throw new InteractiveTextFoundException();

				text += comp.toLegacyText();
			}
		} catch (final Throwable t) {
			Common.Debug("Unable to parse JSON message. Got " + t.getMessage());
		}

		return text;
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 */
	public static String packMessage(String message) {
		Validate.isTrue(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		return ComponentSerializer.toString(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
	}

	public static Sound parseSound(String name) {
		try {
			return Sound.valueOf(name);

		} catch (final Throwable t) {
			Common.Log("Warning: Your settings.yml or another file had invalid sound '" + name + "', using note bass as default");

			try {
				return Sound.valueOf("NOTE_BASS");

			} catch (final Throwable tt) {
				return Sound.valueOf("BLOCK_NOTE_BLOCK_BASS");
			}
		}
	}

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

	public static boolean hasNewSoundNames() {
		return newSoundClass;
	}

	public static boolean hasSounds() {
		return hasSounds;
	}

	// ------------------------ Legacy ------------------------

	private static Player[] getPlayersLegacy() {
		try {
			return (Player[]) getPlayersMethod.invoke(null);
		} catch (final ReflectiveOperationException ex) {
			throw new RuntimeException("Reflection malfunction", ex);
		}
	}

	private static int getHealhLegacy(Player pl) {
		try {
			return (int) getHealthMethod.invoke(pl);
		} catch (final ReflectiveOperationException ex) {
			throw new RuntimeException("Reflection malfunction", ex);
		}
	}

	public static class InteractiveTextFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InteractiveTextFoundException() {
		}
	}
}