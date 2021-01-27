package scw.hikari;

import java.sql.Connection;
import java.sql.SQLException;

import scw.db.ConfigurableDB;
import scw.db.DBUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariDB extends ConfigurableDB {
	private HikariDataSource dataSource;

	static {
		HikariConfig.class.getName();
	}

	public HikariDB(String configLocation) {
		super(configLocation);
		initializing();
	}

	public HikariDataSource getDatasource() {
		return dataSource;
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	protected void initConfig() {
		if (getPropertyManager() != null && dataSource == null) {
			HikariConfig config = new HikariConfig();
			DBUtils.loadProperties(config, getPropertyManager());
			this.dataSource = new HikariDataSource(config);
		}

		if (dataSource != null) {
			setDataBase(DBUtils.automaticRecognition(dataSource.getDriverClassName(), dataSource.getJdbcUrl(),
					dataSource.getUsername(), dataSource.getPassword()));
		}
		super.initConfig();
	}

	@Override
	public synchronized void destroy() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
		super.destroy();
	}
}
