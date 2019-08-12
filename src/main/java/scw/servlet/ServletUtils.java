package scw.servlet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.beans.rpc.http.DefaultRpcService;
import scw.beans.rpc.http.RpcService;
import scw.core.Constants;
import scw.core.DefaultKeyValuePair;
import scw.core.KeyValuePair;
import scw.core.KeyValuePairFilter;
import scw.core.LinkedMultiValueMap;
import scw.core.MultiValueMap;
import scw.core.PropertyFactory;
import scw.core.exception.AlreadyExistsException;
import scw.core.exception.ParameterException;
import scw.core.instance.InstanceUtils;
import scw.core.reflect.ReflectUtils;
import scw.core.utils.ArrayUtils;
import scw.core.utils.Assert;
import scw.core.utils.ClassUtils;
import scw.core.utils.CollectionUtils;
import scw.core.utils.ResourceUtils;
import scw.core.utils.StringUtils;
import scw.core.utils.XUtils;
import scw.io.serializer.Serializer;
import scw.io.serializer.SerializerUtils;
import scw.json.JSONParseSupport;
import scw.json.JSONUtils;
import scw.logger.LoggerUtils;
import scw.net.ContentType;
import scw.servlet.beans.CommonRequestBeanFactory;
import scw.servlet.beans.RequestBeanFactory;
import scw.servlet.http.HttpWrapperFactory;
import scw.servlet.http.filter.HttpServiceFilter;
import scw.servlet.http.filter.NotFoundFilter;

public final class ServletUtils {
	private static final String RESTURL_PATH_PARAMETER = "_resturl_path_parameter";
	private static boolean asyncSupport = true;// 是否支持异步处理
	private static final String JSONP_CALLBACK = "callback";
	private static final String JSONP_RESP_PREFIX = "(";
	private static final String JSONP_RESP_SUFFIX = ");";
	private static ThreadLocal<Map<Object, Object>> controllerLocal = new ThreadLocal<Map<Object, Object>>();
	public static final String COOKIE_HEADER_NAME = "Cookie";
	
	static {
		try {
			Class.forName("javax.servlet.AsyncContext");
		} catch (Throwable e) {
			asyncSupport = false;// 不支持
		}
	}

	private ServletUtils() {
	};

	@SuppressWarnings("unchecked")
	public static Map<String, String> getRestPathParameterMap(ServletRequest request) {
		return (Map<String, String>) request.getAttribute(RESTURL_PATH_PARAMETER);
	}

	public static void setRestPathParameterMap(ServletRequest request, Map<String, String> parameterMap) {
		request.setAttribute(RESTURL_PATH_PARAMETER, parameterMap);
	}

	public static boolean isRestPathParameterMapAttributeName(String name) {
		return RESTURL_PATH_PARAMETER.equals(name);
	}

	/**
	 * 判断是否是AJAX请求
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	/**
	 * 判断是否是json请求
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isJsonRequest(HttpServletRequest request) {
		return isDesignatedContentType(request, scw.net.ContentType.APPLICATION_JSON);
	}

	public static boolean isFormRequest(HttpServletRequest request) {
		return isDesignatedContentType(request, ContentType.APPLICATION_X_WWW_FORM_URLENCODED);
	}

	public static boolean isMultipartRequest(HttpServletRequest request) {
		return isDesignatedContentType(request, ContentType.MULTIPART_FORM_DATA);
	}

	public static boolean isDesignatedContentType(HttpServletRequest request, String contentType) {
		String ct = request.getContentType();
		return StringUtils.isEmpty(ct) ? false : ct.startsWith(contentType);
	}

	/**
	 * 获取ip
	 * 
	 * @param request
	 * @return
	 */
	public static String getIP(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		return ip == null ? request.getRemoteAddr() : ip;
	}

	/**
	 * 是否支持异步处理(实际是否支持还要判断request)
	 * 
	 * @return
	 */
	public static boolean isAsyncSupport() {
		return asyncSupport;
	}

	public static ServletService getServletService(BeanFactory beanFactory, PropertyFactory propertyFactory,
			String configPath, String[] rootBeanFilters, boolean async) {
		if (async) {
			return beanFactory.getInstance("scw.servlet.AsyncServletService", beanFactory, propertyFactory, configPath,
					rootBeanFilters);
		} else {
			return beanFactory.getInstance("scw.servlet.DefaultServletService", beanFactory, propertyFactory,
					configPath, rootBeanFilters);
		}
	}

	public static ServletService getServletService(BeanFactory beanFactory, PropertyFactory propertyFactory,
			String configPath, String[] rootBeanFilters) {
		return getServletService(beanFactory, propertyFactory, configPath, rootBeanFilters, isAsyncSupport());
	}

	/**
	 * 从cookie中获取数据
	 * 
	 * @param request
	 * 
	 * @param name
	 *            cookie中的名字
	 * @param ignoreCase
	 *            查找时是否忽略大小写
	 * @return
	 */
	public static Cookie getCookie(HttpServletRequest request, String name, boolean ignoreCase) {
		if (name == null) {
			return null;
		}

		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (cookie == null) {
				continue;
			}

			if (ignoreCase) {
				if (name.equalsIgnoreCase(cookie.getName())) {
					return cookie;
				}
			} else {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	public static Map<String, String> getRequestFirstValueParameters(ServletRequest request,
			KeyValuePairFilter<String, String> filter) {
		Map<String, String[]> requestParams = request.getParameterMap();
		if (requestParams == null || requestParams.isEmpty()) {
			return null;
		}

		Map<String, String> map = new HashMap<String, String>();
		for (Entry<String, String[]> entry : requestParams.entrySet()) {
			String name = entry.getKey();
			if (name == null) {
				continue;
			}

			String[] values = entry.getValue();
			if (values == null || values.length == 0) {
				continue;
			}

			KeyValuePair<String, String> keyValuePair = filter
					.filter(new DefaultKeyValuePair<String, String>(name, values[0]));
			if (keyValuePair == null) {
				continue;
			}

			map.put(keyValuePair.getKey(), keyValuePair.getValue());
		}
		return map;
	}

	public static MultiValueMap<String, String> getRequestParameters(ServletRequest request,
			KeyValuePairFilter<String, String[]> filter) {
		Map<String, String[]> requestParams = request.getParameterMap();
		if (requestParams == null || requestParams.isEmpty()) {
			return null;
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>(requestParams.size(), 1);
		for (Entry<String, String[]> entry : requestParams.entrySet()) {
			String name = entry.getKey();
			if (name == null) {
				continue;
			}

			String[] values = entry.getValue();
			if (values == null || values.length == 0) {
				continue;
			}

			if (filter == null) {
				map.put(name, new LinkedList<String>(Arrays.asList(values)));
			} else {
				KeyValuePair<String, String[]> keyValuePair = filter
						.filter(new DefaultKeyValuePair<String, String[]>(name, values));
				if (keyValuePair == null) {
					continue;
				}

				map.put(keyValuePair.getKey(), new LinkedList<String>(Arrays.asList(keyValuePair.getValue())));
			}
		}
		return map;
	}

	public static Map<String, String> getRequestParameterAndAppendValues(ServletRequest request,
			CharSequence appendValueChars, KeyValuePairFilter<String, String[]> filter) {
		if (filter == null) {
			Map<String, String[]> requestParams = request.getParameterMap();
			if (CollectionUtils.isEmpty(requestParams)) {
				return null;
			}

			Map<String, String> params = new HashMap<String, String>(requestParams.size(), 1);
			for (Entry<String, String[]> entry : requestParams.entrySet()) {
				String name = entry.getKey();
				if (name == null) {
					continue;
				}

				String[] values = entry.getValue();
				if (values == null || values.length == 0) {
					continue;
				}

				if (appendValueChars == null) {
					params.put(name, values[0]);
				} else {
					StringBuilder sb = new StringBuilder();
					for (String value : values) {
						if (sb.length() != 0) {
							sb.append(appendValueChars);
						}

						sb.append(value);
					}
					params.put(name, sb.toString());
				}
			}
			return params;
		} else {
			MultiValueMap<String, String> requestParams = getRequestParameters(request, filter);
			if (CollectionUtils.isEmpty(requestParams)) {
				return null;
			}

			Map<String, String> params = new HashMap<String, String>(requestParams.size(), 1);
			for (Entry<String, List<String>> entry : requestParams.entrySet()) {
				String name = entry.getKey();
				if (name == null) {
					continue;
				}

				List<String> values = entry.getValue();
				if (CollectionUtils.isEmpty(values)) {
					continue;
				}

				if (appendValueChars == null) {
					params.put(name, requestParams.getFirst(name));
				} else {
					StringBuilder sb = new StringBuilder();
					for (String value : values) {
						if (sb.length() != 0) {
							sb.append(appendValueChars);
						}

						sb.append(value);
					}
					params.put(name, sb.toString());
				}
			}
			return params;
		}
	}

	/**
	 * 判断是否是HttpServlet
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public static boolean isHttpServlet(ServletRequest request, ServletResponse response) {
		return request instanceof HttpServletRequest && response instanceof HttpServletResponse;
	}

	public static void jsp(ServletRequest request, ServletResponse response, String page)
			throws ServletException, IOException {
		RequestDispatcher dispatcher = request.getRequestDispatcher(page);
		dispatcher.forward(request, response);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getRequestObjectParameterWrapper(Request request, Class<T> type, String name) {
		try {
			return (T) privateRequestObjectParameterWrapper(request, type,
					StringUtils.isEmpty(name) ? null : name + ".");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object getParameter(Request request, String name, Class<?> type) {
		try {
			return XUtils.getValue(request, name, type);
		} catch (Exception e) {
			throw new ParameterException(e, "解析参数错误name=" + name + ",type=" + type.getName());
		}
	}

	private static Object privateRequestObjectParameterWrapper(Request request, Class<?> type, String prefix)
			throws Exception {
		if (!ReflectUtils.isInstance(type)) {
			return null;
		}

		Object t = InstanceUtils.newInstance(type);
		Class<?> clz = type;
		while (clz != null && clz != Object.class) {
			for (Field field : clz.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
					continue;
				}

				String key = prefix == null ? field.getName() : prefix + field.getName();
				if (String.class.isAssignableFrom(field.getType())
						|| ClassUtils.isPrimitiveOrWrapper(field.getType())) {
					// 如果是基本数据类型
					Object v = getParameter(request, key, field.getType());
					if (v != null) {
						ReflectUtils.setFieldValue(clz, field, t, v);
					}
				} else {
					ReflectUtils.setFieldValue(clz, field, t,
							privateRequestObjectParameterWrapper(request, field.getType(), key + "."));
				}
			}
			clz = clz.getSuperclass();
		}
		return t;
	}

	public static void defaultResponse(Request request, Response response, JSONParseSupport jsonParseSupport,
			Object obj, boolean jsonp) throws Exception {
		if (obj == null) {
			return;
		}

		if (obj instanceof View) {
			((View) obj).render(request, response);
		} else {
			String content;
			String contentType = null;
			if (obj instanceof Text) {
				content = ((Text) obj).getTextContent();
				contentType = ((Text) obj).getTextContentType();
			} else if ((obj instanceof String) || (ClassUtils.isPrimitiveOrWrapper(obj.getClass()))) {
				content = obj.toString();
			} else {
				content = jsonParseSupport.toJSONString(obj);
			}

			if (jsonp) {
				String callbackTag = request.getParameter(JSONP_CALLBACK);
				if (!StringUtils.isEmpty(callbackTag)) {
					StringBuilder sb = new StringBuilder(
							content == null ? 32 : content.length() + callbackTag.length() + 2);
					sb.append(callbackTag);
					sb.append(JSONP_RESP_PREFIX);
					sb.append(content);
					sb.append(JSONP_RESP_SUFFIX);
					content = sb.toString();
					contentType = ContentType.TEXT_JAVASCRIPT;
				}
			}

			if (StringUtils.isEmpty(contentType)) {
				if (StringUtils.isEmpty(response.getContentType())) {
					response.setContentType(ContentType.TEXT_HTML);
				}
			} else {
				response.setContentType(contentType);
			}

			response.getWriter().write(content);
			if (response.isDebugEnabled()) {
				response.debug(content);
			}
		}
	}

	public static boolean isDebug(PropertyFactory propertyFactory) {
		return StringUtils.parseBoolean(propertyFactory.getProperty("servlet.debug"), true);
	}

	public static int getWarnExecuteTime(PropertyFactory propertyFactory) {
		return StringUtils.parseInt(propertyFactory.getProperty("servlet.warn-execute-time"), 100);
	}

	public static JSONParseSupport getJsonParseSupport(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		JSONParseSupport jsonParseSupport;
		String jsonParseSupportBeanName = propertyFactory.getProperty("servlet.json");
		if (StringUtils.isEmpty(jsonParseSupportBeanName)) {
			jsonParseSupport = JSONUtils.DEFAULT_JSON_SUPPORT;
		} else {
			jsonParseSupport = beanFactory.getInstance(jsonParseSupportBeanName);
		}
		return jsonParseSupport;
	}

	public static String getCharsetName(PropertyFactory propertyFactory) {
		String charsetName = propertyFactory.getProperty("servlet.charsetName");
		return StringUtils.isEmpty(charsetName) ? Constants.DEFAULT_CHARSET_NAME : charsetName;
	}

	public static WrapperFactory getWrapperFactory(BeanFactory beanFactory, RequestBeanFactory requestBeanFactory,
			PropertyFactory propertyFactory) {
		String requestFactoryBeanName = propertyFactory.getProperty("servlet.wrapper-factory");
		if (StringUtils.isEmpty(requestFactoryBeanName)) {
			return beanFactory.getInstance(HttpWrapperFactory.class, requestBeanFactory, isDebug(propertyFactory),
					StringUtils.parseBoolean(propertyFactory.getProperty("servlet.parameter.cookie")),
					getJsonParseSupport(beanFactory, propertyFactory),
					StringUtils.parseBoolean(propertyFactory.getProperty("servlet.jsonp")));
		} else {
			return beanFactory.getInstance(requestFactoryBeanName);
		}
	}

	public static String getRPCPath(PropertyFactory propertyFactory) {
		String path = propertyFactory.getProperty("servlet.rpc-path");
		return StringUtils.isEmpty(path) ? "/rpc" : path;
	}

	public static RpcService getRPCService(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		String rpcServerBeanName = propertyFactory.getProperty("servlet.rpc");
		if (StringUtils.isEmpty(rpcServerBeanName)) {
			String sign = propertyFactory.getProperty("servlet.rpc-sign");
			boolean enable = StringUtils.parseBoolean(propertyFactory.getProperty("servlet.rpc-enable"), false);
			if (enable || !StringUtils.isEmpty(sign)) {// 开启
				LoggerUtils.info(ServletUtils.class, "rpc签名：{}", sign);
				String serializer = propertyFactory.getProperty("servlet.rpc-serializer");
				return beanFactory.getInstance(DefaultRpcService.class, beanFactory, sign,
						StringUtils.isEmpty(serializer) ? SerializerUtils.DEFAULT_SERIALIZER
								: (Serializer) beanFactory.getInstance(serializer));
			}
		} else {
			return beanFactory.getInstance(rpcServerBeanName);
		}

		return null;
	}

	public static HttpServiceFilter getHttpServiceFilter(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		String actionKey = propertyFactory.getProperty("servlet.actionKey");
		actionKey = StringUtils.isEmpty(actionKey) ? "action" : actionKey;
		String packageName = propertyFactory.getProperty("servlet.scanning");
		packageName = StringUtils.isEmpty(packageName) ? "" : packageName;
		return beanFactory.getInstance(HttpServiceFilter.class, beanFactory, ResourceUtils.getClassList(packageName),
				actionKey);

	}

	public static List<Filter> getFilters(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		List<Filter> filters = new ArrayList<Filter>();
		String filterNames = propertyFactory.getProperty("servlet.filters");
		if (!StringUtils.isEmpty(filterNames)) {
			Collection<Filter> rootFilter = BeanUtils.getBeanList(beanFactory,
					Arrays.asList(StringUtils.commonSplit(filterNames)));
			filters.addAll(rootFilter);
		}

		filters.add(getHttpServiceFilter(beanFactory, propertyFactory));
		String lastFilterNames = propertyFactory.getProperty("servlet.lastFilters");
		if (!StringUtils.isEmpty(lastFilterNames)) {
			Collection<Filter> rootFilter = BeanUtils.getBeanList(beanFactory,
					Arrays.asList(StringUtils.commonSplit(lastFilterNames)));
			filters.addAll(rootFilter);
		}
		filters.add(beanFactory.getInstance(NotFoundFilter.class));
		return filters;
	}

	public static RequestBeanFactory getRequestBeanFactory(BeanFactory beanFactory, PropertyFactory propertyFactory,
			String configPath, String[] rootBeanFilters) {
		String config = propertyFactory.getProperty("servlet.beans.config");
		String beanFilters = propertyFactory.getProperty("servlet.beans.filters");
		config = StringUtils.isEmpty(config) ? configPath : config;
		String[] filters = StringUtils.isEmpty(beanFilters) ? rootBeanFilters : StringUtils.commonSplit(beanFilters);
		return beanFactory.getInstance(CommonRequestBeanFactory.class, beanFactory, propertyFactory, config, filters);
	}

	public static void service(Request request, Response response, Collection<Filter> serviceFilter) throws Throwable {
		try {
			FilterChain filterChain = new IteratorFilterChain(serviceFilter, null);
			filterChain.doFilter(request, response);
		} finally {
			controllerLocal.remove();
		}
	}

	public static Object getControllerThreadLocalResource(Object name) {
		Map<Object, Object> map = controllerLocal.get();
		return map == null ? null : map.get(name);
	}

	public static void bindControllerThreadLocalResource(Object name, Object value) {
		Assert.notNull(name);
		Map<Object, Object> map = controllerLocal.get();
		if (map == null) {
			map = new HashMap<Object, Object>(4);
			map.put(name, value);
			controllerLocal.set(map);
		} else {
			if (map.containsKey(name)) {
				throw new AlreadyExistsException(name.toString());
			}
			map.put(name, value);
		}
	}

	public static ActionParameter[] getActionParameter(Method method) {
		String[] tempKeys = ClassUtils.getParameterName(method);
		Class<?>[] types = method.getParameterTypes();
		ActionParameter[] paramInfos = new ActionParameter[types.length];
		for (int l = 0; l < types.length; l++) {
			paramInfos[l] = new ActionParameter(types[l], tempKeys[l]);
		}
		return paramInfos;
	}

	public static Action crateAction(BeanFactory beanFactory, Class<?> clazz, Method method) {
		return new MethodAction(beanFactory, clazz, method);
	}

	/** ----------------------------spread---------------------------- **/
	private static final String HEAD_SOURCE_NAME = ServletUtils.class.getName() + "#header";

	@SuppressWarnings("unchecked")
	private static Map<String, String> privateGetSpreadHeaderMap() {
		return (Map<String, String>) getControllerThreadLocalResource(HEAD_SOURCE_NAME);
	}

	public static void setSpreadHeader(String name, String value) {
		Map<String, String> headerMap = privateGetSpreadHeaderMap();
		if (headerMap == null) {
			headerMap = new HashMap<String, String>(4);
			bindControllerThreadLocalResource(HEAD_SOURCE_NAME, headerMap);
		}
		headerMap.put(name, value);
	}

	public static Map<String, String> getSpreadHeaderMap() {
		Map<String, String> headerMap = privateGetSpreadHeaderMap();
		return headerMap == null ? null : Collections.unmodifiableMap(headerMap);
	}

	public static String getSpreadHeader(String name) {
		Map<String, String> headerMap = privateGetSpreadHeaderMap();
		return headerMap == null ? null : headerMap.get(name);
	}

	public static void clearSpreadHeader() {
		bindControllerThreadLocalResource(HEAD_SOURCE_NAME, null);
	}

	public static void removeSpreadHeader(String name) {
		Map<String, String> headerMap = privateGetSpreadHeaderMap();
		if (headerMap == null) {
			return;
		}
		headerMap.remove(name);
	}

	@SuppressWarnings("unchecked")
	private static HashMap<String, Object> getSpreadData() {
		return (HashMap<String, Object>) getControllerThreadLocalResource(ServletUtils.class);
	}

	public static void setSpreadData(String name) {
		HashMap<String, Object> map = getSpreadData();
		if (map == null) {
			return;
		}
		map.remove(name);
	}

	public static void setSpreadData(String name, Object value) {
		HashMap<String, Object> map = getSpreadData();
		if (map == null) {
			map = new HashMap<String, Object>(8);
			bindControllerThreadLocalResource(ServletUtils.class, map);
		}
		map.put(name, value);
	}

	public static void removeSpreadData(String name) {
		Map<String, Object> map = getSpreadData();
		if (map == null) {
			return;
		}
		map.remove(name);
	}

	public static Map<String, Object> getRequestParameterMap(Method method, Object[] args) {
		String[] names = ClassUtils.getParameterName(method);
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		if (!ArrayUtils.isEmpty(names)) {
			for (int i = 0; i < names.length; i++) {
				map.put(names[i], args[i]);
			}
		}

		Map<String, Object> spreadMap = getSpreadData();
		if (!CollectionUtils.isEmpty(spreadMap)) {
			map.putAll(spreadMap);
		}
		return map;
	}
}
