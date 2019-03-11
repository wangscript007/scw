package scw.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicLong;

import scw.common.utils.XTime;
import scw.common.utils.XUtils;

public final class FileManager {
	private final String rootPath;
	private final AtomicLong atomicLong = new AtomicLong();

	public FileManager(String rootPath) {
		this.rootPath = rootPath;
		File file = new File(rootPath);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public String getRootPath() {
		return rootPath;
	}

	public File createRandomFileWriteObject(Object bean) throws IOException {
		long number = atomicLong.incrementAndGet();
		if (number < 0) {
			number = Long.MAX_VALUE + number;
		}
		return createFileAndWriteObject(XTime.format(System.currentTimeMillis(), "yyyyMMddHHmmss") + number, bean);
	}

	public File createFileAndWriteObject(String fileName, Object bean) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(rootPath);
		sb.append(File.separator);
		sb.append(fileName);
		File file = new File(sb.toString());
		if (file.isDirectory()) {
			if (!file.exists()) {
				file.mkdirs();
			}
		} else {
			if (!file.exists()) {
				file.createNewFile();
			}
		}

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(bean);
		} finally {
			XUtils.close(oos, fos);
		}
		return file;
	}
}
