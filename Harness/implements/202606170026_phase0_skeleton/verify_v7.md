# 验证报告（v7）

## 结果
FAILED

## 统计
- 通过：0
- 失败：13（编译错误）

## 测试执行日志


[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] aimedical-sys                                                      [pom]
[INFO] common                                                             [jar]
[INFO] common-module-api                                                  [jar]
[INFO] common-module-impl                                                 [jar]
[INFO] ai-api                                                             [jar]
[INFO] ai-impl                                                            [jar]
[INFO] application                                                        [jar]
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                              [1/7]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ aimedical-sys ---
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT                                     [2/7]
[INFO]   from common\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ common ---
[INFO] Deleting C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 11 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 11 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.common.base.BaseEntityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 s -- in com.aimedical.common.base.BaseEntityTest
[INFO] Running com.aimedical.common.base.BaseEnumTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.common.base.BaseEnumTest
[INFO] Running com.aimedical.common.CommonPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.common.CommonPlaceholderTest
[INFO] Running com.aimedical.common.config.GlobalExceptionHandlerTest
02:13:51.130 [main] ERROR com.aimedical.common.config.GlobalExceptionHandler -- System exception
java.lang.RuntimeException: unexpected
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleGenericExceptionWith500(GlobalExceptionHandlerTest.java:57)
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
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.070 s -- in com.aimedical.common.config.GlobalExceptionHandlerTest
[INFO] Running com.aimedical.common.config.JacksonConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.169 s -- in com.aimedical.common.config.JacksonConfigTest
[INFO] Running com.aimedical.common.config.JpaConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.common.config.JpaConfigTest
[INFO] Running com.aimedical.common.exception.BusinessExceptionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.common.exception.BusinessExceptionTest
[INFO] Running com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Running com.aimedical.common.result.PageQueryTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.common.result.PageQueryTest
[INFO] Running com.aimedical.common.result.PageResponseTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.result.PageResponseTest
[INFO] Running com.aimedical.common.result.ResultTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.common.result.ResultTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ------------------< com.aimedical:common-module-api >-------------------
[INFO] Building common-module-api 0.0.1-SNAPSHOT                          [3/7]
[INFO]   from common-module-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ common-module-api ---
[INFO] Deleting C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-api ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 1 source file with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-api ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 1 source file with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-api ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.049 s -- in com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT                         [4/7]
[INFO]   from common-module-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ common-module-impl ---
[INFO] Deleting C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 5 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 6 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.035 s -- in com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Running com.aimedical.modules.commonmodule.permission.FunctionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.commonmodule.permission.FunctionTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Running com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT                                     [5/7]
[INFO]   from ai-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ ai-api ---
[INFO] Deleting C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 31 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-api ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 4 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-api ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.ai.api.AiResultTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.054 s -- in com.aimedical.modules.ai.api.AiResultTest
[INFO] Running com.aimedical.modules.ai.api.AiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.034 s -- in com.aimedical.modules.ai.api.AiServiceTest
[INFO] Running com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] -----------------------< com.aimedical:ai-impl >------------------------
[INFO] Building ai-impl 0.0.1-SNAPSHOT                                    [6/7]
[INFO]   from ai-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ ai-impl ---
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\test-classes
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[INFO] -------------------------------------------------------------
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[37,63] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.triage.TriageResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[52,66] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[63,74] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[74,78] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[85,84] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.inspection.InspectionReportResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[96,81] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.labtest.LabTestReportResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[107,70] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.image.ImageAnalysisResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[118,75] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.kb.KbQueryResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[129,77] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[140,75] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[151,80] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.execution.ExecutionOrderResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[162,65] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.schedule.ScheduleResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[173,77] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[INFO] 13 errors 
[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.172 s]
[INFO] common ............................................. SUCCESS [  3.597 s]
[INFO] common-module-api .................................. SUCCESS [  1.097 s]
[INFO] common-module-impl ................................. SUCCESS [  1.567 s]
[INFO] ai-api ............................................. SUCCESS [  1.473 s]
[INFO] ai-impl ............................................ FAILURE [  0.805 s]
[INFO] application ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.011 s
[INFO] Finished at: 2026-06-17T02:13:56+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:testCompile (default-testCompile) on project ai-impl: Compilation failure: Compilation failure: 
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[37,63] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.triage.TriageResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[52,66] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[63,74] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[74,78] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[85,84] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.inspection.InspectionReportResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[96,81] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.labtest.LabTestReportResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[107,70] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.image.ImageAnalysisResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[118,75] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.kb.KbQueryResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[129,77] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[140,75] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[151,80] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.execution.ExecutionOrderResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[162,65] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.schedule.ScheduleResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java:[173,77] �����ݵ�����: java.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionResponse>>�޷�ת��Ϊjava.util.concurrent.CompletableFuture<com.aimedical.modules.ai.api.AiResult<?>>
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :ai-impl

