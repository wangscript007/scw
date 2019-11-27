package scw.orm.sql;

public class SingleTableNameMapping implements TableNameMapping {
	private Class<?> clazz;
	private String tableName;

	public SingleTableNameMapping(Class<?> clazz, String tableName) {
		this.clazz = clazz;
		this.tableName = tableName;
	}

	public String getTableName(Class<?> clazz) {
		return this.clazz == clazz ? tableName : null;
	}
}
