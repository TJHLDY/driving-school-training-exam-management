-- 数据库结构
CREATE DATABASE IF NOT EXISTS driving_school DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE driving_school;

CREATE TABLE user_account (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20),
  account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_user_status CHECK (account_status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(30) NOT NULL UNIQUE,
  role_name VARCHAR(50) NOT NULL,
  description VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu (
  menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  menu_code VARCHAR(50) NOT NULL UNIQUE,
  menu_name VARCHAR(50) NOT NULL,
  menu_path VARCHAR(120) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id),
  CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES role(role_id),
  CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES menu(menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE student_profile (
  student_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  id_card_no VARCHAR(30) NOT NULL UNIQUE,
  gender VARCHAR(10),
  birth_date DATE,
  enrollment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coach_profile (
  coach_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  coach_no VARCHAR(30) NOT NULL UNIQUE,
  license_type VARCHAR(20) NOT NULL,
  employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT fk_coach_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_vehicle (
  vehicle_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plate_no VARCHAR(20) NOT NULL UNIQUE,
  vehicle_model VARCHAR(60) NOT NULL,
  license_type VARCHAR(20) NOT NULL,
  vehicle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_package (
  package_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  package_code VARCHAR(30) NOT NULL UNIQUE,
  package_name VARCHAR(80) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  planned_hours DECIMAL(5,1) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  CONSTRAINT ck_package_price CHECK (price >= 0),
  CONSTRAINT ck_package_hours CHECK (planned_hours > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE enrollment (
  enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  package_id BIGINT NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(300),
  required_amount DECIMAL(10,2) NOT NULL,
  active_at DATETIME,
  CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id),
  CONSTRAINT fk_enrollment_package FOREIGN KEY (package_id) REFERENCES training_package(package_id),
  CONSTRAINT fk_enrollment_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_enrollment_status CHECK (status IN ('DRAFT','SUBMITTED','REJECTED','APPROVED','ACTIVE','WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_record (
  payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enrollment_id BIGINT NOT NULL,
  received_by BIGINT NOT NULL,
  payment_amount DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(20) NOT NULL,
  voucher_no VARCHAR(50) NOT NULL UNIQUE,
  paid_at DATETIME NOT NULL,
  payment_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  CONSTRAINT fk_payment_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(enrollment_id),
  CONSTRAINT fk_payment_receiver FOREIGN KEY (received_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_payment_amount CHECK (payment_amount > 0),
  CONSTRAINT ck_payment_status CHECK (payment_status IN ('SUCCESS','VOID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_slot (
  slot_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slot_start DATETIME NOT NULL,
  slot_end DATETIME NOT NULL,
  location VARCHAR(100) NOT NULL,
  capacity INT NOT NULL DEFAULT 1,
  slot_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_by BIGINT NOT NULL,
  CONSTRAINT fk_slot_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_slot_time CHECK (slot_end > slot_start),
  CONSTRAINT ck_slot_capacity CHECK (capacity > 0),
  CONSTRAINT ck_slot_status CHECK (slot_status IN ('OPEN','CLOSED','EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_booking (
  booking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  slot_id BIGINT NOT NULL,
  booking_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancelled_at DATETIME,
  cancel_reason VARCHAR(300),
  CONSTRAINT fk_booking_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id),
  CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES training_slot(slot_id),
  CONSTRAINT uq_booking_student_slot UNIQUE (student_id, slot_id),
  CONSTRAINT ck_booking_status CHECK (booking_status IN ('PENDING','ASSIGNED','CANCELLED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_assignment (
  assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL UNIQUE,
  coach_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  assignment_status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
  CONSTRAINT fk_assignment_booking FOREIGN KEY (booking_id) REFERENCES training_booking(booking_id),
  CONSTRAINT fk_assignment_coach FOREIGN KEY (coach_id) REFERENCES coach_profile(coach_id),
  CONSTRAINT fk_assignment_vehicle FOREIGN KEY (vehicle_id) REFERENCES training_vehicle(vehicle_id),
  CONSTRAINT fk_assignment_assigner FOREIGN KEY (assigned_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_assignment_status CHECK (assignment_status IN ('ASSIGNED','CANCELLED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL UNIQUE,
  recorded_by BIGINT NOT NULL,
  actual_start DATETIME NOT NULL,
  actual_end DATETIME NOT NULL,
  valid_hours DECIMAL(5,1) NOT NULL,
  record_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
  remark VARCHAR(300),
  CONSTRAINT fk_record_assignment FOREIGN KEY (assignment_id) REFERENCES training_assignment(assignment_id),
  CONSTRAINT fk_record_recorder FOREIGN KEY (recorded_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_record_time CHECK (actual_end > actual_start),
  CONSTRAINT ck_record_hours CHECK (valid_hours >= 0),
  CONSTRAINT ck_record_status CHECK (record_status IN ('COMPLETED','INVALID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE question_bank (
  question_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(30) NOT NULL,
  stem VARCHAR(500) NOT NULL,
  option_a VARCHAR(300) NOT NULL,
  option_b VARCHAR(300) NOT NULL,
  option_c VARCHAR(300) NOT NULL,
  option_d VARCHAR(300) NOT NULL,
  correct_option CHAR(1) NOT NULL,
  explanation VARCHAR(500) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_question_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_question_option CHECK (correct_option IN ('A','B','C','D'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mock_exam (
  exam_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_name VARCHAR(100) NOT NULL,
  question_count INT NOT NULL DEFAULT 20,
  duration_minutes INT NOT NULL DEFAULT 20,
  pass_score INT NOT NULL DEFAULT 90,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  submitted_by BIGINT NOT NULL,
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(300),
  published_at DATETIME,
  CONSTRAINT fk_exam_submitter FOREIGN KEY (submitted_by) REFERENCES user_account(user_id),
  CONSTRAINT fk_exam_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_exam_status CHECK (status IN ('DRAFT','SUBMITTED','REJECTED','PUBLISHED','CLOSED')),
  CONSTRAINT ck_exam_count CHECK (question_count = 20),
  CONSTRAINT ck_exam_duration CHECK (duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mock_exam_question (
  exam_question_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  position_no INT NOT NULL,
  stem_snapshot VARCHAR(500) NOT NULL,
  option_a_snapshot VARCHAR(300) NOT NULL,
  option_b_snapshot VARCHAR(300) NOT NULL,
  option_c_snapshot VARCHAR(300) NOT NULL,
  option_d_snapshot VARCHAR(300) NOT NULL,
  correct_option_snapshot CHAR(1) NOT NULL,
  explanation_snapshot VARCHAR(500) NOT NULL,
  CONSTRAINT fk_exam_question_exam FOREIGN KEY (exam_id) REFERENCES mock_exam(exam_id),
  CONSTRAINT fk_exam_question_source FOREIGN KEY (question_id) REFERENCES question_bank(question_id),
  CONSTRAINT uq_exam_question UNIQUE (exam_id, question_id),
  CONSTRAINT uq_exam_position UNIQUE (exam_id, position_no),
  CONSTRAINT ck_exam_snapshot_option CHECK (correct_option_snapshot IN ('A','B','C','D'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_attempt (
  attempt_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  attempt_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deadline_at DATETIME NOT NULL,
  submitted_at DATETIME,
  score INT,
  correct_count INT,
  passed TINYINT,
  CONSTRAINT fk_attempt_exam FOREIGN KEY (exam_id) REFERENCES mock_exam(exam_id),
  CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id),
  CONSTRAINT ck_attempt_status CHECK (attempt_status IN ('IN_PROGRESS','SUBMITTED','EXPIRED')),
  CONSTRAINT ck_attempt_deadline CHECK (deadline_at > started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_attempt_answer (
  answer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  exam_question_id BIGINT NOT NULL,
  selected_option CHAR(1),
  is_correct TINYINT,
  score_awarded INT NOT NULL DEFAULT 0,
  answered_at DATETIME,
  CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempt(attempt_id),
  CONSTRAINT fk_answer_exam_question FOREIGN KEY (exam_question_id) REFERENCES mock_exam_question(exam_question_id),
  CONSTRAINT uq_attempt_question UNIQUE (attempt_id, exam_question_id),
  CONSTRAINT ck_selected_option CHECK (selected_option IS NULL OR selected_option IN ('A','B','C','D')),
  CONSTRAINT ck_answer_score CHECK (score_awarded IN (0,5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE withdrawal_request (
  withdrawal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(300),
  approved_refund_amount DECIMAL(10,2),
  CONSTRAINT fk_withdrawal_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id),
  CONSTRAINT fk_withdrawal_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_withdrawal_status CHECK (status IN ('SUBMITTED','REJECTED','APPROVED','REFUNDED')),
  CONSTRAINT ck_withdrawal_amount CHECK (approved_refund_amount IS NULL OR approved_refund_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refund_record (
  refund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  withdrawal_id BIGINT NOT NULL,
  paid_by BIGINT NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL,
  refund_method VARCHAR(20) NOT NULL,
  voucher_no VARCHAR(50) NOT NULL UNIQUE,
  refunded_at DATETIME NOT NULL,
  refund_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  CONSTRAINT fk_refund_withdrawal FOREIGN KEY (withdrawal_id) REFERENCES withdrawal_request(withdrawal_id),
  CONSTRAINT fk_refund_payer FOREIGN KEY (paid_by) REFERENCES user_account(user_id),
  CONSTRAINT ck_refund_amount CHECK (refund_amount > 0),
  CONSTRAINT ck_refund_status CHECK (refund_status IN ('SUCCESS','VOID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_enrollment_student_status ON enrollment(student_id, status);
CREATE INDEX idx_slot_time_status ON training_slot(slot_start, slot_end, slot_status);
CREATE INDEX idx_booking_slot_status ON training_booking(slot_id, booking_status);
CREATE INDEX idx_assignment_resource_status ON training_assignment(coach_id, vehicle_id, assignment_status);
CREATE INDEX idx_question_enabled ON question_bank(enabled, category);
CREATE INDEX idx_attempt_student_status ON exam_attempt(student_id, attempt_status);
CREATE INDEX idx_withdrawal_student_status ON withdrawal_request(student_id, status);
