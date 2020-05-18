package scw.http.client;

import java.io.IOException;

import scw.http.HttpRequest;
import scw.net.message.OutputMessage;

public interface ClientHttpRequest extends OutputMessage, HttpRequest {
	ClientHttpResponse execute() throws IOException;
}
