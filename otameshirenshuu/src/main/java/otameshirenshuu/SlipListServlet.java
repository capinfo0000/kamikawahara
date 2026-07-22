package otameshirenshuu;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * ============================================================================
 * 【一覧画面のコントローラ（処理担当）】
 *   以前は index.jsp の先頭に書いていた Java 処理を、この別ファイルに移しました。
 *   流れは次のとおりです。
 *     1) ブラウザが  /list  を開く（検索・並び替え・ページ番号はURLで受け取る）
 *     2) ここで削除・検索・並び替え・ページ切り出しを計算する
 *     3) 計算結果を1つの HashMap（連想配列）にまとめる
 *     4) その HashMap を request に載せて index.jsp へ渡し、表示だけ任せる
 *   ※ 表示に必要なものは request.setAttribute を何度も呼ばず、
 *      HashMap 1つ（キー"data"）にまとめて受け渡しします。
 * ============================================================================
 */
@WebServlet("/list")
public class SlipListServlet extends HttpServlet {

	private static final int PAGE_SIZE = 30; // 1ページに表示する件数

	// GET でアクセスされたとき（一覧表示・削除リンク・ソート・ページ移動 すべて GET）
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// データ処理担当（DB接続やテーブル作成もこの中でやってくれる）
		SlipDao dao = new SlipDao();

		// --- 検索・並び替えの条件をURLパラメータから受け取る（無ければ既定値）---
		String q = request.getParameter("q");
		if (q == null) q = "";                    // キーワード（日付・取引先・購入物）
		String no = request.getParameter("no");
		if (no == null) no = "";                  // 伝票番号（例:5 または 1~10）
		String sortKey = request.getParameter("sortKey");
		if (!"date".equals(sortKey)) sortKey = "id";   // 並べる列（既定は伝票番号）
		String order = request.getParameter("order");
		if (!"asc".equals(order)) order = "desc";      // 昇順/降順（既定は降順）

		// リンクに検索条件を引き継ぐための文字列（ソートやページ移動でも条件を保つ）
		String searchParams =
				(q.isEmpty()  ? "" : "&q="  + URLEncoder.encode(q,  "UTF-8"))
			  + (no.isEmpty() ? "" : "&no=" + URLEncoder.encode(no, "UTF-8"));

		// --- (1) 削除：/list?action=delete&id=... のとき、消してから一覧へ戻す ---
		if ("delete".equals(request.getParameter("action"))) {
			try {
				dao.delete(Integer.parseInt(request.getParameter("id")));
			} catch (Exception e) {
				// idが数字でない等は無視（そのまま一覧へ）
			}
			// 元の並び順・ページ・検索条件を保ったまま一覧へリダイレクト
			String pageParam = request.getParameter("page");
			if (pageParam == null) pageParam = "1";
			response.sendRedirect("list?sortKey=" + sortKey + "&order=" + order
					+ "&page=" + pageParam + searchParams);
			return;
		}

		// --- (2) 一覧を取得（検索・並び替えは SlipDao の SQL で実施）---
		ArrayList<Slip> slips = dao.findFiltered(q, no, sortKey, order);

		// --- (3) ページング：全体から1ページ分（30件）だけ切り出す ---
		int total = slips.size();
		int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
		int pageNo = 1;
		try {
			pageNo = Integer.parseInt(request.getParameter("page"));
		} catch (Exception e) {
			pageNo = 1; // 未指定や数字でなければ1ページ目
		}
		if (pageNo < 1) pageNo = 1;
		if (pageNo > totalPages) pageNo = totalPages; // 範囲外は端に寄せる
		int from = (pageNo - 1) * PAGE_SIZE;
		int to = Math.min(from + PAGE_SIZE, total);
		// subList はビューなので、独立した ArrayList に作り直して渡す
		ArrayList<Slip> pageSlips = (total == 0)
				? slips
				: new ArrayList<>(slips.subList(from, to));

		// --- 表示に使う小物（見出しの矢印と、クリックしたときの次の並び順）---
		boolean hasSearch = !q.isEmpty() || !no.isEmpty();
		String idNextOrder   = ("id".equals(sortKey)   && "asc".equals(order)) ? "desc" : "asc";
		String dateNextOrder = ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
		String idArrow   = "id".equals(sortKey)   ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String dateArrow = "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
		String navParams = "sortKey=" + sortKey + "&order=" + order + searchParams;

		// --- (4) 表示用データを HashMap（連想配列）に1つにまとめる ---
		//     「キー(名前) → 値」の形で入れておき、index.jsp では data.get("名前") で取り出す。
		HashMap<String, Object> data = new HashMap<>();
		data.put("pageSlips", pageSlips);     // この画面に出す伝票一覧
		data.put("total", total);             // 全件数
		data.put("totalPages", totalPages);   // 総ページ数
		data.put("pageNo", pageNo);           // 今のページ番号
		data.put("q", q);                     // 入力中のキーワード
		data.put("no", no);                   // 入力中の伝票番号
		data.put("sortKey", sortKey);         // 並べている列
		data.put("order", order);             // 昇順/降順
		data.put("hasSearch", hasSearch);     // 検索中かどうか
		data.put("searchParams", searchParams);   // リンク用の検索条件
		data.put("navParams", navParams);         // ページ移動リンク用
		data.put("idArrow", idArrow);             // 伝票番号見出しの矢印
		data.put("dateArrow", dateArrow);         // 日付見出しの矢印
		data.put("idNextOrder", idNextOrder);     // 伝票番号見出しを押した時の並び順
		data.put("dateNextOrder", dateNextOrder); // 日付見出しを押した時の並び順

		// HashMap をまとめて request に載せ、表示は index.jsp に任せる（フォワード）
		request.setAttribute("data", data);
		request.getRequestDispatcher("/index.jsp").forward(request, response);
	}
}
