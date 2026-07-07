package otameshirenshuu;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * データベース・テーブルの自動作成と、初回のサンプルデータ投入を行う。
 * アプリ起動時（SlipStore の初期化時）に1度だけ呼ばれる。
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

	private static boolean done = false;

	/** DB・テーブルを用意し、空ならサンプルを投入する（多重呼び出しは無視）。 */
	public static synchronized void ensure() throws SQLException {
		if (done) {
			return;
		}
		// 1. データベースが無ければ作成
		try (Connection c = Db.getServerConnection(); Statement st = c.createStatement()) {
			st.executeUpdate("CREATE DATABASE IF NOT EXISTS otameshirenshuu DEFAULT CHARACTER SET utf8mb4");
		}
		// 2. テーブル作成
		try (Connection c = Db.getConnection(); Statement st = c.createStatement()) {
			st.executeUpdate(CREATE_SLIP);
			st.executeUpdate(CREATE_ENTRY);
		}
		// 3. 空ならサンプル投入
		SlipDao dao = new SlipDao();
		if (dao.count() == 0) {
			seed(dao);
		}
		done = true;
	}

	/** テスト用：テーブルのみ作成する（データベースは接続先が用意済みとみなす）。 */
	public static void createTables(Connection c) throws SQLException {
		try (Statement st = c.createStatement()) {
			st.executeUpdate(CREATE_SLIP);
			st.executeUpdate(CREATE_ENTRY);
		}
	}

	/** 一覧のサンプルデータ（伝票番号1〜20相当）を投入する。 */
	public static void seed(SlipDao dao) throws SQLException {
		dao.insert(make("2026-04-06", "松本システム開発", "システム開発費", "外注費", "普通預金", "88000"));
		dao.insert(make("2026-04-12", "山口オートサービス", "車両整備", "車両費", "現金", "54000"));
		dao.insert(make("2026-04-19", "佐々木エナジー(株)", "電気代", "水道光熱費", "普通預金", "19500"));
		dao.insert(make("2026-04-21", "山田コンサルティング", "コンサルティング料", "支払手数料", "普通預金", "220000"));
		dao.insert(make("2026-04-23", "吉田ベンディングサービス", "飲料補充", "福利厚生費", "現金", "8200"));
		dao.insert(make("2026-04-24", "加藤法律事務所", "顧問料", "支払手数料", "普通預金", "33000"));
		dao.insert(make("2026-04-30", "小林通信(株)", "通信費", "通信費", "普通預金", "12400"));
		dao.insert(make("2026-05-08", "中村不動産", "事務所家賃", "地代家賃", "普通預金", "150000"));
		dao.insert(make("2026-05-15", "山本印刷(株)", "印刷費", "事務用品費", "未払金", "43200"));
		dao.insert(make("2026-05-16", "伊藤デンタルクリニック", "健康診断", "福利厚生費", "現金", "5500"));
		dao.insert(make("2026-05-19", "渡辺事務用品", "文房具", "消耗品費", "現金", "9800"));
		dao.insert(make("2026-05-20", "高橋テック(株)", "PC周辺機器", "消耗品費", "未払金", "62000"));
		dao.insert(make("2026-05-24", "合同会社マツモト", "消耗品", "消耗品費", "現金", "4500"));
		dao.insert(make("2026-05-25", "田中ロジスティクス", "配送料", "荷造運賃", "未払金", "120000"));
		dao.insert(make("2026-05-28", "(有)サトウ商会", "備品", "消耗品費", "現金", "15800"));
		dao.insert(make("2026-06-02", "鈴木商事", "仕入", "仕入高", "買掛金", "80000"));
		dao.insert(make("2026-06-05", "✕✕産業(株)", "材料", "仕入高", "現金", "7100"));
		dao.insert(make("2026-06-10", "△△商店", "事務用品", "消耗品費", "現金", "3000"));
		dao.insert(make("2026-06-12", "▢▢(株)", "モニター", "消耗品費", "現金", "50000"));
		dao.insert(make("2026-06-13", "(株)〇〇", "懇親会費", "接待交際費", "未払金", "100000"));
	}

	private static Slip make(String date, String partner, String description,
			String debitSubject, String creditSubject, String amount) {
		Slip s = new Slip();
		s.setDate(date);
		s.setPartnerName(partner);
		s.setDescription(description);
		s.getEntries().add(new Entry(debitSubject, amount, creditSubject, amount));
		return s;
	}
}
