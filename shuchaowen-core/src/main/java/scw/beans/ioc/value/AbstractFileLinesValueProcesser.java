package scw.beans.ioc.value;

import java.util.List;

import scw.beans.BeanDefinition;
import scw.beans.BeanFactory;
import scw.beans.annotation.Value;
import scw.io.ResourceUtils;
import scw.io.event.ObservableResource;
import scw.io.event.ObservableResourceEvent;
import scw.io.event.ObservableResourceEventListener;
import scw.mapper.Field;
import scw.value.property.PropertyFactory;

public abstract class AbstractFileLinesValueProcesser extends AbstractValueProcesser {

	@Override
	protected void processInteranl(final BeanDefinition beanDefinition, final BeanFactory beanFactory,
			final PropertyFactory propertyFactory, final Object bean, final Field field, final Value value,
			final String name, final String charsetName) {
		ObservableResource<List<String>> res = ResourceUtils.getResourceOperations().getLines(name, charsetName);
		field.getSetter().set(bean, parse(beanDefinition, beanFactory, propertyFactory, bean, field, value, name,
				charsetName, res.getResource()));
		if (isRegisterListener(beanDefinition, field, value)) {
			res.registerListener(new ObservableResourceEventListener<List<String>>() {

				public void onEvent(ObservableResourceEvent<List<String>> event) {
					field.getSetter().set(bean, parse(beanDefinition, beanFactory, propertyFactory, bean, field, value,
							name, charsetName, event.getSource()));
				}
			});
		}
	}

	protected abstract Object parse(BeanDefinition beanDefinition, BeanFactory beanFactory,
			PropertyFactory propertyFactory, Object bean, Field field, Value value, String name, String charsetName,
			List<String> lines);
}