package otameshirenshuu;

import java.util.ArrayList;

/**
 * 1枚の振替伝票。伝票番号・日付・取引先・購入物・備考と、複数の明細行を持つ。
 */
	public class Slip {

		// --- 伝票が持つ項目（外から触れないようprivate）
		private int id;						// 伝票番号
		private String date = "";           // 日付 yyyy-MM-ddの形で表示
		private String partnerName = "";	// 取引先
		private String description = "";	// 購入物
		private String note = "";			// 備考
		private ArrayList<Entry> entries = new ArrayList<>();
	
		public Slip() {
		}
	
		public int getId() { return id; }
		public void setId(int id) {	this.id = id; }
	
		public String getDate() { return date; }
		public void setDate(String date) { this.date = date; }
	
		public String getPartnerName() { return partnerName; }
		public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
	
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
	
		public String getNote() { return note;}
		public void setNote(String note) { this.note = note; }
	
		public ArrayList<Entry> getEntries() { return entries; }
		public void setEntries(ArrayList<Entry> entries) { this.entries = entries; }
	
		/** 借方金額の合計（貸借一致していれば貸方合計と等しい）。一覧の金額表示に使う。 */
		public int getTotal() {
			int t = 0;
			for (Entry e : entries) {
				t += e.getDebitValue();
			}
			return t;
		}
	

/**
 * 振替伝票の1明細行（借方科目・借方金額・貸方科目・貸方金額）を表す。
 * 金額は「数字のみ（カンマなし）の文字列」で保持し、未入力は空文字とする。
 */
	public static class Entry {

		private String debitSubject = "";
		private String debitAmount = "";
		private String creditSubject = "";
		private String creditAmount = "";
	
		public Entry() {
		}
	
		public Entry(String debitSubject, String debitAmount, String creditSubject, String creditAmount) {
			this.debitSubject = debitSubject;
			this.debitAmount = debitAmount;
			this.creditSubject = creditSubject;
			this.creditAmount = creditAmount;
		}
	
		public String getDebitSubject() {
			return debitSubject;
		}
	
		public void setDebitSubject(String debitSubject) {
			this.debitSubject = debitSubject;
		}
	
		public String getDebitAmount() {
			return debitAmount;
		}
	
		public void setDebitAmount(String debitAmount) {
			this.debitAmount = debitAmount;
		}
	
		public String getCreditSubject() {
			return creditSubject;
		}
	
		public void setCreditSubject(String creditSubject) {
			this.creditSubject = creditSubject;
		}
	
		public String getCreditAmount() {
			return creditAmount;
		}
	
		public void setCreditAmount(String creditAmount) {
			this.creditAmount = creditAmount;
		}
	
		/** 4項目すべてが空なら空行とみなす（登録時に自動で詰めるための判定）。 */
		public boolean isBlank() {
			return isEmpty(debitSubject) && isEmpty(debitAmount)
					&& isEmpty(creditSubject) && isEmpty(creditAmount);
		}
	
		public int getDebitValue() {
			return toInt(debitAmount);
		}
	
		public int getCreditValue() {
			return toInt(creditAmount);
		}
	
		private static boolean isEmpty(String s) {
			return s == null || s.isEmpty();
		}
	
		private static int toInt(String s) {
			if (s == null) return 0;
			
			String digits = s.replaceAll("[^0-9]", "");
			return digits.isEmpty() ? 0 : Integer.parseInt(digits);
		}
	}
}