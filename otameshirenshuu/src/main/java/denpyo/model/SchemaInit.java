package denpyo.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 【Model】DB・テーブルの自動作成（と、空なら初期データ投入）。
 * 冪等なので何度呼んでも安全。DBが消えても作り直せる。
 */
public class SchemaInit {

	private static final String CREATE_SLIP =
			"CREATE TABLE IF NOT EXISTS slip ("
			+ "  id INT AUTO_INCREMENT PRIMARY KEY,"
			+ "  slip_date VARCHAR(10) NOT NULL,"
			+ "  partner_name VARCHAR(255) NOT NULL DEFAULT '',"
			+ "  description VARCHAR(255) NOT NULL DEFAULT '',"
			+ "  note TEXT"
			+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

	private static final String CREATE_ENTRY =
			"CREATE TABLE IF NOT EXISTS entry ("
			+ "  id INT AUTO_INCREMENT PRIMARY KEY,"
			+ "  slip_id INT NOT NULL,"
			+ "  line_no INT NOT NULL,"
			+ "  debit_subject VARCHAR(255) NOT NULL DEFAULT '',"
			+ "  debit_amount INT NOT NULL DEFAULT 0,"
			+ "  credit_subject VARCHAR(255) NOT NULL DEFAULT '',"
			+ "  credit_amount INT NOT NULL DEFAULT 0,"
			+ "  CONSTRAINT fk_entry_slip FOREIGN KEY (slip_id) REFERENCES slip(id) ON DELETE CASCADE"
			+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

	public static synchronized void ensure() throws SQLException {
		// 1. データベース作成（失敗しても既存なら続行）
		try (Connection c = Db.getServerConnection(); Statement st = c.createStatement()) {
			st.executeUpdate("CREATE DATABASE IF NOT EXISTS otameshirenshuu DEFAULT CHARACTER SET utf8mb4");
		} catch (SQLException e) {
			System.out.println("[denpyo.SchemaInit] DB作成をスキップ: " + e.getMessage());
		}
		// 2. テーブル作成
		try (Connection c = Db.getConnection(); Statement st = c.createStatement()) {
			st.executeUpdate(CREATE_SLIP);
			st.executeUpdate(CREATE_ENTRY);
		}
	}

	/** テスト用：渡した接続にテーブルだけ作る。 */
	public static void createTables(Connection c) throws SQLException {
		try (Statement st = c.createStatement()) {
			st.executeUpdate(CREATE_SLIP);
			st.executeUpdate(CREATE_ENTRY);
		}
	}
}
