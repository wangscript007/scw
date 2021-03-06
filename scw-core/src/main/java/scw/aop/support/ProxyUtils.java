package scw.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import scw.aop.MethodInterceptor;
import scw.aop.Proxy;
import scw.aop.ProxyFactory;
import scw.aop.WriteReplaceInterface;
import scw.core.reflect.MethodInvoker;
import scw.core.reflect.ReflectionUtils;
import scw.core.utils.ArrayUtils;

public final class ProxyUtils {
	private ProxyUtils() {
	};

	/**
	 * 代理一个对象并忽略其指定的方法
	 * 
	 * @param clazz
	 * @param instance
	 * @param ignoreMethods
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T proxyIngoreMethod(ProxyFactory proxyFactory, Class<? extends T> clazz, T instance, IgnoreMethodAccept ignoreMethodAccept) {
		MethodInterceptor methodInterceptor = new IgnoreMethodFilter(instance, ignoreMethodAccept);
		Proxy proxy = proxyFactory.getProxy(clazz, new Class<?>[] { IgnoreMethodTarget.class }, methodInterceptor);
		return (T) proxy.create();
	}

	public static interface IgnoreMethodTarget {
		Object getIgnoreMethodTarget();
	}

	private static final class IgnoreMethodFilter implements MethodInterceptor {
		private final Object object;
		private final IgnoreMethodAccept ignoreMethodAccept;

		public IgnoreMethodFilter(Object object, IgnoreMethodAccept ignoreMethodAccept) {
			this.object = object;
			this.ignoreMethodAccept = ignoreMethodAccept;
		}

		public Object intercept(MethodInvoker invoker, Object[] args) throws Throwable {
			if (ArrayUtils.isEmpty(args) && invoker.getMethod().getName().equals("getIgnoreMethodTarget")) {
				return object;
			}

			if (ignoreMethodAccept != null && ignoreMethodAccept.accept(invoker.getMethod())) {
				return null;
			}
			return invoker.invoke(args);
		}
	}

	public static interface IgnoreMethodAccept {
		boolean accept(Method method);
	}

	public static boolean isIgnoreMethod(Method method) {
		return ReflectionUtils.isHashCodeMethod(method) && ReflectionUtils.isToStringMethod(method)
				&& ReflectionUtils.isEqualsMethod(method);
	}

	public static int invokeHashCode(MethodInvoker invoker) {
		return System.identityHashCode(invoker.getInstance());
	}

	public static String invokeToString(MethodInvoker invoker) {
		return invoker.getInstance().getClass().getName() + "@" + Integer.toHexString(invokeHashCode(invoker));
	}

	public static boolean invokeEquals(MethodInvoker invoker, Object[] args) {
		if (args == null || args[0] == null) {
			return false;
		}

		return args[0].equals(invoker.getInstance());
	}

	public static Object invokeIgnoreMethod(MethodInvoker invoker, Object[] args) {
		if (ReflectionUtils.isHashCodeMethod(invoker.getMethod())) {
			return invokeHashCode(invoker);
		}

		if (ReflectionUtils.isToStringMethod(invoker.getMethod())) {
			return invokeToString(invoker);
		}

		if (ReflectionUtils.isEqualsMethod(invoker.getMethod())) {
			return invokeEquals(invoker, args);
		}

		throw new UnsupportedOperationException(invoker.getMethod().toString());
	}

	/**
	 * 是否是ObjectStream中的WriteReplaceMethod
	 * 
	 * @return
	 */
	public static boolean isWriteReplaceMethod(MethodInvoker invoker) {
		return ArrayUtils.isEmpty(invoker.getMethod().getParameterTypes()) && invoker.getInstance() instanceof Serializable
				&& invoker.getMethod().getName().equals(WriteReplaceInterface.WRITE_REPLACE_METHOD);
	}

	/**
	 * 是否是ObjectStream中的WriteReplaceMethod
	 * 
	 * @param writeReplaceInterface
	 *            原始类型是否应该实现{@see WriteReplaceInterface}
	 * @return
	 */
	public static boolean isWriteReplaceMethod(MethodInvoker invoker, boolean writeReplaceInterface) {
		if (isWriteReplaceMethod(invoker)) {
			if (writeReplaceInterface) {
				return WriteReplaceInterface.class.isAssignableFrom(invoker.getSourceClass());
			} else {
				return !WriteReplaceInterface.class.isAssignableFrom(invoker.getSourceClass());
			}
		}
		return false;
	}
}
