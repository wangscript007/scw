
package scw.rabbitmq.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitmqConsumerBind {
	public String routingKey();

	public String queueName();

	public boolean durable() default true;

	public boolean exclusive() default false;

	public boolean autoDelete() default false;
	
	public String exchangeService() default "";
}
