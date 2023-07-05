package org.mineacademy.chatcontrol;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.chatcontrol.group.Group;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;

/**
 * Represents temporary player data.
 */
public final class PlayerCache {

	public String lastMessage = "";
	public String lastCommand = "";
	public String lastSignText = "";
	public long lastMessageTime = 0;
	public long lastCommandTime = 0;
	public Location loginLocation = null;
	public long lastLogin = 0;
	public List<Group> groups = null;

	public void assignGroups(Player player) {
		if (!Settings.Groups.ENABLED)
			return;

		if (this.groups == null || Settings.Groups.ALWAYS_CHECK_UPDATES) {
			Common.debug("&bLoading group for &f" + player.getName() + "&b ...");

			this.groups = Group.loadFor(player);
		}
	}
}