package scw.logger;

import scw.core.UnsafeStringBuffer;
import scw.core.utils.FormatUtils;
import scw.core.utils.PlaceholderFormatAppend;
import scw.core.utils.StringAppend;

public final class Message implements StringAppend {
	private final long cts;
	private final Level level;
	private final StringAppend msg;
	private final String name;
	private final Throwable throwable;

	public Message(Level level, String name, StringAppend msg, Throwable throwable) {
		this.cts = System.currentTimeMillis();
		this.level = level;
		this.msg = msg;
		this.throwable = throwable;
		this.name = name;
	}

	public Message(Level level, String name, Object msg, Object[] args, Throwable throwable, String placeholder) {
		this.cts = System.currentTimeMillis();
		this.level = level;
		this.throwable = throwable;
		this.name = name;
		this.msg = new PlaceholderFormatAppend(msg, placeholder, args);
	}

	public long getCts() {
		return cts;
	}

	public Level getLevel() {
		return level;
	}

	public StringAppend getMsg() {
		return msg;
	}

	public String getName() {
		return name;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void appendTo(Appendable appendable) throws Exception {
		FormatUtils.loggerAppend(appendable, cts, level.name(), name, msg);
	}

	public String toString(UnsafeStringBuffer unsafeStringBuffer) throws Exception {
		unsafeStringBuffer.reset();
		appendTo(unsafeStringBuffer);
		return unsafeStringBuffer.toString();
	}

	public String toMessage(UnsafeStringBuffer unsafeStringBuffer) throws Exception {
		unsafeStringBuffer.reset();
		msg.appendTo(unsafeStringBuffer);
		return unsafeStringBuffer.toString();
	}
}