package scw.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.nio.charset.Charset;

import scw.net.Request;

public interface HttpRequest extends Request {
	void setFixedLengthStreamingMode (int contentLength);

    void setFixedLengthStreamingMode(long contentLength);

    void setChunkedStreamingMode (int chunklen);

	void setInstanceFollowRedirects(boolean followRedirects);

	boolean getInstanceFollowRedirects();

	void setRequestMethod(String method) throws ProtocolException;

	String getRequestMethod();

	int getResponseCode() throws IOException;

	String getResponseMessage() throws IOException;

	void disconnect();

	boolean usingProxy();
	
    InputStream getErrorStream();
    
    String getResponseBody(Charset charset) throws IOException;
    
    String getRequestContentType();
    
    void setRequestContentType(String contentType);
}