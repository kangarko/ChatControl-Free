package org.mineacademy.chatcontrol;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.chatcontrol.group.Group;
import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;

public class PlayerCache {

	public String lastMessage = "";
	public String lastCommand = "";

	public String lastSignText = "";

	public long lastMessageTime = 0;
	public long lastCommandTime = 0;

	public Location loginLocation = null;
	public long lastLogin = 0;

	public List<Group> groups = null;

	// Reason for this: I cannot get the player instance from PreLoginEvent.
	public void onCall(Player pl) {
		assignGroups(pl);
	}

	private void assignGroups(Player pl) {
		if (!Settings.Groups.ENABLED)
			return;

		if (groups == null || Settings.Groups.ALWAYS_CHECK_UPDATES) {
			Common.Debug("&bLoading group for &f" + pl.getName() + "&b ...");
			groups = Group.loadFor(pl);
		}
	}
}