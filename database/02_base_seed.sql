USE driving_school_v12;
SET NAMES utf8mb4;

-- 角色与用户
INSERT INTO role VALUES (1,'ADMIN','管理员'),(2,'ACADEMIC','教务'),(3,'FINANCE','财务'),(4,'COACH','教练'),(5,'STUDENT','学员');
INSERT INTO user_account (user_id,username,password_hash,real_name,phone) VALUES
(1,'admin','$2b$12$p7aIAB5EIn.2nfvy9T/ZRuLmjrWeLRfbRtmm5jbON2Y4fzTcfkZs.','管理员','13800000001'),
(2,'academic','$2b$12$p7aIAB5EIn.2nfvy9T/ZRuLmjrWeLRfbRtmm5jbON2Y4fzTcfkZs.','教务示例','13800000002'),
(3,'finance','$2b$12$p7aIAB5EIn.2nfvy9T/ZRuLmjrWeLRfbRtmm5jbON2Y4fzTcfkZs.','财务示例','13800000003'),
(4,'coach','$2b$12$p7aIAB5EIn.2nfvy9T/ZRuLmjrWeLRfbRtmm5jbON2Y4fzTcfkZs.','教练示例','13800000004'),
(5,'student','$2b$12$p7aIAB5EIn.2nfvy9T/ZRuLmjrWeLRfbRtmm5jbON2Y4fzTcfkZs.','学员示例','13800000005'),
(6,'student_refund','$2b$12$p7aIAB5EIn.2nfvy9T/ZRuLmjrWeLRfbRtmm5jbON2Y4fzTcfkZs.','退学示例','13800000006');
INSERT INTO user_role VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,5),(2,4);
INSERT INTO menu (role_id,menu_code,menu_name,route_path,sort_no) VALUES
(1,'users','用户管理','/users',1),(1,'roles','角色管理','/roles',2),(1,'menus','菜单管理','/menus',3),
(2,'enrollments','报名审核','/enrollments',1),(2,'training','培训安排','/training',2),(2,'exams','考试审核','/exams',3),(2,'withdrawals','退费审核','/withdrawals',4),
(3,'enrollments','缴费登记','/enrollments',1),(3,'withdrawals','退款登记','/withdrawals',2),
(4,'training','我的培训','/training',1),(4,'exams','题目与考试','/exams',2),
(5,'enrollments','我的报名','/enrollments',1),(5,'training','培训预约','/training',2),(5,'exams','模拟考试','/exams',3),(5,'withdrawals','退学退费','/withdrawals',4);

-- 报名及缴费
INSERT INTO enrollment (enrollment_id,student_user_id,license_type,required_amount,planned_hours,status,submitted_at,reviewed_by,reviewed_at,review_note,paid_amount,payment_method,payment_voucher,paid_by,paid_at) VALUES
(1,5,'C1',4800,40,'ACTIVE','2026-09-14 09:00:00',2,'2026-09-14 10:00:00','审核通过',4800,'线下转账','PAY-DEMO-001',3,'2026-09-14 11:00:00'),
(2,6,'C2',5000,40,'WITHDRAWN','2026-09-14 09:00:00',2,'2026-09-14 10:00:00','审核通过',5000,'线下转账','PAY-DEMO-002',3,'2026-09-14 11:00:00');

-- 培训车辆与预约
INSERT INTO training_vehicle VALUES (1,'粤A学001','C1',1),(2,'粤A学002','C2',1);
INSERT INTO training_booking (booking_id,student_user_id,coach_user_id,vehicle_id,planned_start,planned_end,location,status,created_by,requested_at,assigned_by,assigned_at,actual_start,actual_end,valid_hours,result_note) VALUES
(1,5,4,1,'2026-09-15 09:00:00','2026-09-15 11:00:00','一号训练场','COMPLETED',2,'2026-09-14 12:00:00',2,'2026-09-14 14:00:00','2026-09-15 09:00:00','2026-09-15 11:00:00',2,'完成基础训练');
INSERT INTO training_booking (planned_start,planned_end,location,created_by) VALUES
('2026-10-01 09:00:00','2026-10-01 11:00:00','一号训练场',2),
('2026-10-01 14:00:00','2026-10-01 16:00:00','一号训练场',2);

-- 退学及退款
INSERT INTO withdrawal_request (withdrawal_id,enrollment_id,reason,requested_at,status,reviewed_by,reviewed_at,review_note,approved_refund_amount,refund_amount,refund_method,refund_voucher,refunded_by,refunded_at) VALUES
(1,2,'个人时间安排变化','2026-09-16 09:00:00','REFUNDED',2,'2026-09-16 10:00:00','未参加培训，全额退费',5000,5000,'线下转账','REFUND-DEMO-001',3,'2026-09-16 11:00:00');
