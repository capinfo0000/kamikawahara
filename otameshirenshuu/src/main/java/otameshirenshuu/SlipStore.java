package otameshirenshuu;

import java.sql.SQLException;
import java.util.List;

/**
 * 伝票データへのアクセス窓口。
 * 以前はインメモリ保持だったが、データベース(MariaDB)保存に変更した。
 * 画面・サーブレット側のコードを変えずに済むよう、これまでと同じメソッドを提供する。
 *
 * ・DBの検査例外(SQLException)は実行時例外に変換して呼び出し側に伝える。
 * ・DBやテーブルが無い状態で呼ばれた場合は、自動でスキーマを作り直して1回だけ再試行する
 *   （MySQLをリセットしてDBが消えても、Tomcatを再起動せずに自動回復できる）。
 */
public class SlipStore {

	private static final SlipStore INSTANCE = new SlipStore();

	public static SlipStore getInstance() {
		return INSTANCE;
	}

	private final SlipDao dao = new SlipDao();

	private SlipStore() {
		// 起動時にDB・テーブルの作成と初回サンプル投入を試みる。
		// ここで失敗しても（MySQL未起動など）例外は投げず、実際の操作時に再試行する。
		try {
			SchemaInit.ensure();
		} catch (SQLException e) {
			System.out.println("[SlipStore] 起動時のDB初期化に失敗しました（操作時に再試行します）: " + e.getMessage());
		}
	}

	/** DB操作を実行。失敗したらスキーマを作り直して1回だけ再試行する。 */
	private <T> T exec(DaoCall<T> call) {
		try {
			return call.run();
		} catch (SQLException first) {
			// DB/テーブルが無い等の可能性 → 初期化してから再試行
			try {
				SchemaInit.ensure();
				return call.run();
			} catch (SQLException second) {
				throw new RuntimeException("データベース処理に失敗しました。"
						+ "XAMPPのMySQL(MariaDB)が起動しているか確認してください: " + second.getMessage(), second);
			}
		}
	}

	@FunctionalInterface
	private interface DaoCall<T> {
		T run() throws SQLException;
	}

	public List<Slip> findAll() {
		return findAllSorted("id", "desc");
	}

	public List<Slip> findAllSorted(String key, String order) {
		return exec(() -> dao.findAllSorted(key, order));
	}

	public List<Slip> findFiltered(String q, String key, String order) {
		return findFiltered(q, "", key, order);
	}

	public List<Slip> findFiltered(String q, String no, String key, String order) {
		return exec(() -> dao.findFiltered(q, no, key, order));
	}

	public Slip findById(int id) {
		return exec(() -> dao.findById(id));
	}

	public int add(Slip slip) {
		return exec(() -> dao.insert(slip));
	}

	public void update(Slip slip) {
		exec(() -> {
			dao.update(slip);
			return null;
		});
	}

	public void delete(int id) {
		exec(() -> {
			dao.delete(id);
			return null;
		});
	}
}
