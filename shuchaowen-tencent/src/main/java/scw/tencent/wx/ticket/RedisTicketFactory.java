package scw.tencent.wx.ticket;

import scw.core.parameter.annotation.ParameterName;
import scw.data.redis.Redis;
import scw.locks.Lock;
import scw.locks.LockFactory;
import scw.locks.RedisLockFactory;
import scw.tencent.wx.Ticket;
import scw.tencent.wx.WeiXinUtils;
import scw.tencent.wx.token.AccessTokenFactory;
import scw.tencent.wx.token.RedisAccessTokenFactory;

public final class RedisTicketFactory extends AbstractTicketFactory {
	private final Redis redis;
	private final String key;
	private final String lockKey;
	private final LockFactory lockFactory;

	public RedisTicketFactory(Redis redis, @ParameterName(WX_APPID_KEY) String appid,
			@ParameterName(WX_APPSECRET_KEY) String appsecret) {
		this(redis, appid, appsecret, "jsapi");
	}

	public RedisTicketFactory(Redis redis, @ParameterName(WX_APPID_KEY) String appid,
			@ParameterName(WX_APPSECRET_KEY) String appsecret, @ParameterName(WX_TICKET_TYPE) String type) {
		this(redis, appid, new RedisAccessTokenFactory(redis, appid, appsecret), type);
	}

	public RedisTicketFactory(Redis redis, String key, AccessTokenFactory accessTokenFactory) {
		this(redis, key, accessTokenFactory, "jsapi");
	}

	public RedisTicketFactory(Redis redis, String key, AccessTokenFactory accessTokenFactory, String type) {
		super(accessTokenFactory, type);
		this.redis = redis;
		this.key = "wx_ticket:#" + type + "#" + key;
		this.lockKey = "wx_ticket:#lock#" + type + "#" + key;
		this.lockFactory = new RedisLockFactory(redis);
	}

	@Override
	protected Ticket getJsApiTicketByCache() {
		return (Ticket) redis.getObjectOperations().get(key);
	}

	@Override
	protected Ticket refreshJsApiTicket() {
		if (!isExpires()) {
			return getJsApiTicketByCache();
		}

		Lock lock = lockFactory.getLock(lockKey);
		if (lock.tryLock()) {
			try {
				if (isExpires()) {
					Ticket ticket = WeiXinUtils.getTicket(getAccessToken(), getType());
					if (ticket.isSuccess()) {
						redis.getObjectOperations().setex(key, ticket.getExpires_in(), ticket);
						return ticket;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}

		// 没有拿到锁
		try {
			Thread.sleep(50L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return refreshJsApiTicket();
	}
}
