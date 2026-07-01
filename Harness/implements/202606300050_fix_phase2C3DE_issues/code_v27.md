# 实现报告（v27）

## 概述

修复 `RegistrationEventListenerTest.java` 中 `JsonProcessingException(String)` protected 构造器调用导致的编译失败。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java` | 新增 `JsonParseException` import；替换 L130 抛出语句 |

## 编译验证

`mvn compile test-compile -pl modules/consultation -am -q` 通过。

## 设计偏差说明

无偏差。
