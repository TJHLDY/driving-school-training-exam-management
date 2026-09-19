# 后端 API 契约

本文件对应 JDK 17、Spring Boot 3.3.5、MyBatis XML 和 MySQL 8 后端。所有 URL 以 `/api` 开头，响应统一为：

```json
{"code": 200, "message": "success", "data": {}}
```

成功时 `code=200`；错误时 HTTP 状态和 `code` 使用 400、401、403、404、409 或 500。分页从 1 开始，默认 `pageSize=10`，最大 100。

## 统一约定

- Java ID 使用 `Long`，JSON 中统一序列化为十进制字符串。
- Java 金额使用 `BigDecimal`，JSON 中统一序列化为两位小数字符串。
- 字段使用 SQL `snake_case`、Java/JSON `camelCase`。
- 时间使用 `Asia/Shanghai`，请求和响应格式为 `yyyy-MM-dd'T'HH:mm:ss+08:00`。
- 登录成功后 JWT 写入 `access_token` HttpOnly Cookie；所有写请求还必须带 `X-XSRF-TOKEN`。
- 先调用 `GET /api/auth/csrf` 获取 token；登录、注册、退出及全部 POST/PUT/DELETE 都受 CSRF 校验。
- 权限由 Controller/Security 与 Service 双重校验，不能只依赖页面隐藏按钮。

## 认证与账号

| URL | 方法 | 参数 | 返回值 | 允许角色 | 页面模板 | Mapper |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/auth/csrf` | GET | 无 | `{token,headerName,parameterName}` | 公开 | `common/csrf` | 无 |
| `/api/auth/register` | POST | `username,password,realName,phone,gender,idNumber` | `LoginResponse` | 公开 | `auth/register` | `insertUser,findRoleIdByCode,insertUserRole,findMenusByUserId` |
| `/api/auth/login` | POST | `username,password` | 用户、角色、去重菜单、过期秒数；设置 JWT Cookie | 公开 | `auth/login` | `findById,findRoleCodesByUserId,findMenusByUserId` |
| `/api/auth/logout` | POST | 无 | 空 | 已登录 | `common/menu` | 无 |
| `/api/auth/me` | GET | 无 | 当前用户、角色、菜单 | 已登录 | `common/menu` | `findById,findRoleCodesByUserId,findMenusByUserId` |
| `/api/admin/users` | GET | `keyword,page,pageSize` | 分页用户 | ADMIN | `admin/users` | `findUsers,countUsers,findRoleCodesByUserId` |
| `/api/admin/users` | POST | 用户资料、密码、`roleCodes` | 用户 | ADMIN | `admin/users/form` | `insertUser,insertUserRole` |
| `/api/admin/users/{id}` | PUT | `realName,phone,gender,idNumber,coachLicense` | 用户 | ADMIN | `admin/users/form` | `updateUser` |
| `/api/admin/users/{id}/roles` | PUT | `roleCodes` | 空 | ADMIN | `admin/users/roles` | `deleteUserRoles,insertUserRole,countUsersByRoleCode` |
| `/api/admin/users/{id}/enabled` | PUT | `enabled` | 空 | ADMIN | `admin/users` | `updateUserEnabled` |
| `/api/admin/users/{id}/password` | PUT | `password` | 空 | ADMIN | `admin/users/password` | `updatePassword` |
| `/api/admin/roles` | GET/POST | `roleCode,roleName` | 角色列表/角色 | ADMIN | `admin/roles` | `findRoles,insertRole,updateRole` |
| `/api/admin/roles/{id}` | PUT | `roleCode,roleName` | 角色 | ADMIN | `admin/roles/form` | `updateRole` |
| `/api/admin/menus` | GET/POST | `roleId,menuCode,menuName,routePath,sortNo,enabled` | 菜单列表/菜单 | ADMIN | `admin/menus` | `findMenus,insertMenu,updateMenu,findMenuById` |
| `/api/admin/menus/{id}` | PUT | 同菜单 POST | 菜单 | ADMIN | `admin/menus/form` | `updateMenu` |

公开注册固定只写 `STUDENT`。密码通过 BCrypt 12 轮哈希后写入 `password_hash`，接口不回传哈希。

## 报名、审核和缴费

| URL | 方法 | 参数 | 返回值 | 允许角色 | 页面模板 | Mapper |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/enrollments` | POST | `licenseType` | 报名 | STUDENT | `enrollments/form` | `findByStudentUserIdForUpdate,insert,updateStudentSubmission,findById` |
| `/api/enrollments` | GET | `status,page,pageSize` | 分页报名 | 学员本人；ACADEMIC、FINANCE、ADMIN | `enrollments/list` | `findPage,count` |
| `/api/enrollments/{id}` | GET | 无 | 报名 | 学员本人；ACADEMIC、FINANCE、ADMIN | `enrollments/detail` | `findById` |
| `/api/enrollments/{id}/review` | POST | `approved,requiredAmount,plannedHours,reviewNote` | 报名 | ACADEMIC | `enrollments/review` | `findByIdForUpdate,review,findById` |
| `/api/enrollments/{id}/payment` | POST | `paymentMethod,paymentVoucher` | 报名 | FINANCE | `enrollments/payment` | `findByIdForUpdate,pay,findById` |

金额和学时由教务审核写入，学员接口不接受价格字段。缴费只允许 `APPROVED`，后端按 `required_amount` 全额入账并转为 `ACTIVE`；重复登记返回已有结果。

## 培训与学时

| URL | 方法 | 参数 | 返回值 | 允许角色 | 页面模板 | Mapper |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/training/vehicles` | GET | `enabled` | 车辆列表 | 已登录 | `training/vehicles` | `findVehicles` |
| `/api/training/vehicles` | POST | `plateNo,licenseType,enabled` | 车辆 | ACADEMIC | `training/vehicles/form` | `insertVehicle` |
| `/api/training/vehicles/{id}` | PUT | 同车辆 POST | 车辆 | ACADEMIC | `training/vehicles/form` | `findVehicleByIdForUpdate,updateVehicle` |
| `/api/training/bookings` | GET | `status,page,pageSize` | 分页预约 | 本人学员/教练；ACADEMIC、ADMIN | `training/bookings` | `findPage,count` |
| `/api/training/bookings/{id}` | GET | 无 | 预约 | 本人学员/教练；ACADEMIC、ADMIN | `training/bookings/detail` | `findBookingById` |
| `/api/training/bookings/open` | POST | `plannedStart,plannedEnd,location` | OPEN 时段 | ACADEMIC | `training/bookings/open` | `insertOpenBooking,findBookingById` |
| `/api/training/bookings/{id}/claim` | POST | 无 | PENDING 预约 | STUDENT | `training/bookings/claim` | `findByIdForUpdate,countOverlaps,claim,findById` |
| `/api/training/bookings/{id}/assign` | POST | `coachUserId,vehicleId` | ASSIGNED 预约 | ACADEMIC | `training/bookings/assign` | 相关账号/车辆/预约锁查询、`countOverlaps,assign,findById` |
| `/api/training/bookings/{id}/cancel` | POST | 无 | CANCELLED 预约 | 本人学员；ACADEMIC | `training/bookings/cancel` | `findBookingByIdForUpdate,cancel` |
| `/api/training/bookings/{id}/complete` | POST | `actualStart,actualEnd,validHours,resultNote` | COMPLETED 预约 | 负责教练 | `training/bookings/complete` | `findBookingByIdForUpdate,complete` |
| `/api/training/hours/{studentUserId}` | GET | 无 | `{studentUserId,completedHours}` | 本人；ACADEMIC、ADMIN；相关教练 | `training/hours` | `count,sumCompletedHours` |

冲突条件统一为 `新开始 < 已有结束 AND 新结束 > 已有开始`，检查学员、教练和车辆。认领使用条件更新 `OPEN + student_user_id IS NULL` 并检查影响行数。

## 模拟考试

| URL | 方法 | 参数 | 返回值 | 允许角色 | 页面模板 | Mapper |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/exams` | GET | `status,page,pageSize` | 分页考试 | 学员看已发布；教练看本人；ACADEMIC、ADMIN 看全部 | `exams/list` | `findExamPage,countExams,findPublishedExamPage,countPublishedExams` |
| `/api/exams/{id}` | GET | 无 | 考试详情 | 本人教练、学员可见发布考试、ACADEMIC、ADMIN | `exams/detail` | `findExamById,findEnabledQuestions` |
| `/api/exams` | POST | `examName` | DRAFT | COACH | `exams/form` | `insertExam,findExamById` |
| `/api/exams/{id}` | PUT | `examName` | 考试 | 创建教练 | `exams/form` | `findExamByIdForUpdate,updateExam,findExamById` |
| `/api/exams/{id}/questions` | POST | `category,stem,optionA-D,correctOption,explanation,enabled` | 题目 | 创建教练 | `exams/questions/form` | `findExamByIdForUpdate,insertQuestion` |
| `/api/exams/questions/{id}` | PUT | 同题目 POST | 题目 | 创建教练 | `exams/questions/form` | `findQuestionById,findExamByIdForUpdate,updateQuestion` |
| `/api/exams/questions/{id}` | DELETE | 无 | 空 | 创建教练 | `exams/questions` | `findQuestionById,findExamByIdForUpdate,disableQuestion` |
| `/api/exams/{id}/submit` | POST | 无 | SUBMITTED | 创建教练 | `exams/detail` | `countEnabledQuestions,submitExam,findExamById` |
| `/api/exams/{id}/review` | POST | `approved,reviewNote` | PUBLISHED/REJECTED | ACADEMIC | `exams/review` | `reviewExam,findExamById` |
| `/api/exams/{id}/close` | POST | 无 | CLOSED | ACADEMIC | `exams/detail` | `closeExam,findExamById` |
| `/api/exams/{id}/attempts` | POST | 无 | 原卷或新答卷 | STUDENT | `exams/attempt` | `findAttemptByExamAndStudent,insertAttempt,findRandomQuestions,insertAnswer,findAttemptQuestions` |
| `/api/exams/attempts/{id}` | GET | 无 | 答卷；进行中隐藏答案/解析 | 本人；ACADEMIC、ADMIN | `exams/attempt` | `findAttemptByIdForUpdate,findAttemptQuestions` |
| `/api/exams/attempts/{id}/answers/{questionId}` | PUT | `selectedOption` | 当前答卷 | 答卷本人 | `exams/attempt` | `findAnswer,saveAnswer,findAttemptQuestions` |
| `/api/exams/attempts/{id}/submit` | POST | 无 | 分数和答题解析 | 答卷本人 | `exams/result` | `gradeAnswers,calculateScore,finishAttempt,findAttemptQuestions` |
| `/api/exams/attempts/{id}/result` | GET | 无 | 分数和答题解析 | 本人；ACADEMIC、ADMIN | `exams/result` | `findAttemptByIdForUpdate,findAttemptQuestions` |

每个教练任务固定 20 题、每题 5 分、20 分钟、90 分及格。刷新或重复开考返回原答卷；超时自动按已保存答案结算为 `EXPIRED`。交卷同时回填 `exam_answer.score_awarded`，保证明细分数与总分一致。

## 退学与退费

| URL | 方法 | 参数 | 返回值 | 允许角色 | 页面模板 | Mapper |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/withdrawals` | POST | `reason` | 退学申请 | STUDENT | `withdrawals/form` | `findByStudentUserIdForUpdate,countUnfinishedStarted,cancelFutureBookings,findByEnrollmentIdForUpdate,insert/resubmit` |
| `/api/withdrawals` | GET | `status,page,pageSize` | 分页记录 | 本人；ACADEMIC、FINANCE、ADMIN | `withdrawals/list` | `findPage,count` |
| `/api/withdrawals/{id}` | GET | 无 | 记录 | 本人；ACADEMIC、FINANCE、ADMIN | `withdrawals/detail` | `findById` |
| `/api/withdrawals/{id}/review` | POST | `approved,approvedRefundAmount,reviewNote` | APPROVED/REJECTED | ACADEMIC | `withdrawals/review` | `findByIdForUpdate,review` |
| `/api/withdrawals/{id}/refund` | POST | `refundMethod,refundVoucher` | REFUNDED | FINANCE | `withdrawals/refund` | `findByIdForUpdate,refund,markWithdrawn` |

退款只能登记一次且必须等于核定额度；零元核定允许零元登记。退款与报名转 `WITHDRAWN` 在同一事务完成。
