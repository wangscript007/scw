package scw.net.message.converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;

import scw.core.Constants;
import scw.core.utils.TypeUtils;
import scw.io.IOUtils;
import scw.json.JSONSupport;
import scw.json.JSONUtils;
import scw.logger.Logger;
import scw.logger.LoggerUtils;
import scw.net.MimeType;
import scw.net.MimeTypeUtils;
import scw.net.MimeTypes;
import scw.net.http.HttpHeaders;
import scw.net.message.Headers;
import scw.net.message.InputMessage;
import scw.net.message.Message;
import scw.net.message.OutputMessage;

public abstract class AbstractMessageConverter<T> extends MimeTypes
		implements MessageConverter {
	private static final long serialVersionUID = 1L;
	protected final transient Logger logger = LoggerUtils.getLogger(getClass());
	public static final MimeType TEXT_ALL = new MimeType("text", "*");
	private Charset defaultCharset = Constants.DEFAULT_CHARSET;
	private JSONSupport jsonSupport = JSONUtils.DEFAULT_JSON_SUPPORT;

	public Enumeration<MimeType> enumerationSupportMimeTypes() {
		return Collections.enumeration(this);
	}
	
	public AbstractMessageConverter<T> add(MimeType ...mimeTypes){
		for(MimeType mimeType : mimeTypes){
			add(mimeType);
		}
		return this;
	}

	public JSONSupport getJsonSupport() {
		return jsonSupport;
	}

	public void setJsonSupport(JSONSupport jsonSupport) {
		this.jsonSupport = jsonSupport;
	}

	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	public boolean canRead(MimeType contentType) {
		if (contentType == null) {
			return true;
		}

		Enumeration<MimeType> enumeration = enumerationSupportMimeTypes();
		while (enumeration.hasMoreElements()) {
			if (enumeration.nextElement().includes(contentType)) {
				return true;
			}
		}
		return false;
	}

	public boolean canWrite(MimeType contentType) {
		if (contentType == null
				|| MimeTypeUtils.ALL.equalsTypeAndSubtype(contentType)) {
			return true;
		}

		Enumeration<MimeType> enumeration = enumerationSupportMimeTypes();
		while (enumeration.hasMoreElements()) {
			if (enumeration.nextElement().isCompatibleWith(contentType)) {
				return true;
			}
		}
		return false;
	}

	public abstract boolean support(Class<?> clazz);

	public boolean canRead(Type type, MimeType contentType) {
		return support(TypeUtils.toClass(type)) && canRead(contentType);
	}

	public boolean canWrite(Object body, MimeType contentType) {
		if (body == null) {
			return false;
		}

		return support(body.getClass()) && canWrite(contentType);
	}

	public Object read(Type type, InputMessage inputMessage)
			throws IOException, MessageConvertException {
		return readInternal(type, inputMessage);
	}

	@SuppressWarnings("unchecked")
	public void write(Object body, MimeType contentType,
			OutputMessage outputMessage) throws IOException,
			MessageConvertException {
		T t = (T) body;
		if (outputMessage.getContentType() == null) {
			MimeType contentTypeToUse = contentType;
			if (contentType == null || contentType.isWildcardType()
					|| contentType.isWildcardSubtype()) {
				contentTypeToUse = getDefaultContentType(t);
			} else if (MimeTypeUtils.APPLICATION_OCTET_STREAM
					.equals(contentType)) {
				MimeType mediaType = getDefaultContentType(t);
				contentTypeToUse = (mediaType != null ? mediaType
						: contentTypeToUse);
			}
			if (contentTypeToUse != null) {
				if (contentTypeToUse.getCharset() == null) {
					Charset defaultCharset = getDefaultCharset();
					if (defaultCharset != null) {
						contentTypeToUse = new MimeType(contentTypeToUse,
								defaultCharset);
					}
				}
				outputMessage.setContentType(contentTypeToUse);
			}
		}

		if (outputMessage.getContentLength() < 0) {
			Headers headers = outputMessage.getHeaders();
			if (headers != null
					&& headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
				Long contentLength = getContentLength(t,
						outputMessage.getContentType());
				if (contentLength != null) {
					outputMessage.setContentLength(contentLength);
				}
			}
		}
		writeInternal(t, contentType, outputMessage);
	}

	protected MimeType getDefaultContentType(T body) throws IOException {
		return first();
	}

	protected Long getContentLength(T body, MimeType contentType)
			throws IOException {
		return null;
	}

	protected Charset getCharset(Message message) {
		MimeType mimeType = message.getContentType();
		if (mimeType == null) {
			return getDefaultCharset();
		}

		Charset charset = mimeType.getCharset();
		if (charset == null) {
			return getDefaultCharset();
		}
		return charset;
	}

	protected String readTextBody(InputMessage inputMessage) throws IOException {
		return IOUtils.toString(inputMessage.getBody(), getCharset(inputMessage).name());
	}

	protected void writeTextBody(String text, MimeType contentType,
			OutputMessage outputMessage) throws IOException {
		if(text == null){
			return ;
		}
		
		IOUtils.write(text, outputMessage.getBody(), getCharset(outputMessage)
				.name());
	}

	protected abstract T readInternal(Type type, InputMessage inputMessage)
			throws IOException, MessageConvertException;

	protected abstract void writeInternal(T body, MimeType contentType,
			OutputMessage outputMessage) throws IOException,
			MessageConvertException;
}
