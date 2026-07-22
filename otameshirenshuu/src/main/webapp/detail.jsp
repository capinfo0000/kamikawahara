<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.Slip, otameshirenshuu.Slip.Entry, otameshirenshuu.SlipDao" %>
<%
	/* ========================================================================
	 * 【明細画面】サーブレットは使わず、このJSPの中でJava(SlipDao)を呼んで処理する。
	 *   mode=view … 閲覧、 mode=edit … 編集、 mode=new … 新規
	 *   POST送信   … 登録／更新、 action=delete … 削除
	 * 画面の下半分（HTML）は、ここで用意した変数を並べて表示するだけ。
	 * ====================================================================== */
	request.setCharacterEncoding("UTF-8"); // 日本語の受け取り用

	SlipDao dao = new SlipDao();

	// 表示に使う変数（このあとの処理で中身を決める）
	String mode = request.getParameter("mode");
	Integer slipId = null;
	String dateRaw = "", partner = "", desc = "", note = "";
	List<Entry> entries = new ArrayList<>();
	List<String> errors = null;

	Integer idParam = null;
	try { idParam = Integer.valueOf(request.getParameter("id")); } catch (Exception e) {}

	// (A) 削除：detail.jsp?action=delete&id=... のとき
	if ("delete".equals(request.getParameter("action"))) {
		if (idParam != null) dao.delete(idParam);
		response.sendRedirect("index.jsp");
		return;
	}

	// (B) 登録・更新：フォームがPOSTで送られてきたとき
	if ("POST".equalsIgnoreCase(request.getMethod())) {
		errors = new ArrayList<>();
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
			// 成功 → 登録した伝票の閲覧画面へ
			response.sendRedirect("detail.jsp?mode=view&id=" + savedId);
			return;
		}
		// 失敗 → 入力内容を保持したまま編集画面を表示（errorsにメッセージ入り）
		mode = "edit";
		slipId = idParam;
		dateRaw = nz(request.getParameter("slipDate"));
		partner = nz(request.getParameter("partnerName"));
		desc    = nz(request.getParameter("description"));
		note    = nz(request.getParameter("note"));
		entries = dao.rebuildRows(
				request.getParameterValues("debitSubject"),
				request.getParameterValues("debitAmount"),
				request.getParameterValues("creditSubject"),
				request.getParameterValues("creditAmount"));
	}
	// (C) 表示のための読み込み（削除でもPOSTエラーでもないとき）
	else if ("new".equals(mode)) {
		// 新規：空の明細を5行用意
		for (int i = 0; i < 5; i++) entries.add(new Entry());
	} else {
		// view / edit：既存の伝票を読み込む
		if (!"edit".equals(mode)) mode = "view";
		Slip s = (idParam != null) ? dao.findById(idParam) : null;
		if (s == null) { response.sendRedirect("index.jsp"); return; }
		slipId  = s.getId();
		dateRaw = s.getDate();
		partner = s.getPartnerName();
		desc    = s.getDescription();
		note    = s.getNote();
		entries = s.getEntries();
	}

	boolean isView = "view".equals(mode);
	boolean isNew  = "new".equals(mode);
	boolean isEdit = "edit".equals(mode);

	// 合計金額（表示用に集計）
	int debitTotal = 0, creditTotal = 0;
	for (Entry e : entries) { debitTotal += e.getDebitValue(); creditTotal += e.getCreditValue(); }

	String pageMod = isNew ? " is-new" : (isEdit ? " is-edit" : "");
%>
<%!
	// nz = null なら空文字（スクリプトレットから使う小道具）
	private static String nz(String s) { return s == null ? "" : s; }
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

        <!-- 編集・新規のときは、入力欄全体を1つのformで囲む（送信先は自分自身=detail.jsp のPOST） -->
        <% if (!isView) { %>
        <form action="detail.jsp" method="POST" id="slip-form">
            <% if (isEdit && slipId != null) { %>
            <input type="hidden" name="id" value="<%= slipId %>">
            <% } %>
        <% } %>

        <div class="header-container">
            <div class="back-actions-group">
            	<% if (isView) { %>
					<a href="index.jsp" class="btn-back">←伝票一覧</a>
				<% } else { %>
					<a href="#leave-popup" class="btn-back">←伝票一覧</a>
				<% } %>

            	<div class="meta-info-group">
                	<div class="header-item item-date">
                    	日付
                    	<% if (isView) { %>
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
	            <% if (isView) { %>
	            	<span class="underline-text partner-name"><%= SlipDao.esc(partner) %></span>
	            <% } else { %>
	            	<input type="text" name="partnerName" class="header-input partner-input" value="<%= SlipDao.esc(partner) %>">
	            <% } %>
	        </div>

        	<div class="header-item item-description">
        		購入物
        		<% if (isView) { %>
        			<span class="underline-text description-text"><%= SlipDao.esc(desc) %></span>
        		<% } else { %>
        			<input type="text" name="description" class="header-input description-input" value="<%= SlipDao.esc(desc) %>">
        		<% } %>
        	</div>

	        <div class="button-group">
	        <% if (isView) { %>
	            <a href="detail.jsp?mode=edit&id=<%= slipId %>" class="btn btn-edit">編集</a>
	            <a href="#delete-popup" class="btn btn-delete">削除</a>
	        <% } else { %>
	            <button type="button" class="btn btn-save" onclick="tryRegister()">登録</button>
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
                    <% if (!isView) { %><th class="col-op">操作</th><% } %>
                </tr>
            </thead>
            <tbody id="entry-tbody">
                <% for (Entry e : entries) { %>
                <tr class="entry-row">
                    <td class="col-subject">
                    	<% if (isView) { %>
							<span class="view-mode"><%= SlipDao.esc(e.getDebitSubject()) %></span>
						<% } else { %>
							<input type="text" name="debitSubject" class="input-field" value="<%= SlipDao.esc(e.getDebitSubject()) %>">
						<% } %>
					</td>
                    <td class="col-amount">
                    	<% if (isView) { %>
							<span class="view-mode"><%= SlipDao.yenOrBlank(e.getDebitValue()) %></span>
						<% } else { %>
							<input type="text" name="debitAmount" class="input-field text-right" value="<%= SlipDao.amount(e.getDebitValue()) %>" oninput="recompute()">
						<% } %>
					</td>
                    <td class="col-subject">
                    	<% if (isView) { %>
							<span class="view-mode"><%= SlipDao.esc(e.getCreditSubject()) %></span>
						<% } else { %>
							<input type="text" name="creditSubject" class="input-field" value="<%= SlipDao.esc(e.getCreditSubject()) %>">
						<% } %>
					</td>
                    <td class="col-amount">
                    	<% if (isView) { %>
							<span class="view-mode"><%= SlipDao.yenOrBlank(e.getCreditValue()) %></span>
						<% } else { %>
							<input type="text" name="creditAmount" class="input-field text-right" value="<%= SlipDao.amount(e.getCreditValue()) %>" oninput="recompute()">
						<% } %>
					</td>
					<% if (!isView) { %>
					<td class="col-op">
						<button type="button" class="btn-row-delete" onclick="removeRow(this)" title="この行を削除">×</button>
					</td>
					<% } %>
                </tr>
                <% } %>

                <tr class="total-row" id="total-row">
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
						<span id="debit-total" class="<%= isView ? "view-mode" : "edit-mode" %>"><%= SlipDao.yen(debitTotal) %></span>
					</td>
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
						<span id="credit-total" class="<%= isView ? "view-mode" : "edit-mode" %>"><%= SlipDao.yen(creditTotal) %></span>
					</td>
					<% if (!isView) { %><td class="col-op"></td><% } %>
                </tr>
            </tbody>
        </table>

        <% if (!isView) { %>
        	<div class="add-row-container">
            	<button type="button" class="btn-add-row" onclick="addRow()">＋ 行を追加</button>
        	</div>
        <% } %>

        <div class="note-container">
            <label for="note-text" class="note-label">備考：</label>
            <% if (isView) { %>
            	<textarea id="note-text" class="note-textarea view-mode" rows="3" readonly><%= SlipDao.esc(note) %></textarea>
            <% } else { %>
            	<textarea id="note-text-edit" name="note" class="note-textarea edit-mode" rows="3"><%= SlipDao.esc(note) %></textarea>
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
                <a href="detail.jsp?mode=view&id=<%= slipId %>" class="popup-close delete-popup-close">❌</a>
                <p class="popup-title">この伝票を<span class="text-danger">削除</span>しますか？</p>
                <div class="popup-btn-group">
                    <a href="detail.jsp?action=delete&id=<%= slipId %>" class="popup-btn btn-yes">はい</a>
                    <a href="detail.jsp?mode=view&id=<%= slipId %>" class="popup-btn">いいえ</a>
                </div>
            </div>
        </div>
        <% } %>

		<!-- 入力エラーポップアップ（登録に失敗したとき自動表示） -->
		<% if (errors != null && !errors.isEmpty()) { %>
		<div id="error-popup" class="popup-overlay error-layout" style="display:flex;">
			<div class="error-popup-box">
				<button type="button" class="popup-close error-close" onclick="closeErrorPopup()">❌</button>
				<div class="error-mennage-container">
					<% for (String err : errors) { %>
						<p class="error-text"><%= SlipDao.esc(err) %></p>
					<% } %>
				</div>
				<div class="popup-btn-group" style="margin-top:20px;">
					<button type="button" class="popup-btn" onclick="closeErrorPopup()">閉じて修正する</button>
				</div>
			</div>
		</div>
		<% } %>

		<!-- 編集・新規モードのポップアップ（一覧に戻る確認／登録確認／不一致） -->
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

			<div id="register-popup" class="popup-overlay register-confirm-layout">
				<div class="register-popup-box">
					<button type="button" class="popup-close" onclick="closeRegisterPopup()">❌</button>
					<p class="popup-title">この内容で登録しますか？</p>
					<div class="popup-btn-group">
						<button type="button" class="popup-btn btn-yes" onclick="submitSlip()">はい</button>
						<button type="button" class="popup-btn" onclick="closeRegisterPopup()">いいえ</button>
					</div>
				</div>
			</div>

			<div id="mismatch-popup" class="popup-overlay mismatch-layout">
				<div class="error-popup-box">
					<button type="button" class="popup-close error-close" onclick="closeMismatchPopup()">❌</button>
					<div class="error-mennage-container">
						<p class="error-text">借方と貸方の合計金額が一致しません。</p>
						<p class="error-text">金額を確認してください。</p>
					</div>
					<div class="popup-btn-group" style="margin-top:20px;">
						<button type="button" class="popup-btn" onclick="closeMismatchPopup()">閉じる</button>
					</div>
				</div>
			</div>
		<% } %>

<script>
/* ---- 画面操作用のJavaScript（行の追加・削除、合計のリアルタイム計算、確認ポップアップ）---- */
function closeErrorPopup(){ var p=document.getElementById('error-popup'); if(p) p.style.display='none'; }
function closeRegisterPopup(){ var p=document.getElementById('register-popup'); if(p) p.style.display='none'; }
function closeMismatchPopup(){ var p=document.getElementById('mismatch-popup'); if(p) p.style.display='none'; }

// 入力行を1行つくる
function makeRow(){
	var tr=document.createElement('tr');
	tr.className='entry-row';
	tr.innerHTML =
		'<td class="col-subject"><input type="text" name="debitSubject" class="input-field"></td>' +
		'<td class="col-amount"><input type="text" name="debitAmount" class="input-field text-right" oninput="recompute()"></td>' +
		'<td class="col-subject"><input type="text" name="creditSubject" class="input-field"></td>' +
		'<td class="col-amount"><input type="text" name="creditAmount" class="input-field text-right" oninput="recompute()"></td>' +
		'<td class="col-op"><button type="button" class="btn-row-delete" onclick="removeRow(this)" title="この行を削除">×</button></td>';
	return tr;
}
function addRow(){ document.getElementById('entry-tbody').insertBefore(makeRow(), document.getElementById('total-row')); }
function removeRow(btn){ var tr=btn.parentNode.parentNode; tr.parentNode.removeChild(tr); recompute(); }

function toNum(v){ v=(v||'').replace(/[^0-9]/g,''); return v===''?0:parseInt(v,10); }
// 借方・貸方の合計を計算して合計欄を更新
function recompute(){
	var rows=document.querySelectorAll('#entry-tbody .entry-row');
	var d=0,c=0;
	rows.forEach(function(r){ var i=r.querySelectorAll('input'); if(i.length>=4){ d+=toNum(i[1].value); c+=toNum(i[3].value); } });
	var de=document.getElementById('debit-total'), ce=document.getElementById('credit-total');
	if(de) de.textContent='¥'+d.toLocaleString();
	if(ce) ce.textContent='¥'+c.toLocaleString();
	return {debit:d, credit:c};
}
// 「登録」ボタン：貸借一致を確認 → 一致なら確認ポップアップ、違えば不一致ポップアップ
function tryRegister(){
	var t=recompute();
	if(t.debit!==t.credit){ document.getElementById('mismatch-popup').style.display='flex'; return; }
	document.getElementById('register-popup').style.display='flex';
}
function submitSlip(){ document.getElementById('slip-form').submit(); }
</script>
</body>
</html>
