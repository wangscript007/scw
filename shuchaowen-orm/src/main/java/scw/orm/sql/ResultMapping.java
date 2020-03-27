package scw.orm.sql;

import java.io.Serializable;
import java.util.Map;

public interface ResultMapping extends Serializable {
	public static final EmptyResult EMPTY_RESULT = new EmptyResult();
	
	<T> T get(Class<? extends T> clazz, TableNameMapping tableNameMapping);

	<T> T get(Class<? extends T> clazz, String tableName);

	<T> T get(Class<? extends T> clazz);

	Map<String, Object> getTableValueMap(String tableName);

	Object[] getValues();

	<T> T get(Class<? extends T> type, int index);

	<T> T get(int index);

	int size();

	boolean isEmpty();
}
