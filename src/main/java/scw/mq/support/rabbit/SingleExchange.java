package scw.mq.support.rabbit;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import scw.beans.annotation.AsyncComplete;
import scw.core.Consumer;
import scw.core.logger.Logger;
import scw.core.logger.LoggerFactory;
import scw.core.logger.LoggerUtils;
import scw.core.serializer.NoTypeSpecifiedSerializer;
import scw.mq.amqp.AmqpQueueConfig;
import scw.mq.amqp.Exchange;

public class SingleExchange<T> implements Exchange<T> {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private final SingleExchangeChannelFactory channelFactory;
	private final NoTypeSpecifiedSerializer serializer;

	public SingleExchange(SingleExchangeChannelFactory channelFactory, NoTypeSpecifiedSerializer serializer)
			throws IOException, TimeoutException {
		this.channelFactory = channelFactory;
		this.serializer = serializer;
	}

	public final SingleExchangeChannelFactory getChannelFactory() {
		return channelFactory;
	}

	public final NoTypeSpecifiedSerializer getSerializer() {
		return serializer;
	}

	public void bindConsumer(String routingKey, String queueName, boolean durable, boolean exclusive,
			boolean autoDelete, Map<String, Object> arguments, Consumer<T> consumer) {
		Channel channel = channelFactory.getChannel(routingKey);
		try {
			channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments);
			channel.queueBind(queueName, channelFactory.getExchange(), routingKey, arguments);
			channel.basicConsume(queueName, autoDelete,
					new RabbitDefaultConsumer(channel, autoDelete, consumer, queueName));
		} catch (IOException e) {
			StringBuilder sb = new StringBuilder();
			try {
				LoggerUtils.loggerAppend(sb, "exchange={},rotingKey={},durable={},exclusive={},autoDelete={}", null,
						channelFactory.getExchangeType(), routingKey, durable, exclusive, autoDelete);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			throw new RuntimeException(sb.toString(), e);
		}
	}

	public void bindConsumer(String routingKey, String queueName, boolean durable, boolean exclusive,
			boolean autoDelete, Consumer<T> consumer) {
		bindConsumer(routingKey, queueName, durable, exclusive, autoDelete, null, consumer);
	}

	@AsyncComplete
	public void push(String routingKey, boolean mandatory, boolean immediate, T message) {
		try {
			channelFactory.getChannel(routingKey).basicPublish(channelFactory.getExchange(), routingKey, mandatory,
					immediate, null, getSerializer().serialize(message));
		} catch (IOException e) {
			StringBuilder sb = new StringBuilder();
			try {
				LoggerUtils.loggerAppend(sb, "exchange={},rotingKey={},mandatory={},immediate={}", null,
						channelFactory.getExchange(), routingKey, mandatory, immediate);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			throw new RuntimeException(sb.toString(), e);
		}
	}

	public void push(String routingKey, T message) {
		try {
			channelFactory.getChannel(routingKey).basicPublish(channelFactory.getExchange(), routingKey, null,
					getSerializer().serialize(message));
		} catch (IOException e) {
			StringBuilder sb = new StringBuilder();
			try {
				LoggerUtils.loggerAppend(sb, "exchange={},rotingKey={}", null, channelFactory.getExchange(),
						routingKey);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			throw new RuntimeException(sb.toString(), e);
		}
	}

	final class RabbitDefaultConsumer extends DefaultConsumer {
		private final Consumer<T> consumer;
		private final boolean autoAck;
		private final String name;

		public RabbitDefaultConsumer(Channel channel, boolean autoAck, Consumer<T> consumer, String name) {
			super(channel);
			this.consumer = consumer;
			this.autoAck = autoAck;
			this.name = name;
		}

		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
				throws IOException {
			if (body == null) {
				return;
			}

			T message;
			try {
				message = getSerializer().deserialize(body);
				consumer.consume(message);
				if (!autoAck) {
					getChannel().basicAck(envelope.getDeliveryTag(), false);
				}
			} catch (Throwable e) {
				logger.error("消费者异常, exchange=" + envelope.getExchange() + ", routingKey=" + envelope.getRoutingKey()
						+ ", queueName=" + name, e);
			}
		}
	}

	public void bindConsumer(String routingKey, String queueName, Consumer<T> consumer) {
		bindConsumer(routingKey, queueName, true, false, false, null, consumer);
	}

	public void bindConsumer(AmqpQueueConfig config, Consumer<T> consumer) {
		bindConsumer(config.getRoutingKey(), config.getQueueName(), config.isDurable(), config.isExclusive(),
				config.isAutoDelete(), null, consumer);
	}
}
