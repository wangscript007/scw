package shuchaowen.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shuchaowen.common.ClassInfo;
import shuchaowen.common.FieldInfo;
import shuchaowen.common.StringFormatSystemProperties;
import shuchaowen.common.io.decoder.StringLineDecoder;

public final class ConfigUtils {
	private static final String WEB_ROOT = "web.root";
	private static final String CLASSPATH = "classpath";
	private static final String CLASSPATH_PREFIX = CLASSPATH + ":";
	private static final String WEB_INF = "WEB-INF";
	public static final StringFormatSystemProperties format1 = new StringFormatSystemProperties("{", "}");
	public static final StringFormatSystemProperties format2 = new StringFormatSystemProperties("[", "]");
	public static final String CONFIG_SUFFIX = "SHUCHAOWEN_CONFIG_SUFFIX";

	private ConfigUtils() {
	};

	public static String getSystemProperty(String key) {
		String v = System.getProperty(key);
		if (v == null) {
			v = System.getenv(key);
		}

		if (v == null) {
			if (WEB_ROOT.equalsIgnoreCase(key)) {
				return getWorkPath();
			} else if (CLASSPATH.equalsIgnoreCase(key)) {
				return getClassPath();
			}
		}
		return v;
	}

	public static String format(String value) {
		if (StringUtils.isNull(value)) {
			return value;
		}

		String newPath = format1.format(value);
		newPath = format2.format(newPath);

		if (newPath.length() < CLASSPATH_PREFIX.length()) {
			return newPath;
		}

		String prefix = newPath.substring(0, CLASSPATH_PREFIX.length());
		prefix = prefix.toUpperCase();
		if (prefix.equals(CLASSPATH_PREFIX.toUpperCase())) {
			return getClassPath() + newPath.substring(CLASSPATH_PREFIX.length());
		}
		return newPath;
	}

	public static String getWorkPath() {
		String path = ConfigUtils.class.getResource("/").getPath();
		File file = new File(path);
		file = file.getParentFile().getParentFile();
		File f = FileUtils.searchDirectory(file.getPath(), WEB_INF);
		if (f != null && f.exists()) {
			f = f.getParentFile();
		}
		return f == null ? file.getPath() : f.getPath();
	}

	public static String getClassPath() {
		return ConfigUtils.class.getResource("/").getPath();
	}

	public static <T> T parseObject(Map<String, String> map, Class<T> clz)
			throws InstantiationException, IllegalAccessException, NoSuchFieldException, SecurityException,
			IllegalArgumentException, InvocationTargetException {
		ClassInfo classInfo = ClassUtils.getClassInfo(clz);
		T t = clz.newInstance();
		for (Entry<String, String> entry : map.entrySet()) {
			FieldInfo fieldInfo = classInfo.getFieldInfo(entry.getKey());
			if (fieldInfo == null) {
				continue;
			}
			fieldInfo.set(t, StringUtils.conversion(entry.getValue(), fieldInfo.getType()));
		}
		return t;
	}

	public static String searchFileName(String fileName) {
		return FileUtils.searchFileName(fileName, getClassPath(), true);
	}

	private static String getFilePath(final String filePath) {
		String path = format(filePath);
		File file = new File(path);
		if (file.exists()) {
			return path;
		} else {// 如果找不到目录就到classpath下去找
			String classPath = getClassPath() + path;
			file = new File(classPath);
			if (file.exists()) {
				return classPath;
			} else {// 如果clsspath下找不到就去workpath下去找
				String workPath = getWorkPath() + path;
				file = new File(workPath);
				if (file.exists()) {
					return workPath;
				} else {
					return path;
				}
			}
		}
	}

	public static File getFile(String filePath) {
		String configSuffix = getSystemProperty(CONFIG_SUFFIX);
		if (StringUtils.isNull(configSuffix)) {
			return getFile(filePath, null);
		} else {
			return getFile(filePath, Arrays.asList(StringUtils.commonSplit(configSuffix)));
		}
	}

	public static File getFile(String filePath, Collection<String> testSuffix) {
		File file = new File(getFilePath(filePath));
		if (testSuffix == null || testSuffix.isEmpty()) {
			return file;
		}

		for (String sf : testSuffix) {
			File testFile = new File(file.getParent() + File.separator + getTestFileName(file.getName(), sf));
			if (testFile.exists()) {
				return testFile;
			}
		}
		return file;
	}

	private static String getTestFileName(String fileName, String str) {
		int index = fileName.indexOf(".");
		if (index == -1) {// 不存在
			return fileName + str;
		} else {
			return fileName.substring(0, index) + str + fileName.substring(index);
		}
	}

	public static final Properties getProperties(File file, String charsetName) {
		Properties properties = new Properties();
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(new FileInputStream(file), charsetName);
			properties.load(isr);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			XUtils.close(isr);
		}
		return properties;
	}

	public static List<String> getFileContentLineList(File file, String charsetName) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return new StringLineDecoder(Charset.forName(charsetName)).decode(fis);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			XUtils.close(fis);
		}
		return null;
	}

	public static List<Map<String, String>> getDefaultXmlContent(File file, String rootTag) {
		if (rootTag == null) {
			throw new NullPointerException("rootTag is null");
		}

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		try {
			try {
				Document doc = XMLUtils.parse(file);
				Element root = doc.getDocumentElement();
				NodeList nhosts = root.getChildNodes();
				for (int x = 0; x < nhosts.getLength(); x++) {
					Node nRoot = nhosts.item(x);
					if (nRoot.getNodeName().equals(rootTag)) {
						list.add(XMLUtils.xmlToMap(nRoot));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static <T> List<T> xmlToList(Class<T> type, File file) {
		List<Map<String, String>> list = ConfigUtils.getDefaultXmlContent(file, "config");
		List<T> objList = new ArrayList<T>();
		try {
			for (Map<String, String> map : list) {
				objList.add(ConfigUtils.parseObject(map, type));
			}
			return objList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> xmlToMap(Class<V> valueType, File file) {
		try {
			Field keyField = null;
			for (Field f : valueType.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())) {
					continue;
				}

				keyField = f;
				break;
			}

			if (keyField == null) {
				throw new NullPointerException("打不到主键字段");
			}

			List<Map<String, String>> list = ConfigUtils.getDefaultXmlContent(file, "config");
			Map<K, V> map = new HashMap<K, V>();
			for (Map<String, String> tempMap : list) {
				Object obj = ConfigUtils.parseObject(tempMap, valueType);
				keyField.setAccessible(true);
				Object kV = keyField.get(obj);
				keyField.setAccessible(false);
				if (map.containsKey(kV)) {
					throw new NullPointerException(
							"已经存在的key=" + keyField.getName() + ",value=" + kV + ", filePath=" + file.getPath());
				}
				map.put((K) kV, (V) obj);
			}

			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Properties getProperties(String filePath, String charsetName) {
		return getProperties(getFile(filePath), charsetName);
	}

	public static List<String> getFileContentLineList(String filePath, String charsetName) {
		return getFileContentLineList(getFile(filePath), charsetName);
	}

	public static String getFileContent(String filePath, String charsetName) {
		return FileUtils.readerFileContent(getFile(filePath), charsetName).toString();
	}
}