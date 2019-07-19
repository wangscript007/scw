package scw.logger.console;

import scw.logger.Logger;

public final class ConsoleLoggerFactory extends AbstractLoggerFactory {

	public Logger getLogger(String name) {
		return new ConsoleLogger(true, true, true, true, true, name, this);
	}
}