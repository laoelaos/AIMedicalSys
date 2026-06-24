# 实现报告（v1）

## 概述
为 User.java 的 password 字段添加 `@Column(nullable = false)` 注解，并将 schema.sql 中 sys_user 表的 password 列从 DEFAULT NULL 改为 NOT NULL。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java` | 第28行添加 `@Column(nullable = false)` |
| 修改 | `AIMedical/backend/application/src/main/resources/db/schema.sql` | 第16行 password 列 DEFAULT NULL → NOT NULL |

## 编译验证
`mvn compile -pl modules/common-module/common-module-impl -am` 编译成功，无错误。

## 设计偏差说明
无偏差。
