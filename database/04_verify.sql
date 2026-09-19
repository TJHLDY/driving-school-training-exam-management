USE driving_school_v12;

-- 表数量
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';
-- 角色数量
SELECT COUNT(*) AS role_count FROM role;
-- 题目数量
SELECT COUNT(*) AS question_count FROM question_bank WHERE exam_id = 1 AND enabled = 1;
-- 答卷题数和分数
SELECT a.attempt_id, COUNT(d.answer_id) AS answer_count, SUM(d.score_awarded) AS detail_score, a.score
FROM exam_attempt a LEFT JOIN exam_answer d ON d.attempt_id=a.attempt_id GROUP BY a.attempt_id;
-- 跨考试错误题目
SELECT COUNT(*) AS invalid_exam_question FROM exam_answer d JOIN exam_attempt a ON a.attempt_id=d.attempt_id JOIN question_bank q ON q.question_id=d.question_id WHERE q.exam_id<>a.exam_id;
-- 退款与报名状态
SELECT COUNT(*) AS invalid_refund FROM withdrawal_request w JOIN enrollment e ON e.enrollment_id=w.enrollment_id
WHERE w.approved_refund_amount>e.paid_amount OR (w.status='REFUNDED' AND (e.status<>'WITHDRAWN' OR w.refund_amount<>w.approved_refund_amount));
-- 培训资源冲突
SELECT COUNT(*) AS training_conflicts FROM training_booking a JOIN training_booking b ON a.booking_id<b.booking_id
AND a.planned_start<b.planned_end AND b.planned_start<a.planned_end
WHERE a.status IN ('PENDING','ASSIGNED','COMPLETED') AND b.status IN ('PENDING','ASSIGNED','COMPLETED')
AND (a.student_user_id=b.student_user_id OR a.coach_user_id=b.coach_user_id OR a.vehicle_id=b.vehicle_id);
-- 多角色菜单
SELECT DISTINCT m.route_path FROM user_role ur JOIN menu m ON m.role_id=ur.role_id WHERE ur.user_id=2 AND m.enabled=1 ORDER BY m.route_path;
