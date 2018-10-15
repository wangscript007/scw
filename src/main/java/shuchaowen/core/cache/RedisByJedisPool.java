package shuchaowen.core.cache;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import shuchaowen.core.exception.ShuChaoWenRuntimeException;

public class RedisByJedisPool implements Redis{
	private final JedisPool jedisPool;
	//发生异常时是否中断
	private final boolean abnormalInterruption;
	
	/**
	 * @param jedisPool
	 * @param abnormalInterruption 发生异常时是否中断
	 */
	public RedisByJedisPool(JedisPool jedisPool, boolean abnormalInterruption){
		this.jedisPool = jedisPool;
		this.abnormalInterruption = abnormalInterruption;
	}
	
	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public String get(String key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.get(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public byte[] get(byte[] key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.get(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public String set(String key, String value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.set(key, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public String set(byte[] key, byte[] value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.set(key, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public String setex(String key, int seconds, String value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.setex(key, seconds, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public String setex(byte[] key, int seconds, byte[] value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.setex(key, seconds, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Boolean exists(String key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.exists(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return false;
	}

	public Boolean exists(byte[] key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.exists(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return false;
	}

	public Long expire(String key, int seconds) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.expire(key, seconds);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long expire(byte[] key, int seconds) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.expire(key, seconds);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long delete(String key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.del(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long delete(byte[] key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.del(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long delete(String... key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.del(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long delete(byte[]... key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.del(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long hset(String key, String field, String value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hset(key, field, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long hset(byte[] key, byte[] field, byte[] value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hset(key, field, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long hsetnx(String key, String field, String value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hsetnx(key, field, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long hsetnx(byte[] key, byte[] field, byte[] value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hsetnx(key, field, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Map<String, String> get(String... key) {
		Map<String, String> map = new HashMap<String, String>();
		Jedis jedis = jedisPool.getResource();
		try {
			for(String k : key){
				map.put(k, jedis.get(k));
			}
			return map;
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Map<byte[], byte[]> get(byte[]... key) {
		Map<byte[], byte[]> map = new HashMap<byte[], byte[]>();
		Jedis jedis = jedisPool.getResource();
		try {
			for(byte[] k : key){
				map.put(k, jedis.get(k));
			}
			return map;
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long hdel(String key, String... fields) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hdel(key, fields);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long hdel(byte[] key, byte[]... fields) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hdel(key, fields);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Boolean hexists(String key, String field) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hexists(key, field);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return false;
	}

	public Boolean hexists(byte[] key, byte[] field) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.hexists(key, field);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return false;
	}
	
	public Long ttl(byte[] key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.ttl(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long ttl(String key) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.ttl(key);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long setnx(String key, String value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.setnx(key, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}

	public Long setnx(byte[] key, byte[] value) {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.setnx(key, value);
		} catch (Exception e) {
			if(abnormalInterruption){
				throw new ShuChaoWenRuntimeException(e);
			}else{
				e.printStackTrace();
			}
		}finally{
			if(jedis != null){
				jedis.close();
			}
		}
		return null;
	}
}
