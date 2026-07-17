package denpyo.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import denpyo.model.SlipDao;

/**
 * 【Controller】振替伝票のフロントコントローラ。
 * URL: /denpyo?action=list|view|edit|new|save|delete
 *
 * ・処理はすべてここ(＋Model)で行い、Viewには表示用データ(Map)だけ渡す。
 * ・ViewのJSPは WEB-INF 配下に置き、必ずこのControllerを通す（直接アクセス不可）。
 */
@WebServlet("/denpyo")
public class SlipController extends HttpServlet {

	private static final int PAGE_SIZE = 30;
	private final SlipDao dao = new SlipDao();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		String action = param(req, "action", "list");
		switch (action) {
			case "view":
				showDetail(req, res, "view");
				break;
			case "edit":
				showDetail(req, res, "edit");
				break;
			case "new":
				showNew(req, res);
				break;
			case "delete":
				delete(req, res);
				break;
			case "list":
			default:
				showList(req, res);
				break;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		// 登録・更新
		save(req, res);
	}

	// ===== 一覧 =====
	private void showList(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		String q = param(req, "q", "");
		String no = param(req, "no", "");
		String sortKey = "date".equals(req.getParameter("sortKey")) ? "date" : "id";
		String order = "asc".equals(req.getParameter("order")) ? "asc" : "desc";

		int total = dao.count(q, no);
		int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
		int page = parseInt(req.getParameter("page"), 1);
		if (page < 1) page = 1;
		if (page > totalPages) page = totalPages;
		int offset = (page - 1) * PAGE_SIZE;

		List<Map<String, Object>> rows = dao.search(q, no, sortKey, order, PAGE_SIZE, offset);

		// 検索・並び替えリンクに引き継ぐパラメータ（Viewで組み立てずここで用意）
		String searchParams = enc("&q=", q) + enc("&no=", no);
		String navParams = "sortKey=" + sortKey + "&order=" + order + searchParams;

		req.setAttribute("rows", rows);
		req.setAttribute("q", q);
		req.setAttribute("no", no);
		req.setAttribute("sortKey", sortKey);
		req.setAttribute("order", order);
		req.setAttribute("page", page);
		req.setAttribute("totalPages", totalPages);
		req.setAttribute("total", total);
		req.setAttribute("hasSearch", !q.isEmpty() || !no.isEmpty());
		req.setAttribute("searchParams", searchParams);
		req.setAttribute("navParams", navParams);
		// ソート矢印と、クリック時の次の並び順
		req.setAttribute("idArrow", "id".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼");
		req.setAttribute("dateArrow", "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼");
		req.setAttribute("idNextOrder", ("id".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc");
		req.setAttribute("dateNextOrder", ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc");

		forward(req, res, "list");
	}

	// ===== 明細（閲覧・編集）=====
	private void showDetail(HttpServletRequest req, HttpServletResponse res, String mode)
			throws ServletException, IOException {
		Integer id = numOrNull(req.getParameter("id"));
		Map<String, Object> slip = (id != null) ? dao.findById(id) : null;
		if (slip == null) {
			res.sendRedirect(req.getContextPath() + "/denpyo");
			return;
		}
		req.setAttribute("slip", slip);
		req.setAttribute("mode", mode);
		forward(req, res, "detail");
	}

	// ===== 新規（空フォーム）=====
	private void showNew(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		Map<String, Object> slip = new LinkedHashMap<>();
		slip.put("id", null);
		slip.put("date", "");
		slip.put("partnerName", "");
		slip.put("description", "");
		slip.put("note", "");
		List<Map<String, Object>> entries = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			entries.add(emptyEntry());
		}
		slip.put("entries", entries);
		slip.put("debitTotal", 0);
		slip.put("creditTotal", 0);
		req.setAttribute("slip", slip);
		req.setAttribute("mode", "new");
		forward(req, res, "detail");
	}

	// ===== 削除 =====
	private void delete(HttpServletRequest req, HttpServletResponse res) throws IOException {
		Integer id = numOrNull(req.getParameter("id"));
		if (id != null) {
			dao.delete(id);
		}
		// 元の一覧条件（ページ・並び順・検索）を保って戻る
		res.sendRedirect(req.getContextPath() + "/denpyo" + listQuery(req));
	}

	// ===== 登録・更新 =====
	private void save(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		Integer id = numOrNull(req.getParameter("id"));
		String date = param(req, "slipDate", "");
		String partnerName = param(req, "partnerName", "");
		String description = param(req, "description", "");
		String note = param(req, "note", "");

		String[] dSub = req.getParameterValues("debitSubject");
		String[] dAmt = req.getParameterValues("debitAmount");
		String[] cSub = req.getParameterValues("creditSubject");
		String[] cAmt = req.getParameterValues("creditAmount");

		List<String> errors = new ArrayList<>();
		boolean formatError = false;

		// 借方・貸方を列ごとに詰める（空マスは飛ばして上へ）
		List<Map<String, Object>> debits = new ArrayList<>();
		List<Map<String, Object>> credits = new ArrayList<>();
		int rows = maxLen(dSub, dAmt, cSub, cAmt);
		for (int i = 0; i < rows; i++) {
			String ds = at(dSub, i), da = at(dAmt, i), cs = at(cSub, i), ca = at(cAmt, i);
			if (!da.isEmpty() && !da.matches("^[0-9,]+$") && !formatError) {
				errors.add("借方金額に半角数字以外が入力されています。");
				formatError = true;
			}
			if (!ca.isEmpty() && !ca.matches("^[0-9,]+$") && !formatError) {
				errors.add("貸方金額に半角数字以外が入力されています。");
				formatError = true;
			}
			if (!ds.isEmpty() || !da.isEmpty()) {
				debits.add(side(ds, digits(da)));
			}
			if (!cs.isEmpty() || !ca.isEmpty()) {
				credits.add(side(cs, digits(ca)));
			}
		}

		// 詰めた借方・貸方を突き合わせて明細行を作る
		List<Map<String, Object>> entries = new ArrayList<>();
		int debitTotal = 0, creditTotal = 0;
		int n = Math.max(debits.size(), credits.size());
		for (int i = 0; i < n; i++) {
			Map<String, Object> d = (i < debits.size()) ? debits.get(i) : side("", 0);
			Map<String, Object> c = (i < credits.size()) ? credits.get(i) : side("", 0);
			Map<String, Object> e = new LinkedHashMap<>();
			e.put("debitSubject", d.get("subject"));
			e.put("debitAmount", d.get("amount"));
			e.put("creditSubject", c.get("subject"));
			e.put("creditAmount", c.get("amount"));
			entries.add(e);
			debitTotal += (Integer) d.get("amount");
			creditTotal += (Integer) c.get("amount");
		}

		if (entries.isEmpty()) {
			errors.add("明細を1行以上入力してください。");
		}
		if (!formatError && !entries.isEmpty() && debitTotal != creditTotal) {
			errors.add("借方と貸方の合計金額が一致しません。");
		}

		// 伝票データ(Map)を組み立て
		Map<String, Object> slip = new LinkedHashMap<>();
		slip.put("id", id);
		slip.put("date", date);
		slip.put("partnerName", partnerName);
		slip.put("description", description);
		slip.put("note", note);
		slip.put("entries", entries);
		slip.put("debitTotal", debitTotal);
		slip.put("creditTotal", creditTotal);

		if (!errors.isEmpty()) {
			// エラー時は入力内容を保ったまま編集画面へ戻す
			slip.put("entries", rebuildRows(dSub, dAmt, cSub, cAmt));
			req.setAttribute("slip", slip);
			req.setAttribute("mode", "edit");
			req.setAttribute("errors", errors);
			forward(req, res, "detail");
			return;
		}

		int savedId = dao.save(slip);
		res.sendRedirect(req.getContextPath() + "/denpyo?action=view&id=" + savedId);
	}

	// ===== ヘルパー =====
	private Map<String, Object> emptyEntry() {
		Map<String, Object> e = new LinkedHashMap<>();
		e.put("debitSubject", "");
		e.put("debitAmount", 0);
		e.put("creditSubject", "");
		e.put("creditAmount", 0);
		return e;
	}

	private Map<String, Object> side(String subject, int amount) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("subject", subject);
		m.put("amount", amount);
		return m;
	}

	/** エラー時に入力された全行（空行含む）を復元。 */
	private List<Map<String, Object>> rebuildRows(String[] dSub, String[] dAmt, String[] cSub, String[] cAmt) {
		List<Map<String, Object>> list = new ArrayList<>();
		int rows = maxLen(dSub, dAmt, cSub, cAmt);
		for (int i = 0; i < rows; i++) {
			Map<String, Object> e = new LinkedHashMap<>();
			e.put("debitSubject", at(dSub, i));
			e.put("debitAmount", digits(at(dAmt, i)));
			e.put("creditSubject", at(cSub, i));
			e.put("creditAmount", digits(at(cAmt, i)));
			list.add(e);
		}
		if (list.isEmpty()) {
			list.add(emptyEntry());
		}
		return list;
	}

	private String listQuery(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		append(sb, "sortKey", req.getParameter("sortKey"));
		append(sb, "order", req.getParameter("order"));
		append(sb, "page", req.getParameter("page"));
		append(sb, "q", req.getParameter("q"));
		append(sb, "no", req.getParameter("no"));
		return sb.length() == 0 ? "" : "?" + sb.substring(1);
	}

	private void append(StringBuilder sb, String k, String v) throws IOException {
		if (v != null && !v.isEmpty()) {
			sb.append("&").append(k).append("=").append(URLEncoder.encode(v, "UTF-8"));
		}
	}

	private void forward(HttpServletRequest req, HttpServletResponse res, String view)
			throws ServletException, IOException {
		req.getRequestDispatcher("/WEB-INF/denpyo/" + view + ".jsp").forward(req, res);
	}

	private static String param(HttpServletRequest req, String name, String def) {
		String v = req.getParameter(name);
		return (v == null) ? def : v;
	}

	private static int maxLen(String[]... arrays) {
		int max = 0;
		for (String[] a : arrays) {
			if (a != null && a.length > max) {
				max = a.length;
			}
		}
		return max;
	}

	private static String at(String[] arr, int i) {
		return (arr != null && arr.length > i && arr[i] != null) ? arr[i] : "";
	}

	private static int digits(String s) {
		if (s == null) {
			return 0;
		}
		String d = s.replaceAll("[^0-9]", "");
		return d.isEmpty() ? 0 : Integer.parseInt(d);
	}

	private static Integer numOrNull(String s) {
		if (s == null || s.trim().isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parseInt(String s, int def) {
		Integer v = numOrNull(s);
		return v == null ? def : v;
	}

	private static String enc(String prefix, String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		try {
			return prefix + URLEncoder.encode(value, "UTF-8");
		} catch (IOException e) {
			return "";
		}
	}
}
