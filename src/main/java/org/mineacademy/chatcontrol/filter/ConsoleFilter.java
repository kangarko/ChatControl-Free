package org.mineacademy.chatcontrol.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;

public final class ConsoleFilter implements Filter {

	@Override
	public boolean isLoggable(LogRecord record) {
		final String message = record.getMessage();

		if (message == null || message.isEmpty())
			return false;

		for (final String ignored : Settings.Console.FILTER_MESSAGES)
			if (message.equalsIgnoreCase(ignored) || message.toLowerCase().contains(ignored.toLowerCase()))
				return false;
			else if (Common.isRegexMatch(ignored, message))
				return false;

		return true;
	}
}