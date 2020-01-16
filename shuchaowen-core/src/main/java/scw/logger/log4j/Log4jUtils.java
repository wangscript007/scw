package scw.logger.log4j;

import java.lang.reflect.Method;
import java.util.Properties;

import org.w3c.dom.Element;

import scw.core.Constants;
import scw.core.reflect.ReflectionUtils;
import scw.core.resource.ResourceUtils;
import scw.core.utils.StringUtils;
import scw.core.utils.SystemPropertyUtils;
import scw.core.utils.XMLUtils;
import scw.lang.NotSupportException;
import scw.logger.Level;
import scw.logger.LoggerLevelUtils;
import scw.logger.LoggerUtils;
import scw.util.FormatUtils;
import scw.util.KeyValuePair;

public final class Log4jUtils {
	private static final String LOG4J_PATH = "scw_log4j";
	private static final String LOG4J_APPEND_PATH = "/log4j-append.properties";

	private Log4jUtils() {
	}

	public static void setLog4jPath(String path) {
		SystemPropertyUtils.setPrivateProperty(LOG4J_PATH, path);
	}

	private static void initByProperties(Properties properties) {
		if (properties == null) {
			return;
		}

		Method method = ReflectionUtils.getMethod("org.apache.log4j.PropertyConfigurator", "configure",
				Properties.class);
		if (method == null) {
			return;
		}

		try {
			method.invoke(null, properties);
		} catch (Exception e) {
		}
	}

	private static void initByXml(Element element) {
		if (element == null) {
			return;
		}

		Method method = ReflectionUtils.getMethod("org.apache.log4j.xml.DOMConfigurator", "configure", Element.class);
		if (method == null) {
			return;
		}

		try {
			method.invoke(null, element);
		} catch (Exception e) {
		}
	}

	public static void defaultInit() {
		Boolean enable = LoggerUtils.defaultConfigEnable();
		if (enable == null) {
			throw new NotSupportException("不支持log4j");
		}

		if (!enable) {
			return;
		}

		String path = SystemPropertyUtils.getProperty(LOG4J_PATH);
		if (StringUtils.isEmpty(path)) {
			if (ResourceUtils.getResourceOperations().isExist("classpath:/log4j.properties")) {
				Properties properties = ResourceUtils.getResourceOperations()
						.getProperties("classpath:/log4j.properties");
				initByProperties(properties);
				return;
			} else if (ResourceUtils.getResourceOperations().isExist("classpath:/log4j.xml")) {
				Element element = XMLUtils.getRootElement("classpath:/log4j.xml");
				initByXml(element);
				return;
			}
		} else {
			if (ResourceUtils.getResourceOperations().isExist(path)) {
				if (path.endsWith(".properties")) {
					Properties properties = ResourceUtils.getResourceOperations().getProperties(path);
					initByProperties(properties);
					return;
				} else if (path.endsWith(".xml")) {
					Element element = XMLUtils.getRootElement(path);
					initByXml(element);
					return;
				}
			}
		}

		String rootPath = SystemPropertyUtils.getWorkPath();
		FormatUtils.info(Log4jUtils.class, "load the default log directory: {}", rootPath);
		Properties properties = new Properties();
		properties.put("log4j.rootLogger", LoggerLevelUtils.getDefaultLevel().name() + ", stdout, max-warn, logfile, warn");
		properties.put("log4j.appender.max-warn", "scw.logger.log4j.MaxWarnConsoleApperder");
		properties.put("log4j.appender.max-warn.layout.ConversionPattern", "%d %p [%c] - %m%n");
		properties.put("log4j.appender.max-warn.layout", "org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.max-warn.Target", "System.out");
		
		properties.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		properties.put("log4j.appender.stdout.layout.ConversionPattern", "%d %p [%c] - %m%n");
		properties.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.stdout.Threshold", "WARN");
		properties.put("log4j.appender.stdout.Target", "System.err");
		
		properties.put("log4j.appender.logfile", "org.apache.log4j.DailyRollingFileAppender");
		properties.put("log4j.appender.logfile.File", rootPath + "/logs/log.log");
		properties.put("log4j.appender.logfile.layout", "org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.logfile.DatePattern", "'.'yyyy-MM-dd");
		properties.put("log4j.appender.logfile.layout.ConversionPattern", "%d %p [%c] - %m%n");
		properties.put("log4j.appender.warn", "org.apache.log4j.DailyRollingFileAppender");
		properties.put("log4j.appender.warn.Encoding", Constants.DEFAULT_CHARSET_NAME);
		properties.put("log4j.appender.warn.Threshold", "WARN");
		properties.put("log4j.appender.warn.File", rootPath + "/logs/error_warn.log");
		properties.put("log4j.appender.warn.layout", "org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.warn.DatePattern", "'.'yyyy-MM-dd");
		properties.put("log4j.appender.warn.layout.ConversionPattern", "%d %p [%c] - %m%n");

		// 过滤无用日志
		properties.put("log4j.logger.org.apache.dubbo", "error");
		properties.put("log4j.logger.org.apache.curator", "warn");
		properties.put("log4j.logger.net.rubyeye.xmemcached", "warn");
		properties.put("log4j.logger.org.apache.zookeeper", "warn");
		properties.put("log4j.logger.com.alibaba.druid", "warn");

		for (KeyValuePair<String, Level> entry : LoggerLevelUtils.getLevelConfigList()) {
			properties.put("log4j.logger." + entry.getKey(), entry.getValue().name());
		}

		if (ResourceUtils.getResourceOperations().isExist(LOG4J_APPEND_PATH)) {
			FormatUtils.info(Log4jUtils.class, "loading " + LOG4J_APPEND_PATH);
			Properties append = ResourceUtils.getResourceOperations().getProperties(LOG4J_APPEND_PATH);
			properties.putAll(append);
		}

		initByProperties(properties);
	}
}
