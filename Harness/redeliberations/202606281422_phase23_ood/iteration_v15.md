# 再审议判定报告（v15）

## 判定结果

RETRY

## 判定理由

诊断报告经质询确认（LOCATED，实际轮次 1 < 最大轮次 12，提前终止且审查被确认），包含 2 个严重问题（问题1：DosageStandard 五级匹配策略存在未覆盖路径；问题2：DoctorFacade 跨模块同步调用缺失降级保护）和 4 个一般问题（问题3-6），符合 RETRY 条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：DosageStandard 五级匹配策略存在未覆盖路径——当一条记录同时设置 ageRange 和 weightRange 且患者条件不满足精确匹配要求时，该记录被跳过至 Level 5 报"标准不存在"，导致临床正确剂量规则被错误跳过
- **所在位置**：§8.4 五级匹配优先级
- **严重程度**：严重
- **改进建议**：补充 Level 2.5 或重新定义匹配优先级，或采用评分制，或补充文字说明 ageRange 和 weightRange 同时非 null 时的处理方式

- **问题描述**：DoctorFacade 跨模块同步调用缺失降级保护——DoctorFacade 作为强制同步调用路径，超时阈值、失败处理、服务不可用时的替代策略均未定义，与同一模块中 RegistrationEvent 消费端完整的四层保护机制形成实质性设计不对称
- **所在位置**：§3.1 TriageService 协作描述 + §4.1 推荐医生列表生成
- **严重程度**：严重
- **改进建议**：补充超时配置（建议默认 2s）、调用失败时将 TriageResponse.doctors 置空列表并说明原因、补充日志级别和可观测性标记

- **问题描述**：MissingFieldDetector 的 null/空字符串判定语义未定义——AI 返回空字符串、字段缺失、显式 null 三种情况是否视为"缺失"未明确，差集比对基于 keySet 存在性还是值非空非 null 未定义
- **所在位置**：§3.3 MissingFieldDetector 职责 + §4.3 病历生成场景 + §10.3 MedicalRecordGenResponse
- **严重程度**：一般
- **改进建议**：明确判定策略为"基于字段值的非空非 null 存在性"——不存在、值为 null、值为空字符串均视为缺失

- **问题描述**：多轮分诊中 DialogueSession 不支持主诉修正——DialogueSession 首轮固定记录 chiefComplaint，患者修正主诉后无法更新，全量拼接时首轮主诉保持原样导致 AI 收到矛盾上下文
- **所在位置**：§3.1 DialogueSession 职责 + 全量拼接策略
- **严重程度**：一般
- **改进建议**：增加 correctedChiefComplaint 可选字段，或在全量拼接策略中说明由 AI 自身处理修正语义

- **问题描述**：forceSubmit=false 路径缺少对处方已修改的感知——forceSubmit=false 时直接返回当前 WARN 审核结果，但若处方已在 WARN 审核后发生了修改，返回的仍是基于旧处方的审核结果
- **所在位置**：§4.2 处方提交端点行为
- **严重程度**：一般
- **改进建议**：在 forceSubmit=false 路径中增加轻量级内容变更检测（结构化 MD5/哈希比对），发现不一致时提示"处方内容已变更，请重新审核"

- **问题描述**：CRITICAL 告警前端同步更新的竞态窗口——PrescriptionDraftContext 全量覆盖语义下，两次 check-dose 之间 CRITICAL 告警的清除与前端展示存在时序窗口，前后端状态不一致
- **所在位置**：§3.4 PrescriptionDraftContext 覆盖更新行为 + §4.2 CRITICAL 阻断合并判定
- **严重程度**：一般
- **改进建议**：在 §4.4 check-dose 流程中补充 PrescriptionDraftContext 状态变更通知机制，每次响应携带 `contextCriticalCount` 字段供前端判断是否需要同步刷新
