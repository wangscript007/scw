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
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import scw.core.Assert;
import scw.core.utils.StringUtils;
import scw.http.HttpHeaders;
import scw.messageing.TextMessage;
import scw.websocket.CloseStatus;
import scw.websocket.WebSocketExtension;
import scw.websocket.WebSocketHandler;
import scw.websocket.WebSocketSession;
import scw.websocket.adapter.NativeWebSocketSession;
import scw.websocket.sockjs.SockJsTransportFailureException;
import scw.websocket.sockjs.frame.SockJsFrame;
import scw.websocket.sockjs.transport.SockJsServiceConfig;

/**
 * A SockJS session for use with the WebSocket transport.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class WebSocketServerSockJsSession extends AbstractSockJsSession implements NativeWebSocketSession {

	private WebSocketSession webSocketSession;

	private volatile boolean openFrameSent;

	private final Queue<String> initSessionCache = new LinkedBlockingDeque<String>();

	private final Object initSessionLock = new Object();

	private final Object disconnectLock = new Object();

	private volatile boolean disconnected;


	public WebSocketServerSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler handler, Map<String, Object> attributes) {

		super(id, config, handler, attributes);
	}

	public URI getUri() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getUri();
	}

	public HttpHeaders getHandshakeHeaders() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getHandshakeHeaders();
	}

	public Principal getPrincipal() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getPrincipal();
	}

	public InetSocketAddress getLocalAddress() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getLocalAddress();
	}
	
	public InetSocketAddress getRemoteAddress() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getRemoteAddress();
	}

	public String getAcceptedProtocol() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getAcceptedProtocol();
	}

	public List<WebSocketExtension> getExtensions() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getExtensions();
	}

	private void checkDelegateSessionInitialized() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
	}

	public Object getNativeSession() {
		return (this.webSocketSession instanceof NativeWebSocketSession ?
				((NativeWebSocketSession) this.webSocketSession).getNativeSession() : this.webSocketSession);
	}

	public <T> T getNativeSession(Class<T> requiredType) {
		return (this.webSocketSession instanceof NativeWebSocketSession ?
				((NativeWebSocketSession) this.webSocketSession).getNativeSession(requiredType) : null);
	}


	public void initializeDelegateSession(WebSocketSession session) {
		synchronized (this.initSessionLock) {
			this.webSocketSession = session;
			try {
				// Let "our" handler know before sending the open frame to the remote handler
				delegateConnectionEstablished();
				this.webSocketSession.sendMessage(new TextMessage(SockJsFrame.openFrame().getContent()));

				// Flush any messages cached in the mean time
				while (!this.initSessionCache.isEmpty()) {
					writeFrame(SockJsFrame.messageFrame(getMessageCodec(), this.initSessionCache.poll()));
				}
				scheduleHeartbeat();
				this.openFrameSent = true;
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			}
		}
	}

	@Override
	public boolean isActive() {
		return (this.webSocketSession != null && this.webSocketSession.isOpen() && !this.disconnected);
	}

	public void handleMessage(TextMessage message, WebSocketSession wsSession) throws Exception {
		String payload = message.getPayload();
		if (StringUtils.isEmpty(payload)) {
			return;
		}
		String[] messages;
		try {
			messages = getSockJsServiceConfig().getMessageCodec().decode(payload);
		}
		catch (Throwable ex) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", ex);
			tryCloseWithSockJsTransportError(ex, CloseStatus.BAD_DATA);
			return;
		}
		if (messages != null) {
			delegateMessages(messages);
		}
	}

	@Override
	public void sendMessageInternal(String message) throws SockJsTransportFailureException {
		// Open frame not sent yet?
		// If in the session initialization thread, then cache, otherwise wait.
		if (!this.openFrameSent) {
			synchronized (this.initSessionLock) {
				if (!this.openFrameSent) {
					this.initSessionCache.add(message);
					return;
				}
			}
		}

		cancelHeartbeat();
		writeFrame(SockJsFrame.messageFrame(getMessageCodec(), message));
		scheduleHeartbeat();
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Writing " + frame);
		}
		TextMessage message = new TextMessage(frame.getContent());
		this.webSocketSession.sendMessage(message);
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		if (isActive()) {
			synchronized (this.disconnectLock) {
				if (isActive()) {
					this.disconnected = true;
					if (this.webSocketSession != null) {
						this.webSocketSession.close(status);
					}
				}
			}
		}
	}

}
