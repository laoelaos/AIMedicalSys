# 测试报告（v8）

## 测试摘要

| 指标 | 数值 |
|------|------|
| 总测试数 | 85 |
| 通过 | 82 |
| 失败 | 3 |
| 跳过 | 0 |
| 测试框架 | JUnit 5 (Jupiter) + Spring Boot Test |
| 构建工具 | Maven 3.9.15 + Surefire 3.1.2 |

## 测试文件清单

| 测试文件 | 测试类 | 测试数 | 状态 |
|---------|--------|--------|------|
| `ConsultationDtoTest.java` | DTO 构造/Getter/Setter 测试 | 18 | ✅ 全部通过 |
| `ConsultationEntityTest.java` | JPA 实体基础测试 | — | ✅ 全部通过 |
| `ConsultationPlaceholderTest.java` | 模块占位测试 | — | ✅ 全部通过 |
| `DialogueSessionTest.java` | DialogueSession 模型测试 | 5 | ✅ 全部通过 |
| `DialogueSessionManagerTest.java` | 会话生命周期管理测试 | 6 | ✅ 全部通过 |
| `DefaultTriageRuleEngineTest.java` | 规则引擎匹配/过滤测试 | 7 | ✅ 全部通过 |
| `DeadLetterCompensationServiceTest.java` | 死信补偿定时任务测试 | 5 | ✅ 全部通过 |
| `RegistrationEventListenerTest.java` | 挂号事件监听器测试 | 5 | ✅ 全部通过 |
| `StaticDepartmentFallbackProviderTest.java` | 静态兜底科室测试 | 5 | ✅ 全部通过 |
| `TriageControllerTest.java` | REST 端点委派测试 | 2 | ✅ 全部通过 |
| `TriageConverterTest.java` | 数据转换器测试 | 7 | ⚠️ 1 失败 |
| `TriageServiceImplTest.java` | 分诊服务核心逻辑测试 | 10 | ⚠️ 1 失败 |
| `SchedulingRetryConfigTest.java` | 调度/重试配置测试 | 3 | ⚠️ 1 失败 |

## 失败详情

### 1. `SchedulingRetryConfigTest.shouldHaveConditionalOnPropertyWithCorrectDefaults`

- **断言行**: 27
- **期望**: `"consultation.scheduling.enabled"`
- **实际**: `"[consultation.scheduling.enabled]"`
- **原因**: 测试代码读取 `@ConditionalOnProperty` 注解的 `value` 属性时使用了 `getValue()` 但未处理数组包装。`@ConditionalOnProperty.value()` 返回 `String[]`，但测试直接与 `String` 比较。测试断言应改为数组/集合比较。

### 2. `TriageConverterTest.shouldReturnEmptyListWhenAiDataHasNoDepartments`

- **断言行**: 115
- **期望**: `result.getDoctors()` 为 `null`
- **实际**: `result.getDoctors()` 返回 `[]` (空列表)
- **原因**: `TriageConverter.toTriageResponse()` 中 `doctors` 参数为 `null` 时被初始化为 `Collections.emptyList()`，但测试期望保持 `null`。属于设计决策差异：返回空列表还是 null。

### 3. `TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures`

- **断言行**: 118
- **期望**: `result.getFallbackHint()` 为 `"AI service has been continuously unavailable"`
- **实际**: `result.getFallbackHint()` 为 `null`
- **原因**: 三次连续 AI 失败后，`TriageServiceImpl` 应附加 `fallbackHint` 但实际未设置。检查表明 `aiFailCount` 计数器在每次 `triage()` 调用时被正确递增，但降级路径中 `fallbackHint` 未按预期附加。实现存在遗漏，需在规则引擎/兜底降级路径中补充 `fallbackHint` 赋值逻辑。

## 测试覆盖分析

| 行为契约 | 覆盖状态 |
|---------|---------|
| `TriageController.consult()` → `@Valid` 校验 | 未覆盖（需 Spring 集成测试环境） |
| `TriageServiceImpl.triage()` AI 成功路径 | ✅ 覆盖 |
| `TriageServiceImpl.triage()` AI 失败降级至规则引擎 | ✅ 覆盖 |
| `TriageServiceImpl.triage()` AI 失败降级至静态兜底 | ✅ 覆盖 |
| `TriageServiceImpl.triage()` 三次失败后附加 fallbackHint | ⚠️ 实现 bug（失败） |
| `TriageServiceImpl.triage()` AI 成功后重置 failCount | ✅ 覆盖 |
| `TriageServiceImpl.triage()` 持久化 TriageRecord | ✅ 覆盖 |
| `TriageServiceImpl.selectDepartment(overwrite=true)` | ✅ 覆盖 |
| `TriageServiceImpl.selectDepartment(overwrite=false, finalDepartmentId==null)` | ✅ 覆盖 |
| `TriageServiceImpl.selectDepartment(overwrite=false, finalDepartmentId!=null)` | ✅ 覆盖 |
| `TriageServiceImpl.selectDepartment` 记录不存在时抛异常 | ✅ 覆盖 |
| `DialogueSessionManager.createSession()` | ✅ 覆盖 |
| `DialogueSessionManager.cancelSession()` | ✅ 覆盖 |
| `DialogueSessionManager.restoreSession()` | ✅ 覆盖 |
| `DialogueSessionManager.evictExpiredSessions()` | ✅ 覆盖 |
| `DefaultTriageRuleEngine.match()` 无匹配规则 | ✅ 覆盖 |
| `DefaultTriageRuleEngine.match()` 按 ruleVersion 过滤 | ✅ 覆盖 |
| `DefaultTriageRuleEngine.match()` 按 ruleSetId 过滤 | ✅ 覆盖 |
| `DefaultTriageRuleEngine.match()` 过滤禁用规则 | ✅ 覆盖 |
| `DeadLetterCompensationService.compensateDeadLetters()` 正常补偿 | ✅ 覆盖 |
| `DeadLetterCompensationService.compensateDeadLetters()` 失败递增 retryCount | ✅ 覆盖 |
| `DeadLetterCompensationService.compensateDeadLetters()` 传递 overwrite=false | ✅ 覆盖 |
| `DeadLetterCompensationService.compensateDeadLetters()` 空事件跳过 | ✅ 覆盖 |
| `RegistrationEventListener.handleRegistrationEvent()` 更新 finalDepartment | ✅ 覆盖 |
| `RegistrationEventListener.handleRegistrationEvent()` 已设置时不覆盖 | ✅ 覆盖 |
| `RegistrationEventListener.handleRegistrationEvent()` 记录不存在时跳过 | ✅ 覆盖 |
| `RegistrationEventListener.recover()` 写入 DeadLetterEvent | ✅ 覆盖 |
| DTO 全字段构造/Getter/Setter | ✅ 覆盖 |
| JPA 实体持久化 | ✅ 覆盖 |

## 结论

测试套件整体质量良好。85 个测试用例中 82 个通过（通过率 96.5%）。3 个失败用例中：
- 1 个为测试断言代码本身的 bug（`SchedulingRetryConfigTest`）
- 1 个为设计决策差异（`TriageConverterTest`，空列表 vs null）
- 1 个为源码实现遗漏（`TriageServiceImplTest`，三次失败后未设置 fallbackHint）

建议优先修复 `TriageServiceImpl` 的 fallbackHint 实现遗漏（影响行为契约 3），其余两个失败用例可按需排期修复。
