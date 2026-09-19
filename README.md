# 驾校培训与考试管理系统

Java 项目开发与实践课程小组项目。

当前仓库先保存作业 1 的设计基线、数据库脚本、用例图和 E-R 图，后续在此仓库共同完成实体层、Mapper、页面、Service、Controller 和整合测试。

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

## 数据库导入

在独立的 MySQL 开发环境执行以下一种方式：

```sql
SOURCE <仓库绝对路径>/database/all.sql;
```

或按顺序执行 `01_schema.sql`、`02_base_seed.sql`、`03_questions_seed.sql`、`04_verify.sql`。脚本会使用数据库 `driving_school`，导入前请确认它不是已有业务数据。

目前 SQL 已完成静态检查，尚待真实 MySQL 环境导入验证；演示密码的待处理项见协作约定。退款样例状态已同步修正；已导入旧版的开发库可执行 `database/migrations/20260918_fix_demo_refund.sql`，新建库只需导入最新 `all.sql`。

## 协作约定

- 新功能先建分支和 Pull Request，不直接覆盖 `main`。
- 结构、字段、状态、接口语义改变时，先在组内说明，再同步 SQL、设计文档和代码。
- 不提交个人数据库密码、Token 或 IDE 临时文件。
- 截图、演示和报告只使用真实运行结果。
