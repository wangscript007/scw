package scw.http.server;

import scw.beans.annotation.AopEnable;
import scw.http.HttpUtils;
import scw.http.multipart.FileItemParser;

@AopEnable(false)
public class HttpServiceConfigAccessor {
	private FileItemParser fileItemParser;
	private final HttpServiceConfig<Boolean> jsonpSupportConfig = new HttpServiceConfig<Boolean>();
	private final HttpServiceConfig<Boolean> jsonSupportWrapperConfig = new HttpServiceConfig<Boolean>();
	private final HttpServiceConfig<Boolean> multipartFormSupportWrapperConfig = new HttpServiceConfig<Boolean>();
	
	public FileItemParser getFileItemParser() {
		return fileItemParser == null ? HttpUtils.getFileItemParser() : fileItemParser;
	}

	public void setFileItemParser(FileItemParser fileItemParser) {
		this.fileItemParser = fileItemParser;
	}

	public final HttpServiceConfig<Boolean> getJsonpSupportConfig() {
		return jsonpSupportConfig;
	}

	public final HttpServiceConfig<Boolean> getJsonSupportWrapperConfig() {
		return jsonSupportWrapperConfig;
	}

	public HttpServiceConfig<Boolean> getMultipartFormSupportWrapperConfig() {
		return multipartFormSupportWrapperConfig;
	}

	public <V> V getConfig(HttpServiceConfig<V> config, ServerHttpRequest request, V defaultValue) {
		if (config == null) {
			return defaultValue;
		}

		V v = config.getConfig(request);
		return v == null ? defaultValue : v;
	}

	public boolean isSupportJsonWrapper(ServerHttpRequest request) {
		return getConfig(getJsonSupportWrapperConfig(), request, true);
	}

	public boolean isSupportJsonp(ServerHttpRequest request) {
		return getConfig(getJsonpSupportConfig(), request, true);
	}
}
