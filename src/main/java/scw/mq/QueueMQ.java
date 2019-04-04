package scw.mq;

import scw.beans.annotation.Destroy;
import scw.beans.annotation.InitMethod;
import scw.utils.queue.Queue;

public class QueueMQ<T> extends AbstractMQ<T> implements Runnable {
	private Queue<T> queue;
	private Thread thread = new Thread(this);

	public QueueMQ(Queue<T> queue) {
		this.queue = queue;
	}

	@InitMethod
	public void start() {
		thread.start();
	}

	@Destroy
	public void shutdown() {
		thread.interrupt();
	}

	public void push(T message) {
		queue.offer(message);
	}

	public void run() {
		try {
			while (!Thread.interrupted()) {
				T message = queue.take();
				try {
					execute(message);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
		}
	}

}