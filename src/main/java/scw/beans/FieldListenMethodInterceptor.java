package scw.beans;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import scw.common.ClassInfo;
import scw.common.FieldInfo;
import scw.common.utils.ClassUtils;

public final class FieldListenMethodInterceptor implements MethodInterceptor, BeanFieldListen {
	private static final long serialVersionUID = 1L;
	private Map<String, Object> changeColumnMap;
	private boolean startListen = false;
	private transient ClassInfo classInfo;

	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		if (classInfo == null) {
			classInfo = ClassUtils.getClassInfo(obj.getClass());
		}

		if (args.length == 0) {
			if (BeanFieldListen.START_LISTEN.equals(method.getName())) {
				if (BeanFieldListen.class.isAssignableFrom(classInfo.getClz())) {
					startListen = true;
					return proxy.invokeSuper(obj, args);
				} else {
					start_field_listen();
					return null;
				}
			} else if (BeanFieldListen.GET_CHANGE_MAP.equals(method.getName())) {
				if (BeanFieldListen.class.isAssignableFrom(classInfo.getClz())) {
					return proxy.invokeSuper(obj, args);
				} else {
					return get_field_change_map();
				}
			}
		}

		if (startListen) {
			FieldInfo fieldInfo = classInfo.getFieldInfoBySetterName(method.getName());
			if (fieldInfo != null) {
				Object rtn;
				Object oldValue = null;
				oldValue = fieldInfo.forceGet(obj);
				rtn = proxy.invokeSuper(obj, args);
				if (BeanFieldListen.class.isAssignableFrom(classInfo.getClz())) {
					((BeanFieldListen) obj).field_change(fieldInfo, oldValue);
				} else {
					field_change(fieldInfo, oldValue);
				}
				return rtn;
			}
		}
		return proxy.invokeSuper(obj, args);
	}

	public Map<String, Object> get_field_change_map() {
		return changeColumnMap;
	}

	public void start_field_listen() {
		if (changeColumnMap != null && !changeColumnMap.isEmpty()) {
			changeColumnMap.clear();
		}
		startListen = true;
	}

	public void field_change(FieldInfo fieldInfo, Object oldValue) {
		if (changeColumnMap == null) {
			changeColumnMap = new HashMap<String, Object>();
		}
		changeColumnMap.put(fieldInfo.getName(), oldValue);
	}
}
