# 代码审查报告（v12 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** `service/assist/impl/PrescriptionAssistServiceImpl.java:108-121` — `assist()` 中对草稿每项药品构建 `DosageCheckRequest` 时未设置 `unit` 字段。`dosageThresholdService.check()` 中执行 `matched.getUnit().equalsIgnoreCase(null)` 始终返回 false，导致所有药品均命中单位不匹配分支并返回 WARNING 告警，剂量超限判定（单次/日剂量）永不执行。后果：CRITICAL 级别告警无法在 assist 流程中检测，`updateCriticalAlerts` 对该 flow 形同虚设，设计要求的辅助开方剂量安全校验失效。

- **[一般]** `service/assist/impl/PrescriptionAssistServiceImpl.java:105` — `doseAlerts` 局部变量被计算填充但从未被用于构建响应或任何输出，为死代码。

## 修改要求

### 问题 1：assist() 中缺少 unit 导致剂量校验失效

**位置**：`PrescriptionAssistServiceImpl.java` 中 `assist()` 方法 `parseDraftItems()` 与 `DosageCheckRequest` 构建处（约第 108-121 行）。

**问题**：`parseDraftItems()` 未从 AI 草稿 JSON 中提取 `unit` 字段，`DosageCheckRequest` 构建时也未设置 `unit`。这是剂量校验的前提字段，缺失导致所有剂量超限判定被跳过。

**期望修正方向**：在 `parseDraftItems()` 中从 draft JSON 提取 `unit`（JSON key 假定为 `"unit"`），在 `PrescriptionItem` 中增加 `unit` 字段；或将 `unit` 通过 `DrugFacade.findByDrugCode(drugId)` 从药品目录获取并在 `DosageCheckRequest` 中设置。

### 问题 2：死代码 doseAlerts

**位置**：`PrescriptionAssistServiceImpl.java` 约第 105-133 行。

**问题**：`doseAlerts` 被填充但无消费方。

**期望修正方向**：如果设计意图是本地校验结果应返回至响应，则将 `DosageAlert` 转换为 `DoseWarning` 后合并至 `response.doseWarnings`；如果设计意图仅为提取 CRITICAL 写入上下文，则去除死代码或改为流式处理不产生临时列表。
