package denpyo.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 【Model】データベース接続の設定と取得。
 * 接続先は XAMPP(MariaDB) の既定値。テーブルは既存の otameshirenshuu を共有する。
 */
public class Db {

	private static String url =
			"jdbc:mariadb://localhost:3306/otameshirenshuu?useUnicode=true&characterEncoding=utf8";
	private static String serverUrl =
			"jdbc:mariadb://localhost:3306/?useUnicode=true&characterEncoding=utf8";
	private static String user = "root";
	private static String password = "";

	static {
		try {
			Class.forName("org.mariadb.jdbc.Driver");
		} catch (Throwable ignore) {
			// 見つからなければ接続時に例外になる
		}
	}

	/** アプリ用DB(otameshirenshuu)への接続。 */
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	/** DB作成用（DB名を指定しないサーバー接続）。 */
	public static Connection getServerConnection() throws SQLException {
		return DriverManager.getConnection(serverUrl, user, password);
	}

	/** テスト用に接続先を差し替える。 */
	public static void configure(String newUrl, String newUser, String newPassword) {
		url = newUrl;
		user = newUser;
		password = newPassword;
	}
}
