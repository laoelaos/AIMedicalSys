# 实现报告（v5）

## 概述

修改 `TriageServiceImplTest.java` 中的 `StubFallbackProvider` 测试桩和 `shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull` 测试方法，修复因 fallback 无法返回空列表导致的三路科室路由全空场景测试失败问题。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 1. `StubFallbackProvider` 增加 `returnEmpty` 标志字段及条件分支；2. 测试方法中设置 `fallbackProvider.returnEmpty = true` |

## 编译验证

通过：`mvn test-compile -pl modules/consultation -am -q` 无报错。

## 设计偏差说明

无偏差。严格按照 v5 详细设计实现。
