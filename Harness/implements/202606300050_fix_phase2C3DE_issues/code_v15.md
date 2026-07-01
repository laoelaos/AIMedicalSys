# 实现报告（v15）

## 概述

实现 P02/E06 — DrugFacade 注入。在 `PrescriptionAuditServiceImpl` 和 `PrescriptionAssistServiceImpl` 中注入 `DrugFacade` 接口，分别在 `audit()` 和 `assist()` 流程中遍历处方条目调用 `DrugFacade.findByDrugCode()` 获取药品信息，异常时记录 WARN 日志，不阻断主流程。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `modules/prescription/src/main/java/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 增加 DrugFacade、drugFacadeTimeout 字段；构造器增加两参数；audit() 中调用 enrichWithDrugInfo()；新增 enrichWithDrugInfo() 方法 |
| 修改 | `modules/prescription/src/main/java/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 增加 Logger、DrugFacade、drugFacadeTimeout 字段；构造器增加两参数；assist() 中调用 enrichWithDrugInfo()；新增 enrichWithDrugInfo() 方法 |
| 修改 | `modules/prescription/src/test/java/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 增加 @Mock DrugFacade；setUp() 构造器传参增加 drugFacade, 2L |
| 修改 | `modules/prescription/src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 增加 @Mock DrugFacade；setUp() 构造器传参增加 drugFacade, 2L |

## 编译验证

`mvn compile -pl modules/prescription -am` 编译成功。

`mvn test -pl modules/prescription -am` 全部 155 个测试通过（0 Failure, 0 Error, 0 Skipped）。

## 设计偏差说明

无偏差。设计规格全部实现：
- DrugFacade 字段及构造器注入 ✓
- `@Value("${prescription.drug-facade.timeout:2}")` 注入超时配置 ✓
- audit() 中 persistAuditRecord 前调用 enrichWithDrugInfo ✓
- assist() 中 parseDraftItems 后、剂量告警循环前调用 enrichWithDrugInfo ✓
- 异常捕获并记录 WARN 日志，格式严格对齐设计规范 ✓
- 返回值当前不使用，仅预留数据通路 ✓
