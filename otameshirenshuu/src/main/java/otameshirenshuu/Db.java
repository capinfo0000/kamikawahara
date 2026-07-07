package otameshirenshuu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * データベース接続の設定と接続取得をまとめたクラス。
 *
 * 接続先は XAMPP（MariaDB）の既定値。必要ならシステムプロパティで上書きできる:
 *   -Dotameshi.db.url=... -Dotameshi.db.user=... -Dotameshi.db.password=...
 */
public class Db {

	private static String url = System.getProperty(
			"otameshi.db.url",
			"jdbc:mariadb://localhost:3306/otameshirenshuu?useUnicode=true&characterEncoding=utf8");
	private static String serverUrl = System.getProperty(
			"otameshi.db.serverUrl",
			"jdbc:mariadb://localhost:3306/?useUnicode=true&characterEncoding=utf8");
	private static String user = System.getProperty("otameshi.db.user", "root");
	private static String password = System.getProperty("otameshi.db.password", "");

	static {
		// JDBC4以降はドライバが自動登録されるが、念のため明示的に読み込む
		try {
			Class.forName("org.mariadb.jdbc.Driver");
		} catch (Throwable ignore) {
			// ドライバが見つからない場合は getConnection 時に例外となる
		}
	}

	/** アプリ用データベース(otameshirenshuu)への接続を返す。 */
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	/** データベース作成用に、DBを指定しないサーバー接続を返す。 */
	public static Connection getServerConnection() throws SQLException {
		return DriverManager.getConnection(serverUrl, user, password);
	}

	/** テストなどで接続先を差し替える。 */
	public static void configure(String newUrl, String newUser, String newPassword) {
		url = newUrl;
		user = newUser;
		password = newPassword;
	}
}
