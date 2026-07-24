package otameshirenshuu;

import java.util.ArrayList;

/*
 * 「データの入れ物」クラス。
 *   Slip        … 伝票1枚ぶん（伝票番号・日付・取引先・購入物・備考＋明細の一覧）
 *   Slip.Entry  … 明細1行ぶん（借方科目/金額・貸方科目/金額）
 * どちらも値を持つだけで、計算やDB処理はしません。
 * Entry は Slip の中の「入れ子クラス」にして、JSPからも otameshirenshuu.Slip.Entry で使えるようにしています。
 */
public class Slip {

	// --- 伝票が持つ項目（外から直接触らせないよう private にしている） ---
	private int id;                  // 伝票番号（DBが自動で採番する）
	private String date = "";        // 日付（"2026-04-23" の形式）
	private String partnerName = ""; // 取引先
	private String description = ""; // 購入物
	private String note = "";        // 備考
	private ArrayList<Entry> entries = new ArrayList<>(); // 明細行の一覧

	// --- getter / setter（値の読み書き口。get=取り出す、set=入れる。中身は代入だけ）---
	public int getId() { return id; }
	public void setId(int id) { this.id = id; }

	public String getDate() { return date; }
	public void setDate(String date) { this.date = date; }

	public String getPartnerName() { return partnerName; }
	public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }

	public ArrayList<Entry> getEntries() { return entries; }
	public void setEntries(ArrayList<Entry> entries) { this.entries = entries; }

	// ===== 【getTotal】この伝票の借方金額の合計を返すメソッド（一覧の「金額」列に使う）=====
	//   明細を1行ずつ見て、借方金額を足し合わせるだけ。
	public int getTotal() {
		int sum = 0;
		for (Entry e : entries) {
			sum += e.getDebitValue();
		}
		return sum;
	}

	/**
	 * 明細1行を表すクラス（Slip の中の入れ子クラス）。
	 * 金額は「数字だけの文字列」で持つ（未入力は空文字。0と空欄を区別するため）。
	 */
	public static class Entry {

		private String debitSubject = "";  // 借方勘定科目
		private String debitAmount = "";   // 借方金額（"50000" のような数字だけの文字列／空欄可）
		private String creditSubject = ""; // 貸方勘定科目
		private String creditAmount = "";  // 貸方金額

		// 引数なしのコンストラクタ（空の1行を作るとき用。新規登録の空欄5行など）
		public Entry() {
		}

		// 4項目をまとめて受け取って作るコンストラクタ（DBから読んだ行を復元するとき等）
		public Entry(String debitSubject, String debitAmount, String creditSubject, String creditAmount) {
			this.debitSubject = debitSubject;
			this.debitAmount = debitAmount;
			this.creditSubject = creditSubject;
			this.creditAmount = creditAmount;
		}

		// --- getter / setter（値の読み書き口）---
		public String getDebitSubject() { return debitSubject; }
		public void setDebitSubject(String v) { this.debitSubject = v; }

		public String getDebitAmount() { return debitAmount; }
		public void setDebitAmount(String v) { this.debitAmount = v; }

		public String getCreditSubject() { return creditSubject; }
		public void setCreditSubject(String v) { this.creditSubject = v; }

		public String getCreditAmount() { return creditAmount; }
		public void setCreditAmount(String v) { this.creditAmount = v; }

		// ===== 【getDebitValue】借方金額を数値(int)にして返すメソッド（空欄は0。合計計算に使う）=====
		public int getDebitValue() { return toInt(debitAmount); }

		// ===== 【getCreditValue】貸方金額を数値(int)にして返すメソッド（空欄は0）=====
		public int getCreditValue() { return toInt(creditAmount); }

		// ===== 【toInt】文字列を数値に変換するメソッド（数字以外は無視。空なら0）=====
		private static int toInt(String s) {
			if (s == null) return 0;
			String digits = s.replaceAll("[^0-9]", "");
			return digits.isEmpty() ? 0 : Integer.parseInt(digits);
		}
	}
}
