package org.mineacademy.chatcontrol.util;

import java.util.IllegalFormatException;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;


public class CompatPlayerChatEvent implements Cancellable {

	private final Player player;
	private boolean cancel = false;
	private String message;
	private String format;

	private final Set<Player> recipients;

	
	public CompatPlayerChatEvent(Event event) {
		final Class<?> cl = event.getClass();

		try {
			player = (Player) cl.getMethod("getPlayer").invoke(event);
			message = (String) cl.getMethod("getMessage").invoke(event);
			format = (String) cl.getMethod("getFormat").invoke(event);
			recipients = (Set<Player>) cl.getMethod("getRecipients").invoke(event);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public void install(Event event) {

		if (cancel)
			((Cancellable) event).setCancelled(true);

		final Class<?> cl = event.getClass();

		try {
			cl.getMethod("setMessage", String.class).invoke(event, message);
			cl.getMethod("setFormat", String.class).invoke(event, format);

		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}

	}

	public Player getPlayer() {
		return player;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getFormat() {
		return format;
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

	public Set<Player> getRecipients() {
		return recipients;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}
}