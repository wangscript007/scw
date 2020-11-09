package scw.core.instance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import scw.compatible.CompatibleUtils;
import scw.compatible.ServiceLoader;
import scw.core.Constants;
import scw.core.GlobalPropertyFactory;
import scw.core.parameter.ParameterUtils;
import scw.core.reflect.ReflectionUtils;
import scw.core.utils.ArrayUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.CollectionUtils;
import scw.lang.NotSupportedException;
import scw.logger.Logger;
import scw.logger.LoggerUtils;
import scw.util.JavaVersion;
import scw.util.MultiEnumeration;
import scw.value.ValueFactory;

@SuppressWarnings("rawtypes")
public final class InstanceUtils {
	private static Logger logger = LoggerUtils.getLogger(InstanceUtils.class);

	private InstanceUtils() {
	};

	public static final ConfigurationScanner CONFIGURATION_SCANNER;

	/**
	 * 默认的实例工厂
	 */
	public static final InstanceFactory INSTANCE_FACTORY = new DefaultInstanceFactory(
			GlobalPropertyFactory.getInstance());

	/**
	 * 不调用构造方法实例化对象
	 */
	public static final NoArgsInstanceFactory NO_ARGS_INSTANCE_FACTORY;

	static {
		ConfigurationScanner configurationScanner = loadService(ConfigurationScanner.class);
		CONFIGURATION_SCANNER = configurationScanner == null ? new ConfigurationScanner() : configurationScanner;

		NoArgsInstanceFactory noArgsInstanceFactory = loadService(NoArgsInstanceFactory.class,
				"scw.core.instance.SunNoArgsInstanceFactory", "scw.core.instance.UnsafeNoArgsInstanceFactory");
		NO_ARGS_INSTANCE_FACTORY = noArgsInstanceFactory;
		if (NO_ARGS_INSTANCE_FACTORY == null) {
			throw new NotSupportedException(NoArgsInstanceFactory.class.getName());
		}
	}

	public static <T> T loadService(Class<? extends T> clazz, String... defaultNames) {
		return loadService(clazz, INSTANCE_FACTORY, GlobalPropertyFactory.getInstance(), defaultNames);
	}

	public static <T> List<T> loadAllService(Class<? extends T> clazz, String... defaultNames) {
		ServiceLoader<T> serviceLoader = getServiceLoader(clazz, INSTANCE_FACTORY, GlobalPropertyFactory.getInstance(),
				defaultNames);
		return Collections.list(CollectionUtils.toEnumeration(serviceLoader.iterator()));
	}

	public static <T> T loadService(Class<? extends T> clazz, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, String... defaultNames) {
		ServiceLoader<T> serviceLoader = getServiceLoader(clazz, instanceFactory, propertyFactory, defaultNames);
		Iterator<T> iterator = serviceLoader.iterator();
		while (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

	public static <S> ServiceLoader<S> getServiceLoader(Class<? extends S> clazz, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, String... defaultNames) {
		return new ConfigurableServiceLoader<S>(CompatibleUtils.getSpi().load(clazz), clazz, instanceFactory,
				propertyFactory, defaultNames);
	}

	public static <T> List<T> loadAllService(Class<? extends T> clazz, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, String... defaultNames) {
		ServiceLoader<T> serviceLoader = getServiceLoader(clazz, instanceFactory, propertyFactory, defaultNames);
		return Collections.list(CollectionUtils.toEnumeration(serviceLoader.iterator()));
	}

	/**
	 * 根据参数名来调用构造方法
	 * 
	 * @param type
	 * @param isPublic
	 * @param parameterMap
	 * @return
	 * @throws NoSuchMethodException
	 */
	public static <T> T newInstance(InstanceFactory instanceFactory, Class<T> type, boolean isPublic,
			Map<String, Object> parameterMap) throws NoSuchMethodException {
		if (CollectionUtils.isEmpty(parameterMap)) {
			try {
				return ReflectionUtils.getConstructor(type, isPublic).newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		int size = parameterMap.size();
		for (Constructor<?> constructor : isPublic ? type.getConstructors() : type.getDeclaredConstructors()) {
			if (size == constructor.getParameterTypes().length) {
				String[] names = ParameterUtils.getParameterNames(constructor);
				Object[] args = new Object[size];
				boolean find = true;
				for (int i = 0; i < names.length; i++) {
					if (!parameterMap.containsKey(names[i])) {
						find = false;
						break;
					}

					args[i] = parameterMap.get(names[i]);
				}

				if (find) {
					return instanceFactory.getInstance(type, constructor.getParameterTypes(), args);
				}
			}
		}
		throw new NoSuchMethodException(type.getName());
	}

	public static <T> Collection<Class<T>> getConfigurationClassList(Class<? extends T> type,
			ValueFactory<String> propertyFactory, Collection<? extends Class> excludeTypes) {
		return getConfigurationClassList(type, excludeTypes,
				Arrays.asList(Constants.SYSTEM_PACKAGE_NAME, getScanAnnotationPackageName(propertyFactory)));
	}

	public static <T> Collection<Class<T>> getConfigurationClassList(Class<? extends T> type,
			ValueFactory<String> propertyFactory, Class... excludeTypes) {
		return getConfigurationClassList(type, propertyFactory, Arrays.asList(excludeTypes));
	}

	public static <T> Collection<Class<T>> getConfigurationClassList(Class<? extends T> type,
			Collection<? extends Class> excludeTypes, Collection<String> packageNames) {
		return CONFIGURATION_SCANNER.scan(type, excludeTypes, packageNames);
	}

	public static <T> List<T> getConfigurationList(Class<? extends T> type, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, Collection<? extends Class> excludeTypes) {
		return getConfigurationList(type, instanceFactory, excludeTypes,
				Arrays.asList(Constants.SYSTEM_PACKAGE_NAME, getScanAnnotationPackageName(propertyFactory)));
	}

	public static <T> List<T> getConfigurationList(Class<? extends T> type, NoArgsInstanceFactory instanceFactory,
			Collection<? extends Class> excludeTypes, Collection<String> packageNames) {
		List<T> list = new ArrayList<T>();
		for (Class<? extends T> clazz : getConfigurationClassList(type, excludeTypes, packageNames)) {
			if (!instanceFactory.isInstance(clazz)) {
				logger.debug("factory [{}] not create {} in instance: {}", instanceFactory.getClass(), type, clazz);
				continue;
			}

			list.add(instanceFactory.getInstance(clazz));
		}
		return list;
	}

	public static <T> List<T> getConfigurationList(Class<? extends T> type, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, Class... excludeTypes) {
		return getConfigurationList(type, instanceFactory, propertyFactory, Arrays.asList(excludeTypes));
	}

	public static String getScanAnnotationPackageName(ValueFactory<String> propertyFactory) {
		return propertyFactory.getValue("scw.scan.annotation.package", String.class,
				GlobalPropertyFactory.getInstance().getBasePackageName());
	}

	public static <T> T getConfiguration(Class<? extends T> type, NoArgsInstanceFactory instanceFactory,
			Collection<? extends Class> excludeTypes, Collection<String> packageNames) {
		for (Class<? extends T> clazz : getConfigurationClassList(type, excludeTypes, packageNames)) {
			if (!instanceFactory.isInstance(clazz)) {
				logger.debug("factory [{}] not create {} in instance: {}", instanceFactory.getClass(), type, clazz);
				continue;
			}

			return instanceFactory.getInstance(clazz);
		}
		return null;
	}

	public static <T> T getConfiguration(Class<? extends T> type, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, Collection<? extends Class> excludeTypes) {
		return getConfiguration(type, instanceFactory, excludeTypes,
				Arrays.asList(Constants.SYSTEM_PACKAGE_NAME, getScanAnnotationPackageName(propertyFactory)));
	}

	public static <T> T getConfiguration(Class<? extends T> type, NoArgsInstanceFactory instanceFactory,
			ValueFactory<String> propertyFactory, Class... excludeTypes) {
		return getConfiguration(type, instanceFactory, propertyFactory, Arrays.asList(excludeTypes));
	}

	public static boolean isSupport(Class<?> clazz) {
		return !ClassUtils.isPrimitiveOrWrapper(clazz) && JavaVersion.isSupported(clazz)
				&& ReflectionUtils.isPresent(clazz);
	}

	private static class ConfigurableServiceLoader<T> implements ServiceLoader<T> {
		private Class<? extends T> clazz;
		private NoArgsInstanceFactory instanceFactory;
		private ValueFactory<String> propertyFactory;
		private String[] configNames;
		private String[] defaultNames;
		private ServiceLoader<? extends T> parentServiceLoader;

		public ConfigurableServiceLoader(ServiceLoader<? extends T> serviceLoader, Class<? extends T> clazz,
				NoArgsInstanceFactory instanceFactory, ValueFactory<String> propertyFactory, String... defaultNames) {
			this.clazz = clazz;
			this.propertyFactory = propertyFactory;
			this.instanceFactory = instanceFactory;
			this.defaultNames = defaultNames;
			this.parentServiceLoader = serviceLoader;

			this.configNames = propertyFactory.getObject(clazz.getName(), String[].class);
		}

		public void reload() {
			if (parentServiceLoader != null) {
				parentServiceLoader.reload();
			}
			this.configNames = propertyFactory.getObject(clazz.getName(), String[].class);
		}

		public Iterator<T> iterator() {
			List<Enumeration<T>> enumerations = new LinkedList<Enumeration<T>>();
			if (!ArrayUtils.isEmpty(configNames)) {
				InstanceIterable<T> instanceIterable = new InstanceIterable<T>(instanceFactory,
						Arrays.asList(configNames));
				enumerations.add(CollectionUtils.toEnumeration(instanceIterable.iterator()));
			}

			if (parentServiceLoader != null) {
				enumerations.add(CollectionUtils.toEnumeration(parentServiceLoader.iterator()));
			}

			if (!ArrayUtils.isEmpty(defaultNames)) {
				InstanceIterable<T> instanceIterable = new InstanceIterable<T>(instanceFactory,
						Arrays.asList(defaultNames));
				enumerations.add(CollectionUtils.toEnumeration(instanceIterable.iterator()));
			}

			Enumeration<T> enumeration = new MultiEnumeration<T>(enumerations);
			return CollectionUtils.toIterator(enumeration);
		}

	}
}
