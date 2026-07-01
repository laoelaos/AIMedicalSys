# 设计审查报告（v10 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** PrescriptionAuditService 接口仅定义了 `audit()` 方法，但 submit 端点（三步阻断检查）需要 DraftContextStore（步① CRITICAL 检查）、AuditRecordRepository（步② BLOCK 检查）、处方内容版本比较（步③ forceSubmit）等复杂编排逻辑。Controller 构造注入仅声明了 PrescriptionAuditService 和 PrescriptionAuditEnforcer，未注入 DraftContextStore / AuditRecordRepository，且这些依赖也不属于 Controller 层职责。设计中缺少一个 Service 层方法（如 `PrescriptionAuditService.submit()`）或独立的 SubmitService 来承载提交流程的业务逻辑。

- **[严重]** 提交流程缺少 OOD §4.2 规定的二次 CRITICAL 验证：步②审核结果阻断检查完成后、步③执行前，必须重新查询 PrescriptionDraftContext 的 CRITICAL 告警并与步①快照比对，发现增量时中止提交流程。设计完全未提及此步骤。

- **[严重]** 提交流程步①依赖 `PrescriptionDraftContext / DraftContextStore` 进行 CRITICAL 阻断检查，但设计文件规划中未列出 `PrescriptionDraftContext` 类型、未列出 `context/` 子包、也未在依赖关系表中声明 DraftContextStore 的注入。该依赖缺失会导致 submit 端点步①无法实现。

- **[一般]** AuditRecord 实体缺少 `aiResult` 字段（OOD §3.2 明确要求持久化 `riskLevel、aiResult、auditIssues` 等审核结果数据），当前设计仅覆盖了 riskLevel 和 auditIssues。

- **[一般]** `@Table(schema = "PHASE4_PRELOAD")` 不是 JPA 标准的有条件建表机制——schema 属性指定实际数据库 schema 名，不会阻止表创建。需要改用排除实体扫描（如 `@EntityScan` 过滤）或 Flyway/Liquibase 按版本控制。

- **[一般]** 提交流程步③ forceSubmit=true 路径中描述了"版本一致"校验，但缺乏具体的处方内容比较逻辑（药品条目的 drugId+dose+frequency+duration+route 五字段结构化比对），也未说明 OptimisticLockException 如何转换为 RX_AUDIT_CONCURRENT_SUBMIT 错误码。

## 修改要求

1. **提交流程 Service 层抽象**：在 PrescriptionAuditService 中增加 `SubmitResponse submit(SubmitRequest request)` 方法，或在 service/ 下新增 PrescriptionSubmitService 接口，将三步阻断检查逻辑从 Controller 下沉至 Service 层，注入 DraftContextStore、AuditRecordRepository 等必要依赖。

2. **补全提交流程**：在 SubmitService 实现中增加 OOD §4.2 规定的二次 CRITICAL 验证步骤（步②与步③之间），引入 SubmitContext 值对象保存步①快照用于比对。

3. **声明 PrescriptionDraftContext 依赖**：在文件规划中增加 `prescription/src/main/java/.../context/PrescriptionDraftContext.java`（或直接声明 DraftContextStore 注入），并在依赖关系表中列出 DraftContextStore。

4. **AuditRecord 补充 aiResult 字段**：按 OOD §3.2 增加 `aiResult`（String, @Column(columnDefinition="TEXT")），持久化 AI 原始响应 JSON。

5. **DrugInteractionPair 建表控制**：移除 `@Table(schema = "PHASE4_PRELOAD")`，改用排除实体扫描或在 Phase 4 时通过 Flyway 建表。

6. **forceSubmit 处方版本校验**：补充五字段（drugId+dose+frequency+duration+route）结构化比较的具体逻辑，明确 `originalPrescription` JSON 结构的提取和比对方式；在 submit 实现中捕获 `OptimisticLockException` 并转换为 `RX_AUDIT_CONCURRENT_SUBMIT`。
