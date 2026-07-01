根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **[严重] PrescriptionDraftContext CRITICAL 告警缺少 /assist 端点写入路径**：/assist 主端点调用 DosageThresholdService 进行剂量阈值校验并产出 doseWarnings，若其中包含 CRITICAL 级别告警，这些告警不会写入 PrescriptionDraftContext。医生通过 /assist 获取处方草案后直接提交（不走 check-dose），步①将判定为无 CRITICAL 告警而放行。改进建议：在 /assist 主端点的本地即时校验执行后，将 alertLevel=CRITICAL 的告警同步写入 PrescriptionDraftContext（以 prescriptionId 为键全量覆盖），写入逻辑与 check-dose 路径一致。同时补充 /assist 端点未携带 prescriptionId 时的兜底处理。

2. **[严重] DedupTaskScheduler 去重逻辑存在 TOCTOU 竞态条件**：check-dose 端点的 AI 异步调用去重逻辑为"查询 AiSuggestionResult → 判断状态 → 按条件创建新 task"三步操作，不属于原子操作。两并发 check-dose 请求可同时通过"无 PENDING task"检查。改进建议：在 SuggestionStore 接口新增原子性的 putIfAbsent/createIfNotExists 方法，将去重判定下沉到存储层原子操作。

3. **[一般] 阻断竞态防护设计中的二次 CRITICAL 验证缺少快照保存机制**：步①检查时未定义快照存储位置和格式。改进建议：提交端点内部设计 SubmitContext 值对象（线程级闭包），在步①执行时快照当前 CRITICAL 告警列表，步③前比对快照与实时查询结果的差异。

4. **[一般] DeadLetterCompensationService "重新投递"的投递目标不明确**：未定义投递目标——是重新发布 ApplicationEvent 还是直接调用 TriageService.selectDepartment()。若为前者会形成 @Retryable 无限循环。改进建议：明确采用直接调用 TriageService.selectDepartment() 的补偿模式，补充状态机迁移规则。

5. **[一般] AiResult.prescriptionAssist() 超时场景下的 PartialData 类型无保证**：超时场景下 partialData 的语义存在歧义——是部分字段还是部分药品。改进建议：明确 partialData 为"已成功生成的完整药品条目列表"（部分药品而非部分字段）。

6. **[一般] MissingFieldDetector 的组织形式在 §1.3 与 §2.1 之间不一致**：§1.3 定义为 interface，但 §2.1 将其归属在 parser/ 包下。改进建议：将 MissingFieldDetector 调整至独立 detector/ 子包或 service/ 下。

7. **[轻微] TriageRecord 未定义"重新分诊"的生命周期行为**：同一患者在同一 session 内重新发起分诊，TriageRecord 的写入行为未定义。改进建议：按 sessionId 一对一映射，同 sessionId 重新分诊时覆盖已有记录。

## 历史迭代回顾

- **已解决的问题**（出现在历史反馈但当前反馈中不再提及）：
  - DrugFacade 超时配置与降级策略（v6 第1项）——已在产出中补充超时阈值和降级行为
  - §8.4 描述文本"1→2→3→4→5"数字不符（v6 第2项）——已修正
  - RX_AUDIT_FORCE_SUBMIT_INVALID 错误码遗漏（v6 第3项）——已补充
  - AiSuggestionResult TTL 清理间隔参数（v6 第4项）——已补充

- **持续存在的问题**（在多轮反馈中反复出现，需重点解决）：
  - 问题#1（PrescriptionDraftContext CRITICAL 告警缺少 /assist 写入路径）——v7 历史第1项与本轮审查第1项一致，说明上一轮修复未成功闭环
  - 问题#2（DedupTaskScheduler 去重竞态）——v7 历史第2项与本轮审查第2项一致，说明去重原子化下沉未在 v7 产出中落实
  - 问题#3（阻断竞态防护快照机制未定义）——v7 历史第3项与本轮审查第3项一致，SubmitContext 设计未落地
  - 问题#4（DeadLetterCompensationService 投递目标不明确）——v7 历史第4项与本轮审查第4项一致，补偿模式定义缺失
  - 问题#5（AiResult.prescriptionAssist() partialData 语义歧义）——v7 历史第5项与本轮审查第5项一致，语义定界未完成

- **新发现的问题**（本轮新识别）：
  - 问题#6（MissingFieldDetector 包归属不一致）
  - 问题#7（TriageRecord 重新分诊生命周期未定义）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v7_copy_from_v6.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
