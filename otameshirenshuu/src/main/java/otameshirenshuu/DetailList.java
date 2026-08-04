package otameshirenshuu;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import otameshirenshuu.Slip.Entry;

/*
 * ============================================================================
 * 【DetailList】= 明細画面の「頭脳」。detail.jsp はここが作った HashMap を表示するだけ。
 *   処理・入力チェック・確認はすべてここ（Java）で行い、detail.jsp では JavaScript を
 *   （3桁カンマの見た目以外）使いません。
 *
 *   ◆画面のモード（HashMap の isView / isNew / isEdit / isConfirm で表す）◆
 *     new     … 新規入力（空5行）        view    … 閲覧（読み取り専用）
 *     edit    … 修正入力                confirm … 登録前の内容確認
 *
 *   ◆動きの流れ◆
 *     一覧から GET で来る:  id が無ければ new、id があれば view
 *     view の「編集」:      GET  action=edit   → edit
 *     入力中の「行を追加」: POST action=addRow → 入力欄を保ったまま空行を1つ増やす
 *     入力中の「登録」:     POST action=confirm→ 入力チェック
 *                              NG なら入力画面へ戻す（エラー表示）
 *                              OK なら confirm（この内容で登録しますか？の確認画面）
 *     確認画面の「登録する」: POST action=save  → DBへ保存 → その伝票の view へ
 *     確認画面の「修正する」: POST action=back  → 入力画面へ戻す
 *     view の「削除」:      GET  action=delete → 削除して一覧へ
 * ============================================================================
 */
public class DetailList {

	// ===== 【detail】明細画面のすべての要求を受け取り、表示用 HashMap を返すメソッド =====
	//   別URLへ飛ばした（リダイレクトした）ときだけ null を返し、JSPは表示を止める。
	public static HashMap<String, Object> detail(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		request.setCharacterEncoding("UTF-8"); // 日本語の受け取り用
		SlipDao dao = new SlipDao();

		Integer id = parseId(request.getParameter("id")); // 伝票番号（無ければ null＝新規）
		String action = nz(request.getParameter("action")); // 押されたボタンの合図

		// 一覧の検索条件・ページ番号（画面をまたいで保持する。戻り先URLにも使う）
		String q = nz(request.getParameter("q"));
		String no = nz(request.getParameter("no"));
		String sortKey = nz(request.getParameter("sortKey"));
		String order = nz(request.getParameter("order"));
		String page = nz(request.getParameter("page"));
		String listUrl = listUrl(q, no, sortKey, order, page); // 一覧へ戻るURL

		// フォームから送られてくる入力欄（GETのときは null。POSTの各処理で使う）
		String fDate = request.getParameter("slipDate");
		String fPartner = request.getParameter("partnerName");
		String fDesc = request.getParameter("description");
		String fNote = request.getParameter("note");
		String[] dSub = request.getParameterValues("debitSubject");
		String[] dAmt = request.getParameterValues("debitAmount");
		String[] cSub = request.getParameterValues("creditSubject");
		String[] cAmt = request.getParameterValues("creditAmount");

		// --- (A) 削除：view画面の削除確認から来る（GET action=delete）---
		if ("delete".equals(action)) {
			if (id != null) dao.delete(id);
			response.sendRedirect(listUrl); // 一覧へ戻す（検索条件つき）
			return null;
		}

		// --- (B) 保存：確認画面の「登録する」から来る（POST action=save）---
		if ("save".equals(action)) {
			ArrayList<String> errors = new ArrayList<>();
			int savedId = dao.saveFromForm(id, fDate, fPartner, fDesc, fNote,
					dSub, dAmt, cSub, cAmt, errors);
			if (savedId > 0) {
				// 保存成功 → その伝票の閲覧画面へ（検索条件も引き継ぐ）
				response.sendRedirect(detailUrl(savedId, q, no, sortKey, order, page));
				return null;
			}
			// まず起きないが、もし失敗したら入力画面へ戻す
			ArrayList<Entry> raw = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
			return render(request, id == null ? "new" : "edit", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), raw, errors);
		}

		// --- (C) 行を追加：入力途中の内容を保ったまま、末尾に空行を1つ足す（POST action=addRow）---
		if ("addRow".equals(action)) {
			ArrayList<Entry> entries = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
			entries.add(new Entry()); // 空の1行を追加
			return render(request, id == null ? "new" : "edit", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), entries, null);
		}

		// --- (D) 登録ボタン：入力チェックして、OKなら確認画面へ（POST action=confirm）---
		if ("confirm".equals(action)) {
			ArrayList<String> errors = new ArrayList<>();
			// buildEntries が「入力チェック＋空行を詰めた明細」を作ってくれる
			ArrayList<Entry> entries = dao.buildEntries(fDate, fPartner, fDesc,
					dSub, dAmt, cSub, cAmt, errors);
			if (!errors.isEmpty()) {
				// NG → 打ち込んだ内容そのままで入力画面へ戻す（エラー文つき）
				ArrayList<Entry> raw = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
				return render(request, id == null ? "new" : "edit", id,
						nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), raw, errors);
			}
			// OK → 「この内容で登録しますか？」の確認画面（詰めた明細を見せる）
			return render(request, "confirm", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), entries, null);
		}

		// --- (E) 修正する：確認画面から入力画面へ戻る（POST action=back）---
		if ("back".equals(action)) {
			ArrayList<Entry> entries = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
			return render(request, id == null ? "new" : "edit", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), entries, null);
		}

		// --- (F) 閲覧→編集：viewの「編集」から来る（GET action=edit）---
		if ("edit".equals(action) && id != null) {
			Slip s = dao.findById(id);
			if (s == null) { response.sendRedirect(listUrl); return null; }
			return render(request, "edit", s.getId(), s.getDate(), s.getPartnerName(),
					s.getDescription(), s.getNote(), s.getEntries(), null);
		}

		// --- (G) 一覧から GET で来た最初の表示：IDが無ければ新規、あれば閲覧 ---
		if (id == null) {
			// 新規：空の明細を5行用意する
			ArrayList<Entry> entries = new ArrayList<>();
			for (int i = 0; i < 5; i++) entries.add(new Entry());
			return render(request, "new", null, "", "", "", "", entries, null);
		}
		Slip s = dao.findById(id);
		if (s == null) { response.sendRedirect(listUrl); return null; } // 無ければ一覧へ
		return render(request, "view", s.getId(), s.getDate(), s.getPartnerName(),
				s.getDescription(), s.getNote(), s.getEntries(), null);
	}

	// ===== 【render】表示用HashMapを作り、一覧の検索条件も一緒に載せて返すメソッド =====
	//   （buildView に画面の中身を作らせ、そこへ検索条件・戻り先URLを追加する）
	private static HashMap<String, Object> render(HttpServletRequest request,
			String mode, Integer slipId, String dateRaw, String partner, String desc, String note,
			ArrayList<Entry> entries, ArrayList<String> errors) {

		HashMap<String, Object> data = buildView(mode, slipId, dateRaw, partner, desc, note, entries, errors);

		// 一覧の検索条件・ページ（input hidden で持ち回るため HashMap に入れておく）
		String q = nz(request.getParameter("q"));
		String no = nz(request.getParameter("no"));
		String sortKey = nz(request.getParameter("sortKey"));
		String order = nz(request.getParameter("order"));
		String page = nz(request.getParameter("page"));
		data.put("q", q);
		data.put("no", no);
		data.put("sortKey", sortKey);
		data.put("order", order);
		data.put("page", page);
		data.put("listUrl", listUrl(q, no, sortKey, order, page));       // 「←伝票一覧」の戻り先
		data.put("stateQuery", stateQuery(q, no, sortKey, order, page)); // 編集/削除リンクに付ける「&q=…」
		return data;
	}

	// ===== 【stateQuery】検索条件・ページを「&名前=値…」の形にするメソッド（view画面のリンク用）=====
	private static String stateQuery(String q, String no, String sortKey, String order, String page) {
		StringBuilder sb = new StringBuilder();
		add(sb, "&", "q", q);
		add(sb, "&", "no", no);
		add(sb, "&", "sortKey", sortKey);
		add(sb, "&", "order", order);
		add(sb, "&", "page", page);
		return sb.toString();
	}

	// ===== 【buildView】画面の中身（モード・各値・合計）を HashMap にまとめるメソッド =====
	private static HashMap<String, Object> buildView(
			String mode, Integer slipId, String dateRaw, String partner, String desc, String note,
			ArrayList<Entry> entries, ArrayList<String> errors) {

		boolean isView    = "view".equals(mode);
		boolean isNew     = "new".equals(mode);
		boolean isEdit    = "edit".equals(mode);
		boolean isConfirm = "confirm".equals(mode);

		// 合計金額（明細を1行ずつ読み、借方合計と貸方合計を出す）
		int debitTotal = 0, creditTotal = 0;
		for (Entry e : entries) {
			debitTotal += e.getDebitValue();
			creditTotal += e.getCreditValue();
		}

		HashMap<String, Object> data = new HashMap<>();
		data.put("isView", isView);
		data.put("isNew", isNew);
		data.put("isEdit", isEdit);
		data.put("isConfirm", isConfirm);
		data.put("slipId", slipId);     // 新規のときは null
		data.put("dateRaw", dateRaw);
		data.put("partner", partner);
		data.put("desc", desc);
		data.put("note", note);
		data.put("entries", entries);
		data.put("errors", errors);     // 無ければ null
		data.put("debitTotal", debitTotal);
		data.put("creditTotal", creditTotal);
		// CSS切替用（新規/編集のときだけ「行を追加」ボタンを出す等に使う）
		data.put("pageMod", isNew ? " is-new" : (isEdit ? " is-edit" : ""));
		return data;
	}

	// ===== 【listUrl】一覧(index.jsp)へ戻るURLを、検索条件つきで組み立てるメソッド =====
	private static String listUrl(String q, String no, String sortKey, String order, String page) {
		StringBuilder sb = new StringBuilder("index.jsp");
		String sep = "?";                       // 最初だけ「?」、2つ目以降は「&」
		sep = add(sb, sep, "q", q);
		sep = add(sb, sep, "no", no);
		sep = add(sb, sep, "sortKey", sortKey);
		sep = add(sb, sep, "order", order);
		sep = add(sb, sep, "page", page);
		return sb.toString();
	}

	// ===== 【detailUrl】保存後に開く「その伝票の閲覧画面」URLを組み立てるメソッド =====
	private static String detailUrl(int id, String q, String no, String sortKey, String order, String page) {
		StringBuilder sb = new StringBuilder("detail.jsp?id=").append(id);
		add(sb, "&", "q", q);       // id が既にあるので区切りは常に「&」
		add(sb, "&", "no", no);
		add(sb, "&", "sortKey", sortKey);
		add(sb, "&", "order", order);
		add(sb, "&", "page", page);
		return sb.toString();
	}

	// ===== 【add】URLに「区切り+名前=値」を足す小道具（値が空なら足さない）。次の区切りを返す =====
	private static String add(StringBuilder sb, String sep, String key, String value) {
		if (value == null || value.isEmpty()) return sep;
		try {
			sb.append(sep).append(key).append("=").append(URLEncoder.encode(value, "UTF-8"));
		} catch (Exception e) {
			// UTF-8は必ずあるのでここには来ない
		}
		return "&";
	}

	// ===== 【parseId】"3" のような文字列を Integer に。数字でなければ null を返すメソッド =====
	private static Integer parseId(String s) {
		try {
			return Integer.valueOf(s);
		} catch (Exception e) {
			return null;
		}
	}

	// ===== 【nz】null なら空文字 "" に変えるメソッド（null対策）=====
	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
