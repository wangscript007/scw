package scw.data.cache;

import scw.data.cache.memory.MemoryCache;

public abstract class AbstractMemoryCache implements MemoryCache {
	private volatile long lastTouch;
	private volatile int exp;

	public AbstractMemoryCache() {
		this.lastTouch = System.currentTimeMillis();
	}

	public void setExpire(int exp) {
		this.exp = exp;
	}

	public void touch() {
		this.lastTouch = System.currentTimeMillis();
	}

	public boolean isExpire(long currentTimeMillis) {
		return exp <= 0 ? false : (currentTimeMillis - lastTouch) > exp * 1000;
	}

	public boolean setIfAbsent(Object value) {
		if (!isExpire(System.currentTimeMillis())) {
			return false;
		}

		set(value);
		return true;
	}
}
