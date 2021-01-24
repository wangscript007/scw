package scw.io;

public interface ConfigurableResourceLoader extends ResourceLoader {
	void addProtocolResolver(ProtocolResolver resolver);

	void addResourceLoader(ResourceLoader resourceLoader);
}
