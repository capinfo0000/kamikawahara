package otameshirenshuu;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 一覧画面の処理担当（検索・ソート・ページング）。
 * ここで計算した結果だけを index.jsp に渡し、index.jsp は表示に専念する。
 * URL: /list
 */
@WebServlet("/list")
public class SlipListServlet extends HttpServlet {

	private static final int PAGE_SIZE = 30;
	private final SlipStore store = SlipStore.getInstance();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 1. 並び順（既定は伝票番号の降順）
		String sortKey = request.getParameter("sortKey");
		if (!"date".equals(sortKey)) {
			sortKey = "id";
		}
		String order = request.getParameter("order");
		if (!"asc".equals(order)) {
			order = "desc";
		}

		// 2. 検索条件（q=キーワード、no=伝票番号）
		String q = request.getParameter("q");
		if (q == null) {
			q = "";
		}
		String no = request.getParameter("no");
		if (no == null) {
			no = "";
		}

		// 3. 検索・並び替え済みの一覧を取得
		List<Slip> slips = store.findFiltered(q, no, sortKey, order);

		// 4. リンクに引き継ぐURLパラメータ
		String qParam = q.isEmpty() ? "" : ("&q=" + URLEncoder.encode(q, "UTF-8"));
		String noParam = no.isEmpty() ? "" : ("&no=" + URLEncoder.encode(no, "UTF-8"));
		String searchParams = qParam + noParam;
		boolean hasSearch = !q.isEmpty() || !no.isEmpty();

		// 5. ソート見出しの矢印と、クリック時の次の並び順
		String idNextOrder = ("id".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
		String dateNextOrder = ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
		String idArrow = "id".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String dateArrow = "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";

		// 6. ページング（1ページ30件）
		int total = slips.size();
		int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
		int pageNo = 1;
		try {
			String pp = request.getParameter("page");
			if (pp != null && !pp.trim().isEmpty()) {
				pageNo = Integer.parseInt(pp.trim());
			}
		} catch (NumberFormatException e) {
			pageNo = 1;
		}
		if (pageNo < 1) pageNo = 1;
		if (pageNo > totalPages) pageNo = totalPages;

		int from = (pageNo - 1) * PAGE_SIZE;
		int to = Math.min(from + PAGE_SIZE, total);
		List<Slip> pageSlips = (total == 0) ? slips : slips.subList(from, to);

		String navParams = "sortKey=" + sortKey + "&order=" + order + searchParams;

		// 7. 表示用データを index.jsp に渡して表示させる
		request.setAttribute("pageSlips", pageSlips);
		request.setAttribute("total", total);
		request.setAttribute("totalPages", totalPages);
		request.setAttribute("pageNo", pageNo);
		request.setAttribute("q", q);
		request.setAttribute("no", no);
		request.setAttribute("sortKey", sortKey);
		request.setAttribute("order", order);
		request.setAttribute("hasSearch", hasSearch);
		request.setAttribute("searchParams", searchParams);
		request.setAttribute("navParams", navParams);
		request.setAttribute("idArrow", idArrow);
		request.setAttribute("dateArrow", dateArrow);
		request.setAttribute("idNextOrder", idNextOrder);
		request.setAttribute("dateNextOrder", dateNextOrder);

		request.getRequestDispatcher("/index.jsp").forward(request, response);
	}
}
