package scw.sql.orm.auto.id;

import javassist.NotFoundException;
import scw.beans.annotation.Bean;
import scw.common.utils.Assert;
import scw.common.utils.StringUtils;
import scw.common.utils.XTime;
import scw.memcached.Memcached;
import scw.redis.Redis;
import scw.sql.orm.ColumnInfo;
import scw.sql.orm.ORMOperations;
import scw.sql.orm.TableInfo;
import scw.sql.orm.auto.AutoCreateService;
import scw.utils.id.IdGenerator;
import scw.utils.id.LongIdGenerator;
import scw.utils.id.MemcachedIdGenerator;
import scw.utils.id.RedisIdGenerator;
import scw.utils.id.SequenceId;

@Bean(proxy = false)
public class SequenceIdAutoCreateService implements AutoCreateService {
	private static final String DEFAULT_TIME_FORMAT = "yyyyMMddHHmmss";
	private final IdGenerator<Long> idGenerator;
	private final String time_format;

	public SequenceIdAutoCreateService() {
		this.idGenerator = new LongIdGenerator();
		this.time_format = DEFAULT_TIME_FORMAT;
	}

	public SequenceIdAutoCreateService(Memcached memcached) {
		this.idGenerator = new MemcachedIdGenerator(memcached, this.getClass().getName(), 0);
		this.time_format = DEFAULT_TIME_FORMAT;
	}

	public SequenceIdAutoCreateService(Redis redis) {
		this.idGenerator = new RedisIdGenerator(redis, this.getClass().getName(), 0);
		this.time_format = DEFAULT_TIME_FORMAT;
	}

	public SequenceIdAutoCreateService(Memcached memcached, String key, String timeformat) {
		Assert.notNull(timeformat);
		Assert.notNull(key);
		this.idGenerator = new MemcachedIdGenerator(memcached, key, 0);
		this.time_format = timeformat;
	}

	public SequenceIdAutoCreateService(Redis redis, String key, String timeformat) {
		Assert.notNull(timeformat);
		Assert.notNull(key);
		this.idGenerator = new RedisIdGenerator(redis, key, 0);
		this.time_format = timeformat;
	}

	public SequenceId next() {
		long t = System.currentTimeMillis();
		int number = idGenerator.next().intValue();
		if (number < 0) {
			number = Integer.MAX_VALUE + number;
		}

		String id = XTime.format(t, time_format) + StringUtils.complemented(number + "", '0', 10);
		return new SequenceId(t, id);
	}

	public void wrapper(ORMOperations ormOperations, Object bean, TableInfo tableInfo, ColumnInfo columnInfo,
			String tableName, String[] args) throws Throwable {
		if (args.length == 0) {
			throw new NotFoundException(
					tableInfo.getClassInfo().getName() + "中字段[" + columnInfo.getName() + "]要生成流水号但找不到时间戳字段");
		}

		ColumnInfo cts = tableInfo.getColumnInfo(args[0]);
		SequenceId id = next();
		columnInfo.setValueToField(bean, id.getId());
		cts.setValueToField(bean, id.getTimestamp());
	}

}