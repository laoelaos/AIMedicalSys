根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **[严重] "规避 Phase 5 迁移成本"目标在内存存储维度未闭合**：三项内存存储（DialogueSessionManager、AiSuggestionResult 存储、PrescriptionDraftContext）直接使用 ConcurrentHashMap 实现，无隔离层。需将 Store 抽象层（如 SessionStore、SuggestionStore、DraftContextStore 接口）从"建议"升级为"设计强制项"，Phase 2/3 使用 ConcurrentHashMapStore 实现，Phase 5 替换为 RedisStore 实现。位置：§1.1 line 9；§6.1 line 991-995

2. **[严重] check-dose 端点高频调用下的异步 AI 去重/节流策略完全缺失**：未定义 prescriptionId 级别的异步 AI 调用去重策略，一次编辑会话可能产生数十至数百个并发 AI 调用。需定义去重策略（同一 prescriptionId 下存在 PENDING/COMPLETED 未读 task 时不重复创建新 AI 调用）、或前端防抖/后端限流配合的节流规则、或覆盖式更新及取消前次 task 的机制。位置：§3.4 line 598-601；§4.4 line 830-835；§6.3 line 1001-1003

3. **[一般] "规则可配置"依赖 admin 模块 OOD，无时间线协调**：§9.3 将 TriageRule、DrugContraindicationMapping 等实体的 CRUD 管理接口定义为"由 admin 模块 OOD 文档独立定义"，但未定义交付时间线，也未标注若 admin OOD 滞后的影响。需在 §1.1a 外部依赖表中补充 admin 模块规则管理接口的依赖项，标注实现时间线约束和风险说明。位置：§9.3 line 1158-1168

4. **[一般] AiResult.success=true 且 data=null 的边界处理未定义**：各 Service 层在解引用 data.getXxx() 时可能引发 NullPointerException。需在各 Service 实现类中增加 data=null 的防御性检查（等价于降级或空结果处理），或在 AiResult 类中增加工厂方法约束，或在 §2.3/§5 中明确契约。位置：§2.3 line 344-345；§3.1 line 358；§3.2 line 452-454；§3.3 line 568-572

5. **[一般] "撤销审核"的操作触发端点未定义**：描述了撤销审核时 isLatest 回退为 false 的逻辑，但未定义触发撤销审核的 API 端点。需新增撤销审核的 API 端点定义（请求/响应/状态码/逻辑时序），或将撤销行为纳入已有端点。位置：§3.2 line 482-483；§4.2 line 759-761

6. **[一般] prescriptionId 在 check-dose 请求链路中的传递路径未闭环**：DosageCheckRequest 包含 prescriptionId，但 check-dose 端点可能先于"创建草稿"动作被调用，prescriptionId 可能尚不存在。需明确定义 prescriptionId 在处方编辑生命周期中的分配时机——由前端预创建或后端首次 check-dose 调用时自动生成。位置：§1.3 line 127-128；§3.4 line 627-629；§4.4 line 826-841

7. **[一般] 缺少 AI Mock 实现的行为契约**：目录结构中标注 ai-impl 包含 Mock/降级/底座管线，但未定义 AI Mock 实现的行为契约（激活方式、返回数据、切换机制）。需在 §2.3 AiService 接口定义后新增"Mock 实现契约"段落，定义 MockAiService 的返回策略和激活方式。位置：§1.2 line 41；§2.1 line 239-247

8. **[轻微] §4.5 命名空间区分说明处有多余字符**：line 888 出现双冒号"：："，影响文档一致性。删除多余冒号。位置：§4.5 line 888

9. **[轻微] DosageStandard 单位字段 unit 缺少枚举约束定义**：§8.4 unit 类型仅标注为 String，未约束为 DosageUnitGroup 中的单位值。将 unit 字段类型改为对 DosageUnitGroup 的单位枚举引用，或在 admin 模块保存接口中做单位合法性校验。位置：§8.4 line 1121

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- 第1轮：RegistrationEvent 缺少 sessionId 字段 → 已修复
- 第1轮：AllergyWarningSeverity 枚举值不匹配 → 已修复
- 第1轮：DosageThresholdService 匹配优先级与 §8.4 不一致 → 已修复
- 第1轮：过敏冲突检查归属未定义 → 已修复
- 第1轮：encounterId→visitId 转换未定义实现路径 → 已修复
- 第1轮：DialogueSession/TriageRecord 事务一致性 → 已修复
- 第1轮：LocalRuleEngine 规则计数不一致 → 已修复
- 第1轮：CRITICAL/BLOCK 阻断竞态防护手段 → 已修复
- 第1轮：PrescriptionAssistResponse 缺少 errorCode 字段 → 已修复
- 第2轮：JPA @Id 主键字段缺失 → 已修复
- 第2轮：手动选科与 RegistrationEvent 覆盖优先级 → 已修复
- 第2轮：全量拼接上下文缺少 AI 感知截断标记 → 已修复
- 第2轮：DrugFacade 有引用无定义 → 已修复
- 第2轮：外部模块依赖缺少说明 → 已修复
- 第2轮：DeadLetterEvent 缺少精确 JPA 字段定义 → 已修复
- 第2轮：AiResult 工厂方法与 Phase 5 兼容性冲突 → 已修复

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **[持续 3/4 轮]** Phase 5 迁移成本在内存存储维度未闭合 — 第1轮(P7)首次提出，第3轮再次出现，本轮仍然存在。建议性表述（"建议"引入 Store 抽象层）未升级为设计强制项，导致问题反复出现。**本轮必须将 Store 抽象层升级为强制设计项**。
- **[持续 2 轮]** check-dose 异步 AI 去重/节流策略缺失 — 第3轮首次提出，本轮再次出现。需在本轮彻底闭合。
- **[持续 2 轮]** admin 模块 OOD 依赖无时间线协调 — 第3轮首次提出，本轮再次出现。
- **[持续 2 轮]** AiResult.success=true + data=null 边界 — 第3轮首次提出，本轮再次出现。
- **[持续 2 轮]** "撤销审核"端点未定义 — 第3轮首次提出，本轮再次出现。
- **[持续 2 轮]** prescriptionId 在 check-dose 链路中未闭环 — 第3轮首次提出，本轮再次出现。
- **[持续 2 轮]** AI Mock 行为契约缺失 — 第3轮首次提出，本轮再次出现。

### 新发现的问题（本轮新识别）
- §4.5 多余字符"：："（轻微）
- DosageStandard unit 字段缺少枚举约束（轻微）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v3_copy_from_v2.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
