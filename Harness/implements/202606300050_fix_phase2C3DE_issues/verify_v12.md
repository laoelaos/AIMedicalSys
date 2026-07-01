# 验证报告（v12）

## 结果
FAILED

## 统计
- common: 201 测试通过，0 失败，0 错误（5 跳过）
- common-module-api: 86 测试通过，0 失败，0 错误
- common-module-impl: 400 测试通过，0 失败，0 错误（1 跳过）
- ai-api: 132 测试通过，0 失败，0 错误
- ai-impl: 53 测试通过，0 失败，0 错误
- patient: 成功
- doctor: 成功
- admin: 成功
- consultation: 成功
- prescription: 153 测试，3 失败，2 错误（**阻断**）
- medical-record: **SKIPPED**（被 prescription 阻断）

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
[INFO] consultation                                                       [jar]
[INFO] prescription                                                       [jar]
[INFO] medical-record                                                     [jar]
[INFO] application                                                        [jar]
[INFO] integration                                                        [jar]
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                             [1/16]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ aimedical-sys ---
[INFO] Skipping JaCoCo execution because property jacoco.skip is set.
[INFO] argLine set to empty
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT                                    [2/16]
[INFO]   from common\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ common ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\common\\target\\jacoco.exec
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

2026-06-30T10:33:42.971+08:00  INFO 11428 --- [           main] c.a.common.base.BaseEntityAuditTest      : Starting BaseEntityAuditTest using Java 21.0.11 with PID 11428 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\common)
2026-06-30T10:33:42.974+08:00  INFO 11428 --- [           main] c.a.common.base.BaseEntityAuditTest      : No active profile set, falling back to 1 default profile: "default"
2026-06-30T10:33:43.529+08:00  INFO 11428 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-30T10:33:43.561+08:00  INFO 11428 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 19 ms. Found 0 JPA repository interfaces.
2026-06-30T10:33:43.620+08:00  INFO 11428 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-06-30T10:33:43.845+08:00  INFO 11428 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:e0599176-6467-455c-ace8-16b22469d0c5;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-06-30T10:33:44.399+08:00  INFO 11428 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-30T10:33:44.533+08:00  INFO 11428 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-06-30T10:33:44.595+08:00  INFO 11428 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-30T10:33:45.067+08:00  INFO 11428 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-30T10:33:46.921+08:00  INFO 11428 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: drop table if exists test_audit_entity cascade 
Hibernate: create table test_audit_entity (deleted boolean not null, created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), primary key (id))
2026-06-30T10:33:46.989+08:00  INFO 11428 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-30T10:33:47.210+08:00  INFO 11428 --- [           main] c.a.common.base.BaseEntityAuditTest      : Started BaseEntityAuditTest in 4.722 seconds (process running for 6.067)
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
Hibernate: insert into test_audit_entity (created_at,deleted,updated_at,id) values (?,?,?,default)
Hibernate: update test_audit_entity set created_at=?,deleted=?,updated_at=? where id=?
Hibernate: insert into test_audit_entity (created_at,deleted,updated_at,id) values (?,?,?,default)
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.185 s -- in com.aimedical.common.base.BaseEntityAuditTest
[INFO] Running com.aimedical.common.base.BaseEntityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.common.base.BaseEntityTest
[INFO] Running com.aimedical.common.base.BaseEnumTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.base.BaseEnumTest
[INFO] Running com.aimedical.common.CommonPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.common.CommonPlaceholderTest
[INFO] Running com.aimedical.common.config.GlobalExceptionHandlerTest
2026-06-30T10:33:48.224+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=BIZ_ERR, message=ҵ���쳣
2026-06-30T10:33:48.238+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=RATE_LIMITED, message=��¼���Թ���Ƶ�������Ժ�����
2026-06-30T10:33:48.241+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=RATE_LIMITED, message=��¼���Թ���Ƶ�������Ժ�����
2026-06-30T10:33:48.243+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=ACCOUNT_LOCKED, message=�˻�����������{����ʱ��}������
2026-06-30T10:33:48.246+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=BIZ_ERR, message=ҵ���쳣
2026-06-30T10:33:48.262+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=NUM_ERR, message=����{0}�ѹ��ڣ�ʣ��{1}��
2026-06-30T10:33:48.264+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=BIZ_ERR, message=ҵ���쳣
2026-06-30T10:33:48.266+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=NUM_ERR, message=����{0}�ѹ��ڣ�ʣ��{1}��
2026-06-30T10:33:48.267+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=ACCOUNT_LOCKED, message=�˻�����������{����ʱ��}������
2026-06-30T10:33:48.269+08:00 ERROR 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : System exception

java.lang.RuntimeException: unexpected
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleGenericExceptionWith500(GlobalExceptionHandlerTest.java:263) ~[test-classes/:na]
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

2026-06-30T10:33:48.280+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Request body malformed

org.springframework.http.converter.HttpMessageNotReadableException: Malformed JSON
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleMessageNotReadableWith400(GlobalExceptionHandlerTest.java:215) ~[test-classes/:na]
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

2026-06-30T10:33:48.282+08:00  WARN 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=TOKEN_REFRESH_FAILED, message=����ˢ��ʧ�ܣ������µ�¼
2026-06-30T10:33:48.284+08:00 ERROR 11428 --- [           main] c.a.c.config.GlobalExceptionHandler      : Response body serialization failed

org.springframework.http.converter.HttpMessageNotWritableException: Serialization failed
	at com.aimedical.common.config.GlobalExceptionHandlerTest.shouldHandleMessageNotWritableWith500(GlobalExceptionHandlerTest.java:239) ~[test-classes/:na]
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

[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.073 s -- in com.aimedical.common.config.GlobalExceptionHandlerTest
[INFO] Running com.aimedical.common.config.JacksonConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.090 s -- in com.aimedical.common.config.JacksonConfigTest
[INFO] Running com.aimedical.common.config.JpaConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.config.JpaConfigTest
[INFO] Running com.aimedical.common.entity.DosageStandardAuditTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-30T10:33:48.433+08:00  INFO 11428 --- [           main] c.a.c.entity.DosageStandardAuditTest     : Starting DosageStandardAuditTest using Java 21.0.11 with PID 11428 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\common)
2026-06-30T10:33:48.433+08:00  INFO 11428 --- [           main] c.a.c.entity.DosageStandardAuditTest     : No active profile set, falling back to 1 default profile: "default"
2026-06-30T10:33:48.528+08:00  INFO 11428 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-30T10:33:48.532+08:00  INFO 11428 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 3 ms. Found 0 JPA repository interfaces.
2026-06-30T10:33:48.547+08:00  INFO 11428 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-06-30T10:33:48.568+08:00  INFO 11428 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:f5a1c275-bc66-45c2-bf4f-f9f694ad74de;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-06-30T10:33:48.594+08:00  INFO 11428 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-30T10:33:48.598+08:00  INFO 11428 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-30T10:33:48.606+08:00  INFO 11428 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-30T10:33:48.760+08:00  INFO 11428 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: drop table if exists dosage_standard cascade 
Hibernate: create table dosage_standard (age_range_end integer, age_range_start integer, daily_max numeric(12,3), deleted boolean not null, single_max numeric(12,3) not null, weight_range_end numeric(10,2), weight_range_start numeric(10,2), created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), route_of_administration varchar(20) not null, unit varchar(20) not null, drug_code varchar(50) not null, primary key (id))
Hibernate: create index idx_dosage_drug_route on dosage_standard (drug_code, route_of_administration)
Hibernate: create index idx_dosage_drug_route_age_weight on dosage_standard (drug_code, route_of_administration, age_range_start, age_range_end, weight_range_start, weight_range_end)
2026-06-30T10:33:48.770+08:00  INFO 11428 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-30T10:33:48.810+08:00  INFO 11428 --- [           main] c.a.c.entity.DosageStandardAuditTest     : Started DosageStandardAuditTest in 0.404 seconds (process running for 7.666)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: update dosage_standard set age_range_end=?,age_range_start=?,created_at=?,daily_max=?,deleted=?,drug_code=?,route_of_administration=?,single_max=?,unit=?,updated_at=?,weight_range_end=?,weight_range_start=? where id=?
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.598 s -- in com.aimedical.common.entity.DosageStandardAuditTest
[INFO] Running com.aimedical.common.entity.DosageStandardTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.059 s -- in com.aimedical.common.entity.DosageStandardTest
[INFO] Running com.aimedical.common.exception.BusinessExceptionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.common.exception.BusinessExceptionTest
[INFO] Running com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Running com.aimedical.common.pom.AggregatorPomTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.070 s -- in com.aimedical.common.pom.AggregatorPomTest
[INFO] Running com.aimedical.common.pom.ApplicationPomTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.common.pom.ApplicationPomTest
[INFO] Running com.aimedical.common.pom.CommonPomTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.common.pom.CommonPomTest
[INFO] Running com.aimedical.common.pom.MovedModulePomTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in com.aimedical.common.pom.MovedModulePomTest
[INFO] Running com.aimedical.common.pom.MovedModulePomVerificationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.common.pom.MovedModulePomVerificationTest
[INFO] Running com.aimedical.common.pom.NewModulePomTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.042 s -- in com.aimedical.common.pom.NewModulePomTest
[INFO] Running com.aimedical.common.pom.ParentPomDependencyManagementCleanupTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.common.pom.ParentPomDependencyManagementCleanupTest
[INFO] Running com.aimedical.common.pom.ParentPomModuleRegistrationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.common.pom.ParentPomModuleRegistrationTest
[INFO] Running com.aimedical.common.pom.ParentPomTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.common.pom.ParentPomTest
[INFO] Running com.aimedical.common.pom.ParentPomVerificationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.common.pom.ParentPomVerificationTest
[INFO] Running com.aimedical.common.pom.ParentPomVersionTest
[WARNING] Tests run: 5, Failures: 0, Errors: 0, Skipped: 5, Time elapsed: 0 s -- in com.aimedical.common.pom.ParentPomVersionTest
[INFO] Running com.aimedical.common.result.PageQueryTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.043 s -- in com.aimedical.common.result.PageQueryTest
[INFO] Running com.aimedical.common.result.PageResponseTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.common.result.PageResponseTest
[INFO] Running com.aimedical.common.result.ResultTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.result.ResultTest
[INFO] Running com.aimedical.common.util.SimpleMessageInterpolatorTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.common.util.SimpleMessageInterpolatorTest
[INFO] 
[INFO] Results:
[INFO] 
[WARNING] Tests run: 201, Failures: 0, Errors: 0, Skipped: 5
[INFO] 
[INFO] 
[INFO] --------------------< com.aimedical:common-module >---------------------
[INFO] Building Common Module Aggregator 0.0.1-SNAPSHOT                  [3/16]
[INFO]   from modules\common-module\pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ common-module ---
[INFO] Skipping JaCoCo execution because property jacoco.skip is set.
[INFO] argLine set to empty
[INFO] 
[INFO] ------------------< com.aimedical:common-module-api >-------------------
[INFO] Building common-module-api 0.0.1-SNAPSHOT                         [4/16]
[INFO]   from modules\common-module\common-module-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ common-module-api ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\common-module\\common-module-api\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-api\src\test\resources
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
[INFO] Running com.aimedical.modules.commonmodule.api.PositionEnumTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.072 s -- in com.aimedical.modules.commonmodule.api.PositionEnumTest
[INFO] Running com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] Running com.aimedical.modules.commonmodule.auth.UserInfoResponseTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.auth.UserInfoResponseTest
[INFO] Running com.aimedical.modules.commonmodule.doctor.AvailableDoctorTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.commonmodule.doctor.AvailableDoctorTest
[INFO] Running com.aimedical.modules.commonmodule.doctor.DoctorFacadeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.doctor.DoctorFacadeTest
[INFO] Running com.aimedical.modules.commonmodule.drug.DrugFacadeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.drug.DrugFacadeTest
[INFO] Running com.aimedical.modules.commonmodule.drug.DrugInfoTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.commonmodule.drug.DrugInfoTest
[INFO] Running com.aimedical.modules.commonmodule.event.RegistrationEventTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.commonmodule.event.RegistrationEventTest
[INFO] Running com.aimedical.modules.commonmodule.store.impl.ConcurrentHashMapStoreTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.065 s -- in com.aimedical.modules.commonmodule.store.impl.ConcurrentHashMapStoreTest
[INFO] Running com.aimedical.modules.commonmodule.visit.VisitFacadeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.visit.VisitFacadeTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT                        [5/16]
[INFO]   from modules\common-module\common-module-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ common-module-impl ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\common-module\\common-module-impl\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-impl ---
[INFO] Copying 0 resource from src\test\resources to target\test-classes
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
[INFO] Running com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLoggerTest
10:33:53.346 [main] WARN com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLogger -- Audit log write failed: Cannot invoke "com.aimedical.modules.commonmodule.auth.audit.SecurityAuditEvent.timestamp()" because "event" is null
10:33:53.373 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.373 eventType=LOGIN_FAILED userId=null username=null clientIp=10.0.0.1 success=false failureReason=BAD_CREDENTIALS refreshTokenMasked=abc123*** newJti=new-jti
10:33:53.379 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.378 eventType=LOGIN_FAILED userId=null username=null clientIp=10.0.0.1 success=false failureReason=USER_NOT_FOUND
10:33:53.381 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.381 eventType=LOGIN_FAILED userId=null username=null clientIp=192.168.1.1 success=false failureReason=BAD_CREDENTIALS
10:33:53.384 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.384 eventType=LOGOUT userId=2 username=user clientIp=10.0.0.1 success=true
10:33:53.387 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.387 eventType=LOGIN_SUCCESS userId=1 username=testuser clientIp=127.0.0.1 success=true newJti=jti-xxx
10:33:53.391 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.391 eventType=LOGOUT userId=2 username=johndoe clientIp=10.0.0.1 success=true refreshTokenMasked=abc123***
10:33:53.393 [main] INFO SECURITY_AUDIT -- timestamp=2026-06-30T10:33:53.393 eventType=LOGIN_SUCCESS userId=1 username=testuser clientIp=127.0.0.1 success=true
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.213 s -- in com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLoggerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.035 s -- in com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Running com.aimedical.modules.commonmodule.auth.config.AuthModuleConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.auth.config.AuthModuleConfigTest
[INFO] Running com.aimedical.modules.commonmodule.auth.converter.UserConverterTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.040 s -- in com.aimedical.modules.commonmodule.auth.converter.UserConverterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationExceptionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationExceptionTest
[INFO] Running com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest
[INFO] Running com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProviderTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.583 s -- in com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProviderTest
[INFO] Running com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.283 s -- in com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.password.PasswordChangeServiceImplTest
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.328 s -- in com.aimedical.modules.commonmodule.auth.password.PasswordChangeServiceImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.password.PasswordPolicyImplTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s -- in com.aimedical.modules.commonmodule.auth.password.PasswordPolicyImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.13 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.594 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.224 s -- in com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.321 s -- in com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest
10:34:09.737 [main] WARN com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter -- Account disabled, userId=1
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.550 s -- in com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest
10:34:09.779 [main] WARN com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter -- Password change required for userId=1, blocking request: GET /api/auth/me
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.147 s -- in com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.071 s -- in com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1CoexistenceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1CoexistenceTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
10:34:10.642 [main] INFO org.springframework.security.web.DefaultSecurityFilterChain -- Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@6fe33512, org.springframework.security.web.header.HeaderWriterFilter@1fa77770, org.springframework.web.filter.CorsFilter@62f3ad90, com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter@18d63996, com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter@6e6bd43b, com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter@18d63996, com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter@6e6bd43b, com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter@4a532271, com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter@4a532271, org.springframework.security.web.session.SessionManagementFilter@35b38986, org.springframework.security.web.access.ExceptionTranslationFilter@1c000c2f, org.springframework.security.web.access.intercept.AuthorizationFilter@2c7ac998]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.578 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
[INFO] Running com.aimedical.modules.commonmodule.auth.UserFacadeImplTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.modules.commonmodule.auth.UserFacadeImplTest
[INFO] Running com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.147 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.217 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.102 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$PathIdConsistencyTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$PathIdConsistencyTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.136 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
10:34:11.387 [main] INFO org.hibernate.validator.internal.util.Version -- HV000001: Hibernate Validator 8.0.1.Final
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.016 s -- in com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.069 s -- in com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$ValidateTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$ValidateTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$GetterSetterTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$GetterSetterTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$DefaultValueTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$DefaultValueTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
10:34:12.384 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
10:34:12.392 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
10:34:12.395 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
10:34:12.412 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.078 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Running com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserRepositoryTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-30T10:34:13.908+08:00  INFO 31076 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Starting UserRepositoryTest using Java 21.0.11 with PID 31076 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl)
2026-06-30T10:34:13.910+08:00  INFO 31076 --- [           main] c.a.m.c.permission.UserRepositoryTest    : No active profile set, falling back to 1 default profile: "default"
2026-06-30T10:34:14.678+08:00  INFO 31076 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-30T10:34:14.804+08:00  INFO 31076 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 104 ms. Found 4 JPA repository interfaces.
2026-06-30T10:34:14.920+08:00  INFO 31076 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-06-30T10:34:15.240+08:00  INFO 31076 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:7e60a786-a595-40ed-8080-5e769f2332f9;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-06-30T10:34:15.951+08:00  INFO 31076 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-30T10:34:16.072+08:00  INFO 31076 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-06-30T10:34:16.205+08:00  INFO 31076 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-30T10:34:16.436+08:00  INFO 31076 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-30T10:34:18.452+08:00  INFO 31076 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: drop table if exists post_function cascade 
Hibernate: drop table if exists sys_function cascade 
Hibernate: drop table if exists sys_post cascade 
Hibernate: drop table if exists sys_role cascade 
Hibernate: drop table if exists sys_user cascade 
Hibernate: drop table if exists user_post cascade 
Hibernate: drop table if exists user_role cascade 
Hibernate: create table post_function (function_id bigint not null, post_id bigint not null, primary key (function_id, post_id))
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
2026-06-30T10:34:18.562+08:00  INFO 31076 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-30T10:34:19.198+08:00  INFO 31076 --- [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-06-30T10:34:20.097+08:00  INFO 31076 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Started UserRepositoryTest in 7.034 seconds (process running for 28.641)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
2026-06-30T10:34:20.592+08:00  WARN 31076 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 23502, SQLState: 23502
2026-06-30T10:34:20.592+08:00 ERROR 31076 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : NULL not allowed for column "PASSWORD"; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [23502-224]
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,p1_0.user_id,p1_1.id,p1_1.code,p1_1.created_at,p1_1.deleted,p1_1.description,p1_1.enabled,f1_0.post_id,f1_1.id,f1_1.code,f1_1.component,f1_1.created_at,f1_1.deleted,f1_1.description,f1_1.enabled,f1_1.icon,f1_1.name,f1_1.parent_id,f1_1.path,f1_1.sort_order,f1_1.type,f1_1.updated_at,f1_1.visible,p1_1.name,p1_1.role_id,p1_1.sort,p1_1.updated_at,r2_0.user_id,r2_1.id,r2_1.code,r2_1.created_at,r2_1.deleted,r2_1.description,r2_1.enabled,r2_1.name,r2_1.sort,r2_1.updated_at,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 left join user_post p1_0 on u1_0.id=p1_0.user_id left join sys_post p1_1 on p1_1.id=p1_0.post_id left join post_function f1_0 on p1_1.id=f1_0.post_id left join sys_function f1_1 on f1_1.id=f1_0.function_id left join user_role r2_0 on u1_0.id=r2_0.user_id left join sys_role r2_1 on r2_1.id=r2_0.role_id where u1_0.id=?
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,p1_0.user_id,p1_1.id,p1_1.code,p1_1.created_at,p1_1.deleted,p1_1.description,p1_1.enabled,f1_0.post_id,f1_1.id,f1_1.code,f1_1.component,f1_1.created_at,f1_1.deleted,f1_1.description,f1_1.enabled,f1_1.icon,f1_1.name,f1_1.parent_id,f1_1.path,f1_1.sort_order,f1_1.type,f1_1.updated_at,f1_1.visible,p1_1.name,p1_1.role_id,p1_1.sort,p1_1.updated_at,r2_0.user_id,r2_1.id,r2_1.code,r2_1.created_at,r2_1.deleted,r2_1.description,r2_1.enabled,r2_1.name,r2_1.sort,r2_1.updated_at,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 left join user_post p1_0 on u1_0.id=p1_0.user_id left join sys_post p1_1 on p1_1.id=p1_0.post_id left join post_function f1_0 on p1_1.id=f1_0.post_id left join sys_function f1_1 on f1_1.id=f1_0.function_id left join user_role r2_0 on u1_0.id=r2_0.user_id left join sys_role r2_1 on r2_1.id=r2_0.role_id where u1_0.id=?
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.282 s -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest
2026-06-30T10:34:21.280+08:00  INFO 31076 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T10:34:21.304+08:00  INFO 31076 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T10:34:21.316+08:00  INFO 31076 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T10:34:21.320+08:00  INFO 31076 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-06-30T10:34:21.324+08:00  INFO 31076 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û������޸ĳɹ���userId: 1
2026-06-30T10:34:21.333+08:00  INFO 31076 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.581 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.122 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[WARNING] Tests run: 8, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.015 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.175 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[WARNING] Tests run: 400, Failures: 0, Errors: 0, Skipped: 1
[INFO] 
[INFO] 
[INFO] --------------------------< com.aimedical:ai >--------------------------
[INFO] Building AI Module Aggregator 0.0.1-SNAPSHOT                      [6/16]
[INFO]   from modules\ai\pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ ai ---
[INFO] Skipping JaCoCo execution because property jacoco.skip is set.
[INFO] argLine set to empty
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT                                    [7/16]
[INFO]   from modules\ai\ai-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ ai-api ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\ai\\ai-api\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\test\resources
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
[INFO] Running com.aimedical.modules.ai.api.AiResultFactoryTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.074 s -- in com.aimedical.modules.ai.api.AiResultFactoryTest
[INFO] Running com.aimedical.modules.ai.api.AiResultTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.ai.api.AiResultTest
[INFO] Running com.aimedical.modules.ai.api.AiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.aimedical.modules.ai.api.AiServiceTest
[INFO] Running com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordDtoTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordDtoTest
[INFO] Running com.aimedical.modules.ai.api.dto.prescription.PrescriptionDtoTest
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.032 s -- in com.aimedical.modules.ai.api.dto.prescription.PrescriptionDtoTest
[INFO] Running com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 132, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] -----------------------< com.aimedical:ai-impl >------------------------
[INFO] Building ai-impl 0.0.1-SNAPSHOT                                   [8/16]
[INFO]   from modules\ai\ai-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ ai-impl ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\ai\\ai-impl\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\test\resources
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
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.078 s -- in com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
10:34:25.539 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.543 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.551 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.551 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.554 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.554 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.555 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.555 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.555 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.557 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.597 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.603 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.603 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.605 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.605 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.606 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.606 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.606 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.607 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.607 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.609 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.609 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.616 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.616 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.618 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.618 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.624 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.624 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.625 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
10:34:25.625 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.478 s -- in com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.pom.AiImplPomCleanDependencyTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.130 s -- in com.aimedical.modules.ai.impl.pom.AiImplPomCleanDependencyTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] -----------------------< com.aimedical:patient >------------------------
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.069 s -- in com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Running com.aimedical.modules.patient.entity.AllergyHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in com.aimedical.modules.patient.entity.AllergyHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.AllergySeverityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.patient.entity.AllergySeverityTest
[INFO] Running com.aimedical.modules.patient.entity.BloodTypeTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.patient.entity.BloodTypeTest
[INFO] Running com.aimedical.modules.patient.entity.ChronicDiseaseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.patient.entity.ChronicDiseaseTest
[INFO] Running com.aimedical.modules.patient.entity.DiseaseStatusTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.patient.entity.DiseaseStatusTest
[INFO] Running com.aimedical.modules.patient.entity.FamilyHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.patient.entity.FamilyHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.GenderTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in com.aimedical.modules.patient.entity.GenderTest
[INFO] Running com.aimedical.modules.patient.entity.HealthProfileTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in com.aimedical.modules.patient.entity.HealthProfileTest
[INFO] Running com.aimedical.modules.patient.entity.MedicationHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.patient.entity.MedicationHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Running com.aimedical.modules.patient.entity.SurgeryHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.patient.entity.SurgeryHistoryTest
[INFO] Running com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Running com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.patient.service.impl.PatientServiceImplTest
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.055 s -- in com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Running com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Running com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s -- in com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Running com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.doctor.service.impl.DoctorServiceImplTest
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.061 s -- in com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Running com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Running com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Running com.aimedical.modules.admin.entity.dict.DictDataTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.aimedical.modules.admin.entity.dict.DictDataTest
[INFO] Running com.aimedical.modules.admin.entity.dict.DictTypeTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.admin.entity.dict.DictTypeTest
[INFO] Running com.aimedical.modules.admin.entity.LoginTypeTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.admin.entity.LoginTypeTest
[INFO] Running com.aimedical.modules.admin.entity.TokenStoreTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.admin.entity.TokenStoreTest
[INFO] Running com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.admin.service.impl.AdminServiceImplTest
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
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.119 s -- in com.aimedical.modules.consultation.ConsultationDtoTest
[INFO] Running com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Running com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Running com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.344 s -- in com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Running com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.100 s -- in com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionManagerTest
10:34:31.843 [Thread-7] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-3] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-11] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-10] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-9] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-8] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-4] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-6] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.848 [Thread-5] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: concurrent-session, returning existing session
10:34:31.853 [main] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: session-001, returning existing session
10:34:31.859 [Thread-14] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-17] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-13] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-19] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-21] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-18] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-16] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-15] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
10:34:31.859 [Thread-20] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: same-session, returning existing session
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.096 s -- in com.aimedical.modules.consultation.DialogueSessionManagerTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Running com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.030 s -- in com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] Running com.aimedical.modules.consultation.SchedulingRetryConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.aimedical.modules.consultation.SchedulingRetryConfigTest
[INFO] Running com.aimedical.modules.consultation.StaticDepartmentFallbackProviderTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.consultation.StaticDepartmentFallbackProviderTest
[INFO] Running com.aimedical.modules.consultation.TriageControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.consultation.TriageControllerTest
[INFO] Running com.aimedical.modules.consultation.TriageConverterTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.consultation.TriageConverterTest
[INFO] Running com.aimedical.modules.consultation.TriageServiceImplTest
10:34:32.074 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.RuntimeException DoctorFacade error
10:34:32.074 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.lang.RuntimeException DoctorFacade error
10:34:32.089 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.RuntimeException DoctorFacade error
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.155 s -- in com.aimedical.modules.consultation.TriageServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 140, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ---------------------< com.aimedical:prescription >---------------------
[INFO] Building prescription 0.0.1-SNAPSHOT                             [13/16]
[INFO]   from modules\prescription\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ prescription ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\prescription\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ prescription ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\prescription\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\prescription\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ prescription ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ prescription ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\prescription\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ prescription ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ prescription ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.prescription.api.PrescriptionAssistControllerTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.567 s -- in com.aimedical.modules.prescription.api.PrescriptionAssistControllerTest
[INFO] Running com.aimedical.modules.prescription.api.PrescriptionAuditControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.138 s -- in com.aimedical.modules.prescription.api.PrescriptionAuditControllerTest
[INFO] Running com.aimedical.modules.prescription.context.DosageAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.context.DosageAlertTest
[INFO] Running com.aimedical.modules.prescription.context.PrescriptionDraftContextTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.087 s -- in com.aimedical.modules.prescription.context.PrescriptionDraftContextTest
[INFO] Running com.aimedical.modules.prescription.converter.AssistConverterTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.modules.prescription.converter.AssistConverterTest
[INFO] Running com.aimedical.modules.prescription.converter.AuditConverterTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.058 s -- in com.aimedical.modules.prescription.converter.AuditConverterTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AiSuggestionResultTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AiSuggestionResultTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AiSuggestionStatusTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AiSuggestionStatusTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AllergyWarningItemTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AllergyWarningItemTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverityTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageAlertLevelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.assist.DosageAlertLevelTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.prescription.dto.assist.DosageAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageCheckRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.assist.DosageCheckRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageCheckResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.DosageCheckResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DoseWarningTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.DoseWarningTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DoseWarningTypeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.DoseWarningTypeTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AlertSeverityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.AlertSeverityTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AllergyDetailTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.audit.AllergyDetailTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.AuditAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditIssueTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in com.aimedical.modules.prescription.dto.audit.AuditIssueTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.AuditRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.AuditResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.BlockResponseTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.BlockResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.DrugInteractionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.DrugInteractionTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.PatientInfoTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.PatientInfoTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.PrescriptionItemTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.audit.PrescriptionItemTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SubmitRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.SubmitRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SubmitResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.SubmitResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SuggestionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.SuggestionTest
[INFO] Running com.aimedical.modules.prescription.entity.AuditRecordTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.prescription.entity.AuditRecordTest
[INFO] Running com.aimedical.modules.prescription.PrescriptionErrorCodeTest
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.034 s <<< FAILURE! -- in com.aimedical.modules.prescription.PrescriptionErrorCodeTest
[ERROR] com.aimedical.modules.prescription.PrescriptionErrorCodeTest.shouldExposeCodeAndMessage -- Time elapsed: 0.019 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <WARN���δȷ��> but was: <WARN ���δȷ�ϣ��� forceSubmit=true ����>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
	at com.aimedical.modules.prescription.PrescriptionErrorCodeTest.shouldExposeCodeAndMessage(PrescriptionErrorCodeTest.java:21)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] Running com.aimedical.modules.prescription.PrescriptionPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.PrescriptionPlaceholderTest
[INFO] Running com.aimedical.modules.prescription.rule.AllergyCheckRuleTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.071 s -- in com.aimedical.modules.prescription.rule.AllergyCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.ContraindicationCheckRuleTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.157 s -- in com.aimedical.modules.prescription.rule.ContraindicationCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DefaultLocalRuleEngineTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.358 s -- in com.aimedical.modules.prescription.rule.DefaultLocalRuleEngineTest
[INFO] Running com.aimedical.modules.prescription.rule.DosageLimitRuleTest
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.086 s <<< FAILURE! -- in com.aimedical.modules.prescription.rule.DosageLimitRuleTest
[ERROR] com.aimedical.modules.prescription.rule.DosageLimitRuleTest.shouldMatchByAgeWhenPatientInfoAvailable -- Time elapsed: 0.020 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <BLOCK> but was: <WARN>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
	at com.aimedical.modules.prescription.rule.DosageLimitRuleTest.shouldMatchByAgeWhenPatientInfoAvailable(DosageLimitRuleTest.java:145)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] Running com.aimedical.modules.prescription.rule.DrugInteractionRuleTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.rule.DrugInteractionRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DuplicateCheckRuleTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.130 s -- in com.aimedical.modules.prescription.rule.DuplicateCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugAllergyMappingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.rule.entity.DrugAllergyMappingTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugCompositionDictTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.rule.entity.DrugCompositionDictTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugContraindicationMappingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.rule.entity.DrugContraindicationMappingTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugInteractionPairTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.rule.entity.DrugInteractionPairTest
[INFO] Running com.aimedical.modules.prescription.rule.LocalRuleResultTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.rule.LocalRuleResultTest
[INFO] Running com.aimedical.modules.prescription.rule.SpecialPopulationDosageRuleTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.prescription.rule.SpecialPopulationDosageRuleTest
[INFO] Running com.aimedical.modules.prescription.service.assist.DedupTaskSchedulerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.086 s -- in com.aimedical.modules.prescription.service.assist.DedupTaskSchedulerTest
[INFO] Running com.aimedical.modules.prescription.service.assist.DosageThresholdServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.026 s -- in com.aimedical.modules.prescription.service.assist.DosageThresholdServiceTest
[INFO] Running com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest
[ERROR] Tests run: 14, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.396 s <<< FAILURE! -- in com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest
[ERROR] com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest.assistShouldGeneratePrescriptionIdWhenBlank -- Time elapsed: 0.006 s <<< ERROR!
java.lang.NullPointerException: Cannot invoke "com.aimedical.modules.prescription.rule.LocalRuleResult.isPassed()" because "ruleResult" is null
	at com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImpl.checkAllergies(PrescriptionAssistServiceImpl.java:270)
	at com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImpl.assist(PrescriptionAssistServiceImpl.java:103)
	at com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest.assistShouldGeneratePrescriptionIdWhenBlank(PrescriptionAssistServiceImplTest.java:86)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] Running com.aimedical.modules.prescription.service.audit.AuditRiskLevelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.service.audit.AuditRiskLevelTest
[INFO] Running com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditEnforcerImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditEnforcerImplTest
[INFO] Running com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
10:34:39.291 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=null
10:34:39.321 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=null
10:34:39.334 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
10:34:39.352 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR_FAIL
10:34:39.401 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
[ERROR] Tests run: 27, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 0.872 s <<< FAILURE! -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
[ERROR] com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock -- Time elapsed: 0.130 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <false> but was: <true>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
	at org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
	at org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:31)
	at org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:231)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock(PrescriptionAuditServiceImplTest.java:549)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.submitShouldReturnConcurrentSubmitErrorWhenOptimisticLockException -- Time elapsed: 0.012 s <<< ERROR!
jakarta.persistence.OptimisticLockException
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl.handleStepThree(PrescriptionAuditServiceImpl.java:236)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl.submit(PrescriptionAuditServiceImpl.java:170)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest.submitShouldReturnConcurrentSubmitErrorWhenOptimisticLockException(PrescriptionAuditServiceImplTest.java:386)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   PrescriptionErrorCodeTest.shouldExposeCodeAndMessage:21 expected: <WARN���δȷ��> but was: <WARN ���δȷ�ϣ��� forceSubmit=true ����>
[ERROR]   DosageLimitRuleTest.shouldMatchByAgeWhenPatientInfoAvailable:145 expected: <BLOCK> but was: <WARN>
[ERROR]   PrescriptionAuditServiceImplTest.submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock:549 expected: <false> but was: <true>
[ERROR] Errors: 
[ERROR]   PrescriptionAssistServiceImplTest.assistShouldGeneratePrescriptionIdWhenBlank:86 ? NullPointer Cannot invoke "com.aimedical.modules.prescription.rule.LocalRuleResult.isPassed()" because "ruleResult" is null
[ERROR]   PrescriptionAuditServiceImplTest.submitShouldReturnConcurrentSubmitErrorWhenOptimisticLockException:386 ? OptimisticLock
[INFO] 
[ERROR] Tests run: 153, Failures: 3, Errors: 2, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.185 s]
[INFO] common ............................................. SUCCESS [  9.554 s]
[INFO] Common Module Aggregator ........................... SUCCESS [  0.008 s]
[INFO] common-module-api .................................. SUCCESS [  1.256 s]
[INFO] common-module-impl ................................. SUCCESS [ 30.928 s]
[INFO] AI Module Aggregator ............................... SUCCESS [  0.004 s]
[INFO] ai-api ............................................. SUCCESS [  1.240 s]
[INFO] ai-impl ............................................ SUCCESS [  2.743 s]
[INFO] patient ............................................ SUCCESS [  1.435 s]
[INFO] doctor ............................................. SUCCESS [  1.113 s]
[INFO] admin .............................................. SUCCESS [  1.221 s]
[INFO] consultation ....................................... SUCCESS [  2.595 s]
[INFO] prescription ....................................... FAILURE [  7.385 s]
[INFO] medical-record ..................................... SKIPPED
[INFO] application ........................................ SKIPPED
[INFO] integration ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:00 min
[INFO] Finished at: 2026-06-30T10:34:39+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project prescription: There are test failures.
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
