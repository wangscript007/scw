/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scw.websocket.sockjs.transport.session;

import java.io.IOException;
import java.util.Map;

import scw.core.Assert;
import scw.mvc.Channel;
import scw.net.http.server.ServerHttpRequest;
import scw.websocket.WebSocketHandler;
import scw.websocket.sockjs.SockJsTransportFailureException;
import scw.websocket.sockjs.frame.SockJsFrame;
import scw.websocket.sockjs.frame.SockJsMessageCodec;
import scw.websocket.sockjs.transport.SockJsServiceConfig;

/**
 * A SockJS session for use with streaming HTTP transports.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class StreamingSockJsSession extends AbstractHttpSockJsSession {

	private int byteCount;


	public StreamingSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}


	/**
	 * @deprecated as of 4.2, since this method is no longer used.
	 */
	@Deprecated
	protected boolean isStreaming() {
		return true;
	}

	/**
	 * Get the prelude to write to the response before any other data.
	 * @since 4.2
	 */
	protected abstract byte[] getPrelude(ServerHttpRequest request);


	@Override
	protected void handleRequestInternal(Channel channel,
			boolean initialRequest) throws IOException {

		byte[] prelude = getPrelude(channel.getRequest());
		Assert.state(prelude != null, "Prelude expected");
		channel.getResponse().getBody().write(prelude);
		channel.getResponse().flush();

		if (initialRequest) {
			writeFrame(SockJsFrame.openFrame());
		}
		flushCache();
	}

	@Override
	protected void flushCache() throws SockJsTransportFailureException {
		while (!getMessageCache().isEmpty()) {
			String message = getMessageCache().poll();
			SockJsMessageCodec messageCodec = getSockJsServiceConfig().getMessageCodec();
			SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, message);
			writeFrame(frame);

			this.byteCount += (frame.getContentBytes().length + 1);
			if (logger.isTraceEnabled()) {
				logger.trace(this.byteCount + " bytes written so far, " +
						getMessageCache().size() + " more messages not flushed");
			}
			if (this.byteCount >= getSockJsServiceConfig().getStreamBytesLimit()) {
				logger.trace("Streamed bytes limit reached, recycling current request");
				resetRequest();
				this.byteCount = 0;
				break;
			}
		}
		scheduleHeartbeat();
	}

}
