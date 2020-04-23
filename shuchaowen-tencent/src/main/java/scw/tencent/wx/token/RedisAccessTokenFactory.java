package scw.tencent.wx.token;

import scw.core.parameter.annotation.ParameterName;
import scw.data.redis.Redis;
import scw.locks.Lock;
import scw.locks.LockFactory;
import scw.locks.RedisLockFactory;
import scw.tencent.wx.AccessToken;
import scw.tencent.wx.WeiXinUtils;

public final class RedisAccessTokenFactory extends AbstractAccessTokenFactory {
	private final Redis redis;
	private final String key;
	private final String lockKey;
	private final LockFactory lockFactory;

	public RedisAccessTokenFactory(Redis redis, @ParameterName(WX_APPID_KEY) String appid,
			@ParameterName(WX_APPSECRET_KEY) String appsecret) {
		super(appid, appsecret);
		this.redis = redis;
		this.key = "wx_access_token:#" + getAppId();
		this.lockKey = "wx_access_token:#lock#" + getAppSecret();
		this.lockFactory = new RedisLockFactory(redis);
	}

	@Override
	protected AccessToken getAccessTokenByCache() {
		return (AccessToken) redis.getObjectOperations().get(key);
	}

	@Override
	protected AccessToken refreshToken() {
		if (!isExpires()) {
			return getAccessTokenByCache();
		}

		Lock lock = lockFactory.getLock(lockKey);
		if (lock.tryLock()) {
			try {
				if (isExpires()) {
					AccessToken accessToken = WeiXinUtils.getAccessToken(getAppId(), getAppSecret());
					if (accessToken.isSuccess()) {
						redis.getObjectOperations().setex(key, accessToken.getExpires_in(), accessToken);
						return accessToken;
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
		return refreshToken();
	}
}
