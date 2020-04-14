package scw.mvc.handler;

import scw.mvc.Channel;

public abstract class AbstractIteratorHandlerChain implements HandlerChain {
	private final HandlerChain chain;

	public AbstractIteratorHandlerChain(HandlerChain chain) {
		this.chain = chain;
	}

	public final Object doHandler(Channel channel) throws Throwable {
		Handler channelHandler = getNextChannelHandler(channel);
		if (channelHandler == null) {
			if (chain == null) {
				return lastHandler(channel);
			} else {
				return chain.doHandler(channel);
			}
		} else {
			return channelHandler.doHandler(channel, this);
		}
	}

	protected abstract Handler getNextChannelHandler(Channel channel)
			throws Throwable;

	protected Object lastHandler(Channel channel) throws Throwable {
		channel.getLogger().warn("handler not support channel:{}", channel.toString());
		return null;
	}
}
