package scw.freemarker;

import java.io.File;
import java.io.IOException;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.core.Constants;
import scw.core.GlobalPropertyFactory;
import scw.core.utils.ClassUtils;
import scw.core.utils.StringUtils;
import scw.freemarker.annotation.SharedVariable;
import scw.io.ResourceUtils;
import scw.logger.LoggerUtils;
import scw.value.property.PropertyFactory;

@scw.core.instance.annotation.Configuration(order = Integer.MIN_VALUE, value = Configuration.class)
public class DefaultConfiguration extends Configuration {
	private static scw.logger.Logger logger = LoggerUtils.getLogger(DefaultConfiguration.class);

	public DefaultConfiguration(BeanFactory beanFactory, PropertyFactory propertyFactory) throws IOException {
		super(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		setDefaultEncoding(Constants.DEFAULT_CHARSET_NAME);
		if (beanFactory.isInstance(TemplateLoader.class)) {
			setTemplateLoader(beanFactory.getInstance(TemplateLoader.class));
		} else {
			setTemplateLoader(getDefaultTemplateLoader(beanFactory, propertyFactory));
		}
		if (beanFactory.isInstance(TemplateExceptionHandler.class)) {
			setTemplateExceptionHandler(beanFactory.getInstance(TemplateExceptionHandler.class));
		}

		setObjectWrapper(new DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));

		for (Class<?> clz : ResourceUtils.getPackageScan().getClasses(Constants.SYSTEM_PACKAGE_NAME, getScanAnnotationPackageName())) {
			SharedVariable sharedVariable = clz.getAnnotation(SharedVariable.class);
			if (sharedVariable == null) {
				continue;
			}

			String name = sharedVariable.value();
			if (StringUtils.isEmpty(name)) {
				// 默认使用简写类名
				name = clz.getSimpleName();
			}

			if (getSharedVariable(name) != null) {
				logger.warn("already exist name={}, class={}", name, clz);
				continue;
			}

			Object instance = beanFactory.getInstance(clz);
			if (instance instanceof TemplateModel) {
				setSharedVariable(name, (TemplateModel) instance);
			} else {
				try {
					setSharedVariable(name, instance);
				} catch (TemplateModelException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	protected TemplateLoader getDefaultTemplateLoader(BeanFactory beanFactory, PropertyFactory propertyFactory)
			throws IOException {
		MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader();
		multiTemplateLoader.add(new FileTemplateLoader(new File(GlobalPropertyFactory.getInstance().getWorkPath())));
		multiTemplateLoader.add(new ClassTemplateLoader(ClassUtils.getDefaultClassLoader(), "/"));
		return multiTemplateLoader;
	}

	public String getScanAnnotationPackageName() {
		return GlobalPropertyFactory.getInstance().getValue("scw.scan.freemarker.shared.variable.package", String.class,
				BeanUtils.getScanAnnotationPackageName());
	}

}