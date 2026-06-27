# 验证报告（v1）

## 结果
FAILED

## 统计
- 后端单元测试：通过 557，失败 0，跳过 5
- 后端集成测试：通过 0，错误 2（ApplicationContextIT, EntityMappingIT — JwtTokenProvider base64 解码异常）
- 前端构建（patient）：通过
- 前端构建（admin, doctor）：失败（vue-tsc 类型错误 — 找不到 pinia/element-plus 模块声明）

## 测试执行日志

[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] aimedical-sys                                                      [pom]
[INFO] common                                                             [jar]
[INFO] Common Module Aggregator                                           [pom]
[INFO] common-module-api                                                  [jar]
[INFO] common-module-impl                                                 [jar]
[INFO] AI Module Aggregator                                               [pom]
[INFO] ai-api                                                             [jar]
[INFO] ai-impl                                                            [jar]
[INFO] patient                                                            [jar]
[INFO] doctor                                                             [jar]
[INFO] admin                                                              [jar]
[INFO] application                                                        [jar]
[INFO] integration                                                        [jar]
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                             [1/13]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ aimedical-sys ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\aimedical-sys\0.0.1-SNAPSHOT\aimedical-sys-0.0.1-SNAPSHOT.pom
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT                                    [2/13]
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
[INFO] Running com.aimedical.common.base.BaseEntityAuditTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-26T20:45:13.593+08:00  INFO 25588 --- [           main] c.a.common.base.BaseEntityAuditTest      : Starting BaseEntityAuditTest using Java 21.0.11 with PID 25588 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\common)
2026-06-26T20:45:13.596+08:00  INFO 25588 --- [           main] c.a.common.base.BaseEntityAuditTest      : No active profile set, falling back to 1 default profile: "default"
2026-06-26T20:45:14.098+08:00  INFO 25588 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-26T20:45:14.134+08:00  INFO 25588 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 21 ms. Found 0 JPA repository interfaces.
2026-06-26T20:45:14.189+08:00  INFO 25588 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-06-26T20:45:14.426+08:00  INFO 25588 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:110b517c-75b9-4c74-b01b-7f69b082dd3e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-06-26T20:45:14.825+08:00  INFO 25588 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-26T20:45:14.923+08:00  INFO 25588 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-06-26T20:45:14.963+08:00  INFO 25588 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-26T20:45:15.278+08:00  INFO 25588 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-26T20:45:16.230+08:00  INFO 25588 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: drop table if exists test_audit_entity cascade 
Hibernate: create table test_audit_entity (deleted boolean not null, created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), primary key (id))
2026-06-26T20:45:16.275+08:00  INFO 25588 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-26T20:45:16.425+08:00  INFO 25588 --- [           main] c.a.common.base.BaseEntityAuditTest      : Started BaseEntityAuditTest in 3.27 seconds (process running for 5.196)
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Hibernate: insert into test_audit_entity (created_at,deleted,updated_at,id) values (?,?,?,default)
Hibernate: update test_audit_entity set created_at=?,deleted=?,updated_at=? where id=?
Hibernate: insert into test_audit_entity (created_at,deleted,updated_at,id) values (?,?,?,default)
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.566 s -- in com.aimedical.common.base.BaseEntityAuditTest
[INFO] Running com.aimedical.common.base.BaseEntityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.common.base.BaseEntityTest
[INFO] Running com.aimedical.common.base.BaseEnumTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.common.base.BaseEnumTest
[INFO] Running com.aimedical.common.CommonPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.common.CommonPlaceholderTest
[INFO] Running com.aimedical.common.config.GlobalExceptionHandlerTest
2026-06-26T20:45:17.287+08:00 ERROR 25588 --- [           main] c.a.c.config.GlobalExceptionHandler      : System exception

java.lang.RuntimeException: unexpected
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleGenericExceptionWith500(GlobalExceptionHandlerTest.java:112) ~[test-classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728) ~[junit-platform-commons-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:45:17.298+08:00  WARN 25588 --- [           main] c.a.c.config.GlobalExceptionHandler      : Request body malformed

org.springframework.http.converter.HttpMessageNotReadableException: Malformed JSON
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleMessageNotReadableWith400(GlobalExceptionHandlerTest.java:64) ~[test-classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728) ~[junit-platform-commons-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:45:17.303+08:00 ERROR 25588 --- [           main] c.a.c.config.GlobalExceptionHandler      : Response body serialization failed

org.springframework.http.converter.HttpMessageNotWritableException: Serialization failed
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleMessageNotWritableWith500(GlobalExceptionHandlerTest.java:88) ~[test-classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728) ~[junit-platform-commons-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.common.config.GlobalExceptionHandlerTest
[INFO] Running com.aimedical.common.config.JacksonConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.068 s -- in com.aimedical.common.config.JacksonConfigTest
[INFO] Running com.aimedical.common.config.JpaConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.common.config.JpaConfigTest
[INFO] Running com.aimedical.common.exception.BusinessExceptionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in com.aimedical.common.exception.BusinessExceptionTest
[INFO] Running com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Running com.aimedical.common.pom.AggregatorPomTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.112 s -- in com.aimedical.common.pom.AggregatorPomTest
[INFO] Running com.aimedical.common.pom.ApplicationPomTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.common.pom.ApplicationPomTest
[INFO] Running com.aimedical.common.pom.CommonPomTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.common.pom.CommonPomTest
[INFO] Running com.aimedical.common.pom.MovedModulePomTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.033 s -- in com.aimedical.common.pom.MovedModulePomTest
[INFO] Running com.aimedical.common.pom.ParentPomDependencyManagementCleanupTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.common.pom.ParentPomDependencyManagementCleanupTest
[INFO] Running com.aimedical.common.pom.ParentPomTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.common.pom.ParentPomTest
[INFO] Running com.aimedical.common.pom.ParentPomVersionTest
[WARNING] Tests run: 5, Failures: 0, Errors: 0, Skipped: 5, Time elapsed: 0 s -- in com.aimedical.common.pom.ParentPomVersionTest
[INFO] Running com.aimedical.common.result.PageQueryTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.085 s -- in com.aimedical.common.result.PageQueryTest
[INFO] Running com.aimedical.common.result.PageResponseTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.common.result.PageResponseTest
[INFO] Running com.aimedical.common.result.ResultTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.common.result.ResultTest
[INFO] 
[INFO] Results:
[INFO] 
[WARNING] Tests run: 121, Failures: 0, Errors: 0, Skipped: 5
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ common ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\target\common-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ common ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\common\0.0.1-SNAPSHOT\common-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\target\common-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\common\0.0.1-SNAPSHOT\common-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --------------------< com.aimedical:common-module >---------------------
[INFO] Building Common Module Aggregator 0.0.1-SNAPSHOT                  [3/13]
[INFO]   from modules\common-module\pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ common-module ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\common-module\0.0.1-SNAPSHOT\common-module-0.0.1-SNAPSHOT.pom
[INFO] 
[INFO] ------------------< com.aimedical:common-module-api >-------------------
[INFO] Building common-module-api 0.0.1-SNAPSHOT                         [4/13]
[INFO]   from modules\common-module\common-module-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-api ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 5 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\src\test\resources
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
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ common-module-api ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\target\common-module-api-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ common-module-api ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\common-module-api\0.0.1-SNAPSHOT\common-module-api-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\target\common-module-api-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\common-module-api\0.0.1-SNAPSHOT\common-module-api-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT                        [5/13]
[INFO]   from modules\common-module\common-module-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 48 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-impl ---
[INFO] Copying 0 resource from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 41 source files with javac [debug release 17] to target\test-classes
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\test\java\com\aimedical\modules\commonmodule\auth\login\LoginAttemptTrackerTest.javaʹ����δ�����򲻰�ȫ�Ĳ�����
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:unchecked ���±��롣
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.119 s -- in com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Running com.aimedical.modules.commonmodule.auth.config.AuthModuleConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.auth.config.AuthModuleConfigTest
[INFO] Running com.aimedical.modules.commonmodule.auth.converter.UserConverterTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in com.aimedical.modules.commonmodule.auth.converter.UserConverterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest
[INFO] Running com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProviderTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.549 s -- in com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProviderTest
[INFO] Running com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.647 s -- in com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.password.PasswordChangeServiceImplTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.469 s -- in com.aimedical.modules.commonmodule.auth.password.PasswordChangeServiceImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.password.PasswordPolicyImplTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.commonmodule.auth.password.PasswordPolicyImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.14 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.596 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.148 s -- in com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.188 s -- in com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest
20:45:41.210 [main] WARN com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter -- Account disabled, userId=1
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.406 s -- in com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest
20:45:41.249 [main] WARN com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter -- Password change required for userId=1, blocking request: GET /api/auth/me
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.074 s -- in com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.053 s -- in com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1CoexistenceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1CoexistenceTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
[INFO] Running com.aimedical.modules.commonmodule.auth.UserFacadeImplTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.053 s -- in com.aimedical.modules.commonmodule.auth.UserFacadeImplTest
[INFO] Running com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.125 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.191 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.041 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.061 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
20:45:41.875 [main] INFO org.hibernate.validator.internal.util.Version -- HV000001: Hibernate Validator 8.0.1.Final
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.392 s -- in com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.043 s -- in com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$ValidateTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$ValidateTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$GetterSetterTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$GetterSetterTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$DefaultValueTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$DefaultValueTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
20:45:42.341 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
20:45:42.346 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
20:45:42.349 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
20:45:42.358 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.075 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Running com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserRepositoryTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-26T20:45:43.117+08:00  INFO 8236 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Starting UserRepositoryTest using Java 21.0.11 with PID 8236 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl)
2026-06-26T20:45:43.120+08:00  INFO 8236 --- [           main] c.a.m.c.permission.UserRepositoryTest    : No active profile set, falling back to 1 default profile: "default"
2026-06-26T20:45:43.482+08:00  INFO 8236 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-26T20:45:43.554+08:00  INFO 8236 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 63 ms. Found 4 JPA repository interfaces.
2026-06-26T20:45:43.612+08:00  INFO 8236 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-06-26T20:45:43.800+08:00  INFO 8236 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:b2daf0e8-7e39-4887-9a68-8c47753ec36b;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-06-26T20:45:44.092+08:00  INFO 8236 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-26T20:45:44.155+08:00  INFO 8236 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-06-26T20:45:44.187+08:00  INFO 8236 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-26T20:45:44.293+08:00  INFO 8236 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-26T20:45:44.339+08:00  WARN 8236 --- [           main] org.hibernate.orm.deprecation            : HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-06-26T20:45:45.273+08:00  INFO 8236 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: 
    drop table if exists post_function cascade 
Hibernate: 
    drop table if exists sys_function cascade 
Hibernate: 
    drop table if exists sys_post cascade 
Hibernate: 
    drop table if exists sys_role cascade 
Hibernate: 
    drop table if exists sys_user cascade 
Hibernate: 
    drop table if exists user_post cascade 
Hibernate: 
    drop table if exists user_role cascade 
Hibernate: 
    create table post_function (
        function_id bigint not null,
        post_id bigint not null,
        primary key (function_id, post_id)
    )
Hibernate: 
    create table sys_function (
        deleted boolean not null,
        enabled boolean not null,
        sort_order integer not null,
        visible boolean not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        parent_id bigint,
        updated_at timestamp(6),
        type varchar(20),
        code varchar(255) not null unique,
        description varchar(255),
        icon varchar(255),
        name varchar(255) not null,
        path varchar(255),
        primary key (id)
    )
Hibernate: 
    create table sys_post (
        deleted boolean not null,
        enabled boolean not null,
        sort integer,
        created_at timestamp(6),
        id bigint generated by default as identity,
        role_id bigint,
        updated_at timestamp(6),
        code varchar(255) not null unique,
        description varchar(255),
        name varchar(255),
        primary key (id)
    )
Hibernate: 
    create table sys_role (
        deleted boolean not null,
        enabled boolean not null,
        sort integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        code varchar(255) not null unique,
        description varchar(255),
        name varchar(255),
        primary key (id)
    )
Hibernate: 
    create table sys_user (
        deleted boolean not null,
        enabled boolean not null,
        password_change_required boolean not null,
        token_version integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),
        email varchar(255),
        nickname varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        username varchar(255) not null unique,
        primary key (id)
    )
Hibernate: 
    create table user_post (
        post_id bigint not null,
        user_id bigint not null,
        primary key (post_id, user_id)
    )
Hibernate: 
    create table user_role (
        role_id bigint not null,
        user_id bigint not null,
        primary key (role_id, user_id)
    )
Hibernate: 
    alter table if exists post_function 
       add constraint FKh56snoidh814t7tmnsvgkyp6c 
       foreign key (function_id) 
       references sys_function
Hibernate: 
    alter table if exists post_function 
       add constraint FKbv50wilq40pjojsdm6sg6g2xg 
       foreign key (post_id) 
       references sys_post
Hibernate: 
    alter table if exists sys_function 
       add constraint FKmp2cmbi9l9c1c7618t2o0v2xb 
       foreign key (parent_id) 
       references sys_function
Hibernate: 
    alter table if exists sys_post 
       add constraint FKjfpb3no7elnlin0vwqbx940gu 
       foreign key (role_id) 
       references sys_role
Hibernate: 
    alter table if exists user_post 
       add constraint FK1qq5m5bsjagqw0s8m1cyb1rmj 
       foreign key (post_id) 
       references sys_post
Hibernate: 
    alter table if exists user_post 
       add constraint FKafwurpfqy3g4a4k0xnse3l8vy 
       foreign key (user_id) 
       references sys_user
Hibernate: 
    alter table if exists user_role 
       add constraint FKdec2ggmqwgdhhb59jw7o488wx 
       foreign key (role_id) 
       references sys_role
Hibernate: 
    alter table if exists user_role 
       add constraint FKsrs64lo4ci4xyu3da9clbiv8r 
       foreign key (user_id) 
       references sys_user
2026-06-26T20:45:45.375+08:00  INFO 8236 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-26T20:45:46.070+08:00  INFO 8236 --- [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-06-26T20:45:46.841+08:00  INFO 8236 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Started UserRepositoryTest in 4.196 seconds (process running for 21.992)
Hibernate: 
    select
        u1_0.id,
        u1_0.created_at,
        u1_0.deleted,
        u1_0.email,
        u1_0.enabled,
        u1_0.nickname,
        u1_0.password,
        u1_0.password_change_required,
        u1_0.phone,
        u1_0.token_version,
        u1_0.updated_at,
        u1_0.user_type,
        u1_0.username 
    from
        sys_user u1_0 
    where
        u1_0.username=?
Hibernate: 
    insert 
    into
        sys_user
        (created_at, deleted, email, enabled, nickname, password, password_change_required, phone, token_version, updated_at, user_type, username, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, default)
Hibernate: 
    insert 
    into
        sys_user
        (created_at, deleted, email, enabled, nickname, password, password_change_required, phone, token_version, updated_at, user_type, username, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, default)
2026-06-26T20:45:47.187+08:00  WARN 8236 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 23502, SQLState: 23502
2026-06-26T20:45:47.187+08:00 ERROR 8236 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : NULL not allowed for column "PASSWORD"; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [23502-224]
Hibernate: 
    insert 
    into
        sys_user
        (created_at, deleted, email, enabled, nickname, password, password_change_required, phone, token_version, updated_at, user_type, username, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, default)
Hibernate: 
    select
        u1_0.id,
        u1_0.created_at,
        u1_0.deleted,
        u1_0.email,
        u1_0.enabled,
        u1_0.nickname,
        u1_0.password,
        u1_0.password_change_required,
        u1_0.phone,
        u1_0.token_version,
        u1_0.updated_at,
        u1_0.user_type,
        u1_0.username 
    from
        sys_user u1_0 
    where
        u1_0.username=?
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.844 s -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest
2026-06-26T20:45:47.619+08:00  INFO 8236 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-26T20:45:47.621+08:00  INFO 8236 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û������޸ĳɹ���userId: 1
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.365 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.070 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.113 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 336, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ common-module-impl ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\target\common-module-impl-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ common-module-impl ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\common-module-impl\0.0.1-SNAPSHOT\common-module-impl-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\target\common-module-impl-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\common-module-impl\0.0.1-SNAPSHOT\common-module-impl-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --------------------------< com.aimedical:ai >--------------------------
[INFO] Building AI Module Aggregator 0.0.1-SNAPSHOT                      [6/13]
[INFO]   from modules\ai\pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ ai ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\ai\0.0.1-SNAPSHOT\ai-0.0.1-SNAPSHOT.pom
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT                                    [7/13]
[INFO]   from modules\ai\ai-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 31 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\test\resources
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
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.070 s -- in com.aimedical.modules.ai.api.AiResultTest
[INFO] Running com.aimedical.modules.ai.api.AiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s -- in com.aimedical.modules.ai.api.AiServiceTest
[INFO] Running com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ ai-api ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\target\ai-api-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ ai-api ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\ai-api\0.0.1-SNAPSHOT\ai-api-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\target\ai-api-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\ai-api\0.0.1-SNAPSHOT\ai-api-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] -----------------------< com.aimedical:ai-impl >------------------------
[INFO] Building ai-impl 0.0.1-SNAPSHOT                                   [8/13]
[INFO]   from modules\ai\ai-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 4 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.066 s -- in com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
20:45:53.207 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
20:45:53.210 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
20:45:53.258 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
20:45:53.261 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
20:45:53.261 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
20:45:53.261 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.423 s -- in com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.041 s -- in com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.pom.AiImplPomCleanDependencyTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.075 s -- in com.aimedical.modules.ai.impl.pom.AiImplPomCleanDependencyTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ ai-impl ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\target\ai-impl-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ ai-impl ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\ai-impl\0.0.1-SNAPSHOT\ai-impl-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\target\ai-impl-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\ai-impl\0.0.1-SNAPSHOT\ai-impl-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] -----------------------< com.aimedical:patient >------------------------
[INFO] Building patient 0.0.1-SNAPSHOT                                   [9/13]
[INFO]   from modules\patient\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ patient ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 17 source files with javac [debug release 17] to target\classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/entity/SurgeryHistory.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/entity/ChronicDisease.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/entity/AllergyHistory.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/entity/FamilyHistory.java:[11,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/entity/MedicationHistory.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/entity/HealthProfile.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ patient ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 4 source files with javac [debug release 17] to target\test-classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ patient ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.065 s -- in com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Running com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Running com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Running com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ patient ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\target\patient-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ patient ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\patient\0.0.1-SNAPSHOT\patient-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\patient\target\patient-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\patient\0.0.1-SNAPSHOT\patient-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] ------------------------< com.aimedical:doctor >------------------------
[INFO] Building doctor 0.0.1-SNAPSHOT                                   [10/13]
[INFO]   from modules\doctor\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ doctor ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 7 source files with javac [debug release 17] to target\classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ doctor ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 4 source files with javac [debug release 17] to target\test-classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ doctor ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Running com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Running com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Running com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ doctor ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\target\doctor-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ doctor ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\doctor\0.0.1-SNAPSHOT\doctor-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\doctor\target\doctor-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\doctor\0.0.1-SNAPSHOT\doctor-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] ------------------------< com.aimedical:admin >-------------------------
[INFO] Building admin 0.0.1-SNAPSHOT                                    [11/13]
[INFO]   from modules\admin\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ admin ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 13 source files with javac [debug release 17] to target\classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/admin/src/main/java/com/aimedical/modules/admin/entity/dict/DictType.java:[17,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/admin/src/main/java/com/aimedical/modules/admin/entity/TokenStore.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[WARNING] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/admin/src/main/java/com/aimedical/modules/admin/entity/dict/DictData.java:[13,1] Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '@EqualsAndHashCode(callSuper=false)' to your type.
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ admin ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 4 source files with javac [debug release 17] to target\test-classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ admin ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.058 s -- in com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Running com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Running com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Running com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ admin ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\target\admin-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ admin ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\admin\0.0.1-SNAPSHOT\admin-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\admin\target\admin-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\admin\0.0.1-SNAPSHOT\admin-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] ---------------------< com.aimedical:application >----------------------
[INFO] Building application 0.0.1-SNAPSHOT                              [12/13]
[INFO]   from application\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ application ---
[INFO] Copying 3 resources from src\main\resources to target\classes
[INFO] Copying 3 resources from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ application ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ application ---
[INFO] Copying 1 resource from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ application ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 2 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ application ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.ApplicationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.055 s -- in com.aimedical.ApplicationPlaceholderTest
[INFO] Running com.aimedical.HealthControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.HealthControllerTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ application ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\target\application-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- spring-boot:3.2.5:repackage (repackage) @ application ---
[INFO] Attaching repackaged archive C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\target\application-0.0.1-SNAPSHOT-exec.jar with classifier exec
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ application ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\application\0.0.1-SNAPSHOT\application-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\target\application-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\application\0.0.1-SNAPSHOT\application-0.0.1-SNAPSHOT.jar
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\target\application-0.0.1-SNAPSHOT-exec.jar to C:\Users\laoE\.m2\repository\com\aimedical\application\0.0.1-SNAPSHOT\application-0.0.1-SNAPSHOT-exec.jar
[INFO] 
[INFO] ---------------------< com.aimedical:integration >----------------------
[INFO] Building integration 0.0.1-SNAPSHOT                              [13/13]
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
[INFO] Copying 2 resources from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ integration ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\test-classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
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
20:46:13.683 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.aimedical.integration.ApplicationContextIT]: ApplicationContextIT does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
20:46:13.818 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.aimedical.Application for test class com.aimedical.integration.ApplicationContextIT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-26T20:46:14.421+08:00  INFO 25068 --- [           main] c.a.integration.ApplicationContextIT     : Starting ApplicationContextIT using Java 21.0.11 with PID 25068 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration)
2026-06-26T20:46:14.423+08:00 DEBUG 25068 --- [           main] c.a.integration.ApplicationContextIT     : Running with Spring Boot v3.2.5, Spring v6.1.6
2026-06-26T20:46:14.424+08:00  INFO 25068 --- [           main] c.a.integration.ApplicationContextIT     : The following 2 profiles are active: "test", "phase1"
2026-06-26T20:46:17.970+08:00  WARN 25068 --- [           main] org.hibernate.orm.deprecation            : HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-06-26T20:46:20.560+08:00  WARN 25068 --- [           main] o.s.w.c.s.GenericWebApplicationContext   : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
2026-06-26T20:46:20.632+08:00 ERROR 25068 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: java.lang.IllegalArgumentException: Illegal base64 character 2d
	at java.base/java.util.Base64$Decoder.decode0(Base64.java:852) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:570) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:593) ~[na:na]
	at com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider.init(JwtTokenProvider.java:34) ~[common-module-impl-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 96 common frames omitted




============================
CONDITIONS EVALUATION REPORT
============================


Positive matches:
-----------------

    None


Negative matches:
-----------------

    None


Exclusions:
-----------

    None


Unconditional classes:
----------------------

    None



2026-06-26T20:46:20.651+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.ApplicationContextIT@34fde3a3]

java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@4a5dc126 testClass = com.aimedical.integration.ApplicationContextIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	... 73 common frames omitted
Caused by: java.lang.IllegalArgumentException: Illegal base64 character 2d
	at java.base/java.util.Base64$Decoder.decode0(Base64.java:852) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:570) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:593) ~[na:na]
	at com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider.init(JwtTokenProvider.java:34) ~[common-module-impl-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 96 common frames omitted

[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 7.213 s <<< FAILURE! -- in com.aimedical.integration.ApplicationContextIT
[ERROR] com.aimedical.integration.ApplicationContextIT.contextLoads -- Time elapsed: 0.033 s <<< ERROR!
java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@4a5dc126 testClass = com.aimedical.integration.ApplicationContextIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624)
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754)
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334)
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46)
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454)
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152)
	... 17 more
Caused by: java.lang.IllegalArgumentException: Illegal base64 character 2d
	at java.base/java.util.Base64$Decoder.decode0(Base64.java:852)
	at java.base/java.util.Base64$Decoder.decode(Base64.java:570)
	at java.base/java.util.Base64$Decoder.decode(Base64.java:593)
	at com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider.init(JwtTokenProvider.java:34)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219)
	... 40 more

[INFO] Running com.aimedical.integration.EntityMappingIT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-26T20:46:20.788+08:00  INFO 25068 --- [           main] c.aimedical.integration.EntityMappingIT  : Starting EntityMappingIT using Java 21.0.11 with PID 25068 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration)
2026-06-26T20:46:20.788+08:00 DEBUG 25068 --- [           main] c.aimedical.integration.EntityMappingIT  : Running with Spring Boot v3.2.5, Spring v6.1.6
2026-06-26T20:46:20.788+08:00  INFO 25068 --- [           main] c.aimedical.integration.EntityMappingIT  : The following 2 profiles are active: "test", "phase1"
2026-06-26T20:46:21.767+08:00  WARN 25068 --- [           main] org.hibernate.orm.deprecation            : HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-06-26T20:46:22.246+08:00  WARN 25068 --- [           main] o.s.w.c.s.GenericWebApplicationContext   : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
2026-06-26T20:46:22.265+08:00 ERROR 25068 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: java.lang.IllegalArgumentException: Illegal base64 character 2d
	at java.base/java.util.Base64$Decoder.decode0(Base64.java:852) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:570) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:593) ~[na:na]
	at com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider.init(JwtTokenProvider.java:34) ~[common-module-impl-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 96 common frames omitted




============================
CONDITIONS EVALUATION REPORT
============================


Positive matches:
-----------------

    None


Negative matches:
-----------------

    None


Exclusions:
-----------

    None


Unconditional classes:
----------------------

    None



2026-06-26T20:46:22.270+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@5127de08]

java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	... 73 common frames omitted
Caused by: java.lang.IllegalArgumentException: Illegal base64 character 2d
	at java.base/java.util.Base64$Decoder.decode0(Base64.java:852) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:570) ~[na:na]
	at java.base/java.util.Base64$Decoder.decode(Base64.java:593) ~[na:na]
	at com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider.init(JwtTokenProvider.java:34) ~[common-module-impl-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 96 common frames omitted

2026-06-26T20:46:22.277+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@5f571d96]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.280+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@1643e817]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.286+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@291fbdc9]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.290+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@4bfaa7b]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.294+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@31b846f]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.299+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@585d570e]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.303+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@247246ea]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.306+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@71639428]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.313+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@10ba757f]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.317+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@1cb3de26]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.321+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@30d5668a]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.324+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@25297d52]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.327+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@72b8b04f]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.330+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@75be3cdf]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.333+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@38dde7ec]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.336+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@3f19f197]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.339+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@6757ed9e]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.344+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@7b0eb7e]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.349+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@7be29ac4]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.363+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@25c887ca]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.365+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@647dcc5e]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.368+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@3b6f7ab3]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

2026-06-26T20:46:22.372+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.web.ServletTestExecutionListener] to prepare test instance [com.aimedical.integration.EntityMappingIT@87f8146]

java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]

[ERROR] Tests run: 24, Failures: 0, Errors: 24, Skipped: 0, Time elapsed: 1.659 s <<< FAILURE! -- in com.aimedical.integration.EntityMappingIT
[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldMapUsernameField -- Time elapsed: 0.002 s <<< ERROR!
java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenProvider': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624)
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754)
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334)
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46)
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454)
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152)
	... 17 more
Caused by: java.lang.IllegalArgumentException: Illegal base64 character 2d
	at java.base/java.util.Base64$Decoder.decode0(Base64.java:852)
	at java.base/java.util.Base64$Decoder.decode(Base64.java:570)
	at java.base/java.util.Base64$Decoder.decode(Base64.java:593)
	at com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider.init(JwtTokenProvider.java:34)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219)
	... 40 more

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldMapUserTypeEnumAsString -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.role_shouldMapCodeField -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.role_shouldEnforceCodeUniqueConstraint -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldPersistWithPassword -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.doctorEntity_shouldMapConsultationFeePrecision -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.tokenStore_shouldMapUniqueToken -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.patientEntity_shouldMapAvatarUrl -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.patientWithHealthProfileAndAllergy_shouldWorkTogether -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.allergyHistory_shouldMapOccurredAtColumn -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldMapManyToManyWithPosts -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldMapManyToManyWithRoles -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.role_shouldMapOneToManyPosts -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.post_shouldMapManyToOneRole -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldMapPasswordChangeRequired -- Time elapsed: 0.002 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.dictType_shouldHaveOneToManyDictDataList -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.dictData_shouldHaveManyToOneRelationWithDictType -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.post_shouldMapManyToManyFunctions -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldMapTokenVersion -- Time elapsed: 0.002 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldEnforceUserTypeNotNull -- Time elapsed: 0.011 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.function_shouldPersistDirectoryType -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.healthProfile_shouldMapDecimalPrecision -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.user_shouldRejectNullPassword -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.integration.EntityMappingIT.function_shouldMapMenuTypeEnum -- Time elapsed: 0 s <<< ERROR!
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:145)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:191)
	at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:130)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] Running com.aimedical.integration.HealthCheckIT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-26T20:46:22.439+08:00  INFO 25068 --- [           main] c.aimedical.integration.HealthCheckIT    : Starting HealthCheckIT using Java 21.0.11 with PID 25068 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration)
2026-06-26T20:46:22.439+08:00 DEBUG 25068 --- [           main] c.aimedical.integration.HealthCheckIT    : Running with Spring Boot v3.2.5, Spring v6.1.6
2026-06-26T20:46:22.439+08:00  INFO 25068 --- [           main] c.aimedical.integration.HealthCheckIT    : The following 2 profiles are active: "test", "phase1"
2026-06-26T20:46:23.879+08:00 ERROR 25068 --- [           main] o.s.b.web.embedded.tomcat.TomcatStarter  : Error starting Tomcat context. Exception: org.springframework.beans.factory.UnsatisfiedDependencyException. Message: Error creating bean with name 'jwtAuthenticationFilter' defined in class path resource [com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.class]: Unsatisfied dependency expressed through method 'jwtAuthenticationFilter' parameter 0: Error creating bean with name 'jwtUtil': Invocation of init method failed
2026-06-26T20:46:23.936+08:00  WARN 25068 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [sliding-window-cleanup] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base/jdk.internal.misc.Unsafe.park(Native Method)
 java.base/java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:269)
 java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:1797)
 java.base/java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1182)
 java.base/java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:899)
 java.base/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1070)
 java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1130)
 java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
 java.base/java.lang.Thread.run(Thread.java:1583)
2026-06-26T20:46:23.942+08:00  WARN 25068 --- [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.context.ApplicationContextException: Unable to start web server
2026-06-26T20:46:23.948+08:00 ERROR 25068 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.context.ApplicationContextException: Unable to start web server
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:165) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:618) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.injectDependencies(DependencyInjectionTestExecutionListener.java:142) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.prepareTestInstance(DependencyInjectionTestExecutionListener.java:98) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:145) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:105) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:499) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:218) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:188) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:162) ~[spring-boot-3.2.5.jar:3.2.5]
	... 87 common frames omitted
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'jwtAuthenticationFilter' defined in class path resource [com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.class]: Unsatisfied dependency expressed through method 'jwtAuthenticationFilter' parameter 0: Error creating bean with name 'jwtUtil': Invocation of init method failed
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:795) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:542) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1335) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1165) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:205) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.getOrderedBeansOfType(ServletContextInitializerBeans.java:210) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:173) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:168) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans(ServletContextInitializerBeans.java:153) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.<init>(ServletContextInitializerBeans.java:86) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.getServletContextInitializerBeans(ServletWebServerApplicationContext.java:266) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.selfInitialize(ServletWebServerApplicationContext.java:240) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatStarter.onStartup(TomcatStarter.java:52) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4880) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:845) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:240) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardService.startInternal(StandardService.java:433) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:921) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.startup.Tomcat.start(Tomcat.java:437) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:126) ~[spring-boot-3.2.5.jar:3.2.5]
	... 92 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtUtil': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:904) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:782) ~[spring-beans-6.1.6.jar:6.1.6]
	... 133 common frames omitted
Caused by: java.lang.IllegalStateException: JWT_SECRET contains invalid characters
	at com.aimedical.modules.commonmodule.jwt.JwtUtil.init(JwtUtil.java:53) ~[common-module-impl-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 146 common frames omitted




============================
CONDITIONS EVALUATION REPORT
============================


Positive matches:
-----------------

    None


Negative matches:
-----------------

    None


Exclusions:
-----------

    None


Unconditional classes:
----------------------

    None



2026-06-26T20:46:23.951+08:00  WARN 25068 --- [           main] o.s.test.context.TestContextManager      : Caught exception while allowing TestExecutionListener [org.springframework.test.context.support.DependencyInjectionTestExecutionListener] to prepare test instance [com.aimedical.integration.HealthCheckIT@262d72a]

java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@61e4cf9b testClass = com.aimedical.integration.HealthCheckIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true", "server.port=0"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@5593aedd], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.injectDependencies(DependencyInjectionTestExecutionListener.java:142) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.prepareTestInstance(DependencyInjectionTestExecutionListener.java:98) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.springframework.context.ApplicationContextException: Unable to start web server
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:165) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:618) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	... 73 common frames omitted
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:145) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:105) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:499) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:218) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:188) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:162) ~[spring-boot-3.2.5.jar:3.2.5]
	... 87 common frames omitted
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'jwtAuthenticationFilter' defined in class path resource [com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.class]: Unsatisfied dependency expressed through method 'jwtAuthenticationFilter' parameter 0: Error creating bean with name 'jwtUtil': Invocation of init method failed
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:795) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:542) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1335) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1165) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:205) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.getOrderedBeansOfType(ServletContextInitializerBeans.java:210) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:173) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:168) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans(ServletContextInitializerBeans.java:153) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.<init>(ServletContextInitializerBeans.java:86) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.getServletContextInitializerBeans(ServletWebServerApplicationContext.java:266) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.selfInitialize(ServletWebServerApplicationContext.java:240) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatStarter.onStartup(TomcatStarter.java:52) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4880) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:845) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:240) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardService.startInternal(StandardService.java:433) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:921) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.startup.Tomcat.start(Tomcat.java:437) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:126) ~[spring-boot-3.2.5.jar:3.2.5]
	... 92 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtUtil': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:904) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:782) ~[spring-beans-6.1.6.jar:6.1.6]
	... 133 common frames omitted
Caused by: java.lang.IllegalStateException: JWT_SECRET contains invalid characters
	at com.aimedical.modules.commonmodule.jwt.JwtUtil.init(JwtUtil.java:53) ~[common-module-impl-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 146 common frames omitted

[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1.578 s <<< FAILURE! -- in com.aimedical.integration.HealthCheckIT
[ERROR] com.aimedical.integration.HealthCheckIT.shouldPingSuccess -- Time elapsed: 0.001 s <<< ERROR!
java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@61e4cf9b testClass = com.aimedical.integration.HealthCheckIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true", "server.port=0"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@5593aedd], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180)
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.injectDependencies(DependencyInjectionTestExecutionListener.java:142)
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.prepareTestInstance(DependencyInjectionTestExecutionListener.java:98)
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762)
	at java.base/java.util.Optional.orElseGet(Optional.java:364)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.springframework.context.ApplicationContextException: Unable to start web server
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:165)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:618)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146)
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754)
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334)
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58)
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46)
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454)
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225)
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152)
	... 17 more
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:145)
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:105)
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:499)
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:218)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:188)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:162)
	... 31 more
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'jwtAuthenticationFilter' defined in class path resource [com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.class]: Unsatisfied dependency expressed through method 'jwtAuthenticationFilter' parameter 0: Error creating bean with name 'jwtUtil': Invocation of init method failed
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:795)
	at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:542)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1335)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1165)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:205)
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.getOrderedBeansOfType(ServletContextInitializerBeans.java:210)
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:173)
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:168)
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans(ServletContextInitializerBeans.java:153)
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.<init>(ServletContextInitializerBeans.java:86)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.getServletContextInitializerBeans(ServletWebServerApplicationContext.java:266)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.selfInitialize(ServletWebServerApplicationContext.java:240)
	at org.springframework.boot.web.embedded.tomcat.TomcatStarter.onStartup(TomcatStarter.java:52)
	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4880)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171)
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332)
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75)
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145)
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866)
	at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:845)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171)
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332)
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75)
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145)
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866)
	at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:240)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171)
	at org.apache.catalina.core.StandardService.startInternal(StandardService.java:433)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171)
	at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:921)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171)
	at org.apache.catalina.startup.Tomcat.start(Tomcat.java:437)
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:126)
	... 36 more
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtUtil': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
	at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353)
	at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:904)
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:782)
	... 77 more
Caused by: java.lang.IllegalStateException: JWT_SECRET contains invalid characters
	at com.aimedical.modules.commonmodule.jwt.JwtUtil.init(JwtUtil.java:53)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219)
	... 90 more

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   ApplicationContextIT.contextLoads ? IllegalState Failed to load ApplicationContext for [WebMergedContextConfiguration@4a5dc126 testClass = com.aimedical.integration.ApplicationContextIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.allergyHistory_shouldMapOccurredAtColumn ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.dictData_shouldHaveManyToOneRelationWithDictType ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.dictType_shouldHaveOneToManyDictDataList ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.doctorEntity_shouldMapConsultationFeePrecision ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.function_shouldMapMenuTypeEnum ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.function_shouldPersistDirectoryType ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.healthProfile_shouldMapDecimalPrecision ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.patientEntity_shouldMapAvatarUrl ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.patientWithHealthProfileAndAllergy_shouldWorkTogether ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.post_shouldMapManyToManyFunctions ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.post_shouldMapManyToOneRole ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.role_shouldEnforceCodeUniqueConstraint ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.role_shouldMapCodeField ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.role_shouldMapOneToManyPosts ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.tokenStore_shouldMapUniqueToken ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldEnforceUserTypeNotNull ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldMapManyToManyWithPosts ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldMapManyToManyWithRoles ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldMapPasswordChangeRequired ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldMapTokenVersion ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldMapUserTypeEnumAsString ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldMapUsernameField ? IllegalState Failed to load ApplicationContext for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldPersistWithPassword ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   EntityMappingIT.user_shouldRejectNullPassword ? IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [WebMergedContextConfiguration@3ae81b53 testClass = com.aimedical.integration.EntityMappingIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [[ImportsContextCustomizer@4b9da2f0 key = [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration]], org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@f9b186c2, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@53c53b03], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[ERROR]   HealthCheckIT.shouldPingSuccess ? IllegalState Failed to load ApplicationContext for [WebMergedContextConfiguration@61e4cf9b testClass = com.aimedical.integration.HealthCheckIT, locations = [], classes = [com.aimedical.Application], contextInitializerClasses = [], activeProfiles = ["test", "phase1"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true", "server.port=0"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@19976a65, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@4bc222e, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@3336e6b6, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@352ff4da, org.springframework.boot.test.context.SpringBootTestAnnotation@5593aedd], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[INFO] 
[ERROR] Tests run: 26, Failures: 0, Errors: 26, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- failsafe:3.1.2:verify (default) @ integration ---
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.520 s]
[INFO] common ............................................. SUCCESS [ 11.638 s]
[INFO] Common Module Aggregator ........................... SUCCESS [  0.057 s]
[INFO] common-module-api .................................. SUCCESS [  1.966 s]
[INFO] common-module-impl ................................. SUCCESS [ 26.734 s]
[INFO] AI Module Aggregator ............................... SUCCESS [  0.041 s]
[INFO] ai-api ............................................. SUCCESS [  2.372 s]
[INFO] ai-impl ............................................ SUCCESS [  3.279 s]
[INFO] patient ............................................ SUCCESS [  3.372 s]
[INFO] doctor ............................................. SUCCESS [  2.552 s]
[INFO] admin .............................................. SUCCESS [  3.335 s]
[INFO] application ........................................ SUCCESS [  8.144 s]
[INFO] integration ........................................ FAILURE [ 13.058 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:17 min
[INFO] Finished at: 2026-06-26T20:46:24+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-failsafe-plugin:3.1.2:verify (default) on project integration: 
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\target\failsafe-reports for the individual test results.
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
[ERROR]   mvn <args> -rf :integration

> build:all
> npm run build --workspaces --if-present


> build
> vue-tsc --noEmit && vite build

src/main.ts(2,29): error TS2307: Cannot find module 'pinia' or its corresponding type declarations.
src/main.ts(3,25): error TS2307: Cannot find module 'element-plus' or its corresponding type declarations.
../../packages/shared/src/stores/auth.ts(1,29): error TS2307: Cannot find module 'pinia' or its corresponding type declarations.
../../packages/shared/src/stores/menu.ts(1,29): error TS2307: Cannot find module 'pinia' or its corresponding type declarations.
npm error Lifecycle script `build` failed with error:
npm error code 2
npm error path C:\Develop\Software\AIMedicalSys\AIMedical\frontend\apps\admin
npm error workspace @aimedical/app-admin
npm error location C:\Develop\Software\AIMedicalSys\AIMedical\frontend\apps\admin
npm error command failed
npm error command C:\WINDOWS\system32\cmd.exe /d /s /c vue-tsc --noEmit && vite build


> build
> vue-tsc --noEmit && vite build

src/main.ts(2,29): error TS2307: Cannot find module 'pinia' or its corresponding type declarations.
src/main.ts(3,25): error TS2307: Cannot find module 'element-plus' or its corresponding type declarations.
../../packages/shared/src/stores/auth.ts(1,29): error TS2307: Cannot find module 'pinia' or its corresponding type declarations.
../../packages/shared/src/stores/menu.ts(1,29): error TS2307: Cannot find module 'pinia' or its corresponding type declarations.
npm error Lifecycle script `build` failed with error:
npm error code 2
npm error path C:\Develop\Software\AIMedicalSys\AIMedical\frontend\apps\doctor
npm error workspace @aimedical/app-doctor
npm error location C:\Develop\Software\AIMedicalSys\AIMedical\frontend\apps\doctor
npm error command failed
npm error command C:\WINDOWS\system32\cmd.exe /d /s /c vue-tsc --noEmit && vite build


> build
> vue-tsc --noEmit && vite build

[33mThe CJS build of Vite's Node API is deprecated. See https://vite.dev/guide/troubleshooting.html#vite-cjs-node-api-deprecated for more details.[39m
[36mvite v5.4.21 [32mbuilding for production...[36m[39m
transforming...
[32m✓[39m 11 modules transformed.
rendering chunks...
computing gzip size...
[2mdist/[22m[32mindex.html                 [39m[1m[2m 0.39 kB[22m[1m[22m[2m │ gzip:  0.33 kB[22m
[2mdist/[22m[35massets/index-DwsreQJT.css  [39m[1m[2m 0.21 kB[22m[1m[22m[2m │ gzip:  0.17 kB[22m
[2mdist/[22m[36massets/index-D82AQSAA.js   [39m[1m[2m60.97 kB[22m[1m[22m[2m │ gzip: 24.46 kB[22m
[32m✓ built in 877ms[39m
