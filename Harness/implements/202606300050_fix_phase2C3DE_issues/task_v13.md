# 任务指令（v13）

## 动作
NEW

## 任务描述
修复 prescription 模块 5 个测试失败以解除全量构建阻断，使后续模块（medical-record）可正常验证。

**系统级分析**：R3, R4, R5, R7, R9, R11, R12 连续 7 轮失败的根本原因是 **prescription 模块预存测试失败未被修复，每次全量 `mvn test` 都在该模块中断**，导致 medical-record 等后续模块被 SKIPPED。R12 自身变更（medical-record 测试编译修复）经验证完全正确（`mvn compile test-compile -pl modules/medical-record -q` 通过，consultation 140 测试全通过），但全量构建被 prescription 阻断。本轮实施手术式定点修复，仅解除 5 个阻断测试。

**修复清单**（只改 5 个文件：4 个测试 + 1 个生产代码，不动无关生产代码）：

### 1. PrescriptionErrorCodeTest.java:21 — 错误码消息不匹配
- **当前**：`assertEquals("WARN审核未确认", PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getMessage())`
- **改为**：`assertEquals("WARN 审核未确认，需 forceSubmit=true 放行", PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getMessage())`
- **说明**：PrescriptionErrorCode 生产代码的消息在之前轮次已被更新为更详细的文案，但测试未同步。

### 2. DosageLimitRuleTest.java:145 — 期望严重级别错误
- **当前**：`assertEquals(AuditRiskLevel.BLOCK, result.getSeverity())`
- **改为**：`assertEquals(AuditRiskLevel.WARN, result.getSeverity())`
- **说明**：测试中 dose=100, singleMax=50, dose 等于 2×singleMax。生产代码 `dose.compareTo(singleMax * 2) > 0` 为 false（100 > 100 假），落入 WARN 分支（`dose > singleMax` 真）。剂量等于双倍上限时正确返回 WARN 而非 BLOCK，测试期望错误。

### 3. PrescriptionAuditServiceImpl.java （buildStepThreeResponse 方法）+ 测试
- **生产代码修复**：`buildStepThreeResponse()` 方法中新增 BLOCK 风险等级分支，在 `riskLevel == AuditRiskLevel.PASS` 和 `riskLevel == AuditRiskLevel.WARN` 两个 if 块之间插入：
  ```java
  if (riskLevel == AuditRiskLevel.BLOCK) {
      SubmitResponse resp = new SubmitResponse();
      resp.setSubmitted(false);
      BlockResponse blockInfo = new BlockResponse(
          List.of("Prescription audit blocked"),
          "RX_BLOCK_AUDIT",
          LocalDateTime.now());
      resp.setBlockInfo(blockInfo);
      return resp;
  }
  ```
- **说明**：R9 A09 变更后，AI 返回 BLOCK 时进入 `buildStepThreeResponse` 但该方法的 BLOCK 分支不存在，误落入兜底 `submitted=true` 逻辑。这是 A09 变更的副作用。测试 `submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock` 期望 `assertFalse(result.isSubmitted())` 是正确的。

### 4. PrescriptionAssistServiceImplTest.java — 未 stub allergyCheckRule
- **在 setUp() 或 assistShouldGeneratePrescriptionIdWhenBlank 方法内增加**：
  ```java
  when(allergyCheckRule.check(any())).thenReturn(
      new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
  ```
- **位置**：在 `when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(new PrescriptionAssistResponse())` 之后（L84）或 `setUp()` 方法内。
- **说明**：`allergyCheckRule` 是 `@Mock`，`check()` 默认返回 null，`assist()` 流程中 `checkAllergies()` 调用 `ruleResult.isPassed()` 抛出 NPE。

### 5. PrescriptionAuditServiceImplTest.java:378 — 异常类型不匹配
- **当前**：
  ```java
  when(auditRecordRepository.save(any())).thenThrow(new jakarta.persistence.OptimisticLockException());
  ```
- **改为**：
  ```java
  when(auditRecordRepository.save(any())).thenThrow(
      new ObjectOptimisticLockingFailureException(
          "com.aimedical.modules.prescription.entity.AuditRecord",
          new jakarta.persistence.OptimisticLockException()));
  ```
- **说明**：生产代码 `catch (ObjectOptimisticLockingFailureException e)` 只能捕获 Spring 包装异常，Mockito mock 的 `save()` 直接抛 `OptimisticLockException` 不会被捕获。需要构造 Spring 的 `ObjectOptimisticLockingFailureException` 并传入可序列化的 resource description 字符串和原始异常。注意：不可使用 `auditRecordRepository.save(any()).getClass()`（mock 的 `save()` 默认返回 null，`null.getClass()` 抛出 NPE）。

**问题编号映射**（对应 plan.md 路线表 R13 行涉及问题列）：
| 修复项 | 关联问题 | 说明 |
|--------|---------|------|
| 1. PrescriptionErrorCodeTest 消息 | 预存测试同步 | 生产代码错误码消息已更新但测试未同步——非独立 P 编号，属于测试同步遗漏 |
| 2. DosageLimitRuleTest 严重级别 | 预存测试同步 / P11（域相关） | 测试期望 `dose=100 > 2×50` 为 BLOCK，但 `dose.compareTo(singleMax*2) > 0` 在等于时 false，WARN 正确——非 P11 直接修复，同属剂量规则域 |
| 3. PrescriptionAuditServiceImpl BLOCK 分支 | R9 A09 副作用 | A09（降级前置检查）在 PrescriptionAuditServiceImpl 中新增了 BLOCK 路径，但 `buildStepThreeResponse` 未同步新增 BLOCK 分支——非独立 P 编号，属前一轮变更副作用 |
| 4. PrescriptionAssistServiceImplTest NPE | 预存测试同步 / P14（域相关） | `allergyCheckRule` mock stub 遗漏——非 P14 直接修复，同属辅助开方流程域 |
| 5. PrescriptionAuditServiceImplTest 异常类型 | 预存测试同步 | `OptimisticLockException` 与 `ObjectOptimisticLockingFailureException` 类型不匹配——原始测试设计缺陷，非独立 P 编号 |

> 注：原路线表中 `P02,P05,P06,P11,P14` 标记为误植。R13 的 5 项修复均为预存测试同步问题（测试-生产代码不同步 + 测试设计缺陷），不直接对应实现报告中的 P02（DrugFacade 注入）、P05（SubmitResponse warnResult）或 P06（降级路径转换）。P11 和 P14 仅为域相关（同属剂量规则/辅助开方流程），非直接修复。已同步修正 plan.md 路线表。

## 选择理由
R3-R12 连续 7 轮失败的根本原因是 prescription 模块预存测试失败阻断了构建管线。修复 5 个测试即可解除阻断，使后续 medical-record 验证能被纳入全量构建。这是一个手术式"解除阻塞"的回合，不做 scope creep。此后其他模块可正常验证。

## 任务上下文
- 项目根目录：C:\Develop\Software\AIMedicalSys
- 涉及文件（均位于 `AIMedical/backend/modules/prescription` 下）：

| 文件 | 操作 | 类型 |
|------|------|------|
| `src/test/java/.../PrescriptionErrorCodeTest.java` | 修改 | 仅改断言 |
| `src/test/java/.../rule/DosageLimitRuleTest.java` | 修改 | 仅改断言 |
| `src/main/java/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 生产代码（buildStepThreeResponse 新增 BLOCK 分支） |
| `src/test/java/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | 仅改 mock 异常类型 |
| `src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | 新增 mock stub |

## 验证标准
```bash
# 全量测试通过，无阻断
mvn test -pl modules/prescription -am -q
# 结果: 无失败，无错误

# 全量构建通过
mvn test -q
# 结果: 所有模块均不是 FAILURE 状态
```

## 修订说明（v13 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| Item 5 示例代码 `auditRecordRepository.save(any()).getClass()` 导致 NPE（mock 的 save() 默认返回 null） | 改用可序列化字符串 `"com.aimedical.modules.prescription.entity.AuditRecord"` 作为 resource description；补充说明为何不可使用 `.getClass()` |
| "只改 4 个文件"与实际文件数不一致（实际改 5 个） | 修正为"只改 5 个文件：4 个测试 + 1 个生产代码" |
| plan.md R13 行涉及问题编号 P02,P05,P06,P11,P14 与修复清单无映射关系 | 在修复清单后新增"问题编号映射"说明表，澄清 P02/P05/P06 为误植（R13 修复预存测试同步，不直接对应实现报告问题），P11/P14 仅为域相关；同步修正 plan.md 路线表 |

## 修订说明（v13 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 缺失 P0 问题（P01+A03, P02/E06）和多项 P1 遗漏 | plan.md 已修订：新增 R14（P02/E06 DrugFacade注入）、R15（P06/P07/P08/P16 审核记录完善）、R18（P01+A03 异步AI调度）；A01 纳入 R17；P10/P13 纳入排期外说明；R14-R18 递移为 R16-R21，总轮次增至 R1-R21 |
