/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Minimal error/warning output on top of java.util.logging. Messages go to
 * stderr as a single plain line, like the former System.err output.
 */
public final class Log {

	private static final Logger LOGGER = createLogger();

	private Log() {
	}

	private static Logger createLogger() {
		Logger logger = Logger.getLogger("unitth");
		logger.setUseParentHandlers(false);
		Handler handler = new ConsoleHandler();
		handler.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord r) {
				return formatMessage(r) + System.lineSeparator();
			}
		});
		logger.addHandler(handler);
		return logger;
	}

	public static void error(String message) {
		LOGGER.log(Level.SEVERE, message);
	}

	public static void error(String message, Throwable t) {
		LOGGER.log(Level.SEVERE, () -> message + ": " + t);
	}

	public static void warn(String message) {
		LOGGER.log(Level.WARNING, message);
	}

	/** User-facing command line output (usage, progress, result) on stdout. */
	public static void out(String message) {
		System.out.println(message); // NOSONAR stdout is the CLI's output channel, not logging
	}
}
