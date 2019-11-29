package scw.core.attribute;

import java.util.Enumeration;

public interface AttributesReadOnly<K, V> {
	V getAttribute(K name);

	Enumeration<K> getAttributeNames();
}
