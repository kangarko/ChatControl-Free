package org.mineacademy.chatcontrol.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.mineacademy.chatcontrol.group.GroupOption.OptionType;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;

import lombok.Getter;

/**
 * A group with settings. A player can be associated with one or more groups
 * according to the settings and their permission.
 */
public final class Group {

	/**
	 * The name of the group. A permission in form "chatcontrol.group.NAME" will be
	 * effective on this group.
	 */
	@Getter
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

	public Collection<GroupOption> getSettings() {
		return this.settings.values();
	}

	/**
	 * Call be null.
	 *
	 * @param type
	 * @return
	 */
	public GroupOption getSetting(OptionType type) {
		return this.settings.get(type);
	}

	public void addSetting(OptionType type, Object value) {
		Common.checkBoolean(!this.settings.containsKey(type), "Duplicate setting: " + type + " for: " + this.name);

		this.settings.put(type, type.create(value));
	}

	public static List<Group> loadFor(Player player) {
		final Set<Group> playerGroups = new HashSet<>();

		for (final Group group : Settings.Groups.LOADED_GROUPS)
			if (Common.hasPermission(player, "chatcontrol.group." + group.name)) {
				Common.debug("Adding " + player.getName() + " to group " + group.name);
				playerGroups.add(group);
			}

		return new ArrayList<>(playerGroups);
	}
}