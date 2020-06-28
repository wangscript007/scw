package scw.sql.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import scw.core.GlobalPropertyFactory;
import scw.core.utils.CollectionUtils;
import scw.core.utils.StringUtils;
import scw.lang.Ignore;
import scw.mapper.Field;
import scw.mapper.FieldFilter;
import scw.mapper.FilterFeature;
import scw.mapper.Mapper;
import scw.mapper.MapperUtils;
import scw.sql.orm.annotation.NotColumn;
import scw.sql.orm.annotation.Table;
import scw.util.Accept;

/**
 * 默认的orm定义
 * 
 * @author shuchaowen
 *
 */
public class ObjectRelationalMapping implements FieldFilter {
	/**
	 * 默认对象主键的连接符
	 */
	public static final char PRIMARY_KEY_CONNECTOR_CHARACTER = StringUtils
			.parseChar(GlobalPropertyFactory.getInstance().getString("orm.primary.key.connector.character"), ':');

	public Mapper getMapper() {
		return MapperUtils.getMapper();
	}

	protected void appendObjectKeyByValue(StringBuilder appendable, Column column, Object value) {
		appendable.append(PRIMARY_KEY_CONNECTOR_CHARACTER);
		appendable.append(StringUtils.transferredMeaning(value == null ? null : value.toString(),
				PRIMARY_KEY_CONNECTOR_CHARACTER));
	}

	/**
	 * 迭代可以映射的字段，包含entity字段
	 * 
	 * @param entityClass
	 * @param parentField
	 *            指明父级字段
	 * @return
	 */
	public Enumeration<Column> enumeration(Class<?> entityClass, Field parentField) {
		return new EnumerationColumn(getMapper().enumeration(entityClass, true, parentField, this));
	}

	/**
	 * 迭代可以映射的字段，包含entity字段
	 * 
	 * @param entityClass
	 * @return
	 */
	public final Enumeration<Column> enumeration(Class<?> entityClass) {
		return enumeration(entityClass, null);
	}

	/**
	 * 获取数据库字段
	 * 
	 * @param entityClass
	 * @return
	 */
	public final Collection<Column> getColumns(Class<?> entityClass) {
		LinkedHashSet<Column> columns = new LinkedHashSet<Column>();
		Enumeration<Column> enumeration = enumeration(entityClass);
		while (enumeration.hasMoreElements()) {
			Column column = enumeration.nextElement();
			if (column.isEntity() || columns.contains(column)) {
				continue;
			}

			columns.add(column);
		}
		return columns;
	}

	/**
	 * 获取主键字段
	 * 
	 * @param entityClass
	 * @return
	 */
	public final Collection<Column> getPrimaryKeys(Class<?> entityClass) {
		LinkedHashSet<Column> columns = new LinkedHashSet<Column>();
		Enumeration<Column> enumeration = enumeration(entityClass);
		while (enumeration.hasMoreElements()) {
			Column column = enumeration.nextElement();
			if (!column.isPrimaryKey() || column.isEntity() || columns.contains(column)) {
				continue;
			}

			columns.add(column);
		}
		return columns;
	}

	/**
	 * 获取非主键字段
	 * 
	 * @param entityClass
	 * @return
	 */
	public final Collection<Column> getNotPrimaryKeys(Class<?> entityClass) {
		LinkedHashSet<Column> columns = new LinkedHashSet<Column>();
		Enumeration<Column> enumeration = enumeration(entityClass);
		while (enumeration.hasMoreElements()) {
			Column column = enumeration.nextElement();
			if (column.isPrimaryKey() || column.isEntity() || columns.contains(column)) {
				continue;
			}

			columns.add(column);
		}
		return columns;
	}

	protected Column createColumn(Field field) {
		return new Column(field);
	}

	public final <T> String getObjectKey(Class<? extends T> clazz, final T bean) {
		final StringBuilder sb = new StringBuilder(128);
		sb.append(clazz.getName());
		for (Column column : getPrimaryKeys(clazz)) {
			Object value = column.get(bean);
			appendObjectKeyByValue(sb, column, value);
		}
		return sb.toString();
	}

	public final String getObjectKeyById(Class<?> clazz, Collection<Object> primaryKeys) {
		StringBuilder sb = new StringBuilder(128);
		sb.append(clazz.getName());
		Iterator<Column> columnIterator = getPrimaryKeys(clazz).iterator();
		Iterator<Object> valueIterator = primaryKeys.iterator();
		while (columnIterator.hasNext() && valueIterator.hasNext()) {
			Column column = columnIterator.next();
			appendObjectKeyByValue(sb, column, column.toDataBaseValue(valueIterator.next()));
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public final <K> Map<String, K> getInIdKeyMap(Class<?> clazz, Collection<? extends K> lastPrimaryKeys,
			Object[] primaryKeys) {
		if (CollectionUtils.isEmpty(lastPrimaryKeys)) {
			return Collections.EMPTY_MAP;
		}

		Map<String, K> keyMap = new LinkedHashMap<String, K>();
		Iterator<? extends K> valueIterator = lastPrimaryKeys.iterator();

		while (valueIterator.hasNext()) {
			K k = valueIterator.next();
			Object[] ids;
			if (primaryKeys == null || primaryKeys.length == 0) {
				ids = new Object[] { k };
			} else {
				ids = new Object[primaryKeys.length];
				System.arraycopy(primaryKeys, 0, ids, 0, primaryKeys.length);
				ids[ids.length - 1] = k;
			}
			keyMap.put(getObjectKeyById(clazz, Arrays.asList(ids)), k);
		}
		return keyMap;
	}

	public Column getColumn(Class<?> entityClass, String name) {
		Enumeration<Column> enumeration = enumeration(entityClass);
		while (enumeration.hasMoreElements()) {
			Column column = enumeration.nextElement();
			if (column.isEntity()) {
				continue;
			}

			if (column.getName().equals(name) || column.getField().getGetter().getName().equalsIgnoreCase(name)
					|| column.getField().getSetter().getName().equalsIgnoreCase(name)) {
				return column;
			}
		}
		return null;
	}

	public boolean isTable(Class<?> clazz) {
		return clazz.getAnnotation(Table.class) != null;
	}

	/**
	 * 必须满足的条件
	 */
	public boolean accept(Field field) {
		if (!(field.isSupportGetter() && field.isSupportSetter()
				&& FilterFeature.IGNORE_STATIC.getFilter().accept(field))) {
			return false;
		}

		if (field.getSetter().getField() == null || field.getGetter().getField() == null) {
			return false;
		}

		Ignore ignore = field.getAnnotatedElement().getAnnotation(Ignore.class);
		if (ignore != null) {
			return false;
		}

		NotColumn exclude = field.getAnnotatedElement().getAnnotation(NotColumn.class);
		if (exclude != null) {
			return false;
		}
		return true;
	}

	protected final class EnumerationColumn implements Enumeration<Column> {
		private Enumeration<Field> enumeration;

		public EnumerationColumn(Enumeration<Field> enumeration) {
			this.enumeration = enumeration;
		}

		public boolean hasMoreElements() {
			return enumeration.hasMoreElements();
		}

		public Column nextElement() {
			return createColumn(enumeration.nextElement());
		}
	}

	public Map<IndexInfo, List<IndexInfo>> getIndexInfoMap(Class<?> entityClass) {
		Map<IndexInfo, List<IndexInfo>> indexMap = new LinkedHashMap<IndexInfo, List<IndexInfo>>();
		for (Column column : getColumns(entityClass)) {
			scw.sql.orm.annotation.Index index = column.getField().getAnnotatedElement()
					.getAnnotation(scw.sql.orm.annotation.Index.class);
			if (index == null) {
				continue;
			}

			IndexInfo indexInfo = new IndexInfo(column, index.name(), index.type(), index.length(), index.method(),
					index.order());
			List<IndexInfo> list = indexMap.get(indexInfo);
			if (list == null) {
				list = new ArrayList<IndexInfo>();
				indexMap.put(indexInfo, list);
			}
			list.add(indexInfo);
		}
		return indexMap;
	}

	public String getTableName(Class<?> tableClass) {
		Table table = tableClass.getAnnotation(Table.class);
		if (table == null) {
			return StringUtils.humpNamingReplacement(tableClass.getSimpleName(), "_");
		}

		if (StringUtils.isEmpty(table.name())) {
			return StringUtils.humpNamingReplacement(tableClass.getSimpleName(), "_");
		}
		return table.name();
	}

	public Column findColumn(Class<?> tableClass, Accept<Column> accept) {
		Enumeration<Column> enumeration = enumeration(tableClass);
		if (enumeration.hasMoreElements()) {
			Column column = enumeration.nextElement();
			if (accept.accept(column)) {
				return column;
			}
		}
		return null;
	}
}
