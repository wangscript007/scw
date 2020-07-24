package scw.application;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import scw.core.GlobalPropertyFactory;
import scw.core.instance.InstanceUtils;
import scw.core.reflect.ReflectionUtils;
import scw.core.utils.CollectionUtils;
import scw.logger.Logger;
import scw.logger.LoggerUtils;
import scw.logger.SplitLineAppend;

public class MainApplication extends CommonApplication implements Application, Runnable {
	private final Logger logger;
	private final Class<?> mainClass;
	private final MainArgs args;

	public MainApplication(Class<?> mainClass, String[] args) {
		super(DEFAULT_BEANS_PATH);
		this.mainClass = mainClass;
		this.args = new MainArgs(args);

		BasePackage basePackage = mainClass.getAnnotation(BasePackage.class);
		if (basePackage == null) {
			Package p = mainClass.getPackage();
			if (p != null) {
				GlobalPropertyFactory.getInstance().setBasePackageName(p.getName());
			}
		} else {
			GlobalPropertyFactory.getInstance().setBasePackageName(basePackage.value());
		}

		for (Entry<String, String> entry : this.args.getParameterMap().entrySet()) {
			getPropertyFactory().put(entry.getKey(), entry.getValue());
		}

		this.logger = LoggerUtils.getLogger(mainClass);
		if (args != null) {
			logger.debug("args: {}", this.args);
			addInternalSingleton(MainArgs.class, this.args);
		}
	}

	public Class<?> getMainClass() {
		return mainClass;
	}

	public MainArgs getArgs() {
		return args;
	}

	public final Logger getLogger() {
		return logger;
	}

	@Override
	protected void initInternal() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					MainApplication.this.destroy();
				} catch (Exception e) {
					logger.error(e, "destroy error");
				}
			}
		});
		super.initInternal();
	}

	@Override
	protected void destroyInternal() {
		logger.info(new SplitLineAppend("destroy"));
		super.destroyInternal();
	}

	public void run() {
		init();
		while (true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public static void run(MainApplication application) {
		Thread run = new Thread(application);
		run.setContextClassLoader(application.getMainClass().getClassLoader());
		run.setName(application.getMainClass().getName());
		run.setDaemon(false);
		run.start();
	}

	public static MainApplication getAutoMainApplicationImpl(Class<?> mainClass, String[] args)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Collection<Class<MainApplication>> impls = InstanceUtils.getConfigurationClassList(MainApplication.class,
				GlobalPropertyFactory.getInstance());
		if (!CollectionUtils.isEmpty(impls)) {
			Iterator<Class<MainApplication>> iterator = impls.iterator();
			while (iterator.hasNext()) {
				Constructor<MainApplication> constructor = ReflectionUtils.findConstructor(iterator.next(), false,
						Class.class, String[].class);
				if (constructor != null) {
					ReflectionUtils.makeAccessible(constructor);
					return constructor.newInstance(mainClass, args);
				}
			}
		}
		return null;
	}

	public static void run(Class<?> mainClass, String[] args) {
		MainApplication application;
		try {
			application = getAutoMainApplicationImpl(mainClass, args);
		} catch (Exception e) {
			throw new ApplicationException("获取MainApplication实现异常", e);
		}

		if (application == null) {
			application = new MainApplication(mainClass, args);
		}

		application.getLogger().info("use application: {}", application.getClass().getName());
		run(application);
	}

	public static void run(Class<?> mainClass) {
		run(mainClass, null);
	}
}
