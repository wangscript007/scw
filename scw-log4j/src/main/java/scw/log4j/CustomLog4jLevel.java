package scw.log4j;

import org.apache.log4j.net.SyslogAppender;

import scw.logger.Level;

public class CustomLog4jLevel extends org.apache.log4j.Level {
	private static final long serialVersionUID = 1L;

	public CustomLog4jLevel(Level level) {
		this(level.getValue(), level.getName(), SyslogAppender.LOG_LOCAL0);
	}

	public CustomLog4jLevel(int level, String levelStr, int syslogEquivalent) {
		super(level, levelStr, syslogEquivalent);
	}
}
