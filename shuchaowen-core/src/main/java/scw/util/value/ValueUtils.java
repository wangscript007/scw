package scw.util.value;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

import scw.core.utils.ClassUtils;
import scw.core.utils.TypeUtils;

public final class ValueUtils {
	private ValueUtils() {
	};

	public static <K> Object getValue(ValueFactory<K> valueFactory, K key,
			Type type, Object defaultValue) {
		Object v;
		if (TypeUtils.isPrimitive(type)) {
			v = valueFactory.getObject(key,
					ClassUtils.resolvePrimitiveIfNecessary((Class<?>) type));
		} else {
			v = valueFactory.getObject(key, type);
		}

		return v == null ? defaultValue : v;
	}

	public static <K, T> T getValue(ValueFactory<K> valueFactory, K key,
			Class<? extends T> type, T defaultValue) {
		@SuppressWarnings("unchecked")
		T v = (T) valueFactory.getObject(key,
				ClassUtils.resolvePrimitiveIfNecessary(type));
		return v == null ? defaultValue : v;
	}

	public static Object parse(String text, Class<?> type) {
		return new StringValue(text).getAsObject(type);
	}

	public static Object parse(String text, Type type) {
		return new StringValue(text).getAsObject(type);
	}

	public static boolean isCommonType(Type type) {
		if (TypeUtils.isClass(type)) {
			return isCommonType((Class<?>) type);
		}

		try {
			return isCommonType(ClassUtils.forName(type.toString(),
					ClassUtils.getDefaultClassLoader()));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean isCommonType(Class<?> type) {
		return ClassUtils.isPrimitiveOrWrapper(type) || type.isEnum()
				|| type == Class.class || type == String.class
				|| BigInteger.class.isAssignableFrom(type)
				|| BigDecimal.class.isAssignableFrom(type);
	}
}
