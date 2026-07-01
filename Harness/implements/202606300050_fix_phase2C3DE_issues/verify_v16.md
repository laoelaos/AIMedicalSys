# 验证报告（v16）

## 结果
FAILED

## 统计
### 第1次（非clean构建，consultation模块JVM fork崩溃 + common-module-impl LoginAttemptTracker 时序竞争）
- consultation: 140 测试中有 RegistrationEventListenerTest JVM fork 崩溃 + NoSuchMethodError（5 Errors, 均与 stale class 有关）
- common-module-impl: 400/0/1/1（LoginAttemptTrackerTest.shouldResetUsernameFailuresWhenWindowExpires 时序竞争）
- prescription: 176/0/0/0 ✅

### 第2次（clean构建）
- common: 201/0/0/5 ✅
- common-module-api: 86/0/0/0 ✅
- common-module-impl: 400/0/0/1 ✅
- ai-api: 132/0/0/0 ✅
- ai-impl: 53/0/0/0 ✅
- patient: 46/0/0/0 ✅
- doctor: 14/0/0/0 ✅
- admin: 27/0/0/0 ✅
- consultation: 140/0/0/0 ✅
- prescription: 176/0/1/0（❌ auditShouldHandleAiDataNull: NPE）
- medical-record / application / integration: SKIPPED（因 prescription 构建中止）

### 总结
- prescription 模块 v16 新增测试中存在 1 个 NPE 错误：`auditShouldHandleAiDataNull` 调用 `AiResult.success(null)`，但 `AiResult.success()` 使用 `Objects.requireNonNull(data)` 拒绝 null，直接抛出 NPE
- 其余预存在模块（common、consultation 等）在 clean 构建下全部通过

## 测试执行日志

[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.569 s -- in com.aimedical.modules.prescription.context.PrescriptionDraftContextTest
[INFO] Running com.aimedical.modules.prescription.converter.AssistConverterTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.155 s -- in com.aimedical.modules.prescription.converter.AssistConverterTest
[INFO] Running com.aimedical.modules.prescription.converter.AuditConverterTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.165 s -- in com.aimedical.modules.prescription.converter.AuditConverterTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AiSuggestionResultTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AiSuggestionResultTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AiSuggestionStatusTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.modules.prescription.dto.assist.AiSuggestionStatusTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AllergyWarningItemTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.aimedical.modules.prescription.dto.assist.AllergyWarningItemTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverityTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageAlertLevelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.assist.DosageAlertLevelTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.045 s -- in com.aimedical.modules.prescription.dto.assist.DosageAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageCheckRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.assist.DosageCheckRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageCheckResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.prescription.dto.assist.DosageCheckResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DoseWarningTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.assist.DoseWarningTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DoseWarningTypeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in com.aimedical.modules.prescription.dto.assist.DoseWarningTypeTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AlertSeverityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.prescription.dto.audit.AlertSeverityTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AllergyDetailTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.dto.audit.AllergyDetailTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.030 s -- in com.aimedical.modules.prescription.dto.audit.AuditAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditIssueTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.prescription.dto.audit.AuditIssueTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.dto.audit.AuditRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.audit.AuditResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.BlockResponseTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.audit.BlockResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.DrugInteractionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.audit.DrugInteractionTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.PatientInfoTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.prescription.dto.audit.PatientInfoTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.PrescriptionItemTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.prescription.dto.audit.PrescriptionItemTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SubmitRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.prescription.dto.audit.SubmitRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SubmitResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.SubmitResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SuggestionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.prescription.dto.audit.SuggestionTest
[INFO] Running com.aimedical.modules.prescription.entity.AuditRecordTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.prescription.entity.AuditRecordTest
[INFO] Running com.aimedical.modules.prescription.PrescriptionErrorCodeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.053 s -- in com.aimedical.modules.prescription.PrescriptionErrorCodeTest
[INFO] Running com.aimedical.modules.prescription.PrescriptionPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.033 s -- in com.aimedical.modules.prescription.PrescriptionPlaceholderTest
[INFO] Running com.aimedical.modules.prescription.rule.AllergyCheckRuleTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.653 s -- in com.aimedical.modules.prescription.rule.AllergyCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.ContraindicationCheckRuleTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.282 s -- in com.aimedical.modules.prescription.rule.ContraindicationCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DefaultLocalRuleEngineTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.475 s -- in com.aimedical.modules.prescription.rule.DefaultLocalRuleEngineTest
[INFO] Running com.aimedical.modules.prescription.rule.DosageLimitRuleTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.151 s -- in com.aimedical.modules.prescription.rule.DosageLimitRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DrugInteractionRuleTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.prescription.rule.DrugInteractionRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DuplicateCheckRuleTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.182 s -- in com.aimedical.modules.prescription.rule.DuplicateCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugAllergyMappingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.prescription.rule.entity.DrugAllergyMappingTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugCompositionDictTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.rule.entity.DrugCompositionDictTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugContraindicationMappingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.rule.entity.DrugContraindicationMappingTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugInteractionPairTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.rule.entity.DrugInteractionPairTest
[INFO] Running com.aimedical.modules.prescription.rule.LocalRuleResultTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.prescription.rule.LocalRuleResultTest
[INFO] Running com.aimedical.modules.prescription.rule.SpecialPopulationDosageRuleTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.aimedical.modules.prescription.rule.SpecialPopulationDosageRuleTest
[INFO] Running com.aimedical.modules.prescription.service.assist.DedupTaskSchedulerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.122 s -- in com.aimedical.modules.prescription.service.assist.DedupTaskSchedulerTest
[INFO] Running com.aimedical.modules.prescription.service.assist.DosageThresholdServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.040 s -- in com.aimedical.modules.prescription.service.assist.DosageThresholdServiceTest
[INFO] Running com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest
12:29:38.448 [main] WARN com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 1ms: RuntimeException
12:29:38.618 [main] WARN com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 0ms: RuntimeException
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.234 s -- in com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest
[INFO] Running com.aimedical.modules.prescription.service.audit.AuditRiskLevelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.service.audit.AuditRiskLevelTest
[INFO] Running com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditEnforcerImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditEnforcerImplTest
[INFO] Running com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
12:29:40.019 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 0ms: RuntimeException
12:29:40.066 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=null
12:29:40.301 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
12:29:40.332 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=null
12:29:40.419 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
12:29:40.646 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
12:29:44.313 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
12:29:44.315 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- Failed to serialize audit issues
com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest$2: fail
	at com.fasterxml.jackson.databind.ObjectMapper.writeValueAsString(ObjectMapper.java:3962)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl.persistAuditRecord(PrescriptionAuditServiceImpl.java:421)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl.audit(PrescriptionAuditServiceImpl.java:137)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.lambda$auditShouldHandleAuditIssuesSerializationFailureGracefully$2(PrescriptionAuditServiceImplTest.java:414)
	at org.junit.jupiter.api.AssertDoesNotThrow.assertDoesNotThrow(AssertDoesNotThrow.java:71)
	at org.junit.jupiter.api.AssertDoesNotThrow.assertDoesNotThrow(AssertDoesNotThrow.java:58)
	at org.junit.jupiter.api.Assertions.assertDoesNotThrow(Assertions.java:3228)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.auditShouldHandleAuditIssuesSerializationFailureGracefully(PrescriptionAuditServiceImplTest.java:414)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
12:29:44.357 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR_FAIL
12:29:44.374 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 0ms: RuntimeException
12:29:44.384 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
12:29:44.782 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.071 s -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 176, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:00 min
[INFO] Finished at: 2026-06-30T12:29:46+08:00
[INFO] ------------------------------------------------------------------------
[INFO] Building patient 0.0.1-SNAPSHOT                                   [9/16]
[INFO]   from modules\patient\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ patient ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\patient\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ patient ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ patient ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ patient ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.074 s -- in com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Running com.aimedical.modules.patient.entity.AllergyHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.aimedical.modules.patient.entity.AllergyHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.AllergySeverityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.patient.entity.AllergySeverityTest
[INFO] Running com.aimedical.modules.patient.entity.BloodTypeTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.patient.entity.BloodTypeTest
[INFO] Running com.aimedical.modules.patient.entity.ChronicDiseaseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.patient.entity.ChronicDiseaseTest
[INFO] Running com.aimedical.modules.patient.entity.DiseaseStatusTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.patient.entity.DiseaseStatusTest
[INFO] Running com.aimedical.modules.patient.entity.FamilyHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.patient.entity.FamilyHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.GenderTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.patient.entity.GenderTest
[INFO] Running com.aimedical.modules.patient.entity.HealthProfileTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.patient.entity.HealthProfileTest
[INFO] Running com.aimedical.modules.patient.entity.MedicationHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.patient.entity.MedicationHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Running com.aimedical.modules.patient.entity.SurgeryHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.patient.entity.SurgeryHistoryTest
[INFO] Running com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Running com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ------------------------< com.aimedical:doctor >------------------------
[INFO] Building doctor 0.0.1-SNAPSHOT                                   [10/16]
[INFO]   from modules\doctor\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ doctor ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\doctor\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ doctor ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ doctor ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ doctor ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.073 s -- in com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Running com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Running com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s -- in com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Running com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ------------------------< com.aimedical:admin >-------------------------
[INFO] Building admin 0.0.1-SNAPSHOT                                    [11/16]
[INFO]   from modules\admin\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ admin ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\admin\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ admin ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ admin ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ admin ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.056 s -- in com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Running com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Running com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Running com.aimedical.modules.admin.entity.dict.DictDataTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.aimedical.modules.admin.entity.dict.DictDataTest
[INFO] Running com.aimedical.modules.admin.entity.dict.DictTypeTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.admin.entity.dict.DictTypeTest
[INFO] Running com.aimedical.modules.admin.entity.LoginTypeTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.admin.entity.LoginTypeTest
[INFO] Running com.aimedical.modules.admin.entity.TokenStoreTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.admin.entity.TokenStoreTest
[INFO] Running com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ---------------------< com.aimedical:consultation >---------------------
[INFO] Building consultation 0.0.1-SNAPSHOT                             [12/16]
[INFO]   from modules\consultation\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ consultation ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\consultation\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ consultation ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ consultation ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ consultation ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.consultation.ConsultationDtoTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.099 s -- in com.aimedical.modules.consultation.ConsultationDtoTest
[INFO] Running com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Running com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Running com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.983 s -- in com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Running com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.161 s -- in com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionManagerTest
12:33:32.079 [Thread-4] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.168 [Thread-3] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.168 [Thread-11] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.170 [Thread-7] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.170 [Thread-10] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.170 [Thread-6] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.170 [Thread-8] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.170 [Thread-9] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.183 [Thread-5] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
12:33:33.187 [main] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: session-001, returning existing session
12:33:33.247 [Thread-14] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-21] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-18] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-13] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-16] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-20] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-19] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-17] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
12:33:33.247 [Thread-15] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.421 s -- in com.aimedical.modules.consultation.DialogueSessionManagerTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.092 s -- in com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Running com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[WARNING] Corrupted channel by directly writing to native stream in forked JVM 1. See FAQ web page and the dump file C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\target\surefire-reports\2026-06-30T12-30-56_542-jvmRun1.dumpstream
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  1.275 s]
[INFO] common ............................................. SUCCESS [01:58 min]
[INFO] Common Module Aggregator ........................... SUCCESS [  0.036 s]
[INFO] common-module-api .................................. SUCCESS [  1.905 s]
[INFO] common-module-impl ................................. SUCCESS [ 31.429 s]
[INFO] AI Module Aggregator ............................... SUCCESS [  0.003 s]
[INFO] ai-api ............................................. SUCCESS [  1.336 s]
[INFO] ai-impl ............................................ SUCCESS [  2.880 s]
[INFO] patient ............................................ SUCCESS [  1.303 s]
[INFO] doctor ............................................. SUCCESS [  1.186 s]
[INFO] admin .............................................. SUCCESS [  1.216 s]
[INFO] consultation ....................................... FAILURE [  6.490 s]
[INFO] prescription ....................................... SKIPPED
[INFO] medical-record ..................................... SKIPPED
[INFO] application ........................................ SKIPPED
[INFO] integration ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:50 min
[INFO] Finished at: 2026-06-30T12:33:33+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project consultation: 
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Command was cmd.exe /X /C "C:\Users\laoE\.jdks\ms-21.0.11\bin\java -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\consultation\\target\\jacoco.exec -jar C:\Users\laoE\AppData\Local\Temp\surefire8864187534373968949\surefirebooter-20260630123327452_93.jar C:\Users\laoE\AppData\Local\Temp\surefire8864187534373968949 2026-06-30T12-30-56_542-jvmRun1 surefire-20260630123327452_91tmp surefire_8-20260630123327452_92tmp"
[ERROR] Error occurred in starting fork, check output in log
[ERROR] Process Exit Code: 1
[ERROR] Crashed tests:
[ERROR] com.aimedical.modules.consultation.RegistrationEventListenerTest
[ERROR] org.apache.maven.surefire.booter.SurefireBooterForkException: The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Command was cmd.exe /X /C "C:\Users\laoE\.jdks\ms-21.0.11\bin\java -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\consultation\\target\\jacoco.exec -jar C:\Users\laoE\AppData\Local\Temp\surefire8864187534373968949\surefirebooter-20260630123327452_93.jar C:\Users\laoE\AppData\Local\Temp\surefire8864187534373968949 2026-06-30T12-30-56_542-jvmRun1 surefire-20260630123327452_91tmp surefire_8-20260630123327452_92tmp"
[ERROR] Error occurred in starting fork, check output in log
[ERROR] Process Exit Code: 1
[ERROR] Crashed tests:
[ERROR] com.aimedical.modules.consultation.RegistrationEventListenerTest
[ERROR] 	at org.apache.maven.plugin.surefire.booterclient.ForkStarter.fork(ForkStarter.java:643)
[ERROR] 	at org.apache.maven.plugin.surefire.booterclient.ForkStarter.run(ForkStarter.java:285)
[ERROR] 	at org.apache.maven.plugin.surefire.booterclient.ForkStarter.run(ForkStarter.java:250)
[ERROR] 	at org.apache.maven.plugin.surefire.AbstractSurefireMojo.executeProvider(AbstractSurefireMojo.java:1203)
[ERROR] 	at org.apache.maven.plugin.surefire.AbstractSurefireMojo.executeAfterPreconditionsChecked(AbstractSurefireMojo.java:1055)
[ERROR] 	at org.apache.maven.plugin.surefire.AbstractSurefireMojo.execute(AbstractSurefireMojo.java:871)
[ERROR] 	at org.apache.maven.plugin.DefaultBuildPluginManager.executeMojo(DefaultBuildPluginManager.java:126)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor.doExecute2(MojoExecutor.java:328)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor.doExecute(MojoExecutor.java:316)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor.execute(MojoExecutor.java:212)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor.execute(MojoExecutor.java:174)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor.access$000(MojoExecutor.java:75)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor$1.run(MojoExecutor.java:162)
[ERROR] 	at org.apache.maven.plugin.DefaultMojosExecutionStrategy.execute(DefaultMojosExecutionStrategy.java:39)
[ERROR] 	at org.apache.maven.lifecycle.internal.MojoExecutor.execute(MojoExecutor.java:159)
[ERROR] 	at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject(LifecycleModuleBuilder.java:105)
[ERROR] 	at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject(LifecycleModuleBuilder.java:73)
[ERROR] 	at org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder.build(SingleThreadedBuilder.java:53)
[ERROR] 	at org.apache.maven.lifecycle.internal.LifecycleStarter.execute(LifecycleStarter.java:118)
[ERROR] 	at org.apache.maven.DefaultMaven.doExecute(DefaultMaven.java:261)
[ERROR] 	at org.apache.maven.DefaultMaven.doExecute(DefaultMaven.java:173)
[ERROR] 	at org.apache.maven.DefaultMaven.execute(DefaultMaven.java:101)
[ERROR] 	at org.apache.maven.cli.MavenCli.execute(MavenCli.java:919)
[ERROR] 	at org.apache.maven.cli.MavenCli.doMain(MavenCli.java:285)
[ERROR] 	at org.apache.maven.cli.MavenCli.main(MavenCli.java:207)
[ERROR] 	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
[ERROR] 	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
[ERROR] 	at org.codehaus.plexus.classworlds.launcher.Launcher.launchEnhanced(Launcher.java:255)
[ERROR] 	at org.codehaus.plexus.classworlds.launcher.Launcher.launch(Launcher.java:201)
[ERROR] 	at org.codehaus.plexus.classworlds.launcher.Launcher.mainWithExitCode(Launcher.java:361)
[ERROR] 	at org.codehaus.plexus.classworlds.launcher.Launcher.main(Launcher.java:314)
[ERROR] 
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoExecutionException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :consultation
12:34:16.514 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.lang.RuntimeException DoctorFacade error
12:34:16.529 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.RuntimeException DoctorFacade error
[ERROR] Tests run: 38, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.164 s <<< FAILURE! -- in com.aimedical.modules.consultation.TriageServiceImplTest
[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetCorrectedChiefComplaintFromRequestToSession -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setCorrectedChiefComplaint(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:52)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:92)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetCorrectedChiefComplaintFromRequestToSession(TriageServiceImplTest.java:268)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setCorrectedChiefComplaint(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:52)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:92)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest(TriageServiceImplTest.java:299)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionCcIsNull:134 NoSuchMethod 'java.lang.String com.aimedical.modules.ai.api.dto.triage.TriageRequest.getCorrectedChiefComplaint()'
[ERROR]   TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionIsNull:122 NoSuchMethod 'java.lang.String com.aimedical.modules.ai.api.dto.triage.TriageRequest.getCorrectedChiefComplaint()'
[ERROR]   TriageConverterTest.shouldPassCorrectedChiefComplaintFromSessionToAiRequest:111 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setCorrectedChiefComplaint(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest:299 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setCorrectedChiefComplaint(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSetCorrectedChiefComplaintFromRequestToSession:268 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setCorrectedChiefComplaint(java.lang.String)'
[INFO] 
[ERROR] Tests run: 140, Failures: 0, Errors: 5, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.166 s
[INFO] Finished at: 2026-06-30T12:34:16+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project consultation: 
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
Hibernate: create table sys_function (deleted boolean not null, enabled boolean not null, sort_order integer not null, visible boolean not null, created_at timestamp(6), id bigint generated by default as identity, parent_id bigint, updated_at timestamp(6), type varchar(20), code varchar(255) not null unique, component varchar(255), description varchar(255), icon varchar(255), name varchar(255) not null, path varchar(255), primary key (id))
Hibernate: create table sys_post (deleted boolean not null, enabled boolean not null, sort integer, created_at timestamp(6), id bigint generated by default as identity, role_id bigint, updated_at timestamp(6), code varchar(255) not null unique, description varchar(255), name varchar(255), primary key (id))
Hibernate: create table sys_role (deleted boolean not null, enabled boolean not null, sort integer not null, created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), code varchar(255) not null unique, description varchar(255), name varchar(255), primary key (id))
Hibernate: create table sys_user (deleted boolean not null, enabled boolean not null, password_change_required boolean not null, token_version integer not null, created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')), email varchar(255), nickname varchar(255) not null, password varchar(255) not null, phone varchar(255), username varchar(255) not null unique, primary key (id))
Hibernate: create table user_post (post_id bigint not null, user_id bigint not null, primary key (post_id, user_id))
Hibernate: create table user_role (role_id bigint not null, user_id bigint not null, primary key (role_id, user_id))
Hibernate: alter table if exists post_function add constraint FKh56snoidh814t7tmnsvgkyp6c foreign key (function_id) references sys_function
Hibernate: alter table if exists post_function add constraint FKbv50wilq40pjojsdm6sg6g2xg foreign key (post_id) references sys_post
Hibernate: alter table if exists sys_function add constraint FKmp2cmbi9l9c1c7618t2o0v2xb foreign key (parent_id) references sys_function
Hibernate: alter table if exists sys_post add constraint FKjfpb3no7elnlin0vwqbx940gu foreign key (role_id) references sys_role
Hibernate: alter table if exists user_post add constraint FK1qq5m5bsjagqw0s8m1cyb1rmj foreign key (post_id) references sys_post
Hibernate: alter table if exists user_post add constraint FKafwurpfqy3g4a4k0xnse3l8vy foreign key (user_id) references sys_user
Hibernate: alter table if exists user_role add constraint FKdec2ggmqwgdhhb59jw7o488wx foreign key (role_id) references sys_role
Hibernate: alter table if exists user_role add constraint FKsrs64lo4ci4xyu3da9clbiv8r foreign key (user_id) references sys_user
2026-06-30T12:45:44.271+08:00  INFO 27220 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-30T12:45:45.234+08:00  INFO 27220 --- [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-06-30T12:45:46.236+08:00  INFO 27220 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Started UserRepositoryTest in 8.504 seconds (process running for 62.804)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
2026-06-30T12:45:46.762+08:00  WARN 27220 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 23502, SQLState: 23502
2026-06-30T12:45:46.763+08:00 ERROR 27220 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : NULL not allowed for column "PASSWORD"; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [23502-224]
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,p1_0.user_id,p1_1.id,p1_1.code,p1_1.created_at,p1_1.deleted,p1_1.description,p1_1.enabled,f1_0.post_id,f1_1.id,f1_1.code,f1_1.component,f1_1.created_at,f1_1.deleted,f1_1.description,f1_1.enabled,f1_1.icon,f1_1.name,f1_1.parent_id,f1_1.path,f1_1.sort_order,f1_1.type,f1_1.updated_at,f1_1.visible,p1_1.name,p1_1.role_id,p1_1.sort,p1_1.updated_at,r2_0.user_id,r2_1.id,r2_1.code,r2_1.created_at,r2_1.deleted,r2_1.description,r2_1.enabled,r2_1.name,r2_1.sort,r2_1.updated_at,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 left join user_post p1_0 on u1_0.id=p1_0.user_id left join sys_post p1_1 on p1_1.id=p1_0.post_id left join post_function f1_0 on p1_1.id=f1_0.post_id left join sys_function f1_1 on f1_1.id=f1_0.function_id left join user_role r2_0 on u1_0.id=r2_0.user_id left join sys_role r2_1 on r2_1.id=r2_0.role_id where u1_0.id=?
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,p1_0.user_id,p1_1.id,p1_1.code,p1_1.created_at,p1_1.deleted,p1_1.description,p1_1.enabled,f1_0.post_id,f1_1.id,f1_1.code,f1_1.component,f1_1.created_at,f1_1.deleted,f1_1.description,f1_1.enabled,f1_1.icon,f1_1.name,f1_1.parent_id,f1_1.path,f1_1.sort_order,f1_1.type,f1_1.updated_at,f1_1.visible,p1_1.name,p1_1.role_id,p1_1.sort,p1_1.updated_at,r2_0.user_id,r2_1.id,r2_1.code,r2_1.created_at,r2_1.deleted,r2_1.description,r2_1.enabled,r2_1.name,r2_1.sort,r2_1.updated_at,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 left join user_post p1_0 on u1_0.id=p1_0.user_id left join sys_post p1_1 on p1_1.id=p1_0.post_id left join post_function f1_0 on p1_1.id=f1_0.post_id left join sys_function f1_1 on f1_1.id=f1_0.function_id left join user_role r2_0 on u1_0.id=r2_0.user_id left join sys_role r2_1 on r2_1.id=r2_0.role_id where u1_0.id=?
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.826 s -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest
2026-06-30T12:45:47.486+08:00  INFO 27220 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T12:45:47.516+08:00  INFO 27220 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T12:45:47.534+08:00  INFO 27220 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T12:45:47.543+08:00  INFO 27220 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T12:45:47.550+08:00  INFO 27220 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û������޸ĳɹ���userId: 1
2026-06-30T12:45:47.564+08:00  INFO 27220 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.638 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.134 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[WARNING] Tests run: 8, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.023 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.224 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   LoginAttemptTrackerTest.shouldResetUsernameFailuresWhenWindowExpires:240 expected: <5> but was: <4>
[INFO] 
[ERROR] Tests run: 400, Failures: 1, Errors: 0, Skipped: 1
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.246 s]
[INFO] common ............................................. SUCCESS [02:30 min]
[INFO] Common Module Aggregator ........................... SUCCESS [  1.152 s]
[INFO] common-module-api .................................. SUCCESS [01:13 min]
[INFO] common-module-impl ................................. FAILURE [01:38 min]
[INFO] AI Module Aggregator ............................... SKIPPED
[INFO] ai-api ............................................. SKIPPED
[INFO] ai-impl ............................................ SKIPPED
[INFO] patient ............................................ SKIPPED
[INFO] doctor ............................................. SKIPPED
[INFO] admin .............................................. SKIPPED
[INFO] consultation ....................................... SKIPPED
[INFO] prescription ....................................... SKIPPED
[INFO] medical-record ..................................... SKIPPED
[INFO] application ........................................ SKIPPED
[INFO] integration ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  05:25 min
[INFO] Finished at: 2026-06-30T12:45:48+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project common-module-impl: There are test failures.
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :common-module-impl
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
12:47:54.714 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR_FAIL
12:47:54.727 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 1ms: RuntimeException
12:47:54.733 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
12:47:54.824 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
[ERROR] Tests run: 44, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1.433 s <<< FAILURE! -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
[ERROR] com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.auditShouldHandleAiDataNull -- Time elapsed: 0.024 s <<< ERROR!
java.lang.NullPointerException
	at java.base/java.util.Objects.requireNonNull(Objects.java:233)
	at com.aimedical.modules.ai.api.AiResult.success(AiResult.java:25)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.auditShouldHandleAiDataNull(PrescriptionAuditServiceImplTest.java:455)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   PrescriptionAuditServiceImplTest.auditShouldHandleAiDataNull:455 ? NullPointer
[INFO] 
[ERROR] Tests run: 176, Failures: 0, Errors: 1, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.828 s]
[INFO] common ............................................. SUCCESS [ 22.483 s]
[INFO] Common Module Aggregator ........................... SUCCESS [  0.008 s]
[INFO] common-module-api .................................. SUCCESS [  2.185 s]
[INFO] common-module-impl ................................. SUCCESS [ 36.347 s]
[INFO] AI Module Aggregator ............................... SUCCESS [  0.004 s]
[INFO] ai-api ............................................. SUCCESS [  2.222 s]
[INFO] ai-impl ............................................ SUCCESS [  3.563 s]
[INFO] patient ............................................ SUCCESS [  3.847 s]
[INFO] doctor ............................................. SUCCESS [  3.244 s]
[INFO] admin .............................................. SUCCESS [  3.752 s]
[INFO] consultation ....................................... SUCCESS [  5.670 s]
[INFO] prescription ....................................... FAILURE [ 11.876 s]
[INFO] medical-record ..................................... SKIPPED
[INFO] application ........................................ SKIPPED
[INFO] integration ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:36 min
[INFO] Finished at: 2026-06-30T12:47:55+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project prescription: 
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\prescription\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :prescription
