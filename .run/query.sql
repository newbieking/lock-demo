# ==>  Preparing: UPDATE seller_account SET balance = balance - ?, version = version + 1 WHERE seller_id = ? AND balance >= ? AND version = ?
#     ==> Parameters: 350.00(BigDecimal), 1001(Long), 350.00(BigDecimal), 0(Integer)


UPDATE seller_account
SET balance = balance - ?,
    version = version + 1
WHERE seller_id = 1001
  AND balance >= ?
  AND version = ?;


select balance, version
from seller_account
where seller_id=1001;

