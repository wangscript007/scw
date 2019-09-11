package scw.beans;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import scw.beans.annotation.Autowired;
import scw.beans.annotation.Bean;
import scw.beans.annotation.Config;
import scw.beans.annotation.Destroy;
import scw.beans.annotation.InitMethod;
import scw.beans.annotation.Proxy;
import scw.beans.annotation.Service;
import scw.beans.annotation.Value;
import scw.beans.property.ValueWired;
import scw.beans.property.ValueWiredManager;
import scw.beans.xml.XmlBeanParameter;
import scw.core.Init;
import scw.core.PropertyFactory;
import scw.core.aop.Filter;
import scw.core.aop.FilterInvocationHandler;
import scw.core.aop.Invoker;
import scw.core.aop.ProxyUtils;
import scw.core.aop.ReflectInvoker;
import scw.core.cglib.proxy.Enhancer;
import scw.core.exception.BeansException;
import scw.core.reflect.DefaultFieldDefinition;
import scw.core.reflect.FieldDefinition;
import scw.core.reflect.ReflectUtils;
import scw.core.utils.AnnotationUtils;
import scw.core.utils.ArrayUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.CollectionUtils;
import scw.core.utils.ObjectUtils;
import scw.core.utils.StringUtils;
import scw.logger.LoggerUtils;

public final class BeanUtils {
	private BeanUtils() {
	};

	/**
	 * 调用init方法
	 * 
	 * @param beanFactory
	 * @param classList
	 * @throws Exception
	 */
	private static void invokerInitStaticMethod(Collection<Class<?>> classList)
			throws Exception {
		List<ReflectInvoker> list = new ArrayList<ReflectInvoker>();
		for (Class<?> clz : classList) {
			for (Method method : ReflectUtils.getDeclaredMethods(clz)) {
				if (!Modifier.isStatic(method.getModifiers())) {
					continue;
				}

				InitMethod initMethod = method.getAnnotation(InitMethod.class);
				if (initMethod == null) {
					continue;
				}

				if (method.getParameterTypes().length != 0) {
					throw new BeansException("ClassName=" + clz.getName()
							+ ",MethodName=" + method.getName()
							+ "There must be no parameter.");
				}

				ReflectInvoker invoke = new ReflectInvoker(null, method);
				list.add(invoke);
			}
		}

		// 调用指定注解的方法
		CountDownLatch countDownLatch = new CountDownLatch(list.size());
		for (ReflectInvoker info : list) {
			InitProcess process = new InitProcess(info, countDownLatch);
			new Thread(process).start();
		}
		countDownLatch.await();
	}

	public synchronized static void destroyStaticMethod(
			ValueWiredManager valueWiredManager, Collection<Class<?>> classList)
			throws Exception {
		List<ReflectInvoker> list = new ArrayList<ReflectInvoker>();
		for (Class<?> clz : classList) {
			if (valueWiredManager != null) {
				valueWiredManager.cancel(clz);
			}

			for (Method method : ReflectUtils.getDeclaredMethods(clz)) {
				if (!Modifier.isStatic(method.getModifiers())) {
					continue;
				}

				Destroy destroy = method.getAnnotation(Destroy.class);
				if (destroy == null) {
					continue;
				}

				if (method.getParameterTypes().length != 0) {
					throw new RuntimeException("ClassName=" + clz.getName()
							+ ",MethodName=" + method.getName()
							+ "There must be no parameter.");
				}

				ReflectInvoker invoke = new ReflectInvoker(null, method);
				list.add(invoke);
			}
		}

		CountDownLatch countDownLatch = new CountDownLatch(list.size());
		for (ReflectInvoker info : list) {
			InitProcess process = new InitProcess(info, countDownLatch);
			new Thread(process).start();
		}
		countDownLatch.await();
	}

	public synchronized static void initStatic(
			ValueWiredManager valueWiredManager, BeanFactory beanFactory,
			PropertyFactory propertyFactory, Collection<Class<?>> classList)
			throws Exception {
		initAutowriteStatic(valueWiredManager, beanFactory, propertyFactory,
				classList);
		invokerInitStaticMethod(classList);
	}

	public static void autoWrite(ValueWiredManager valueWiredManager,
			BeanFactory beanFactory, PropertyFactory propertyFactory,
			Class<?> clz, Object obj, Collection<FieldDefinition> fields)
			throws Exception {
		for (FieldDefinition field : fields) {
			setBean(beanFactory, clz, obj, field);
			setConfig(beanFactory, clz, obj, field);
		}

		setValue(valueWiredManager, beanFactory, propertyFactory, clz, obj,
				fields);
	}

	/**
	 * 过滤非静态方法和非静态字段
	 * 
	 * @param classList
	 */
	private static void initAutowriteStatic(
			ValueWiredManager valueWiredManager, BeanFactory beanFactory,
			PropertyFactory propertyFactory, Collection<Class<?>> classList)
			throws Exception {
		for (Class<?> clz : classList) {
			autoWrite(valueWiredManager, beanFactory, propertyFactory, clz,
					null, getAutowriteFieldDefinitionList(clz, true));
		}
	}

	private static XmlBeanParameter[] sortParameters(String[] paramNames,
			Type[] parameterTypes, XmlBeanParameter[] beanMethodParameters) {
		XmlBeanParameter[] methodParameters = new XmlBeanParameter[beanMethodParameters.length];
		Type[] types = new Type[methodParameters.length];
		for (int i = 0; i < methodParameters.length; i++) {
			XmlBeanParameter beanMethodParameter = beanMethodParameters[i]
					.clone();
			if (!StringUtils.isNull(beanMethodParameter.getName())) {
				for (int a = 0; a < paramNames.length; a++) {
					if (paramNames[a].equals(beanMethodParameter.getName())) {
						types[a] = parameterTypes[a];
						methodParameters[a] = beanMethodParameters[i].clone();
						methodParameters[a].setParameterType(parameterTypes[a]);
					}
				}
			} else if (beanMethodParameter.getParameterType() != null) {
				methodParameters[i] = beanMethodParameter;
				types[i] = beanMethodParameter.getParameterType();
			} else {
				types[i] = parameterTypes[i];
				methodParameters[i] = beanMethodParameter;
				methodParameters[i].setParameterType(types[i]);
			}
		}

		return ObjectUtils.equals(Arrays.asList(parameterTypes),
				Arrays.asList(types)) ? methodParameters : null;
	}

	/**
	 * 对参数重新排序
	 * 
	 * @param executable
	 * @param beanMethodParameters
	 * @return
	 */
	public static XmlBeanParameter[] sortParameters(Method method,
			XmlBeanParameter[] beanMethodParameters) {
		if (method.getParameterTypes().length != beanMethodParameters.length) {
			return null;
		}

		return sortParameters(ClassUtils.getParameterName(method),
				method.getParameterTypes(), beanMethodParameters);
	}

	public static XmlBeanParameter[] sortParameters(Constructor<?> constructor,
			XmlBeanParameter[] beanMethodParameters) {
		if (constructor.getParameterTypes().length != beanMethodParameters.length) {
			return null;
		}

		return sortParameters(ClassUtils.getParameterName(constructor),
				constructor.getParameterTypes(), beanMethodParameters);
	}

	public static Object[] getBeanMethodParameterArgs(
			XmlBeanParameter[] beanParameters, BeanFactory beanFactory,
			scw.core.PropertyFactory propertyFactory) throws Exception {
		Object[] args = new Object[beanParameters.length];
		for (int i = 0; i < args.length; i++) {
			XmlBeanParameter xmlBeanParameter = beanParameters[i];
			args[i] = xmlBeanParameter.parseValue(beanFactory, propertyFactory);
		}
		return args;
	}

	private static void setConfig(BeanFactory beanFactory, Class<?> clz,
			Object obj, FieldDefinition field) {
		Config config = field.getAnnotation(Config.class);
		if (config != null) {
			staticFieldWarnLog(Config.class.getName(), clz, field);
			Object value = null;
			try {
				existDefaultValueWarnLog(Config.class.getName(), clz, field,
						obj);

				value = beanFactory.getInstance(config.parse()).parse(
						beanFactory, field, config.value(), config.charset());
				field.set(obj, value);
			} catch (Exception e) {
				throw new RuntimeException("config：clz=" + clz.getName()
						+ ",fieldName=" + field.getField().getName(), e);
			}
		}
	}

	private static boolean checkExistDefaultValue(FieldDefinition field,
			Object obj) throws Exception {
		if (field.getField().getType().isPrimitive()) {// 值类型一定是有默认值的,所以不用判断直接所回false
			return false;
		}
		return field.get(obj) != null;
	}

	private static void existDefaultValueWarnLog(String tag, Class<?> clz,
			FieldDefinition field, Object obj) throws Exception {
		if (checkExistDefaultValue(field, obj)) {
			LoggerUtils.warn(BeanUtils.class, tag + " class[" + clz.getName()
					+ "] fieldName[" + field.getField().getName()
					+ "] existence default value");
		}
	}

	private static void staticFieldWarnLog(String tag, Class<?> clz,
			FieldDefinition field) {
		if (Modifier.isStatic(field.getField().getModifiers())) {
			LoggerUtils.warn(BeanUtils.class, tag + " class[" + clz.getName()
					+ "] fieldName[" + field.getField().getName()
					+ "] is a static field");
		}
	}

	public static Object getValueTaskId(Class<?> clazz, Object obj) {
		return obj == null ? clazz : obj;
	}

	public static void setValue(ValueWiredManager valueWiredManager,
			BeanFactory beanFactory, PropertyFactory propertyFactory,
			Class<?> clz, Object obj,
			Collection<FieldDefinition> fieldDefinitions) throws Exception {
		if (CollectionUtils.isEmpty(fieldDefinitions)) {
			return;
		}

		Collection<ValueWired> valueWireds = new LinkedList<ValueWired>();
		for (FieldDefinition field : fieldDefinitions) {
			Value value = field.getAnnotation(Value.class);
			if (value != null) {
				staticFieldWarnLog(Value.class.getName(), clz, field);
				try {
					existDefaultValueWarnLog(Value.class.getName(), clz, field,
							obj);
					ValueWired valueWired = new ValueWired(obj, field, value);
					if (valueWiredManager == null) {
						valueWired.wired(beanFactory, propertyFactory);
					} else {
						valueWireds.add(valueWired);
					}
				} catch (Throwable e) {
					throw new RuntimeException("properties：clz="
							+ clz.getName() + ",fieldName="
							+ field.getField().getName(), e);
				}
			}
		}

		if (valueWiredManager != null) {
			valueWiredManager.write(getValueTaskId(clz, obj), valueWireds);
		}
	}

	private static void setBean(BeanFactory beanFactory, Class<?> clz,
			Object obj, FieldDefinition field) {
		Autowired s = field.getAnnotation(Autowired.class);
		if (s != null) {
			staticFieldWarnLog(Autowired.class.getName(), clz, field);

			String name = s.value();
			if (name.length() == 0) {
				name = field.getField().getType().getName();
			}

			try {
				existDefaultValueWarnLog(Autowired.class.getName(), clz, field,
						obj);
				field.set(obj, beanFactory.getInstance(name));
			} catch (Exception e) {
				throw new RuntimeException("autowrite：clz=" + clz.getName()
						+ ",fieldName=" + field.getField().getName(), e);
			}
		}
	}

	public static Enhancer createEnhancer(Class<?> clz,
			BeanFactory beanFactory, String[] filterNames, Filter lastFilter) {
		Enhancer enhancer = new Enhancer();
		enhancer.setCallback(new RootFilter(beanFactory, filterNames,
				lastFilter));
		if (Serializable.class.isAssignableFrom(clz)) {
			enhancer.setSerialVersionUID(1L);
		}
		enhancer.setSuperclass(clz);
		return enhancer;
	}

	public static boolean checkProxy(Class<?> type, String[] filterNames) {
		if (Modifier.isFinal(type.getModifiers())) {// final修饰的类无法代理
			return false;
		}

		if (Filter.class.isAssignableFrom(type)) {
			return false;
		}

		Proxy proxy = type.getAnnotation(Proxy.class);
		if (proxy != null) {
			return true;
		}

		boolean b = true;
		Bean bean = type.getAnnotation(Bean.class);
		if (bean != null && !bean.proxy()) {
			b = false;
		}
		return b;
	}

	public static Invoker getInvoker(BeanFactory beanFactory, Class<?> clz,
			Method method) {
		if (Modifier.isStatic(method.getModifiers())) {
			return new ReflectInvoker(null, method);
		} else {
			return new ReflectInvoker(beanFactory.getInstance(clz), method);
		}
	}

	public static LinkedList<String> getBeanFilterNameList(Class<?> clz,
			Method method, String[] filterNames) {
		// 把重复的filter过渡
		LinkedList<String> list = new LinkedList<String>();
		if (filterNames != null) {
			for (String name : filterNames) {
				list.addFirst(name);
			}
		}

		scw.beans.annotation.BeanFilter beanFilter = method.getDeclaringClass()
				.getAnnotation(scw.beans.annotation.BeanFilter.class);
		if (beanFilter != null) {
			for (Class<? extends Filter> c : beanFilter.value()) {
				list.add(c.getName());
			}
		}

		beanFilter = method
				.getAnnotation(scw.beans.annotation.BeanFilter.class);
		if (beanFilter != null) {
			for (Class<? extends Filter> c : beanFilter.value()) {
				list.add(c.getName());
			}
		}
		return list;
	}

	public static <T> T proxyInterface(BeanFactory beanFactory,
			Class<T> interfaceClass, String[] filterNames, Filter invocation) {
		Object newProxyInstance = java.lang.reflect.Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass }, new FilterInvocationHandler(
						Arrays.asList(invocation)));
		return (T) proxyInterface(beanFactory, interfaceClass,
				newProxyInstance, filterNames);
	}

	@SuppressWarnings("unchecked")
	public static <T> T proxyInterface(BeanFactory beanFactory,
			Class<T> interfaceClass, Object obj, String[] filterNames) {
		return (T) ProxyUtils.newProxyInstance(obj, interfaceClass,
				new RootFilter(beanFactory, filterNames, null));
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getBeanList(BeanFactory beanFactory,
			Collection<String> beanNameList) {
		if (CollectionUtils.isEmpty(beanNameList)) {
			return Collections.EMPTY_LIST;
		}

		LinkedHashSet<T> set = new LinkedHashSet<T>(beanNameList.size());
		for (String name : beanNameList) {
			if (StringUtils.isEmpty(name)) {
				continue;
			}

			T t = beanFactory.getInstance(name);
			if (t == null) {
				continue;
			}

			set.add(t);
		}
		return new ArrayList<T>(set);
	}

	public static LinkedList<FieldDefinition> getAutowriteFieldDefinitionList(
			Class<?> clazz, boolean isStatic) {
		Class<?> clz = clazz;
		LinkedList<FieldDefinition> list = new LinkedList<FieldDefinition>();
		while (clz != null && clz != Object.class) {
			for (final Field field : ReflectUtils.getDeclaredFields(clz)) {
				if (AnnotationUtils.isDeprecated(field)) {
					continue;
				}

				Autowired autowired = field.getAnnotation(Autowired.class);
				Config config = field.getAnnotation(Config.class);
				Value value = field.getAnnotation(Value.class);
				if (autowired == null && config == null && value == null) {
					continue;
				}

				field.setAccessible(true);
				if (isStatic) {
					if (Modifier.isStatic(field.getModifiers())) {
						list.add(new DefaultFieldDefinition(clz, field, false,
								false, true, true));
					}
				} else {
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					}

					list.add(new DefaultFieldDefinition(clz, field, false,
							false, true, true));
				}
			}

			clz = clz.getSuperclass();
		}
		return list;
	}

	public static String getAnnotationPackage(PropertyFactory propertyFactory) {
		return propertyFactory.getProperty("scan.package");
	}

	public static String getORMPackage(PropertyFactory propertyFactory) {
		return propertyFactory.getProperty("scan.orm");
	}

	public static String getServiceAnnotationPackage(
			PropertyFactory propertyFactory) {
		return propertyFactory.getProperty("scan.service");
	}

	public static String getCrontabAnnotationPackage(
			PropertyFactory propertyFactory) {
		return propertyFactory.getProperty("scan.crontab");
	}

	public static String getConsumerAnnotationPackage(
			PropertyFactory propertyFactory) {
		return propertyFactory.getProperty("scan.consumer");
	}

	public static String getInitStaticPackage(PropertyFactory propertyFactory) {
		return propertyFactory.getProperty("scan.static");
	}

	public static List<NoArgumentBeanMethod> getInitMethodList(Class<?> type) {
		List<NoArgumentBeanMethod> list = new ArrayList<NoArgumentBeanMethod>();
		for (Method method : AnnotationUtils.getAnnoationMethods(type, true,
				true, InitMethod.class)) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			method.setAccessible(true);
			list.add(new NoArgumentBeanMethod(method));
		}
		return list;
	}

	public static List<NoArgumentBeanMethod> getDestroyMethdoList(Class<?> type) {
		List<NoArgumentBeanMethod> list = new ArrayList<NoArgumentBeanMethod>();
		for (Method method : AnnotationUtils.getAnnoationMethods(type, true,
				true, Destroy.class)) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			method.setAccessible(true);
			list.add(new NoArgumentBeanMethod(method));
		}
		return list;
	}

	public static String[] getServiceNames(Class<?> clz) {
		Service service = clz.getAnnotation(Service.class);
		if (service != null && !ArrayUtils.isEmpty(service.value())) {
			return service.value();
		}

		HashSet<String> list = new HashSet<String>();
		Class<?>[] clzs = clz.getInterfaces();
		if (clzs != null) {
			for (Class<?> i : clzs) {
				if (i.getName().startsWith("java.")
						|| i.getName().startsWith("javax.")
						|| i == scw.core.Destroy.class || i == Init.class) {
					continue;
				}

				list.add(i.getName());
			}
		}
		return list.isEmpty() ? null : list.toArray(new String[list.size()]);
	}
}

class InitProcess implements Runnable {
	private ReflectInvoker invoke;
	private CountDownLatch countDown;

	public InitProcess(ReflectInvoker invoke, CountDownLatch countDownLatch) {
		this.invoke = invoke;
		this.countDown = countDownLatch;
	}

	public void run() {
		try {
			invoke.invoke();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		countDown.countDown();
	}
}
