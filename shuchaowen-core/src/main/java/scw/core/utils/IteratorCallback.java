package scw.core.utils;

/**
 * 迭代器回调
 * @author shuchaowen
 *
 * @param <T>
 */
public interface IteratorCallback<T> {
	/**
	 * 迭代器回调
	 * @param data 迭代数据
	 * @return 是否继续迭代
	 */
	boolean iteratorCallback(T data);
}