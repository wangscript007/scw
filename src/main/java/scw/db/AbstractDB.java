package scw.db;

import java.sql.SQLException;

import scw.common.Iterator;
import scw.common.Logger;
import scw.database.DataBaseUtils;
import scw.sql.ConnectionFactory;
import scw.sql.Sql;
import scw.sql.orm.ORMUtils;
import scw.sql.orm.SqlFormat;
import scw.sql.orm.SqlSelect;
import scw.sql.orm.TableInfo;
import scw.sql.orm.cache.AbstractORMCacheTemplate;
import scw.sql.orm.mysql.MysqlFormat;
import scw.sql.orm.result.DefaultResult;
import scw.sql.orm.result.Result;

public abstract class AbstractDB extends AbstractORMCacheTemplate
		implements ConnectionFactory, AutoCloseable, SqlSelect, MaxIdByDB {
	{
		Logger.info("Init DB for className:" + this.getClass().getName());
	}

	public AbstractDB(SqlFormat sqlFormat) {
		super(sqlFormat == null ? new MysqlFormat() : sqlFormat, null);
	}

	public void iterator(Class<?> tableClass, Iterator<Result> iterator) {
		TableInfo tableInfo = ORMUtils.getTableInfo(tableClass);
		iterator(getSqlFormat().toSelectByIdSql(tableInfo, tableInfo.getName(), null), iterator);
	}

	public void iterator(Sql sql, final Iterator<Result> iterator) {
		DataBaseUtils.iterator(this, sql, new Iterator<java.sql.ResultSet>() {

			public void iterator(java.sql.ResultSet data) {
				try {
					iterator.iterator(new DefaultResult(data));
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
