package scw.net.message;

import java.nio.charset.Charset;

import scw.core.Constants;
import scw.core.utils.StringUtils;
import scw.http.HttpHeaders;
import scw.net.MimeType;
import scw.net.MimeTypeUtils;

public abstract class AbstractMessage implements Message {
	public long getContentLength() {
		String len = getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
		return StringUtils.hasLength(len) ? StringUtils.parseLong(len) : -1;
	}

	public MimeType getContentType() {
		String contentType = getRawContentType();
		return StringUtils.hasLength(contentType) ? MimeTypeUtils.parseMimeType(contentType) : null;
	}

	public String getRawContentType() {
		return getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
	}

	public Charset getDefaultCharset() {
		MimeType mimeType = getContentType();
		if (mimeType == null) {
			return Constants.DEFAULT_CHARSET;
		}

		Charset charset = mimeType.getCharset();
		return charset == null ? Constants.DEFAULT_CHARSET : charset;
	}
}
