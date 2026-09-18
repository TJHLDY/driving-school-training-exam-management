USE driving_school;

-- 基础数据
INSERT INTO role (role_id, role_code, role_name, description) VALUES
(1,'ADMIN','管理员','维护用户、角色和菜单'),
(2,'STUDENT','学员','报名、预约、练习和退学申请'),
(3,'ACADEMIC','教务','审核报名、分配资源和审核考试'),
(4,'COACH','教练','维护题库和登记训练'),
(5,'FINANCE','财务','登记缴费和退款');

INSERT INTO menu (menu_id, menu_code, menu_name, menu_path, sort_no, enabled) VALUES
(1,'USER_ROLE','用户与角色','/admin/users',10,1),
(2,'ENROLLMENT','报名管理','/enrollments',20,1),
(3,'PAYMENT','缴费管理','/payments',30,1),
(4,'SLOT','培训时段','/training/slots',40,1),
(5,'BOOKING','培训预约','/training/bookings',50,1),
(6,'QUESTION','题库管理','/questions',60,1),
(7,'MOCK_EXAM','模拟考试','/mock-exams',70,1),
(8,'RESULT','成绩查询','/exam-results',80,1),
(9,'WITHDRAWAL','退学退费','/withdrawals',90,1),
(10,'PROFILE','个人中心','/profile',100,1);

INSERT INTO role_menu (role_id, menu_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(2,2),(2,5),(2,7),(2,8),(2,9),(2,10),
(3,2),(3,4),(3,5),(3,7),(3,9),(3,10),
(4,5),(4,6),(4,7),(4,10),
(5,3),(5,9),(5,10);

INSERT INTO user_account (user_id, username, password_hash, real_name, phone, account_status) VALUES
(1,'admin','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','系统管理员','13800000001','ACTIVE'),
(2,'academic01','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','王教务','13800000002','ACTIVE'),
(3,'finance01','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','赵财务','13800000003','ACTIVE'),
(4,'coach01','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','李教练','13800000004','ACTIVE'),
(5,'coach02','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','周教练','13800000005','ACTIVE'),
(6,'student01','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','张同学','13800000006','ACTIVE'),
(7,'student02','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','陈同学','13800000007','ACTIVE'),
(8,'demo_multi','$2a$10$7EqJtq98hPqEX7fNZaFWoOaSe9JfA5D4zwHh6F8vR0W1XvW8vS4.a','多角色演示账号','13800000008','ACTIVE');

INSERT INTO user_role (user_id, role_id) VALUES
(1,1),(2,3),(3,5),(4,4),(5,4),(6,2),(7,2),(8,2),(8,3);

INSERT INTO student_profile (student_id, user_id, id_card_no, gender, birth_date, enrollment_status) VALUES
(1,6,'110101200401010011','男','2004-01-01','ACTIVE'),
(2,7,'110101200402020022','女','2004-02-02','PENDING'),
(3,8,'110101200403030033','男','2004-03-03','ACTIVE');

INSERT INTO coach_profile (coach_id, user_id, coach_no, license_type, employment_status) VALUES
(1,4,'C001','C1','ACTIVE'),
(2,5,'C002','C1','ACTIVE');

INSERT INTO training_vehicle (vehicle_id, plate_no, vehicle_model, license_type, vehicle_status) VALUES
(1,'京A·D1001','桑塔纳手动挡','C1','ACTIVE'),
(2,'京A·D1002','朗逸手动挡','C1','ACTIVE');

INSERT INTO training_package (package_id, package_code, package_name, price, planned_hours, enabled) VALUES
(1,'C1-BASIC','C1基础班',3000.00,40.0,1),
(2,'C1-PLUS','C1强化班',3800.00,52.0,1);

INSERT INTO enrollment (enrollment_id, student_id, package_id, submitted_at, status, reviewed_by, reviewed_at, review_note, required_amount, active_at) VALUES
(1,1,1,'2026-09-01 09:00:00','ACTIVE',2,'2026-09-01 10:00:00','资料齐全',3000.00,'2026-09-01 14:00:00'),
(2,2,2,'2026-09-15 09:00:00','SUBMITTED',NULL,NULL,NULL,3800.00,NULL),
(3,3,1,'2026-09-02 09:00:00','ACTIVE',2,'2026-09-02 10:00:00','资料齐全',3000.00,'2026-09-02 14:00:00');

INSERT INTO payment_record (payment_id, enrollment_id, received_by, payment_amount, payment_method, voucher_no, paid_at, payment_status) VALUES
(1,1,3,3000.00,'CASH','PAY-20260901-001','2026-09-01 13:30:00','SUCCESS'),
(2,3,3,3000.00,'TRANSFER','PAY-20260902-001','2026-09-02 13:30:00','SUCCESS');

INSERT INTO training_slot (slot_id, slot_start, slot_end, location, capacity, slot_status, created_by) VALUES
(1,'2026-09-18 09:00:00','2026-09-18 11:00:00','训练场A区',1,'OPEN',2),
(2,'2026-09-19 14:00:00','2026-09-19 16:00:00','训练场B区',1,'OPEN',2),
(3,'2026-09-10 09:00:00','2026-09-10 11:00:00','训练场A区',1,'CLOSED',2);

INSERT INTO training_booking (booking_id, student_id, slot_id, booking_status, requested_at) VALUES
(1,1,1,'PENDING','2026-09-16 09:00:00'),
(2,3,3,'COMPLETED','2026-09-08 09:00:00');

INSERT INTO training_assignment (assignment_id, booking_id, coach_id, vehicle_id, assigned_by, assigned_at, assignment_status) VALUES
(1,2,1,1,2,'2026-09-08 10:00:00','COMPLETED');

INSERT INTO training_record (record_id, assignment_id, recorded_by, actual_start, actual_end, valid_hours, record_status, remark) VALUES
(1,1,4,'2026-09-10 09:00:00','2026-09-10 11:00:00',2.0,'COMPLETED','倒车入库练习完成');

INSERT INTO withdrawal_request (withdrawal_id, student_id, reason, requested_at, status, reviewed_by, reviewed_at, review_note, approved_refund_amount) VALUES
(1,3,'个人时间安排变化','2026-09-12 09:00:00','APPROVED',2,'2026-09-13 10:00:00','扣除已完成训练费用后退还余额',2600.00);

INSERT INTO refund_record (refund_id, withdrawal_id, paid_by, refund_amount, refund_method, voucher_no, refunded_at, refund_status) VALUES
(1,1,3,2600.00,'TRANSFER','REF-20260914-001','2026-09-14 11:00:00','SUCCESS');
