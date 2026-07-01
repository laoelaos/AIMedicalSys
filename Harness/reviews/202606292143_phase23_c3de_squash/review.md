# 审查进度跟踪

## 计划

- **R1**：三个业务模块的核心实现
  - R1.1 consultation 模块（包C 智能分诊）
  - R1.2 prescription 模块（包D-AI1 + 包E）
  - R1.3 medical-record 模块（包D-AI2）
- **R2**：横切关注点
  - R2.1 Store 抽象 + 并发安全
  - R2.2 AI 集成 + 降级策略
  - R2.3 跨模块事件 + 门面
- **R3**：契约完整性与质量
  - R3.1 DTO 字段对齐 + Converter
  - R3.2 错误码 + 异常处理
  - R3.3 测试覆盖 + 可追溯性

## 已完成轮次

### R1.1: 包C 智能分诊（consultation 模块）— 严重 7 / 一般 14 / 轻微 9 — 核心抽象骨架齐全但关键契约偏离设计文档，主要集中在事务一致性、主诉修正双路径、DoctorFacade 降级保护、规则引擎关键词匹配、推荐医生截断/评分 → `review_v1_1.md`

### R1.2: 包D-AI1 处方审核 + 包E 辅助开方（prescription 模块）— 严重 5 / 一般 10 / 轻微 8 — 模块结构清晰但 5 个运行时机制未实现（异步 AI 调度 / DrugFacade 注入 / TTL 清理 / 规则变更事件 / 提交 WARN 契约）构成上线 blocker → `review_v1_2.md`

### R1.3: 包D-AI2 病历生成（medical-record 模块）— 严重 4 / 一般 7 / 轻微 6 — 核心抽象与数据契约正确实现，但 8 个错误码仅实现 4 个、TemplateConfigChangeEvent 监听与 VisitIdReconciledTask 定时任务未实现、并发写保护路径形同虚设 → `review_v1_3.md`

---

### R2.1: Store 抽象层 + 并发安全 — 严重 4 / 一般 7 / 轻微 2 — SuggestionStore.createIfNotExists 原子方法缺失、AiSuggestionResult / PrescriptionDraftContext TTL 清理缺失、DialogueSession 并发串行化未实现、AuditRecord isLatest 并发写违反约束 → `review_v2_1.md`

### R2.2: AI 集成 + 降级策略 — 严重 6 / 一般 8 / 轻微 4 — AiResultFactory 在业务层 0 引用、超时配置 0 外化（三处无限等待）、异步 AI 流程断链（AiSuggestionResult 永远 PENDING）、MockAiService 空壳、correctedChiefComplaint 透传失效 → `review_v2_2.md`

### R2.3: 跨模块事件 + 门面 — 严重 4 / 一般 4 / 轻微 1 — 5 类 ChangeEvent 事件驱动缓存刷新全部缺失、@Retryable 异常范围过宽、TriageRecord 同 sessionId 约束冲突、DoctorFacade 降级保护缺失 → `review_v2_3.md`
