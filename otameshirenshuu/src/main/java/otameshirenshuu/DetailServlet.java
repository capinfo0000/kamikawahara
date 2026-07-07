package otameshirenshuu;

import java.io.IOException;
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
			response.sendRedirect(request.getContextPath() + "/index.jsp");
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
			// 該当データがなければ一覧へ戻す
			response.sendRedirect(request.getContextPath() + "/index.jsp");
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

		// 送信された明細行を組み立てる。空白行（4項目すべて空）は取り込まず自動で詰める。
		List<Entry> entries = new ArrayList<>();
		int rowCount = (debitAmounts != null) ? debitAmounts.length : 0;
		int debitTotal = 0;
		int creditTotal = 0;
		boolean hasFormatError = false;

		for (int i = 0; i < rowCount; i++) {
			String dSub = at(debitSubjects, i);
			String dAmt = at(debitAmounts, i);
			String cSub = at(creditSubjects, i);
			String cAmt = at(creditAmounts, i);

			Entry entry = new Entry(dSub, digitsOf(dAmt), cSub, digitsOf(cAmt));

			// 空白行は登録データに含めない（上に空行があっても自動で詰まる）
			if (entry.isBlank()) {
				continue;
			}

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

			debitTotal += entry.getDebitValue();
			creditTotal += entry.getCreditValue();
			entries.add(entry);
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
