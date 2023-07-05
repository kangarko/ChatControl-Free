package org.mineacademy.chatcontrol.util;

import java.util.IllegalFormatException;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import lombok.Getter;
import lombok.Setter;

/**
 * A workaround class for Tekkit Classic (1.2.5) where async player chat event is not available.
 */
@Getter
@Setter
public final class CompatPlayerChatEvent implements Cancellable {

	private final Player player;
	private boolean cancelled = false;
	private String message;
	private String format;
	private final Set<Player> recipients;

	public CompatPlayerChatEvent(Event event) {
		final Class<?> eventClass = event.getClass();

		try {
			player = (Player) eventClass.getMethod("getPlayer").invoke(event);
			message = (String) eventClass.getMethod("getMessage").invoke(event);
			format = (String) eventClass.getMethod("getFormat").invoke(event);
			recipients = (Set<Player>) eventClass.getMethod("getRecipients").invoke(event);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public void install(Event event) {

		if (cancelled)
			((Cancellable) event).setCancelled(true);

		final Class<?> eventClass = event.getClass();

		try {
			eventClass.getMethod("setMessage", String.class).invoke(event, message);
			eventClass.getMethod("setFormat", String.class).invoke(event, format);

		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}

	}

	public void setFormat(final String format) throws IllegalFormatException, NullPointerException {
		// Oh for a better way to do this!
		try {
			String.format(format, player, message);
		} catch (final RuntimeException ex) {
			ex.fillInStackTrace();
			throw ex;
		}

		this.format = format;
	}
}