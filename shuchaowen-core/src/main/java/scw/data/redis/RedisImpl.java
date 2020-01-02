package scw.data.redis;

import scw.core.string.StringCodec;
import scw.data.cas.CASOperations;
import scw.io.serializer.Serializer;

public class RedisImpl implements Redis {
	private final RedisOperations<byte[], byte[]> binaryOperations;
	private final RedisOperations<String, String> stringOperations;
	private final RedisOperations<String, Object> objectOperations;
	private final CASOperations casOperations;

	public RedisImpl(RedisOperations<byte[], byte[]> binaryOperations, RedisOperations<String, String> stringOperations,
			StringCodec stringCodec, Serializer serializer) {
		this.binaryOperations = binaryOperations;
		this.stringOperations = stringOperations;
		this.objectOperations = new ObjectOperations(binaryOperations, serializer, stringCodec);
		this.casOperations = new RedisCASOperations(objectOperations, serializer, stringCodec);
	}

	public RedisOperations<String, String> getStringOperations() {
		return stringOperations;
	}

	public RedisOperations<byte[], byte[]> getBinaryOperations() {
		return binaryOperations;
	}

	public RedisOperations<String, Object> getObjectOperations() {
		return objectOperations;
	}

	public CASOperations getCASOperations() {
		return casOperations;
	}

}
