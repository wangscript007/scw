package scw.beans.ioc.value;

import java.nio.charset.Charset;
import java.util.Properties;

import scw.beans.BeanDefinition;
import scw.beans.BeanFactory;
import scw.beans.annotation.Value;
import scw.core.utils.ClassUtils;
import scw.event.Observable;
import scw.instance.InstanceUtils;
import scw.mapper.Field;
import scw.mapper.Fields;
import scw.mapper.FilterFeature;
import scw.mapper.MapperUtils;
import scw.value.ValueUtils;

public final class PropertiesFileValueProcesser extends AbstractObservableValueProcesser<Properties> {

	@Override
	protected Observable<Properties> getObservableResource(BeanDefinition beanDefinition,
			BeanFactory beanFactory, Object bean, Field field, Value value,
			String name, Charset charset) {
		return beanFactory.getEnvironment().getProperties(name, charset);
	}

	@Override
	protected Object parse(BeanDefinition beanDefinition, BeanFactory beanFactory,
			Object bean, Field field, Value value, String name, Charset charset, Properties properties) {
		if (ClassUtils.isPrimitiveOrWrapper(field.getSetter().getType())
				|| field.getSetter().getType() == String.class) {
			return ValueUtils.parse(properties.getProperty(field.getSetter().getName()),
					field.getSetter().getGenericType());
		} else if (Properties.class.isAssignableFrom(field.getSetter().getType())) {
			return properties;
		} else {
			Class<?> fieldType = field.getSetter().getType();
			Object obj = InstanceUtils.INSTANCE_FACTORY.getInstance(fieldType);
			Fields fields = MapperUtils.getMapper().getFields(fieldType, FilterFeature.SUPPORT_SETTER);
			for (final Object key : properties.keySet()) {
				Field keyField = fields.findSetter(key.toString(), null);
				if (keyField == null) {
					continue;
				}
				
				MapperUtils.setValue(beanFactory.getEnvironment(), obj, keyField, properties.getProperty(keyField.getSetter().getName()));
			}
			return obj;
		}
	}
}