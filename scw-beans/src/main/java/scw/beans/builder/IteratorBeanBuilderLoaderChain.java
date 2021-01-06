package scw.beans.builder;

import java.util.Collection;
import java.util.Iterator;

import scw.core.utils.CollectionUtils;

public class IteratorBeanBuilderLoaderChain extends AbstractBeanBuilderLoaderChain {
	private Iterator<BeanBuilderLoader> iterator;

	public IteratorBeanBuilderLoaderChain(Collection<BeanBuilderLoader> loaders) {
		this(loaders, null);
	}

	public IteratorBeanBuilderLoaderChain(Collection<BeanBuilderLoader> loaders, BeanBuilderLoaderChain chain) {
		super(chain);
		if (!CollectionUtils.isEmpty(loaders)) {
			iterator = loaders.iterator();
		}
	}

	@Override
	protected BeanBuilderLoader getNext(LoaderContext context) {
		if (iterator == null) {
			return null;
		}

		if (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}
}