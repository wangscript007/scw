package scw.locks;

import java.util.Collections;

import scw.data.redis.Redis;
import scw.data.redis.enums.EXPX;
import scw.data.redis.enums.NXXX;

public final class RedisLock extends AbstractLock {
	private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
	private static final Long UNLOCK_SUCCESS_RESULT = 1L;
	private final Redis redis;
	private final String key;
	private final int timeout;
	private final String id;

	public RedisLock(Redis redis, String key, String id, int timeout) {
		this.redis = redis;
		this.key = key;
		this.timeout = timeout;
		this.id = id;
	}

	public boolean tryLock() {
		return redis.getStringOperations().set(key, id, NXXX.NX, EXPX.EX, timeout);
	}

	public void unlock() {
		Object result = redis.getStringOperations().eval(UNLOCK_SCRIPT, Collections.singletonList(key),
				Collections.singletonList(id));
		UNLOCK_SUCCESS_RESULT.equals(result);
	}
}