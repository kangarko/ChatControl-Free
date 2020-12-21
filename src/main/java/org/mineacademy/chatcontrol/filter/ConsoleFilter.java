package org.mineacademy.chatcontrol.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.mineacademy.chatcontrol.settings.Settings;
import org.mineacademy.chatcontrol.util.Common;

public class ConsoleFilter implements Filter {

	@Override
	public boolean isLoggable(LogRecord record) {
		final String msg = record.getMessage();

		if (msg == null || msg.isEmpty())
			return false;

		for (final String ignored : Settings.Console.FILTER_MESSAGES)
			if (msg.equalsIgnoreCase(ignored) || msg.toLowerCase().contains(ignored.toLowerCase()))
				return false;
			else if (Common.regExMatch(ignored, msg))
				return false;

		return true;
	}
}