# 实现报告（v4）

## 概述
在 common 模块 entity 包中新建 `DosageStandard` JPA 实体类，继承 `BaseEntity`，按药品编码+给药途径+年龄/体重分级定义单次/日剂量上限标准，并声明数据库复合索引。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | AIMedical/backend/common/src/main/java/com/aimedical/common/entity/DosageStandard.java | 实现 DosageStandard 实体类型 |

## 编译验证
未执行编译验证

## 设计偏差说明
无偏差。所有类型签名、字段约束、索引声明、继承关系均严格按详细设计 v4 编码。
