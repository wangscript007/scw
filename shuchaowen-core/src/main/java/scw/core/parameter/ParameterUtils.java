package scw.core.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;

import scw.core.annotation.AnnotationUtils;
import scw.core.parameter.annotation.DefaultValue;
import scw.core.parameter.annotation.ParameterName;
import scw.core.utils.ArrayUtils;
import scw.core.utils.StringUtils;
import scw.value.StringValue;
import scw.value.Value;

public final class ParameterUtils {
	private static final LocalVariableTableParameterNameDiscoverer LVTPND = new LocalVariableTableParameterNameDiscoverer();

	private ParameterUtils() {
	};

	public static ParameterDescriptor[] getParameterDescriptors(Constructor<?> constructor) {
		String[] names = ParameterUtils.getParameterName(constructor);
		if (ArrayUtils.isEmpty(names)) {
			return ParameterDescriptor.EMPTY_ARRAY;
		}

		Annotation[][] parameterAnnoatations = constructor.getParameterAnnotations();
		Type[] parameterGenericTypes = constructor.getGenericParameterTypes();
		Class<?>[] parameterTypes = constructor.getParameterTypes();
		ParameterDescriptor[] parameterDefinitions = new ParameterDescriptor[names.length];
		for (int i = 0; i < names.length; i++) {
			parameterDefinitions[i] = new DefaultParameterDescriptor(names[i], parameterAnnoatations[i],
					parameterTypes[i], parameterGenericTypes[i]);
		}
		return parameterDefinitions;
	}

	public static ParameterDescriptor[] getParameterDescriptors(Method method) {
		String[] names = ParameterUtils.getParameterName(method);
		if (ArrayUtils.isEmpty(names)) {
			return ParameterDescriptor.EMPTY_ARRAY;
		}

		Annotation[][] parameterAnnoatations = method.getParameterAnnotations();
		Type[] parameterGenericTypes = method.getGenericParameterTypes();
		Class<?>[] parameterTypes = method.getParameterTypes();
		ParameterDescriptor[] parameterDefinitions = new ParameterDescriptor[names.length];
		for (int i = 0; i < names.length; i++) {
			parameterDefinitions[i] = new DefaultParameterDescriptor(names[i], parameterAnnoatations[i],
					parameterTypes[i], parameterGenericTypes[i]);
		}
		return parameterDefinitions;
	}

	public static String[] getParameterName(Method method) {
		return LVTPND.getParameterNames(method);
	}

	@SuppressWarnings("rawtypes")
	public static String[] getParameterName(Constructor constructor) {
		return LVTPND.getParameterNames(constructor);
	}

	public static boolean isNullAble(ParameterDescriptor parameterConfig) {
		if (parameterConfig.getType().isPrimitive()) {
			return false;
		}

		return AnnotationUtils.isNullable(parameterConfig.getAnnotatedElement(), true);
	}

	public static LinkedHashMap<String, Object> getParameterMap(ParameterDescriptor[] parameterDescriptors,
			Object[] args) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(parameterDescriptors.length);
		for (int i = 0; i < parameterDescriptors.length; i++) {
			ParameterDescriptor parameterDescriptor = parameterDescriptors[i];
			map.put(parameterDescriptor.getName(), args[i]);
		}
		return map;
	}

	public static LinkedHashMap<String, Object> getParameterMap(Method method, Object[] args) {
		return getParameterMap(getParameterDescriptors(method), args);
	}

	public static LinkedHashMap<String, Object> getParameterMap(Constructor<?> constructor, Object[] args) {
		return getParameterMap(getParameterDescriptors(constructor), args);
	}
	
	public static Value getDefaultValue(ParameterDescriptor parameterDescriptor){
		DefaultValue defaultValue = parameterDescriptor.getAnnotatedElement().getAnnotation(DefaultValue.class);
		if (defaultValue == null) {
			return null;
		}
		return new StringValue(defaultValue.value());
	}
	
	public static String getDisplayName(ParameterDescriptor parameterDescriptor){
		ParameterName parameterName = parameterDescriptor.getAnnotatedElement().getAnnotation(ParameterName.class);
		if (parameterName != null && StringUtils.isNotEmpty(parameterName.value())) {
			return parameterName.value();
		}	
		return parameterDescriptor.getName();
	}
}
