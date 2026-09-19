CREATE DATABASE driving_school_v12 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE driving_school_v12;
SET NAMES utf8mb4;

-- 用户账号表
CREATE TABLE user_account (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  real_name VARCHAR(30) NOT NULL,
  phone VARCHAR(20) NOT NULL UNIQUE,
  gender VARCHAR(10),
  id_number VARCHAR(20) UNIQUE,
  coach_license VARCHAR(40) UNIQUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 角色表
CREATE TABLE role (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(20) NOT NULL UNIQUE,
  role_name VARCHAR(30) NOT NULL
) ENGINE=InnoDB;

-- 用户角色表
CREATE TABLE user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES user_account(user_id),
  FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB;

-- 角色菜单表
CREATE TABLE menu (
  menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_code VARCHAR(40) NOT NULL,
  menu_name VARCHAR(40) NOT NULL,
  route_path VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (role_id, menu_code),
  FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB;

-- 报名及缴费表
CREATE TABLE enrollment (
  enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_user_id BIGINT NOT NULL UNIQUE,
  license_type VARCHAR(2) NOT NULL,
  required_amount DECIMAL(10,2) NOT NULL,
  planned_hours DECIMAL(6,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(200),
  paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  payment_method VARCHAR(20),
  payment_voucher VARCHAR(80) UNIQUE,
  paid_by BIGINT,
  paid_at DATETIME,
  FOREIGN KEY (student_user_id) REFERENCES user_account(user_id),
  FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  FOREIGN KEY (paid_by) REFERENCES user_account(user_id),
  CHECK (license_type IN ('C1','C2')),
  CHECK (required_amount > 0 AND planned_hours > 0),
  CHECK (paid_amount >= 0 AND paid_amount <= required_amount),
  CHECK (status IN ('SUBMITTED','REJECTED','APPROVED','ACTIVE','WITHDRAWN')),
  CHECK (status NOT IN ('ACTIVE','WITHDRAWN') OR
    (paid_amount = required_amount AND paid_by IS NOT NULL AND paid_at IS NOT NULL AND payment_voucher IS NOT NULL))
) ENGINE=InnoDB;

-- 培训车辆表
CREATE TABLE training_vehicle (
  vehicle_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plate_no VARCHAR(20) NOT NULL UNIQUE,
  license_type VARCHAR(2) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (license_type IN ('C1','C2'))
) ENGINE=InnoDB;

-- 培训预约及结果表
CREATE TABLE training_booking (
  booking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_user_id BIGINT,
  coach_user_id BIGINT,
  vehicle_id BIGINT,
  planned_start DATETIME NOT NULL,
  planned_end DATETIME NOT NULL,
  location VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_by BIGINT NOT NULL,
  requested_at DATETIME,
  assigned_by BIGINT,
  assigned_at DATETIME,
  actual_start DATETIME,
  actual_end DATETIME,
  valid_hours DECIMAL(5,2),
  result_note VARCHAR(200),
  cancelled_at DATETIME,
  FOREIGN KEY (student_user_id) REFERENCES user_account(user_id),
  FOREIGN KEY (coach_user_id) REFERENCES user_account(user_id),
  FOREIGN KEY (vehicle_id) REFERENCES training_vehicle(vehicle_id),
  FOREIGN KEY (created_by) REFERENCES user_account(user_id),
  FOREIGN KEY (assigned_by) REFERENCES user_account(user_id),
  INDEX (student_user_id, planned_start),
  INDEX (coach_user_id, planned_start),
  INDEX (vehicle_id, planned_start),
  CHECK (planned_end > planned_start),
  CHECK (status IN ('OPEN','PENDING','ASSIGNED','COMPLETED','CANCELLED')),
  CHECK (status <> 'OPEN' OR student_user_id IS NULL),
  CHECK (status NOT IN ('PENDING','ASSIGNED','COMPLETED') OR student_user_id IS NOT NULL),
  CHECK (status NOT IN ('ASSIGNED','COMPLETED') OR (coach_user_id IS NOT NULL AND vehicle_id IS NOT NULL)),
  CHECK (actual_end IS NULL OR actual_start IS NULL OR actual_end > actual_start),
  CHECK (valid_hours IS NULL OR valid_hours >= 0),
  CHECK (status <> 'COMPLETED' OR (actual_start IS NOT NULL AND actual_end IS NOT NULL AND valid_hours IS NOT NULL)),
  CHECK (valid_hours IS NULL OR actual_start IS NULL OR actual_end IS NULL OR valid_hours * 3600 <= TIMESTAMPDIFF(SECOND, actual_start, actual_end))
) ENGINE=InnoDB;

-- 模拟考试表
CREATE TABLE mock_exam (
  exam_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_name VARCHAR(80) NOT NULL,
  question_count INT NOT NULL DEFAULT 20,
  duration_minutes INT NOT NULL DEFAULT 20,
  pass_score INT NOT NULL DEFAULT 90,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  submitted_by BIGINT NOT NULL,
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(200),
  published_at DATETIME,
  FOREIGN KEY (submitted_by) REFERENCES user_account(user_id),
  FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  CHECK (question_count = 20 AND duration_minutes = 20 AND pass_score = 90),
  CHECK (status IN ('DRAFT','SUBMITTED','REJECTED','PUBLISHED','CLOSED'))
) ENGINE=InnoDB;

-- 考试题目表
CREATE TABLE question_bank (
  question_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  category VARCHAR(30) NOT NULL,
  stem VARCHAR(500) NOT NULL,
  option_a VARCHAR(200) NOT NULL,
  option_b VARCHAR(200) NOT NULL,
  option_c VARCHAR(200) NOT NULL,
  option_d VARCHAR(200) NOT NULL,
  correct_option CHAR(1) NOT NULL,
  explanation VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (exam_id) REFERENCES mock_exam(exam_id),
  CHECK (correct_option IN ('A','B','C','D'))
) ENGINE=InnoDB;

-- 学员答卷表
CREATE TABLE exam_attempt (
  attempt_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  student_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  started_at DATETIME NOT NULL,
  deadline_at DATETIME NOT NULL,
  submitted_at DATETIME,
  score INT,
  UNIQUE (exam_id, student_user_id),
  FOREIGN KEY (exam_id) REFERENCES mock_exam(exam_id),
  FOREIGN KEY (student_user_id) REFERENCES user_account(user_id),
  CHECK (status IN ('IN_PROGRESS','SUBMITTED','EXPIRED')),
  CHECK (deadline_at > started_at),
  CHECK (score IS NULL OR score BETWEEN 0 AND 100),
  CHECK (status = 'IN_PROGRESS' OR (score IS NOT NULL AND submitted_at IS NOT NULL))
) ENGINE=InnoDB;

-- 答题明细表
CREATE TABLE exam_answer (
  answer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  position_no INT NOT NULL,
  selected_option CHAR(1),
  score_awarded INT NOT NULL DEFAULT 0,
  answered_at DATETIME,
  UNIQUE (attempt_id, question_id),
  UNIQUE (attempt_id, position_no),
  FOREIGN KEY (attempt_id) REFERENCES exam_attempt(attempt_id),
  FOREIGN KEY (question_id) REFERENCES question_bank(question_id),
  CHECK (position_no BETWEEN 1 AND 20),
  CHECK (selected_option IS NULL OR selected_option IN ('A','B','C','D')),
  CHECK (score_awarded IN (0,5))
) ENGINE=InnoDB;

-- 退学及退款表
CREATE TABLE withdrawal_request (
  withdrawal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enrollment_id BIGINT NOT NULL UNIQUE,
  reason VARCHAR(200) NOT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(200),
  approved_refund_amount DECIMAL(10,2),
  refund_amount DECIMAL(10,2),
  refund_method VARCHAR(20),
  refund_voucher VARCHAR(80) UNIQUE,
  refunded_by BIGINT,
  refunded_at DATETIME,
  FOREIGN KEY (enrollment_id) REFERENCES enrollment(enrollment_id),
  FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  FOREIGN KEY (refunded_by) REFERENCES user_account(user_id),
  CHECK (status IN ('SUBMITTED','REJECTED','APPROVED','REFUNDED')),
  CHECK (approved_refund_amount IS NULL OR approved_refund_amount >= 0),
  CHECK (refund_amount IS NULL OR (refund_amount >= 0 AND approved_refund_amount IS NOT NULL AND refund_amount <= approved_refund_amount)),
  CHECK (status NOT IN ('APPROVED','REFUNDED') OR approved_refund_amount IS NOT NULL),
  CHECK (status <> 'REFUNDED' OR (refund_amount IS NOT NULL AND refund_amount = approved_refund_amount AND refunded_by IS NOT NULL AND refunded_at IS NOT NULL AND refund_voucher IS NOT NULL))
) ENGINE=InnoDB;
