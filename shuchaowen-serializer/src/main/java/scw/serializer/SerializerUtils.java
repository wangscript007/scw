package scw.serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import scw.core.Bits;
import scw.core.instance.InstanceUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.SystemPropertyUtils;
import scw.io.IOUtils;
import scw.io.UnsafeByteArrayOutputStream;
import scw.util.FormatUtils;

public final class SerializerUtils {
	static {
		Class<?> serializerClass = null;
		String[] seralizerClassNames = SystemPropertyUtils.getArrayProperty(
				String.class, "serializer.support.class", new String[] {
						"scw.serializer.hessian.Hessian2Serializer",
						"scw.serializer.hessian.HessianSerializer" });

		for (String name : seralizerClassNames) {
			try {
				serializerClass = ClassUtils.forName(name);
				break;
			} catch (Throwable e) {
			}
		}

		DEFAULT_SERIALIZER = (Serializer) (serializerClass == null ? JavaSerializer.SERIALIZER
				: InstanceUtils.getInstance(serializerClass));
		FormatUtils.info(SerializerUtils.class, "default serializer："
				+ (serializerClass == null ? JavaSerializer.class.getName()
						: serializerClass.getName()));
	}

	/**
	 * 默认的序列化实现
	 */
	public static final Serializer DEFAULT_SERIALIZER;

	private SerializerUtils() {
	}

	public static byte[] class2bytes(Class<?> clazz) {
		String name = clazz.getName();
		UnsafeByteArrayOutputStream byteArray = null;
		try {
			byteArray = new UnsafeByteArrayOutputStream();
			for (char c : name.toCharArray()) {
				byte[] bs = { 0, 0 };
				Bits.putChar(bs, 0, c);
				byteArray.write(bs, 0, bs.length);
			}
			return byteArray.toByteArray();
		} finally {
			IOUtils.close(byteArray);
		}
	}

	public static Class<?> bytes2class(byte[] bytes)
			throws ClassNotFoundException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i += 2) {
			char c = Bits.getChar(bytes, i);
			sb.append(c);
		}
		return ClassUtils.forName(sb.toString(),
				ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 使用序列化来实现对象拷贝
	 * 
	 * @param obj
	 * @return
	 */
	public static <T> T clone(T obj) {
		if (obj == null) {
			return null;
		}

		return DEFAULT_SERIALIZER
				.deserialize(DEFAULT_SERIALIZER.serialize(obj));
	}

	public static <T> T readObject(File file) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return SerializerUtils.DEFAULT_SERIALIZER.deserialize(fis);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	public static void writeObject(File file, Object obj) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			SerializerUtils.DEFAULT_SERIALIZER.serialize(fos, obj);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}
}
