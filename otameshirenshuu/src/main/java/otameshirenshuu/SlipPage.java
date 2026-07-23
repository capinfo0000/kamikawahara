package otameshirenshuu;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import otameshirenshuu.Slip.Entry; // 明細クラス（Slipの入れ子）を Entry の名前で使う

/*
 * ============================================================================
 * 【画面の下ごしらえ担当（サーブレットは使わない普通のクラス）】
 *   これまで index.jsp / detail.jsp の先頭に書いていた Java 処理を、
 *   そのままこのクラスへ引っ越しました。JSP は
 *       HashMap<String,Object> data = SlipPage.list(request, response);
 *   のように「呼ぶだけ」になり、表示に専念できます。
 *
 *   ・HttpServlet を継承したり @WebServlet を付けたりはしていません
 *     （＝サーブレットの形ではありません）。ただの静的メソッドの集まりです。
 *   ・戻り値は表示に必要な値をまとめた HashMap。
 *     削除やエラー無し登録の後など「画面を出さずに別URLへ飛ばした」ときは
 *     null を返すので、JSP 側は  if (data == null) return;  で止めます。
 * ============================================================================
 */
public class SlipPage {

	private static final int PAGE_SIZE = 30; // 1ページに表示する件数

	// ========================================================================
	// 一覧画面（index.jsp）の下ごしらえ … 削除・検索・並び替え・ページング
	// ========================================================================
	public static HashMap<String, Object> list(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		// データ処理担当（DB接続やテーブル作成もこの中でやってくれる）
		SlipDao dao = new SlipDao();

		// --- 検索・並び替えの条件をURLパラメータから受け取る（無ければ既定値）---
		String q = request.getParameter("q");
		if (q == null) q = "";                       // キーワード
		String no = request.getParameter("no");
		if (no == null) no = "";                     // 伝票番号（例:5 または 1~10）
		String sortKey = request.getParameter("sortKey");
		if (!"date".equals(sortKey)) sortKey = "id"; // 並べる列（既定は伝票番号）
		String order = request.getParameter("order");
		if (!"asc".equals(order)) order = "desc";    // 昇順/降順（既定は降順）

		// リンクに検索条件を引き継ぐための文字列
		String searchParams =
				(q.isEmpty()  ? "" : "&q="  + URLEncoder.encode(q,  "UTF-8"))
			  + (no.isEmpty() ? "" : "&no=" + URLEncoder.encode(no, "UTF-8"));

		// --- 削除：index.jsp?action=delete&id=... のとき、消して一覧へ戻す ---
		if ("delete".equals(request.getParameter("action"))) {
			try {
				dao.delete(Integer.parseInt(request.getParameter("id")));
			} catch (Exception e) {
				// idが数字でない等は無視
			}
			String pageParam = request.getParameter("page");
			if (pageParam == null) pageParam = "1";
			// 元の並び順・ページ・検索条件を保ったまま一覧へ飛ばす
			response.sendRedirect("index.jsp?sortKey=" + sortKey + "&order=" + order
					+ "&page=" + pageParam + searchParams);
			return null; // 飛ばしたので画面は出さない
		}

		// --- 一覧を取得（検索・並び替えは SlipDao の SQL で実施）---
		ArrayList<Slip> slips = dao.findFiltered(q, no, sortKey, order);

		// --- ページング：全体から1ページ分（30件）だけ切り出す ---
		int total = slips.size();
		int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
		int pageNo = 1;
		try {
			pageNo = Integer.parseInt(request.getParameter("page"));
		} catch (Exception e) {
			pageNo = 1; // 未指定や数字でなければ1ページ目
		}
		if (pageNo < 1) pageNo = 1;
		if (pageNo > totalPages) pageNo = totalPages;
		int from = (pageNo - 1) * PAGE_SIZE;
		int to = Math.min(from + PAGE_SIZE, total);
		// subList はビューなので、独立した ArrayList に作り直す
		ArrayList<Slip> pageSlips = (total == 0)
				? slips
				: new ArrayList<>(slips.subList(from, to));

		// --- 表示に使う小物（見出しの矢印と、押したときの次の並び順）---
		boolean hasSearch = !q.isEmpty() || !no.isEmpty();
		String idNextOrder   = ("id".equals(sortKey)   && "asc".equals(order)) ? "desc" : "asc";
		String dateNextOrder = ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
		String idArrow   = "id".equals(sortKey)   ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String dateArrow = "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String navParams = "sortKey=" + sortKey + "&order=" + order + searchParams;

		// --- 表示用データを HashMap（連想配列）に1つにまとめて返す ---
		HashMap<String, Object> data = new HashMap<>();
		data.put("pageSlips", pageSlips);
		data.put("total", total);
		data.put("totalPages", totalPages);
		data.put("pageNo", pageNo);
		data.put("q", q);
		data.put("no", no);
		data.put("sortKey", sortKey);
		data.put("order", order);
		data.put("hasSearch", hasSearch);
		data.put("searchParams", searchParams);
		data.put("navParams", navParams);
		data.put("idArrow", idArrow);
		data.put("dateArrow", dateArrow);
		data.put("idNextOrder", idNextOrder);
		data.put("dateNextOrder", dateNextOrder);
		return data;
	}

	// ========================================================================
	// 明細画面（detail.jsp）の下ごしらえ … 閲覧・編集・新規・削除・登録
	// ========================================================================
	public static HashMap<String, Object> detail(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		request.setCharacterEncoding("UTF-8"); // 日本語の受け取り用
		SlipDao dao = new SlipDao();
		Integer idParam = parseId(request.getParameter("id"));

		// --- 削除：detail.jsp?action=delete&id=... のとき、消して一覧へ戻す ---
		if ("delete".equals(request.getParameter("action"))) {
			if (idParam != null) dao.delete(idParam);
			response.sendRedirect("index.jsp");
			return null;
		}

		// --- 登録・更新：フォームがPOSTで送られてきたとき ---
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			ArrayList<String> errors = new ArrayList<>(); // 失敗理由の入れ物
			int savedId = dao.saveFromForm(
					idParam,
					request.getParameter("slipDate"),
					request.getParameter("partnerName"),
					request.getParameter("description"),
					request.getParameter("note"),
					request.getParameterValues("debitSubject"),
					request.getParameterValues("debitAmount"),
					request.getParameterValues("creditSubject"),
					request.getParameterValues("creditAmount"),
					errors);
			if (savedId > 0) {
				// 成功 → 登録した伝票の閲覧画面へ飛ばす
				response.sendRedirect("detail.jsp?mode=view&id=" + savedId);
				return null;
			}
			// 失敗 → 入力内容を保持したまま編集画面を再表示（errorsにメッセージ入り）
			ArrayList<Entry> entries = dao.rebuildRows(
					request.getParameterValues("debitSubject"),
					request.getParameterValues("debitAmount"),
					request.getParameterValues("creditSubject"),
					request.getParameterValues("creditAmount"));
			return buildView("edit", idParam,
					nz(request.getParameter("slipDate")),
					nz(request.getParameter("partnerName")),
					nz(request.getParameter("description")),
					nz(request.getParameter("note")),
					entries, errors);
		}

		// --- 表示のための読み込み（削除でもPOSTでもないとき）---
		String mode = request.getParameter("mode");
		if ("new".equals(mode)) {
			// 新規：空の明細を5行用意する
			ArrayList<Entry> entries = new ArrayList<>();
			for (int i = 0; i < 5; i++) entries.add(new Entry());
			return buildView("new", null, "", "", "", "", entries, null);
		}
		// 閲覧 / 編集：既存の伝票を読み込む（modeが変な値なら閲覧扱い）
		if (!"edit".equals(mode)) mode = "view";
		Slip s = (idParam != null) ? dao.findById(idParam) : null;
		if (s == null) {                 // 見つからなければ一覧へ
			response.sendRedirect("index.jsp");
			return null;
		}
		return buildView(mode, s.getId(), s.getDate(), s.getPartnerName(),
				s.getDescription(), s.getNote(), s.getEntries(), null);
	}

	/*
	 * 明細画面の表示に必要な値を1つの HashMap にまとめる共通処理。
	 * mode="new"/"view"/"edit"、slipId は新規のとき null、errors は無ければ null。
	 */
	private static HashMap<String, Object> buildView(String mode, Integer slipId,
			String dateRaw, String partner, String desc, String note,
			ArrayList<Entry> entries, ArrayList<String> errors) {

		boolean isView = "view".equals(mode);
		boolean isNew  = "new".equals(mode);
		boolean isEdit = "edit".equals(mode);

		// 合計金額（借方・貸方それぞれ足し合わせる）
		int debitTotal = 0, creditTotal = 0;
		for (Entry e : entries) {
			debitTotal  += e.getDebitValue();
			creditTotal += e.getCreditValue();
		}

		HashMap<String, Object> data = new HashMap<>();
		data.put("isView", isView);
		data.put("isNew", isNew);
		data.put("isEdit", isEdit);
		data.put("slipId", slipId);       // 新規のときは null
		data.put("dateRaw", dateRaw);
		data.put("partner", partner);
		data.put("desc", desc);
		data.put("note", note);
		data.put("entries", entries);
		data.put("errors", errors);       // 無ければ null
		data.put("debitTotal", debitTotal);
		data.put("creditTotal", creditTotal);
		// CSS切り替え用（新規/編集で見た目を少し変える）
		data.put("pageMod", isNew ? " is-new" : (isEdit ? " is-edit" : ""));
		return data;
	}

	/** "3" のような文字列を Integer に。数字でなければ null。 */
	private static Integer parseId(String s) {
		try {
			return Integer.valueOf(s);
		} catch (Exception e) {
			return null;
		}
	}

	/** null なら空文字にする小道具。 */
	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
