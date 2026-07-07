-- 振替伝票アプリ用のデータベース／テーブル定義とサンプルデータ
--
-- 通常はアプリ起動時に自動で作成・投入されるため、この SQL の手動実行は不要です。
-- phpMyAdmin（XAMPP Control Panel の MySQL「Admin」）や
-- コマンド（例: mysql -u root < schema.sql）で手動投入したい場合に利用してください。

CREATE DATABASE IF NOT EXISTS otameshirenshuu DEFAULT CHARACTER SET utf8mb4;
USE otameshirenshuu;

-- 伝票（ヘッダー）
CREATE TABLE IF NOT EXISTS slip (
    id           INT AUTO_INCREMENT PRIMARY KEY,   -- 伝票番号（自動採番）
    slip_date    VARCHAR(10)  NOT NULL,            -- 日付 yyyy-MM-dd
    partner_name VARCHAR(255) NOT NULL DEFAULT '', -- 取引先
    description  VARCHAR(255) NOT NULL DEFAULT '', -- 購入物
    note         TEXT                              -- 備考
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 明細行
CREATE TABLE IF NOT EXISTS entry (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    slip_id        INT NOT NULL,                     -- 伝票への参照
    line_no        INT NOT NULL,                     -- 行番号（表示順）
    debit_subject  VARCHAR(255) NOT NULL DEFAULT '', -- 借方勘定科目
    debit_amount   INT NOT NULL DEFAULT 0,           -- 借方金額
    credit_subject VARCHAR(255) NOT NULL DEFAULT '', -- 貸方勘定科目
    credit_amount  INT NOT NULL DEFAULT 0,           -- 貸方金額
    CONSTRAINT fk_entry_slip FOREIGN KEY (slip_id) REFERENCES slip(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- サンプルデータ（伝票番号1〜20）
INSERT INTO slip (slip_date, partner_name, description, note) VALUES
 ('2026-04-06','松本システム開発','システム開発費',NULL),
 ('2026-04-12','山口オートサービス','車両整備',NULL),
 ('2026-04-19','佐々木エナジー(株)','電気代',NULL),
 ('2026-04-21','山田コンサルティング','コンサルティング料',NULL),
 ('2026-04-23','吉田ベンディングサービス','飲料補充',NULL),
 ('2026-04-24','加藤法律事務所','顧問料',NULL),
 ('2026-04-30','小林通信(株)','通信費',NULL),
 ('2026-05-08','中村不動産','事務所家賃',NULL),
 ('2026-05-15','山本印刷(株)','印刷費',NULL),
 ('2026-05-16','伊藤デンタルクリニック','健康診断',NULL),
 ('2026-05-19','渡辺事務用品','文房具',NULL),
 ('2026-05-20','高橋テック(株)','PC周辺機器',NULL),
 ('2026-05-24','合同会社マツモト','消耗品',NULL),
 ('2026-05-25','田中ロジスティクス','配送料',NULL),
 ('2026-05-28','(有)サトウ商会','備品',NULL),
 ('2026-06-02','鈴木商事','仕入',NULL),
 ('2026-06-05','✕✕産業(株)','材料',NULL),
 ('2026-06-10','△△商店','事務用品',NULL),
 ('2026-06-12','▢▢(株)','モニター',NULL),
 ('2026-06-13','(株)〇〇','懇親会費',NULL);

INSERT INTO entry (slip_id, line_no, debit_subject, debit_amount, credit_subject, credit_amount) VALUES
 (1,1,'外注費',88000,'普通預金',88000),
 (2,1,'車両費',54000,'現金',54000),
 (3,1,'水道光熱費',19500,'普通預金',19500),
 (4,1,'支払手数料',220000,'普通預金',220000),
 (5,1,'福利厚生費',8200,'現金',8200),
 (6,1,'支払手数料',33000,'普通預金',33000),
 (7,1,'通信費',12400,'普通預金',12400),
 (8,1,'地代家賃',150000,'普通預金',150000),
 (9,1,'事務用品費',43200,'未払金',43200),
 (10,1,'福利厚生費',5500,'現金',5500),
 (11,1,'消耗品費',9800,'現金',9800),
 (12,1,'消耗品費',62000,'未払金',62000),
 (13,1,'消耗品費',4500,'現金',4500),
 (14,1,'荷造運賃',120000,'未払金',120000),
 (15,1,'消耗品費',15800,'現金',15800),
 (16,1,'仕入高',80000,'買掛金',80000),
 (17,1,'仕入高',7100,'現金',7100),
 (18,1,'消耗品費',3000,'現金',3000),
 (19,1,'消耗品費',50000,'現金',50000),
 (20,1,'接待交際費',100000,'未払金',100000);
