package scw.net.message.converter;

import java.io.IOException;

import scw.core.ResolvableType;
import scw.http.MediaType;
import scw.io.IOUtils;
import scw.net.MimeType;
import scw.net.message.InputMessage;
import scw.net.message.OutputMessage;

public class ByteArrayMessageConverter extends AbstractMessageConverter<byte[]>{
	
	public ByteArrayMessageConverter(){
		supportMimeTypes.add(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
		setSupportBytes(true);
	}
	
	@Override
	public boolean support(Class<?> clazz) {
		return clazz == byte[].class;
	}

	@Override
	protected byte[] readInternal(ResolvableType type, InputMessage inputMessage)
			throws IOException, MessageConvertException {
		return IOUtils.toByteArray(inputMessage.getBody());
	}

	@Override
	protected void writeInternal(ResolvableType type, byte[] body, MimeType contentType,
			OutputMessage outputMessage) throws IOException,
			MessageConvertException {
		outputMessage.getBody().write(body);
	}
	
	@Override
	protected Long getContentLength(byte[] body, MimeType contentType)
			throws IOException {
		return (long) body.length;
	}
}
