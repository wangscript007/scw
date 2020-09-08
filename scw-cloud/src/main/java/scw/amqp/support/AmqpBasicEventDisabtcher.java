package scw.amqp.support;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import scw.amqp.Exchange;
import scw.amqp.Message;
import scw.amqp.MessageListener;
import scw.amqp.QueueDeclare;
import scw.event.Event;
import scw.event.EventListener;
import scw.event.EventRegistration;
import scw.event.support.DefaultBasicEventDispatcher;
import scw.io.serialzer.Serializer;
import scw.io.serialzer.SerializerUtils;

public class AmqpBasicEventDisabtcher<T extends Event> extends DefaultBasicEventDispatcher<T>
		implements MessageListener {
	private Exchange exchange;
	private String routingKey;
	private QueueDeclare queueDeclare;
	private Serializer serializer = SerializerUtils.DEFAULT_SERIALIZER;

	public AmqpBasicEventDisabtcher(Exchange exchange, String routingKey, QueueDeclare queueDeclare,
			boolean concurrent) {
		super(concurrent);
		this.exchange = exchange;
		this.queueDeclare = queueDeclare;
	}

	public Serializer getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	private volatile AtomicBoolean bind = new AtomicBoolean(false);

	@Override
	public EventRegistration registerListener(EventListener<T> eventListener) {
		if (!bind.get() && bind.compareAndSet(false, true)) {
			exchange.bind(routingKey, queueDeclare, this);
		}
		return super.registerListener(eventListener);
	}

	@Override
	public void onMessage(String exchange, String routingKey, Message message) throws IOException {
		try {
			T event = getSerializer().deserialize(message.getBody());
			publishEvent(event);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}