package scw.http.server;

import java.io.IOException;

import scw.beans.annotation.AopEnable;

@AopEnable(false)
public interface HttpServiceHandler {
	void doHandle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;
}
