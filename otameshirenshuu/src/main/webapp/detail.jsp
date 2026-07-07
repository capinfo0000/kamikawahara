

<%@ page contentType="text/html; charset=UTF-8" %>



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

<%
    // サーブレットからのデータを受け取る
    String currentMode = (String) request.getAttribute("currentMode");
    if (currentMode == null) {
        currentMode = "view";
    }
    
    String description = (String) request.getAttribute("description");
    if (description == null) {
        description = "";
    }
    
    String slipId = (String) request.getAttribute("slipId");
    if (slipId == null) {
    	slipId = "";
    }
    
    String slipDate = (String) request.getAttribute("slipDate");
    if (slipDate == null) {
    	slipDate = "";
    }

    String partnerName = (String) request.getAttribute("partnerName");
    if (partnerName == null) {
    	partnerName = "";
    }
    
    String debitSubject = (String) request.getAttribute("debitSubject");
    if (debitSubject == null) { 
    	debitSubject = ""; 
    }

    String debitAmount = (String) request.getAttribute("debitAmount");
    if (debitAmount == null) { 
    	debitAmount = ""; 
    }

    String creditSubject = (String) request.getAttribute("creditSubject");
    if (creditSubject == null) { 
    	creditSubject = ""; 
    }

    String creditAmount = (String) request.getAttribute("creditAmount");
    if (creditAmount == null) { 
    	creditAmount = ""; 
    }
    
    String note = (String) request.getAttribute("note");
    if (note == null) {
    	note = "";
    }
    
%>

    <div class="page-wrapper">

        <!-- 入力エラーメッセージ（サーブレットから渡された場合のみ表示） -->
        <%
            java.util.List<String> errors =
                (java.util.List<String>) request.getAttribute("errors");
            if (errors != null && !errors.isEmpty()) {
        %>
        <div class="error-banner">
            <ul>
                <% for (String err : errors) { %>
                    <li><%= err %></li>
                <% } %>
            </ul>
        </div>
        <% } %>

        <!-- 上部ヘッダーエリア（基本情報とボタン） -->
        <div class="header-container">
            
            <!-- 「伝票一覧に戻る」ボタン -->
            <div class="back-actions-group">
            	<% if ("view".equals(currentMode)) { %>
            		<!-- 閲覧モードの時は、そのまま一覧画面に戻る -->
					<a href="index.jsp" class="btn-back">←伝票一覧</a>
				<% } else if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
					<!-- 編集・新規モードの時は、一覧に戻らず、ポップアップを開く -->
					<a href="#leave-popup" class="btn-back">←伝票一覧</a>
				<% } %>
				
			<!-- 編集モード・新規登録モードの時のみ入力欄全体をformタグで囲む -->
				<% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
					<form action="detail" method="POST">
				<% } %>
            
            <!-- 日付と伝票番号の縦並びエリア -->
            	<div class="meta-info-group">
					
					<!-- 日付 -->
                	<div class="header-item item-date">
                    	日付 
                    	<% if ("view".equals(currentMode)) { %>
                    	<!-- 閲覧用 -->
                   	 	<span class="underline-text"><%= slipDate %></span>
               			<% } %>
                
               			<% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
                    	<!-- 編集用：日付選択カレンダー -->
               	    		 <input type="date" name="slipDate" class="header-input" value="<%= slipDate %>">
               			<% } %>
                    </div>
                    
                    <!-- 伝票番号 -->
                	<div class="header-item item-id">
                    	伝票番号
                    	<% if (!"new".equals(currentMode)) { %>
                    		<!-- 閲覧・編集用：既存の番号を表示 -->
                    		<span class="underline-text"><%= slipId %></span>
                    	<% } %>
                    	
                    	<% if ( "new".equals(currentMode)) { %>
                    		<!-- 新規登録用：番号がないのでハイフンを表示 -->
                    		<span class="underline-text">-</span>
                    	<% } %>
               		</div>
            	</div>
            </div>
            
            <!-- 取引先 -->
	            <div class="header-item item-partner">
	                取引先 
	                <% if ("view".equals(currentMode)) { %>
	                	<!-- 閲覧用 -->
	                	<span class="underline-text partner-name"><%= partnerName %></span>
	                <% } %>
	                
	                <% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
	                	<!-- 編集用：テキスト入力欄 -->
	                	<input type="text" name="partnerName" class="header-input partner-input" value="<%= partnerName %>">
	                <% } %>
	            </div>
	            
	        <!-- 購入物 -->
	        	<div class="header-item item-description">
	        		購入物
	        		
	        		<% if ("view".equals(currentMode)) { %>
	        		<!-- 閲覧用 -->
	        			<span class="underline-text description-text"><%= description %></span>
	        		<% } %>
	        		
	        		
	        		<% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
	        		<!-- 編集用・テキスト入力欄 -->
	        			<input type="text" name="description" class="header-input description-input" value="<%= description %>">
	        		<% } %>
	        		
	        	</div>
            
            <!-- 右上ボタンエリア -->
	            <div class="button-group">
	            <% if ("view".equals(currentMode)) { %>
	            	<!-- 閲覧モードの時に見えるボタン -->
	                <a href="detail?mode=edit" class="btn btn-edit">編集</a>
	                <a href="#delete-popup" class="btn btn-delete">削除</a>
	            <% } %>
	           
	               
	            <%  if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
	                <!-- 編集モードの時に見えるボタン -->
	               
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
                </tr>
            </thead>
            <tbody>
            
            	<%
            	// サーブレットぁら送られてきた大きな箱（明細リスト）を受け取る
            	java.util.List<java.util.Map<String, String>> detailList =
            		(java.util.List<java.util.Map<String, String>>) request.getAttribute("detailList");
            	
            	if (detailList == null) {
            		detailList = new java.util.ArrayList<>();
            	}

            	//借方・貸方の合計金額を集計する
            	int debitTotal = 0;
            	int creditTotal = 0;

            	//箱に入っているデータの数だけtrを自動で繰り返す
            	for (java.util.Map<String, String> row : detailList) {

            		String dSub = row.getOrDefault("debitSubject", "");
            		String dAmt = row.getOrDefault("debitAmount", "");
            		String cSub = row.getOrDefault("creditSubject", "");
            		String cAmt = row.getOrDefault("creditAmount", "");

            		//金額文字列から数字以外を除去して合計に加算
            		String cleanDAmt = dAmt.replaceAll("[^0-9]", "");
            		String cleanCAmt = cAmt.replaceAll("[^0-9]", "");
            		if (!cleanDAmt.isEmpty()) {
            			debitTotal += Integer.parseInt(cleanDAmt);
            		}
            		if (!cleanCAmt.isEmpty()) {
            			creditTotal += Integer.parseInt(cleanCAmt);
            		}
            	%>
            
            
	                <tr>
						<!-- 借方勘定科目 -->
	                    <td class="col-subject">
	                    	<% if ("view".equals(currentMode)) { %>
	                    		<!-- 閲覧モードの時 -->
								<span class="view-mode"><%= dSub %></span>
							<% } else { %>
								<!-- 編集モード、新規登録モードの時 -->
								<input type="text" name="debitSubject" class="input-field" value="<%= dSub %>">
							<% } %>
						</td>
						
						<!-- 借方金額 -->
	                    <td class="col-amount">
	                    	<% if ("view".equals(currentMode)) { %>
								<span class="view-mode">&yen;<%= dAmt %></span>
							<% } else { %>
								<input type="text" name="debitAmount" class="input-field text-right" value="<%= dAmt %>">
							<% } %>
						</td>
						
						<!-- 貸方勘定科目 -->
	                    <td class="col-subject">
	                    	<% if ("view".equals(currentMode)) { %>
								<span class="view-mode"><%= cSub %></span>
							<% } else { %>
								<input type="text" name="creditSubject" class="input-field" value="<%= cSub %>">
							<% } %>
						</td>
						
						<!-- 貸方金額 -->
	                    <td class="col-amount">
	                    	<% if ("view".equals(currentMode)) { %>
								<span class="view-mode">&yen;<%= cAmt %></span>
							<% } else { %>
								<input type="text" name="creditAmount" class="input-field text-right" value="<%= cAmt %>">
							<% } %>
						</td>
	                </tr>
	                
                <% } %>
                
	                
	                
	                <!-- 合計行 -->
	                <tr class="total-row">

						<!-- 借方合計金額エリア -->
	                    <td class="col-subject">合計</td>
	                    <td class="col-amount-total">
							<!-- 明細から集計した借方合計を表示 -->
							<span class="<%= "view".equals(currentMode) ? "view-mode" : "edit-mode" %>">&yen;<%= String.format("%,d", debitTotal) %></span>
						</td>

						<!-- 貸方合計金額エリア -->
	                    <td class="col-subject">合計</td>
	                    <td class="col-amount-total">
							<!-- 明細から集計した貸方合計を表示 -->
							<span class="<%= "view".equals(currentMode) ? "view-mode" : "edit-mode" %>">&yen;<%= String.format("%,d", creditTotal) %></span>
						</td>
	                </tr>
	                
            </tbody>
        </table>
        
        <!-- 行追加ボタンエリア（貸方の合計金額の下に配置） -->
        <% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
        	<div class="add-row-container">
            	<button type="button" class="btn-add-row">＋ 行を追加</button>
        	</div> 
        <% } %>

         <!-- 備考エリア -->
        <div class="note-container">
            <label for="note-text" class="note-label">備考：</label>
            
            <% if ("view".equals(currentMode)) { %>
            	<!-- 閲覧用 -->
            	<textarea id="note-text" class="note-textarea view-mode" rows="3" readonly><%= note %></textarea>
            <% } %>
            
            <% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
            	<!-- 編集用 -->
            	<textarea id="note-text-edit" textarea name="note" class="note-textarea edit-mode" rows="3"><%= note %></textarea>
            <% } %>
        </div>
        
        <% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
        	</form>
        <% } %>
      </div>

        <!-- 削除確認ポップアップ（閲覧・編集共通） -->
        <% if ("view".equals(currentMode)) { %>
        <div id="delete-popup" class="popup-overlay delete-layout">
            <div class="delete-popup-box">
            
                <a href="detail?mode=view" class="popup-close delete-popup-close" style="text-decoraion: none;">❌</a>
                
                <p class="popup-title"><span class="text-danger">削除</span>しますか？</p>
                
                <div class="popup-btn-group">
                    <a href="index.jsp" class="popup-btn btn-yes">はい</a>
                    <a href="detail?mode=view" class="popup-btn">いいえ</a>
                </div>
            </div>
        </div>
        <% } %>
        
        <!-- 金額不一致エラーポップアップ（編集モード専用） -->
        <div class="popup-overlay error-layout">
			<div class="error-popup-box">
				<button class="popup-close error-close">❌</button>
				<div class="error-mennage-container">
					<p class="error-text">借方と貸方の合計金額が一致しないため</p>
					<p class="error-text">登録できません。</p>
				</div>
			</div>
		</div>
		
		<!-- 登録確認ポップアップ（編集モード専用） -->
		<div class="popup-overlay register-layout">
			<div class="register-popup-box">
				<button class="popup-close">❌</button>
				<p class="popup-title"><span class="text-primary">登録</span>しますか？</p>
				<div class="popup-btn-group">
					<button class="popup-btn">はい</button>
					<button class="popup-btn">いいえ</button>
				</div>
			</div>
		</div>
		
		<!-- 編集破棄確認ポップアップ -->
		<% if ("edit".equals(currentMode) || "new".equals(currentMode)) { %>
			<!-- 編集・新規モードの時のみポップアップを表示させる -->
			<div id="leave-popup" class="popup-overlay leave-popup-layout">
				<div class="leave-popup-box">
					<a href="detail?mode=<%= currentMode %>" class="popup-close">❌</a>
					
					<div class="leave-massage-container">
						<p class="leave-text">編集中の内容は削除されます。</p>
						<p class="leave-text">よろしいですか？</p>
					</div>
					
					<div class="popup-btn-group">
						<a href="index.jsp" class="popup-btn btn-ok-link">はい</a>
						<a href="detail?mode=<%= currentMode %>" class="popup-btn">いいえ</a>
					</div>
					
				</div>
			</div>
		<% } %>

    

</body>
</html>
