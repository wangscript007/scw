package scw.beans.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 提供异常重试
 * @author shuchaowen
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Retry {
	public Class<? extends Throwable>[] errors();
	
	public int maxCount() default -1;
	
	/**
	 * 重试间隔时间
	 * @return
	 */
	public long delayMillis() default 0;
	
	public TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
	
	public boolean log() default false;
}
