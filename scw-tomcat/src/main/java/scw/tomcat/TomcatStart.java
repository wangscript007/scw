package scw.tomcat;

import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.descriptor.web.ErrorPage;

import scw.application.Application;
import scw.application.ApplicationUtils;
import scw.application.Main;
import scw.application.MainApplication;
import scw.beans.Destroy;
import scw.core.Constants;
import scw.core.GlobalPropertyFactory;
import scw.core.instance.InstanceUtils;
import scw.core.instance.annotation.SPI;
import scw.core.reflect.ReflectionUtils;
import scw.core.utils.ArrayUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.StringUtils;
import scw.http.HttpMethod;
import scw.http.server.HttpControllerDescriptor;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.mvc.action.Action;
import scw.mvc.action.ActionManager;
import scw.servlet.ApplicationServletContainerInitializer;
import scw.servlet.ServletUtils;
import scw.util.ClassScanner;
import scw.value.property.PropertyFactory;

@SPI(order = -1)
public class TomcatStart implements Main, Destroy {
	private static Logger logger = LoggerFactory.getLogger(TomcatStart.class);
	private Tomcat tomcat;

	protected String getDocBase(PropertyFactory propertyFactory) {
		return GlobalPropertyFactory.getInstance().getWorkPath();
	}

	protected String getContextPath(PropertyFactory propertyFactory) {
		String contextPath = TomcatUtils.getContextPath(propertyFactory);
		return StringUtils.isEmpty(contextPath) ? "" : contextPath;
	}

	protected JarScanner getJarScanner(Application application) {
		return InstanceUtils.loadService(JarScanner.class, application.getBeanFactory(),
				application.getPropertyFactory(), "scw.embed.tomcat.Tomcat8AboveStandardJarScanner");
	}

	protected Context createContext(MainApplication application) {
		Context context = tomcat.addContext(getContextPath(application.getPropertyFactory()),
				getDocBase(application.getPropertyFactory()));
		context.setParentClassLoader(application.getClassLoader());
		JarScanner jarScanner = getJarScanner(application);
		if (jarScanner != null) {
			context.setJarScanner(jarScanner);
		}
		return context;
	}

	protected void addErrorPage(Context context, Application application) {
		if (application.getBeanFactory().isInstance(ActionManager.class)) {
			for (Action action : application.getBeanFactory().getInstance(ActionManager.class).getActions()) {
				ErrorPageController errorCodeController = action.getAnnotatedElement()
						.getAnnotation(ErrorPageController.class);
				if (errorCodeController == null) {
					continue;
				}

				HttpControllerDescriptor controllerDescriptorToUse = null;
				for (HttpControllerDescriptor httpControllerDescriptor : action.getHttpControllerDescriptors()) {
					if (httpControllerDescriptor.getMethod() == HttpMethod.GET
							&& !httpControllerDescriptor.getRestful().isRestful()) {
						controllerDescriptorToUse = httpControllerDescriptor;
					}
				}

				if (controllerDescriptorToUse == null) {
					logger.warn("not support error controller action: {}", action);
					continue;
				}

				if (errorCodeController != null) {
					for (int code : errorCodeController.value()) {
						ErrorPage errorPage = new ErrorPage();
						errorPage.setErrorCode(code);
						errorPage.setLocation(controllerDescriptorToUse.getPath());
						context.addErrorPage(errorPage);
					}
				}
			}
		}
	}

	protected boolean isVersion(String version) {
		return StringUtils.startsWithIgnoreCase(ServerInfo.getServerNumber(), version);
	}

	protected void configureConnector(Tomcat tomcat, int port, MainApplication application) {
		Connector connector = null;
		String connectorName = TomcatUtils.getTomcatConnectorName(application.getPropertyFactory());
		if (!StringUtils.isEmpty(connectorName)) {
			connector = application.getBeanFactory().getInstance(connectorName);
		} else {
			connector = new Connector(TomcatUtils.getTomcatProtocol(application.getPropertyFactory()));
		}

		connector.setPort(port);
		tomcat.setConnector(connector);
	}

	protected void configureJSP(Context context, MainApplication application) throws Exception {
		if (application.getBeanFactory().isInstance(JspConfigDescriptor.class)) {
			context.setJspConfigDescriptor(application.getBeanFactory().getInstance(JspConfigDescriptor.class));
		}

		if (ClassUtils.isPresent("org.apache.jasper.servlet.JspServlet")) {
			ServletContainerInitializer containerInitializer = InstanceUtils.INSTANCE_FACTORY
					.getInstance("org.apache.jasper.servlet.JasperInitializer");
			if (containerInitializer != null) {
				context.addServletContainerInitializer(containerInitializer, null);
			} // else Probably not Tomcat 8

			Tomcat.addServlet(context, "jsp", "org.apache.jasper.servlet.JspServlet");
			addServletMapping(context, "*.jsp", "jsp");
			addServletMapping(context, "*.jspx", "jsp");
		}
	}

	protected void addServletMapping(Context context, String pattern, String servletName) throws Exception {
		Method method = ReflectionUtils.getMethod(Context.class, "addServletMappingDecoded", String.class,
				String.class);
		if (method == null) {// tomcat8以下
			method = ReflectionUtils.getMethod(Context.class, "addServletMapping", String.class, String.class);
		}
		method.invoke(context, pattern, servletName);
	}

	protected void configureServlet(Context context, MainApplication application) throws Exception {
		String servletName = application.getMainClass().getSimpleName();
		Servlet servlet = application.getBeanFactory().getInstance(Servlet.class);
		Wrapper wrapper = Tomcat.addServlet(context, servletName, servlet);
		Properties properties = TomcatUtils.getServletInitParametersConfig(servletName, true);
		for (Entry<Object, Object> entry : properties.entrySet()) {
			wrapper.addInitParameter(entry.getKey().toString(), entry.getValue().toString());
		}

		addServletMapping(context, "/", servletName);
		String sourceMapping = TomcatUtils.getDefaultServletMapping(application.getPropertyFactory());
		if (!StringUtils.isEmpty(sourceMapping)) {
			String[] patternArr = StringUtils.commonSplit(sourceMapping);
			if (!ArrayUtils.isEmpty(patternArr)) {
				String tempServletName = "default";
				Wrapper tempWrapper = Tomcat.addServlet(context, tempServletName,
						"org.apache.catalina.servlets.DefaultServlet");
				Properties tempProperties = TomcatUtils.getServletInitParametersConfig(tempServletName, false);
				for (Entry<Object, Object> entry : tempProperties.entrySet()) {
					tempWrapper.addInitParameter(entry.getKey().toString(), entry.getValue().toString());
				}
				for (String pattern : patternArr) {
					logger.info("default mapping [{}]", pattern);
					addServletMapping(context, pattern, tempServletName);
				}
			}
		}
	}

	private void tomcat8(ClassLoader classLoader) throws Throwable {
		Class<?> clz = ClassUtils.forNameNullable("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
				classLoader);
		if (clz != null) {
			Method method = clz.getDeclaredMethod("disable");
			method.invoke(null);
		}
	}

	public void main(final MainApplication application) throws Throwable {
		if(application.getPropertyFactory().getValue("tomcat.log.enable", boolean.class, true)){
			java.util.logging.Logger.getLogger("org.apache").setLevel(Level.WARNING);
		}
		
		try {
			tomcat8(application.getClassLoader());
		} catch (Throwable e1) {
		}

		this.tomcat = new Tomcat();
		int port = ApplicationUtils.getApplicationPort(application);
		tomcat.setPort(port);
		logger.info("The boot port is {}", port);

		String basedir = TomcatUtils.getBaseDir(application.getPropertyFactory());
		if (StringUtils.isNotEmpty(basedir)) {
			tomcat.setBaseDir(basedir);
		}

		configureConnector(tomcat, port, application);
		tomcat.getHost().setAutoDeploy(false);
		
		final Context context = createContext(application);
		if (AprLifecycleListener.isAprAvailable()) {
			context.addLifecycleListener(new AprLifecycleListener());
		}

		configureJSP(context, application);
		configureServlet(context, application);

		for (TomcatContextConfiguration configuration : ApplicationUtils
				.loadAllService(TomcatContextConfiguration.class, application)) {
			configuration.configuration(application, context);
		}

		ServletUtils.setApplication(context.getServletContext(), application);
		context.addServletContainerInitializer(new ApplicationServletContainerInitializer(),
				ClassScanner.getInstance().getClasses(Constants.SYSTEM_PACKAGE_NAME,
						InstanceUtils.getScanAnnotationPackageName(application.getPropertyFactory())));
		tomcat.start();
		addErrorPage(context, application);
	}

	public void destroy() throws Throwable {
		tomcat.stop();
	}
}
