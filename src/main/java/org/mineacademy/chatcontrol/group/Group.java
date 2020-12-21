package org.mineacademy.chatcontrol.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.mineacademy.chatcontrol.group.GroupOption.OptionType;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;

/**
 * A group with settings. A player can be associated with one or more groups
 * according to the settings and their permission.
 */
public class Group {

	/**
	 * The name of the group. A permission in form "chatcontrol.group.NAME" will be
	 * effective on this group.
	 */
	private final String name;
	private final HashMap<OptionType, GroupOption> settings;

	public Group(String name, GroupOption... settings) {
		this(name, Arrays.asList(settings));
	}

	public Group(String name, List<GroupOption> settings) {
		this.name = name;

		final HashMap<OptionType, GroupOption> map = new HashMap<>();

		for (final GroupOption setting : settings)
			map.put(setting.getOption(), setting);

		this.settings = map;
	}

	public String getName() {
		return name;
	}

	public Collection<GroupOption> getSettings() {
		return settings.values();
	}

	/**
	 * Call be null.
	 */
	public GroupOption getSetting(OptionType type) {
		return settings.get(type);
	}

	public void addSetting(OptionType type, Object value) {
		Validate.isTrue(!settings.containsKey(type), "Duplicate setting: " + type + " for: " + name);

		settings.put(type, type.create(value));
	}

	public static List<Group> loadFor(Player pl) {
		final Set<Group> playerGroups = new HashSet<>();

		for (final Group group : Settings.Groups.LOADED_GROUPS)
			if (Common.hasPerm(pl, "chatcontrol.group." + group.name)) {
				Common.Debug("Adding " + pl.getName() + " to group " + group.name);
				playerGroups.add(group);
			}

		return new ArrayList<>(playerGroups);
	}
}