<%@ page contentType="text/html; charset=UTF-8" %>

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
            <div class="search-area">
            	<div class="search-box">
                	<input type="text" id="search-input" placeholder="検索（伝票番号、日付、取引先）">
            	</div>
                <button class="search-exec-btn" id="search-trigger-btn">検索</button>
            </div>
        	
			<a href="detail?mode=new" class="register-btn" style="text-decoration: none; display: inline-block;">
    			新規登録
			</a>

        </div>

        <!-- 伝票テーブル -->
        <table class="slip-table">
            <thead>
                <tr>
                    <th class="col-id sort-trigger">伝票番号<span class="sort-arrows">▲▼</span></th>
                    <th class="col-date sort-trigger">日付<span class="sort-arrows">▲▼</span></th>
                    <th class="col-partner">取引先（購入先）</th>
                    <th class="col-description">購入物</th>
                    <th class="col-amount col-amount-th">金額</th>
                    <th class="col-management-header" colspan="2">管理</th>
                </tr>
            </thead>
            <tbody>
                <!-- 明細データ -->
                <tr>
                    <td class="col-id">20</td>
                    <td class="col-date">2026/06/13</td>
                    <td class="col-partner">(株)〇〇</td>
                    <td class="col-description">懇親会費</td>
                    <td class="col-amount col-amount-td">¥100,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">19</td>
                    <td class="col-date">2026/06/12</td>
                    <td class="col-partner">▢▢(株)</td>
                    <td class="col-description">モニター</td>
                    <td class="col-amount col-amount-td">¥50,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">18</td>
                    <td class="col-date">2026/06/10</td>
                    <td class="col-partner">△△商店</td>
                    <td class="col-description">　</td>
                    <td class="col-amount col-amount-td">¥3,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">17</td>
                    <td class="col-date">2026/06/05</td>
                    <td class="col-partner">✕✕産業(株)</td>
                    <td class="col-description">　</td>
                    <td class="col-amount col-amount-td">¥7,100</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">16</td>
                    <td class="col-date">2026/06/02</td>
                    <td class="col-partner">鈴木商事</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥80,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">15</td>
                    <td class="col-date">2026/05/28</td>
                    <td class="col-partner">(有)サトウ商会</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥15,800</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">14</td>
                    <td class="col-date">2026/05/25</td>
                    <td class="col-partner">田中ロジスティクス</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥120,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">13</td>
                    <td class="col-date">2026/05/24</td>
                    <td class="col-partner">合同会社マツモト</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥4,500</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">12</td>
                    <td class="col-date">2026/05/20</td>
                    <td class="col-partner">高橋テック(株)</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥62,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">11</td>
                    <td class="col-date">2026/05/19</td>
                    <td class="col-partner">渡辺事務用品</td>
                    <td class="col-description">　</td>
                    <td class="col-amount col-amount-td">¥9,800</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">10</td>
                    <td class="col-date">2026/05/16</td>
                    <td class="col-partner">伊藤デンタルクリニック</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥5,500</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">9</td>
                    <td class="col-date">2026/05/15</td>
                    <td class="col-partner">山本印刷(株)</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥43,200</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">8</td>
                    <td class="col-date">2026/05/08</td>
                    <td class="col-partner">中村不動産</td>
                    <td class="col-description">　</td>
                    <td class="col-amount col-amount-td">¥150,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">7</td>
                    <td class="col-date">2026/04/30</td>
                    <td class="col-partner">小林通信(株)</td>
                    <td class="col-description">　</td>
                    <td class="col-amount col-amount-td">¥12,400</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">6</td>
                    <td class="col-date">2026/04/24</td>
                    <td class="col-partner">加藤法律事務所</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥33,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">5</td>
                    <td class="col-date">2026/04/23</td>
                    <td class="col-partner">吉田ベンディングサービス</td>
                    <td class="col-description">　</td>
                    <td class="col-amount col-amount-td">¥8,200</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">4</td>
                    <td class="col-date">2026/04/21</td>
                    <td class="col-partner">山田コンサルティング</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥220,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">3</td>
                    <td class="col-date">2026/04/19</td>
                    <td class="col-partner">佐々木エナジー(株)</td>
                    <td class="col-description"></td>
                    <td class="col-amount col-amount-td">¥19,500</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">2</td>
                    <td class="col-date">2026/04/12</td>
                    <td class="col-partner">山口オートサービス</td>
                    <td class="col-description"> </td>
                    <td class="col-amount col-amount-td">¥54,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
                <tr>
                    <td class="col-id">1</td>
                    <td class="col-date">2026/04/06</td>
                    <td class="col-partner">松本システム開発</td>
                    <td class="col-description"> </td>
                    <td class="col-amount col-amount-td">¥88,000</td>
                    <td class="col-detail"><a href="detail.jsp" class="detail-link">明細</a></td>
                    <td class="col-delete-cell"><button class="btn-list-delete">削除</button></td>
                </tr>
            </tbody>
        </table>
    </div>
    
    <div class="popup-overlay delete-layout">
            <div class="delete-popup-box">
                <button class="popup-close delete-popup-close">❌</button>
                <p class="popup-title">この伝票を<span class="text-danger">削除</span>しますか？</p>
                <div class="popup-btn-group">
                    <button class="popup-btn btn-yes">はい</button>
                    <button class="popup-btn btn-no">いいえ</button>
                </div>
            </div>
    </div>

</body>
</html>
