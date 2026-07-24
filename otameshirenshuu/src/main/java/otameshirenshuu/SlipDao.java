package otameshirenshuu;

import java.sql.Connection;        // DBとの接続を表す
import java.sql.DriverManager;     // 接続を作ってくれる係
import java.sql.PreparedStatement; // 「?」付きの安全なSQLを実行する道具
import java.sql.ResultSet;         // SELECTの結果（表）を1行ずつ読む道具
import java.sql.SQLException;      // DB処理で起きる例外（エラー）
import java.sql.Statement;         // 採番されたIDを受け取るとき等に使う
import java.util.ArrayList;        // 可変長の配列（一覧を入れる）

import otameshirenshuu.Slip.Entry; // 明細クラス（Slipの入れ子）を Entry の名前で使う

/*
 * ============================================================================
 * 【SlipDao】= 「伝票データの処理係」クラス
 *   DBへの接続・テーブル作成・検索・登録・削除・表示整形を、この1つで担当します。
 *   画面ごとの受け付け（入力の読み取り等）は SlipPage が行い、そこから
 *   new SlipDao() して、下の各メソッドを呼び出します。サーブレットは使いません。
 *
 *   ◆メソッド一覧（何をするメソッドかの早見表）◆
 *     ・SlipDao()      … コンストラクタ。使い始めにテーブルを用意する
 *     ・findFiltered() … 一覧を「検索＋並び替え」して取り出す
 *     ・findById()     … 伝票を1件だけ取り出す
 *     ・delete()       … 伝票を1件削除する
 *     ・saveFromForm() … 画面入力を検査して、OKなら登録／更新する
 *     ・rebuildRows()  … 入力エラー時に、打ち込んだ行をそのまま復元する
 *     ・yen()/yenOrBlank()/amount()/slash()/esc() … 表示用の文字列整形（JSPも使う）
 *     ・save()         … 実際にDBへ書き込む（新規INSERT／更新UPDATE）
 *     ・ensureSchema() … DBとテーブルが無ければ作る
 *     ・getConnection()… DB接続を1本もらう
 *     ・readSlip()     … SELECT結果の1行＋明細を Slip に組み立てる
 *     ・bindHeader()   … 伝票の見出し4項目をSQLの「?」に流し込む
 *     ・exec()         … 「id=?」だけの単純なSQLを実行する
 *     ・addNoCondition()… 伝票番号の検索（単一/範囲）を WHERE 条件に変える
 *     ・nz()/digits()/at()/maxLen()/numOrNull() … ちょっとした変換の小道具
 * ============================================================================
 */
public class SlipDao {

	// ---- 接続設定（XAMPPのMySQL/MariaDBの初期値）----
	private static String url =        // ← 使うDB「otameshirenshuu」への接続先
			"jdbc:mariadb://localhost:3306/otameshirenshuu?useUnicode=true&characterEncoding=utf8";
	private static String serverUrl =  // ← DBを作るとき用（DB名を付けない接続先）
			"jdbc:mariadb://localhost:3306/?useUnicode=true&characterEncoding=utf8";
	private static String user = "root"; // XAMPPの初期ユーザー
	private static String password = ""; // XAMPPの初期パスワード（空）

	// DB処理に失敗したときに見せるメッセージ（同じ文を何度も書かないよう定数にまとめる）
	private static final String DB_ERROR =
			"データベース処理に失敗しました。XAMPPのMySQL(MariaDB)が起動しているか確認してください: ";

	// クラスが最初に読み込まれた時に1回だけ動く初期化ブロック。ドライバを登録する。
	static {
		try {
			Class.forName("org.mariadb.jdbc.Driver"); // jarは WEB-INF/lib にある
		} catch (Throwable ignore) {
		}
	}

	// ===== 【configure】テスト用に接続先を差し替えるメソッド（普段のアプリでは使わない）=====
	public static void configure(String u, String usr, String pw) {
		url = u; user = usr; password = pw;
	}

	// ===== 【SlipDao()】コンストラクタ：使い始めるときにテーブルが無ければ作るメソッド =====
	//   SlipPage が画面ごとに new SlipDao() するので、ここが毎回呼ばれる＝
	//   もしDBが消えても次のアクセスで作り直され、勝手に復活する仕組み。
	public SlipDao() {
		try {
			ensureSchema();
		} catch (SQLException e) {
			// MySQLが起動していない等。ここでは止めず、メッセージだけ出す。
			System.out.println("[SlipDao] 起動時のDB準備に失敗: " + e.getMessage());
		}
	}

	// ===== 【findFiltered】一覧を「検索＋並び替え」して取り出すメソッド =====
	//   q  … 日付・取引先・購入物のキーワード（部分一致）
	//   no … 伝票番号。単一「5」／範囲「1~10」（〜 ～ - も可、片側省略も可）
	//   戻り値 … 条件に合う伝票の一覧（新しい順など指定どおりに並ぶ）
	public ArrayList<Slip> findFiltered(String q, String no, String sortKey, String order) {
		// (1) 並べる列と向きを決める（想定外の値は既定にして安全に）
		String col = "date".equals(sortKey) ? "slip_date" : "id"; // 日付順か伝票番号順か
		String dir = "desc".equals(order) ? "DESC" : "ASC";       // 新しい順か古い順か

		// (2) WHERE に入れる条件と、「?」に流し込む値を、必要なぶんだけ組み立てる
		ArrayList<String> conds = new ArrayList<>();  // 例:「id = ?」などの条件文
		ArrayList<Object> params = new ArrayList<>(); // 上の「?」に入れる実際の値
		addNoCondition(conds, params, no);            // 伝票番号の条件を追加
		if (q != null && !q.trim().isEmpty()) {       // キーワードがあれば部分一致で追加
			String like = "%" + q.trim() + "%";       // 前後に % を付けると「含む」検索
			conds.add("(slip_date LIKE ? OR REPLACE(slip_date,'-','/') LIKE ? "
					+ "OR partner_name LIKE ? OR description LIKE ?)");
			params.add(like); params.add(like); params.add(like); params.add(like);
		}

		// (3) SQL文を組み立てる（条件があるときだけ WHERE を付ける）
		StringBuilder sql = new StringBuilder(
				"SELECT id, slip_date, partner_name, description, note FROM slip");
		if (!conds.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conds));
		}
		sql.append(" ORDER BY ").append(col).append(" ").append(dir).append(", id ").append(dir);

		// (4) 実行して、1行ずつ Slip に組み立てて一覧に追加する
		try (Connection c = getConnection();
				PreparedStatement ps = c.prepareStatement(sql.toString())) {
			for (int i = 0; i < params.size(); i++) {
				ps.setObject(i + 1, params.get(i)); // 「?」に値を1つずつ入れる（1始まり）
			}
			ArrayList<Slip> list = new ArrayList<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {          // 結果を1行ずつ進める
					list.add(readSlip(c, rs)); // 1行＋明細を Slip にして追加
				}
			}
			return list;
		} catch (SQLException e) {
			throw new RuntimeException(DB_ERROR + e.getMessage(), e); // 呼び出し側に伝える
		}
	}

	// ===== 【findById】伝票を1件だけ取り出すメソッド（明細画面の表示に使う。無ければnull）=====
	public Slip findById(int id) {
		try (Connection c = getConnection();
				PreparedStatement ps = c.prepareStatement(
						"SELECT id, slip_date, partner_name, description, note FROM slip WHERE id=?")) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) return null;   // 1行も無ければ「見つからない」
				return readSlip(c, rs);        // 見つかった1行＋明細を返す
			}
		} catch (SQLException e) {
			throw new RuntimeException(DB_ERROR + e.getMessage(), e);
		}
	}

	// ===== 【delete】伝票を1件削除するメソッド（明細→伝票の順に消す）=====
	//   途中で失敗したら rollback で「無かったこと」にして、DBを壊さない。
	public void delete(int id) {
		try (Connection c = getConnection()) {
			c.setAutoCommit(false); // ここから手動コミット（2つの削除をまとめて確定させる）
			try {
				exec(c, "DELETE FROM entry WHERE slip_id=?", id); // 先に明細を消す
				exec(c, "DELETE FROM slip WHERE id=?", id);       // 次に伝票本体を消す
				c.commit();                                       // ここで確定
			} catch (SQLException e) {
				c.rollback();  // 失敗したら両方とも取り消す
				throw e;
			}
		} catch (SQLException e) {
			throw new RuntimeException(DB_ERROR + e.getMessage(), e);
		}
	}

	// ===== 【saveFromForm】画面入力を検査して、OKなら登録／更新するメソッド =====
	//   ・借方／貸方を「列ごと」に、入力のある行だけ上へ詰める
	//   ・金額の書式・貸借の一致・明細1行以上、をチェック
	//   ・OK → 保存して伝票番号を返す ／ NG → errors に理由を入れて -1 を返す
	public int saveFromForm(Integer id, String date, String partner, String desc, String note,
			String[] dSub, String[] dAmt, String[] cSub, String[] cAmt, ArrayList<String> errors) {

		boolean formatError = false;                     // 金額に数字以外が混ざっていたか
		ArrayList<String[]> debitSides = new ArrayList<>();  // 借方の {科目, 金額} を詰める
		ArrayList<String[]> creditSides = new ArrayList<>(); // 貸方の {科目, 金額} を詰める

		// (1) 送られてきた全行を上から確認する
		int rowCount = maxLen(dSub, dAmt, cSub, cAmt); // 一番長い配列の長さ＝行数
		for (int i = 0; i < rowCount; i++) {
			String ds = at(dSub, i), da = at(dAmt, i), cs = at(cSub, i), ca = at(cAmt, i);
			// 金額に半角数字とカンマ以外が入っていないか（1回だけメッセージを出す）
			if (!da.isEmpty() && !da.matches("^[0-9,]+$") && !formatError) {
				errors.add("借方金額に半角数字以外が入力されています。"); formatError = true;
			}
			if (!ca.isEmpty() && !ca.matches("^[0-9,]+$") && !formatError) {
				errors.add("貸方金額に半角数字以外が入力されています。"); formatError = true;
			}
			// 借方・貸方それぞれ、入力がある行だけ上へ詰める（空行は捨てる）
			if (!ds.isEmpty() || !da.isEmpty()) debitSides.add(new String[] { ds, digits(da) });
			if (!cs.isEmpty() || !ca.isEmpty()) creditSides.add(new String[] { cs, digits(ca) });
		}

		// (2) 詰めた借方・貸方を上から突き合わせて、明細行を作りつつ合計を出す
		ArrayList<Entry> entries = new ArrayList<>();
		int debitTotal = 0, creditTotal = 0;
		int rows = Math.max(debitSides.size(), creditSides.size());
		for (int i = 0; i < rows; i++) {
			String[] d  = (i < debitSides.size())  ? debitSides.get(i)  : new String[] { "", "" };
			String[] cc = (i < creditSides.size()) ? creditSides.get(i) : new String[] { "", "" };
			Entry e = new Entry(d[0], d[1], cc[0], cc[1]);
			debitTotal += e.getDebitValue();
			creditTotal += e.getCreditValue();
			entries.add(e);
		}

		// (3) 中身のチェック（1つでも引っかかれば保存しない）
		if (entries.isEmpty()) {
			errors.add("明細を1行以上入力してください。");
		}
		if (!formatError && !entries.isEmpty() && debitTotal != creditTotal) {
			errors.add("借方と貸方の合計金額が一致しません。");
		}
		if (!errors.isEmpty()) {
			return -1; // NG。呼び出し側で入力画面に戻す
		}

		// (4) OKなら保存用の伝票を組み立てて save() に渡す
		Slip slip = new Slip();
		slip.setId(id == null ? 0 : id); // id が無ければ 0（＝新規登録の合図）
		slip.setDate(nz(date));
		slip.setPartnerName(nz(partner));
		slip.setDescription(nz(desc));
		slip.setNote(nz(note));
		slip.setEntries(entries);
		return save(slip);
	}

	// ===== 【rebuildRows】入力エラー時に、打ち込んだ行をそのまま復元するメソッド =====
	//   空行も含めて画面に戻したいので、詰めずにそのまま Entry にして返す。
	public ArrayList<Entry> rebuildRows(String[] dSub, String[] dAmt, String[] cSub, String[] cAmt) {
		ArrayList<Entry> list = new ArrayList<>();
		int n = maxLen(dSub, dAmt, cSub, cAmt);
		for (int i = 0; i < n; i++) {
			list.add(new Entry(at(dSub, i), digits(at(dAmt, i)), at(cSub, i), digits(at(cAmt, i))));
		}
		if (list.isEmpty()) list.add(new Entry()); // 1行も無ければ空1行を用意
		return list;
	}

	// ========================================================================
	// 表示用の整形メソッド（金額や日付を見やすい文字列にする。JSPからも呼ぶ）
	// ========================================================================

	// ===== 【yen】数値を「¥1,000」の形にするメソッド（0でも ¥0。合計欄などに使う）=====
	public static String yen(int n) { return "¥" + String.format("%,d", n); }

	// ===== 【yenOrBlank】「¥1,000」にするが、0のときは空欄にするメソッド（閲覧の明細金額）=====
	public static String yenOrBlank(int n) { return n == 0 ? "" : yen(n); }

	// ===== 【amount】入力欄用に「1,000」（¥なし）にするメソッド（0は空欄）=====
	public static String amount(int n) { return n == 0 ? "" : String.format("%,d", n); }

	// ===== 【slash】日付 "2026-04-23" を "2026/04/23" に変えるメソッド =====
	public static String slash(String date) { return date == null ? "" : date.replace("-", "/"); }

	// ===== 【esc】HTMLとして危険な文字（< > & "）を無害な表記に変えるメソッド =====
	//   これをしないと、入力にタグが混じったとき表示が壊れたり悪用されたりする。
	public static String esc(String v) {
		if (v == null) return "";
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	// ========================================================================
	// ここから下は「クラスの中だけで使う道具」（private）
	// ========================================================================

	// ===== 【save】実際にDBへ書き込むメソッド（id=0なら新規INSERT、それ以外は更新UPDATE）=====
	//   見出しの保存 → 明細の入れ直し、をまとめて1つの取引（トランザクション）で確定する。
	private int save(Slip slip) {
		try (Connection c = getConnection()) {
			c.setAutoCommit(false); // まとめて確定させたいので手動コミットにする
			try {
				int savedId;
				if (slip.getId() == 0) {
					// --- 新規登録：INSERT して、自動採番された伝票番号を受け取る ---
					try (PreparedStatement ps = c.prepareStatement(
							"INSERT INTO slip(slip_date,partner_name,description,note) VALUES(?,?,?,?)",
							Statement.RETURN_GENERATED_KEYS)) {
						bindHeader(ps, slip);
						ps.executeUpdate();
						try (ResultSet gk = ps.getGeneratedKeys()) {
							gk.next(); savedId = gk.getInt(1); // 付けられた番号を取得
						}
					}
				} else {
					// --- 更新：既存の伝票番号のまま UPDATE ---
					savedId = slip.getId();
					try (PreparedStatement ps = c.prepareStatement(
							"UPDATE slip SET slip_date=?,partner_name=?,description=?,note=? WHERE id=?")) {
						bindHeader(ps, slip);
						ps.setInt(5, savedId);
						ps.executeUpdate();
					}
					exec(c, "DELETE FROM entry WHERE slip_id=?", savedId); // 明細は一度消して入れ直す
				}

				// --- 明細を1行ずつ INSERT（addBatch でためて、最後にまとめて実行）---
				try (PreparedStatement ps = c.prepareStatement(
						"INSERT INTO entry(slip_id,line_no,debit_subject,debit_amount,"
						+ "credit_subject,credit_amount) VALUES(?,?,?,?,?,?)")) {
					int line = 1; // 行番号（並び順を覚えておくため）
					for (Entry e : slip.getEntries()) {
						ps.setInt(1, savedId);
						ps.setInt(2, line++);
						ps.setString(3, nz(e.getDebitSubject()));
						ps.setInt(4, e.getDebitValue());
						ps.setString(5, nz(e.getCreditSubject()));
						ps.setInt(6, e.getCreditValue());
						ps.addBatch();
					}
					ps.executeBatch();
				}

				c.commit();      // すべて成功したのでここで確定
				return savedId;
			} catch (SQLException e) {
				c.rollback();    // 途中で失敗したら全部取り消す
				throw e;
			}
		} catch (SQLException e) {
			throw new RuntimeException(DB_ERROR + e.getMessage(), e);
		}
	}

	// ===== 【ensureSchema】DBとテーブルが無ければ作るメソッド（何度呼んでも安全）=====
	private void ensureSchema() throws SQLException {
		// (1) データベース本体を作る（既にあれば何もしない）
		try (Connection c = DriverManager.getConnection(serverUrl, user, password);
				Statement st = c.createStatement()) {
			st.executeUpdate("CREATE DATABASE IF NOT EXISTS otameshirenshuu DEFAULT CHARACTER SET utf8mb4");
		} catch (SQLException ignore) {
		}
		// (2) 伝票テーブルと明細テーブルを作る（既にあれば何もしない）
		try (Connection c = getConnection(); Statement st = c.createStatement()) {
			st.executeUpdate("CREATE TABLE IF NOT EXISTS slip ("
					+ "id INT AUTO_INCREMENT PRIMARY KEY,"           // 伝票番号（自動採番）
					+ "slip_date VARCHAR(10) NOT NULL,"              // 日付
					+ "partner_name VARCHAR(255) NOT NULL DEFAULT '',"// 取引先
					+ "description VARCHAR(255) NOT NULL DEFAULT '',"// 購入物
					+ "note TEXT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"); // 備考
			st.executeUpdate("CREATE TABLE IF NOT EXISTS entry ("
					+ "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "slip_id INT NOT NULL,"                        // どの伝票の明細か
					+ "line_no INT NOT NULL,"                        // 行番号（並び順）
					+ "debit_subject VARCHAR(255) NOT NULL DEFAULT '',"
					+ "debit_amount INT NOT NULL DEFAULT 0,"
					+ "credit_subject VARCHAR(255) NOT NULL DEFAULT '',"
					+ "credit_amount INT NOT NULL DEFAULT 0,"
					// 伝票が消えたら、その明細も自動で消える設定（ON DELETE CASCADE）
					+ "CONSTRAINT fk_entry_slip FOREIGN KEY (slip_id) REFERENCES slip(id) ON DELETE CASCADE"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
		}
	}

	// ===== 【getConnection】DB接続を1本もらうメソッド =====
	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	// ===== 【readSlip】SELECT結果の1行＋その明細を、1つの Slip に組み立てるメソッド =====
	//   （もとの mapSlip と loadEntries を1つにまとめたもの）
	private Slip readSlip(Connection c, ResultSet rs) throws SQLException {
		// (1) 伝票の見出し部分を Slip に詰める
		Slip s = new Slip();
		s.setId(rs.getInt("id"));
		s.setDate(nz(rs.getString("slip_date")));
		s.setPartnerName(nz(rs.getString("partner_name")));
		s.setDescription(nz(rs.getString("description")));
		s.setNote(nz(rs.getString("note")));

		// (2) その伝票にひもづく明細を line_no（行番号）順に読み込む
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT debit_subject,debit_amount,credit_subject,credit_amount "
				+ "FROM entry WHERE slip_id=? ORDER BY line_no")) {
			ps.setInt(1, s.getId());
			try (ResultSet ers = ps.executeQuery()) {
				while (ers.next()) {
					// 金額は0のとき空文字にして「未入力」と区別する
					int dv = ers.getInt("debit_amount");
					int cv = ers.getInt("credit_amount");
					String da = (dv == 0) ? "" : String.valueOf(dv);
					String ca = (cv == 0) ? "" : String.valueOf(cv);
					s.getEntries().add(new Entry(
							nz(ers.getString("debit_subject")), da,
							nz(ers.getString("credit_subject")), ca));
				}
			}
		}
		return s;
	}

	// ===== 【bindHeader】伝票の見出し4項目（日付・取引先・購入物・備考）を「?」に流し込むメソッド =====
	private void bindHeader(PreparedStatement ps, Slip slip) throws SQLException {
		ps.setString(1, nz(slip.getDate()));
		ps.setString(2, nz(slip.getPartnerName()));
		ps.setString(3, nz(slip.getDescription()));
		ps.setString(4, nz(slip.getNote()));
	}

	// ===== 【exec】「id=?」だけの単純なUPDATE/DELETEを実行するメソッド =====
	private void exec(Connection c, String sql, int id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
	}

	// ===== 【addNoCondition】伝票番号の検索（単一/範囲）を WHERE 条件に変えるメソッド =====
	//   例:「5」→ id=5 ／「1~10」→ id>=1 かつ id<=10 ／「5~」→ id>=5 だけ、等。
	private void addNoCondition(ArrayList<String> conds, ArrayList<Object> params, String no) {
		if (no == null || no.trim().isEmpty()) return; // 未入力なら何もしない
		// 全角の波ダッシュやハイフンを、半角の「~」に統一する
		String t = no.trim().replace('～', '~').replace('〜', '~')
				.replace('－', '~').replace('−', '~').replace('-', '~');
		if (t.contains("~")) { // --- 範囲指定 ---
			String[] p = t.split("~", -1);
			Integer lo = numOrNull(p[0]);              // 下限
			Integer hi = numOrNull(p[p.length - 1]);   // 上限
			if (lo == null && hi == null) return;      // 両方数字でなければ無視
			if (lo != null && hi != null && lo > hi) { int tmp = lo; lo = hi; hi = tmp; } // 逆なら入れ替え
			if (lo != null) { conds.add("id >= ?"); params.add(lo); }
			if (hi != null) { conds.add("id <= ?"); params.add(hi); }
		} else {               // --- 単一指定 ---
			Integer v = numOrNull(t);
			if (v != null) { conds.add("id = ?"); params.add(v); }
			else conds.add("1 = 0"); // 数字でなければ「該当なし」にする
		}
	}

	// ---- ちょっとした変換の小道具（1行の便利メソッド）----

	// ===== 【nz】null なら空文字 "" に変えるメソッド（null対策）=====
	private static String nz(String s) { return s == null ? "" : s; }

	// ===== 【digits】文字列から数字だけを取り出すメソッド（"1,000"→"1000"）=====
	private static String digits(String s) { return s == null ? "" : s.replaceAll("[^0-9]", ""); }

	// ===== 【at】配列の i 番目を安全に取り出すメソッド（無ければ ""）=====
	private static String at(String[] arr, int i) {
		return (arr != null && arr.length > i && arr[i] != null) ? arr[i] : "";
	}

	// ===== 【maxLen】渡された複数の配列のうち、一番長い長さを返すメソッド（＝行数）=====
	private static int maxLen(String[]... arrays) {
		int max = 0;
		for (String[] a : arrays) if (a != null && a.length > max) max = a.length;
		return max;
	}

	// ===== 【numOrNull】文字列を数値(Integer)にするメソッド（数字が無ければ null）=====
	private static Integer numOrNull(String s) {
		if (s == null) return null;
		String d = s.replaceAll("[^0-9]", "");
		if (d.isEmpty()) return null;
		try { return Integer.valueOf(d); } catch (NumberFormatException e) { return null; }
	}
}
