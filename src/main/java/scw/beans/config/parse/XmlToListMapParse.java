package scw.beans.config.parse;

import java.lang.reflect.Field;

import scw.beans.BeanFactory;
import scw.beans.config.ConfigParse;
import scw.beans.property.AbstractCharsetNameValueFormat;
import scw.core.Constants;
import scw.core.PropertyFactory;
import scw.core.reflect.FieldDefinition;
import scw.core.utils.ConfigUtils;

public final class XmlToListMapParse extends AbstractCharsetNameValueFormat implements ConfigParse {
	public XmlToListMapParse() {
		this(Constants.DEFAULT_CHARSET_NAME);
	}

	public XmlToListMapParse(String charsetName) {
		super(charsetName);
	}

	public Object parse(BeanFactory beanFactory, FieldDefinition fieldDefinition, String filePath, String charset)
			throws Exception {
		return ConfigUtils.getDefaultXmlContent(filePath, "config");
	}

	public Object format(BeanFactory beanFactory, PropertyFactory propertyFactory, Field field, String name)
			throws Exception {
		return ConfigUtils.getDefaultXmlContent(name, "config");
	}
}
