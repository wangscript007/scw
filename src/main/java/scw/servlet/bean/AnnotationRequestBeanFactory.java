package scw.servlet.bean;

import java.util.HashMap;
import java.util.Map;

import scw.beans.BeanFactory;
import scw.beans.property.PropertiesFactory;
import scw.common.exception.AlreadyExistsException;
import scw.common.utils.ClassUtils;

public final class AnnotationRequestBeanFactory implements RequestBeanFactory {
	private volatile Map<String, AnnotationRequestBean> beanMap = new HashMap<String, AnnotationRequestBean>();
	private final BeanFactory beanFactory;
	private final PropertiesFactory propertiesFactory;

	public AnnotationRequestBeanFactory(BeanFactory beanFactory,
			PropertiesFactory propertiesFactory) {
		this.beanFactory = beanFactory;
		this.propertiesFactory = propertiesFactory;
	}

	public RequestBean get(String name) {
		AnnotationRequestBean bean = beanMap.get(name);
		if (bean == null) {
			synchronized (beanMap) {
				bean = beanMap.get(name);
				if (bean == null && contains(name)) {
					try {
						bean = new AnnotationRequestBean(beanFactory,
								propertiesFactory, ClassUtils.forName(name));
						if (beanMap.containsKey(bean.getId())) {
							throw new AlreadyExistsException(bean.getId());
						}

						beanMap.put(bean.getId(), bean);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return bean;
	}

	public boolean contains(String name) {
		if (beanMap.containsKey(name)) {
			return true;
		}
		try {
			Class<?> clz = ClassUtils.forName(name);
			if (ClassUtils.isInstance(clz)
					&& AnnotationRequestBean
							.getAnnotationRequestBeanConstructor(clz) != null) {
				return true;
			}
		} catch (ClassNotFoundException e) {
		}
		return false;
	}

}