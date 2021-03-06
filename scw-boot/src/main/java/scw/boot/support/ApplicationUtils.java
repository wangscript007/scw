package scw.boot.support;

import java.util.LinkedHashSet;
import java.util.Set;

import scw.boot.Application;
import scw.boot.annotation.BasePackage;
import scw.core.utils.StringUtils;
import scw.env.ConfigurableEnvironment;
import scw.env.Environment;
import scw.instance.InstanceUtils;
import scw.io.resolver.PropertiesResolver;
import scw.lang.Nullable;
import scw.util.concurrent.ListenableFuture;
import scw.value.Value;

public final class ApplicationUtils {
	private static final String APPLICATION_PREFIX = "application";
	private static final PropertiesResolver YAML_PROPERTIES_RESOLVER = InstanceUtils.INSTANCE_FACTORY
			.getInstance("scw.yaml.YamlPropertiesResolver");

	public static void config(ConfigurableEnvironment environment) {
		environment.loadProperties(APPLICATION_PREFIX + ".properties")
				.register();

		if (YAML_PROPERTIES_RESOLVER != null) {
			environment.addPropertiesResolver(YAML_PROPERTIES_RESOLVER);
			environment.loadProperties(APPLICATION_PREFIX + ".yaml").register();
		}
	}

	public static String getBasePackage(Class<?> mainClass) {
		BasePackage basePackage = mainClass.getAnnotation(BasePackage.class);
		if (basePackage == null) {
			return mainClass.getPackage().getName();
		} else {
			return basePackage.value();
		}
	}

	public static String getApplicatoinName(Environment environment) {
		return environment.getString("application.name");
	}

	/**
	 * 默认为8080端口
	 * 
	 * @param application
	 * @return
	 */
	public static int getApplicationPort(Application application) {
		return getApplicationPort(application, 8080);
	}

	public static int getApplicationPort(Application application,
			int defaultPort) {
		if (application instanceof MainApplication) {
			Value value = ((MainApplication) application).getMainArgs()
					.getNextValue("-p");
			if (value != null && value.isNumber()) {
				return value.getAsIntValue();
			}
		}

		// tomcat.port兼容老版本
		int port = application.getEnvironment().getValue("tomcat.port",
				int.class, defaultPort);
		port = application.getEnvironment().getValue("port", int.class, port);
		return application.getEnvironment().getValue("server.port", int.class,
				port);
	}

	public static Set<Class<?>> getContextClasses(Application application) {
		return new LinkedHashSet<Class<?>>(InstanceUtils.asList(application
				.getContextClassesLoader()));
	}

	public static <T extends Application> ListenableFuture<T> run(final T application, @Nullable String threadName) {
		String threadNameToUse = StringUtils.hasText(threadName)? threadName:application.getClass().getSimpleName();
		Thread shutdown = new Thread(new Runnable() {

			public void run() {
				if (application.isInitialized()) {
					try {
						application.destroy();
					} catch (Throwable e) {
						application.getLogger().error(e, "destroy error");
					}
				}
			}
		}, threadNameToUse + "-shutdown");
		Runtime.getRuntime().addShutdownHook(shutdown);

		ApplicationRunnable<T> runnable = new ApplicationRunnable<T>(
				application);
		Thread run = new Thread(runnable);
		run.setContextClassLoader(application.getClassLoader());
		run.setName(threadNameToUse);
		run.setDaemon(false);
		run.start();
		return runnable;
	}
}
