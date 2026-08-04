package otameshirenshuu;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * ============================================================================
 * 【SlipList】= 一覧画面の「頭脳」。index.jsp はここが作った HashMap を表示するだけ。
 *   検索・並び替え・ページング・（行の削除）をこのクラスで行う。
 *
 *   ◆流れ◆
 *     ・action=delete のとき … その伝票を消して、同じ条件の一覧へ戻す（return null）
 *     ・それ以外          … 検索条件で一覧を取り出し、1ページ30件に切り出して返す
 * ============================================================================
 */
public class SlipList {

	private static final int PAGE_SIZE = 30; // 1ページに表示する件数

	// ===== 【execute】一覧画面の要求を処理して、表示用 HashMap を返すメソッド =====
	//   削除して別URLへ飛ばしたときだけ null を返し、JSPは表示を止める。
	public HashMap<String, Object> execute(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		SlipDao dao = new SlipDao();

		// 1. 並び順（既定は伝票番号の降順）
		String sortKey = request.getParameter("sortKey");
		if (!"date".equals(sortKey)) sortKey = "id";
		String order = request.getParameter("order");
		if (!"asc".equals(order)) order = "desc";

		// 2. 検索条件（q=キーワード、no=伝票番号）
		String q = request.getParameter("q");
		if (q == null) q = "";
		String no = request.getParameter("no");
		if (no == null) no = "";

		// 3. リンクやhiddenに引き継ぐURLパラメータ
		String searchParams =
				(q.isEmpty()  ? "" : "&q="  + URLEncoder.encode(q,  "UTF-8"))
			  + (no.isEmpty() ? "" : "&no=" + URLEncoder.encode(no, "UTF-8"));

		// 4. 削除：index.jsp?action=delete&id=... のとき、消して同じ条件の一覧へ戻す
		if ("delete".equals(request.getParameter("action"))) {
			try {
				dao.delete(Integer.parseInt(request.getParameter("id")));
			} catch (Exception e) {
				// idが数字でない等は無視
			}
			String pageParam = request.getParameter("page");
			if (pageParam == null) pageParam = "1";
			response.sendRedirect("index.jsp?sortKey=" + sortKey + "&order=" + order
					+ "&page=" + pageParam + searchParams);
			return null;
		}

		// 5. 検索・並び替え済みの一覧を取得
		ArrayList<Slip> slips = dao.findFiltered(q, no, sortKey, order);

		// 6. ソート見出しの矢印と、クリック時の次の並び順
		boolean hasSearch = !q.isEmpty() || !no.isEmpty();
		String idNextOrder = ("id".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
		String dateNextOrder = ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
		String idArrow = "id".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String dateArrow = "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String navParams = "sortKey=" + sortKey + "&order=" + order + searchParams;

		// 7. ページング（1ページ30件）
		int total = slips.size();
		int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
		int pageNo = 1;
		try {
			String pp = request.getParameter("page");
			if (pp != null && !pp.trim().isEmpty()) pageNo = Integer.parseInt(pp.trim());
		} catch (NumberFormatException e) {
			pageNo = 1;
		}
		if (pageNo < 1) pageNo = 1;
		if (pageNo > totalPages) pageNo = totalPages;
		int from = (pageNo - 1) * PAGE_SIZE;
		int to = Math.min(from + PAGE_SIZE, total);
		// subList はビューなので、独立した ArrayList に作り直す
		ArrayList<Slip> pageSlips = (total == 0)
				? slips
				: new ArrayList<>(slips.subList(from, to));

		// 8. 表示用データを HashMap にまとめて index.jsp へ渡す
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("pageSlips", pageSlips);
		resp.put("total", total);
		resp.put("totalPages", totalPages);
		resp.put("pageNo", pageNo);
		resp.put("q", q);
		resp.put("no", no);
		resp.put("sortKey", sortKey);
		resp.put("order", order);
		resp.put("hasSearch", hasSearch);
		resp.put("searchParams", searchParams);
		resp.put("navParams", navParams);
		resp.put("idArrow", idArrow);
		resp.put("dateArrow", dateArrow);
		resp.put("idNextOrder", idNextOrder);
		resp.put("dateNextOrder", dateNextOrder);
		return resp;
	}
}
