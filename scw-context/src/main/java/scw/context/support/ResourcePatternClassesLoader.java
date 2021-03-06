package scw.context.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import scw.io.Resource;
import scw.io.ResourceLoader;
import scw.io.ResourcePatternResolver;
import scw.io.support.PathMatchingResourcePatternResolver;

public class ResourcePatternClassesLoader<S> extends
		AbstractResourceClassesLoader<S> {
	static final String CLASSES_SUFFIX = "**/*" + SUFFIX;
	private final String locationPattern;

	public ResourcePatternClassesLoader(String locationPattern) {
		this.locationPattern = locationPattern.endsWith(SUFFIX) ? locationPattern
				: (locationPattern + CLASSES_SUFFIX);
	}

	public ResourcePatternResolver getResourcePatternResolver(
			ResourceLoader resourceLoader, ClassLoader classLoader) {
		return new PathMatchingResourcePatternResolver(resourceLoader);
	}

	@Override
	protected Collection<Resource> getResources(ResourceLoader resourceLoader,
			ClassLoader classLoader) throws IOException {
		Resource[] resources = getResourcePatternResolver(resourceLoader,
				classLoader).getResources(locationPattern);
		if (resources == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(resources);
	}
}
