package org.mineacademy.chatcontrol.settings;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.chatcontrol.ChatControl;
import org.mineacademy.chatcontrol.PlayerCache;
import org.mineacademy.chatcontrol.group.Group;
import org.mineacademy.chatcontrol.group.GroupOption;
import org.mineacademy.chatcontrol.util.Common;
import org.mineacademy.chatcontrol.util.CompatProvider;
import org.mineacademy.chatcontrol.util.Writer;

/**
 * A helper managing settings and localization classes
 */
public abstract class ConfHelper {

	protected static YamlConfiguration config;
	protected static File file;

	private static String pathPrefix = null;
	private static boolean save = false;

	protected ConfHelper() {
	}

	public static void loadAll() throws Exception {
		final File datafolder = ChatControl.getInstance().getDataFolder();
		final File oldconfig = new File(datafolder, "config.yml");

		if (oldconfig.exists()) {
			final String version = YamlConfiguration.loadConfiguration(oldconfig).getString("Do_Not_Change_Version_Number");

			Common.logInFrame(false, "&bDetected old &fconfiguration &bfrom version " + version, "&bThis is &cnot &bcompatible with the new version", "&bEntire folder renamed to ChatControl_Old");
			datafolder.renameTo(new File(datafolder.getParent(), "ChatControl_Old"));
			datafolder.delete();
		}

		// Order matters.
		Settings.load();
		Localization.load();
	}

	protected static void loadValues(Class<?> clazz) throws Exception {
		Objects.requireNonNull(config, "YamlConfiguration is null!");

		// The class itself.
		invokeMethods(clazz);

		// All sub-classes.
		for (final Class<?> subClazz : clazz.getDeclaredClasses()) {
			invokeMethods(subClazz);

			// And classes in sub-classes.
			for (final Class<?> subSubClazz : subClazz.getDeclaredClasses())
				invokeMethods(subSubClazz);
		}

		save();
	}

	private static void invokeMethods(Class<?> clazz) throws Exception {
		for (final Method m : clazz.getDeclaredMethods()) {
			final int modifier = m.getModifiers();

			if (Modifier.isPrivate(modifier) && Modifier.isStatic(modifier) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0) {
				Common.checkBoolean(m.getName().equals("init") || m.getName().startsWith("lambda"), "Method must be called init() not " + m.getName());

				m.setAccessible(true);
				m.invoke(null);
				pathPrefix(null);
			}
		}

		ensureNonNull(clazz);
	}

	private static void ensureNonNull(Class<?> clazz) throws Exception {
		for (final Field f : clazz.getDeclaredFields())
			Objects.requireNonNull(f.get(null), "Null field '" + f.getName() + "' in " + clazz.getName() + ".class!");
	}

	private static void save() throws IOException {
		if (file != null && save) {
			config.options()
					.header("!---------------------------------------------------------!\n" + "! File automatically updated at " + Common.getFormattedDate() + "\n" + "! to plugin version v" + ChatControl.getInstance().getDescription().getVersion() + "\n" + "!---------------------------------------------------------!\n" + "! Unfortunatelly due to how Bukkit handles YAML\n" + "! configurations, all comments (#) were wiped. \n" + "! For reference values and comments visit\n"
							+ "! https://github.com/kangarko/chatcontrol\n" + "!---------------------------------------------------------!\n");
			config.save(file);

			Common.log("&eSaved updated file: " + file.getName() + " (# Comments removed)");
			save = false;
		}
	}

	protected static void createFileAndLoad(String path, Class<?> loadFrom) throws Exception {
		Objects.requireNonNull(path, "File path cannot be null!");

		file = Writer.extract(path);

		config = new YamlConfiguration();
		config.load(file);

		loadValues(loadFrom);
	}

	// --------------- Getters ---------------

	private static Object getObject(String path, String def, boolean addPathPrefix) {
		if (addPathPrefix)
			path = addPathPrefix(path);
		addDefault(path, def);

		return config.get(path);
	}

	protected static boolean getBoolean(String path, boolean def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Common.checkBoolean(config.isBoolean(path), "Malformed config value, expected boolean at: " + path);
		return config.getBoolean(path);
	}

	protected static String getString(String path, String def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Common.checkBoolean(config.isString(path), "Malformed config value, expected string at: " + path);
		return config.getString(path);
	}

	protected static int getInteger(String path, int def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Common.checkBoolean(config.isInt(path), "Malformed config value, expected integer at: " + path);
		return config.getInt(path);
	}

	protected static double getDouble(String path, double def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Common.checkBoolean(config.isDouble(path), "Malformed config value, expected double at: " + path);
		return config.getDouble(path);
	}

	protected static HashMap<String, List<String>> getValuesAndList(String path, HashMap<String, List<String>> def) {
		path = addPathPrefix(path);

		// add default
		if (!config.isSet(path)) {
			validate(path, def);

			for (final String str : def.keySet())
				config.set(path + "." + str, def.get(str));
		}

		Common.checkBoolean(config.isConfigurationSection(path), "Malformed config value, expected configuration section at: " + path);
		final HashMap<String, List<String>> keys = new HashMap<>();

		for (final String key : config.getConfigurationSection(path).getKeys(true)) {
			if (keys.containsKey(key))
				Common.warn("Duplicate key: " + key + " in " + path);
			keys.put(key, getStringList(path + "." + key, Arrays.asList(""), false));
		}

		return keys;
	}

	protected static HashMap<String, Object> getValuesAndKeys(String path, HashMap<String, Object> def, boolean deep) {
		if (!path.startsWith(pathPrefix))
			path = addPathPrefix(path);

		// add default
		if (!config.isSet(path) && def != null) {
			validate(path, def);

			for (final String str : def.keySet())
				config.set(path + "." + str, def.get(str));
		}

		Common.checkBoolean(config.isConfigurationSection(path), "Malformed config value, expected configuration section at: " + path);
		final HashMap<String, Object> keys = new HashMap<>();

		for (final String key : config.getConfigurationSection(path).getKeys(deep)) {
			if (keys.containsKey(key))
				Common.warn("Duplicate key: " + key + " in " + path);
			keys.put(key, getObject(path + "." + key, "", false));
		}

		return keys;
	}

	protected static List<String> getStringList(String path, List<String> def, boolean addPathPrefix) {
		if (addPathPrefix)
			path = addPathPrefix(path);
		addDefault(path, def);

		Common.checkBoolean(config.isList(path), "Malformed config value, expected list at: " + path);
		return config.getStringList(path);
	}

	protected static List<String> getStringList(String path, List<String> def) {
		return getStringList(path, def, true);
	}

	protected static ChatMessage getMessage(String path, ChatMessage def) {
		return new ChatMessage(getString(path, def.getMessage()));
	}

	protected static List<Group> getGroups(String path, List<Group> defaults) {
		path = addPathPrefix(path);

		// add default
		if (!config.isConfigurationSection(path)) {
			for (final Group group : defaults) {
				final String groupPath = path + "." + group.getName();

				if (!config.isSet(groupPath))
					for (final GroupOption setting : group.getSettings()) {
						final Object val = setting.getValue();
						addDefault(groupPath + "." + setting.getOption(), val instanceof ChatMessage ? ((ChatMessage) val).getMessage() : val);
					}
			}

			try {
				save();
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}

		// group name, settings
		final List<Group> groups = new ArrayList<>();

		for (final String groupName : config.getConfigurationSection(path).getKeys(false)) {
			// type, value (UNPARSED)
			final HashMap<String, Object> settingsRaw = getValuesAndKeys(path + "." + groupName, null, false);

			final List<GroupOption> settings = new ArrayList<>();

			for (final Map.Entry<String, Object> entry : settingsRaw.entrySet())
				settings.add(GroupOption.OptionType.parseOption(entry.getKey()).create(entry.getValue()));

			groups.add(new Group(groupName, settings));
		}

		return groups;
	}

	protected static void set(String path, Object value) {
		path = addPathPrefix(path);

		validate(path, value);
		config.set(path, value);
	}

	private static <T> void validate(String path, T def) {
		if (file == null)
			throw new IllegalStateException("Inbuilt config doesn't contain " + def.getClass().getTypeName() + " at \"" + path + "\". Is it outdated?");

		save = true;
		Common.log("&fUpdating " + file.getName() + " with &b\'&f" + path + "&b\' &f-> &b\'&f" + def + "&b\'&r");
	}

	// --------------- Lazy helpers ---------------

	private static <T> void addDefault(String path, T def) {
		if (!config.isSet(path)) {
			validate(path, def);
			config.set(path, def);
		}
	}

	private static String addPathPrefix(String path) {
		return pathPrefix != null ? pathPrefix + "." + path : path;
	}

	protected static void pathPrefix(String pathPrefix) {
		ConfHelper.pathPrefix = pathPrefix;
	}

	// --------------- Classes ---------------

	public static class ChatMessage {
		private final Type type;
		private final String message;

		public ChatMessage(String message) {
			if (message.startsWith("kangarko"))
				Thread.dumpStack();

			this.type = Type.fromValue(message);
			this.message = message;
		}

		protected ChatMessage(Type type) {
			Common.checkBoolean(type != Type.CUSTOM, "Type cannot be custom.");

			this.type = type;
			this.message = type == Type.DEFAULT ? "default" : type == Type.HIDDEN ? "hidden" : null;
		}

		public Type getType() {
			return this.type;
		}

		public String getMessage() {
			Objects.requireNonNull(this.message, "Message cannot be null!");
			return this.message;
		}

		public enum Type {
			DEFAULT,
			HIDDEN,
			CUSTOM;

			public static Type fromValue(String raw) {
				switch (raw.toLowerCase()) {
					case "default":
					case "def":
					case "vanilla":
						return DEFAULT;
					case "none":
					case "hide":
					case "hidden":
						return HIDDEN;
					default:
						return CUSTOM;
				}
			}
		}
	}

	public static class CasusHelper {
		private final String akuzativSg; // 1 second (slovak case - sekundu) - not in english
		private final String nominativPl; // 2 to 4 seconds (slovak case - sekundy)
		private final String genitivePl; // 5 and more seconds (slovak case - sekund)

		protected CasusHelper(String raw) {
			final String[] values = raw.split(", ");

			if (values.length == 2) {
				this.akuzativSg = values[0];
				this.nominativPl = values[1];
				this.genitivePl = this.nominativPl;
				return;
			}

			if (values.length != 3)
				throw new RuntimeException("Malformed type, use format: 'second, seconds' OR 'sekundu, sekundy, sekund' (if your language has it)");

			this.akuzativSg = values[0];
			this.nominativPl = values[1];
			this.genitivePl = values[2];
		}

		public String formatNumbers(long count) {
			if (count == 1)
				return this.akuzativSg;
			if (count > 1 && count < 5)
				return this.nominativPl;

			return this.genitivePl;
		}
	}

	public static class SoundHelper {
		public final Sound sound;
		public final float volume, pitch;

		protected SoundHelper(String raw) {
			final String[] values = raw.split(", ");

			if (values.length == 1) {
				this.sound = CompatProvider.parseSound(values[0].toUpperCase());
				this.volume = 1F;
				this.pitch = 1.5F;
				return;
			}

			Common.checkBoolean(values.length == 3, "Malformed sound type, use format: 'sound' OR 'sound, volume, pitch'");
			this.sound = CompatProvider.parseSound(this.mapSomeSounds(values[0].toUpperCase()));
			this.volume = Float.parseFloat(values[1]);
			this.pitch = Float.parseFloat(values[2]);
		}

		@Deprecated
		private String mapSomeSounds(String sound) {

			// 1.9+
			if (CompatProvider.hasNewSoundNames())
				switch (sound) {
					case "SUCCESSFUL_HIT":
						return "ENTITY_ARROW_HIT_PLAYER";

					case "CHICKEN_EGG_POP":
						return "ENTITY_CHICKEN_EGG";
				}
			else
				switch (sound) {
					case "ENTITY_ARROW_HIT_PLAYER":
						return "SUCCESSFUL_HIT";

					case "ENTITY_CHICKEN_EGG":
						return "CHICKEN_EGG_POP";
				}

			return sound;
		}
	}

	public static class GroupSpecificHelper<T> {

		private final GroupOption.OptionType type;
		private final T def;

		protected GroupSpecificHelper(GroupOption.OptionType type, T def) {
			this.type = type;
			this.def = def;
		}

		public T getDefault() {
			return this.def;
		}

		public T getFor(PlayerCache cache) {
			if (!Settings.Groups.ENABLED)
				return this.def;

			for (final Group group : cache.groups) {
				final GroupOption setting = group.getSetting(this.type);

				if (setting != null)
					return (T) setting.getValue();
			}

			return this.def;
		}

		@Override
		public String toString() {
			throw new RuntimeException("call getFor(player)");
		}
	}

	public static class IllegalLocaleException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class InBuiltFileMissingException extends Exception {
		private static final long serialVersionUID = 1L;
		public final String file;

		public InBuiltFileMissingException(String msg, String file) {
			super(msg.replace("{file}", file));
			this.file = file;
		}
	}
}