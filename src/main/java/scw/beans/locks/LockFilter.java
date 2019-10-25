package scw.beans.locks;

import java.lang.reflect.Method;

import scw.beans.annotation.LockConfig;
import scw.beans.annotation.LockParameter;
import scw.core.aop.Filter;
import scw.core.aop.FilterChain;
import scw.core.aop.Invoker;
import scw.core.parameter.ContainAnnotationParameterConfig;
import scw.core.parameter.ParameterUtils;
import scw.core.reflect.AnnotationUtils;
import scw.json.JSONUtils;
import scw.locks.Lock;
import scw.locks.LockFactory;
import scw.transaction.sql.cache.QueryCacheUtils;

/**
 * 实现方法级别的分布式锁
 * 
 * @author shuchaowen
 *
 */
public final class LockFilter implements Filter {
	private LockFactory lockFactory;

	public LockFilter(LockFactory lockFactory) {
		this(lockFactory, "");
	}

	public LockFilter(LockFactory lockFactory, String keyPrefix) {
		this.lockFactory = lockFactory;
	}

	public Object filter(Invoker invoker, Object proxy, Method method, Object[] args, FilterChain filterChain)
			throws Throwable {
		LockConfig lockConfig = AnnotationUtils.getAnnotation(LockConfig.class, method.getDeclaringClass(), method);
		if (lockConfig == null) {
			return filterChain.doFilter(invoker, proxy, method, args);
		}

		StringBuilder sb = new StringBuilder(128);
		sb.append(method.toString());
		ContainAnnotationParameterConfig[] configs = ParameterUtils.getParameterConfigs(method);
		for (int i = 0; i < configs.length; i++) {
			ContainAnnotationParameterConfig config = configs[i];
			LockParameter lockParameter = config.getAnnotation(LockParameter.class);
			if (lockConfig.all() && (lockParameter != null && lockParameter.value())) {
				sb.append(i == 0 ? "?" : "&");
				sb.append(config.getName());
				sb.append("=");
				sb.append(JSONUtils.toJSONString(args[i]));
			}
		}

		String lockKey = sb.toString();
		Lock lock = lockFactory.getLock(lockKey);
		try {
			if (lockConfig.isWait()) {
				lock.lock();
			} else if (!lock.tryLock()) {
				throw new HasBeenLockedException(lockKey);
			}

			QueryCacheUtils.setQueryCacheEnable(false);
			return filterChain.doFilter(invoker, proxy, method, args);
		} finally {
			lock.unlock();
		}
	}
}