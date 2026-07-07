<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.Entry" %>
<%!
	// 数字文字列を3桁区切りに整形（空なら空文字）
	private String fmt(String v) {
		if (v == null) return "";
		String digits = v.replaceAll("[^0-9]", "");
		if (digits.isEmpty()) return "";
		return String.format("%,d", Long.parseLong(digits));
	}
	// 金額が空のときは ¥ を付けない（閲覧モード用）
	private String yen(String v) {
		String f = fmt(v);
		return f.isEmpty() ? "" : "&yen;" + f;
	}
	private String esc(String v) {
		if (v == null) return "";
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
%>
<%
	String currentMode = (String) request.getAttribute("currentMode");
	if (currentMode == null) currentMode = "view";
	boolean isView = "view".equals(currentMode);
	boolean isNew  = "new".equals(currentMode);
	boolean isEdit = "edit".equals(currentMode);

	Integer slipId = (Integer) request.getAttribute("slipId");

	String slipDate = (String) request.getAttribute("slipDate");
	if (slipDate == null) slipDate = "";
	String slipDateSlash = slipDate.replace("-", "/");

	String partnerName = (String) request.getAttribute("partnerName");
	if (partnerName == null) partnerName = "";

	String description = (String) request.getAttribute("description");
	if (description == null) description = "";

	String note = (String) request.getAttribute("note");
	if (note == null) note = "";

	List<Entry> detailList = (List<Entry>) request.getAttribute("detailList");
	if (detailList == null) detailList = new ArrayList<>();

	List<String> errors = (List<String>) request.getAttribute("errors");

	String pageMod = isNew ? " is-new" : (isEdit ? " is-edit" : "");
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>振替伝票明細</title>
    <!-- CSSファイルの読み込み -->
    <link rel="stylesheet" href="detail-style.css">
</head>
<body>

    <div class="page-wrapper<%= pageMod %>">

        <!-- 編集・新規モードでは、ヘッダー・明細・備考をすべて1つのformで囲む
             （divの途中でformを開くとブラウザがformを早期に閉じ、明細の入力欄が
               送信されなくなるため、page-wrapper直下でformを開く） -->
        <% if (!isView) { %>
        <form action="detail" method="POST" id="slip-form">
            <% if (isEdit && slipId != null) { %>
            <input type="hidden" name="id" value="<%= slipId %>">
            <% } %>
        <% } %>

        <!-- 上部ヘッダーエリア（基本情報とボタン） -->
        <div class="header-container">

            <!-- 「伝票一覧に戻る」ボタン -->
            <div class="back-actions-group">
            	<% if (isView) { %>
					<a href="index.jsp" class="btn-back">←伝票一覧</a>
				<% } else { %>
					<!-- 編集・新規モードの時は、一覧に戻らずポップアップを開く -->
					<a href="#leave-popup" class="btn-back">←伝票一覧</a>
				<% } %>

            <!-- 日付と伝票番号の縦並びエリア -->
            	<div class="meta-info-group">

					<!-- 日付 -->
                	<div class="header-item item-date">
                    	日付
                    	<% if (isView) { %>
                   	 		<span class="underline-text"><%= esc(slipDateSlash) %></span>
               			<% } else { %>
               	    		<input type="date" name="slipDate" class="header-input" value="<%= esc(slipDate) %>">
               			<% } %>
                    </div>

                    <!-- 伝票番号 -->
                	<div class="header-item item-id">
                    	伝票番号
                    	<% if (!isNew && slipId != null) { %>
                    		<span class="underline-text"><%= slipId %></span>
                    	<% } else { %>
                    		<!-- 新規登録は採番前なのでハイフン -->
                    		<span class="underline-text">-</span>
                    	<% } %>
               		</div>
            	</div>
            </div>

            <!-- 取引先 -->
	            <div class="header-item item-partner">
	                取引先
	                <% if (isView) { %>
	                	<span class="underline-text partner-name"><%= esc(partnerName) %></span>
	                <% } else { %>
	                	<input type="text" name="partnerName" class="header-input partner-input" value="<%= esc(partnerName) %>">
	                <% } %>
	            </div>

        <!-- 購入物 -->
        	<div class="header-item item-description">
        		購入物
        		<% if (isView) { %>
        			<span class="underline-text description-text"><%= esc(description) %></span>
        		<% } else { %>
        			<input type="text" name="description" class="header-input description-input" value="<%= esc(description) %>">
        		<% } %>
        	</div>

            <!-- 右上ボタンエリア -->
	            <div class="button-group">
	            <% if (isView) { %>
	                <a href="detail?mode=edit&id=<%= slipId %>" class="btn btn-edit">編集</a>
	                <a href="#delete-popup" class="btn btn-delete">削除</a>
	            <% } else { %>
	                <button type="submit" class="btn btn-save">登録</button>
	            <% } %>
	            </div>
        </div>

        <!-- 振替伝票テーブル -->
        <table class="journal-table">
            <thead>
                <tr>
                    <th class="col-subject">借方勘定科目</th>
                    <th class="col-amount">金額</th>
                    <th class="col-subject">貸方勘定科目</th>
                    <th class="col-amount">金額</th>
                    <% if (!isView) { %><th class="col-op">操作</th><% } %>
                </tr>
            </thead>
            <tbody id="entry-tbody">

            	<%
            		// 登録されている明細の数だけ行を表示し、あわせて合計を集計する
            		int debitTotal = 0;
            		int creditTotal = 0;
            		for (Entry en : detailList) {
            			String dSub = en.getDebitSubject();
            			String dAmt = en.getDebitAmount();
            			String cSub = en.getCreditSubject();
            			String cAmt = en.getCreditAmount();
            			debitTotal  += en.getDebitValue();
            			creditTotal += en.getCreditValue();
            	%>
	                <tr class="entry-row">
						<!-- 借方勘定科目 -->
	                    <td class="col-subject">
	                    	<% if (isView) { %>
								<span class="view-mode"><%= esc(dSub) %></span>
							<% } else { %>
								<input type="text" name="debitSubject" class="input-field" value="<%= esc(dSub) %>">
							<% } %>
						</td>

						<!-- 借方金額 -->
	                    <td class="col-amount">
	                    	<% if (isView) { %>
								<span class="view-mode"><%= yen(dAmt) %></span>
							<% } else { %>
								<input type="text" name="debitAmount" class="input-field text-right" value="<%= fmt(dAmt) %>" oninput="recompute()">
							<% } %>
						</td>

						<!-- 貸方勘定科目 -->
	                    <td class="col-subject">
	                    	<% if (isView) { %>
								<span class="view-mode"><%= esc(cSub) %></span>
							<% } else { %>
								<input type="text" name="creditSubject" class="input-field" value="<%= esc(cSub) %>">
							<% } %>
						</td>

						<!-- 貸方金額 -->
	                    <td class="col-amount">
	                    	<% if (isView) { %>
								<span class="view-mode"><%= yen(cAmt) %></span>
							<% } else { %>
								<input type="text" name="creditAmount" class="input-field text-right" value="<%= fmt(cAmt) %>" oninput="recompute()">
							<% } %>
						</td>

						<!-- 行削除（編集・新規モードのみ） -->
						<% if (!isView) { %>
						<td class="col-op">
							<button type="button" class="btn-row-delete" onclick="removeRow(this)" title="この行を削除">×</button>
						</td>
						<% } %>
	                </tr>
                <% } %>

                <!-- 合計行 -->
                <tr class="total-row" id="total-row">
					<!-- 借方合計金額エリア -->
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
						<span id="debit-total" class="<%= isView ? "view-mode" : "edit-mode" %>">&yen;<%= String.format("%,d", debitTotal) %></span>
					</td>

					<!-- 貸方合計金額エリア -->
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
						<span id="credit-total" class="<%= isView ? "view-mode" : "edit-mode" %>">&yen;<%= String.format("%,d", creditTotal) %></span>
					</td>
					<% if (!isView) { %><td class="col-op"></td><% } %>
                </tr>

            </tbody>
        </table>

        <!-- 行追加ボタンエリア（編集・新規モードのみ） -->
        <% if (!isView) { %>
        	<div class="add-row-container">
            	<button type="button" class="btn-add-row" onclick="addRow()">＋ 行を追加</button>
        	</div>
        <% } %>

         <!-- 備考エリア（自由入力・デフォルト3行） -->
        <div class="note-container">
            <label for="note-text" class="note-label">備考：</label>
            <% if (isView) { %>
            	<textarea id="note-text" class="note-textarea view-mode" rows="3" readonly><%= esc(note) %></textarea>
            <% } else { %>
            	<textarea id="note-text-edit" name="note" class="note-textarea edit-mode" rows="3"><%= esc(note) %></textarea>
            <% } %>
        </div>

        <% if (!isView) { %>
        	</form>
        <% } %>
      </div>

        <!-- 削除確認ポップアップ（閲覧モード） -->
        <% if (isView && slipId != null) { %>
        <div id="delete-popup" class="popup-overlay delete-layout">
            <div class="delete-popup-box">
                <a href="detail?mode=view&id=<%= slipId %>" class="popup-close delete-popup-close">❌</a>
                <p class="popup-title">この伝票を<span class="text-danger">削除</span>しますか？</p>
                <div class="popup-btn-group">
                    <a href="detail?action=delete&id=<%= slipId %>" class="popup-btn btn-yes">はい</a>
                    <a href="detail?mode=view&id=<%= slipId %>" class="popup-btn">いいえ</a>
                </div>
            </div>
        </div>
        <% } %>

		<!-- 入力エラーポップアップ（バリデーションエラー時に自動表示） -->
		<% if (errors != null && !errors.isEmpty()) { %>
		<div id="error-popup" class="popup-overlay error-layout" style="display:flex;">
			<div class="error-popup-box">
				<button type="button" class="popup-close error-close" onclick="closeErrorPopup()">❌</button>
				<div class="error-mennage-container">
					<% for (String err : errors) { %>
						<p class="error-text"><%= esc(err) %></p>
					<% } %>
				</div>
				<div class="popup-btn-group" style="margin-top:20px;">
					<button type="button" class="popup-btn" onclick="closeErrorPopup()">閉じて修正する</button>
				</div>
			</div>
		</div>
		<% } %>

		<!-- 編集破棄確認ポップアップ（編集・新規モード） -->
		<% if (!isView) { %>
			<div id="leave-popup" class="popup-overlay leave-popup-layout">
				<div class="leave-popup-box">
					<a href="#" class="popup-close">❌</a>
					<div class="leave-massage-container">
						<p class="leave-text">編集中の内容は削除されます。</p>
						<p class="leave-text">よろしいですか？</p>
					</div>
					<div class="popup-btn-group">
						<a href="index.jsp" class="popup-btn btn-ok-link">はい</a>
						<a href="#" class="popup-btn">いいえ</a>
					</div>
				</div>
			</div>
		<% } %>

<script>
// エラーポップアップを閉じる（背後の入力内容はそのまま残る）
function closeErrorPopup() {
	var p = document.getElementById('error-popup');
	if (p) p.style.display = 'none';
}

// 入力行を1行作る（編集・新規モードで「行を追加」に使用）
function makeRow() {
	var tr = document.createElement('tr');
	tr.className = 'entry-row';
	tr.innerHTML =
		'<td class="col-subject"><input type="text" name="debitSubject" class="input-field"></td>' +
		'<td class="col-amount"><input type="text" name="debitAmount" class="input-field text-right" oninput="recompute()"></td>' +
		'<td class="col-subject"><input type="text" name="creditSubject" class="input-field"></td>' +
		'<td class="col-amount"><input type="text" name="creditAmount" class="input-field text-right" oninput="recompute()"></td>' +
		'<td class="col-op"><button type="button" class="btn-row-delete" onclick="removeRow(this)" title="この行を削除">×</button></td>';
	return tr;
}

// 行を追加（合計行の直前に挿入）
function addRow() {
	var tbody = document.getElementById('entry-tbody');
	var totalRow = document.getElementById('total-row');
	tbody.insertBefore(makeRow(), totalRow);
}

// 行を削除
function removeRow(btn) {
	var tr = btn.parentNode.parentNode;
	tr.parentNode.removeChild(tr);
	recompute();
}

function toNum(v) {
	v = (v || '').replace(/[^0-9]/g, '');
	return v === '' ? 0 : parseInt(v, 10);
}

// 借方・貸方の合計を再計算して合計行に反映
function recompute() {
	var rows = document.querySelectorAll('#entry-tbody .entry-row');
	var d = 0, c = 0;
	rows.forEach(function (r) {
		var ins = r.querySelectorAll('input');
		if (ins.length >= 4) {
			d += toNum(ins[1].value);
			c += toNum(ins[3].value);
		}
	});
	var de = document.getElementById('debit-total');
	var ce = document.getElementById('credit-total');
	if (de) de.textContent = '¥' + d.toLocaleString();
	if (ce) ce.textContent = '¥' + c.toLocaleString();
}
</script>

</body>
</html>
