package shuchaowen.db.result;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import shuchaowen.beans.BeanFieldListen;
import shuchaowen.common.ClassInfo;
import shuchaowen.common.Logger;
import shuchaowen.common.exception.ShuChaoWenRuntimeException;
import shuchaowen.common.utils.ClassUtils;
import shuchaowen.common.utils.XTime;
import shuchaowen.db.ColumnInfo;
import shuchaowen.db.DB;
import shuchaowen.db.TableInfo;
import shuchaowen.db.TableMapping;
import shuchaowen.db.annoation.Table;

public final class Result implements Serializable {
	private static final long serialVersionUID = -3443652927449459314L;
	private TableMapping tableMapping;
	private LinkedHashMap<String, Object> dataMap;

	// 缓存一下
	private transient Object[] values;

	public Result() {
	};

	public Result(TableMapping tableMapping, ResultSet resultSet) throws SQLException {
		this.tableMapping = tableMapping;
		render(resultSet);
	}

	public TableMapping getTableMapping() {
		return tableMapping;
	}

	public void setTableMapping(TableMapping tableMapping) {
		this.tableMapping = tableMapping;
	}

	public String getTableName(Class<?> tableClass) {
		return tableMapping == null ? DB.getTableInfo(tableClass).getName() : tableMapping.getTableName(tableClass);
	}

	public void render(ResultSet resultSet) throws SQLException {
		if (dataMap == null) {
			dataMap = new LinkedHashMap<String, Object>();
		}

		ResultSetMetaData rsmd = resultSet.getMetaData();
		int columnCount = rsmd.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			StringBuilder sb = new StringBuilder();
			String tName = rsmd.getTableName(i);
			if (tName != null && tName.length() != 0) {
				sb.append(tName);
				sb.append(".");
			}
			sb.append(rsmd.getColumnName(i));
			dataMap.put(sb.toString(), resultSet.getObject(i));
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<?> tableClass, String name) {
		if (dataMap == null) {
			return null;
		}

		TableInfo tableInfo = DB.getTableInfo(tableClass);
		String tableName = tableMapping == null ? tableInfo.getName() : tableMapping.getTableName(tableClass);
		ColumnInfo columnInfo = tableInfo.getColumnInfo(name);
		StringBuilder sb = new StringBuilder(tableName);
		sb.append(".");
		sb.append(columnInfo.getName());
		return (T) dataMap.get(sb.toString());
	}

	public Object[] getValues() {
		if (values == null && dataMap != null) {
			values = dataMap.values().toArray();
		}
		return values;
	}

	public LinkedHashMap<String, Object> getDataMap() {
		return dataMap;
	}

	public void setDataMap(LinkedHashMap<String, Object> dataMap) {
		this.dataMap = dataMap;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type) {
		if (dataMap == null) {
			return null;
		}

		if (type.isArray()) {
			return (T) getValues();
		} else if (type.getName().startsWith("java") || ClassUtils.containsBasicValueType(type)) {
			for (Entry<String, Object> entry : dataMap.entrySet()) {
				return (T) entry.getValue();
			}
			return null;
		} else {
			TableInfo tableInfo = DB.getTableInfo(type);
			String tableName = getTableName(type);
			T t = newInstance(tableInfo.getClassInfo());
			boolean b;
			try {
				b = wrapper(t, tableName + ".", tableInfo);
			} catch (Exception e) {
				throw new ShuChaoWenRuntimeException(e);
			}

			if (b) {
				if (tableInfo.isTable()) {
					((BeanFieldListen) t).start_field_listen();
				}
				return t;
			}
			return null;
		}
	}

	private boolean wrapper(Object root, String prefix, TableInfo tableInfo)
			throws IllegalArgumentException, IllegalAccessException {
		boolean b = (tableInfo.getColumns().length == 0);
		for (ColumnInfo columnInfo : tableInfo.getColumns()) {
			String name;
			if (prefix == null) {
				name = columnInfo.getName();
			} else {
				name = prefix + columnInfo.getName();
			}

			if (dataMap.containsKey(name)) {
				columnInfo.setValueToField(root, dataMap.get(name));
				if (!b) {
					b = true;
				}
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(tableInfo.getClassInfo().getName());
				sb.append(" [");
				sb.append(columnInfo.getName());
				sb.append("] not found for DataSource");
				Logger.warn("Result", sb.toString());
			}
		}

		if (b) {
			for (ColumnInfo columnInfo : tableInfo.getTableColumns()) {
				TableInfo info = DB.getTableInfo(columnInfo.getType());
				Object obj = newInstance(info.getClassInfo());
				String tName = getTableName(columnInfo.getType());
				boolean b1 = wrapper(obj, tName + ".", info);
				if (b1) {
					columnInfo.setValueToField(root, obj);
					if (info.isTable()) {
						((BeanFieldListen) obj).start_field_listen();
					}
				}
			}
		}
		return b;
	}

	public Object getObject(int index) {
		Object[] values = getValues();
		if (values == null) {
			return null;
		}
		return values[index];
	}

	public String getString(int index) {
		Object value = getObject(index);
		return value == null ? null : value.toString();
	}

	public Long getLong(int index) {
		Object value = getObject(index);
		return value == null ? null : (Long) value;
	}

	public Integer getInteger(int index) {
		Object value = getObject(index);
		return value == null ? null : (Integer) value;
	}

	public Short getShort(int index) {
		Object value = getObject(index);
		return value == null ? null : (Short) value;
	}

	public String getFormatDate(int index, String formatter) {
		Object value = getObject(index);
		if (value == null) {
			return null;
		}

		if (value instanceof Date) {
			return XTime.format((Date) value, formatter);
		} else if (value instanceof Long) {
			return XTime.format((Long) value, formatter);
		} else {
			return value.toString();
		}
	}

	public Long getTime(int index, String formatter) {
		Object value = getObject(index);
		if (value == null) {
			return null;
		}

		if (value instanceof Date) {
			return ((Date) value).getTime();
		} else if (value instanceof Long) {
			return (Long) value;
		} else {
			return XTime.getTime(value.toString(), formatter);
		}
	}

	public Date getDate(int index) {
		Object value = getObject(index);
		if (value == null) {
			return null;
		}

		if (value instanceof Date) {
			return (Date) value;
		} else if (value instanceof Long) {
			return new Date((Long) value);
		}
		throw new NullPointerException("to date error value:" + value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(ClassInfo classInfo) {
		Table table = classInfo.getClz().getAnnotation(Table.class);
		if (table == null) {
			try {
				return (T) classInfo.getClz().newInstance();
			} catch (InstantiationException e) {
				throw new ShuChaoWenRuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new ShuChaoWenRuntimeException(e);
			}
		} else {
			return (T) classInfo.newFieldListenInstance();
		}
	}
}