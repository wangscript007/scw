package scw.util.queue;

import scw.core.Destroy;
import scw.logger.Logger;
import scw.logger.LoggerUtils;

/**
 * 使用阻塞队列实现消息队列
 * 
 * @author shuchaowen
 */
public class BlockingMessageQueue<E> extends AbstractQueue<E> implements Runnable, Destroy {
	private static Logger logger = LoggerUtils.getLogger(MemoryQueue.class);
	private BlockingQueue<E> blockingQueue;
	private Thread thread;

	public BlockingMessageQueue(BlockingQueue<E> blockingQueue) {
		this.blockingQueue = blockingQueue;
		this.thread = new Thread(this, getClass().getName());
		thread.start();
	}

	public void push(E message) {
		try {
			blockingQueue.put(message);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void destroy() {
		if (thread != null) {
			thread.interrupt();
		}
	}

	public void run() {
		try {
			while (!thread.isInterrupted()) {
				E message = blockingQueue.take();
				try {
					consume(message);
				} catch (Throwable e) {
					logger.error(e, "消费者异常");
				}
			}
		} catch (InterruptedException e) {
		}
	}
}