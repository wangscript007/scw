package scw.servlet.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import scw.core.Destroy;
import scw.core.utils.StringUtils;
import scw.servlet.ServletUtils;
import scw.servlet.beans.RequestBeanFactory;
import scw.servlet.context.DefaultRequestBeanContext;
import scw.servlet.context.RequestBeanContext;

public abstract class AbstractHttpRequest extends HttpServletRequestWrapper implements HttpRequest, Destroy {
	private static final String GET_DEFAULT_CHARSET_ANME = "ISO-8859-1";
	private long createTime;
	private RequestBeanContext requestBeanContext;
	private boolean cookieValue;
	private boolean debug;

	public AbstractHttpRequest(RequestBeanFactory requestBeanFactory, HttpServletRequest httpServletRequest,
			boolean cookieValue, boolean debug) throws IOException {
		super(httpServletRequest);
		this.createTime = System.currentTimeMillis();
		this.requestBeanContext = new DefaultRequestBeanContext(this, requestBeanFactory);
		this.cookieValue = cookieValue;
		this.debug = debug;
	}

	public long getCreateTime() {
		return createTime;
	}

	@SuppressWarnings("unchecked")
	public final <T> T getParameter(Class<T> type, String name) {
		T v = (T) getAttribute(name);
		if (v == null) {
			v = (T) ServletUtils.getParameter(this, type, name);
		}
		return v;
	}

	public boolean isAJAX() {
		return ServletUtils.isAjaxRequest(this);
	}

	public String getIP() {
		return ServletUtils.getIP(this);
	}

	/**
	 * 从cookie中获取数据
	 * 
	 * @param name
	 *            cookie中的名字
	 * @param ignoreCase
	 *            查找时是否忽略大小写
	 * @return
	 */
	public Cookie getCookie(String name, boolean ignoreCase) {
		return ServletUtils.getCookie(this, name, ignoreCase);
	}

	@Override
	public String getParameter(String name) {
		String v = super.getParameter(name);
		if (v == null) {
			Map<String, String> restParameterMap = ServletUtils.getRestPathParameterMap(this);
			if (restParameterMap != null) {
				v = restParameterMap.get(name);
			}
		}

		if (v == null) {
			if (cookieValue) {
				Cookie cookie = getCookie(name, false);
				if (cookie != null) {
					v = cookie.getValue();
				}
			}
		} else {
			if ("GET".equals(getMethod())) {
				v = decodeGETParameter(v);
			}
		}
		return v;
	}

	protected boolean isNull(String value) {
		return StringUtils.isEmpty(value);
	}

	public String decodeGETParameter(String value) {
		if (StringUtils.containsChinese(value)) {
			return value;
		}

		try {
			return new String(value.getBytes(GET_DEFAULT_CHARSET_ANME), getCharacterEncoding());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return value;
		}
	}

	public String getString(String key) {
		return getParameter(key);
	}

	protected void parameterError(Exception e, String key, String v) {
		getLogger().error("参数解析错误key={},value={}", key, v);
	}

	public Byte getByte(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseByte(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public byte getByteValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseByte(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public Short getShort(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseShort(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public short getShortValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseShort(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public Integer getInteger(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseInt(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public int getIntValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseInt(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public Long getLong(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseLong(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public long getLongValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseLong(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public Boolean getBoolean(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseBoolean(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public boolean getBooleanValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseBoolean(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return false;
	}

	public Float getFloat(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseFloat(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public float getFloatValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseFloat(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public Double getDouble(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseDouble(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public double getDoubleValue(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseDouble(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public char getChar(String key) {
		String v = getParameter(key);
		try {
			return StringUtils.parseChar(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return 0;
	}

	public Character getCharacter(String key) {
		String v = getParameter(key);
		if (isNull(v)) {
			return null;
		}

		try {
			return StringUtils.parseChar(v);
		} catch (Exception e) {
			parameterError(e, key, v);
		}
		return null;
	}

	public void destroy() {
		requestBeanContext.destroy();
	}

	public <T> T getBean(Class<T> type, String name) {
		return requestBeanContext.getBean(type, name);
	}

	public final <T> T getBean(Class<T> type) {
		return requestBeanContext.getBean(type);
	}

	public boolean isDebugEnabled() {
		return debug;
	}

	public void debug(String format, Object... args) {
		if (isDebugEnabled()) {
			getLogger().debug(format, args);
		}
	}
}