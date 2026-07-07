package otameshirenshuu;


	import java.io.IOException;
	import java.util.List;

	import javax.servlet.ServletException;
	import javax.servlet.annotation.WebServlet;
	import javax.servlet.http.HttpServlet;
	import javax.servlet.http.HttpServletRequest;
	import javax.servlet.http.HttpServletResponse;



	@WebServlet("/detail")
	public class DetailServlet extends HttpServlet {

		@Override
	    protected void doGet(HttpServletRequest request, HttpServletResponse response)
	            throws ServletException, IOException {

	        // 1. 画面から送られてきたモード（mode=view や mode=edit）を取得する
	        // 指定がなければ「view（閲覧）」にする
	        String mode = request.getParameter("mode");
	        if (mode == null || mode.isEmpty()) {
	            mode = "view";
	        }
	        request.setAttribute("currentMode", mode);

	        List<java.util.Map<String, String>> detailList = new java.util.ArrayList<>();


	        // 2. 新規作成(new)の場合は、データを空っぽにする
	        if ("new".equals(mode)) {

	            request.setAttribute("slipId", null);
	            request.setAttribute("slipDate", ""); // カレンダーを空にするため空文字
	            request.setAttribute("partnerName", null);
	            request.setAttribute("description", null);
	            request.setAttribute("note", "");

	            for (int i = 0; i < 5; i++) {
	            	java.util.Map<String,String> emptyRow = new java.util.HashMap<>();
		            emptyRow.put("debitSubject", "");
		            emptyRow.put("debitAmount", "");
		            emptyRow.put("creditSubject", "");
		            emptyRow.put("creditAmount", "");
		            detailList.add(emptyRow);
	            }


	        } else {

	        // 既存のデータを表示する場合（view や edit）は、今まで通りのダミーデータを入れておく
	            request.setAttribute("slipId", "20");
	            request.setAttribute("slipDate", "2026-06-13");
	            request.setAttribute("partnerName", "(株)〇〇");
	            request.setAttribute("description", "懇親会");
	            request.setAttribute("note", "クレジットカード使用 (月末引落)");


	            java.util.Map<String, String> row = new java.util.HashMap<>();
	            row.put("debitSubject", "接待交際費");
	            row.put("debitAmount", "100,000");
	            row.put("creditSubject", "未払金");
	            row.put("creditAmount", "100,000");
	            detailList.add(row);

	        }

	        // 3. 完成した明細リストを「detailList」という名前でえ画面に引き渡す
	        request.setAttribute("detailList", detailList);

	        // 4. 画面（JSPファイル）を表示する
	        request.getRequestDispatcher("/detail.jsp").forward(request, response);
	    }

	    // 登録ボタン（POST送信）が押されたときの処理
	    @Override
	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	            throws ServletException, IOException {

	    	request.setCharacterEncoding("UTF-8");

	    	//画面のデータを受け取る
	    	String slipDate = request.getParameter("slipDate");
	    	String partnerName = request.getParameter("partnerName");
	    	String description = request.getParameter("description");
	    	String note = request.getParameter("note");


	    	String[] debitSubjects = request.getParameterValues("debitSubject");
	    	String[] debitAmounts = request.getParameterValues("debitAmount");
	    	String[] creditSubjects = request.getParameterValues("creditSubject");
	    	String[] creditAmounts = request.getParameterValues("creditAmount");

	    	//エラーメッセージを格納するリスト
	    	List<String> errorMessages = new java.util.ArrayList<>();

	    	//画面に返すための明細リスト
	    	List<java.util.Map<String, String>> detailList = new java.util.ArrayList<>();

	    	int debitTotal = 0;
	    	int creditTotal = 0;

	    	//明細行の件数を取得（debitAmountsの長さを基準にする）
	    	int rowCount = (debitAmounts != null) ? debitAmounts.length : 0;

	    	//明細行のループ処理と「数字チェック」
	    	boolean hasFormatError = false;


	    	for(int i = 0; i < rowCount; i++) {
	    		String dSub = (debitSubjects != null && debitSubjects.length > i) ? debitSubjects[i] : "";
	    		String dAmt = (debitAmounts != null && debitAmounts.length > i) ? debitAmounts[i] : "";
	    		String cSub = (creditSubjects != null && creditSubjects.length > i) ? creditSubjects[i] : "";
	    		String cAmt = (creditAmounts != null && creditAmounts.length > i) ? creditAmounts[i] : "";

	    		//カンマなどが含まれている場合に備えてチェック前に除去
	    		String cleanDAmt = dAmt.replaceAll("[^0-9]", "");
	    		String cleanCAmt = cAmt.replaceAll("[^0-9]", "");


	    		//借方金額チェック
	    		if (!dAmt.isEmpty()) {
	    			if (dAmt.matches("^[0-9,]+$")) {
	    				if (!cleanDAmt.isEmpty()) {
	    					debitTotal += Integer.parseInt(cleanDAmt);
	    				}
	    			} else if (!hasFormatError) {
	    				errorMessages.add("借方金額に半角数字以外が入力されています。");
	    				hasFormatError = true;
	    			}
	    		}

	    		//貸方金額チェック
	    		if (!cAmt.isEmpty()) {
	    			if(cAmt.matches("^[0-9,]+$")) {
	    				if (!cleanCAmt.isEmpty()) {
	    					creditTotal += Integer.parseInt(cleanCAmt);
	    				}
	    			} else if (!hasFormatError) {
	    				errorMessages.add("貸方金額に半角数字以外が入力されています。");
	    				hasFormatError = true;
	    			}
	    		}

	    		java.util.Map<String, String> row = new java.util.HashMap<>();
	            row.put("debitSubject", dSub);
	            row.put("debitAmount", dAmt);
	            row.put("creditSubject", cSub);
	            row.put("creditAmount", cAmt);
	            detailList.add(row);
	    	}

	    	//貸借金額一致チェック（数字フォーマットエラーがない場合のみ実行）
	    	if (!hasFormatError && debitTotal != creditTotal) {
	    		errorMessages.add("借方と貸方の合計金額が一致しません。");
	    	}

	    	//エラーがあれば、入力内容を保持したまま編集画面へ戻す
	    	if (!errorMessages.isEmpty()) {
	    		request.setAttribute("errors", errorMessages);
	    		request.setAttribute("currentMode", "edit");	//	エラー時は「編集モード」を開く

	    		request.setAttribute("slipDate", slipDate);
	    		request.setAttribute("partnerName", partnerName);
	    		request.setAttribute("description", description);
	    		request.setAttribute("note", note);
	    		request.setAttribute("detailList", detailList);

	    		//テスト用コンソール出力
	    		System.out.println("エラー発生　元の画面へ送り返します。");
	    		for (String err : errorMessages) {
	    			System.out.println("エラー内容：" + err);
	    		}

	    		request.getRequestDispatcher("/detail.jsp").forward(request, response);
	    		return;
	    	}


	    	//テスト　受け取ったデータがJavaに届いているかコンソールに出力
	    	System.out.println("サーブレット登録データ受信テスト");
	    	System.out.println("日付 : " + slipDate);
	    	System.out.println("取引先 : " + partnerName);
	    	System.out.println("購入物 :" + description);
	    	System.out.println("借方合計：￥" + String.format("%,d", debitTotal));
	    	System.out.println("貸方合計：￥" + String.format("%,d", creditTotal));
	    	System.out.println("備考 : " + note);

	    	//登録成功。入力した内容をそのまま閲覧モードで表示する
	    	request.setAttribute("currentMode", "view");
	    	request.setAttribute("slipId", "-");
	    	request.setAttribute("slipDate", slipDate);
	    	request.setAttribute("partnerName", partnerName);
	    	request.setAttribute("description", description);
	    	request.setAttribute("note", note);
	    	request.setAttribute("detailList", detailList);

	    	request.getRequestDispatcher("/detail.jsp").forward(request, response);
	    }
	}
