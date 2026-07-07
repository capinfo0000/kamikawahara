package otameshirenshuu;

import java.util.ArrayList;
import java.util.List;

/**
 * 1枚の振替伝票。伝票番号・日付・取引先・購入物・備考と、複数の明細行を持つ。
 */
public class Slip {

	private int id;
	private String date = "";          // yyyy-MM-dd
	private String partnerName = "";
	private String description = "";
	private String note = "";
	private List<Entry> entries = new ArrayList<>();

	public Slip() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getPartnerName() {
		return partnerName;
	}

	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public void setEntries(List<Entry> entries) {
		this.entries = entries;
	}

	/** 借方金額の合計（貸借一致していれば貸方合計と等しい）。一覧の金額表示に使う。 */
	public int getTotal() {
		int t = 0;
		for (Entry e : entries) {
			t += e.getDebitValue();
		}
		return t;
	}
}
