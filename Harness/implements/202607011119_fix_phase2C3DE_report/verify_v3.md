# 验证报告（v3）

## 结果
PASSED

## 统计
- 通过：186
- 失败：0

## 测试执行日志

[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------------< com.aimedical:consultation >---------------------
[INFO] Building consultation 0.0.1-SNAPSHOT
[INFO]   from pom.xml
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
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 s -- in com.aimedical.modules.consultation.ConsultationDtoTest
[INFO] Running com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Running com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Running com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.334 s -- in com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Running com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
12:57:36.681 [main] WARN com.aimedical.modules.consultation.rule.DefaultTriageRuleEngine -- Failed to parse conditions JSON for rule, skipping: not valid json
com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'not': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: (String)"not valid json"; line: 1, column: 4]
	at com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:2477)
	at com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:760)
	at com.fasterxml.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(ReaderBasedJsonParser.java:3041)
	at com.fasterxml.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(ReaderBasedJsonParser.java:3019)
	at com.fasterxml.jackson.core.json.ReaderBasedJsonParser._matchToken(ReaderBasedJsonParser.java:2793)
	at com.fasterxml.jackson.core.json.ReaderBasedJsonParser._matchNull(ReaderBasedJsonParser.java:2779)
	at com.fasterxml.jackson.core.json.ReaderBasedJsonParser.nextToken(ReaderBasedJsonParser.java:778)
	at com.fasterxml.jackson.databind.ObjectMapper._readTreeAndClose(ObjectMapper.java:4854)
	at com.fasterxml.jackson.databind.ObjectMapper.readTree(ObjectMapper.java:3219)
	at com.aimedical.modules.consultation.rule.DefaultTriageRuleEngine.matchesConditions(DefaultTriageRuleEngine.java:90)
	at com.aimedical.modules.consultation.rule.DefaultTriageRuleEngine.match(DefaultTriageRuleEngine.java:67)
	at com.aimedical.modules.consultation.DefaultTriageRuleEngineTest.shouldPassRuleWhenConditionsInvalidJson(DefaultTriageRuleEngineTest.java:241)
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
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.177 s -- in com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionManagerTest
12:57:36.719 [Thread-4] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
12:57:36.719 [Thread-8] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
12:57:36.721 [Thread-10] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
12:57:36.721 [Thread-6] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
12:57:36.724 [Thread-14] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-19] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-13] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-21] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-20] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-18] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-17] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-16] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.724 [Thread-15] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
12:57:36.738 [main] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 550e8400-e29b-41d4-a716-446655440000, returning existing session
12:57:36.745 [Thread-34] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-33] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-35] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-36] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-37] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-38] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-39] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-40] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
12:57:36.745 [Thread-41] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.041 s -- in com.aimedical.modules.consultation.DialogueSessionManagerTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Running com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleEdgeCaseTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.057 s -- in com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleEdgeCaseTest
[INFO] Running com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.042 s -- in com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleTest
[INFO] Running com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] Running com.aimedical.modules.consultation.SchedulingRetryConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.consultation.SchedulingRetryConfigTest
[INFO] Running com.aimedical.modules.consultation.StaticDepartmentFallbackProviderTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.consultation.StaticDepartmentFallbackProviderTest
[INFO] Running com.aimedical.modules.consultation.TriageControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.consultation.TriageControllerTest
[INFO] Running com.aimedical.modules.consultation.TriageConverterTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.consultation.TriageConverterTest
[INFO] Running com.aimedical.modules.consultation.TriageServiceImplTest
12:57:36.967 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.TimeoutException null
12:57:36.973 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.InterruptedException null
12:57:36.996 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
12:57:36.996 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
12:57:37.105 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
12:57:42.176 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
[INFO] Tests run: 55, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.245 s -- in com.aimedical.modules.consultation.TriageServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 186, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.037 s
[INFO] Finished at: 2026-07-01T12:57:42+08:00
[INFO] ------------------------------------------------------------------------
