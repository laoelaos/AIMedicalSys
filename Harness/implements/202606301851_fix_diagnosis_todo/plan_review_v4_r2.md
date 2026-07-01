# 计划审查报告（v4 r2）

## 审查结果
APPROVED

## 发现

### P14 CRITICAL 阻断写入

- **[轻微]** task_v4 §P14 修复方案中"AI 失败时，如果之前 check-dose 写入的 CRITICAL 告警已因处方草案变更而不再适用，旧告警被清除"的论述存在逻辑跳跃。`assist()` 被调用时并不一定意味着处方草案已变更——用户可能仅重新请求 AI 推荐而未修改处方。清除旧 CRITICAL 告警的正确理由是：`assist()` 是一次新的完整校验周期，该周期内如果 AI 失败则无法执行剂量校验，此时保留旧告警可能导致提交端点阻断一个已不存在的风险（因为处方可能已被用户修改）。但反过来，保留旧告警也有合理性——如果处方未变，旧告警仍然有效。task_v4 选择了"清除"策略，这与 OOD §4.4 "无 CRITICAL 时清除对应条目"的语义一致（AI 失败路径无 CRITICAL 告警产出，因此应清除），且避免了过期告警残留的更严重问题。此决策合理，仅论述可更精确。

### DraftContextCleanupTask 模块迁移

- **[轻微]** task_v4 §DraftContextCleanupTask 迁移方案中未提及 `@Component` 注解保留。当前 `DraftContextCleanupTask` 使用 `@Component`（非 `@Service`），迁移后应保持一致。此为细节问题，实现时自然保留，不影响正确性。

### 移除 enrichWithDrugInfo 死代码

- **[轻微]** task_v4 §enrichWithDrugInfo 移除方案中"DrugFacade 在 OOD §2.2 中定义为跨模块药品信息查询门面，当前仅用于 enrichWithDrugInfo 死代码。移除后 DrugFacade 接口定义保留在 common-module-api 中"的决策正确。但需注意 OOD §2.2 line 318 明确要求 PrescriptionAssistServiceImpl 和 PrescriptionAuditServiceImpl 调用 DrugFacade 并"返回空药品信息 + WARN 日志"。当前代码的 `enrichWithDrugInfo` 实现了"捕获异常 + WARN 日志"部分，但"返回空药品信息"未实现（结果未被消费）。移除后，OOD §2.2 的 DrugFacade 调用要求将完全未实现。这属于 OOD 与实现的偏差，但鉴于当前实现本身就是死代码（调用但未使用结果），移除死代码比保留无意义的调用更诚实。后续若需真正实现 DrugFacade 的药品信息填充功能，应作为独立需求重新设计。

### 整体评估

- P14 修复方案经 r1 审查修订后，从"写入伪 CRITICAL 告警"改为"清除旧 CRITICAL 告警"，与 OOD 语义一致，`clearCriticalAlerts()` 封装合理，5 个失败路径覆盖完整。
- DraftContextCleanupTask 迁移方案简洁明确，不做键命名变更的决策正确。
- enrichWithDrugInfo 死代码移除方案完整，测试文件配套修改清单准确（4+4=8 个 DrugFacade 测试用例、Mock 字段、import、构造器签名）。
- 涉及文件清单与实际代码位置一致，行号引用准确。
