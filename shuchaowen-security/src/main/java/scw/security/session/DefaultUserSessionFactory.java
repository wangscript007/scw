package scw.security.session;

import scw.core.annotation.Order;
import scw.core.parameter.annotation.DefaultValue;
import scw.core.parameter.annotation.ParameterName;
import scw.data.TemporaryCache;
import scw.data.memcached.Memcached;
import scw.data.redis.Redis;
import scw.data.redis.RedisDataTemplete;

public final class DefaultUserSessionFactory<T> extends AbstractUserSessionFactory<T> {
	private TemporaryCache temporaryCache;
	private SessionFactory sessionFactory;

	@Order
	public DefaultUserSessionFactory(
			@ParameterName("user.session.timeout") @DefaultValue((86400 * 7) + "") int defaultMaxInactiveInterval,
			TemporaryCache temporaryCache) {
		this.sessionFactory = new DefaultSessionFactory(defaultMaxInactiveInterval, temporaryCache);
		this.temporaryCache = temporaryCache;
	}

	public DefaultUserSessionFactory(int defaultMaxInactiveInterval, Memcached memcached) {
		this(defaultMaxInactiveInterval, (TemporaryCache) memcached);
	}

	public DefaultUserSessionFactory(int defaultMaxInactiveInterval, Redis redis) {
		this(defaultMaxInactiveInterval, new RedisDataTemplete(redis));
	}

	@Override
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	protected String getKey(String key) {
		return "user-session-factory:" + key;
	}

	@Override
	protected void invalidate(T uid) {
		String sessionId = getSessionId(uid);
		if (sessionId != null) {
			temporaryCache.delete(getKey(sessionId));
		}
		temporaryCache.delete(getKey(uid.toString()));
	}

	@Override
	protected void touch(T uid, Session session) {
		temporaryCache.touch(getKey(uid.toString()), session.getMaxInactiveInterval());
		temporaryCache.touch(getKey(session.getId()), session.getMaxInactiveInterval());
	}

	@Override
	protected void addUidMapping(T uid, Session session) {
		temporaryCache.set(getKey(uid.toString()), session.getMaxInactiveInterval(), session.getId());
		temporaryCache.set(getKey(session.getId()), session.getMaxInactiveInterval(), uid);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T getUid(String sessionId) {
		return (T) temporaryCache.get(getKey(sessionId));
	}

	@Override
	protected String getSessionId(T uid) {
		return (String) temporaryCache.get(getKey(uid.toString()));
	}
}
