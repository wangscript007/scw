package scw.event.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import scw.core.utils.CollectionUtils;
import scw.event.EventType;
import scw.event.KeyValuePairEvent;
import scw.event.NamedEventDispatcher;
import scw.util.DefaultGenericMap;

public class ObservableMap<K, V> extends DefaultGenericMap<K, V> {
	private final NamedEventDispatcher<K, KeyValuePairEvent<K, V>> eventDispatcher;
	private final boolean concurrent;

	public ObservableMap(boolean concurrent) {
		this(concurrent, new DefaultNamedEventDispatcher<K, KeyValuePairEvent<K, V>>(concurrent));
	}

	public ObservableMap(boolean concurrent, NamedEventDispatcher<K, KeyValuePairEvent<K, V>> eventDispatcher) {
		super(concurrent);
		this.concurrent = concurrent;
		this.eventDispatcher = eventDispatcher;
	}

	public NamedEventDispatcher<K, KeyValuePairEvent<K, V>> getEventDispatcher() {
		return eventDispatcher;
	}

	public boolean isSupportedConcurrent() {
		return getTargetMap() instanceof ConcurrentMap;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(super.entrySet());
	}

	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(super.keySet());
	}

	@Override
	public Collection<V> values() {
		return Collections.unmodifiableCollection(super.values());
	}

	@Override
	public V put(K key, V value) {
		V v;
		if (isSupportedConcurrent()) {
			v = super.put(key, value);
		} else {
			synchronized (this) {
				v = super.put(key, value);
			}
		}

		KeyValuePairEvent<K, V> event = null;
		if (v == null) {
			event = new KeyValuePairEvent<K, V>(EventType.CREATE, key, value);
		} else {
			if (!v.equals(value)) {
				event = new KeyValuePairEvent<K, V>(EventType.UPDATE, key, value);
			}
		}

		if (event != null) {
			getEventDispatcher().publishEvent(key, event);
		}
		return v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		K keyToUse = (K)key;
		V v;
		if (isSupportedConcurrent()) {
			v = super.remove(keyToUse);
		} else {
			synchronized (this) {
				v = super.remove(keyToUse);
			}
		}

		if (v != null) {
			eventDispatcher.publishEvent(keyToUse, new KeyValuePairEvent<K, V>(EventType.DELETE, keyToUse, v));
		}
		return v;
	}

	@Override
	public void clear() {
		Map<K, V> cloneMap;
		if (isSupportedConcurrent()) {
			cloneMap = new HashMap<K, V>(this);
			super.clear();
		} else {
			synchronized (this) {
				cloneMap = new HashMap<K, V>(this);
				super.clear();
			}
		}

		for (Entry<K, V> entry : cloneMap.entrySet()) {
			getEventDispatcher().publishEvent(entry.getKey(), new KeyValuePairEvent<K, V>(EventType.DELETE, entry.getKey(), entry.getValue()));
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		V v;
		if (isSupportedConcurrent()) {
			v = super.putIfAbsent(key, value);
		} else {
			synchronized (this) {
				v = super.putIfAbsent(key, value);
			}
		}

		if (v != null) {
			getEventDispatcher().publishEvent(key, new KeyValuePairEvent<K, V>(EventType.CREATE, key, value));
		}
		return v;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (CollectionUtils.isEmpty(m)) {
			return;
		}

		for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public boolean isConcurrent() {
		return concurrent;
	}
}
