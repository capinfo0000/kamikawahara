package otameshirenshuu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import otameshirenshuu.Slip.Entry; // 明細クラス（Slipの入れ子）を Entry の名前で使う

/*
 * ============================================================================
 * 【明細画面のコントローラ（処理担当）】
 *   以前は detail.jsp の先頭に書いていた Java 処理を、この別ファイルに移しました。
 *   受け付ける操作は次のとおりです。
 *     ・GET  /detail?mode=new              … 新規登録（空の5行を用意）
 *     ・GET  /detail?mode=view&id=..       … 閲覧
 *     ・GET  /detail?mode=edit&id=..       … 編集
 *     ・GET  /detail?action=delete&id=..   … 削除
 *     ・POST /detail                       … 登録／更新（フォーム送信）
 *   計算した表示用データは HashMap（連想配列）1つにまとめ、detail.jsp に渡します。
 * ============================================================================
 */
@WebServlet("/detail")
public class DetailServlet extends HttpServlet {

	// GET：表示（新規/閲覧/編集）と削除
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		SlipDao dao = new SlipDao();
		Integer idParam = parseId(request.getParameter("id"));

		// --- 削除：/detail?action=delete&id=... のとき、消して一覧へ戻す ---
		if ("delete".equals(request.getParameter("action"))) {
			if (idParam != null) dao.delete(idParam);
			response.sendRedirect("list");
			return;
		}

		String mode = request.getParameter("mode");
		HashMap<String, Object> data;

		if ("new".equals(mode)) {
			// 新規：空の明細を5行用意する
			ArrayList<Entry> entries = new ArrayList<>();
			for (int i = 0; i < 5; i++) entries.add(new Entry());
			data = buildView("new", null, "", "", "", "", entries, null);
		} else {
			// 閲覧 / 編集：既存の伝票を読み込む（modeが変な値なら閲覧扱い）
			if (!"edit".equals(mode)) mode = "view";
			Slip s = (idParam != null) ? dao.findById(idParam) : null;
			if (s == null) {            // 見つからなければ一覧へ
				response.sendRedirect("list");
				return;
			}
			data = buildView(mode, s.getId(), s.getDate(), s.getPartnerName(),
					s.getDescription(), s.getNote(), s.getEntries(), null);
		}

		// HashMap をまとめて渡し、表示は detail.jsp に任せる
		request.setAttribute("data", data);
		request.getRequestDispatcher("/detail.jsp").forward(request, response);
	}

	// POST：登録・更新（フォームの「登録」ボタンから送られてくる）
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8"); // 日本語の受け取り用
		SlipDao dao = new SlipDao();
		Integer idParam = parseId(request.getParameter("id"));

		// エラーメッセージの入れ物（保存に失敗したらここに理由が入る）
		ArrayList<String> errors = new ArrayList<>();
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
			// 成功 → 登録した伝票の閲覧画面へリダイレクト
			response.sendRedirect("detail?mode=view&id=" + savedId);
			return;
		}

		// 失敗 → 入力内容を保持したまま編集画面を再表示（errorsにメッセージ入り）
		ArrayList<Entry> entries = dao.rebuildRows(
				request.getParameterValues("debitSubject"),
				request.getParameterValues("debitAmount"),
				request.getParameterValues("creditSubject"),
				request.getParameterValues("creditAmount"));
		HashMap<String, Object> data = buildView("edit", idParam,
				nz(request.getParameter("slipDate")),
				nz(request.getParameter("partnerName")),
				nz(request.getParameter("description")),
				nz(request.getParameter("note")),
				entries, errors);

		request.setAttribute("data", data);
		request.getRequestDispatcher("/detail.jsp").forward(request, response);
	}

	/*
	 * 画面表示に必要な値を1つの HashMap（連想配列）にまとめる共通処理。
	 * mode      : "new" / "view" / "edit"
	 * slipId    : 伝票番号（新規はnull）
	 * entries   : 明細行の一覧
	 * errors    : エラーメッセージ（無ければnull）
	 * detail.jsp では data.get("名前") で取り出して表示するだけにする。
	 */
	private HashMap<String, Object> buildView(String mode, Integer slipId,
			String dateRaw, String partner, String desc, String note,
			ArrayList<Entry> entries, ArrayList<String> errors) {

		boolean isView = "view".equals(mode);
		boolean isNew  = "new".equals(mode);
		boolean isEdit = "edit".equals(mode);

		// 合計金額（借方・貸方それぞれ足し合わせて表示に使う）
		int debitTotal = 0, creditTotal = 0;
		for (Entry e : entries) {
			debitTotal  += e.getDebitValue();
			creditTotal += e.getCreditValue();
		}

		HashMap<String, Object> data = new HashMap<>();
		data.put("mode", mode);
		data.put("isView", isView);
		data.put("isNew", isNew);
		data.put("isEdit", isEdit);
		data.put("slipId", slipId);          // 新規のときは null
		data.put("dateRaw", dateRaw);
		data.put("partner", partner);
		data.put("desc", desc);
		data.put("note", note);
		data.put("entries", entries);
		data.put("errors", errors);          // 無ければ null
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
