package scw.net;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class MimeTypes implements Comparator<MimeType>, Iterable<MimeType> {
	private final TreeSet<MimeType> mimeTypes = new TreeSet<MimeType>(this);
	private boolean readyOnly;

	public Iterator<MimeType> iterator() {
		return Collections.unmodifiableCollection(mimeTypes).iterator();
	}

	public final SortedSet<MimeType> getMimeTypes() {
		return readyOnly ? Collections.unmodifiableSortedSet(mimeTypes)
				: mimeTypes;
	}

	public final MimeTypes add(MimeType... mimeTypes) {
		for (MimeType mimeType : mimeTypes) {
			getMimeTypes().add(mimeType);
		}
		return this;
	}

	public final boolean isReadyOnly() {
		return readyOnly;
	}

	public final MimeTypes readyOnly() {
		this.readyOnly = true;
		return this;
	}

	public int compare(MimeType o1, MimeType o2) {
		return MimeTypeUtils.SPECIFICITY_COMPARATOR.compare(o1, o2);
	}
}
