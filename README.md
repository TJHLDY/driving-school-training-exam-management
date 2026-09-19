# 驾校培训与考试管理系统

Java 项目开发与实践课程小组项目。

当前统一使用 **12 表简化版**，替代旧的 22 表设计。仓库保存作业 1、SQL、用例图和 E-R 图，后续在此共同完成实体、Mapper、页面、Service、Controller 和整合测试，当前不包含已完成的 Java 工程。

## 成员与分工

| 成员 | 学号 | 主要负责 |
| --- | --- | --- |
| 谭卓谦（组长） | 241548153 | 需求、数据库、作业 1、设计变更协调 |
| 陈志鹏 | 241616118 | 作业 2：实体、Mapper、Mapper XML、页面 |
| 刘梓烨 | 241541217 | 作业 3：Service、Controller、认证权限、工程整合 |

具体协作规则见 [docs/发给组员_后续开发约定.md](docs/发给组员_后续开发约定.md)。

## 仓库结构

```text
database/  MySQL 建表、基础数据、题库与验证脚本
docs/      作业 1 设计基线、协作约定与当前 Word 作业
diagrams/  可编辑 Draw.io 图和 PNG 图
```

E-R 图在 `diagrams/overall-er.drawio` 中同时提供整体图和五个模块分图；Word 包含综合总览和各模块详图。同名实体代表同一张表，连线和基数按当前 SQL 核对。

当前作业文档：[作业 1（12 表版）](docs/组长241548153_谭卓谦_作业01_12表简化版.docx)。字段与短用例见 [设计基线](docs/作业01内容基线.md)。`.drawio` 用 diagrams.net 网站的“从设备打开”载入。

## 数据库导入

在独立的 MySQL 开发环境执行以下一种方式：

```sql
SOURCE <仓库绝对路径>/database/all.sql;
```

或按顺序执行 `01_schema.sql`、`02_base_seed.sql`、`03_questions_seed.sql`、`04_verify.sql`，两种方式二选一。使用 MySQL 8.0.16 或以上。脚本新建独立数据库 `driving_school_v12`，不删除旧库；已有同名库时报错，必须停止并确认情况，不可忽略错误继续执行。

已在隔离 MySQL 8.4.7 实例完成真实导入，核对 12 张表、5 种角色、100 道教学演示题及答卷、退款样例，8 项非法写入测试通过。演示账号及密码见协作约定。尚未实现的页面、Service 和并发流程需在后续开发中测试。

旧版资料和 SQL 可在 Git 历史中恢复，不再作为当前开发依据；不将旧版迁移脚本用于新库。

## 协作约定

- 新功能先建分支和 Pull Request，不直接覆盖 `main`。
- 结构、字段、状态、接口语义改变时，先在组内说明，再同步 SQL、设计文档和代码。
- 不提交个人数据库密码、Token 或 IDE 临时文件。
- 截图、演示和报告只使用真实运行结果。

## 后端工程（JDK 17）

当前仓库已建立 Spring Boot 后端，根包为 `com.example.drivingschool`，包含 `entity`、`mapper`、`service`、`controller`、`dto`、`config` 目录。数据库仍以 `database/01_schema.sql` 为准，不修改 12 表设计。

技术依赖固定在 `pom.xml`：Spring Boot 3.3.5、Java 17、Spring Security、MyBatis XML、MySQL 8、JJWT 0.12.6、BCrypt。

### 启动

1. 按 README 原有说明导入 `database/all.sql`。
2. 设置数据库环境变量，未设置时使用本机 `root` 和空密码：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/driving_school_v12?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的本地密码"
$env:JWT_SECRET="至少32字节的本地随机密钥"
```

3. 编译、测试和启动：

```powershell
mvn clean test
mvn spring-boot:run
```

生产或共享环境必须设置 `JWT_SECRET`，并将 `JWT_COOKIE_SECURE=true` 配合 HTTPS。未设置 secret 时进程会生成仅本次启动有效的随机密钥，重启后旧登录失效。

### 登录与 CSRF

1. `GET /api/auth/csrf`，同时保存响应的 `XSRF-TOKEN` Cookie 和 JSON 中的 token。
2. `POST /api/auth/login`，带 `X-XSRF-TOKEN` 与 `Content-Type: application/json`。
3. 登录成功后服务端设置 HttpOnly、SameSite=Strict 的 `access_token` Cookie。
4. 后续 POST、PUT、DELETE 请求继续带 `X-XSRF-TOKEN`。

密码使用 BCrypt 12 轮哈希存储；JWT 只保存用户标识，每次请求重新读取账号和角色，停用账号、角色变更立即生效。

### 已覆盖功能

- 账号注册、登录、退出、账号/角色/菜单维护，菜单按 `route_path` 去重。
- 报名提交、驳回重提、教务审核、财务一次全额缴费。
- 车辆、开放时段、学员认领、资源分配、取消、教练登记结果和累计学时。
- 教练考试任务与题库、教务审核发布、随机 20 题、保存答案、超时结算、判分与解析权限。
- 退学申请、未来预约取消、教务核定、财务一次退款和报名转 WITHDRAWN。
- 关键状态条件更新、行锁、跨表归属检查和时间冲突检查。

完整接口表见 [api_contract.md](api_contract.md)。
