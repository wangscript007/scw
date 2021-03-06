package scw.instance;

import scw.util.ClassLoaderProvider;

public interface NoArgsInstanceFactory extends ClassLoaderProvider{
	<T> T getInstance(Class<T> clazz);

	<T> T getInstance(String name);

	/**
	 * 表示可以使用getInstance(String name)方式获取实例
	 * 
	 * @param name
	 * @return
	 */
	boolean isInstance(String name);

	/**
	 * 表示可以使用getInstance(Class<T> type)方式获取实例
	 * 
	 * @param clazz
	 * @return
	 */
	boolean isInstance(Class<?> clazz);
}
