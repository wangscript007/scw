package scw.util.task;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import scw.beans.BeanDefinition;
import scw.beans.DefaultBeanDefinition;
import scw.beans.builder.BeanBuilderLoader;
import scw.beans.builder.BeanBuilderLoaderChain;
import scw.beans.builder.LoaderContext;
import scw.core.GlobalPropertyFactory;
import scw.core.instance.annotation.Configuration;
import scw.core.utils.StringUtils;

@Configuration(order = Integer.MIN_VALUE)
public class ExecutorBeanBuilderLoader implements BeanBuilderLoader {
	private static final int DEFAULT_CORE_POOL_SIZE = StringUtils
			.parseInt(GlobalPropertyFactory.getInstance().getString("executor.pool.core.size"), 16);
	private static final int DEFAULT_MAXMUM_POOL_SIZE = StringUtils
			.parseInt(GlobalPropertyFactory.getInstance().getString("executor.pool.max.size"), 512);
	private static final long DEFAULT_KEEP_ALIVE_TIME = StringUtils
			.parseLong(GlobalPropertyFactory.getInstance().getString("executor.pool.keepAliveTime"), 1);
	private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.valueOf(
			StringUtils.toString(GlobalPropertyFactory.getInstance().getString("executor.pool.keepAliveTime.unit"),
					TimeUnit.HOURS.name()).toUpperCase());

	public BeanDefinition loading(LoaderContext context, BeanBuilderLoaderChain loaderChain) {
		if (context.getTargetClass().isAssignableFrom(ThreadPoolExecutor.class)) {
			return new ThreadPoolExecutorBeanBuilder(context);
		} else if (ScheduledExecutorService.class == context.getTargetClass()) {
			return new ScheduledExecutorServiceBeanBuilder(context);
		}
		return loaderChain.loading(context);
	}

	private final class ScheduledExecutorServiceBeanBuilder extends DefaultBeanDefinition {

		public ScheduledExecutorServiceBeanBuilder(LoaderContext context) {
			super(context);
		}

		public boolean isInstance() {
			return true;
		}

		public Object create() throws Exception {
			return Executors.newScheduledThreadPool(DEFAULT_CORE_POOL_SIZE);
		}

		@Override
		public void destroy(Object instance) throws Exception {
			if (instance instanceof ScheduledExecutorService) {
				((ScheduledExecutorService) instance).shutdownNow();
			}
			super.destroy(instance);
		}
	}

	private final class ThreadPoolExecutorBeanBuilder extends DefaultBeanDefinition {
		public ThreadPoolExecutorBeanBuilder(LoaderContext context) {
			super(context);
		}

		@Override
		public boolean isInstance() {
			return true;
		}

		@Override
		protected boolean isProxy() {
			return false;
		}

		@Override
		public Object create() throws Exception {
			return new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXMUM_POOL_SIZE, DEFAULT_KEEP_ALIVE_TIME,
					DEFAULT_TIME_UNIT, new LinkedBlockingQueue<Runnable>());
		}

		@Override
		public void destroy(Object instance) throws Exception {
			if (instance instanceof ThreadPoolExecutor) {
				((ThreadPoolExecutor) instance).shutdownNow();
			}
			super.destroy(instance);
		}
	}
}
