# 再审议判定报告（v7）

## 判定结果

RETRY

## 判定理由

组件B的诊断报告（b_v7_diag_v1.md）共识别 7 个问题，其中严重等级 2 个（#1 CRITICAL 告警缺少 /assist 端点写入路径、#2 去重 TOCTOU 竞态条件），一般等级 3 个（#3 快照保存机制未定义、#4 死信补偿投递目标不明确、#5 PartialData 类型无保证），轻微等级 2 个（#6 包结构不一致、#7 TriageRecord 生命周期未定义）。质询报告（b_v7_challenge_v1.md）结果为 LOCATED，确认全部问题的有效性。因诊断报告包含严重和一般等级的问题，满足 RETRY 条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：PrescriptionDraftContext CRITICAL 告警缺少 /assist 端点写入路径，导致医生通过 /assist 获取处方草案后直接提交时 CRITICAL 告警被遗漏
- **所在位置**：§3.4 PrescriptionAssistService 职责描述（line 619–621）、§3.4 PrescriptionDraftContext 覆盖更新行为契约（line 689）、§4.2 处方提交端点步①（line 824）
- **严重程度**：严重
- **改进建议**：在 /assist 主端点本地即时校验执行后，将 alertLevel=CRITICAL 的告警同步写入 PrescriptionDraftContext，写入逻辑与 check-dose 路径一致

- **问题描述**：DedupTaskScheduler 去重逻辑由"查询→判断→创建"三步组成，非原子操作，并发请求可绕过去重约束
- **所在位置**：§3.4 "异步 AI 调用去重策略"（line 643）、§6.3 "包E 的异步 AI 建议与去重"（line 1305）
- **严重程度**：严重
- **改进建议**：在 SuggestionStore 接口新增原子性 putIfAbsent/createIfNotExists 方法，将去重判定下沉到存储层

- **问题描述**：阻断竞态防护中步①的 CRITICAL 验证快照未定义存储位置和格式
- **所在位置**：§4.2 阻断竞态防护手段（line 836）
- **严重程度**：一般
- **改进建议**：引入 SubmitContext 值对象（线程级闭包），在步①执行时快照 CRITICAL 告警列表，步③前比对差异

- **问题描述**：DeadLetterCompensationService "重新投递"未定义投递目标，存在与 @Retryable 形成无限循环的风险
- **所在位置**：§2.2 DeadLetterCompensationService 补偿描述（line 320）
- **严重程度**：一般
- **改进建议**：明确采用直接调用 TriageService.selectDepartment() 的业务补偿模式，补充状态机迁移规则

- **问题描述**：AiResult.prescriptionAssist() 超时时 partialData 的语义不明确（部分字段 vs 部分药品）
- **所在位置**：§2.3 AiService.prescriptionAssist() 方法定义（line 350–355）、§3.4 AiResult→AiSuggestionResult 映射表（line 665–676）
- **严重程度**：一般
- **改进建议**：明确 partialData 为"已成功生成的完整药品条目列表"（部分药品而非部分字段）
