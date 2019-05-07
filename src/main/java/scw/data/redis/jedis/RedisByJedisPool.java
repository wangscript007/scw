package scw.data.redis.jedis;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scw.beans.annotation.Bean;
import scw.beans.annotation.Destroy;
import scw.core.Constants;
import scw.core.serializer.Serializer;
import scw.core.utils.ConfigUtils;
import scw.core.utils.PropertiesUtils;
import scw.core.utils.StringUtils;

@Bean(proxy = false)
public class RedisByJedisPool extends AbstractJedisOperations implements Closeable {

	private final JedisPool jedisPool;
	private final String auth;
	private final Serializer serializer;

	public RedisByJedisPool(String propertiesFile, Serializer serializer) {
		JedisPoolConfig config = createConfig(propertiesFile);
		Properties properties = ConfigUtils.getProperties(propertiesFile, Constants.DEFAULT_CHARSET.name());
		String host = PropertiesUtils.getProperty(properties, "host", "address");
		String port = PropertiesUtils.getProperty(properties, "port");
		this.auth = PropertiesUtils.getProperty(properties, "auth", "password", "pwd");
		if (StringUtils.isEmpty(port)) {
			this.jedisPool = new JedisPool(config, host);
		} else {
			this.jedisPool = new JedisPool(config, host, Integer.parseInt(port));
		}
		this.serializer = serializer;
	}

	public RedisByJedisPool(String propertiesFile) {
		this(propertiesFile, Constants.DEFAULT_SERIALIZER);
	}

	public static JedisPoolConfig createConfig(String propertiesFile) {
		JedisPoolConfig config = new JedisPoolConfig();
		PropertiesUtils.loadProperties(config, propertiesFile, Arrays.asList("maxWait,maxWaitMillis"));
		return config;
	}

	public RedisByJedisPool() {
		this(512, 200, true, "localhost");
	}

	public RedisByJedisPool(int maxTotal, int maxIdle, boolean testOnBorrow, String host) {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(maxTotal);
		jedisPoolConfig.setMaxIdle(maxIdle);
		jedisPoolConfig.setTestOnBorrow(testOnBorrow);
		this.jedisPool = new JedisPool(jedisPoolConfig, host);
		this.auth = null;
		this.serializer = Constants.DEFAULT_SERIALIZER;
	}

	@Destroy
	public void close() {
		jedisPool.close();
	}

	public Jedis getResource() {
		Jedis jedis = jedisPool.getResource();
		if (jedis != null && auth != null) {
			jedis.auth(auth);
		}
		return jedis;
	}

	public void close(Jedis resource) {
		if (resource != null) {
			resource.close();
		}
	}

	@Override
	protected Charset getCharset() {
		return Constants.DEFAULT_CHARSET;
	}

	@Override
	protected Serializer getSerializer() {
		return serializer;
	}
}