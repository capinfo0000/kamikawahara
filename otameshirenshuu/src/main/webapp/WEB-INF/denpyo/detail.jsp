<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%!
	private String esc(Object v) {
		if (v == null) return "";
		return v.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
	// 金額(Integer)を表示用に。0は空欄（¥も出さない）
	private String yen(Object v) {
		int n = (v instanceof Integer) ? (Integer) v : 0;
		return n == 0 ? "" : "&yen;" + String.format("%,d", n);
	}
	// 入力欄の value 用。0は空文字
	private String editv(Object v) {
		int n = (v instanceof Integer) ? (Integer) v : 0;
		return n == 0 ? "" : String.valueOf(n);
	}
	private int intval(Object v) {
		return (v instanceof Integer) ? (Integer) v : 0;
	}
%>
<%
	String ctx = request.getContextPath();
	Map<String,Object> slip = (Map<String,Object>) request.getAttribute("slip");
	String mode = (String) request.getAttribute("mode");
	if (mode == null) mode = "view";
	boolean isView = "view".equals(mode);
	boolean isNew  = "new".equals(mode);
	boolean isEdit = "edit".equals(mode);
	List<String> errors = (List<String>) request.getAttribute("errors");

	Object slipId   = slip.get("id");
	String dateRaw  = esc(slip.get("date"));
	String dateSlash= esc(slip.get("dateSlash"));
	String partner  = esc(slip.get("partnerName"));
	String desc     = esc(slip.get("description"));
	String note     = esc(slip.get("note"));
	List<Map<String,Object>> entries = (List<Map<String,Object>>) slip.get("entries");
	if (entries == null) entries = new ArrayList<>();
	String pageMod = isNew ? " is-new" : (isEdit ? " is-edit" : "");
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>振替伝票明細</title>
    <link rel="stylesheet" href="<%= ctx %>/detail-style.css">
</head>
<body>
    <div class="page-wrapper<%= pageMod %>">

        <% if (!isView) { %>
        <form action="<%= ctx %>/denpyo" method="POST" id="slip-form">
            <input type="hidden" name="action" value="save">
            <% if (isEdit && slipId != null) { %>
            <input type="hidden" name="id" value="<%= slipId %>">
            <% } %>
        <% } %>

        <div class="header-container">
            <div class="back-actions-group">
                <% if (isView) { %>
                    <a href="<%= ctx %>/denpyo" class="btn-back">←伝票一覧</a>
                <% } else { %>
                    <a href="#leave-popup" class="btn-back">←伝票一覧</a>
                <% } %>

                <div class="meta-info-group">
                    <div class="header-item item-date">
                        日付
                        <% if (isView) { %>
                            <span class="underline-text"><%= dateSlash %></span>
                        <% } else { %>
                            <input type="date" name="slipDate" class="header-input" value="<%= dateRaw %>">
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
                    <span class="underline-text partner-name"><%= partner %></span>
                <% } else { %>
                    <input type="text" name="partnerName" class="header-input partner-input" value="<%= partner %>">
                <% } %>
            </div>

            <div class="header-item item-description">
                購入物
                <% if (isView) { %>
                    <span class="underline-text description-text"><%= desc %></span>
                <% } else { %>
                    <input type="text" name="description" class="header-input description-input" value="<%= desc %>">
                <% } %>
            </div>

            <div class="button-group">
                <% if (isView) { %>
                    <a href="<%= ctx %>/denpyo?action=edit&id=<%= slipId %>" class="btn btn-edit">編集</a>
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
                <% for (Map<String,Object> e : entries) { %>
                <tr class="entry-row">
                    <td class="col-subject">
                        <% if (isView) { %>
                            <span class="view-mode"><%= esc(e.get("debitSubject")) %></span>
                        <% } else { %>
                            <input type="text" name="debitSubject" class="input-field" value="<%= esc(e.get("debitSubject")) %>">
                        <% } %>
                    </td>
                    <td class="col-amount">
                        <% if (isView) { %>
                            <span class="view-mode"><%= yen(e.get("debitAmount")) %></span>
                        <% } else { %>
                            <input type="text" name="debitAmount" class="input-field text-right" value="<%= editv(e.get("debitAmount")) %>" oninput="recompute()">
                        <% } %>
                    </td>
                    <td class="col-subject">
                        <% if (isView) { %>
                            <span class="view-mode"><%= esc(e.get("creditSubject")) %></span>
                        <% } else { %>
                            <input type="text" name="creditSubject" class="input-field" value="<%= esc(e.get("creditSubject")) %>">
                        <% } %>
                    </td>
                    <td class="col-amount">
                        <% if (isView) { %>
                            <span class="view-mode"><%= yen(e.get("creditAmount")) %></span>
                        <% } else { %>
                            <input type="text" name="creditAmount" class="input-field text-right" value="<%= editv(e.get("creditAmount")) %>" oninput="recompute()">
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
                        <span id="debit-total" class="<%= isView ? "view-mode" : "edit-mode" %>">&yen;<%= String.format("%,d", intval(slip.get("debitTotal"))) %></span>
                    </td>
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
                        <span id="credit-total" class="<%= isView ? "view-mode" : "edit-mode" %>">&yen;<%= String.format("%,d", intval(slip.get("creditTotal"))) %></span>
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
                <textarea id="note-text" class="note-textarea view-mode" rows="3" readonly><%= note %></textarea>
            <% } else { %>
                <textarea name="note" class="note-textarea edit-mode" rows="3"><%= note %></textarea>
            <% } %>
        </div>

        <% if (!isView) { %>
        </form>
        <% } %>
    </div>

    <!-- 削除確認ポップアップ（閲覧） -->
    <% if (isView && slipId != null) { %>
    <div id="delete-popup" class="popup-overlay delete-layout">
        <div class="delete-popup-box">
            <a href="<%= ctx %>/denpyo?action=view&id=<%= slipId %>" class="popup-close delete-popup-close">❌</a>
            <p class="popup-title">この伝票を<span class="text-danger">削除</span>しますか？</p>
            <div class="popup-btn-group">
                <a href="<%= ctx %>/denpyo?action=delete&id=<%= slipId %>" class="popup-btn btn-yes">はい</a>
                <a href="<%= ctx %>/denpyo?action=view&id=<%= slipId %>" class="popup-btn">いいえ</a>
            </div>
        </div>
    </div>
    <% } %>

    <!-- 編集・新規モードのポップアップ -->
    <% if (!isView) { %>
    <div id="leave-popup" class="popup-overlay leave-popup-layout">
        <div class="leave-popup-box">
            <a href="#" class="popup-close">❌</a>
            <div class="leave-massage-container">
                <p class="leave-text">編集中の内容は削除されます。</p>
                <p class="leave-text">よろしいですか？</p>
            </div>
            <div class="popup-btn-group">
                <a href="<%= ctx %>/denpyo" class="popup-btn btn-ok-link">はい</a>
                <a href="#" class="popup-btn">いいえ</a>
            </div>
        </div>
    </div>

    <!-- 登録確認ポップアップ（文字は黒） -->
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

    <!-- 貸借不一致ポップアップ -->
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

    <!-- サーバー側バリデーションエラー（自動表示） -->
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

<script>
function closeErrorPopup(){ var p=document.getElementById('error-popup'); if(p) p.style.display='none'; }
function closeRegisterPopup(){ var p=document.getElementById('register-popup'); if(p) p.style.display='none'; }
function closeMismatchPopup(){ var p=document.getElementById('mismatch-popup'); if(p) p.style.display='none'; }

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
function addRow(){
    var tbody=document.getElementById('entry-tbody');
    tbody.insertBefore(makeRow(), document.getElementById('total-row'));
}
function removeRow(btn){
    var tr=btn.parentNode.parentNode;
    tr.parentNode.removeChild(tr);
    recompute();
}
function toNum(v){ v=(v||'').replace(/[^0-9]/g,''); return v===''?0:parseInt(v,10); }
function recompute(){
    var rows=document.querySelectorAll('#entry-tbody .entry-row');
    var d=0,c=0;
    rows.forEach(function(r){ var i=r.querySelectorAll('input'); if(i.length>=4){ d+=toNum(i[1].value); c+=toNum(i[3].value); } });
    var de=document.getElementById('debit-total'); var ce=document.getElementById('credit-total');
    if(de) de.textContent='¥'+d.toLocaleString();
    if(ce) ce.textContent='¥'+c.toLocaleString();
    return {debit:d, credit:c};
}
function tryRegister(){
    var t=recompute();
    if(t.debit!==t.credit){ document.getElementById('mismatch-popup').style.display='flex'; return; }
    document.getElementById('register-popup').style.display='flex';
}
function submitSlip(){ document.getElementById('slip-form').submit(); }
</script>
</body>
</html>
