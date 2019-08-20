package scw.servlet.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;

import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.beans.annotation.Destroy;
import scw.beans.annotation.InitMethod;
import scw.beans.property.ValueWiredManager;
import scw.core.Init;
import scw.core.PropertyFactory;
import scw.core.cglib.proxy.Enhancer;
import scw.core.exception.NotFoundException;
import scw.core.reflect.FieldDefinition;
import scw.core.reflect.ReflectUtils;
import scw.core.utils.ClassUtils;

public final class AnnotationRequestBean implements RequestBean {
	private final BeanFactory beanFactory;
	private final Class<?> type;
	private final String id;
	private final Method[] initMethods;
	private final Method[] destroyMethods;
	private final boolean proxy;
	private Enhancer enhancer;
	private final PropertyFactory propertyFactory;
	private final FieldDefinition[] autowriteFields;
	private Constructor<?> constructor;
	private int parameterLength;
	private final ValueWiredManager valueWiredManager;

	public AnnotationRequestBean(ValueWiredManager valueWiredManager, BeanFactory beanFactory,
			PropertyFactory propertyFactory, Class<?> type, String[] filterNames) throws Exception {
		this.valueWiredManager = valueWiredManager;
		this.beanFactory = beanFactory;
		this.type = type;
		this.propertyFactory = propertyFactory;
		this.id = ClassUtils.getProxyRealClassName(type);

		List<Method> initMethodList = new ArrayList<Method>();
		List<Method> destroyMethodList = new ArrayList<Method>();
		Class<?> tempClz = type;
		for (Method method : tempClz.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			InitMethod initMethod = method.getAnnotation(InitMethod.class);
			if (initMethod != null) {
				method.setAccessible(true);
				initMethodList.add(method);
			}

			Destroy destroy = method.getAnnotation(Destroy.class);
			if (destroy != null) {
				method.setAccessible(true);
				destroyMethodList.add(method);
			}
		}
		this.initMethods = initMethodList.toArray(new Method[initMethodList.size()]);
		this.destroyMethods = destroyMethodList.toArray(new Method[destroyMethodList.size()]);

		this.constructor = getConstructor(type);
		if (this.constructor == null) {
			throw new NotFoundException(type.getName() + "找不到合法的构造方法");
		}

		this.parameterLength = constructor.getParameterTypes().length;

		this.proxy = BeanUtils.checkProxy(type, filterNames);
		enhancer = BeanUtils.createEnhancer(type, beanFactory, filterNames);
		this.autowriteFields = BeanUtils.getAutowriteFieldDefinitionList(type, false).toArray(new FieldDefinition[0]);
	}

	public static Constructor<?> getConstructor(Class<?> type) {
		return ReflectUtils.findConstructor(type, false, ServletRequest.class);
	}

	public boolean isProxy() {
		return this.proxy;
	}

	public String getId() {
		return this.id;
	}

	public Class<?> getType() {
		return this.type;
	}

	@SuppressWarnings("unchecked")
	public <T> T newInstance(ServletRequest request) {
		Object obj;
		try {
			if (parameterLength == 0) {
				if (isProxy()) {
					obj = enhancer.create();
				} else {
					obj = constructor.newInstance();
				}
			} else {
				if (isProxy()) {
					obj = enhancer.create(constructor.getParameterTypes(), new Object[] { request });
				} else {
					obj = constructor.newInstance(request);
				}
			}
			return (T) obj;
		} catch (Exception e) {
			throw new RuntimeException(type.getName());
		}
	}

	public void autowrite(Object bean) throws Exception {
		for (FieldDefinition definition : autowriteFields) {
			BeanUtils.autoWrite(valueWiredManager, beanFactory, propertyFactory, type, bean, definition);
		}
	}

	public void init(Object bean) throws Exception {
		if (initMethods.length != 0) {
			for (Method method : initMethods) {
				method.invoke(bean);
			}
		}
		
		if(bean instanceof Init){
			((Init) bean).init();
		}
	}

	public void destroy(Object bean) {
		valueWiredManager.cancel(bean);
		if (destroyMethods.length != 0) {
			for (Method method : destroyMethods) {
				try {
					method.invoke(bean);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

		if (bean instanceof scw.core.Destroy) {
			((scw.core.Destroy) bean).destroy();
		}
	}

	public String[] getNames() {
		return null;
	}
}
