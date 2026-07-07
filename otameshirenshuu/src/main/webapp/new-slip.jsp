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

    <div class="page-wrapper is-new">
        
        <!-- 上部ヘッダーエリア（基本情報とボタン） -->
        <div class="header-container">
            
            <!-- 「伝票一覧に戻る」ボタン -->
            <div class="back-actions-group">
				<a href="index.jsp" class="btn-back">←伝票一覧</a>
			
            
            <!-- 日付と伝票番号の縦並びエリア -->
            	<div class="meta-info-group">
					
					<!-- 日付 -->
                	<div class="header-item item-date">
                    	日付 
                    	<!-- 閲覧用 -->
                    	<span class="underline-text view-mode">2026/06/13</span>
                    	<!-- 編集用：日付選択ができるカレンダー型input（枠線なし） -->
                    	<!-- valueに値があれば編集、なければ新規登録 -->
                    	<input type="date" class="edit-mode header-input" value="">
                    </div>
                    
                    <!-- 伝票番号 -->
                	<div class="header-item item-id">
                    	伝票番号 
                    	<!-- 閲覧・編集用：既存の番号を表示 -->
                    	<span class="underline-text view-mode">20</span>
                    	
                    	<!-- 新規登録用：番号がないのでハイフンを表示 -->
                    	<span class="edit-mode register-only">
                    		<span class="underline-text">-</span>
                    	</span>
               		</div>
            	</div>
            </div>
            
            <!-- 取引先 -->
	            <div class="header-item item-partner">
	                取引先 
	                <!-- 閲覧用 -->
	                <span class="underline-text partner-name view-mode">(株)〇〇</span>
	                <!-- 新規登録用：テキスト入力欄 -->
	                <input type="text" class="edit-mode header-input partner-input" value="">
	            </div>
            
            <!-- 右上ボタンエリア -->
	            <div class="button-group">
					<!-- 閲覧モードの時に見えるボタン -->
	                <button class="btn btn-edit view-mode">編集</button>
	                <button class="btn btn-delete view-mode">削除</button>
	                
	                <!-- 新規登録・編集モードの時に見えるボタン -->
	                <button class="btn btn-save edit-mode">登録</button>
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
                <tr>
					<!-- 借方勘定科目 -->
                    <td class="col-subject">
						<span class="view-mode">接待交際費</span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
					
					<!-- 借方金額 -->
                    <td class="col-amount">
						<span class="view-mode">&yen;100,000</span>
						<!-- 金額入力欄は右寄せ(right)にする -->
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
					
					<!-- 貸方勘定科目 -->
                    <td class="col-subject">
						<span class="view-mode">未払金</span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
					
					<!-- 貸方金額 -->
                    <td class="col-amount">
						<span class="view-mode">&yen;100,000</span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                </tr>
                
                <tr>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                </tr>
               
                <tr>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                </tr>
                
                 <tr>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                </tr>
                
                 <tr>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                    <td class="col-subject">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field" value="">
					</td>
                    <td class="col-amount">
						<span class="view-mode"></span>
						<input type="text" class="edit-mode input-field text-right" value="">
					</td>
                </tr>
                
                <!-- 合計行 -->
                <tr class="total-row">
					
					<!-- 借方合計金額エリア -->
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
						<!-- 閲覧用、編集用の時は計算された金額 -->
						<span class="view-mode">&yen;100,000</span>
						
						<!-- 新規登録用の時は、初期値として\0を出す -->
						<span class="edit-mode register-only">&yen;0</span>
					</td>
					
					<!-- 貸方合計金額エリア -->
                    <td class="col-subject">合計</td>
                    <td class="col-amount-total">
						<span class="view-mode">&yen;100,000</span>
						<span class="edit-mode register-only">&yen;0</span>
					</td>
                </tr>
            </tbody>
        </table>
        
        <!-- 行追加ボタンエリア（貸方の合計金額の下に配置） -->
        <div class="add-row-container">
            <button class="btn-add-row">＋ 行を追加</button>
        </div> 
        

         <!-- 備考エリア -->
        <div class="note-container">
            <label for="note-text" class="note-label">備考：</label>
            
            <!-- 閲覧用 -->
            <textarea id="note-text" class="note-textarea view-mode" rows="3" readonly>懇親会費 クレジットカード使用 (月末引落)</textarea>
            
            <!-- 新規登録用 -->
            <textarea id="note-text-edit" class="note-textarea edit-mode" rows="3"></textarea>
        </div>
      </div>

        <!-- 削除確認ポップアップ（閲覧・編集共通） -->
        <div class="popup-overlay delete-layout">
            <div class="delete-popup-box">
                <button class="popup-close delete-popup-close">❌</button>
                <p class="popup-title"><span class="text-danger">削除</span>しますか？</p>
                <div class="popup-btn-group">
                    <button class="popup-btn">はい</button>
                    <button class="popup-btn">いいえ</button>
                </div>
            </div>
        </div>
        
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
		<div class="popup-overlay leave-popup-layout">
			<div class="leave-popup-box">
				<button class="popup-close">❌</button>
				<div class="leave-massage-container">
					<p class="leave-text">編集中の内容は削除されます。</p>
					<p class="leave-text">よろしいですか？</p>
				</div>
				
				<div class="popup-btn-group">
					<a href="index.html" class="popup-btn btn-ok-link">はい</a>
					<button class="popup-btn">いいえ</button>
				</div>
				
			</div>
		</div>

    

</body>
</html>
