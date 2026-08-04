<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.Slip, otameshirenshuu.Slip.Entry, otameshirenshuu.SlipDao, otameshirenshuu.DetailList" %>
<%
	/* ========================================================================
	 * 【明細画面（表示だけ担当）】
	 *   処理・入力チェック・確認はすべて DetailList.detail(...) が行う。
	 *   このJSPは、返ってきた HashMap を読んで画面を組み立てるだけ。
	 *   JavaScript は「金額の3桁カンマ」だけに使い、それ以外は使わない。
	 * ====================================================================== */

	HashMap<String, Object> data = DetailList.detail(request, response);
	if (data == null) return; // 削除や保存成功で別URLへ飛ばしたときは表示しない

	// --- HashMap から値を取り出す ---
	boolean isView    = (Boolean) data.get("isView");    // 閲覧（読み取り専用）
	boolean isNew     = (Boolean) data.get("isNew");     // 新規入力
	boolean isEdit    = (Boolean) data.get("isEdit");    // 修正入力
	boolean isConfirm = (Boolean) data.get("isConfirm"); // 登録前の確認
	boolean isInput   = isNew || isEdit;                 // 入力できる画面か
	boolean readOnly  = isView || isConfirm;             // 表示だけの画面か（同じ見た目）

	Integer slipId = (Integer) data.get("slipId");       // 新規のときは null
	String dateRaw = (String) data.get("dateRaw");
	String partner = (String) data.get("partner");
	String desc    = (String) data.get("desc");
	String note    = (String) data.get("note");
	ArrayList<Entry> entries  = (ArrayList<Entry>) data.get("entries");
	ArrayList<String> errors  = (ArrayList<String>) data.get("errors"); // 無ければ null
	int debitTotal  = (Integer) data.get("debitTotal");
	int creditTotal = (Integer) data.get("creditTotal");
	String pageMod  = (String) data.get("pageMod");

	// 一覧の検索条件・ページ（画面をまたいで保持する）
	String q       = (String) data.get("q");
	String no      = (String) data.get("no");
	String sortKey = (String) data.get("sortKey");
	String order   = (String) data.get("order");
	String pageParam = (String) data.get("page");
	String listUrl    = (String) data.get("listUrl");    // 「←伝票一覧」の戻り先
	String stateQuery = (String) data.get("stateQuery"); // 編集/削除リンクに付ける「&q=…」
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>振替伝票明細</title>
    <link rel="stylesheet" href="detail-style.css">
</head>
<body>

    <div class="page-wrapper<%= pageMod %>">

        <!-- 入力エラーがあれば画面上部に一覧表示（ポップアップは使わない）-->
        <% if (errors != null && !errors.isEmpty()) { %>
        <div class="error-banner">
            入力内容を確認してください：
            <ul>
                <% for (String er : errors) { %>
                <li><%= SlipDao.esc(er) %></li>
                <% } %>
            </ul>
        </div>
        <% } %>

        <!-- 確認モードの案内 -->
        <% if (isConfirm) { %>
        <p style="background:#eef6ff; border:1px solid #b6d4f2; color:#1b4f72; padding:12px 16px; border-radius:4px; margin:0 0 16px;">
            この内容で登録します。よろしければ「登録する」を押してください。
        </p>
        <% } %>

        <!-- 入力(新規/編集)・確認のときは、全体を1つの form で囲む（送信先は detail.jsp の POST）-->
        <% if (!isView) { %>
        <form action="detail.jsp" method="POST" id="slip-form">
            <% if (slipId != null) { %><input type="hidden" name="id" value="<%= slipId %>"><% } %>
            <!-- 検索条件・ページを input hidden で持ち回る -->
            <input type="hidden" name="q"       value="<%= SlipDao.esc(q) %>">
            <input type="hidden" name="no"      value="<%= SlipDao.esc(no) %>">
            <input type="hidden" name="sortKey" value="<%= SlipDao.esc(sortKey) %>">
            <input type="hidden" name="order"   value="<%= SlipDao.esc(order) %>">
            <input type="hidden" name="page"    value="<%= SlipDao.esc(pageParam) %>">
            <% if (isConfirm) { %>
            <!-- 確認画面は読み取り専用表示なので、実際に送る値は hidden で持つ -->
            <input type="hidden" name="slipDate"    value="<%= SlipDao.esc(dateRaw) %>">
            <input type="hidden" name="partnerName" value="<%= SlipDao.esc(partner) %>">
            <input type="hidden" name="description" value="<%= SlipDao.esc(desc) %>">
            <input type="hidden" name="note"        value="<%= SlipDao.esc(note) %>">
            <% for (Entry e : entries) { %>
            <input type="hidden" name="debitSubject"  value="<%= SlipDao.esc(e.getDebitSubject()) %>">
            <input type="hidden" name="debitAmount"   value="<%= SlipDao.esc(e.getDebitAmount()) %>">
            <input type="hidden" name="creditSubject" value="<%= SlipDao.esc(e.getCreditSubject()) %>">
            <input type="hidden" name="creditAmount"  value="<%= SlipDao.esc(e.getCreditAmount()) %>">
            <% } %>
            <% } %>
        <% } %>

        <div class="header-container">
            <div class="back-actions-group">
                <% if (isView) { %>
                    <a href="<%= listUrl %>" class="btn-back">←伝票一覧</a>
                <% } else { %>
                    <a href="#leave-popup" class="btn-back">←伝票一覧</a>
                <% } %>

                <div class="meta-info-group">
                    <div class="header-item item-date">
                        日付
                        <% if (readOnly) { %>
                            <span class="underline-text"><%= SlipDao.slash(dateRaw) %></span>
                        <% } else { %>
                            <input type="date" name="slipDate" class="header-input" value="<%= SlipDao.esc(dateRaw) %>">
                        <% } %>
                    </div>
                    <div class="header-item item-id">
                        伝票番号
                        <% if (!isNew && slipId != null) { %>
                            <span class="underline-text"><%= slipId %></span>
                        <% } else { %>
                            <span class="underline-text">-</span>
                        <% } %>
                    </div>
                </div>
            </div>

            <div class="header-item item-partner">
                取引先
                <% if (readOnly) { %>
                    <span class="underline-text partner-name"><%= SlipDao.esc(partner) %></span>
                <% } else { %>
                    <input type="text" name="partnerName" class="header-input partner-input" value="<%= SlipDao.esc(partner) %>">
                <% } %>
            </div>

            <div class="header-item item-description">
                購入物
                <% if (readOnly) { %>
                    <span class="underline-text description-text"><%= SlipDao.esc(desc) %></span>
                <% } else { %>
                    <input type="text" name="description" class="header-input description-input" value="<%= SlipDao.esc(desc) %>">
                <% } %>
            </div>

            <div class="button-group">
                <% if (isView) { %>
                    <a href="detail.jsp?action=edit&id=<%= slipId %><%= stateQuery %>" class="btn btn-edit">編集</a>
                    <a href="#delete-popup" class="btn btn-delete">削除</a>
                <% } else if (isConfirm) { %>
                    <button type="submit" name="action" value="save" class="btn btn-save">登録する</button>
                    <button type="submit" name="action" value="back" class="btn">修正する</button>
                <% } else { %>
                    <button type="submit" name="action" value="confirm" class="btn btn-save">登録</button>
                <% } %>
            </div>
        </div>

        <table class="journal-table">
            <thead>
                <tr>
                    <th class="col-subject">借方勘定科目</th>
                    <th class="col-amount">金額</th>
                    <th class="col-subject">貸方勘定科目</th>
                    <th class="col-amount">金額</th>
                </tr>
            </thead>
            <tbody id="entry-tbody">
                <% for (Entry e : entries) { %>
                <tr class="entry-row">
                    <td class="col-subject">
                        <% if (readOnly) { %>
                            <span class="view-mode"><%= SlipDao.esc(e.getDebitSubject()) %></span>
                        <% } else { %>
                            <input type="text" name="debitSubject" class="input-field" value="<%= SlipDao.esc(e.getDebitSubject()) %>">
                        <% } %>
                    </td>
                    <td class="col-amount">
                        <% if (readOnly) { %>
                            <span class="view-mode"><%= SlipDao.yenOrBlank(e.getDebitValue()) %></span>
                        <% } else { %>
                            <input type="text" name="debitAmount" class="input-field text-right amount-input" value="<%= SlipDao.amount(e.getDebitValue()) %>">
                        <% } %>
                    </td>
                    <td class="col-subject">
                        <% if (readOnly) { %>
                            <span class="view-mode"><%= SlipDao.esc(e.getCreditSubject()) %></span>
                        <% } else { %>
                            <input type="text" name="creditSubject" class="input-field" value="<%= SlipDao.esc(e.getCreditSubject()) %>">
                        <% } %>
                    </td>
                    <td class="col-amount">
                        <% if (readOnly) { %>
                            <span class="view-mode"><%= SlipDao.yenOrBlank(e.getCreditValue()) %></span>
                        <% } else { %>
                            <input type="text" name="creditAmount" class="input-field text-right amount-input" value="<%= SlipDao.amount(e.getCreditValue()) %>">
                        <% } %>
                    </td>
                </tr>
                <% } %>

                <tr class="total-row" id="total-row">
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
                        <span id="debit-total" class="<%= isInput ? "edit-mode" : "view-mode" %>"><%= SlipDao.yen(debitTotal) %></span>
                    </td>
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
                        <span id="credit-total" class="<%= isInput ? "edit-mode" : "view-mode" %>"><%= SlipDao.yen(creditTotal) %></span>
                    </td>
                </tr>
            </tbody>
        </table>

        <!-- 行を追加（入力画面だけ）。押すとサーバーで空行を1つ足して再表示する -->
        <% if (isInput) { %>
        <div class="add-row-container">
            <button type="submit" name="action" value="addRow" class="btn-add-row">＋ 行を追加</button>
        </div>
        <% } %>

        <div class="note-container">
            <label for="note-text" class="note-label">備考：</label>
            <% if (readOnly) { %>
                <textarea id="note-text" class="note-textarea view-mode" rows="3" readonly><%= SlipDao.esc(note) %></textarea>
            <% } else { %>
                <textarea id="note-text-edit" name="note" class="note-textarea edit-mode" rows="3"><%= SlipDao.esc(note) %></textarea>
            <% } %>
        </div>

        <% if (!isView) { %>
        </form>
        <% } %>
    </div>

    <!-- 削除確認ポップアップ（閲覧モードのみ。CSSの :target で開閉、JS不要）-->
    <% if (isView && slipId != null) { %>
    <div id="delete-popup" class="popup-overlay delete-layout">
        <div class="delete-popup-box">
            <a href="detail.jsp?id=<%= slipId %><%= stateQuery %>" class="popup-close delete-popup-close">❌</a>
            <p class="popup-title">この伝票を<span class="text-danger">削除</span>しますか？</p>
            <div class="popup-btn-group">
                <a href="detail.jsp?action=delete&id=<%= slipId %><%= stateQuery %>" class="popup-btn btn-yes">はい</a>
                <a href="detail.jsp?id=<%= slipId %><%= stateQuery %>" class="popup-btn">いいえ</a>
            </div>
        </div>
    </div>
    <% } %>

    <!-- 「←伝票一覧」を押したときの確認ポップアップ（入力/確認モード。CSSの :target で開閉）-->
    <% if (!isView) { %>
    <div id="leave-popup" class="popup-overlay leave-popup-layout">
        <div class="leave-popup-box">
            <a href="#" class="popup-close">❌</a>
            <div class="leave-massage-container">
                <p class="leave-text">入力中の内容は保存されません。</p>
                <p class="leave-text">一覧へ戻ってよろしいですか？</p>
            </div>
            <div class="popup-btn-group">
                <a href="<%= listUrl %>" class="popup-btn btn-ok-link">はい</a>
                <a href="#" class="popup-btn">いいえ</a>
            </div>
        </div>
    </div>
    <% } %>

<% if (isInput) { %>
<script>
/* 金額欄だけ、入力するたびに3桁ごとのカンマを付ける（見た目だけ。
   送信された値はサーバー側で数字だけ取り出すのでカンマがあってもOK）。
   ※ detail.jsp で使う JavaScript はこれだけ。 */
document.addEventListener('input', function (e) {
	if (e.target && e.target.classList.contains('amount-input')) {
		var digits = e.target.value.replace(/[^0-9]/g, '');      // 数字以外を消す
		e.target.value = (digits === '') ? '' : Number(digits).toLocaleString('en-US'); // 3桁区切り
	}
});
</script>
<% } %>

</body>
</html>
