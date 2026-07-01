# 代码审查报告（v10 r3）

## 审查结果
REJECTED

## 发现

- **[一般]** `PrescriptionAuditController.java` — 构造器依赖与设计规格不符，撤销业务逻辑应在 Service 层而非 Controller 层

## 修改要求

### [一般] PrescriptionAuditController 构造器依赖偏离设计 / revoke 逻辑层级错误

**位置**：`prescription/.../api/PrescriptionAuditController.java:32-40`（构造器注入），第73-89行（revoke 方法体）

**问题**：
1. 设计明确声明 Controller 的构造方式为「Spring 构造器注入 PrescriptionAuditService, PrescriptionAuditEnforcer」（detail_v10.md 第66行），实现额外注入了 `AuditRecordRepository`。
2. 设计约束「Controller 层仅返回 ResponseEntity.ok().build()」（detail_v10.md 第71行），实现将完整的撤销业务逻辑（findById 查询 → isLatest 校验 → WARN 级别校验 → save 持久化）全部置于 Controller 方法体内，而非委托给 Service 层。

**原因**：违反了设计文档对 Controller 层职责和依赖的显式约束。将数据访问和业务规则判断放在 Controller 层不利于单元测试和职责分离。

**期望修正方向**：
1. 在 `PrescriptionAuditService` 接口增加 `void revoke(Long auditId)` 方法签名。
2. 在 `PrescriptionAuditServiceImpl` 中实现 revoke 逻辑（findById → isLatest/warn 校验 → setLatest(false) → save，异常场景抛出 `BusinessException`）。
3. 移除 `PrescriptionAuditController` 中的 `AuditRecordRepository` 注入，仅保留 `PrescriptionAuditService` 和 `PrescriptionAuditEnforcer`。
4. Controller 的 revoke 端点简化为调用 `prescriptionAuditService.revoke(auditId)` 后直接返回 `ResponseEntity.ok().build()`。
