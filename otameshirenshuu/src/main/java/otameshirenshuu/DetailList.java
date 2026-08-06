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
 * 【DetailList】= 明細画面の「頭脳」。
 *   画面をどう見せるか（閲覧か入力か、金額に¥やカンマを付けるか等）を
 *   すべてここ（サーバー側）で決め、「そのまま画面に出す文字列（HTML部品）」を
 *   HashMap に入れて detail.jsp に渡します。
 *   → detail.jsp は if 文で分岐せず、部品を並べて出すだけ（＝JSPは何も考えない）。
 *
 *   ◆情報の流れ◆
 *     ブラウザ → request（id・action・入力欄）→ DetailList が判断・加工
 *       → HashMap（表示部品）→ detail.jsp が <%= %> で出力 → ブラウザ
 *
 *   ◆画面の種類（mode）◆
 *     new  … 新規入力（IDなし）    view … 閲覧（IDあり）
 *     edit … 修正入力             confirm … 登録前の確認（内容を見せる画面）
 * ============================================================================
 */
public class DetailList {

	// ===== 【detail】明細画面のすべての要求を受け取り、表示部品の HashMap を返すメソッド =====
	//   別URLへ飛ばした（リダイレクトした）ときだけ null を返し、JSPは表示を止める。
	//   ↓↓ ここは「処理の順番」どおりに上から書いています ↓↓
	public static HashMap<String, Object> detail(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		// ---- 0. 画面から届いた情報を受け取る（request から取り出す）----
		request.setCharacterEncoding("UTF-8");               // 日本語の受け取り準備
		SlipDao dao = new SlipDao();                         // データ処理係を用意
		Integer id = parseId(request.getParameter("id"));    // 伝票番号（無ければ null＝新規）
		String action = nz(request.getParameter("action"));  // 押されたボタンの合図
		// 一覧の検索条件・ページ（画面をまたいで保持する。戻り先URLにも使う）
		String q = nz(request.getParameter("q"));
		String no = nz(request.getParameter("no"));
		String sortKey = nz(request.getParameter("sortKey"));
		String order = nz(request.getParameter("order"));
		String page = nz(request.getParameter("page"));
		String listUrl = listUrl(q, no, sortKey, order, page); // 「←伝票一覧」の戻り先
		// フォームの入力欄（新規/編集のPOSTで届く。GETのときは null）
		String fDate = request.getParameter("slipDate");
		String fPartner = request.getParameter("partnerName");
		String fDesc = request.getParameter("description");
		String fNote = request.getParameter("note");
		String[] dSub = request.getParameterValues("debitSubject");
		String[] dAmt = request.getParameterValues("debitAmount");
		String[] cSub = request.getParameterValues("creditSubject");
		String[] cAmt = request.getParameterValues("creditAmount");

		// ---- 1. 初期表示（ボタンの合図が無いGET）: IDで閲覧か新規かを決める ----
		if (action.isEmpty()) {
			if (id == null) {
				// IDなし → 新規登録用。空の明細を5行だけ用意して「入力できる状態」で表示
				ArrayList<Entry> entries = new ArrayList<>();
				for (int i = 0; i < 5; i++) entries.add(new Entry());
				return buildData(request, "new", null, "", "", "", "", entries, null);
			}
			// IDあり → その伝票をDBから読み、HashMapに入れて「閲覧用」で表示
			Slip s = dao.findById(id);
			if (s == null) { response.sendRedirect(listUrl); return null; } // 無ければ一覧へ
			return buildData(request, "view", s.getId(), s.getDate(), s.getPartnerName(),
					s.getDescription(), s.getNote(), s.getEntries(), null);
		}

		// ---- 2. 閲覧 → 編集：view画面の「編集」から（GET action=edit）----
		//   画面の切り替えはサーバー側だけで行う（JSPは何もしない）。
		if (action.equals("edit") && id != null) {
			Slip s = dao.findById(id);
			if (s == null) { response.sendRedirect(listUrl); return null; }
			return buildData(request, "edit", s.getId(), s.getDate(), s.getPartnerName(),
					s.getDescription(), s.getNote(), s.getEntries(), null);
		}

		// ---- 3. 行を追加：入力途中の内容を保ったまま、末尾に空行を1つ足す（POST action=addRow）----
		if (action.equals("addRow")) {
			ArrayList<Entry> entries = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
			entries.add(new Entry());
			return buildData(request, id == null ? "new" : "edit", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), entries, null);
		}

		// ---- 4. 登録ボタン：入力チェックして、OKなら確認画面へ（POST action=confirm）----
		if (action.equals("confirm")) {
			ArrayList<String> errors = new ArrayList<>();
			// buildEntries が「入力チェック＋空行を詰めた明細」を作る
			ArrayList<Entry> entries = dao.buildEntries(fDate, fPartner, fDesc,
					dSub, dAmt, cSub, cAmt, errors);
			if (!errors.isEmpty()) {
				// ミスあり → 打ち込んだ内容そのままで入力画面へ戻す（エラーはポップアップで表示）
				ArrayList<Entry> raw = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
				return buildData(request, id == null ? "new" : "edit", id,
						nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), raw, errors);
			}
			// OK → 「この内容で登録しますか？」の確認画面（詰めた明細を見せる）
			return buildData(request, "confirm", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), entries, null);
		}

		// ---- 5. 登録する：確認画面から（POST action=save）→ DBへ保存して閲覧画面へ ----
		if (action.equals("save")) {
			ArrayList<String> errors = new ArrayList<>();
			int savedId = dao.saveFromForm(id, fDate, fPartner, fDesc, fNote,
					dSub, dAmt, cSub, cAmt, errors);
			if (savedId > 0) {
				response.sendRedirect(detailUrl(savedId, q, no, sortKey, order, page));
				return null;
			}
			// まず起きないが、失敗したら入力画面へ戻す
			ArrayList<Entry> raw = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
			return buildData(request, id == null ? "new" : "edit", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), raw, errors);
		}

		// ---- 6. キャンセルして修正：確認画面から入力画面へ戻る（POST action=back）----
		if (action.equals("back")) {
			ArrayList<Entry> entries = dao.rebuildRows(dSub, dAmt, cSub, cAmt);
			return buildData(request, id == null ? "new" : "edit", id,
					nz(fDate), nz(fPartner), nz(fDesc), nz(fNote), entries, null);
		}

		// ---- 7. 削除：view画面の削除確認から（GET action=delete）----
		if (action.equals("delete")) {
			if (id != null) dao.delete(id);
			response.sendRedirect(listUrl);
			return null;
		}

		// 想定外の合図 → 安全のため一覧へ
		response.sendRedirect(listUrl);
		return null;
	}

	// ========================================================================
	// ここから下：画面に出す「HTML部品」を組み立てて HashMap に詰めるメソッド群
	//   （detail.jsp はこの部品を <%= %> で並べるだけ。判断はすべてこちらで済ませる）
	// ========================================================================

	// ===== 【buildData】mode（画面の種類）と値から、表示部品を全部作って HashMap に入れるメソッド =====
	private static HashMap<String, Object> buildData(HttpServletRequest request,
			String mode, Integer slipId, String dateRaw, String partner, String desc, String note,
			ArrayList<Entry> entries, ArrayList<String> errors) {

		boolean isView    = "view".equals(mode);
		boolean isNew     = "new".equals(mode);
		boolean isEdit    = "edit".equals(mode);
		boolean isConfirm = "confirm".equals(mode);
		boolean editable  = isNew || isEdit;      // 入力欄を出す画面か
		boolean readOnly  = isView || isConfirm;  // 読み取り専用（¥やカンマ付きで見せる）画面か

		// 一覧の検索条件・ページ（hiddenやリンクに引き継ぐため取り直す）
		String q = nz(request.getParameter("q"));
		String no = nz(request.getParameter("no"));
		String sortKey = nz(request.getParameter("sortKey"));
		String order = nz(request.getParameter("order"));
		String page = nz(request.getParameter("page"));
		String listUrl = listUrl(q, no, sortKey, order, page);
		String stateQuery = stateQuery(q, no, sortKey, order, page);

		// 合計金額（明細を1行ずつ足す）。¥・カンマはサーバー側で付ける（JS不要）
		int debitTotal = 0, creditTotal = 0;
		for (Entry e : entries) {
			debitTotal += e.getDebitValue();
			creditTotal += e.getCreditValue();
		}

		HashMap<String, Object> data = new HashMap<>();

		// (a) form の開始/終了（入力・確認のときだけ form で囲む。閲覧は form 無し）
		boolean useForm = editable || isConfirm;
		data.put("formOpen", useForm ? "<form action=\"detail.jsp\" method=\"POST\" id=\"slip-form\">" : "");
		data.put("formClose", useForm ? "</form>" : "");

		// (b) hidden 群（検索条件・ページを持ち回る。確認画面では入力値もhiddenで運ぶ）
		StringBuilder h = new StringBuilder();
		if (slipId != null) h.append(hidden("id", String.valueOf(slipId)));
		h.append(hidden("q", q)).append(hidden("no", no)).append(hidden("sortKey", sortKey))
		 .append(hidden("order", order)).append(hidden("page", page));
		if (isConfirm) {
			// 確認画面は見た目が読み取り専用なので、送信する値は hidden で持つ
			h.append(hidden("slipDate", dateRaw)).append(hidden("partnerName", partner))
			 .append(hidden("description", desc)).append(hidden("note", note));
			for (Entry e : entries) {
				h.append(hidden("debitSubject", e.getDebitSubject()));
				h.append(hidden("debitAmount", e.getDebitAmount()));   // 数字だけ（カンマ無し）
				h.append(hidden("creditSubject", e.getCreditSubject()));
				h.append(hidden("creditAmount", e.getCreditAmount()));
			}
		}
		data.put("hiddenInputs", useForm ? h.toString() : "");

		// (c) 「←伝票一覧」リンク（閲覧はそのまま戻る。入力/確認は破棄確認ポップアップへ）
		data.put("backLink", isView
				? "<a href=\"" + listUrl + "\" class=\"btn-back\">←伝票一覧</a>"
				: "<a href=\"#leave-popup\" class=\"btn-back\">←伝票一覧</a>");

		// (d) 見出しの各項目：入力できる画面は input、読み取り画面は span（文字）
		data.put("dateField", editable
				? "<input type=\"date\" name=\"slipDate\" class=\"header-input\" value=\"" + SlipDao.esc(dateRaw) + "\">"
				: "<span class=\"underline-text\">" + SlipDao.esc(SlipDao.slash(dateRaw)) + "</span>");
		data.put("idField", (!isNew && slipId != null)
				? "<span class=\"underline-text\">" + slipId + "</span>"
				: "<span class=\"underline-text\">-</span>");
		data.put("partnerField", editable
				? "<input type=\"text\" name=\"partnerName\" class=\"header-input partner-input\" value=\"" + SlipDao.esc(partner) + "\">"
				: "<span class=\"underline-text partner-name\">" + SlipDao.esc(partner) + "</span>");
		data.put("descField", editable
				? "<input type=\"text\" name=\"description\" class=\"header-input description-input\" value=\"" + SlipDao.esc(desc) + "\">"
				: "<span class=\"underline-text description-text\">" + SlipDao.esc(desc) + "</span>");

		// (e) ボタン群（画面ごとに出すボタンを変える）
		String buttons;
		if (isView) {
			buttons = "<a href=\"detail.jsp?action=edit&id=" + slipId + stateQuery + "\" class=\"btn btn-edit\">編集</a>"
					+ "<a href=\"#delete-popup\" class=\"btn btn-delete\">削除</a>";
		} else if (isConfirm) {
			buttons = "<button type=\"submit\" name=\"action\" value=\"save\" class=\"btn btn-save\">登録する</button>"
					+ "<button type=\"submit\" name=\"action\" value=\"back\" class=\"btn\">修正する</button>";
		} else { // 入力（新規/編集）
			buttons = "<button type=\"submit\" name=\"action\" value=\"confirm\" class=\"btn btn-save\">登録</button>";
		}
		data.put("buttons", buttons);

		// (f) 明細の行（入力画面は入力欄、読み取り画面は¥＋カンマ付きの文字）
		StringBuilder rows = new StringBuilder();
		for (Entry e : entries) {
			rows.append("<tr class=\"entry-row\">");
			rows.append(cellSubject(editable, "debitSubject", e.getDebitSubject()));
			rows.append(cellAmount(editable, "debitAmount", e.getDebitAmount(), e.getDebitValue()));
			rows.append(cellSubject(editable, "creditSubject", e.getCreditSubject()));
			rows.append(cellAmount(editable, "creditAmount", e.getCreditAmount(), e.getCreditValue()));
			rows.append("</tr>");
		}
		data.put("entriesRows", rows.toString());
		// 合計は常に¥＋カンマ付き（サーバー側で整形）
		data.put("debitTotalText", SlipDao.yen(debitTotal));
		data.put("creditTotalText", SlipDao.yen(creditTotal));

		// (g) 「＋ 行を追加」ボタン（入力画面だけ。押すとサーバーで空行を1つ足す）
		data.put("addRowButton", editable
				? "<div class=\"add-row-container\"><button type=\"submit\" name=\"action\" value=\"addRow\" class=\"btn-add-row\">＋ 行を追加</button></div>"
				: "");

		// (h) 備考欄（入力画面は書き込み可、読み取り画面は readonly）
		data.put("noteField", editable
				? "<textarea id=\"note-text-edit\" name=\"note\" class=\"note-textarea edit-mode\" rows=\"3\">" + SlipDao.esc(note) + "</textarea>"
				: "<textarea id=\"note-text\" class=\"note-textarea view-mode\" rows=\"3\" readonly>" + SlipDao.esc(note) + "</textarea>");

		// (i) 確認画面の案内文
		data.put("confirmNotice", isConfirm
				? "<p class=\"confirm-notice\">この内容で登録します。よろしければ「登録する」を押してください。</p>"
				: "");

		// (j) ポップアップ（削除確認・破棄確認・入力エラー）をまとめて作る
		data.put("popups", buildPopups(isView, slipId, listUrl, stateQuery, errors));

		// (k) 画面全体のCSS切替クラス
		data.put("pageMod", isNew ? " is-new" : (isEdit ? " is-edit" : ""));
		return data;
	}

	// ===== 【cellSubject】勘定科目のセルを作る（入力欄 or 文字）=====
	private static String cellSubject(boolean editable, String name, String subject) {
		if (editable) {
			return "<td class=\"col-subject\"><input type=\"text\" name=\"" + name
					+ "\" class=\"input-field\" value=\"" + SlipDao.esc(subject) + "\"></td>";
		}
		return "<td class=\"col-subject\"><span class=\"view-mode\">" + SlipDao.esc(subject) + "</span></td>";
	}

	// ===== 【cellAmount】金額のセルを作る =====
	//   入力欄 … 数字だけ（カンマ無し）を表示 ／ 読み取り … ¥＋3桁カンマ付きで表示（どちらもサーバー側）
	private static String cellAmount(boolean editable, String name, String rawDigits, int value) {
		if (editable) {
			return "<td class=\"col-amount\"><input type=\"text\" name=\"" + name
					+ "\" class=\"input-field text-right\" value=\"" + SlipDao.esc(rawDigits) + "\"></td>";
		}
		return "<td class=\"col-amount\"><span class=\"view-mode\">" + SlipDao.yenOrBlank(value) + "</span></td>";
	}

	// ===== 【buildPopups】3つのポップアップのHTMLを、必要なものだけ作って返すメソッド =====
	//   ・削除確認（閲覧のみ・CSSの:targetで開く）
	//   ・破棄確認（入力/確認・CSSの:targetで開く）
	//   ・入力エラー（ミスがあるとき・チェックボックス方式で自動表示。すべてJS不要）
	private static String buildPopups(boolean isView, Integer slipId, String listUrl,
			String stateQuery, ArrayList<String> errors) {
		StringBuilder p = new StringBuilder();

		// 削除確認（閲覧モードのみ）
		if (isView && slipId != null) {
			String back = "detail.jsp?id=" + slipId + stateQuery;
			p.append("<div id=\"delete-popup\" class=\"popup-overlay delete-layout\">")
			 .append("<div class=\"delete-popup-box\">")
			 .append("<a href=\"").append(back).append("\" class=\"popup-close delete-popup-close\">❌</a>")
			 .append("<p class=\"popup-title\">この伝票を<span class=\"text-danger\">削除</span>しますか？</p>")
			 .append("<div class=\"popup-btn-group\">")
			 .append("<a href=\"detail.jsp?action=delete&id=").append(slipId).append(stateQuery).append("\" class=\"popup-btn btn-yes\">はい</a>")
			 .append("<a href=\"").append(back).append("\" class=\"popup-btn\">いいえ</a>")
			 .append("</div></div></div>");
		}

		// 破棄確認（入力/確認モード）
		if (!isView) {
			p.append("<div id=\"leave-popup\" class=\"popup-overlay leave-popup-layout\">")
			 .append("<div class=\"leave-popup-box\">")
			 .append("<a href=\"#\" class=\"popup-close\">❌</a>")
			 .append("<div class=\"leave-massage-container\">")
			 .append("<p class=\"leave-text\">入力中の内容は保存されません。</p>")
			 .append("<p class=\"leave-text\">一覧へ戻ってよろしいですか？</p></div>")
			 .append("<div class=\"popup-btn-group\">")
			 .append("<a href=\"").append(listUrl).append("\" class=\"popup-btn btn-ok-link\">はい</a>")
			 .append("<a href=\"#\" class=\"popup-btn\">いいえ</a>")
			 .append("</div></div></div>");
		}

		// 入力エラー（ミスがあるときだけ。チェックボックスのON/OFFで開閉＝JS不要）
		if (errors != null && !errors.isEmpty()) {
			// checked を付けておくと、読み込んだ瞬間からポップアップが開いた状態になる
			p.append("<input type=\"checkbox\" id=\"err-toggle\" class=\"err-toggle\" checked>");
			p.append("<div class=\"popup-overlay err-popup\"><div class=\"error-popup-box\">");
			p.append("<label for=\"err-toggle\" class=\"popup-close error-close\">❌</label>");
			p.append("<p class=\"popup-title\">入力内容を確認してください</p>");
			p.append("<div class=\"error-mennage-container\">");
			for (String er : errors) {
				p.append("<p class=\"error-text\">").append(SlipDao.esc(er)).append("</p>");
			}
			p.append("</div><div class=\"popup-btn-group\" style=\"margin-top:20px;\">");
			p.append("<label for=\"err-toggle\" class=\"popup-btn\">閉じて修正する</label>");
			p.append("</div></div></div>");
		}
		return p.toString();
	}

	// ===== 【hidden】<input type="hidden"> を1つ作る小道具（値は空でも作る）=====
	private static String hidden(String name, String value) {
		return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + SlipDao.esc(value) + "\">";
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
		add(sb, "&", "q", q);
		add(sb, "&", "no", no);
		add(sb, "&", "sortKey", sortKey);
		add(sb, "&", "order", order);
		add(sb, "&", "page", page);
		return sb.toString();
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
