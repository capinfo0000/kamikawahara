package otameshirenshuu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import otameshirenshuu.Slip.Entry; // 明細クラス（Slipの入れ子）を Entry の名前で使う

/*
 * ============================================================================
 * このクラス1つで「伝票のデータ処理」をまとめて担当します。
 *   ・データベースへの接続
 *   ・テーブルの自動作成（初回や、消えてしまったとき）
 *   ・一覧の検索／並び替え、1件取得、登録／更新、削除
 *   ・画面表示用の小さな整形（金額を「¥1,000」にする等）
 * サーブレットは使いません。JSP から  new SlipDao()  して各メソッドを呼びます。
 * （以前は Db / SchemaInit / SlipStore / SlipDao / SlipListServlet に分かれていた
 *   ものを、分かりやすいようにこの1ファイルへまとめました）
 * ============================================================================
 */
public class SlipDao {

	// ---- 接続設定（XAMPPのMySQL/MariaDBの初期値）----
	private static String url =
			"jdbc:mariadb://localhost:3306/otameshirenshuu?useUnicode=true&characterEncoding=utf8";
	private static String serverUrl =
			"jdbc:mariadb://localhost:3306/?useUnicode=true&characterEncoding=utf8"; // DB作成用
	private static String user = "root";
	private static String password = "";

	static {
		// MariaDBのドライバを読み込む（jarは WEB-INF/lib にある）
		try {
			Class.forName("org.mariadb.jdbc.Driver");
		} catch (Throwable ignore) {
		}
	}

	/** テスト用に接続先を差し替える（普段は使わない）。 */
	public static void configure(String u, String usr, String pw) {
		url = u; user = usr; password = pw;
	}

	/** コンストラクタ：使い始めるときにテーブルが無ければ作る。 */
	public SlipDao() {
		try {
			ensureSchema();
		} catch (SQLException e) {
			// MySQLが起動していない等。操作時にもう一度試すのでここでは止めない。
			System.out.println("[SlipDao] 起動時のDB準備に失敗（操作時に再試行）: " + e.getMessage());
		}
	}

	// ========================================================================
	// 一覧：検索＋並び替え（SQLの WHERE と ORDER BY で行う）
	// q  … 日付・取引先・購入物のキーワード
	// no … 伝票番号。単一「5」／範囲「1~10」（〜 ～ - も可、片側省略も可）
	// ========================================================================
	public List<Slip> findFiltered(String q, String no, String sortKey, String order) {
		// 並べる列と向き（想定外の値は既定にして安全に）
		final String col = "date".equals(sortKey) ? "slip_date" : "id";
		final String dir = "desc".equals(order) ? "DESC" : "ASC";

		final List<String> conds = new ArrayList<>();  // WHEREの条件
		final List<Object> params = new ArrayList<>(); // ?に入れる値
		addNoCondition(conds, params, no);              // 伝票番号の条件
		if (q != null && !q.trim().isEmpty()) {         // キーワードの条件（部分一致）
			String like = "%" + q.trim() + "%";
			conds.add("(slip_date LIKE ? OR REPLACE(slip_date,'-','/') LIKE ? "
					+ "OR partner_name LIKE ? OR description LIKE ?)");
			params.add(like); params.add(like); params.add(like); params.add(like);
		}

		final StringBuilder sql = new StringBuilder(
				"SELECT id, slip_date, partner_name, description, note FROM slip");
		if (!conds.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conds));
		}
		sql.append(" ORDER BY ").append(col).append(" ").append(dir).append(", id ").append(dir);

		return run(() -> {
			List<Slip> list = new ArrayList<>();
			try (Connection c = getConnection();
					PreparedStatement ps = c.prepareStatement(sql.toString())) {
				for (int i = 0; i < params.size(); i++) {
					ps.setObject(i + 1, params.get(i));
				}
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Slip s = mapSlip(rs);
						loadEntries(c, s);
						list.add(s);
					}
				}
			}
			return list;
		});
	}

	/** 1件を明細つきで取得（無ければ null）。明細画面の表示に使う。 */
	public Slip findById(int id) {
		return run(() -> {
			try (Connection c = getConnection();
					PreparedStatement ps = c.prepareStatement(
							"SELECT id, slip_date, partner_name, description, note FROM slip WHERE id=?")) {
				ps.setInt(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) return null;
					Slip s = mapSlip(rs);
					loadEntries(c, s);
					return s;
				}
			}
		});
	}

	/** 削除（明細→伝票の順に消す）。 */
	public void delete(int id) {
		run(() -> {
			try (Connection c = getConnection()) {
				c.setAutoCommit(false);
				try {
					exec(c, "DELETE FROM entry WHERE slip_id=?", id);
					exec(c, "DELETE FROM slip WHERE id=?", id);
					c.commit();
				} catch (SQLException e) {
					c.rollback();
					throw e;
				}
			}
			return null;
		});
	}

	// ========================================================================
	// 登録・更新：画面から来た入力をまとめて処理する
	//  ・借方／貸方を「列ごと」に空白を詰める
	//  ・金額の書式、貸借一致、明細1行以上 をチェック
	//  ・OKなら保存して伝票番号を返す／NGなら errors にメッセージを入れて -1 を返す
	// ========================================================================
	public int saveFromForm(Integer id, String date, String partner, String desc, String note,
			String[] dSub, String[] dAmt, String[] cSub, String[] cAmt, List<String> errors) {

		boolean formatError = false;
		List<String[]> debitSides = new ArrayList<>();  // {科目, 金額(数字のみ)}
		List<String[]> creditSides = new ArrayList<>();

		int rowCount = maxLen(dSub, dAmt, cSub, cAmt);
		for (int i = 0; i < rowCount; i++) {
			String ds = at(dSub, i), da = at(dAmt, i), cs = at(cSub, i), ca = at(cAmt, i);
			// 金額に数字以外が入っていないか
			if (!da.isEmpty() && !da.matches("^[0-9,]+$") && !formatError) {
				errors.add("借方金額に半角数字以外が入力されています。"); formatError = true;
			}
			if (!ca.isEmpty() && !ca.matches("^[0-9,]+$") && !formatError) {
				errors.add("貸方金額に半角数字以外が入力されています。"); formatError = true;
			}
			// 借方・貸方それぞれ、入力がある行だけを上へ詰める
			if (!ds.isEmpty() || !da.isEmpty()) debitSides.add(new String[] { ds, digits(da) });
			if (!cs.isEmpty() || !ca.isEmpty()) creditSides.add(new String[] { cs, digits(ca) });
		}

		// 詰めた借方・貸方を上から突き合わせて明細行を作る＋合計
		List<Entry> entries = new ArrayList<>();
		int debitTotal = 0, creditTotal = 0;
		int rows = Math.max(debitSides.size(), creditSides.size());
		for (int i = 0; i < rows; i++) {
			String[] d = (i < debitSides.size()) ? debitSides.get(i) : new String[] { "", "" };
			String[] cc = (i < creditSides.size()) ? creditSides.get(i) : new String[] { "", "" };
			Entry e = new Entry(d[0], d[1], cc[0], cc[1]);
			debitTotal += e.getDebitValue();
			creditTotal += e.getCreditValue();
			entries.add(e);
		}

		if (entries.isEmpty()) {
			errors.add("明細を1行以上入力してください。");
		}
		if (!formatError && !entries.isEmpty() && debitTotal != creditTotal) {
			errors.add("借方と貸方の合計金額が一致しません。");
		}
		if (!errors.isEmpty()) {
			return -1; // 保存せず、呼び出し側で入力画面に戻す
		}

		// 保存する伝票を組み立て
		Slip slip = new Slip();
		slip.setId(id == null ? 0 : id);
		slip.setDate(nz(date));
		slip.setPartnerName(nz(partner));
		slip.setDescription(nz(desc));
		slip.setNote(nz(note));
		slip.setEntries(entries);
		return save(slip);
	}

	/** 入力エラー時に、送信された全行（空白行も含む）を復元して画面へ戻す用。 */
	public List<Entry> rebuildRows(String[] dSub, String[] dAmt, String[] cSub, String[] cAmt) {
		List<Entry> list = new ArrayList<>();
		int n = maxLen(dSub, dAmt, cSub, cAmt);
		for (int i = 0; i < n; i++) {
			list.add(new Entry(at(dSub, i), digits(at(dAmt, i)), at(cSub, i), digits(at(cAmt, i))));
		}
		if (list.isEmpty()) list.add(new Entry());
		return list;
	}

	/** 伝票を保存。id が 0 なら新規登録（採番）、それ以外は更新。保存後のidを返す。 */
	private int save(Slip slip) {
		return run(() -> {
			try (Connection c = getConnection()) {
				c.setAutoCommit(false);
				try {
					int savedId;
					if (slip.getId() == 0) {
						try (PreparedStatement ps = c.prepareStatement(
								"INSERT INTO slip(slip_date,partner_name,description,note) VALUES(?,?,?,?)",
								Statement.RETURN_GENERATED_KEYS)) {
							bindHeader(ps, slip);
							ps.executeUpdate();
							try (ResultSet gk = ps.getGeneratedKeys()) {
								gk.next(); savedId = gk.getInt(1);
							}
						}
					} else {
						savedId = slip.getId();
						try (PreparedStatement ps = c.prepareStatement(
								"UPDATE slip SET slip_date=?,partner_name=?,description=?,note=? WHERE id=?")) {
							bindHeader(ps, slip);
							ps.setInt(5, savedId);
							ps.executeUpdate();
						}
						exec(c, "DELETE FROM entry WHERE slip_id=?", savedId); // 明細は入れ替え
					}
					insertEntries(c, savedId, slip.getEntries());
					c.commit();
					return savedId;
				} catch (SQLException e) {
					c.rollback();
					throw e;
				}
			}
		});
	}

	// ========================================================================
	// 表示用の整形ヘルパー（金額や日付を見やすい文字列にする。重複を避けてここに集約）
	// ========================================================================

	/** 金額を「¥1,000」にする（0でも ¥0）。合計欄などに。 */
	public static String yen(int n) { return "¥" + String.format("%,d", n); }

	/** 金額を「¥1,000」に。ただし0は空欄。閲覧の明細金額に。 */
	public static String yenOrBlank(int n) { return n == 0 ? "" : yen(n); }

	/** 入力欄の値用「1,000」。0は空欄。 */
	public static String amount(int n) { return n == 0 ? "" : String.format("%,d", n); }

	/** 日付を "2026-04-23" → "2026/04/23" に。 */
	public static String slash(String date) { return date == null ? "" : date.replace("-", "/"); }

	/** HTMLとして安全な文字に変換（<, >, &, " を無害化）。 */
	public static String esc(String v) {
		if (v == null) return "";
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	// ========================================================================
	// ここから下は「内部で使う道具」
	// ========================================================================

	/** SQL実行。失敗したらテーブルを作り直して1回だけ再試行（DBが消えても自動回復）。 */
	private <T> T run(SqlCall<T> call) {
		try {
			return call.run();
		} catch (SQLException first) {
			try {
				ensureSchema();
				return call.run();
			} catch (SQLException second) {
				throw new RuntimeException("データベース処理に失敗しました。"
						+ "XAMPPのMySQL(MariaDB)が起動しているか確認してください: " + second.getMessage(), second);
			}
		}
	}

	@FunctionalInterface
	private interface SqlCall<T> {
		T run() throws SQLException;
	}

	/** DB・テーブルが無ければ作る（何度呼んでも安全）。 */
	private void ensureSchema() throws SQLException {
		// データベース本体（失敗しても既にあれば続行）
		try (Connection c = DriverManager.getConnection(serverUrl, user, password);
				Statement st = c.createStatement()) {
			st.executeUpdate("CREATE DATABASE IF NOT EXISTS otameshirenshuu DEFAULT CHARACTER SET utf8mb4");
		} catch (SQLException ignore) {
		}
		// テーブル
		try (Connection c = getConnection(); Statement st = c.createStatement()) {
			st.executeUpdate("CREATE TABLE IF NOT EXISTS slip ("
					+ "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "slip_date VARCHAR(10) NOT NULL,"
					+ "partner_name VARCHAR(255) NOT NULL DEFAULT '',"
					+ "description VARCHAR(255) NOT NULL DEFAULT '',"
					+ "note TEXT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
			st.executeUpdate("CREATE TABLE IF NOT EXISTS entry ("
					+ "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "slip_id INT NOT NULL,"
					+ "line_no INT NOT NULL,"
					+ "debit_subject VARCHAR(255) NOT NULL DEFAULT '',"
					+ "debit_amount INT NOT NULL DEFAULT 0,"
					+ "credit_subject VARCHAR(255) NOT NULL DEFAULT '',"
					+ "credit_amount INT NOT NULL DEFAULT 0,"
					+ "CONSTRAINT fk_entry_slip FOREIGN KEY (slip_id) REFERENCES slip(id) ON DELETE CASCADE"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
		}
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	/** SELECT結果の1行を Slip に詰める（明細はまだ）。 */
	private Slip mapSlip(ResultSet rs) throws SQLException {
		Slip s = new Slip();
		s.setId(rs.getInt("id"));
		s.setDate(nz(rs.getString("slip_date")));
		s.setPartnerName(nz(rs.getString("partner_name")));
		s.setDescription(nz(rs.getString("description")));
		s.setNote(nz(rs.getString("note")));
		return s;
	}

	/** その伝票の明細を line_no 順に読み込む。 */
	private void loadEntries(Connection c, Slip s) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT debit_subject,debit_amount,credit_subject,credit_amount "
				+ "FROM entry WHERE slip_id=? ORDER BY line_no")) {
			ps.setInt(1, s.getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					s.getEntries().add(new Entry(
							nz(rs.getString("debit_subject")), intToStr(rs.getInt("debit_amount")),
							nz(rs.getString("credit_subject")), intToStr(rs.getInt("credit_amount"))));
				}
			}
		}
	}

	private void bindHeader(PreparedStatement ps, Slip slip) throws SQLException {
		ps.setString(1, nz(slip.getDate()));
		ps.setString(2, nz(slip.getPartnerName()));
		ps.setString(3, nz(slip.getDescription()));
		ps.setString(4, nz(slip.getNote()));
	}

	private void insertEntries(Connection c, int slipId, List<Entry> entries) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO entry(slip_id,line_no,debit_subject,debit_amount,credit_subject,credit_amount) "
				+ "VALUES(?,?,?,?,?,?)")) {
			int line = 1;
			for (Entry e : entries) {
				ps.setInt(1, slipId);
				ps.setInt(2, line++);
				ps.setString(3, nz(e.getDebitSubject()));
				ps.setInt(4, e.getDebitValue());
				ps.setString(5, nz(e.getCreditSubject()));
				ps.setInt(6, e.getCreditValue());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	/** 「id=?」だけの単純なUPDATE/DELETEを実行する小道具。 */
	private void exec(Connection c, String sql, int id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
	}

	/** 伝票番号（単一/範囲）を WHERE 条件に変換して追加。 */
	private void addNoCondition(List<String> conds, List<Object> params, String no) {
		if (no == null || no.trim().isEmpty()) return;
		String t = no.trim().replace('～', '~').replace('〜', '~')
				.replace('－', '~').replace('−', '~').replace('-', '~');
		if (t.contains("~")) { // 範囲指定
			String[] p = t.split("~", -1);
			Integer lo = numOrNull(p[0]);
			Integer hi = numOrNull(p[p.length - 1]);
			if (lo == null && hi == null) return;
			if (lo != null && hi != null && lo > hi) { int tmp = lo; lo = hi; hi = tmp; }
			if (lo != null) { conds.add("id >= ?"); params.add(lo); }
			if (hi != null) { conds.add("id <= ?"); params.add(hi); }
		} else { // 単一指定
			Integer v = numOrNull(t);
			if (v != null) { conds.add("id = ?"); params.add(v); }
			else conds.add("1 = 0"); // 数字でなければ該当なし
		}
	}

	// --- ちょっとした変換ヘルパー ---
	private static String intToStr(int v) { return v == 0 ? "" : String.valueOf(v); }
	private static String nz(String s) { return s == null ? "" : s; }
	private static String digits(String s) { return s == null ? "" : s.replaceAll("[^0-9]", ""); }

	private static String at(String[] arr, int i) {
		return (arr != null && arr.length > i && arr[i] != null) ? arr[i] : "";
	}

	private static int maxLen(String[]... arrays) {
		int max = 0;
		for (String[] a : arrays) if (a != null && a.length > max) max = a.length;
		return max;
	}

	private static Integer numOrNull(String s) {
		if (s == null) return null;
		String d = s.replaceAll("[^0-9]", "");
		if (d.isEmpty()) return null;
		try { return Integer.valueOf(d); } catch (NumberFormatException e) { return null; }
	}
}
