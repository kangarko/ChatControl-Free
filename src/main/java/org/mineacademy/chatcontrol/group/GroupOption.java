package org.mineacademy.chatcontrol.group;

import java.util.Arrays;

import org.mineacademy.chatcontrol.settings.ConfHelper.ChatMessage;
import org.mineacademy.chatcontrol.util.Common;

import lombok.Getter;

/**
 * Represents the different options a group can have.
 */
@Getter
public final class GroupOption {

	private final OptionType option;
	private final Object value;

	private GroupOption(OptionType option, Object value) {
		this.option = option;
		this.value = value;
	}

	public enum OptionType {
		MESSAGE_DELAY(Integer.class),
		COMMAND_DELAY(Integer.class),

		JOIN_MESSAGE(ChatMessage.class),
		LEAVE_MESSAGE(ChatMessage.class),
		KICK_MESSAGE(ChatMessage.class);

		private final Class<?> validValue;
		private final String toString;

		OptionType(Class<?> validValue) {
			this.validValue = validValue;
			this.toString = this.makeString();
		}

		public <T> GroupOption create(T valueRaw) {
			Object value = valueRaw;

			if (!value.getClass().isAssignableFrom(this.validValue)) {
				value = null;

				try {
					value = Integer.parseInt(String.valueOf(valueRaw));
				} catch (final Exception ex) {
				}
				try {
					value = Double.parseDouble(String.valueOf(valueRaw));
				} catch (final Exception ex) {
				}
				try {
					value = valueRaw.equals("true") || valueRaw.equals("false") ? Boolean.parseBoolean(String.valueOf(valueRaw)) : null;
				} catch (final Exception ex) {
				}

				if (value == null)
					value = valueRaw;
			}

			this.checkValid(value);

			return new GroupOption(this, this.validValue == ChatMessage.class && valueRaw.getClass() != ChatMessage.class ? new ChatMessage(String.valueOf(value)) : value);
		}

		public static OptionType parseOption(String name) {
			for (final OptionType type : values())
				if (type.name().equalsIgnoreCase(name) || type.toString().equalsIgnoreCase(name))
					return type;

			throw new RuntimeException("Unknown group setting: '" + name + "', use one of these: " + Arrays.asList(values()));
		}

		private void checkValid(Object value) {
			if (this.validValue == ChatMessage.class) {
				// all valid
			} else
				Common.checkBoolean(value.getClass().isAssignableFrom(this.validValue), this + " has to be " + this.validValue.getSimpleName() + "! (got " + value + ")");
		}

		private String makeString() {
			final String[] splitted = this.name().toLowerCase().split("_");
			String nazov = "";

			for (final String part : splitted)
				nazov = nazov + (nazov.isEmpty() ? "" : "_") + part.substring(0, 1).toUpperCase() + part.substring(1);

			return nazov;
		}

		@Override
		public String toString() {
			return this.toString;
		}
	}
}