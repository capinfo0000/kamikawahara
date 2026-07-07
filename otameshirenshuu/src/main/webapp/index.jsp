<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, java.net.URLEncoder, otameshirenshuu.Slip, otameshirenshuu.SlipStore" %>
<%!
	private String esc(String v) {
		if (v == null) return "";
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
%>
<%
	// ソート条件（sortKey: id / date, order: asc / desc）。既定は伝票番号の降順（新しい順）。
	String sortKey = request.getParameter("sortKey");
	if (!"date".equals(sortKey)) {
		sortKey = "id";
	}
	String order = request.getParameter("order");
	if (!"asc".equals(order)) {
		order = "desc";
	}

	// 検索キーワード（伝票番号・日付・取引先・購入物を対象）
	String q = request.getParameter("q");
	if (q == null) {
		q = "";
	}

	List<Slip> slips = SlipStore.getInstance().findFiltered(q, sortKey, order);

	// ソートリンクに検索キーワードを引き継ぐためのURLパラメータ
	String qParam = q.isEmpty() ? "" : ("&q=" + URLEncoder.encode(q, "UTF-8"));

	// ヘッダーのクリックで昇順⇔降順を切り替える。矢印で現在の並び順を示す。
	String idNextOrder   = ("id".equals(sortKey)   && "asc".equals(order)) ? "desc" : "asc";
	String dateNextOrder = ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
	String idArrow   = "id".equals(sortKey)   ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
	String dateArrow = "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";

	// ページング（1ページ30件）。削除で件数が減ると自動的に上へ詰まる。
	final int PAGE_SIZE = 30;
	int total = slips.size();
	int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
	int pageNo = 1;
	try {
		String pp = request.getParameter("page");
		if (pp != null && !pp.trim().isEmpty()) {
			pageNo = Integer.parseInt(pp.trim());
		}
	} catch (NumberFormatException e) {
		pageNo = 1;
	}
	if (pageNo < 1) pageNo = 1;
	if (pageNo > totalPages) pageNo = totalPages;

	int from = (pageNo - 1) * PAGE_SIZE;
	int to = Math.min(from + PAGE_SIZE, total);
	List<Slip> pageSlips = (total == 0) ? slips : slips.subList(from, to);

	// ページ移動リンクに、並び順・検索キーワードを引き継ぐための共通パラメータ
	String navParams = "sortKey=" + sortKey + "&order=" + order + qParam;
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

        <!-- 検索と新規登録エリア -->
        <div class="actions-container">
            <!-- 検索フォーム（GETで送信。ソート条件も引き継ぐ） -->
            <form class="search-area" method="get" action="index.jsp">
            	<div class="search-box">
                	<input type="text" id="search-input" name="q" value="<%= esc(q) %>" placeholder="検索（伝票番号、日付、取引先、購入物）">
            	</div>
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <button type="submit" class="search-exec-btn" id="search-trigger-btn">検索</button>
                <% if (!q.isEmpty()) { %>
                	<a href="index.jsp?sortKey=<%= sortKey %>&order=<%= order %>" class="search-clear-btn" style="margin-left:8px; font-size:13px; color:#337ab7; text-decoration:none;">クリア</a>
                <% } %>
            </form>

			<a href="detail?mode=new" class="register-btn" style="text-decoration: none; display: inline-block;">
    			新規登録
			</a>

        </div>

        <% if (!q.isEmpty()) { %>
        <p style="font-size:14px; margin:0 0 10px;">「<%= esc(q) %>」の検索結果：<%= slips.size() %>件</p>
        <% } %>

        <!-- 伝票テーブル -->
        <table class="slip-table">
            <thead>
                <tr>
                    <th class="col-id sort-trigger" onclick="location.href='index.jsp?sortKey=id&order=<%= idNextOrder %><%= qParam %>'">伝票番号<span class="sort-arrows"><%= idArrow %></span></th>
                    <th class="col-date sort-trigger" onclick="location.href='index.jsp?sortKey=date&order=<%= dateNextOrder %><%= qParam %>'">日付<span class="sort-arrows"><%= dateArrow %></span></th>
                    <th class="col-partner">取引先（購入先）</th>
                    <th class="col-description">購入物</th>
                    <th class="col-amount col-amount-th">金額</th>
                    <th class="col-management-header" colspan="2">管理</th>
                </tr>
            </thead>
            <tbody>
                <!-- 明細データ（ストアから取得） -->
                <% if (slips.isEmpty()) { %>
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
                        <a href="detail?action=delete&id=<%= s.getId() %>" class="btn-list-delete"
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
                <a class="page-btn" href="index.jsp?<%= navParams %>&page=1">&laquo; 最初</a>
                <a class="page-btn" href="index.jsp?<%= navParams %>&page=<%= pageNo - 1 %>">&lsaquo; 前へ</a>
            <% } else { %>
                <span class="page-btn disabled">&laquo; 最初</span>
                <span class="page-btn disabled">&lsaquo; 前へ</span>
            <% } %>

            <!-- ページ番号を直接入力して移動 -->
            <form method="get" action="index.jsp" class="page-jump">
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <% if (!q.isEmpty()) { %><input type="hidden" name="q" value="<%= esc(q) %>"><% } %>
                <input type="number" name="page" min="1" max="<%= totalPages %>" value="<%= pageNo %>" class="page-input">
                <span class="page-total">/ <%= totalPages %> ページ（全 <%= total %> 件）</span>
                <button type="submit" class="page-btn">移動</button>
            </form>

            <% if (pageNo < totalPages) { %>
                <a class="page-btn" href="index.jsp?<%= navParams %>&page=<%= pageNo + 1 %>">次へ &rsaquo;</a>
                <a class="page-btn" href="index.jsp?<%= navParams %>&page=<%= totalPages %>">最後 &raquo;</a>
            <% } else { %>
                <span class="page-btn disabled">次へ &rsaquo;</span>
                <span class="page-btn disabled">最後 &raquo;</span>
            <% } %>
        </div>
        <% } %>
    </div>

</body>
</html>
