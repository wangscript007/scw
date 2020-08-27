package scw.core.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

public class DefaultParameterDescriptors<T> implements ParameterDescriptors {
	private final T source;
	private final Class<?> declaringClass;
	private final String[] names;
	private final Annotation[][] annotations;
	private final Type[] genericTypes;
	private final Class<?> types[];

	public DefaultParameterDescriptors(Class<?> declaringClass, T source, String[] names, Annotation[][] annotations,
			Type[] genericTypes, Class<?>[] types) {
		this.source = source;
		this.declaringClass = declaringClass;
		this.names = names;
		this.annotations = annotations;
		this.genericTypes = genericTypes;
		this.types = types;
	}

	public T getSource() {
		return source;
	}

	public Class<?> getDeclaringClass() {
		return declaringClass;
	}

	public Iterator<ParameterDescriptor> iterator() {
		return new InternalIterator();
	}

	public int size() {
		return names.length;
	}

	private class InternalIterator implements Iterator<ParameterDescriptor> {
		private int index = 0;

		public boolean hasNext() {
			return index < names.length;
		}

		public ParameterDescriptor next() {
			return getParameterDescriptor(index++);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}

	public Class<?>[] getTypes() {
		return types;
	}

	public ParameterDescriptor getParameterDescriptor(int index) {
		return new DefaultParameterDescriptor(names[index], annotations[index], types[index], genericTypes[index]);
	}

	public ParameterDescriptor getParameterDescriptor(String name) {
		for (ParameterDescriptor parameterDescriptor : this) {
			if (parameterDescriptor.getName().equals(name)) {
				return parameterDescriptor;
			}
		}
		return null;
	}
}
