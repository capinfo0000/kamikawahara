<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.Slip, otameshirenshuu.SlipDao, java.net.URLEncoder" %>
<%
	/* ========================================================================
	 * 【一覧画面】サーブレットは使わず、このJSPの中でJava(SlipDao)を呼んで処理する。
	 *   1) 削除リンクが押されたら削除して一覧へ戻す
	 *   2) 検索・並び替え条件を受け取り、SlipDaoで一覧を取得
	 *   3) 1ページ30件に切り出して、下のHTMLで表示する
	 * ====================================================================== */

	// データ処理担当（DB接続やテーブル作成もこの中でやってくれる）
	SlipDao dao = new SlipDao();

	// URLパラメータを受け取るための小さな準備（無ければ既定値にする）
	String q       = request.getParameter("q");       if (q == null) q = "";       // キーワード
	String no      = request.getParameter("no");      if (no == null) no = "";      // 伝票番号
	String sortKey = request.getParameter("sortKey");                               // 並べる列
	if (!"date".equals(sortKey)) sortKey = "id";
	String order   = request.getParameter("order");                                 // 昇順/降順
	if (!"asc".equals(order)) order = "desc";

	// リンクに引き継ぐための文字列（検索条件を保持したままソート・ページ移動するため）
	String searchParams = (q.isEmpty()  ? "" : "&q="  + URLEncoder.encode(q, "UTF-8"))
	                    + (no.isEmpty() ? "" : "&no=" + URLEncoder.encode(no, "UTF-8"));

	// (1) 削除：URLが index.jsp?action=delete&id=... のとき
	if ("delete".equals(request.getParameter("action"))) {
		try { dao.delete(Integer.parseInt(request.getParameter("id"))); } catch (Exception e) {}
		// 削除後は、元の並び順・ページ・検索条件を保ったまま一覧へ戻す
		String pageParam = request.getParameter("page"); if (pageParam == null) pageParam = "1";
		response.sendRedirect("index.jsp?sortKey=" + sortKey + "&order=" + order
				+ "&page=" + pageParam + searchParams);
		return;
	}

	// (2) 一覧を取得（検索・並び替えはSlipDaoのSQLで実施）
	List<Slip> slips = dao.findFiltered(q, no, sortKey, order);

	// (3) ページング（1ページ30件ぶんだけ切り出す）
	int total = slips.size();
	int pageSize = 30;
	int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
	int pageNo = 1;
	try { pageNo = Integer.parseInt(request.getParameter("page")); } catch (Exception e) {}
	if (pageNo < 1) pageNo = 1;
	if (pageNo > totalPages) pageNo = totalPages;
	int from = (pageNo - 1) * pageSize;
	int to = Math.min(from + pageSize, total);
	List<Slip> pageSlips = (total == 0) ? slips : slips.subList(from, to);

	// 画面表示に使う小物（ソート見出しの矢印・次にクリックしたときの向き）
	boolean hasSearch = !q.isEmpty() || !no.isEmpty();
	String idNextOrder   = ("id".equals(sortKey)   && "asc".equals(order)) ? "desc" : "asc";
	String dateNextOrder = ("date".equals(sortKey) && "asc".equals(order)) ? "desc" : "asc";
	String idArrow   = "id".equals(sortKey)   ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
	String dateArrow = "date".equals(sortKey) ? ("asc".equals(order) ? "▲" : "▼") : "▲▼";
	String navParams = "sortKey=" + sortKey + "&order=" + order + searchParams; // ページ移動リンク用
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>伝票一覧</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="page-wrapper">
        <h1>伝票一覧</h1>

        <!-- 検索と新規登録 -->
        <div class="actions-container">
            <form class="search-area" method="get" action="index.jsp">
                <input type="text" name="no" value="<%= SlipDao.esc(no) %>" class="no-search-box" placeholder="伝票番号（例: 5 または 1~10）">
                <div class="search-box">
                    <input type="text" name="q" value="<%= SlipDao.esc(q) %>" placeholder="検索（日付、取引先、購入物）">
                </div>
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <button type="submit" class="search-exec-btn">検索</button>
                <% if (hasSearch) { %>
                    <a href="index.jsp?sortKey=<%= sortKey %>&order=<%= order %>" class="search-clear-btn" style="margin-left:8px; font-size:13px; color:#337ab7; text-decoration:none;">クリア</a>
                <% } %>
            </form>
            <a href="detail.jsp?mode=new" class="register-btn" style="text-decoration:none; display:inline-block;">新規登録</a>
        </div>

        <% if (hasSearch) { %>
        <p style="font-size:14px; margin:0 0 10px;">検索結果：<%= total %>件<%
            if (!no.isEmpty()) { %>（伝票番号: <%= SlipDao.esc(no) %>）<% }
            if (!q.isEmpty())  { %>（キーワード: <%= SlipDao.esc(q) %>）<% }
        %></p>
        <% } %>

        <!-- 伝票テーブル -->
        <table class="slip-table">
            <thead>
                <tr>
                    <th class="col-id sort-trigger" onclick="location.href='index.jsp?sortKey=id&order=<%= idNextOrder %><%= searchParams %>'">伝票番号<span class="sort-arrows"><%= idArrow %></span></th>
                    <th class="col-date sort-trigger" onclick="location.href='index.jsp?sortKey=date&order=<%= dateNextOrder %><%= searchParams %>'">日付<span class="sort-arrows"><%= dateArrow %></span></th>
                    <th class="col-partner">取引先（購入先）</th>
                    <th class="col-description">購入物</th>
                    <th class="col-amount col-amount-th">金額</th>
                    <th class="col-management-header" colspan="2">管理</th>
                </tr>
            </thead>
            <tbody>
                <% if (pageSlips.isEmpty()) { %>
                <tr><td colspan="7">伝票がありません。「新規登録」から追加してください。</td></tr>
                <% } %>
                <% for (Slip s : pageSlips) { %>
                <tr>
                    <td class="col-id"><%= s.getId() %></td>
                    <td class="col-date"><%= SlipDao.slash(s.getDate()) %></td>
                    <td class="col-partner"><%= SlipDao.esc(s.getPartnerName()) %></td>
                    <td class="col-description"><%= SlipDao.esc(s.getDescription()) %></td>
                    <td class="col-amount col-amount-td"><%= SlipDao.yen(s.getTotal()) %></td>
                    <td class="col-detail"><a href="detail.jsp?mode=view&id=<%= s.getId() %>" class="detail-link">明細</a></td>
                    <td class="col-delete-cell">
                        <a href="index.jsp?action=delete&id=<%= s.getId() %>&sortKey=<%= sortKey %>&order=<%= order %>&page=<%= pageNo %><%= searchParams %>"
                           class="btn-list-delete" style="text-decoration:none; display:inline-block; padding:2px 10px;"
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

            <form method="get" action="index.jsp" class="page-jump">
                <input type="hidden" name="sortKey" value="<%= sortKey %>">
                <input type="hidden" name="order" value="<%= order %>">
                <% if (!q.isEmpty())  { %><input type="hidden" name="q"  value="<%= SlipDao.esc(q) %>"><% } %>
                <% if (!no.isEmpty()) { %><input type="hidden" name="no" value="<%= SlipDao.esc(no) %>"><% } %>
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
