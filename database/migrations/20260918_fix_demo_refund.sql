-- 修正旧版退款演示数据
USE driving_school;
START TRANSACTION;

UPDATE withdrawal_request wr
JOIN student_profile sp ON sp.student_id = wr.student_id
JOIN user_account ua ON ua.user_id = sp.user_id
JOIN enrollment e ON e.student_id = sp.student_id
JOIN refund_record rr ON rr.withdrawal_id = wr.withdrawal_id
SET wr.status = 'REFUNDED',
    sp.enrollment_status = 'WITHDRAWN',
    e.status = 'WITHDRAWN'
WHERE wr.withdrawal_id = 1
  AND sp.student_id = 3
  AND ua.username = 'demo_multi'
  AND e.enrollment_id = 3
  AND rr.voucher_no = 'REF-20260914-001'
  AND rr.refund_status = 'SUCCESS'
  AND rr.refund_amount = 2600.00
  AND wr.approved_refund_amount = 2600.00
  AND wr.status = 'APPROVED'
  AND sp.enrollment_status = 'ACTIVE'
  AND e.status = 'ACTIVE';

COMMIT;
