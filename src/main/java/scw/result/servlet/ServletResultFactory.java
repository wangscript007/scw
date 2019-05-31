package scw.result.servlet;

import scw.result.DataResult;
import scw.result.DefaultResultFactory;

public final class ServletResultFactory extends DefaultResultFactory {
	private final String contentType;

	public ServletResultFactory(String propertiesFilePath, String charsetName, int defaultErrorCode,
			int defaultSuccessCode, int loginExpiredCode, int parameterErrorCode, String contentType) {
		super(propertiesFilePath, charsetName, defaultErrorCode, defaultSuccessCode, loginExpiredCode,
				parameterErrorCode);
		this.contentType = contentType;
	}

	public <T> DataResult<T> success(int code, T data, String msg) {
		return new ServletTextResult<T>(true, code, data, msg, contentType);
	}

	public <T> DataResult<T> error(int code, String msg) {
		return new ServletTextResult<T>(false, code, null, msg, contentType);
	}
}
