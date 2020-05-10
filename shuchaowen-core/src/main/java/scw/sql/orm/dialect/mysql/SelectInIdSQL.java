package scw.sql.orm.dialect.mysql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import scw.core.utils.ArrayUtils;
import scw.core.utils.CollectionUtils;
import scw.sql.orm.Column;
import scw.sql.orm.ObjectRelationalMapping;
import scw.sql.orm.dialect.SqlDialectException;

public final class SelectInIdSQL extends MysqlDialectSql {
	private static final long serialVersionUID = 1L;
	private static final String IN = " in (";
	private String sql;
	private Object[] params;

	public SelectInIdSQL(ObjectRelationalMapping objectRelationalMapping, Class<?> clazz, String tableName, Object[] primaryKeys,
			Collection<?> inPrimaryKeys) {
		if (CollectionUtils.isEmpty(inPrimaryKeys)) {
			throw new SqlDialectException("in 语句至少要有一个in条件");
		}

		Collection<Column> primaryKeyColumns = objectRelationalMapping.getPrimaryKeys(clazz);
		int whereSize = ArrayUtils.isEmpty(primaryKeys) ? 0 : primaryKeys.length;
		if (whereSize > primaryKeyColumns.size()) {
			throw new NullPointerException("primaryKeys length  greater than primary key lenght");
		}

		List<Object> params = new ArrayList<Object>(inPrimaryKeys.size() + whereSize);
		StringBuilder sb = new StringBuilder();
		Iterator<Column> iterator = primaryKeyColumns.iterator();
		if (whereSize > 0) {
			for (int i = 0; i < whereSize && iterator.hasNext(); i++) {
				if (sb.length() != 0) {
					sb.append(AND);
				}

				Column column = iterator.next();
				keywordProcessing(sb, column.getName());
				sb.append("=?");
				params.add(primaryKeys[i]);
			}
		}
		
		if(iterator.hasNext()){
			if (sb.length() != 0) {
				sb.append(AND);
			}
			
			keywordProcessing(sb, iterator.next().getName());
			sb.append(IN);
			Iterator<?> valueIterator = inPrimaryKeys.iterator();
			while (valueIterator.hasNext()) {
				params.add(valueIterator.next());
				sb.append("?");
				if (valueIterator.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		}

		String where = sb.toString();
		sb = new StringBuilder();
		sb.append(SELECT_ALL_PREFIX);
		keywordProcessing(sb, tableName);
		sb.append(WHERE).append(where);
		this.sql = sb.toString();
		this.params = params.toArray(new Object[params.size()]);
	}

	public String getSql() {
		return sql;
	}

	public Object[] getParams() {
		return params;
	}
}
