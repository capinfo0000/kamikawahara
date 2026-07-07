package otameshirenshuu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 伝票データを保持するインメモリ・ストア（アプリ全体で1つ）。
 * DBを使わず、サーバー起動中だけデータを保持する（再起動でシードデータに戻る）。
 * 伝票番号(id)は登録のたびに自動採番する。
 */
public class SlipStore {

	private static final SlipStore INSTANCE = new SlipStore();

	public static SlipStore getInstance() {
		return INSTANCE;
	}

	private final List<Slip> slips = new ArrayList<>();
	private int nextId = 1;

	private SlipStore() {
		seed();
	}

	/** 全件のコピーを返す。 */
	public synchronized List<Slip> findAll() {
		return new ArrayList<>(slips);
	}

	/** ソート済みのコピーを返す。key: "id" or "date" / order: "asc" or "desc"。 */
	public synchronized List<Slip> findAllSorted(String key, String order) {
		List<Slip> list = new ArrayList<>(slips);
		Comparator<Slip> cmp;
		if ("date".equals(key)) {
			// 日付が同じ場合は伝票番号で安定させる
			cmp = Comparator.comparing(Slip::getDate).thenComparingInt(Slip::getId);
		} else {
			cmp = Comparator.comparingInt(Slip::getId);
		}
		if ("desc".equals(order)) {
			cmp = cmp.reversed();
		}
		list.sort(cmp);
		return list;
	}

	public synchronized Slip findById(int id) {
		for (Slip s : slips) {
			if (s.getId() == id) {
				return s;
			}
		}
		return null;
	}

	/** 新規登録。伝票番号を自動採番して返す。 */
	public synchronized int add(Slip slip) {
		slip.setId(nextId++);
		slips.add(slip);
		return slip.getId();
	}

	/** 既存伝票の更新（伝票番号はそのまま）。 */
	public synchronized void update(Slip slip) {
		for (int i = 0; i < slips.size(); i++) {
			if (slips.get(i).getId() == slip.getId()) {
				slips.set(i, slip);
				return;
			}
		}
	}

	public synchronized void delete(int id) {
		slips.removeIf(s -> s.getId() == id);
	}

	/** 一覧のサンプルデータ（伝票番号1〜20）を投入する。 */
	private void seed() {
		add(makeSlip("2026-04-06", "松本システム開発", "システム開発費", "外注費", "普通預金", "88000"));
		add(makeSlip("2026-04-12", "山口オートサービス", "車両整備", "車両費", "現金", "54000"));
		add(makeSlip("2026-04-19", "佐々木エナジー(株)", "電気代", "水道光熱費", "普通預金", "19500"));
		add(makeSlip("2026-04-21", "山田コンサルティング", "コンサルティング料", "支払手数料", "普通預金", "220000"));
		add(makeSlip("2026-04-23", "吉田ベンディングサービス", "飲料補充", "福利厚生費", "現金", "8200"));
		add(makeSlip("2026-04-24", "加藤法律事務所", "顧問料", "支払手数料", "普通預金", "33000"));
		add(makeSlip("2026-04-30", "小林通信(株)", "通信費", "通信費", "普通預金", "12400"));
		add(makeSlip("2026-05-08", "中村不動産", "事務所家賃", "地代家賃", "普通預金", "150000"));
		add(makeSlip("2026-05-15", "山本印刷(株)", "印刷費", "事務用品費", "未払金", "43200"));
		add(makeSlip("2026-05-16", "伊藤デンタルクリニック", "健康診断", "福利厚生費", "現金", "5500"));
		add(makeSlip("2026-05-19", "渡辺事務用品", "文房具", "消耗品費", "現金", "9800"));
		add(makeSlip("2026-05-20", "高橋テック(株)", "PC周辺機器", "消耗品費", "未払金", "62000"));
		add(makeSlip("2026-05-24", "合同会社マツモト", "消耗品", "消耗品費", "現金", "4500"));
		add(makeSlip("2026-05-25", "田中ロジスティクス", "配送料", "荷造運賃", "未払金", "120000"));
		add(makeSlip("2026-05-28", "(有)サトウ商会", "備品", "消耗品費", "現金", "15800"));
		add(makeSlip("2026-06-02", "鈴木商事", "仕入", "仕入高", "買掛金", "80000"));
		add(makeSlip("2026-06-05", "✕✕産業(株)", "材料", "仕入高", "現金", "7100"));
		add(makeSlip("2026-06-10", "△△商店", "事務用品", "消耗品費", "現金", "3000"));
		add(makeSlip("2026-06-12", "▢▢(株)", "モニター", "消耗品費", "現金", "50000"));
		add(makeSlip("2026-06-13", "(株)〇〇", "懇親会費", "接待交際費", "未払金", "100000"));
	}

	private Slip makeSlip(String date, String partner, String description,
			String debitSubject, String creditSubject, String amount) {
		Slip s = new Slip();
		s.setDate(date);
		s.setPartnerName(partner);
		s.setDescription(description);
		s.getEntries().add(new Entry(debitSubject, amount, creditSubject, amount));
		return s;
	}
}
