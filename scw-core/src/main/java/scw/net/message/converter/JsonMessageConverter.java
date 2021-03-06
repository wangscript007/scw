package scw.net.message.converter;

import java.io.IOException;

import scw.core.ResolvableType;
import scw.json.JSONSupport;
import scw.net.MimeType;
import scw.net.MimeTypeUtils;
import scw.net.message.InputMessage;
import scw.net.message.OutputMessage;
import scw.util.ToMap;
import scw.util.XUtils;

public final class JsonMessageConverter extends AbstractMessageConverter<Object> {
	public static final MimeType JSON_ALL = new MimeType("application", "*+json");

	public JsonMessageConverter() {
		supportMimeTypes.add(MimeTypeUtils.APPLICATION_JSON, JSON_ALL, TEXT_ALL);
	}

	@Override
	public boolean support(Class<?> clazz) {
		return true;
	}

	@Override
	protected Object readInternal(ResolvableType type, InputMessage inputMessage) throws IOException, MessageConvertException {
		String text = readTextBody(inputMessage);
		if (text == null) {
			return null;
		}
		return getJsonSupport().parseObject(text, type.getType());
	}

	@Override
	protected void writeInternal(ResolvableType type, Object body, MimeType contentType, OutputMessage outputMessage)
			throws IOException, MessageConvertException {
		String text = toJsonString(body, getJsonSupport());
		if (text == null) {
			return;
		}
		writeTextBody(text, contentType, outputMessage);
	}

	public static String toJsonString(Object body, JSONSupport jsonSupport) {
		if (body == null) {
			return null;
		}

		if (body instanceof ToMap) {
			return jsonSupport.toJSONString(XUtils.toMap(body));
		} else {
			return jsonSupport.toJSONString(body);
		}
	}
}
