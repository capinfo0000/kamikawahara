<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.Slip, otameshirenshuu.SlipList, otameshirenshuu.SlipDao" %>
<%
	/* ========================================================================
	 * 【一覧画面（表示だけ担当）】
	 *   検索・並び替え・ページング・削除は SlipList.execute(...) が計算する。
	 *   このJSPは、返ってきた HashMap から値を取り出して表を組み立てるだけ。
	 * ====================================================================== */

	SlipList list = new SlipList();
	HashMap<String, Object> data = list.execute(request, response);
	if (data == null) return; // 削除で別URLへ飛ばしたときは表示を止める

	// HashMap から、この画面で使う値を取り出す（キー名で1つずつ get する）
	List<Slip> pageSlips = (List<Slip>) data.get("pageSlips");
	int total        = (Integer) data.get("total");
	int totalPages   = (Integer) data.get("totalPages");
	int pageNo       = (Integer) data.get("pageNo");
	String q         = (String)  data.get("q");
	String no        = (String)  data.get("no");
	String sortKey   = (String)  data.get("sortKey");
	String order     = (String)  data.get("order");
	boolean hasSearch    = (Boolean) data.get("hasSearch");
	String searchParams  = (String) data.get("searchParams");
	String navParams     = (String) data.get("navParams");
	String idArrow       = (String) data.get("idArrow");
	String dateArrow     = (String) data.get("dateArrow");
	String idNextOrder   = (String) data.get("idNextOrder");
	String dateNextOrder = (String) data.get("dateNextOrder");

	// 明細・新規登録へ GET で渡す「検索条件＋ページ」（明細から戻るとき同じ状態に戻すため）
	String detailState = "sortKey=" + sortKey + "&order=" + order + "&page=" + pageNo + searchParams;
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

        <!-- 検索と新規登録（検索は index.jsp へ GET。並び順は hidden で保持）-->
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
            <!-- 新規登録：IDなしで明細へ GET → 新規モードになる -->
            <a href="detail.jsp?<%= detailState %>" class="register-btn" style="text-decoration:none; display:inline-block;">新規登録</a>
        </div>

        <% if (hasSearch) { %>
        <p style="font-size:14px; margin:0 0 10px;">検索結果：<%= total %>件<%
            if (!no.isEmpty()) { %>（伝票番号: <%= SlipDao.esc(no) %>）<% }
            if (!q.isEmpty())  { %>（キーワード: <%= SlipDao.esc(q) %>）<% }
        %></p>
        <% } %>

        <!-- 伝票テーブル（見出しクリックで並び替え）-->
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
                    <!-- 明細：ID付きで明細へ GET → 閲覧モードになる -->
                    <td class="col-detail"><a href="detail.jsp?id=<%= s.getId() %>&<%= detailState %>" class="detail-link">明細</a></td>
                    <td class="col-delete-cell">
                        <a href="index.jsp?action=delete&id=<%= s.getId() %>&sortKey=<%= sortKey %>&order=<%= order %>&page=<%= pageNo %><%= searchParams %>"
                           class="btn-list-delete" style="text-decoration:none; display:inline-block; padding:2px 10px;"
                           onclick="return confirm('伝票番号 <%= s.getId() %> を削除しますか？');">削除</a>
                    </td>
                </tr>
                <% } %>
            </tbody>
        </table>

        <!-- ページネーション（前後移動・番号入力はすべて hidden で条件を保持）-->
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
