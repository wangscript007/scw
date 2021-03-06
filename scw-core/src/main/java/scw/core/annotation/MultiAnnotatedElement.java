package scw.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import scw.core.utils.ArrayUtils;
import scw.core.utils.CollectionUtils;

public class MultiAnnotatedElement implements AnnotatedElement {
	private final Collection<? extends AnnotatedElement> annotatedElements;
	
	public MultiAnnotatedElement(AnnotatedElement ...annotatedElements) {
		this(Arrays.asList(annotatedElements));
	}

	public MultiAnnotatedElement(
			Collection<? extends AnnotatedElement> annotatedElements) {
		this.annotatedElements = annotatedElements;
	}

	public static Annotation[] toAnnotations(
			Collection<? extends AnnotatedElement> annotatedElements,
			boolean isDeclared) {
		if (CollectionUtils.isEmpty(annotatedElements)) {
			return AnnotatedElementUtils.EMPTY_ANNOTATION_ARRAY;
		}

		LinkedHashSet<Annotation> annotations = new LinkedHashSet<Annotation>();
		for (AnnotatedElement annotatedElement : annotatedElements) {
			if (annotatedElement == null) {
				continue;
			}

			Annotation[] as = isDeclared ? annotatedElement
					.getDeclaredAnnotations() : annotatedElement
					.getAnnotations();
			if (ArrayUtils.isEmpty(as)) {
				continue;
			}

			annotations.addAll(new AnnotatedElementNoCopyList<Annotation>(as));
		}
		return annotations.toArray(new Annotation[annotations.size()]);
	}

	public static Annotation[] toAnnotations(boolean isDeclared,
			AnnotatedElement... annotatedElements) {
		return toAnnotations(Arrays.asList(annotatedElements), isDeclared);
	}

	public static AnnotatedElement forAnnotatedElements(
			Collection<AnnotatedElement> annotatedElements, boolean isDeclared) {
		return AnnotatedElementUtils.forAnnotations(toAnnotations(
				annotatedElements, isDeclared));
	}

	public static AnnotatedElement forAnnotatedElements(boolean isDeclared,
			AnnotatedElement... annotatedElements) {
		return AnnotatedElementUtils.forAnnotations(toAnnotations(isDeclared,
				annotatedElements));
	}

	public Collection<AnnotatedElement> getAnnotatedElements() {
		if (annotatedElements == null) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableCollection(annotatedElements);
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		for (AnnotatedElement annotatedElement : getAnnotatedElements()) {
			if (annotatedElement == null) {
				continue;
			}

			T annotation = annotatedElement.getAnnotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	public Annotation[] getAnnotations() {
		return toAnnotations(getAnnotatedElements(), false);
	}

	public Annotation[] getDeclaredAnnotations() {
		return toAnnotations(getAnnotatedElements(), true);
	}

	private static final class AnnotatedElementNoCopyList<E> extends
			AbstractList<E> {
		private final E[] a;

		AnnotatedElementNoCopyList(E[] array) {
			a = array;
		}

		@Override
		public int size() {
			return a.length;
		}

		@Override
		public Object[] toArray() {
			return a;
		}

		@Override
		public E get(int index) {
			return a[index];
		}
	}
}
