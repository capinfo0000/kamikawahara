<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, otameshirenshuu.DetailList" %>
<%
	/* ========================================================================
	 * 【明細画面（表示だけ）】
	 *   判断・入力チェック・確認・整形（¥やカンマ付け）はすべて DetailList が行い、
	 *   その結果を「そのまま出す HTML 部品」として HashMap に入れて返す。
	 *   このJSPは if 文で分岐せず、部品を式タグで並べて出すだけ。
	 * ====================================================================== */
	HashMap<String, Object> data = DetailList.detail(request, response);
	if (data == null) return; // 削除・保存で別URLへ飛ばしたときは表示しない（唯一の判定）
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

    <div class="page-wrapper<%= data.get("pageMod") %>">

        <!-- 確認モードのときだけ案内文（それ以外は空文字）-->
        <%= data.get("confirmNotice") %>

        <!-- 入力/確認のときは form で囲む（閲覧のときは空文字）-->
        <%= data.get("formOpen") %>
            <!-- 検索条件・ページ・（確認時は入力値）を持ち回る hidden 群 -->
            <%= data.get("hiddenInputs") %>

            <div class="header-container">
                <div class="back-actions-group">
                    <%= data.get("backLink") %>
                    <div class="meta-info-group">
                        <div class="header-item item-date">日付 <%= data.get("dateField") %></div>
                        <div class="header-item item-id">伝票番号 <%= data.get("idField") %></div>
                    </div>
                </div>
                <div class="header-item item-partner">取引先 <%= data.get("partnerField") %></div>
                <div class="header-item item-description">購入物 <%= data.get("descField") %></div>
                <div class="button-group"><%= data.get("buttons") %></div>
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
                    <!-- 明細行（入力欄 or ¥付きの文字）はサーバーが作った文字列をそのまま出す -->
                    <%= data.get("entriesRows") %>
                    <tr class="total-row" id="total-row">
                        <td class="col-subject">合計</td>
                        <td class="col-amount-total"><span class="view-mode"><%= data.get("debitTotalText") %></span></td>
                        <td class="col-subject">合計</td>
                        <td class="col-amount-total"><span class="view-mode"><%= data.get("creditTotalText") %></span></td>
                    </tr>
                </tbody>
            </table>

            <!-- 「＋ 行を追加」ボタン（入力画面のときだけ中身が入る）-->
            <%= data.get("addRowButton") %>

            <div class="note-container">
                <label class="note-label">備考：</label>
                <%= data.get("noteField") %>
            </div>
        <%= data.get("formClose") %>
    </div>

    <!-- ポップアップ（削除確認・破棄確認・入力エラー）。必要なものだけサーバーが作る -->
    <%= data.get("popups") %>

</body>
</html>
