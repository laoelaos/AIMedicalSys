# 验证报告（v12）

## 结果
PASSED

## 统计
- 通过：166
- 失败：0

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
[INFO] patient                                                            [jar]
[INFO] doctor                                                             [jar]
[INFO] admin                                                              [jar]
[INFO] application                                                        [jar]
[INFO] integration                                                        [jar]
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                             [1/11]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT                                    [2/11]
[INFO]   from common\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.common.base.BaseEntityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s -- in com.aimedical.common.base.BaseEntityTest
[INFO] Running com.aimedical.common.base.BaseEnumTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.common.base.BaseEnumTest
[INFO] Running com.aimedical.common.CommonPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.common.CommonPlaceholderTest
[INFO] Running com.aimedical.common.config.GlobalExceptionHandlerTest
12:30:27.487 [main] ERROR com.aimedical.common.config.GlobalExceptionHandler -- System exception
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
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.083 s -- in com.aimedical.common.config.GlobalExceptionHandlerTest
[INFO] Running com.aimedical.common.config.JacksonConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.182 s -- in com.aimedical.common.config.JacksonConfigTest
[INFO] Running com.aimedical.common.config.JpaConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.common.config.JpaConfigTest
[INFO] Running com.aimedical.common.exception.BusinessExceptionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.common.exception.BusinessExceptionTest
[INFO] Running com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Running com.aimedical.common.result.PageQueryTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.common.result.PageQueryTest
[INFO] Running com.aimedical.common.result.PageResponseTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.common.result.PageResponseTest
[INFO] Running com.aimedical.common.result.ResultTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.common.result.ResultTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ common ---
[INFO] 
[INFO] ------------------< com.aimedical:common-module-api >-------------------
[INFO] Building common-module-api 0.0.1-SNAPSHOT                         [3/11]
[INFO]   from common-module-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-api ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.059 s -- in com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ common-module-api ---
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT                        [4/11]
[INFO]   from common-module-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Running com.aimedical.modules.commonmodule.permission.FunctionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.permission.FunctionTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Running com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ common-module-impl ---
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT                                    [5/11]
[INFO]   from ai-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-api ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.ai.api.AiResultTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in com.aimedical.modules.ai.api.AiResultTest
[INFO] Running com.aimedical.modules.ai.api.AiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.ai.api.AiServiceTest
[INFO] Running com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ ai-api ---
[INFO] 
[INFO] -----------------------< com.aimedical:ai-impl >------------------------
[INFO] Building ai-impl 0.0.1-SNAPSHOT                                   [6/11]
[INFO]   from ai-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.047 s -- in com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
mvn.cmd : WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agen
t\1.14.13\byte-buddy-agent-1.14.13.jar)
所在位置 行:1 字符: 1
+ & "C:\MyPrograms\apache-maven-3.9.15\bin\mvn.cmd" verify -pl integrat ...
+ ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (WARNING: A Java...nt-1.14.13.jar):String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has bee
n appended
12:30:31.686 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.035 s -- in com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ ai-impl ---
[INFO] 
[INFO] -----------------------< com.aimedical:patient >------------------------
[INFO] Building patient 0.0.1-SNAPSHOT                                   [7/11]
[INFO]   from patient\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ patient ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\src\test\resources
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 s -- in com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Running com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Running com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Running com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ patient ---
[INFO] 
[INFO] ------------------------< com.aimedical:doctor >------------------------
[INFO] Building doctor 0.0.1-SNAPSHOT                                    [8/11]
[INFO]   from doctor\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ doctor ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\src\test\resources
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.043 s -- in com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Running com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Running com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Running com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ doctor ---
[INFO] 
[INFO] ------------------------< com.aimedical:admin >-------------------------
[INFO] Building admin 0.0.1-SNAPSHOT                                     [9/11]
[INFO]   from admin\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ admin ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\src\test\resources
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.039 s -- in com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Running com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Running com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Running com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ admin ---
[INFO] 
[INFO] ---------------------< com.aimedical:application >----------------------
[INFO] Building application 0.0.1-SNAPSHOT                              [10/11]
[INFO]   from application\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ application ---
[INFO] Copying 3 resources from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ application ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ application ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ application ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ application ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.ApplicationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.038 s -- in com.aimedical.ApplicationPlaceholderTest
[INFO] Running com.aimedical.HealthControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.HealthControllerTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ application ---
[INFO] 
[INFO] --- spring-boot:3.2.5:repackage (repackage) @ application ---
[INFO] Attaching repackaged archive C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\target\application-0.0.1-SNAPSHOT-exec.jar with classifier exec
[INFO] 
[INFO] ---------------------< com.aimedical:integration >----------------------
[INFO] Building integration 0.0.1-SNAPSHOT                              [11/11]
[INFO]   from integration\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ integration ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ integration ---
[INFO] No sources to compile
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ integration ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ integration ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ integration ---
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ integration ---
[WARNING] JAR will be empty - no content was marked for inclusion!
[INFO] 
[INFO] --- failsafe:3.1.2:integration-test (default) @ integration ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.integration.ApplicationContextIT
12:30:36.134 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.aimedical.integration.ApplicationContextIT]: ApplicationContextIT does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
12:30:36.224 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.aimedical.Application for test class com.aimedical.integration.ApplicationContextIT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-17T12:30:36.604+08:00  INFO 25248 --- [aimedical-sys] [           main] c.a.integration.ApplicationContextIT     : Starting ApplicationContextIT using Java 21.0.11 with PID 25248 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration)
2026-06-17T12:30:36.606+08:00  INFO 25248 --- [aimedical-sys] [           main] c.a.integration.ApplicationContextIT     : The following 2 profiles are active: "phase0", "dev"
2026-06-17T12:30:37.323+08:00  INFO 25248 --- [aimedical-sys] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-17T12:30:37.371+08:00  INFO 25248 --- [aimedical-sys] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 42 ms. Found 4 JPA repository interfaces.
2026-06-17T12:30:38.423+08:00  INFO 25248 --- [aimedical-sys] [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-17T12:30:38.483+08:00  INFO 25248 --- [aimedical-sys] [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-06-17T12:30:38.518+08:00  INFO 25248 --- [aimedical-sys] [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-17T12:30:38.722+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-17T12:30:38.739+08:00  INFO 25248 --- [aimedical-sys] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-06-17T12:30:38.869+08:00  INFO 25248 --- [aimedical-sys] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:aimedical user=SA
2026-06-17T12:30:38.871+08:00  INFO 25248 --- [aimedical-sys] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-06-17T12:30:39.765+08:00  INFO 25248 --- [aimedical-sys] [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: create table admin_entity (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), primary key (id))
Hibernate: create table doctor_entity (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), primary key (id))
Hibernate: create table patient_entity (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), primary key (id))
Hibernate: create table post_function (post_id bigint not null, function_id bigint not null, primary key (post_id, function_id))
Hibernate: create table sys_function (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), code varchar(255) not null, description varchar(255), enabled boolean, name varchar(255), primary key (id))
Hibernate: create table sys_post (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), code varchar(255) not null, description varchar(255), enabled boolean, name varchar(255), role_id bigint, primary key (id))
Hibernate: create table sys_role (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), code varchar(255) not null, description varchar(255), enabled boolean, name varchar(255), primary key (id))
Hibernate: create table sys_user (id bigint generated by default as identity, created_at timestamp(6), deleted boolean not null, updated_at timestamp(6), email varchar(255), enabled boolean, nickname varchar(255), password varchar(255), phone varchar(255), user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')), username varchar(255) not null, primary key (id))
Hibernate: create table user_post (user_id bigint not null, post_id bigint not null, primary key (user_id, post_id))
Hibernate: create table user_role (user_id bigint not null, role_id bigint not null, primary key (user_id, role_id))
Hibernate: alter table if exists sys_function drop constraint if exists UK_agenm56t88ivhsqmlb0pmng7r
Hibernate: alter table if exists sys_function add constraint UK_agenm56t88ivhsqmlb0pmng7r unique (code)
Hibernate: alter table if exists sys_post drop constraint if exists UK_r5b7w4kya2gmxcc1asyns6odk
Hibernate: alter table if exists sys_post add constraint UK_r5b7w4kya2gmxcc1asyns6odk unique (code)
Hibernate: alter table if exists sys_role drop constraint if exists UK_plpigyqwsqfn7mn66npgf9ftp
Hibernate: alter table if exists sys_role add constraint UK_plpigyqwsqfn7mn66npgf9ftp unique (code)
Hibernate: alter table if exists sys_user drop constraint if exists UK_51bvuyvihefoh4kp5syh2jpi4
Hibernate: alter table if exists sys_user add constraint UK_51bvuyvihefoh4kp5syh2jpi4 unique (username)
Hibernate: alter table if exists post_function add constraint FKh56snoidh814t7tmnsvgkyp6c foreign key (function_id) references sys_function
Hibernate: alter table if exists post_function add constraint FKbv50wilq40pjojsdm6sg6g2xg foreign key (post_id) references sys_post
Hibernate: alter table if exists sys_post add constraint FKjfpb3no7elnlin0vwqbx940gu foreign key (role_id) references sys_role
Hibernate: alter table if exists user_post add constraint FK1qq5m5bsjagqw0s8m1cyb1rmj foreign key (post_id) references sys_post
Hibernate: alter table if exists user_post add constraint FKafwurpfqy3g4a4k0xnse3l8vy foreign key (user_id) references sys_user
Hibernate: alter table if exists user_role add constraint FKdec2ggmqwgdhhb59jw7o488wx foreign key (role_id) references sys_role
Hibernate: alter table if exists user_role add constraint FKsrs64lo4ci4xyu3da9clbiv8r foreign key (user_id) references sys_user
2026-06-17T12:30:39.834+08:00  INFO 25248 --- [aimedical-sys] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-17T12:30:40.048+08:00  WARN 25248 --- [aimedical-sys] [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-06-17T12:30:40.099+08:00  WARN 25248 --- [aimedical-sys] [           main] .s.s.UserDetailsServiceAutoConfiguration : 

Using generated security password: d7906554-360f-49f1-b550-a3bc4667c1d2

This generated password is for development use only. Your security configuration must be updated before running your application in production.

2026-06-17T12:30:40.761+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 3 endpoint(s) beneath base path '/actuator'
2026-06-17T12:30:40.846+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.s.web.DefaultSecurityFilterChain     : Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@15eaa2ce, org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@1d0fae45, org.springframework.security.web.context.SecurityContextHolderFilter@49c3487f, org.springframework.security.web.header.HeaderWriterFilter@a824f87, org.springframework.web.filter.CorsFilter@596343e7, org.springframework.security.web.authentication.logout.LogoutFilter@2fb171f5, org.springframework.security.web.savedrequest.RequestCacheAwareFilter@67e7fe73, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter@5f1d270a, org.springframework.security.web.authentication.AnonymousAuthenticationFilter@3b25ce5e, org.springframework.security.web.access.ExceptionTranslationFilter@718198db, org.springframework.security.web.access.intercept.AuthorizationFilter@24eb71f6]
2026-06-17T12:30:41.709+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.b.a.h2.H2ConsoleAutoConfiguration    : H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:aimedical'
2026-06-17T12:30:41.792+08:00  INFO 25248 --- [aimedical-sys] [           main] c.a.integration.ApplicationContextIT     : Started ApplicationContextIT in 5.456 seconds (process running for 6.225)
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\
byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has bee
n appended
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.280 s -- in com.aimedical.integration.ApplicationContextIT
[INFO] Running com.aimedical.integration.HealthCheckIT
2026-06-17T12:30:42.291+08:00  INFO 25248 --- [aimedical-sys] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.aimedical.integration.HealthCheckIT]: HealthCheckIT does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-06-17T12:30:42.292+08:00  INFO 25248 --- [aimedical-sys] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.aimedical.Application for test class com.aimedical.integration.HealthCheckIT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-17T12:30:42.321+08:00  INFO 25248 --- [aimedical-sys] [           main] c.aimedical.integration.HealthCheckIT    : Starting HealthCheckIT using Java 21.0.11 with PID 25248 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration)
2026-06-17T12:30:42.322+08:00  INFO 25248 --- [aimedical-sys] [           main] c.aimedical.integration.HealthCheckIT    : The following 2 profiles are active: "phase0", "dev"
2026-06-17T12:30:42.488+08:00  INFO 25248 --- [aimedical-sys] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-17T12:30:42.497+08:00  INFO 25248 --- [aimedical-sys] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 8 ms. Found 4 JPA repository interfaces.
2026-06-17T12:30:42.785+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 0 (http)
2026-06-17T12:30:42.796+08:00  INFO 25248 --- [aimedical-sys] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-06-17T12:30:42.797+08:00  INFO 25248 --- [aimedical-sys] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.20]
2026-06-17T12:30:42.847+08:00  INFO 25248 --- [aimedical-sys] [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2026-06-17T12:30:42.847+08:00  INFO 25248 --- [aimedical-sys] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 524 ms
2026-06-17T12:30:42.878+08:00  INFO 25248 --- [aimedical-sys] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Starting...
2026-06-17T12:30:42.879+08:00  INFO 25248 --- [aimedical-sys] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-2 - Added connection conn10: url=jdbc:h2:mem:aimedical user=SA
2026-06-17T12:30:42.879+08:00  INFO 25248 --- [aimedical-sys] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Start completed.
2026-06-17T12:30:42.879+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.b.a.h2.H2ConsoleAutoConfiguration    : H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:aimedical'
2026-06-17T12:30:42.951+08:00  INFO 25248 --- [aimedical-sys] [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-17T12:30:42.954+08:00  INFO 25248 --- [aimedical-sys] [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-17T12:30:42.956+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-17T12:30:43.087+08:00  INFO 25248 --- [aimedical-sys] [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-06-17T12:30:43.107+08:00  INFO 25248 --- [aimedical-sys] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-17T12:30:43.158+08:00  WARN 25248 --- [aimedical-sys] [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-06-17T12:30:43.172+08:00  WARN 25248 --- [aimedical-sys] [           main] .s.s.UserDetailsServiceAutoConfiguration : 

Using generated security password: 264f0352-0727-489d-9e84-b75d39ceabbc

This generated password is for development use only. Your security configuration must be updated before running your application in production.

2026-06-17T12:30:43.283+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 3 endpoint(s) beneath base path '/actuator'
2026-06-17T12:30:43.298+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.s.web.DefaultSecurityFilterChain     : Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@7e452d2b, org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@37a1ac0, org.springframework.security.web.context.SecurityContextHolderFilter@3def5336, org.springframework.security.web.header.HeaderWriterFilter@69339b3b, org.springframework.web.filter.CorsFilter@615ef647, org.springframework.security.web.authentication.logout.LogoutFilter@21624bde, org.springframework.security.web.savedrequest.RequestCacheAwareFilter@3789bd95, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter@31f575aa, org.springframework.security.web.authentication.AnonymousAuthenticationFilter@73a91b68, org.springframework.security.web.access.ExceptionTranslationFilter@51959e46, org.springframework.security.web.access.intercept.AuthorizationFilter@5c5432d9]
2026-06-17T12:30:43.498+08:00  INFO 25248 --- [aimedical-sys] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 58373 (http) with context path ''
2026-06-17T12:30:43.503+08:00  INFO 25248 --- [aimedical-sys] [           main] c.aimedical.integration.HealthCheckIT    : Started HealthCheckIT in 1.207 seconds (process running for 7.937)
2026-06-17T12:30:43.617+08:00  INFO 25248 --- [aimedical-sys] [o-auto-1-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2026-06-17T12:30:43.618+08:00  INFO 25248 --- [aimedical-sys] [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2026-06-17T12:30:43.619+08:00  INFO 25248 --- [aimedical-sys] [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.402 s -- in com.aimedical.integration.HealthCheckIT
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- failsafe:3.1.2:verify (default) @ integration ---
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.003 s]
[INFO] common ............................................. SUCCESS [  2.229 s]
[INFO] common-module-api .................................. SUCCESS [  0.690 s]
[INFO] common-module-impl ................................. SUCCESS [  0.703 s]
[INFO] ai-api ............................................. SUCCESS [  0.731 s]
[INFO] ai-impl ............................................ SUCCESS [  1.710 s]
[INFO] patient ............................................ SUCCESS [  0.684 s]
[INFO] doctor ............................................. SUCCESS [  0.703 s]
[INFO] admin .............................................. SUCCESS [  0.672 s]
[INFO] application ........................................ SUCCESS [  1.398 s]
[INFO] integration ........................................ SUCCESS [  8.603 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  18.504 s
[INFO] Finished at: 2026-06-17T12:30:43+08:00
[INFO] ------------------------------------------------------------------------
