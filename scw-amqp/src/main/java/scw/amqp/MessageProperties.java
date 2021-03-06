package scw.amqp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import scw.core.utils.StringUtils;
import scw.math.NumberHolder;
import scw.script.MathScriptEngine;
import scw.value.AnyValue;
import scw.value.Value;

public class MessageProperties implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String DELAY_MESSAGE = "scw.delay";
	private static final String RETRY_COUNT = "scw.retry.count";
	private static final String MAX_RETRY_COUNT = "scw.retry.max.count";
	private static final String RETRY_DELAY = "scw.retry.delay";
	private static final String RETRY_DELAY_SCRIPT = "scw.retry.delay";
	private static final String RETRY_DELAY_MULTIPLE = "scw.retry.delay.multiple";
	private static final String PUBLISH_ROUTING_KEY = "scw.publish.routingKey";
	private static final String ENABLE_LOCAL_RETRY_PUSH = "scw.enable.local.retry.push";

	private String contentType;
	private String contentEncoding;
	private Map<String, Object> headers;
	private Integer deliveryMode;
	private Integer priority;
	private String correlationId;
	private String replyTo;
	private String expiration;
	private String messageId;
	private Date timestamp;
	private String type;
	private String userId;
	private String appId;
	private String clusterId;

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public Integer getDeliveryMode() {
		return deliveryMode;
	}

	public void setDeliveryMode(Integer deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public String getExpiration() {
		return expiration;
	}

	public void setExpiration(Long expiration) {
		this.expiration = expiration == null ? null : ("" + expiration);
	}

	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
	
	public Object getHeader(String name) {
		if (headers == null) {
			return null;
		}

		return headers.get(name);
	}
	
	public Value getHeaderValue(String name) {
		Object value = getHeader(name);
		if (value == null) {
			return null;
		}
		return new AnyValue(value);
	}

	public MessageProperties removeHeader(String name) {
		if (headers != null) {
			headers.remove(name);
		}
		return this;
	}

	/**====================以下为框架支持的方法，并非AMQP协议内容==========================**/
	
	public long getDelay() {
		Object delay = getHeader(DELAY_MESSAGE);
		return delay == null ? 0 : StringUtils.parseLong(delay.toString());
	}

	public MessageProperties setDelay(long delay, TimeUnit timeUnit) {
		if (delay <= 0) {
			removeHeader(DELAY_MESSAGE);
			setExpiration((String) null);
		} else {
			setExpiration(timeUnit.toMillis(delay));
			setHeader(DELAY_MESSAGE, getExpiration());
		}
		return this;
	}

	public MessageProperties setHeader(String name, Object value) {
		if (headers == null) {
			headers = new HashMap<String, Object>();
		}

		if (value == null) {
			headers.remove(name);
		} else {
			headers.put(name, value);
		}
		return this;
	}

	public int getRetryCount() {
		Value value = getHeaderValue(RETRY_COUNT);
		return value == null ? 0 : value.getAsIntValue();
	}

	public void incrRetryCount() {
		setHeader(RETRY_COUNT, getRetryCount() + 1);
	}

	/**
	 * 0表示没的最大重试次数，-1表示不重试
	 * 
	 * @return
	 */
	public int getMaxRetryCount() {
		Value value = getHeaderValue(MAX_RETRY_COUNT);
		return value == null ? 0 : value.getAsIntValue();
	}

	public void setMaxRetryCount(int maxRetryCount) {
		setHeader(MAX_RETRY_COUNT, maxRetryCount);
	}

	/**
	 * 重试时间的倍数 0表示没有倍数
	 * 
	 * @return
	 */
	public double getRetryDelayMultiple() {
		Value multiple = getHeaderValue(RETRY_DELAY_MULTIPLE);
		return multiple == null ? 0 : multiple.getAsDoubleValue();
	}

	/**
	 * 设置重试时间的倍数
	 * 
	 * @return
	 */
	public MessageProperties setRetryDelayMultiple(double multiple) {
		if (multiple <= 0) {
			removeHeader(RETRY_DELAY_MULTIPLE);
		} else {
			setHeader(RETRY_DELAY_MULTIPLE, multiple);
		}
		return this;
	}

	public long getRetryDelay() {
		Object script = getHeader(RETRY_DELAY_SCRIPT);
		if (script == null) {
			;
			Value value = getHeaderValue(RETRY_DELAY);
			if (value == null) {
				return 0;
			}

			return value.getAsLongValue();
		}

		MathScriptEngine mathScriptEngine = new MathScriptEngine();
		mathScriptEngine.getResolvers().add(new MathScriptEngine.ObjectFieldScriptResolver(this));
		NumberHolder value = mathScriptEngine.eval(script.toString());
		return value == null ? null : value.toBigDecimal().longValue();
	}

	public void setRetryDelayScript(String script) {
		setHeader(RETRY_DELAY_SCRIPT, script);
	}

	public void setRetryDelay(long delay, TimeUnit timeUnit) {
		setHeader(RETRY_DELAY, timeUnit.toMillis(delay));
	}
	
	/**
	 * 获取消息发送时的ORIGIN_ROUTING_KEY
	 * @return
	 */
	public String getPublishRoutingKey(){
		return StringUtils.toString(getHeader(PUBLISH_ROUTING_KEY), null);
	}
	
	public void setPublishRoutingKey(String routingKey){
		setHeader(PUBLISH_ROUTING_KEY, routingKey);
	}
	
	public Boolean isEnableLocalRetryPush(){
		Value value = getHeaderValue(ENABLE_LOCAL_RETRY_PUSH);
		return value == null? null:value.getAsBoolean();
	}
	
	public void setEnableLocalRetryPush(Boolean enableLocalRetryPush){
		setHeader(ENABLE_LOCAL_RETRY_PUSH, enableLocalRetryPush);
	}
}
