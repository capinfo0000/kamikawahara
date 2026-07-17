package otameshirenshuu;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/detail")
public class DetailServlet extends HttpServlet {

	private final SlipStore store = SlipStore.getInstance();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 削除アクション（一覧・明細どちらの「削除」からも呼ばれる）
		String action = request.getParameter("action");
		if ("delete".equals(action)) {
			Integer id = parseIntOrNull(request.getParameter("id"));
			if (id != null) {
				store.delete(id);
			}
			// 削除後は、元の一覧（同じページ・並び順・検索条件）へ戻す（処理担当の /list 経由）
			response.sendRedirect(request.getContextPath() + "/list" + listQuery(request));
			return;
		}

		// 表示モード（view / edit / new）。指定がなければ view。
		String mode = request.getParameter("mode");
		if (mode == null || mode.isEmpty()) {
			mode = "view";
		}

		if ("new".equals(mode)) {
			// 新規登録：最初は空白5行
			List<Entry> detailList = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				detailList.add(new Entry());
			}
			request.setAttribute("currentMode", "new");
			request.setAttribute("slipId", null);
			request.setAttribute("slipDate", "");
			request.setAttribute("partnerName", "");
			request.setAttribute("description", "");
			request.setAttribute("note", "");
			request.setAttribute("detailList", detailList);
			request.getRequestDispatcher("/detail.jsp").forward(request, response);
			return;
		}

		// view / edit：既存伝票を読み込む
		Integer id = parseIntOrNull(request.getParameter("id"));
		Slip slip = (id != null) ? store.findById(id) : null;
		if (slip == null) {
			// 該当データがなければ一覧へ戻す（処理担当の /list 経由）
			response.sendRedirect(request.getContextPath() + "/list");
			return;
		}

		request.setAttribute("currentMode", "edit".equals(mode) ? "edit" : "view");
		setSlipAttributes(request, slip);
		request.getRequestDispatcher("/detail.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		Integer id = parseIntOrNull(request.getParameter("id"));
		boolean isUpdate = (id != null && store.findById(id) != null);

		String slipDate = nullToEmpty(request.getParameter("slipDate"));
		String partnerName = nullToEmpty(request.getParameter("partnerName"));
		String description = nullToEmpty(request.getParameter("description"));
		String note = nullToEmpty(request.getParameter("note"));

		String[] debitSubjects = request.getParameterValues("debitSubject");
		String[] debitAmounts = request.getParameterValues("debitAmount");
		String[] creditSubjects = request.getParameterValues("creditSubject");
		String[] creditAmounts = request.getParameterValues("creditAmount");

		List<String> errorMessages = new ArrayList<>();
		boolean hasFormatError = false;

		// 借方側・貸方側を「列ごとに独立して」集め、空白マスを飛ばして上へ詰める。
		// （借方の途中が空でも、下に入力があれば上へ寄る／貸方も同様。左右は別々に詰める）
		List<String[]> debitSides = new ArrayList<>();   // {科目, 金額(数字のみ)}
		List<String[]> creditSides = new ArrayList<>();

		int rowCount = maxLen(debitSubjects, debitAmounts, creditSubjects, creditAmounts);
		for (int i = 0; i < rowCount; i++) {
			String dSub = at(debitSubjects, i);
			String dAmt = at(debitAmounts, i);
			String cSub = at(creditSubjects, i);
			String cAmt = at(creditAmounts, i);

			// 借方金額の書式チェック
			if (!dAmt.isEmpty() && !dAmt.matches("^[0-9,]+$") && !hasFormatError) {
				errorMessages.add("借方金額に半角数字以外が入力されています。");
				hasFormatError = true;
			}
			// 貸方金額の書式チェック
			if (!cAmt.isEmpty() && !cAmt.matches("^[0-9,]+$") && !hasFormatError) {
				errorMessages.add("貸方金額に半角数字以外が入力されています。");
				hasFormatError = true;
			}

			// 借方側：科目か金額のどちらかがあれば「データあり」として詰める
			if (!dSub.isEmpty() || !dAmt.isEmpty()) {
				debitSides.add(new String[] { dSub, digitsOf(dAmt) });
			}
			// 貸方側も同様に独立して詰める
			if (!cSub.isEmpty() || !cAmt.isEmpty()) {
				creditSides.add(new String[] { cSub, digitsOf(cAmt) });
			}
		}

		// 詰めた借方・貸方を上から突き合わせて明細行を作る
		List<Entry> entries = new ArrayList<>();
		int debitTotal = 0;
		int creditTotal = 0;
		int rows = Math.max(debitSides.size(), creditSides.size());
		for (int i = 0; i < rows; i++) {
			String[] d = (i < debitSides.size()) ? debitSides.get(i) : new String[] { "", "" };
			String[] c = (i < creditSides.size()) ? creditSides.get(i) : new String[] { "", "" };
			Entry e = new Entry(d[0], d[1], c[0], c[1]);
			debitTotal += e.getDebitValue();
			creditTotal += e.getCreditValue();
			entries.add(e);
		}

		// 明細が1行もなければエラー
		if (entries.isEmpty()) {
			errorMessages.add("明細を1行以上入力してください。");
		}

		// 貸借金額一致チェック（書式エラーがない場合のみ）
		if (!hasFormatError && !entries.isEmpty() && debitTotal != creditTotal) {
			errorMessages.add("借方と貸方の合計金額が一致しません。");
		}

		// エラーがあれば入力内容を保持したまま編集画面へ戻す
		if (!errorMessages.isEmpty()) {
			request.setAttribute("errors", errorMessages);
			request.setAttribute("currentMode", "edit");
			request.setAttribute("slipId", id);
			request.setAttribute("slipDate", slipDate);
			request.setAttribute("partnerName", partnerName);
			request.setAttribute("description", description);
			request.setAttribute("note", note);
			// 入力途中の行をそのまま返す（空白行も残して編集を継続できるようにする）
			request.setAttribute("detailList", rebuildRows(debitSubjects, debitAmounts, creditSubjects, creditAmounts));
			request.getRequestDispatcher("/detail.jsp").forward(request, response);
			return;
		}

		// 保存
		Slip slip = new Slip();
		slip.setDate(slipDate);
		slip.setPartnerName(partnerName);
		slip.setDescription(description);
		slip.setNote(note);
		slip.setEntries(entries);

		int savedId;
		if (isUpdate) {
			slip.setId(id);
			store.update(slip);
			savedId = id;
		} else {
			savedId = store.add(slip); // 伝票番号を自動採番
		}

		// 登録後は閲覧モードで表示する
		response.sendRedirect(request.getContextPath() + "/detail?mode=view&id=" + savedId);
	}

	private void setSlipAttributes(HttpServletRequest request, Slip slip) {
		request.setAttribute("slipId", slip.getId());
		request.setAttribute("slipDate", slip.getDate());
		request.setAttribute("partnerName", slip.getPartnerName());
		request.setAttribute("description", slip.getDescription());
		request.setAttribute("note", slip.getNote());
		request.setAttribute("detailList", slip.getEntries());
	}

	/** エラー時に、送信されたすべての行（空白行含む）を復元する。 */
	private List<Entry> rebuildRows(String[] dSub, String[] dAmt, String[] cSub, String[] cAmt) {
		List<Entry> list = new ArrayList<>();
		int n = (dAmt != null) ? dAmt.length : 0;
		for (int i = 0; i < n; i++) {
			list.add(new Entry(at(dSub, i), digitsOf(at(dAmt, i)), at(cSub, i), digitsOf(at(cAmt, i))));
		}
		if (list.isEmpty()) {
			list.add(new Entry());
		}
		return list;
	}

	/** 一覧に戻る際のクエリ（並び順・ページ・検索キーワード）を組み立てる。 */
	private String listQuery(HttpServletRequest request) throws IOException {
		StringBuilder sb = new StringBuilder();
		appendParam(sb, "sortKey", request.getParameter("sortKey"));
		appendParam(sb, "order", request.getParameter("order"));
		appendParam(sb, "page", request.getParameter("page"));
		appendParam(sb, "q", request.getParameter("q"));
		appendParam(sb, "no", request.getParameter("no"));
		return (sb.length() == 0) ? "" : "?" + sb.substring(1);
	}

	private void appendParam(StringBuilder sb, String key, String value) throws IOException {
		if (value == null || value.isEmpty()) {
			return;
		}
		sb.append("&").append(key).append("=").append(URLEncoder.encode(value, "UTF-8"));
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

	private static String digitsOf(String s) {
		return (s == null) ? "" : s.replaceAll("[^0-9]", "");
	}

	private static String nullToEmpty(String s) {
		return (s == null) ? "" : s;
	}

	private static Integer parseIntOrNull(String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
