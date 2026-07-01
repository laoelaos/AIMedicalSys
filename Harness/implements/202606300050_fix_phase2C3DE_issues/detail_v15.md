# 详细设计（v15）

## 概述

实现 P02/E06 — DrugFacade 注入：在 `PrescriptionAuditServiceImpl` 和 `PrescriptionAssistServiceImpl` 中构造器注入 `DrugFacade` 接口，在遍历处方条目时调用 `DrugFacade.findByDrugCode()` 获取药品信息，异常/超时时捕获并记录 WARN 日志，不阻断主流程。

此任务仅涉及 prescription 模块，不修改 `DrugFacade` 接口、`DrugInfo` record 以及两个 Service 的业务逻辑主流程。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/prescription/src/main/java/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 增加 DrugFacade 字段、构造器参数、audit() 中调用 |
| `modules/prescription/src/main/java/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 增加 DrugFacade 字段、构造器参数、assist() 中调用 |
| `modules/prescription/src/test/java/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | setUp() 构造器传参增加 drugFacade mock |
| `modules/prescription/src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | setUp() 构造器传参增加 drugFacade mock |

## 类型定义

### PrescriptionAuditServiceImpl（修改）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl`
**职责**：处方审核 Service 实现。增加 DrugFacade 注入，在 audit() 流程中遍历 prescriptionItems 调用 DrugFacade.findByDrugCode() 获取药品信息（旁路信息查询，失败不阻断主流程）。

**新增字段**：
- `private final DrugFacade drugFacade` — 药品信息查询门面
- `private final long drugFacadeTimeout` — 通过 `@Value("${prescription.drug-facade.timeout:2}")` 注入超时阈值（单位秒，用于 future.get 或预留扩展）

**构造方式变更**：
- 当前 7 参构造器 → 增加 `DrugFacade drugFacade, @Value("${prescription.drug-facade.timeout:2}") long drugFacadeTimeout` 变为 9 参

**修改方法**：
- `audit(AuditRequest request)`：在现有流程中（`persistAuditRecord()` 调用之前），遍历 `request.getPrescriptionItems()`，对每个 item 的 `getDrugId()` 调用 `drugFacade.findByDrugCode()`。调用包裹 try-catch，超时/异常时记录 WARN 日志，继续处理下一项。返回值（DrugInfo）当前不使用，仅为后续功能预留数据通路。
- 新增辅助方法如 `private void enrichWithDrugInfo(AuditRequest request)` 封装上述遍历逻辑

**新增依赖**：
- `import com.aimedical.modules.commonmodule.drug.DrugFacade`
- `import com.aimedical.modules.commonmodule.drug.DrugInfo`

### PrescriptionAssistServiceImpl（修改）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImpl`
**职责**：辅助开方 Service 实现。增加 DrugFacade 注入，在 assist() 流程中遍历处方条目调用 DrugFacade.findByDrugCode() 获取药品信息（旁路信息查询，失败不阻断主流程）。

**新增字段**：
- `private final DrugFacade drugFacade` — 药品信息查询门面
- `private final long drugFacadeTimeout` — 通过 `@Value("${prescription.drug-facade.timeout:2}")` 注入超时阈值

**构造方式变更**：
- 当前 8 参构造器 → 增加 `DrugFacade drugFacade, @Value("${prescription.drug-facade.timeout:2}") long drugFacadeTimeout` 变为 10 参

**修改方法**：
- `assist(PrescriptionAssistRequest request)`：在遍历 `parseDraftItems()` 返回的 items 列表处理剂量告警的循环中，对每个 item 的 `getDrugId()` 调用 `drugFacade.findByDrugCode()`。调用包裹 try-catch，异常时记录 WARN 日志，继续处理下一项。返回值（DrugInfo）当前不使用，仅为后续功能预留数据通路。
- 新增辅助方法如 `private void enrichWithDrugInfo(List<PrescriptionItem> items)` 封装上述遍历逻辑

### PrescriptionAuditServiceImplTest（修改）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest`

**变更**：
- 增加 `@Mock private DrugFacade drugFacade;`
- `setUp()` 中构造器传参增加 `drugFacade, 2L`

**新增导入**：
- `import com.aimedical.modules.commonmodule.drug.DrugFacade`

### PrescriptionAssistServiceImplTest（修改）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest`

**变更**：
- 增加 `@Mock private DrugFacade drugFacade;`
- `setUp()` 中构造器传参增加 `drugFacade, 2L`

**新增导入**：
- `import com.aimedical.modules.commonmodule.drug.DrugFacade`

## 错误处理

- DrugFacade.findByDrugCode() 调用失败（任何异常）时：捕获异常，记录 WARN 日志，继续处理下一处方条目
- 不抛出异常，不阻断主流程
- DrugInfo 返回值当前不使用，仅预留数据通路

## 行为契约

- DrugFacade 调用是旁路信息查询，成功/失败均不影响业务主流程的 AuditResponse/PrescriptionAssistResponse 生成
- 同一处方中多个药品条目：每个条目独立调用 findByDrugCode()，各自独立 try-catch
- 日志格式：`log.warn("DrugFacade.findByDrugCode({}) failed after {}ms: {}", drugCode, elapsed, e.getClass().getSimpleName());`

## 依赖关系

- 依赖已有类型：`DrugFacade`（common-module-api）、`DrugInfo`（common-module-api）
- `common-module-api` 已在 prescription 模块 pom.xml 中以 compile scope 依赖，DrugFacade 接口可直接导入
- 暴露给后续任务的公开接口：无新增
