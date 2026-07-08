package otameshirenshuu;

import java.sql.SQLException;
import java.util.List;

/**
 * 伝票データへのアクセス窓口。
 * 以前はインメモリ保持だったが、データベース(MariaDB)保存に変更した。
 * 画面・サーブレット側のコードを変えずに済むよう、これまでと同じメソッドを提供する。
 * DBの検査例外(SQLException)は実行時例外に変換して呼び出し側に伝える。
 */
public class SlipStore {

	private static final SlipStore INSTANCE = new SlipStore();

	public static SlipStore getInstance() {
		return INSTANCE;
	}

	private final SlipDao dao = new SlipDao();

	private SlipStore() {
		try {
			// DB・テーブルの自動作成と初回サンプル投入
			SchemaInit.ensure();
		} catch (SQLException e) {
			throw new RuntimeException("データベースの初期化に失敗しました。"
					+ "XAMPPのMySQL(MariaDB)が起動しているか確認してください: " + e.getMessage(), e);
		}
	}

	public List<Slip> findAll() {
		try {
			return dao.findAllSorted("id", "desc");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Slip> findAllSorted(String key, String order) {
		try {
			return dao.findAllSorted(key, order);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Slip> findFiltered(String q, String key, String order) {
		return findFiltered(q, "", key, order);
	}

	public List<Slip> findFiltered(String q, String no, String key, String order) {
		try {
			return dao.findFiltered(q, no, key, order);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Slip findById(int id) {
		try {
			return dao.findById(id);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int add(Slip slip) {
		try {
			return dao.insert(slip);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(Slip slip) {
		try {
			dao.update(slip);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void delete(int id) {
		try {
			dao.delete(id);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
