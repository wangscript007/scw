package scw.util;

import java.io.Serializable;

/**
 * 并发执行结果
 * @author shuchaowen
 *
 * @param <T>
 */
public class Result<T> implements Serializable{
	private static final long serialVersionUID = 1L;
	private final boolean active;
	private final Supplier<T> result;
	
	public Result(boolean active, Supplier<T> result){
		this.active = active;
		this.result = result;
	}
	
	public Result(boolean active, T result){
		this(active, new StaticSupplier<T>(result));
	}

	public boolean isActive() {
		return active;
	}

	public T getResult() {
		return result.get();
	}
}
