# 任务指令（v8）

## 动作
NEW

## 任务描述
实现包C（智能分诊/智能导诊）consultation 模块的全部代码，位于 `AIMedical/backend/modules/consultation/` 模块中，包根 `com.aimedical.modules.consultation`。

需实现以下全部类型（按分包）：

### api/
- `TriageController.java` — REST 端点：POST /api/triage/consult（单轮/多轮分诊对话）、POST /api/triage/select-department（手动选科写入）

### dto/（6 个业务层 DTO）
- `DialogueCreateRequest.java` — 分诊对话创建请求，含 chiefComplaint(5-500字符,必填)/patientId/age/gender/sessionId(必填,UUID v4)/ruleVersion/ruleSetId/additionalResponses(List\<AdditionalResponse\>,与chiefComplaint互斥)/correctedChiefComplaint(可选)
- `AdditionalResponse.java` — 追问回答值对象，含 question/answer/answeredAt(ISO日期时间,可选)
- `TriageResponse.java` — 分诊响应，含 departments(List\<RecommendedDepartment\>,0-3项)/doctors(List\<RecommendedDoctor\>,0-5项)/reason(必填,≥1字符)/matchedRules(List\<MatchedRule\>)/sessionId/needFollowUp/followUpQuestion/confidence(可选)/degraded/fallbackHint(可选)/ruleVersionMismatch(可选)
- `RecommendedDepartment.java` — 含 departmentId/departmentName/score
- `RecommendedDoctor.java` — 含 doctorId/doctorName/departmentId/availableSlotCount/score
- `MatchedRule.java` — 含 ruleId/ruleName/score

### service/
- `TriageService.java`（interface）— triage(DialogueCreateRequest)→TriageResponse, selectDepartment(sessionId,departmentId,departmentName)→TriageResponse
- `impl/TriageServiceImpl.java` — 核心实现：委托 AiService.triage()；AI 不可用时降级至 TriageRuleEngine.match() → DepartmentFallbackProvider.getFallbackDepartments()；AI 连续失败 3 次时附加 fallbackHint；调用 DoctorFacade 为各推荐科室获取排班医生；分诊结果写入 TriageRecord（先写数据库再更新内存）；多轮对话上下文全量拼接
- `DeadLetterCompensationService.java` — 定时补偿任务（@Scheduled 每 30 分钟），扫描 state=FAILED 且 retryCount<maxRetryCount 的死信事件，反序列化后调用 selectDepartment 完成补偿

### dialogue/
- `DialogueSession.java` — 可变会话状态对象，含 sessionId/chiefComplaint/correctedChiefComplaint/additionalResponses(List)/aiFailCount/roundCount/ruleVersion/ruleSetId/createdAt/lastAccessedAt
- `DialogueSessionManager.java` — 会话生命周期管理器，依赖 SessionStore 接口存储；提供 createSession/cancelSession/restoreSession 等方法；会话 TTL 30 分钟

### rule/
- `TriageRuleEngine.java`（interface）— match(chiefComplaint,ruleVersion,ruleSetId)→List\<RecommendedDepartment\>, currentRuleVersion()→String, currentRuleSetId()→String
- `DefaultTriageRuleEngine.java` — 数据库规则源实现，Caffeine refreshAfterWrite(60s) 定时刷新缓存；规则快照失效时降级使用最新版本
- `entity/TriageRule.java`（JPA @Entity）— 含 ruleId/ruleSetId/ruleVersion/conditions(JSON TEXT)/resultDepartmentId/resultDepartmentName/score/enabled/createTime/updateTime

### fallback/
- `DepartmentFallbackProvider.java`（interface）— getFallbackDepartments()→List\<RecommendedDepartment\>
- `StaticDepartmentFallbackProvider.java` — 返回静态兜底科室列表（配置化）

### entity/（2 个 JPA @Entity）
- `TriageRecord.java` — 分诊结果实体，含 recordId(@Id @GeneratedValue)/sessionId(@Column(unique=true))/patientId(建索引)/chiefComplaint/aiRecommendedDepartments(JSON TEXT)/recommendedDoctors(JSON TEXT)/ruleMatchedDepartments(JSON TEXT)/finalDepartmentId(nullable)/finalDepartmentName(nullable)/confidence/degraded/ruleVersion/ruleSetId/triageTime
- `DeadLetterEvent.java` — 死信事件实体，含 id(@Id @GeneratedValue)/eventPayload(TEXT,NOT NULL)/failReason(VARCHAR(500),NOT NULL)/failTime/state(VARCHAR(20),default 'FAILED')/retryCount(Integer,default 0)/maxRetryCount(Integer,default 3)

### repository/
- `TriageRecordRepository.java` — findBySessionId, findTopByPatientIdOrderByTriageTimeDesc, findBySessionIdIn
- `TriageRuleRepository.java` — findByRuleSetIdAndRuleVersion, findByEnabledTrue
- `DeadLetterEventRepository.java` — findByStateAndRetryCountLessThan

### event/
- `RegistrationEventListener.java` — @EventListener/@Retryable 监听 RegistrationEvent，通过 sessionId 关联 TriageRecord 后调用 selectDepartment 写入 finalDepartmentId；可治愈异常重试(2s间隔,最多3次)；@Recover 写入 DeadLetterEvent

### converter/
- `TriageConverter.java` — toAiTriageRequest(DialogueCreateRequest,DialogueSession)→ai-api TriageRequest, toTriageResponse(AiResult\<ai-api TriageResponse\>,List\<RecommendedDoctor\>)→业务层 TriageResponse

## 选择理由
T1–T7 已全部完成，T8 的编译依赖（T1 ai-api DTO、T4 Store 接口、T5 门面接口、T7 模块骨架）均已就绪。consultation 模块是包C 智能分诊的核心实现，按底层依赖优先原则推进。

## 任务上下文
### OOD 设计文档
核心行为契约见 §3.1（包C 核心抽象）和 §4.1（智能分诊场景）。

### 已有代码上下文
- ai-api 模块已提供：TriageRequest（含 chiefComplaint/additionalResponses/patientId/sessionId/ruleVersion/ruleSetId）、TriageResponse（含 recommendedDepartments/reason/recommendedDoctors/matchedRules/needFollowUp/followUpQuestion/confidence/degraded/sessionId/correctedChiefComplaint）、RecommendedDepartment、RecommendedDoctor、MatchedRuleItem、AdditionalResponseItem、AiResult\<T\>、AiResultFactory、AiService（triage 方法签名 `CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request)`）
- common-module-api 已提供：SessionStore(V get/put/remove/containsKey/keySet)、SuggestionStore（extends SessionStore）、DraftContextStore（extends SessionStore）、ConcurrentHashMapStore、DoctorFacade（findAvailableDoctorsByDepartment→List\<AvailableDoctor\>）、AvailableDoctor（doctorId/doctorName/departmentId/availableSlotCount）、RegistrationEvent（registrationId/patientId/sessionId/departmentId/departmentName/doctorId/eventTime）
- consultation/pom.xml 已创建，依赖 common/common-module-api/ai-api/spring-boot-starter-web/jpa/validation/test
- 目录结构已就绪：src/main/java、src/test/java

### 参考模块模式
参照 patient 模块的包结构（api/dto/service/impl/repository/entity/converter）和编码风格（无 Lombok，手写 getter/setter，构造器，JPA @Entity 继承 BaseEntity）。

## 修订说明（v8 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| plan.md 中 R1 NEW T8 描述错误，应标为"包C（智能分诊）"而非"包D-AI1（处方审核）" | 已将 plan.md 中 R1 NEW T8 描述修正为"实现包C（智能分诊）的全部代码"，选择理由同步修正为"包C 智能分诊是用户交互入口，核心依赖优先" |
