package scw.mq.support;

import scw.aop.Invoker;
import scw.core.Parameters;

@SuppressWarnings("rawtypes")
public class MqMethodConsumer implements scw.util.queue.Consumer {
	private Invoker invoker;

	public MqMethodConsumer(Invoker invoker) {
		this.invoker = invoker;
	}

	public void consume(Object message) throws Throwable {
		if (message == null) {
			return;
		}

		if (message instanceof Parameters) {
			invoker.invoke(((Parameters) message).getParameters());
		} else {
			if (message.getClass().isArray()) {
				invoker.invoke((Object[]) message);
			} else {
				invoker.invoke(message);
			}
		}
	}
}
