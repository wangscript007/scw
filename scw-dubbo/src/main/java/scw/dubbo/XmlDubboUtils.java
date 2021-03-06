package scw.dubbo;

import java.util.ArrayList;
import java.util.List;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scw.context.ClassesLoaderFactory;
import scw.core.annotation.AnnotationUtils;
import scw.core.utils.StringUtils;
import scw.dom.DomUtils;
import scw.env.Environment;
import scw.instance.InstanceUtils;
import scw.instance.NoArgsInstanceFactory;
import scw.logger.Logger;
import scw.logger.LoggerUtils;
import scw.mapper.Copy;
import scw.mapper.Field;
import scw.mapper.Fields;
import scw.mapper.FilterFeature;
import scw.mapper.MapperUtils;

public final class XmlDubboUtils {
	private static final String TAG_NAME_PREFIX = "dubbo:";

	private static final String DUBBO_SERVICE_REF = "ref";

	private static final String DUBBO_SCAN_PACKAGE = "package";

	private static final String DEFAULT_PROTOCOL_NAME = "dubbo";

	private static Logger logger = LoggerUtils.getLogger(XmlDubboUtils.class);

	private XmlDubboUtils() {
	};

	public static List<ApplicationConfig> parseApplicationConfigList(final Environment environment,
			NodeList nodeList, ApplicationConfig defaultConfig) {
		return parseConfigList(ApplicationConfig.class, environment, nodeList, defaultConfig,
				new ConfigFilter<ApplicationConfig>() {
					@Override
					public boolean doFilter(List<ApplicationConfig> list, Node node, ApplicationConfig config) {
						List<RegistryConfig> registryConfigs = parseRegistryConfigList(environment,
								node.getChildNodes(), null);
						if (!registryConfigs.isEmpty()) {
							config.setRegistries(registryConfigs);
						}

						return config.isValid();
					}
				});
	}

	private static List<MethodConfig> parseMethodConfigList(final Environment environment, NodeList nodeList) {
		return parseConfigList(MethodConfig.class, environment, nodeList, null, new ConfigFilter<MethodConfig>() {
			@Override
			public boolean doFilter(List<MethodConfig> list, Node node, MethodConfig config) {
				if (config.isValid()) {
					List<ArgumentConfig> argumentConfigs = parseArgumentConfigList(environment,
							node.getChildNodes());
					if (!argumentConfigs.isEmpty()) {
						config.setArguments(argumentConfigs);
					}
					return true;
				}
				return false;
			}
		});
	}

	private static List<ArgumentConfig> parseArgumentConfigList(Environment environment, NodeList nodeList) {
		return parseConfigList(ArgumentConfig.class, environment, nodeList, null);
	}

	public static List<MetadataReportConfig> parseMetadataReportConfigList(Environment environment,
			NodeList nodeList, MetadataReportConfig defaultConfig) {
		return parseConfigList(MetadataReportConfig.class, environment, nodeList, defaultConfig);
	}

	@SuppressWarnings("rawtypes")
	public static List<ServiceConfig> parseServiceConfigList(final Environment environment, NodeList nodeList,
			ServiceConfig<?> defaultConfig, final NoArgsInstanceFactory refInstanceFactory, final ClassesLoaderFactory classesLoaderFactory) {
		return parseConfigList(ServiceConfig.class, environment, nodeList, defaultConfig,
				new ConfigFilter<ServiceConfig>() {

					@SuppressWarnings("unchecked")
					@Override
					public boolean doFilter(List<ServiceConfig> list, Node node, ServiceConfig config) {
						String ref = DomUtils.getNodeAttributeValue(environment, node, "ref");
						if (StringUtils.isNotEmpty(ref) && refInstanceFactory.isInstance(ref)) {
							config.setRef(refInstanceFactory.getInstance(ref));
						}

						String packageName = getPackageName(environment, node);
						if (StringUtils.isNotEmpty(packageName)) {
							for (Class<?> clazz : classesLoaderFactory.getClassesLoader(packageName)) {
								if(!clazz.isInterface()){
									continue;
								}
								
								if(!refInstanceFactory.isInstance(clazz)){
									logger.warn("{} not supported get instance", clazz);
									continue;
								}

								Object refInstance = refInstanceFactory.getInstance(clazz);
								ServiceConfig<Object> scanService = Copy.copy(ServiceConfig.class, config);
								scanService.setInterface(clazz);
								scanService.setRef(refInstance);
								if (scanService.isValid()) {
									list.add(scanService);
								}
							}
						}

						List<RegistryConfig> registryConfigs = parseRegistryConfigList(environment,
								node.getChildNodes(), null);
						if (!registryConfigs.isEmpty()) {
							config.setRegistries(registryConfigs);
						}

						List<ProtocolConfig> protocolConfigs = parseProtocolConfigList(environment,
								node.getChildNodes(), null);
						if (!protocolConfigs.isEmpty()) {
							config.setProtocols(protocolConfigs);
						}

						if (config.isValid() && config.getRef() != null
								&& StringUtils.isNotEmpty(config.getInterface())) {
							List<MethodConfig> methodConfigs = parseMethodConfigList(environment,
									node.getChildNodes());
							if (!methodConfigs.isEmpty()) {
								config.setMethods(methodConfigs);
							}
							return true;
						}

						return false;
					}
				});
	}

	public static List<ProtocolConfig> parseProtocolConfigList(Environment environment, NodeList nodeList,
			ProtocolConfig defaultConfig) {
		return parseConfigList(ProtocolConfig.class, environment, nodeList, defaultConfig,
				new ConfigFilter<ProtocolConfig>() {
					@Override
					public boolean doFilter(List<ProtocolConfig> list, Node node, ProtocolConfig config) {
						if (StringUtils.isEmpty(config.getName())) {
							config.setName(DEFAULT_PROTOCOL_NAME);
						}
						return config.isValid();
					}
				});
	}

	public static List<RegistryConfig> parseRegistryConfigList(Environment environment, NodeList nodeList,
			RegistryConfig defaultConfig) {
		return parseConfigList(RegistryConfig.class, environment, nodeList, defaultConfig);
	}

	private static String getPackageName(Environment environment, Node node) {
		return DomUtils.getNodeAttributeValue(environment, node, DUBBO_SCAN_PACKAGE);
	}

	@SuppressWarnings("rawtypes")
	public static List<ReferenceConfig> parseReferenceConfigList(final Environment environment,
			NodeList nodeList, ReferenceConfig<?> defaultConfig, final ClassesLoaderFactory classesLoaderFactory) {
		return parseConfigList(ReferenceConfig.class, environment, nodeList, defaultConfig,
				new ConfigFilter<ReferenceConfig>() {
					@Override
					public boolean doFilter(List<ReferenceConfig> list, Node node, ReferenceConfig config) {
						String packageName = getPackageName(environment, node);
						if (StringUtils.isNotEmpty(packageName)) {
							for (Class<?> clazz : classesLoaderFactory.getClassesLoader(packageName)) {
								if (!clazz.isInterface() || AnnotationUtils.isIgnore(clazz)) {
									continue;
								}

								ReferenceConfig<?> referenceConfig = Copy.copy(ReferenceConfig.class, config);
								referenceConfig.setInterface(clazz);
								if (referenceConfig.isValid()) {
									list.add(referenceConfig);
								}
							}
						}

						List<RegistryConfig> registryConfigs = parseRegistryConfigList(environment,
								node.getChildNodes(), null);
						if (!registryConfigs.isEmpty()) {
							config.setRegistries(registryConfigs);
						}

						if (config.isValid() && config.getInterfaceClass() != null) {
							List<MethodConfig> methodConfigs = parseMethodConfigList(environment,
									node.getChildNodes());
							if (!methodConfigs.isEmpty()) {
								config.setMethods(methodConfigs);
							}
							return true;
						}
						return false;
					}
				});
	}

	private static <T> void loader(Object instance, Environment environment, Node node) {
		NamedNodeMap namedNodeMap = node.getAttributes();
		Fields fields = MapperUtils.getMapper().getFields(instance.getClass(), FilterFeature.SETTER);
		for (int i = 0, len = namedNodeMap.getLength(); i < len; i++) {
			Node n = namedNodeMap.item(i);
			String name = n.getNodeName();
			if (name.equals(DUBBO_SERVICE_REF) || name.equals(DUBBO_SCAN_PACKAGE)) {
				continue;
			}

			String value = n.getNodeValue();
			if (StringUtils.isEmpty(value)) {
				continue;
			}

			value = environment.resolvePlaceholders(value);
			Field field = fields.findSetter(name, null);
			if (field == null) {
				logger.warn("{} ignore attribute name={}, value={}", instance.getClass(), name, value);
				continue;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("{} set name={}, value={}", instance.getClass(), name, value);
			}
			MapperUtils.setValue(environment, instance, field, value);
		}
	}

	public static List<ProviderConfig> parseProviderConfigList(Environment environment, NodeList nodeList,
			ProviderConfig defaultConfig) {
		return parseConfigList(ProviderConfig.class, environment, nodeList, defaultConfig);
	}

	private static interface ConfigFilter<T> {
		boolean doFilter(List<T> list, Node node, T config);
	}

	private static <T> List<T> parseConfigList(Class<? extends T> type, Environment environment,
			NodeList nodeList, T defaultConfig) {
		return parseConfigList(type, environment, nodeList, defaultConfig, null);
	}

	private static <T> List<T> parseConfigList(Class<? extends T> type, Environment environment,
			NodeList nodeList, T defaultConfig, ConfigFilter<T> filter) {
		List<T> list = new ArrayList<T>(4);
		if (nodeList != null) {
			String tagName = TAG_NAME_PREFIX + AbstractConfig.getTagName(type);
			for (int i = 0, len = nodeList.getLength(); i < len; i++) {
				Node node = nodeList.item(i);
				if (!node.getNodeName().equalsIgnoreCase(tagName)) {
					continue;
				}

				T config = defaultConfig == null ? InstanceUtils.INSTANCE_FACTORY.getInstance(type)
						: Copy.copy(type, defaultConfig);
				loader(config, environment, node);

				if (filter != null && !filter.doFilter(list, node, config)) {
					continue;
				}

				if (config instanceof AbstractConfig) {
					if (((AbstractConfig) config).isValid()) {
						list.add(config);
					} else {
						logger.error(config);
					}
				} else {
					list.add(config);
				}

				list.addAll(parseConfigList(type, environment, node.getChildNodes(), config, filter));
			}
		}
		return list;
	}

	public static List<ConsumerConfig> parseConsumerConfigList(Environment environment, NodeList nodeList,
			ConsumerConfig defaultConfig) {
		return parseConfigList(ConsumerConfig.class, environment, nodeList, defaultConfig);
	}

	public static List<SslConfig> parseSslConfigList(Environment environment, NodeList nodeList) {
		return parseConfigList(SslConfig.class, environment, nodeList, null);
	}

	public static List<MetricsConfig> parseMetricsConfigList(Environment environment, NodeList nodeList) {
		return parseConfigList(MetricsConfig.class, environment, nodeList, null);
	}

	public static List<ModuleConfig> parseModuleConfigList(Environment environment, NodeList nodeList) {
		return parseConfigList(ModuleConfig.class, environment, nodeList, null);
	}

	public static List<MonitorConfig> parseMonitorConfigList(Environment environment, NodeList nodeList) {
		return parseConfigList(MonitorConfig.class, environment, nodeList, null);
	}
}
