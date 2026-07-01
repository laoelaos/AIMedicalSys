# 测试报告（v15）

## 概述

为 P02/E06 — DrugFacade 注入编写单元测试。测试基于详细设计（`detail_v15.md`）中的行为契约，验证 DrugFacade 调用在 `PrescriptionAuditServiceImpl.audit()` 和 `PrescriptionAssistServiceImpl.assist()` 中的正确行为。

## 测试文件变更

### PrescriptionAuditServiceImplTest.java

**新增 4 个测试方法**：

| 测试方法 | 覆盖维度 | 验证点 |
|---------|---------|--------|
| `auditShouldCallDrugFacadeForEachItem` | 正常路径 | 每个处方条目独立调用 `drugFacade.findByDrugCode()`，传入正确的 drugId |
| `auditShouldNotBlockWhenDrugFacadeThrows` | 错误路径 | DrugFacade 抛出异常时，`audit()` 正常返回响应，不阻断主流程 |
| `auditShouldLogWarnWhenDrugFacadeFails` | 错误路径 | 异常时记录 WARN 日志，消息包含 "DrugFacade.findByDrugCode" |
| `auditShouldSkipItemsWithNullDrugId` | 边界条件 | 条目的 drugId 为 null 时跳过，不调用 drugFacade |

### PrescriptionAssistServiceImplTest.java

**新增 4 个测试方法**：

| 测试方法 | 覆盖维度 | 验证点 |
|---------|---------|--------|
| `assistShouldCallDrugFacadeForEachDraftItem` | 正常路径 | 每个解析出的处方条目调用 `drugFacade.findByDrugCode()` |
| `assistShouldNotBlockWhenDrugFacadeThrows` | 错误路径 | DrugFacade 异常时 `assist()` 正常返回，不阻断 |
| `assistShouldLogWarnWhenDrugFacadeFails` | 错误路径 | 异常时记录 WARN 日志 |
| `assistShouldSkipDraftItemsWithNullDrugId` | 边界条件 | 草稿中药条无 drugId 时跳过，不调用 drugFacade |

## 测试规范遵守情况

- [x] 基于行为契约编写，验证公开接口行为，不测实现细节
- [x] 每个行为契约至少一个正向用例
- [x] 覆盖维度：正常路径、边界条件、错误路径
- [x] 用例独立，不依赖执行顺序
- [x] 不修改编码 agent 的源码文件
- [x] 沿用项目已有测试风格（Mockito + JUnit 5 + Logback ListAppender）
