# 设计审查报告（v10 r4）

## 审查结果
REJECTED

## 发现

### [一般] forceSubmit=false + WARN 经过五字段比对一致后的结果未定义

设计在第 293 行仅描述了五字段比对「不一致」时返回 `RX_AUDIT_PRESCRIPTION_MODIFIED`，但比对「一致」时未定义任何行为。`forceSubmit=false + WARN` 的语义本应阻止直接提交（WARN 要求显式 forceSubmit=true 才能放行），但设计缺失了「比对一致后应阻断/返回错误」的完整路径，导致实现阶段无法确定该分支的最终响应。

**期望修正方向**：补充 `forceSubmit=false + WARN + 五字段比对一致` 的明确行为，例如返回 `SubmitResponse(submitted=false, blockInfo=BlockResponse(...))` 或对应的错误码。

### [一般] 提交流程步②未检查 isLatest，导致与撤销流程产生交互遗漏

步②使用 `findTopByPrescriptionIdOrderByAuditSequenceDesc` 查取最高 auditSequence 的记录，未按 isLatest 过滤。撤销流程将某条 WARN 记录的 isLatest 置为 false 后，该记录（仍为 WARN、仍为最高序列）依然会进入步③的 forceSubmit=false + WARN 判定分支。设计未说明是否应在此处仅考虑 `isLatest=true` 的记录，也未定义撤销后提交流程的预期行为。

**期望修正方向**：步②明确是否应基于 `isLatest=true` 查询最新审核结果，并在步②/步③中说明撤销记录对提交流程的影响。
