<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.Slip" %>
<%!
	// 表示用のHTMLエスケープ（表示の整形のみ）
	private String esc(String v) {
		if (v == null) return "";
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
%>
<%
	// このJSPは「表示」だけを担当する。処理は SlipListServlet(/list) が行う。
	// 直接開かれて表示データが無いときは、処理担当(/list)へ回す。
	if (request.getAttribute("pageSlips") == null) {
		response.sendRedirect(request.getContextPath() + "/list");
		return;
	}

	// SlipListServlet が用意した表示用データを受け取るだけ
	List<Slip> pageSlips = (List<Slip>) request.getAttribute("pageSlips");
	int total        = (Integer) request.getAttribute("total");
	int totalPages   = (Integer) request.getAttribute("totalPages");
	int pageNo       = (Integer) request.getAttribute("pageNo");
	String q         = (String) request.getAttribute("q");
	String no        = (String) request.getAttribute("no");
	String sortKey   = (String) request.getAttribute("sortKey");
	String order     = (String) request.getAttribute("order");
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
    <!-- CSSファイルの読み込み -->
    <link rel="stylesheet" href="style.css">
</head>
<body>

    <div class="page-wrapper">
        <h1>伝票一覧</h1>

        <!-- 検索と新規登録エリア（送信先は処理担当の /list） -->
        <div class="actions-container">
            <form class="search-area" method="get" action="list">
                <input type="text" name="no" value="<%= esc(no) %>" class="no-search-box" placeholder="伝票番号（例: 5 または 1~10）">
            	<div class="search-box">
                	<input type="text" id="search-input" name="q" value="<%= esc(q) %>" placeholder="検索（日付、取引先、購入物）">
            	</div>
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <button type="submit" class="search-exec-btn" id="search-trigger-btn">検索</button>
                <% if (hasSearch) { %>
                	<a href="list?sortKey=<%= sortKey %>&order=<%= order %>" class="search-clear-btn" style="margin-left:8px; font-size:13px; color:#337ab7; text-decoration:none;">クリア</a>
                <% } %>
            </form>

			<a href="detail?mode=new" class="register-btn" style="text-decoration: none; display: inline-block;">
    			新規登録
			</a>

        </div>

        <% if (hasSearch) { %>
        <p style="font-size:14px; margin:0 0 10px;">検索結果：<%= total %>件<%
            if (!no.isEmpty()) { %>（伝票番号: <%= esc(no) %>）<% }
            if (!q.isEmpty()) { %>（キーワード: <%= esc(q) %>）<% }
        %></p>
        <% } %>

        <!-- 伝票テーブル -->
        <table class="slip-table">
            <thead>
                <tr>
                    <th class="col-id sort-trigger" onclick="location.href='list?sortKey=id&order=<%= idNextOrder %><%= searchParams %>'">伝票番号<span class="sort-arrows"><%= idArrow %></span></th>
                    <th class="col-date sort-trigger" onclick="location.href='list?sortKey=date&order=<%= dateNextOrder %><%= searchParams %>'">日付<span class="sort-arrows"><%= dateArrow %></span></th>
                    <th class="col-partner">取引先（購入先）</th>
                    <th class="col-description">購入物</th>
                    <th class="col-amount col-amount-th">金額</th>
                    <th class="col-management-header" colspan="2">管理</th>
                </tr>
            </thead>
            <tbody>
                <% if (pageSlips.isEmpty()) { %>
                <tr>
                    <td colspan="7">伝票がありません。「新規登録」から追加してください。</td>
                </tr>
                <% } %>
                <% for (Slip s : pageSlips) {
                       String dateSlash = s.getDate().replace("-", "/");
                %>
                <tr>
                    <td class="col-id"><%= s.getId() %></td>
                    <td class="col-date"><%= esc(dateSlash) %></td>
                    <td class="col-partner"><%= esc(s.getPartnerName()) %></td>
                    <td class="col-description"><%= esc(s.getDescription()) %></td>
                    <td class="col-amount col-amount-td">&yen;<%= String.format("%,d", s.getTotal()) %></td>
                    <td class="col-detail"><a href="detail?mode=view&id=<%= s.getId() %>" class="detail-link">明細</a></td>
                    <td class="col-delete-cell">
                        <a href="detail?action=delete&id=<%= s.getId() %>&sortKey=<%= sortKey %>&order=<%= order %>&page=<%= pageNo %><%= searchParams %>" class="btn-list-delete"
                           style="text-decoration:none; display:inline-block; padding:2px 10px;"
                           onclick="return confirm('伝票番号 <%= s.getId() %> を削除しますか？');">削除</a>
                    </td>
                </tr>
                <% } %>
            </tbody>
        </table>

        <!-- ページネーション -->
        <% if (total > 0) { %>
        <div class="pagination">
            <% if (pageNo > 1) { %>
                <a class="page-btn" href="list?<%= navParams %>&page=1">&laquo; 最初</a>
                <a class="page-btn" href="list?<%= navParams %>&page=<%= pageNo - 1 %>">&lsaquo; 前へ</a>
            <% } else { %>
                <span class="page-btn disabled">&laquo; 最初</span>
                <span class="page-btn disabled">&lsaquo; 前へ</span>
            <% } %>

            <!-- ページ番号を直接入力して移動 -->
            <form method="get" action="list" class="page-jump">
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <% if (!q.isEmpty()) { %><input type="hidden" name="q" value="<%= esc(q) %>"><% } %>
                <% if (!no.isEmpty()) { %><input type="hidden" name="no" value="<%= esc(no) %>"><% } %>
                <input type="number" name="page" min="1" max="<%= totalPages %>" value="<%= pageNo %>" class="page-input">
                <span class="page-total">/ <%= totalPages %> ページ（全 <%= total %> 件）</span>
                <button type="submit" class="page-btn">移動</button>
            </form>

            <% if (pageNo < totalPages) { %>
                <a class="page-btn" href="list?<%= navParams %>&page=<%= pageNo + 1 %>">次へ &rsaquo;</a>
                <a class="page-btn" href="list?<%= navParams %>&page=<%= totalPages %>">最後 &raquo;</a>
            <% } else { %>
                <span class="page-btn disabled">次へ &rsaquo;</span>
                <span class="page-btn disabled">最後 &raquo;</span>
            <% } %>
        </div>
        <% } %>
    </div>

</body>
</html>
