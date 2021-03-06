package scw.core.type.filter;

import java.io.IOException;

import scw.core.type.ClassMetadata;
import scw.core.type.classreading.MetadataReader;
import scw.core.type.classreading.MetadataReaderFactory;

/**
 * Type filter that exposes a
 * {@link org.springframework.core.type.ClassMetadata} object
 * to subclasses, for class testing purposes.
 *
 * @see #match(org.springframework.core.type.ClassMetadata)
 */
public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

	public final boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {

		return match(metadataReader.getClassMetadata());
	}

	/**
	 * Determine a match based on the given ClassMetadata object.
	 * @param metadata the ClassMetadata object
	 * @return whether this filter matches on the specified type
	 */
	protected abstract boolean match(ClassMetadata metadata);

}
