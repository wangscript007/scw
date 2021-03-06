package scw.beans.xml;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Node;

import scw.beans.BeanDefinition;
import scw.beans.BeanFactory;
import scw.beans.BeansException;
import scw.core.annotation.AnnotatedElementUtils;
import scw.core.parameter.ParameterDescriptor;
import scw.core.utils.StringUtils;
import scw.lang.NotFoundException;
import scw.value.StringValue;
import scw.value.Value;

public final class XmlBeanParameter implements Cloneable, ParameterDescriptor, Serializable {
	private static final long serialVersionUID = 1L;
	private final XmlParameterType parameterType;
	private Class<?> type;
	private final String name;// 可能为空
	private final XmlValue xmlValue;

	public XmlBeanParameter(XmlParameterType parameterType, Class<?> type, String name, String value, Node node) {
		this.parameterType = parameterType;
		this.type = type;
		this.name = name;
		this.xmlValue = new XmlValue(value, node);
	}
	
	@Override
	public String toString() {
		return "[parameterType=" + parameterType + ", type=" + type + ", name=" + name + ", xmlValue=" + xmlValue + "]";
	}

	@Override
	public XmlBeanParameter clone() {
		try {
			return (XmlBeanParameter) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public XmlParameterType getParameterType() {
		return parameterType;
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public Type getGenericType() {
		return type;
	}

	public AnnotatedElement getAnnotatedElement() {
		return AnnotatedElementUtils.EMPTY_ANNOTATED_ELEMENT;
	}

	public boolean isAccept(ParameterDescriptor parameterDescriptor, BeanFactory beanFactory){
		switch (parameterType) {
		case ref:
			BeanDefinition definition = beanFactory.getDefinition(xmlValue.formatValue(beanFactory.getEnvironment()));
			if(definition == null){
				return false;
			}
			
			return parameterDescriptor.getType().isAssignableFrom(definition.getTargetClass()) && definition.isInstance();
		case property:
			return beanFactory.getEnvironment().containsKey(xmlValue.getValue());
		default:
			break;
		}
		return true;
	}

	public Object parseValue(ParameterDescriptor parameterDescriptor, BeanFactory beanFactory) {
		Object value = null;
		switch (parameterType) {
		case value:
			String text = xmlValue.formatValue(beanFactory.getEnvironment());
			if (text != null) {
				value = formatStringValue(new StringValue(text), parameterDescriptor);
			}
			break;
		case ref:
			value = beanFactory.getInstance(xmlValue.formatValue(beanFactory.getEnvironment()));
			break;
		case property:
			Value v = beanFactory.getEnvironment().getValue(xmlValue.getValue());
			if (v != null) {
				value = formatStringValue(v, parameterDescriptor);
			}
			break;
		default:
			break;
		}

		if (xmlValue.isRequire() && value == null) {
			throw new NotFoundException(xmlValue.getValue());
		}
		return value;
	}

	private Object formatStringValue(Value value, ParameterDescriptor parameterDescriptor) {
		if (value == null) {
			return null;
		}

		if (Date.class == parameterDescriptor.getType()) {
			if (value.isNumber()) {
				return new Date(value.getAsLong());
			} else {
				String dateFormat = xmlValue.getNodeAttributeValue("date-format");
				if (StringUtils.isEmpty(dateFormat)) {
					throw new NotFoundException("data-format [" + value + "]");
				}

				SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
				try {
					return sdf.parse(value.getAsString());
				} catch (ParseException e) {
					throw new BeansException(value.getAsString(), e);
				}
			}
		}
		
		return value.getAsObject(parameterDescriptor.getGenericType());
	}
}
