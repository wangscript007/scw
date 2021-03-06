package scw.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import scw.beans.ioc.value.EnvironmentValueProcess;
import scw.beans.ioc.value.ValueProcesser;

@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Value {
	String value();

	Class<? extends ValueProcesser> processer() default EnvironmentValueProcess.class;

	String charsetName() default "";
	
	/**
	 * 是否监听变更
	 * @return
	 */
	boolean listener() default true;
}
