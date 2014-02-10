package kc.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import uk.ac.imperial.presage2.db.sql.SqlStorage;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class KCStorage extends SqlStorage {

	PreparedStatement gameInsert;
	PreparedStatement actionInsert;

	@Inject
	public KCStorage(@Named(value = "sql.info") Properties jdbcInfo) {
		super(jdbcInfo);
	}

	@Override
	protected void initTables() {
		super.initTables();

		Statement createTable;
		try {
			createTable = this.conn.createStatement();
			createTable.execute("CREATE TABLE IF NOT EXISTS nArmedBandit "
					+ "(`simId` bigint(20) NOT NULL, `time` int NOT NULL,"
					+ "`strategyId` int NOT NULL,"
					+ "`utility` double NOT NULL,"
					+ "PRIMARY KEY (`simId`, `time`, `strategyId`));");
			createTable
					.execute("CREATE TABLE IF NOT EXISTS gameActions "
							+ "(`simId` bigint(20) NOT NULL, `time` int NOT NULL, "
							+ "`actor` varchar(32) NOT NULL, `strategyId` int NOT NULL,"
							+ "`utility` double NOT NULL,`account` double NOT NULL, "
							+ "PRIMARY KEY(`simId`, `time`, `actor`))");
			createTable.execute("CREATE TABLE IF NOT EXISTS droolsSnapshot "
					+ "(`simId` bigint(20) NOT NULL, `time` int NOT NULL, "
					+ "`object` VARCHAR(255) NOT NULL, "
					+ "INDEX(`simId`, `time`));");

			gameInsert = conn
					.prepareStatement("INSERT INTO nArmedBandit VALUES(?,?,?,?);");
			actionInsert = conn
					.prepareStatement("INSERT INTO gameActions VALUES(?,?,?,?,?,?);");
		} catch (SQLException e) {
			logger.fatal("Couldn't create tables", e);
			throw new RuntimeException(e);
		}

	}

	public Connection getConn() {
		return this.conn;
	}

	public void insertGameRound(int t, int strat, double ut) {
		try {
			gameInsert.setLong(1, this.simId);
			gameInsert.setInt(2, t);
			gameInsert.setInt(3, strat);
			gameInsert.setDouble(4, ut);
			gameInsert.execute();
		} catch (SQLException e) {
			logger.fatal("Error inserting data", e);
			throw new RuntimeException(e);
		}
	}

	public void insertPlayerGameRound(int t, String name, int strat, double ut,
			double account) {
		try {
			actionInsert.setLong(1, this.simId);
			actionInsert.setInt(2, t);
			actionInsert.setString(3, name);
			actionInsert.setInt(4, strat);
			actionInsert.setDouble(5, ut);
			actionInsert.setDouble(6, account);
			actionInsert.execute();
		} catch (SQLException e) {
			logger.fatal("Error inserting data", e);
			throw new RuntimeException(e);
		}
	}

	public void insertDroolsSnapshot(int t, Set<Object> objects) {
		try {
			PreparedStatement logSnapshotInsert = conn
					.prepareStatement("INSERT INTO droolsSnapshot VALUES(?,?,?);");
			for (Object o : objects) {
				logSnapshotInsert.setLong(1, simId);
				logSnapshotInsert.setInt(2, t);
				logSnapshotInsert.setString(3, o.toString());
				logSnapshotInsert.addBatch();
			}
			batchQueryQ.put(logSnapshotInsert);
		} catch (SQLException e) {
			logger.fatal("Error inserting data", e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void updateTransientEnvironment() {
		super.updateTransientEnvironment();
		/*
		 * try { batchQueryQ.put(gameInsert); } catch (InterruptedException e) {
		 * e.printStackTrace(); }
		 */
	}

}
