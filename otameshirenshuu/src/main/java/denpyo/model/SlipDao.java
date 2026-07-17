package denpyo.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 【Model】伝票データの読み書き（連想配列 Map で受け渡し）。
 *
 * ・データは Slip/Entry クラスを使わず Map<String,Object>（HashMap系）で扱う。
 * ・検索・並び替え・集計・ページングは SQL 側（DB）で完結させる。
 * ・DB/テーブルが無い場合は自動で作り直して1回だけ再試行する（自動回復）。
 * ・SQLException は実行時例外に変換して返す（呼ぶ側＝Controllerを簡潔に保つ）。
 */
public class SlipDao {

	public SlipDao() {
		try {
			SchemaInit.ensure();
		} catch (SQLException e) {
			System.out.println("[denpyo.SlipDao] 起動時のDB初期化に失敗（操作時に再試行）: " + e.getMessage());
		}
	}

	// ===== 一覧（検索＋ソート＋ページング。すべてSQLで実施）=====

	/** 検索条件に一致する件数（ページ数計算用）。 */
	public int count(String q, String no) {
		return exec(() -> {
			List<Object> params = new ArrayList<>();
			String where = buildWhere(q, no, params);
			String sql = "SELECT COUNT(*) FROM slip" + where;
			try (Connection c = Db.getConnection();
					PreparedStatement ps = c.prepareStatement(sql)) {
				bind(ps, params);
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					return rs.getInt(1);
				}
			}
		});
	}

	/** 1ページ分の伝票（金額合計つき）を返す。 */
	public List<Map<String, Object>> search(String q, String no, String sortKey, String order,
			int limit, int offset) {
		return exec(() -> {
			String col = "date".equals(sortKey) ? "slip_date" : "id";
			String dir = "desc".equals(order) ? "DESC" : "ASC";
			List<Object> params = new ArrayList<>();
			String where = buildWhere(q, no, params);

			// 金額合計は明細のSUMをDB側で計算
			String sql = "SELECT s.id, s.slip_date, s.partner_name, s.description, "
					+ "COALESCE((SELECT SUM(e.debit_amount) FROM entry e WHERE e.slip_id = s.id), 0) AS total "
					+ "FROM slip s" + where
					+ " ORDER BY " + col + " " + dir + ", id " + dir
					+ " LIMIT ? OFFSET ?";

			List<Map<String, Object>> list = new ArrayList<>();
			try (Connection c = Db.getConnection();
					PreparedStatement ps = c.prepareStatement(sql)) {
				int i = bind(ps, params);
				ps.setInt(i++, limit);
				ps.setInt(i, offset);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Map<String, Object> row = new LinkedHashMap<>();
						String date = nz(rs.getString("slip_date"));
						row.put("id", rs.getInt("id"));
						row.put("date", date);
						row.put("dateSlash", date.replace("-", "/"));
						row.put("partnerName", nz(rs.getString("partner_name")));
						row.put("description", nz(rs.getString("description")));
						row.put("total", rs.getInt("total"));
						list.add(row);
					}
				}
			}
			return list;
		});
	}

	// ===== 1件取得（明細つき）=====

	public Map<String, Object> findById(int id) {
		return exec(() -> {
			String sql = "SELECT id, slip_date, partner_name, description, note FROM slip WHERE id = ?";
			try (Connection c = Db.getConnection();
					PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return null;
					}
					Map<String, Object> slip = new LinkedHashMap<>();
					String date = nz(rs.getString("slip_date"));
					slip.put("id", rs.getInt("id"));
					slip.put("date", date);
					slip.put("dateSlash", date.replace("-", "/"));
					slip.put("partnerName", nz(rs.getString("partner_name")));
					slip.put("description", nz(rs.getString("description")));
					slip.put("note", nz(rs.getString("note")));
					List<Map<String, Object>> entries = loadEntries(c, id);
					slip.put("entries", entries);
					int dt = 0, ct = 0;
					for (Map<String, Object> e : entries) {
						dt += (Integer) e.get("debitAmount");
						ct += (Integer) e.get("creditAmount");
					}
					slip.put("debitTotal", dt);
					slip.put("creditTotal", ct);
					return slip;
				}
			}
		});
	}

	private List<Map<String, Object>> loadEntries(Connection c, int slipId) throws SQLException {
		String sql = "SELECT debit_subject, debit_amount, credit_subject, credit_amount "
				+ "FROM entry WHERE slip_id = ? ORDER BY line_no";
		List<Map<String, Object>> list = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, slipId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> e = new LinkedHashMap<>();
					e.put("debitSubject", nz(rs.getString("debit_subject")));
					e.put("debitAmount", rs.getInt("debit_amount"));
					e.put("creditSubject", nz(rs.getString("credit_subject")));
					e.put("creditAmount", rs.getInt("credit_amount"));
					list.add(e);
				}
			}
		}
		return list;
	}

	// ===== 保存（新規/更新）・削除 =====

	/** id が無ければ新規登録（採番して返す）、あれば更新。 */
	@SuppressWarnings("unchecked")
	public int save(Map<String, Object> slip) {
		return exec(() -> {
			Integer id = asInt(slip.get("id"));
			List<Map<String, Object>> entries =
					(List<Map<String, Object>>) slip.get("entries");
			try (Connection c = Db.getConnection()) {
				c.setAutoCommit(false);
				try {
					int savedId;
					if (id == null || id == 0) {
						String sql = "INSERT INTO slip(slip_date, partner_name, description, note) VALUES(?,?,?,?)";
						try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
							bindHeader(ps, slip);
							ps.executeUpdate();
							try (ResultSet gk = ps.getGeneratedKeys()) {
								gk.next();
								savedId = gk.getInt(1);
							}
						}
					} else {
						savedId = id;
						String sql = "UPDATE slip SET slip_date=?, partner_name=?, description=?, note=? WHERE id=?";
						try (PreparedStatement ps = c.prepareStatement(sql)) {
							bindHeader(ps, slip);
							ps.setInt(5, savedId);
							ps.executeUpdate();
						}
						try (PreparedStatement ps = c.prepareStatement("DELETE FROM entry WHERE slip_id=?")) {
							ps.setInt(1, savedId);
							ps.executeUpdate();
						}
					}
					insertEntries(c, savedId, entries);
					c.commit();
					return savedId;
				} catch (SQLException e) {
					c.rollback();
					throw e;
				}
			}
		});
	}

	public void delete(int id) {
		exec(() -> {
			try (Connection c = Db.getConnection()) {
				c.setAutoCommit(false);
				try {
					try (PreparedStatement ps = c.prepareStatement("DELETE FROM entry WHERE slip_id=?")) {
						ps.setInt(1, id);
						ps.executeUpdate();
					}
					try (PreparedStatement ps = c.prepareStatement("DELETE FROM slip WHERE id=?")) {
						ps.setInt(1, id);
						ps.executeUpdate();
					}
					c.commit();
				} catch (SQLException e) {
					c.rollback();
					throw e;
				}
			}
			return null;
		});
	}

	// ===== 内部：WHERE組み立て・バインド・変換 =====

	/** 検索条件(q=キーワード / no=伝票番号)から WHERE 句を作る。paramsに値を積む。 */
	private String buildWhere(String q, String no, List<Object> params) {
		List<String> conds = new ArrayList<>();
		addNoCondition(conds, params, no);
		if (q != null && !q.trim().isEmpty()) {
			String like = "%" + q.trim() + "%";
			conds.add("(slip_date LIKE ? OR REPLACE(slip_date,'-','/') LIKE ? "
					+ "OR partner_name LIKE ? OR description LIKE ?)");
			params.add(like);
			params.add(like);
			params.add(like);
			params.add(like);
		}
		return conds.isEmpty() ? "" : " WHERE " + String.join(" AND ", conds);
	}

	private void addNoCondition(List<String> conds, List<Object> params, String no) {
		if (no == null || no.trim().isEmpty()) {
			return;
		}
		String t = no.trim().replace('～', '~').replace('〜', '~')
				.replace('－', '~').replace('−', '~').replace('-', '~');
		if (t.contains("~")) {
			String[] p = t.split("~", -1);
			Integer lo = numOrNull(p[0]);
			Integer hi = numOrNull(p[p.length - 1]);
			if (lo == null && hi == null) {
				return;
			}
			if (lo != null && hi != null && lo > hi) {
				int tmp = lo; lo = hi; hi = tmp;
			}
			if (lo != null) {
				conds.add("id >= ?");
				params.add(lo);
			}
			if (hi != null) {
				conds.add("id <= ?");
				params.add(hi);
			}
		} else {
			Integer v = numOrNull(t);
			if (v != null) {
				conds.add("id = ?");
				params.add(v);
			} else {
				conds.add("1 = 0");
			}
		}
	}

	private void bindHeader(PreparedStatement ps, Map<String, Object> slip) throws SQLException {
		ps.setString(1, str(slip.get("date")));
		ps.setString(2, str(slip.get("partnerName")));
		ps.setString(3, str(slip.get("description")));
		ps.setString(4, str(slip.get("note")));
	}

	private void insertEntries(Connection c, int slipId, List<Map<String, Object>> entries) throws SQLException {
		if (entries == null) {
			return;
		}
		String sql = "INSERT INTO entry(slip_id, line_no, debit_subject, debit_amount, credit_subject, credit_amount) "
				+ "VALUES(?,?,?,?,?,?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int line = 1;
			for (Map<String, Object> e : entries) {
				ps.setInt(1, slipId);
				ps.setInt(2, line++);
				ps.setString(3, str(e.get("debitSubject")));
				ps.setInt(4, intval(e.get("debitAmount")));
				ps.setString(5, str(e.get("creditSubject")));
				ps.setInt(6, intval(e.get("creditAmount")));
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	/** paramsを ? に順に埋める。次に使う ? の位置(1始まり)を返す。 */
	private int bind(PreparedStatement ps, List<Object> params) throws SQLException {
		int i = 1;
		for (Object p : params) {
			ps.setObject(i++, p);
		}
		return i;
	}

	// ---- SQL処理を実行し、失敗したらスキーマを作り直して1回だけ再試行 ----
	private <T> T exec(SqlCall<T> call) {
		try {
			return call.run();
		} catch (SQLException first) {
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
	private interface SqlCall<T> {
		T run() throws SQLException;
	}

	private static Integer numOrNull(String s) {
		if (s == null) {
			return null;
		}
		String d = s.replaceAll("[^0-9]", "");
		if (d.isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(d);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer asInt(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Integer) {
			return (Integer) o;
		}
		return numOrNull(o.toString());
	}

	private static int intval(Object o) {
		Integer v = asInt(o);
		return v == null ? 0 : v;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
