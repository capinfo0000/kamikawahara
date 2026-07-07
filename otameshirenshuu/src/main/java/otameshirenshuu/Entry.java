package otameshirenshuu;

/**
 * 振替伝票の1明細行（借方科目・借方金額・貸方科目・貸方金額）を表す。
 * 金額は「数字のみ（カンマなし）の文字列」で保持し、未入力は空文字とする。
 */
public class Entry {

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
		if (s == null) {
			return 0;
		}
		String digits = s.replaceAll("[^0-9]", "");
		return digits.isEmpty() ? 0 : Integer.parseInt(digits);
	}
}
