package scw.locks;

import java.util.concurrent.TimeUnit;

import scw.env.SystemEnvironment;

public abstract class AbstractLockFactory implements LockFactory {
	private static final long DEFAULT_TIMEOUT = SystemEnvironment
			.getInstance().getValue("locks.default.timeout", Long.class, 60L);

	public Lock getLock(String name) {
		return getLock(name, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
	}

}
