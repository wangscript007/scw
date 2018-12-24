package scw.beans.config.parse;

import java.io.File;
import java.util.Properties;

import scw.beans.BeanFactory;
import scw.beans.config.ConfigParse;
import scw.common.ClassInfo;
import scw.common.FieldInfo;
import scw.common.utils.ClassUtils;
import scw.common.utils.ConfigUtils;
import scw.common.utils.StringUtils;

public class PropertiesParse implements ConfigParse {
	public Object parse(BeanFactory beanFactory, FieldInfo field, String filePath, String charset) throws Exception{
		File file = ConfigUtils.getFile(filePath);
		Properties properties = ConfigUtils.getProperties(file, charset);
		if(ClassUtils.isBasicType(field.getType()) || ClassUtils.isStringType(field.getType())){
			String v = ConfigUtils.format(properties.getProperty(field.getName()));
			return StringUtils.conversion(v, field.getType());
		}else if(Properties.class.isAssignableFrom(field.getType())){
			return properties;
		}else{
			Class<?> fieldTpe = field.getType();
			ClassInfo fieldClassInfo = ClassUtils.getClassInfo(fieldTpe);
			try {
				Object obj = fieldTpe.newInstance();
				for (Object key : properties.keySet()) {
					FieldInfo fieldInfo = fieldClassInfo.getFieldMap().get(key.toString());
					if(fieldInfo == null){
						continue;
					}
					
					String value = ConfigUtils.format(properties.getProperty(key.toString()));
					fieldInfo.set(obj, StringUtils.conversion(value, fieldInfo.getType()));
				}
				return obj;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}