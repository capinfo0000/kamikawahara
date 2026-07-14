package otameshirenshuu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 伝票データのデータベースアクセス（JDBC）。slip / entry テーブルを操作する。
 */
public class SlipDao {

	/** ソート済み全件を返す。key: id / date, order: asc / desc。 */
	public List<Slip> findAllSorted(String key, String order) throws SQLException {
		String col = "date".equals(key) ? "slip_date" : "id";
		String dir = "desc".equals(order) ? "DESC" : "ASC";
		// key/order は呼び出し側で検証済みだが、ここでも固定値のみに制限してSQLインジェクションを防ぐ
		String sql = "SELECT id, slip_date, partner_name, description, note FROM slip "
				+ "ORDER BY " + col + " " + dir + ", id " + dir;

		List<Slip> list = new ArrayList<>();
		try (Connection c = Db.getConnection();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				Slip s = mapSlipHeader(rs);
				loadEntries(c, s);
				list.add(s);
			}
		}
		return list;
	}

	/** 後方互換：伝票番号指定なしでキーワード検索する。 */
	public List<Slip> findFiltered(String q, String key, String order) throws SQLException {
		return findFiltered(q, "", key, order);
	}

	/**
	 * SQLのWHEREで絞り込む。
	 * q  … 日付・取引先・購入物のキーワード（LIKEで部分一致。伝票番号は対象外）
	 * no … 伝票番号。単一「5」または範囲「1~10」（〜／～／- も可、片側省略も可）
	 */
	public List<Slip> findFiltered(String q, String no, String key, String order) throws SQLException {
		String col = "date".equals(key) ? "slip_date" : "id";
		String dir = "desc".equals(order) ? "DESC" : "ASC";

		// WHEREに入れる条件と、それに対応する「?」の値を並行して組み立てる
		List<String> conditions = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		// 伝票番号（単一 / 範囲）の条件
		addNoCondition(conditions, params, no);

		// キーワード（日付・取引先・購入物）を LIKE で部分一致
		if (q != null && !q.trim().isEmpty()) {
			String like = "%" + q.trim() + "%";
			conditions.add("(slip_date LIKE ? OR REPLACE(slip_date, '-', '/') LIKE ? "
					+ "OR partner_name LIKE ? OR description LIKE ?)");
			params.add(like);
			params.add(like);
			params.add(like);
			params.add(like);
		}

		StringBuilder sql = new StringBuilder(
				"SELECT id, slip_date, partner_name, description, note FROM slip");
		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conditions));
		}
		// 並び順（列・方向は固定値のみなので安全）
		sql.append(" ORDER BY ").append(col).append(" ").append(dir).append(", id ").append(dir);

		List<Slip> list = new ArrayList<>();
		try (Connection c = Db.getConnection();
				PreparedStatement ps = c.prepareStatement(sql.toString())) {
			for (int i = 0; i < params.size(); i++) {
				ps.setObject(i + 1, params.get(i)); // 1始まりで ? に値を埋める
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Slip s = mapSlipHeader(rs);
					loadEntries(c, s);
					list.add(s);
				}
			}
		}
		return list;
	}

	/** 伝票番号（単一「5」/範囲「1~10」/片側省略）を WHERE 条件に変換して追加する。 */
	private void addNoCondition(List<String> conditions, List<Object> params, String no) {
		if (no == null || no.trim().isEmpty()) {
			return; // 伝票番号での絞り込みなし
		}
		// 各種の波ダッシュ・ハイフンを区切り文字「~」に統一
		String t = no.trim()
				.replace('～', '~').replace('〜', '~')
				.replace('－', '~').replace('−', '~').replace('-', '~');
		if (t.contains("~")) {
			String[] p = t.split("~", -1);
			Integer lo = numOrNull(p[0]);
			Integer hi = numOrNull(p[p.length - 1]);
			if (lo == null && hi == null) {
				return; // 「~」だけ等 → 実質フィルタなし
			}
			if (lo != null && hi != null && lo > hi) {
				int tmp = lo; lo = hi; hi = tmp;
			}
			if (lo != null) {
				conditions.add("id >= ?");
				params.add(lo);
			}
			if (hi != null) {
				conditions.add("id <= ?");
				params.add(hi);
			}
		} else {
			Integer v = numOrNull(t);
			if (v != null) {
				conditions.add("id = ?");
				params.add(v);
			} else {
				conditions.add("1 = 0"); // 数字でない指定は該当なし
			}
		}
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

	public Slip findById(int id) throws SQLException {
		String sql = "SELECT id, slip_date, partner_name, description, note FROM slip WHERE id = ?";
		try (Connection c = Db.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				Slip s = mapSlipHeader(rs);
				loadEntries(c, s);
				return s;
			}
		}
	}

	/** 新規登録。伝票番号(id)を自動採番して返す。slip と entry をまとめて登録する。 */
	public int insert(Slip slip) throws SQLException {
		try (Connection c = Db.getConnection()) {
			c.setAutoCommit(false);
			try {
				int id;
				String sql = "INSERT INTO slip(slip_date, partner_name, description, note) VALUES(?,?,?,?)";
				try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
					bindHeader(ps, slip);
					ps.executeUpdate();
					try (ResultSet gk = ps.getGeneratedKeys()) {
						gk.next();
						id = gk.getInt(1);
					}
				}
				insertEntries(c, id, slip.getEntries());
				c.commit();
				return id;
			} catch (SQLException e) {
				c.rollback();
				throw e;
			}
		}
	}

	/** 既存伝票の更新。明細は入れ替える（一旦削除して再登録）。 */
	public void update(Slip slip) throws SQLException {
		try (Connection c = Db.getConnection()) {
			c.setAutoCommit(false);
			try {
				String sql = "UPDATE slip SET slip_date=?, partner_name=?, description=?, note=? WHERE id=?";
				try (PreparedStatement ps = c.prepareStatement(sql)) {
					bindHeader(ps, slip);
					ps.setInt(5, slip.getId());
					ps.executeUpdate();
				}
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM entry WHERE slip_id=?")) {
					ps.setInt(1, slip.getId());
					ps.executeUpdate();
				}
				insertEntries(c, slip.getId(), slip.getEntries());
				c.commit();
			} catch (SQLException e) {
				c.rollback();
				throw e;
			}
		}
	}

	public void delete(int id) throws SQLException {
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
	}

	public int count() throws SQLException {
		try (Connection c = Db.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM slip");
				ResultSet rs = ps.executeQuery()) {
			rs.next();
			return rs.getInt(1);
		}
	}

	// ---- helpers ----

	private void bindHeader(PreparedStatement ps, Slip slip) throws SQLException {
		ps.setString(1, nz(slip.getDate()));
		ps.setString(2, nz(slip.getPartnerName()));
		ps.setString(3, nz(slip.getDescription()));
		ps.setString(4, nz(slip.getNote()));
	}

	private void insertEntries(Connection c, int slipId, List<Entry> entries) throws SQLException {
		String sql = "INSERT INTO entry(slip_id, line_no, debit_subject, debit_amount, credit_subject, credit_amount) "
				+ "VALUES(?,?,?,?,?,?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
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

	private Slip mapSlipHeader(ResultSet rs) throws SQLException {
		Slip s = new Slip();
		s.setId(rs.getInt("id"));
		s.setDate(nz(rs.getString("slip_date")));
		s.setPartnerName(nz(rs.getString("partner_name")));
		s.setDescription(nz(rs.getString("description")));
		s.setNote(nz(rs.getString("note")));
		return s;
	}

	private void loadEntries(Connection c, Slip s) throws SQLException {
		String sql = "SELECT debit_subject, debit_amount, credit_subject, credit_amount "
				+ "FROM entry WHERE slip_id=? ORDER BY line_no";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, s.getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Entry e = new Entry(
							nz(rs.getString("debit_subject")), amtToStr(rs.getInt("debit_amount")),
							nz(rs.getString("credit_subject")), amtToStr(rs.getInt("credit_amount")));
					s.getEntries().add(e);
				}
			}
		}
	}

	/** 金額intを表示・保持用の文字列へ。0は「未入力」として空文字にする。 */
	private static String amtToStr(int v) {
		return v == 0 ? "" : String.valueOf(v);
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
