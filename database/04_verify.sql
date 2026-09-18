-- 验证查询
USE driving_school;

-- 表数量
SELECT COUNT(*) AS expected_table_count_22
FROM information_schema.tables
WHERE table_schema = 'driving_school' AND table_type = 'BASE TABLE';

-- 角色
SELECT COUNT(*) AS expected_role_count_5 FROM role;
SELECT ua.username, COUNT(ur.role_id) AS role_count
FROM user_account ua JOIN user_role ur ON ua.user_id = ur.user_id
GROUP BY ua.user_id, ua.username HAVING COUNT(ur.role_id) > 1;

-- 题库与快照
SELECT COUNT(*) AS expected_enabled_question_count_100 FROM question_bank WHERE enabled = 1;
SELECT me.exam_name, COUNT(meq.exam_question_id) AS expected_snapshot_count_100
FROM mock_exam me LEFT JOIN mock_exam_question meq ON me.exam_id = meq.exam_id
WHERE me.status = 'PUBLISHED'
GROUP BY me.exam_id, me.exam_name;

-- 答卷明细
SELECT ea.attempt_id, COUNT(eaa.answer_id) AS answer_count, COUNT(DISTINCT eaa.exam_question_id) AS distinct_question_count
FROM exam_attempt ea LEFT JOIN exam_attempt_answer eaa ON ea.attempt_id = eaa.attempt_id
WHERE ea.attempt_status = 'SUBMITTED'
GROUP BY ea.attempt_id
HAVING COUNT(eaa.answer_id) <> 20 OR COUNT(DISTINCT eaa.exam_question_id) <> 20;

-- 答卷得分
SELECT ea.attempt_id, ea.score, COALESCE(SUM(eaa.score_awarded),0) AS detail_score
FROM exam_attempt ea LEFT JOIN exam_attempt_answer eaa ON ea.attempt_id = eaa.attempt_id
WHERE ea.attempt_status = 'SUBMITTED'
GROUP BY ea.attempt_id, ea.score
HAVING ea.score <> COALESCE(SUM(eaa.score_awarded),0);

-- 培训学时
SELECT record_id, valid_hours FROM training_record WHERE record_status = 'COMPLETED' AND valid_hours < 0;

-- 退款金额
SELECT wr.withdrawal_id, rr.refund_amount, COALESCE(SUM(pr.payment_amount),0) AS paid_amount
FROM withdrawal_request wr
JOIN refund_record rr ON rr.withdrawal_id = wr.withdrawal_id AND rr.refund_status = 'SUCCESS'
JOIN enrollment e ON e.student_id = wr.student_id
LEFT JOIN payment_record pr ON pr.enrollment_id = e.enrollment_id AND pr.payment_status = 'SUCCESS'
GROUP BY wr.withdrawal_id, rr.refund_amount
HAVING rr.refund_amount > COALESCE(SUM(pr.payment_amount),0);

-- 资源冲突
SELECT s.slot_id, a.coach_id, a.vehicle_id, COUNT(*) AS assignment_count
FROM training_assignment a
JOIN training_booking b ON b.booking_id = a.booking_id
JOIN training_slot s ON s.slot_id = b.slot_id
WHERE a.assignment_status = 'ASSIGNED'
GROUP BY s.slot_id, a.coach_id, a.vehicle_id
HAVING COUNT(*) > 1;
