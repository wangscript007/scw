package scw.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import scw.core.Assert;
import scw.core.utils.CollectionUtils;
import scw.core.utils.ObjectUtils;
import scw.event.EventListener;
import scw.event.EventType;
import scw.io.event.ResourceEvent;
import scw.io.event.ResourceEventDispatcher;
import scw.io.event.SimpleResourceEventDispatcher;

public class AutomaticResource extends ResourceWrapper {
	private Collection<Resource> resources;
	private volatile Resource currentResource;
	
	public AutomaticResource(Resource ...resources) {
		this(Arrays.asList(resources));
	}

	/**
	 * 从多个resource中自动选择一个可用的
	 * @param resources 使用优先级从高到低
	 */
	public AutomaticResource(Collection<Resource> resources) {
		Assert.requiredArgument(!CollectionUtils.isEmpty(resources), "resources");
		this.resources = resources;
		this.currentResource = getCurrentResource();
	}

	private Resource getCurrentResource() {
		Iterator<Resource> iterator = this.resources.iterator();
		while (iterator.hasNext()) {
			Resource resource = iterator.next();
			if (resource.exists() || !iterator.hasNext()) {
				return resource;
			}
		}
		throw new RuntimeException("It's impossible to be here");
	}

	@Override
	public Resource getResource() {
		return currentResource;
	}

	private volatile ResourceEventDispatcher eventDispatcher;

	@Override
	public ResourceEventDispatcher getEventDispatcher() {
		if (eventDispatcher == null) {
			synchronized (this) {
				if (eventDispatcher == null) {
					eventDispatcher = new SimpleResourceEventDispatcher();
					for (Resource resource : resources) {
						if (resource == null) {
							continue;
						}

						if (resource.isSupportEventDispatcher()) {
							resource.getEventDispatcher().registerListener(new EventListener<ResourceEvent>() {
								public void onEvent(ResourceEvent event) {
									currentResource = getCurrentResource();
									if (ObjectUtils.nullSafeEquals(event.getSource(), currentResource)) {
										eventDispatcher.publishEvent(event);
									}else{
										eventDispatcher.publishEvent(new ResourceEvent(EventType.UPDATE, currentResource));
									}
								}
							});
						}
					}
				}
			}
		}
		return eventDispatcher;
	}

	@Override
	public boolean isSupportEventDispatcher() {
		return true;
	}
}
