package scw.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import scw.core.Assert;
import scw.core.utils.ClassUtils;
import scw.core.utils.StringUtils;
import scw.util.ClassLoaderProvider;
import scw.util.DefaultClassLoaderProvider;

public class DefaultResourceLoader implements ConfigurableResourceLoader {
	private ClassLoaderProvider classLoaderProvider;
	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<ProtocolResolver>(4);
	private final Set<ResourceLoader> resourceLoaders = new LinkedHashSet<ResourceLoader>(4);
	
	public DefaultResourceLoader() {
	}

	public DefaultResourceLoader(ClassLoader classLoader) {
		this(new DefaultClassLoaderProvider(classLoader));
	}
	
	public DefaultResourceLoader(ClassLoaderProvider classLoaderProvider){
		this.classLoaderProvider = classLoaderProvider;
	}

	public void setClassLoaderProvider(ClassLoaderProvider classLoaderProvider) {
		this.classLoaderProvider = classLoaderProvider;
	}

	public ClassLoader getClassLoader() {
		return ClassUtils.getClassLoader(classLoaderProvider);
	}

	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	public Collection<ResourceLoader> getResourceLoaders() {
		return resourceLoaders;
	}

	public void addResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoaders.add(resourceLoader);
	}

	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		for (ProtocolResolver protocolResolver : this.protocolResolvers) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		for (ResourceLoader resourceLoader : this.resourceLoaders) {
			Resource resource = resourceLoader.getResource(location);
			if (resource != null) {
				return resource;
			}
		}

		if (location.startsWith("/")) {
			return getResourceByPath(location);
		} else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		} else {
			try {
				// Try to parse the location as a URL...
				URL url = new URL(location);
				return new UrlResource(url);
			} catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}
	}

	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}

	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, ClassLoader classLoader) {
			super(path, classLoader);
		}

		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
