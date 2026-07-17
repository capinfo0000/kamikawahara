<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%!
	// 表示用の小さなヘルパー（Viewでの整形のみ。業務処理はしない）
	private String esc(Object v) {
		if (v == null) return "";
		return v.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
	private String yen(Object v) {
		int n = (v instanceof Integer) ? (Integer) v : 0;
		return "&yen;" + String.format("%,d", n);
	}
%>
<%
	// Controllerが用意した表示用データを受け取るだけ
	String ctx = request.getContextPath();
	List<Map<String,Object>> rows = (List<Map<String,Object>>) request.getAttribute("rows");
	if (rows == null) rows = new ArrayList<>();
	String q         = (String) request.getAttribute("q");
	String no        = (String) request.getAttribute("no");
	String sortKey   = (String) request.getAttribute("sortKey");
	String order     = (String) request.getAttribute("order");
	int pageNo       = (Integer) request.getAttribute("page");
	int totalPages   = (Integer) request.getAttribute("totalPages");
	int total        = (Integer) request.getAttribute("total");
	boolean hasSearch= (Boolean) request.getAttribute("hasSearch");
	String searchParams = (String) request.getAttribute("searchParams");
	String navParams    = (String) request.getAttribute("navParams");
	String idArrow   = (String) request.getAttribute("idArrow");
	String dateArrow = (String) request.getAttribute("dateArrow");
	String idNextOrder   = (String) request.getAttribute("idNextOrder");
	String dateNextOrder = (String) request.getAttribute("dateNextOrder");
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>伝票一覧</title>
    <link rel="stylesheet" href="<%= ctx %>/style.css">
</head>
<body>
    <div class="page-wrapper">
        <h1>伝票一覧</h1>

        <div class="actions-container">
            <form class="search-area" method="get" action="<%= ctx %>/denpyo">
                <input type="text" name="no" value="<%= esc(no) %>" class="no-search-box" placeholder="伝票番号（例: 5 または 1~10）">
                <div class="search-box">
                    <input type="text" name="q" value="<%= esc(q) %>" placeholder="検索（日付、取引先、購入物）">
                </div>
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <button type="submit" class="search-exec-btn">検索</button>
                <% if (hasSearch) { %>
                    <a href="<%= ctx %>/denpyo?sortKey=<%= sortKey %>&order=<%= order %>" class="search-clear-btn" style="margin-left:8px; font-size:13px; color:#337ab7; text-decoration:none;">クリア</a>
                <% } %>
            </form>

            <a href="<%= ctx %>/denpyo?action=new" class="register-btn" style="text-decoration:none; display:inline-block;">新規登録</a>
        </div>

        <% if (hasSearch) { %>
        <p style="font-size:14px; margin:0 0 10px;">検索結果：<%= total %>件<%
            if (no != null && !no.isEmpty()) { %>（伝票番号: <%= esc(no) %>）<% }
            if (q != null && !q.isEmpty()) { %>（キーワード: <%= esc(q) %>）<% }
        %></p>
        <% } %>

        <table class="slip-table">
            <thead>
                <tr>
                    <th class="col-id sort-trigger" onclick="location.href='<%= ctx %>/denpyo?sortKey=id&order=<%= idNextOrder %><%= searchParams %>'">伝票番号<span class="sort-arrows"><%= idArrow %></span></th>
                    <th class="col-date sort-trigger" onclick="location.href='<%= ctx %>/denpyo?sortKey=date&order=<%= dateNextOrder %><%= searchParams %>'">日付<span class="sort-arrows"><%= dateArrow %></span></th>
                    <th class="col-partner">取引先（購入先）</th>
                    <th class="col-description">購入物</th>
                    <th class="col-amount col-amount-th">金額</th>
                    <th class="col-management-header" colspan="2">管理</th>
                </tr>
            </thead>
            <tbody>
                <% if (rows.isEmpty()) { %>
                <tr><td colspan="7">伝票がありません。「新規登録」から追加してください。</td></tr>
                <% } %>
                <% for (Map<String,Object> r : rows) { %>
                <tr>
                    <td class="col-id"><%= r.get("id") %></td>
                    <td class="col-date"><%= esc(r.get("dateSlash")) %></td>
                    <td class="col-partner"><%= esc(r.get("partnerName")) %></td>
                    <td class="col-description"><%= esc(r.get("description")) %></td>
                    <td class="col-amount col-amount-td"><%= yen(r.get("total")) %></td>
                    <td class="col-detail"><a href="<%= ctx %>/denpyo?action=view&id=<%= r.get("id") %>" class="detail-link">明細</a></td>
                    <td class="col-delete-cell">
                        <a href="<%= ctx %>/denpyo?action=delete&id=<%= r.get("id") %>&sortKey=<%= sortKey %>&order=<%= order %>&page=<%= pageNo %><%= searchParams %>"
                           class="btn-list-delete" style="text-decoration:none; display:inline-block; padding:2px 10px;"
                           onclick="return confirm('伝票番号 <%= r.get("id") %> を削除しますか？');">削除</a>
                    </td>
                </tr>
                <% } %>
            </tbody>
        </table>

        <% if (total > 0) { %>
        <div class="pagination">
            <% if (pageNo > 1) { %>
                <a class="page-btn" href="<%= ctx %>/denpyo?<%= navParams %>&page=1">&laquo; 最初</a>
                <a class="page-btn" href="<%= ctx %>/denpyo?<%= navParams %>&page=<%= pageNo - 1 %>">&lsaquo; 前へ</a>
            <% } else { %>
                <span class="page-btn disabled">&laquo; 最初</span>
                <span class="page-btn disabled">&lsaquo; 前へ</span>
            <% } %>

            <form method="get" action="<%= ctx %>/denpyo" class="page-jump">
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <% if (q != null && !q.isEmpty()) { %><input type="hidden" name="q" value="<%= esc(q) %>"><% } %>
                <% if (no != null && !no.isEmpty()) { %><input type="hidden" name="no" value="<%= esc(no) %>"><% } %>
                <input type="number" name="page" min="1" max="<%= totalPages %>" value="<%= pageNo %>" class="page-input">
                <span class="page-total">/ <%= totalPages %> ページ（全 <%= total %> 件）</span>
                <button type="submit" class="page-btn">移動</button>
            </form>

            <% if (pageNo < totalPages) { %>
                <a class="page-btn" href="<%= ctx %>/denpyo?<%= navParams %>&page=<%= pageNo + 1 %>">次へ &rsaquo;</a>
                <a class="page-btn" href="<%= ctx %>/denpyo?<%= navParams %>&page=<%= totalPages %>">最後 &raquo;</a>
            <% } else { %>
                <span class="page-btn disabled">次へ &rsaquo;</span>
                <span class="page-btn disabled">最後 &raquo;</span>
            <% } %>
        </div>
        <% } %>
    </div>
</body>
</html>
