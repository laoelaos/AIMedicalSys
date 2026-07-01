# 测试报告（v10）

## 概述

依据详细设计 v10 的行为契约，为 prescription 模块审核子域编写单元测试。共新建 28 个测试文件，覆盖全部 43 个源文件的公开接口行为。

## 测试文件清单

| # | 文件路径 | 被测类型 | 用例数 |
|---|---------|---------|--------|
| 1 | `prescription/.../PrescriptionErrorCodeTest.java` | PrescriptionErrorCode | 2 |
| 2 | `prescription/.../dto/audit/AlertSeverityTest.java` | AlertSeverity | 1 |
| 3 | `prescription/.../dto/audit/AuditRequestTest.java` | AuditRequest | 1 |
| 4 | `prescription/.../dto/audit/AuditResponseTest.java` | AuditResponse | 1 |
| 5 | `prescription/.../dto/audit/SubmitRequestTest.java` | SubmitRequest | 1 |
| 6 | `prescription/.../dto/audit/SubmitResponseTest.java` | SubmitResponse | 1 |
| 7 | `prescription/.../dto/audit/BlockResponseTest.java` | BlockResponse | 2 |
| 8 | `prescription/.../dto/audit/PrescriptionItemTest.java` | PrescriptionItem | 1 |
| 9 | `prescription/.../dto/audit/PatientInfoTest.java` | PatientInfo | 1 |
| 10 | `prescription/.../dto/audit/AllergyDetailTest.java` | AllergyDetail | 1 |
| 11 | `prescription/.../dto/audit/AuditAlertTest.java` | AuditAlert | 1 |
| 12 | `prescription/.../dto/audit/AuditIssueTest.java` | AuditIssue | 1 |
| 13 | `prescription/.../dto/audit/DrugInteractionTest.java` | DrugInteraction | 1 |
| 14 | `prescription/.../dto/audit/SuggestionTest.java` | Suggestion | 1 |
| 15 | `prescription/.../entity/AuditRecordTest.java` | AuditRecord | 2 |
| 16 | `prescription/.../rule/DrugInteractionRuleTest.java` | DrugInteractionRule | 1 |
| 17 | `prescription/.../rule/AllergyCheckRuleTest.java` | AllergyCheckRule | 7 |
| 18 | `prescription/.../rule/ContraindicationCheckRuleTest.java` | ContraindicationCheckRule | 5 |
| 19 | `prescription/.../rule/DuplicateCheckRuleTest.java` | DuplicateCheckRule | 3 |
| 20 | `prescription/.../rule/DosageLimitRuleTest.java` | DosageLimitRule | 5 |
| 21 | `prescription/.../rule/SpecialPopulationDosageRuleTest.java` | SpecialPopulationDosageRule | 5 |
| 22 | `prescription/.../rule/DefaultLocalRuleEngineTest.java` | DefaultLocalRuleEngine | 1 |
| 23 | `prescription/.../impl/PrescriptionAuditEnforcerImplTest.java` | PrescriptionAuditEnforcerImpl | 1 |
| 24 | `prescription/.../context/PrescriptionDraftContextTest.java` | PrescriptionDraftContext | 4 |
| 25 | `prescription/.../context/DosageAlertTest.java` | DosageAlert | 1 |
| 26 | `prescription/.../converter/AuditConverterTest.java` | AuditConverter | 6 |
| 27 | `prescription/.../api/PrescriptionAuditControllerTest.java` | PrescriptionAuditController | 6 |
| 28 | `prescription/.../impl/PrescriptionAuditServiceImplTest.java` | PrescriptionAuditServiceImpl | 16 |

> 共 **78** 个单元测试用例。

## 覆盖维度

### 正常路径
- 各 DTO 的 getter/setter 置值读取验证
- AuditConverter 的正向映射：AuditRequest → PrescriptionCheckRequest，AiResult → AuditResponse
- AllergyCheckRule：无过敏信息时通过，非匹配过敏原时通过
- ContraindicationCheckRule：无合并症时通过
- DuplicateCheckRule：单药品时通过，成分无重叠时通过
- DosageLimitRule：剂量在范围内时通过
- SpecialPopulationDosageRule：普通年龄范围（15-64）时跳过
- DrugInteractionRule：始终 PASS
- **audit()**：AI 成功路径返回 AuditResponse
- **submit()**：PASS 级别直接提交成功
- **submit()**：forceSubmit=true + WARN + 处方一致 → 提交成功
- **revoke()**：WARN + isLatest → 撤销成功

### 边界条件
- DosageLimitRule：dose == singleMax 不触发；dose > singleMax 触发 WARN；dose > singleMax * 2 触发 BLOCK
- SpecialPopulationDosageRule：age=14（儿童）触发；age=65（老年）触发；age=14~65 跳过
- AllergyCheckRule：SEVERE → BLOCK；MODERATE/MILD → WARN
- ContraindicationCheckRule：ABSOLUTE_CONTRAINDICATION → BLOCK；RELATIVE_CONTRAINDICATION → WARN

### 错误路径
- **audit()**：AI success=false → 降级 LocalRuleEngine
- **audit()**：AI ExecutionException → 降级 LocalRuleEngine
- **audit()**：AI InterruptedException → 降级 LocalRuleEngine
- **submit()** 步①：存在 CRITICAL 告警 → 422 BlockResponse(RX_BLOCK_CRITICAL_DOSE)
- **submit()** 步②：riskLevel=BLOCK → 422 BlockResponse(RX_BLOCK_AUDIT)
- **submit()** 步③ forceSubmit=false + WARN + 处方变更 → RX_AUDIT_PRESCRIPTION_MODIFIED
- **submit()** 步③ forceSubmit=false + WARN + 处方一致 → RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
- **submit()** 步③ forceSubmit=true + auditRecordId 不匹配 → RX_AUDIT_FORCE_SUBMIT_INVALID
- **submit()** 步③ forceSubmit=true + 非 WARN → RX_AUDIT_FORCE_SUBMIT_INVALID
- **submit()** 步③ OptimisticLockException → RX_AUDIT_CONCURRENT_SUBMIT
- **revoke()** auditId 不存在 → BusinessException(RX_AUDIT_REVOKE_NOT_FOUND)
- **revoke()** isLatest=false → BusinessException(RX_AUDIT_REVOKE_ALREADY_REVOKED)
- **revoke()** riskLevel≠WARN → BusinessException(RX_AUDIT_REVOKE_NOT_WARN)

### 状态交互
- **audit()** 持久化：查询已有 isLatest=true 记录 → 标记 false → 新记录 isLatest=true、auditSequence+1
- **submit()** 步① → 步② 间 SubmitContext 快照传递
- **submit()** 步② → 步③ 二次 CRITICAL 增量检测
- **submit()** 无最新审核结果 → 自动执行 audit() 后走对应路径
- **revoke()** 成功 → isLatest→false

## 测试框架

JUnit 5 + Mockito。规则类测试使用 `@ExtendWith(MockitoExtension.class)` + `@Mock` 注入 Repository 依赖。
Service 和 Controller 测试同理，通过构造器传入 Mock 实例。

## 约定

- 测试文件与源码包路径对齐，放在 `src/test/java/com/aimedical/modules/prescription/` 下
- 每个被测类型对应一个测试文件
- 用例命名遵循 `shouldXxxWhenYyy` 模式
- 断言使用 AssertJ 风格的工具方法（assertEquals/assertTrue/assertThrows）

## 偏差说明

无。所有测试对齐详细设计 v10 行为契约，未引入额外依赖。
