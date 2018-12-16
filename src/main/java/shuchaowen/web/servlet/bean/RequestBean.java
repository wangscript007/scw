package shuchaowen.web.servlet.bean;

import shuchaowen.web.servlet.Request;

public interface RequestBean {
	String getId();

	String[] getNames();
	
	Class<?> getType();

	<T> T newInstance(Request request);
	
	void autowrite(Object bean) throws Exception;

	void init(Object bean) throws Exception;

	void destroy(Object bean) throws Exception;
}
