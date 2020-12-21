package org.mineacademy.chatcontrol.group;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.mineacademy.chatcontrol.settings.ConfHelper.ChatMessage;

public class GroupOption {

	private final OptionType option;
	private final Object value;

	private GroupOption(OptionType option, Object value) {
		this.option = option;
		this.value = value;
	}

	public OptionType getOption() {
		return option;
	}

	public Object getValue() {
		return value;
	}

	public static enum OptionType {
		MESSAGE_DELAY(Integer.class),
		COMMAND_DELAY(Integer.class),

		JOIN_MESSAGE(ChatMessage.class),
		LEAVE_MESSAGE(ChatMessage.class),
		KICK_MESSAGE(ChatMessage.class);

		private final Class<?> validValue;
		private final String toString;

		OptionType(Class<?> validValue) {
			this.validValue = validValue;
			toString = makeString();
		}

		public <T> GroupOption create(T valueRaw) {
			Object value = valueRaw;

			if (!value.getClass().isAssignableFrom(validValue)) {
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

			checkValid(value);

			return new GroupOption(this, validValue == ChatMessage.class && valueRaw.getClass() != ChatMessage.class ? new ChatMessage(String.valueOf(value)) : value);
		}

		public static OptionType parseOption(String name) {
			for (final OptionType type : values())
				if (type.name().equalsIgnoreCase(name) || type.toString().equalsIgnoreCase(name))
					return type;

			throw new RuntimeException("Unknown group setting: '" + name + "', use one of these: " + StringUtils.join(values(), ", "));
		}

		private void checkValid(Object value) {
			if (validValue == ChatMessage.class) {
				// all valid
			} else
				Validate.isTrue(value.getClass().isAssignableFrom(validValue), this + " has to be " + validValue.getSimpleName() + "! (got " + value + ")");
		}

		private String makeString() {
			final String[] splitted = name().toLowerCase().split("_");
			String nazov = "";

			for (final String part : splitted)
				nazov = nazov + (nazov.isEmpty() ? "" : "_") + part.substring(0, 1).toUpperCase() + part.substring(1);

			return nazov;
		}

		@Override
		public String toString() {
			return toString;
		}
	}
}