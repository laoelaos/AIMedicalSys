# 设计审查报告（v12 r2）

## 审查结果
**REJECTED**

## 发现

### [一般] 9p：interrupted 消息中文文案未明确指定

设计第 327 行描述第 160 行 `"AI medical record generation interrupted"` 的处理为"保留或改为中文（由 9c 变更后）"。任务要求 MedicalRecordServiceImpl 所有英文文案改为中文（与 consultation 统一），该描述存在歧义，未给出明确的最终中文文案。实现者无法判断是保留英文还是改为中文。

修正方向：明确指定该行的中文文案，例如 `"AI 病历生成被中断"`。

### [一般] 文件规划表 TriageConverter.java 标记与描述矛盾

文件规划表（第 21 行）将 `consultation/.../converter/TriageConverter.java` 列为"修改 | 9n"，但 9n 类型定义部分（第 287-288 行）明确说明"保留 TriageConverter.toTriageResponse 中为唯一边界"，不涉及任何代码改动。设计已对 AuditConverter 做了类似修正（从规划表中移除），但对 TriageConverter 未做同样处理。规划表与描述矛盾，可能导致实现者误改文件。

修正方向：将 TriageConverter.java 标记改为"参考（无需修改）"或从规划表中移除。

### [轻微] 文件规划表 DegradationContext.java 标记为"修改"但无实际变更

文件规划表（第 20 行）将 `ai/ai-api/.../DegradationContext.java` 列为"修改 | 9j"，但 9j 类型定义部分（第 223-258 行）仅描述了 FallbackAiService.java 的变更，未描述对 DegradationContext.java 的任何修改。任务也要求 DegradationContext 构造函数"保持兼容"。

修正方向：将 DegradationContext.java 标记改为"参考（保持兼容，无需变更）"或单独说明无需修改。
