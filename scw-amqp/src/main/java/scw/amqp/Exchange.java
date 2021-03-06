package scw.amqp;

import scw.core.reflect.MethodInvoker;


public interface Exchange {
	void bind(String routingKey, QueueDeclare queueDeclare, MessageListener messageListener);

	void push(String routingKey, Message message);

	void push(String routingKey, MessageProperties messageProperties, byte[] body);

	void bind(String routingKey, QueueDeclare queueDeclare, MethodInvoker invoker);

	void push(String routingKey, MethodMessage message);
}
