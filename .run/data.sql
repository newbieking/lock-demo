-- 插入测试商户账户
INSERT INTO seller_account (seller_id, balance)
VALUES
    (1001, 5000.00),
    (1002, 3000.00),
    (1003, 10000.00);

-- 插入测试发票数据
INSERT INTO seller_invoice (seller_id, invoice_no, amount, status, version)
VALUES
    (1001, 'INV202603280001', 120.50, 1, 0),
    (1001, 'INV202603280002', 350.00, 1, 0),
    (1002, 'INV202603280003', 880.80, 1, 0),
    (1003, 'INV202603280004', 1500.00, 1, 0);

-- 插入测试操作日志（空数据也可以，这里给一条演示）
INSERT INTO seller_invoice_log (invoice_id, action, content)
VALUES
    (1, 'INIT', '发票创建');