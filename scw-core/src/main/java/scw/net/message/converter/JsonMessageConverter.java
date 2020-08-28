package scw.net.message.converter;

import java.io.IOException;
import java.lang.reflect.Type;

import scw.http.HttpUtils;
import scw.net.MimeType;
import scw.net.MimeTypeUtils;
import scw.net.message.InputMessage;
import scw.net.message.OutputMessage;

public final class JsonMessageConverter extends
		AbstractMessageConverter<Object> {
	public static final MimeType JSON_ALL = new MimeType("application",
			"*+json");

	public JsonMessageConverter() {
		supportMimeTypes.add(MimeTypeUtils.APPLICATION_JSON, JSON_ALL, TEXT_ALL);
	}

	@Override
	public boolean support(Class<?> clazz) {
		return true;
	}

	@Override
	protected Object readInternal(Type type, InputMessage inputMessage)
			throws IOException, MessageConvertException {
		String text = readTextBody(inputMessage);
		if (text == null) {
			return null;
		}
		return getJsonSupport().parseObject(text, type);
	}

	@Override
	protected void writeInternal(Object body, MimeType contentType,
			OutputMessage outputMessage) throws IOException,
			MessageConvertException {
		String text = HttpUtils.toJsonString(body, getJsonSupport());
		if (text == null) {
			return;
		}
		writeTextBody(text, contentType, outputMessage);
	}

}