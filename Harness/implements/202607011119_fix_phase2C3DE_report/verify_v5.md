# 验证报告（v5）

## 结果
PASSED

## 统计
- 通过：1604
- 失败：0
- 跳过：6

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

2026-07-01T14:30:35.809+08:00  INFO 2732 --- [           main] c.a.common.base.BaseEntityAuditTest      : Starting BaseEntityAuditTest using Java 21.0.11 with PID 2732 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\common)
2026-07-01T14:30:35.813+08:00  INFO 2732 --- [           main] c.a.common.base.BaseEntityAuditTest      : No active profile set, falling back to 1 default profile: "default"
2026-07-01T14:30:36.315+08:00  INFO 2732 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-07-01T14:30:36.348+08:00  INFO 2732 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 18 ms. Found 0 JPA repository interfaces.
2026-07-01T14:30:36.405+08:00  INFO 2732 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-07-01T14:30:36.610+08:00  INFO 2732 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:0a4e622a-feec-4d48-9bae-a017c929753e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-07-01T14:30:37.041+08:00  INFO 2732 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-07-01T14:30:37.129+08:00  INFO 2732 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-07-01T14:30:37.173+08:00  INFO 2732 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-07-01T14:30:37.485+08:00  INFO 2732 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-07-01T14:30:38.615+08:00  INFO 2732 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: drop table if exists test_audit_entity cascade 
Hibernate: create table test_audit_entity (deleted boolean not null, created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), primary key (id))
2026-07-01T14:30:38.655+08:00  INFO 2732 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-07-01T14:30:38.797+08:00  INFO 2732 --- [           main] c.a.common.base.BaseEntityAuditTest      : Started BaseEntityAuditTest in 3.459 seconds (process running for 4.841)
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
Hibernate: insert into test_audit_entity (created_at,deleted,updated_at,id) values (?,?,?,default)
Hibernate: update test_audit_entity set created_at=?,deleted=?,updated_at=? where id=?
Hibernate: insert into test_audit_entity (created_at,deleted,updated_at,id) values (?,?,?,default)
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.703 s -- in com.aimedical.common.base.BaseEntityAuditTest
[INFO] Running com.aimedical.common.base.BaseEntityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.common.base.BaseEntityTest
[INFO] Running com.aimedical.common.base.BaseEnumTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.common.base.BaseEnumTest
[INFO] Running com.aimedical.common.CommonPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.common.CommonPlaceholderTest
[INFO] Running com.aimedical.common.config.GlobalExceptionHandlerTest
2026-07-01T14:30:39.592+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=BIZ_ERR, message=ҵ���쳣
2026-07-01T14:30:39.603+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=RATE_LIMITED, message=��¼���Թ���Ƶ�������Ժ�����
2026-07-01T14:30:39.606+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=RATE_LIMITED, message=��¼���Թ���Ƶ�������Ժ�����
2026-07-01T14:30:39.607+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=ACCOUNT_LOCKED, message=�˻�����������{����ʱ��}������
2026-07-01T14:30:39.608+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=BIZ_ERR, message=ҵ���쳣
2026-07-01T14:30:39.623+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=NUM_ERR, message=����{0}�ѹ��ڣ�ʣ��{1}��
2026-07-01T14:30:39.625+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=BIZ_ERR, message=ҵ���쳣
2026-07-01T14:30:39.627+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=NUM_ERR, message=����{0}�ѹ��ڣ�ʣ��{1}��
2026-07-01T14:30:39.630+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=ACCOUNT_LOCKED, message=�˻�����������{����ʱ��}������
2026-07-01T14:30:39.631+08:00 ERROR 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : System exception

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

2026-07-01T14:30:39.639+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Request body malformed

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

2026-07-01T14:30:39.640+08:00  WARN 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Business exception: code=TOKEN_REFRESH_FAILED, message=����ˢ��ʧ�ܣ������µ�¼
2026-07-01T14:30:39.641+08:00 ERROR 2732 --- [           main] c.a.c.config.GlobalExceptionHandler      : Response body serialization failed

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

[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.058 s -- in com.aimedical.common.config.GlobalExceptionHandlerTest
[INFO] Running com.aimedical.common.config.JacksonConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.common.config.JacksonConfigTest
[INFO] Running com.aimedical.common.config.JpaConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.common.config.JpaConfigTest
[INFO] Running com.aimedical.common.entity.DosageStandardAuditTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-07-01T14:30:39.749+08:00  INFO 2732 --- [           main] c.a.c.entity.DosageStandardAuditTest     : Starting DosageStandardAuditTest using Java 21.0.11 with PID 2732 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\common)
2026-07-01T14:30:39.749+08:00  INFO 2732 --- [           main] c.a.c.entity.DosageStandardAuditTest     : No active profile set, falling back to 1 default profile: "default"
2026-07-01T14:30:39.833+08:00  INFO 2732 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-07-01T14:30:39.836+08:00  INFO 2732 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 2 ms. Found 0 JPA repository interfaces.
2026-07-01T14:30:39.849+08:00  INFO 2732 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-07-01T14:30:39.866+08:00  INFO 2732 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:0c844ca0-f368-40eb-929b-534d38180304;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-07-01T14:30:39.884+08:00  INFO 2732 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-07-01T14:30:39.886+08:00  INFO 2732 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-07-01T14:30:39.889+08:00  INFO 2732 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-07-01T14:30:39.982+08:00  INFO 2732 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: drop table if exists dosage_standard cascade 
Hibernate: create table dosage_standard (age_range_end integer, age_range_start integer, daily_max numeric(12,3), deleted boolean not null, single_max numeric(12,3) not null, weight_range_end numeric(10,2), weight_range_start numeric(10,2), created_at timestamp(6), id bigint generated by default as identity, updated_at timestamp(6), route_of_administration varchar(20) not null, unit varchar(20) not null, drug_code varchar(50) not null, primary key (id))
Hibernate: create index idx_dosage_drug_route on dosage_standard (drug_code, route_of_administration)
Hibernate: create index idx_dosage_drug_route_age_weight on dosage_standard (drug_code, route_of_administration, age_range_start, age_range_end, weight_range_start, weight_range_end)
2026-07-01T14:30:39.987+08:00  INFO 2732 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-07-01T14:30:40.015+08:00  INFO 2732 --- [           main] c.a.c.entity.DosageStandardAuditTest     : Started DosageStandardAuditTest in 0.283 seconds (process running for 6.06)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: update dosage_standard set age_range_end=?,age_range_start=?,created_at=?,daily_max=?,deleted=?,drug_code=?,route_of_administration=?,single_max=?,unit=?,updated_at=?,weight_range_end=?,weight_range_start=? where id=?
Hibernate: insert into dosage_standard (age_range_end,age_range_start,created_at,daily_max,deleted,drug_code,route_of_administration,single_max,unit,updated_at,weight_range_end,weight_range_start,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.403 s -- in com.aimedical.common.entity.DosageStandardAuditTest
[INFO] Running com.aimedical.common.entity.DosageStandardTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.039 s -- in com.aimedical.common.entity.DosageStandardTest
[INFO] Running com.aimedical.common.exception.BusinessExceptionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.common.exception.BusinessExceptionTest
[INFO] Running com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.common.exception.GlobalErrorCodeTest
[INFO] Running com.aimedical.common.pom.AggregatorPomTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.048 s -- in com.aimedical.common.pom.AggregatorPomTest
[INFO] Running com.aimedical.common.pom.ApplicationPomTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.common.pom.ApplicationPomTest
[INFO] Running com.aimedical.common.pom.CommonPomTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.common.pom.CommonPomTest
[INFO] Running com.aimedical.common.pom.MovedModulePomTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in com.aimedical.common.pom.MovedModulePomTest
[INFO] Running com.aimedical.common.pom.MovedModulePomVerificationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.common.pom.MovedModulePomVerificationTest
[INFO] Running com.aimedical.common.pom.NewModulePomTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s -- in com.aimedical.common.pom.NewModulePomTest
[INFO] Running com.aimedical.common.pom.ParentPomDependencyManagementCleanupTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.common.pom.ParentPomDependencyManagementCleanupTest
[INFO] Running com.aimedical.common.pom.ParentPomModuleRegistrationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.common.pom.ParentPomModuleRegistrationTest
[INFO] Running com.aimedical.common.pom.ParentPomTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.common.pom.ParentPomTest
[INFO] Running com.aimedical.common.pom.ParentPomVerificationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.common.pom.ParentPomVerificationTest
[INFO] Running com.aimedical.common.pom.ParentPomVersionTest
[WARNING] Tests run: 5, Failures: 0, Errors: 0, Skipped: 5, Time elapsed: 0 s -- in com.aimedical.common.pom.ParentPomVersionTest
[INFO] Running com.aimedical.common.result.PageQueryTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.030 s -- in com.aimedical.common.result.PageQueryTest
[INFO] Running com.aimedical.common.result.PageResponseTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.common.result.PageResponseTest
[INFO] Running com.aimedical.common.result.ResultTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.result.ResultTest
[INFO] Running com.aimedical.common.util.SimpleMessageInterpolatorTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.common.util.SimpleMessageInterpolatorTest
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
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.082 s -- in com.aimedical.modules.commonmodule.api.PositionEnumTest
[INFO] Running com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.api.UserTypeTest
[INFO] Running com.aimedical.modules.commonmodule.auth.UserInfoResponseTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.commonmodule.auth.UserInfoResponseTest
[INFO] Running com.aimedical.modules.commonmodule.doctor.AvailableDoctorTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in com.aimedical.modules.commonmodule.doctor.AvailableDoctorTest
[INFO] Running com.aimedical.modules.commonmodule.doctor.DoctorFacadeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.doctor.DoctorFacadeTest
[INFO] Running com.aimedical.modules.commonmodule.drug.DrugFacadeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.drug.DrugFacadeTest
[INFO] Running com.aimedical.modules.commonmodule.drug.DrugInfoTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.drug.DrugInfoTest
[INFO] Running com.aimedical.modules.commonmodule.event.RegistrationEventTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.event.RegistrationEventTest
[INFO] Running com.aimedical.modules.commonmodule.store.impl.ConcurrentHashMapStoreTest
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.057 s -- in com.aimedical.modules.commonmodule.store.impl.ConcurrentHashMapStoreTest
[INFO] Running com.aimedical.modules.commonmodule.store.impl.DraftContextStoreImplTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.commonmodule.store.impl.DraftContextStoreImplTest
[INFO] Running com.aimedical.modules.commonmodule.visit.VisitFacadeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.visit.VisitFacadeTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 111, Failures: 0, Errors: 0, Skipped: 0
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
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 54 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-impl ---
[INFO] Copying 0 resource from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 44 source files with javac [debug release 17] to target\test-classes
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtUtilTest.java: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\test\java\com\aimedical\modules\commonmodule\jwt\JwtUtilTest.javaʹ�û򸲸����ѹ�ʱ�� API��
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtUtilTest.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:deprecation ���±��롣
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java: ĳЩ�����ļ�ʹ����δ�����򲻰�ȫ�Ĳ�����
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:unchecked ���±��롣
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLoggerTest
14:30:48.171 [main] WARN com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLogger -- Audit log write failed: Cannot invoke "com.aimedical.modules.commonmodule.auth.audit.SecurityAuditEvent.timestamp()" because "event" is null
14:30:48.189 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.189 eventType=LOGIN_FAILED userId=null username=null clientIp=10.0.0.1 success=false failureReason=BAD_CREDENTIALS refreshTokenMasked=abc123*** newJti=new-jti
14:30:48.194 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.194 eventType=LOGIN_FAILED userId=null username=null clientIp=10.0.0.1 success=false failureReason=USER_NOT_FOUND
14:30:48.197 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.196 eventType=LOGIN_FAILED userId=null username=null clientIp=192.168.1.1 success=false failureReason=BAD_CREDENTIALS
14:30:48.200 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.2 eventType=LOGOUT userId=2 username=user clientIp=10.0.0.1 success=true
14:30:48.203 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.203 eventType=LOGIN_SUCCESS userId=1 username=testuser clientIp=127.0.0.1 success=true newJti=jti-xxx
14:30:48.207 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.205 eventType=LOGOUT userId=2 username=johndoe clientIp=10.0.0.1 success=true refreshTokenMasked=abc123***
14:30:48.209 [main] INFO SECURITY_AUDIT -- timestamp=2026-07-01T14:30:48.209 eventType=LOGIN_SUCCESS userId=1 username=testuser clientIp=127.0.0.1 success=true
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.227 s -- in com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLoggerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.034 s -- in com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Running com.aimedical.modules.commonmodule.auth.config.AuthModuleConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.auth.config.AuthModuleConfigTest
[INFO] Running com.aimedical.modules.commonmodule.auth.converter.UserConverterTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.modules.commonmodule.auth.converter.UserConverterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationExceptionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationExceptionTest
[INFO] Running com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest
[INFO] Running com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProviderTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.570 s -- in com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProviderTest
[INFO] Running com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.281 s -- in com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.password.PasswordChangeServiceImplTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.582 s -- in com.aimedical.modules.commonmodule.auth.password.PasswordChangeServiceImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.password.PasswordPolicyImplTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.commonmodule.auth.password.PasswordPolicyImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.13 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.590 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.161 s -- in com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.221 s -- in com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest
14:31:03.234 [main] WARN com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter -- Account disabled, userId=1
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.352 s -- in com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest
14:31:03.249 [main] WARN com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter -- Password change required for userId=1, blocking request: GET /api/auth/me
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.092 s -- in com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 s -- in com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1CoexistenceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1CoexistenceTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
14:31:03.786 [main] INFO org.springframework.security.web.DefaultSecurityFilterChain -- Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@16eb01d4, org.springframework.security.web.header.HeaderWriterFilter@4246f904, org.springframework.web.filter.CorsFilter@5b01b3f8, com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter@27c49f54, com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter@69efdfd9, com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter@27c49f54, com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter@69efdfd9, com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter@1c149539, com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter@1c149539, org.springframework.security.web.session.SessionManagementFilter@4a5c6b30, org.springframework.security.web.access.ExceptionTranslationFilter@49616310, org.springframework.security.web.access.intercept.AuthorizationFilter@5ee76cf2]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.378 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
[INFO] Running com.aimedical.modules.commonmodule.auth.UserFacadeImplTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.041 s -- in com.aimedical.modules.commonmodule.auth.UserFacadeImplTest
[INFO] Running com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.087 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.138 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.085 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$PathIdConsistencyTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$PathIdConsistencyTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.113 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
14:31:04.299 [main] INFO org.hibernate.validator.internal.util.Version -- HV000001: Hibernate Validator 8.0.1.Final
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.696 s -- in com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.047 s -- in com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$ValidateTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$ValidateTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$GetterSetterTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$GetterSetterTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtConfigTest$DefaultValueTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest$DefaultValueTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.commonmodule.jwt.JwtConfigTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
14:31:04.982 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
14:31:04.989 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
14:31:04.997 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
14:31:05.005 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.066 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Running com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserRepositoryTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-07-01T14:31:05.848+08:00  INFO 17068 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Starting UserRepositoryTest using Java 21.0.11 with PID 17068 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl)
2026-07-01T14:31:05.849+08:00  INFO 17068 --- [           main] c.a.m.c.permission.UserRepositoryTest    : No active profile set, falling back to 1 default profile: "default"
2026-07-01T14:31:06.274+08:00  INFO 17068 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-07-01T14:31:06.390+08:00  INFO 17068 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 105 ms. Found 4 JPA repository interfaces.
2026-07-01T14:31:06.453+08:00  INFO 17068 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-07-01T14:31:06.664+08:00  INFO 17068 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:0397ebab-8fa9-4415-b89b-a59f1a2bb68e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-07-01T14:31:07.127+08:00  INFO 17068 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-07-01T14:31:07.260+08:00  INFO 17068 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-07-01T14:31:07.300+08:00  INFO 17068 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-07-01T14:31:07.435+08:00  INFO 17068 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-07-01T14:31:08.629+08:00  INFO 17068 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
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
2026-07-01T14:31:08.710+08:00  INFO 17068 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-07-01T14:31:09.134+08:00  INFO 17068 --- [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-07-01T14:31:09.648+08:00  INFO 17068 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Started UserRepositoryTest in 4.288 seconds (process running for 23.708)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
2026-07-01T14:31:09.918+08:00  WARN 17068 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 23502, SQLState: 23502
2026-07-01T14:31:09.919+08:00 ERROR 17068 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : NULL not allowed for column "PASSWORD"; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [23502-224]
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,p1_0.user_id,p1_1.id,p1_1.code,p1_1.created_at,p1_1.deleted,p1_1.description,p1_1.enabled,f1_0.post_id,f1_1.id,f1_1.code,f1_1.component,f1_1.created_at,f1_1.deleted,f1_1.description,f1_1.enabled,f1_1.icon,f1_1.name,f1_1.parent_id,f1_1.path,f1_1.sort_order,f1_1.type,f1_1.updated_at,f1_1.visible,p1_1.name,p1_1.role_id,p1_1.sort,p1_1.updated_at,r2_0.user_id,r2_1.id,r2_1.code,r2_1.created_at,r2_1.deleted,r2_1.description,r2_1.enabled,r2_1.name,r2_1.sort,r2_1.updated_at,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 left join user_post p1_0 on u1_0.id=p1_0.user_id left join sys_post p1_1 on p1_1.id=p1_0.post_id left join post_function f1_0 on p1_1.id=f1_0.post_id left join sys_function f1_1 on f1_1.id=f1_0.function_id left join user_role r2_0 on u1_0.id=r2_0.user_id left join sys_role r2_1 on r2_1.id=r2_0.role_id where u1_0.id=?
Hibernate: insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)
Hibernate: select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,p1_0.user_id,p1_1.id,p1_1.code,p1_1.created_at,p1_1.deleted,p1_1.description,p1_1.enabled,f1_0.post_id,f1_1.id,f1_1.code,f1_1.component,f1_1.created_at,f1_1.deleted,f1_1.description,f1_1.enabled,f1_1.icon,f1_1.name,f1_1.parent_id,f1_1.path,f1_1.sort_order,f1_1.type,f1_1.updated_at,f1_1.visible,p1_1.name,p1_1.role_id,p1_1.sort,p1_1.updated_at,r2_0.user_id,r2_1.id,r2_1.code,r2_1.created_at,r2_1.deleted,r2_1.description,r2_1.enabled,r2_1.name,r2_1.sort,r2_1.updated_at,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 left join user_post p1_0 on u1_0.id=p1_0.user_id left join sys_post p1_1 on p1_1.id=p1_0.post_id left join post_function f1_0 on p1_1.id=f1_0.post_id left join sys_function f1_1 on f1_1.id=f1_0.function_id left join user_role r2_0 on u1_0.id=r2_0.user_id left join sys_role r2_1 on r2_1.id=r2_0.role_id where u1_0.id=?
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.995 s -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest
2026-07-01T14:31:10.379+08:00  INFO 17068 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-07-01T14:31:10.392+08:00  INFO 17068 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-07-01T14:31:10.399+08:00  INFO 17068 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-07-01T14:31:10.401+08:00  INFO 17068 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
2026-07-01T14:31:10.403+08:00  INFO 17068 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û������޸ĳɹ���userId: 1
2026-07-01T14:31:10.409+08:00  INFO 17068 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ��ɹ�
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.383 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.064 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[WARNING] Tests run: 8, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.012 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.100 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest
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
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.ai.api.AiResultTest
[INFO] Running com.aimedical.modules.ai.api.AiServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.031 s -- in com.aimedical.modules.ai.api.AiServiceTest
[INFO] Running com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.ai.api.degradation.DegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordDtoTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordDtoTest
[INFO] Running com.aimedical.modules.ai.api.dto.prescription.PrescriptionDtoTest
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in com.aimedical.modules.ai.api.dto.prescription.PrescriptionDtoTest
[INFO] Running com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 136, Failures: 0, Errors: 0, Skipped: 0
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
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 4 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-impl\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 5 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategyTest
[INFO] Running com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
14:31:15.050 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.054 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.063 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.064 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.105 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.106 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.107 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.107 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.109 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.109 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.112 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.120 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.123 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.123 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.125 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.125 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.127 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.127 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.127 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.128 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.128 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.129 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.129 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.135 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.136 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.139 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.139 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.143 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.148 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.148 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.148 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
14:31:15.148 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.592 s -- in com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.mock.MockAdminControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in com.aimedical.modules.ai.impl.mock.MockAdminControllerTest
[INFO] Running com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.aimedical.modules.ai.impl.mock.MockAiServiceTest
[INFO] Running com.aimedical.modules.ai.impl.pom.AiImplPomCleanDependencyTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.126 s -- in com.aimedical.modules.ai.impl.pom.AiImplPomCleanDependencyTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.068 s -- in com.aimedical.modules.patient.api.PatientControllerTest
[INFO] Running com.aimedical.modules.patient.entity.AllergyHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.patient.entity.AllergyHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.AllergySeverityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.patient.entity.AllergySeverityTest
[INFO] Running com.aimedical.modules.patient.entity.BloodTypeTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.patient.entity.BloodTypeTest
[INFO] Running com.aimedical.modules.patient.entity.ChronicDiseaseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.patient.entity.ChronicDiseaseTest
[INFO] Running com.aimedical.modules.patient.entity.DiseaseStatusTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.patient.entity.DiseaseStatusTest
[INFO] Running com.aimedical.modules.patient.entity.FamilyHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.patient.entity.FamilyHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.GenderTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.patient.entity.GenderTest
[INFO] Running com.aimedical.modules.patient.entity.HealthProfileTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.patient.entity.HealthProfileTest
[INFO] Running com.aimedical.modules.patient.entity.MedicationHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.patient.entity.MedicationHistoryTest
[INFO] Running com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.patient.entity.PatientEntityTest
[INFO] Running com.aimedical.modules.patient.entity.SurgeryHistoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.patient.entity.SurgeryHistoryTest
[INFO] Running com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.patient.PatientPlaceholderTest
[INFO] Running com.aimedical.modules.patient.service.impl.PatientServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.patient.service.impl.PatientServiceImplTest
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in com.aimedical.modules.doctor.api.DoctorControllerTest
[INFO] Running com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.doctor.DoctorPlaceholderTest
[INFO] Running com.aimedical.modules.doctor.entity.DoctorEntityTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.034 s -- in com.aimedical.modules.doctor.entity.DoctorEntityTest
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
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.057 s -- in com.aimedical.modules.admin.AdminPlaceholderTest
[INFO] Running com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.aimedical.modules.admin.api.AdminControllerTest
[INFO] Running com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.modules.admin.entity.AdminEntityTest
[INFO] Running com.aimedical.modules.admin.entity.dict.DictDataTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.046 s -- in com.aimedical.modules.admin.entity.dict.DictDataTest
[INFO] Running com.aimedical.modules.admin.entity.dict.DictTypeTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.admin.entity.dict.DictTypeTest
[INFO] Running com.aimedical.modules.admin.entity.LoginTypeTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.admin.entity.LoginTypeTest
[INFO] Running com.aimedical.modules.admin.entity.TokenStoreTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.admin.entity.TokenStoreTest
[INFO] Running com.aimedical.modules.admin.service.impl.AdminServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.admin.service.impl.AdminServiceImplTest
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
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.130 s -- in com.aimedical.modules.consultation.ConsultationDtoTest
[INFO] Running com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in com.aimedical.modules.consultation.ConsultationEntityTest
[INFO] Running com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.consultation.ConsultationPlaceholderTest
[INFO] Running com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.322 s -- in com.aimedical.modules.consultation.DeadLetterCompensationServiceTest
[INFO] Running com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
14:31:21.716 [main] WARN com.aimedical.modules.consultation.rule.DefaultTriageRuleEngine -- Failed to parse conditions JSON for rule, skipping: not valid json
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
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.219 s -- in com.aimedical.modules.consultation.DefaultTriageRuleEngineTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionManagerTest
14:31:21.764 [Thread-4] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
14:31:21.764 [Thread-10] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
14:31:21.765 [Thread-6] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
14:31:21.765 [Thread-8] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e, returning existing session
14:31:21.767 [Thread-13] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.767 [Thread-14] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.767 [Thread-20] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.767 [Thread-21] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.767 [Thread-15] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.767 [Thread-19] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.767 [Thread-18] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.768 [Thread-17] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.768 [Thread-16] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
14:31:21.779 [main] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 550e8400-e29b-41d4-a716-446655440000, returning existing session
14:31:21.784 [Thread-33] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.784 [Thread-37] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.784 [Thread-39] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.784 [Thread-36] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.784 [Thread-35] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.784 [Thread-34] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.784 [Thread-41] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.785 [Thread-40] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
14:31:21.785 [Thread-38] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in com.aimedical.modules.consultation.DialogueSessionManagerTest
[INFO] Running com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.consultation.DialogueSessionTest
[INFO] Running com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleEdgeCaseTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.065 s -- in com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleEdgeCaseTest
[INFO] Running com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.041 s -- in com.aimedical.modules.consultation.ObjectMapperJavaTimeModuleTest
[INFO] Running com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.aimedical.modules.consultation.RegistrationEventListenerTest
[INFO] Running com.aimedical.modules.consultation.SchedulingRetryConfigTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in com.aimedical.modules.consultation.SchedulingRetryConfigTest
[INFO] Running com.aimedical.modules.consultation.StaticDepartmentFallbackProviderTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.aimedical.modules.consultation.StaticDepartmentFallbackProviderTest
[INFO] Running com.aimedical.modules.consultation.TriageControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.consultation.TriageControllerTest
[INFO] Running com.aimedical.modules.consultation.TriageConverterTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.consultation.TriageConverterTest
[INFO] Running com.aimedical.modules.consultation.TriageServiceImplTest
14:31:22.082 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.TimeoutException null
14:31:22.087 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.InterruptedException null
14:31:22.120 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
14:31:22.120 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
14:31:22.229 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
14:31:27.290 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
[INFO] Tests run: 55, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.238 s -- in com.aimedical.modules.consultation.TriageServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 186, Failures: 0, Errors: 0, Skipped: 0
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
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.571 s -- in com.aimedical.modules.prescription.api.PrescriptionAssistControllerTest
[INFO] Running com.aimedical.modules.prescription.api.PrescriptionAuditControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.104 s -- in com.aimedical.modules.prescription.api.PrescriptionAuditControllerTest
[INFO] Running com.aimedical.modules.prescription.cache.DrugDictCacheManagerTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.122 s -- in com.aimedical.modules.prescription.cache.DrugDictCacheManagerTest
[INFO] Running com.aimedical.modules.prescription.context.DosageAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.context.DosageAlertTest
[INFO] Running com.aimedical.modules.prescription.context.PrescriptionDraftContextTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.modules.prescription.context.PrescriptionDraftContextTest
[INFO] Running com.aimedical.modules.prescription.converter.AssistConverterTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.aimedical.modules.prescription.converter.AssistConverterTest
[INFO] Running com.aimedical.modules.prescription.converter.AuditConverterTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.aimedical.modules.prescription.converter.AuditConverterTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AiSuggestionResultTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.prescription.dto.assist.AiSuggestionResultTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AiSuggestionStatusTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.AiSuggestionStatusTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AllergyWarningItemTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.assist.AllergyWarningItemTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverityTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageAlertLevelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.DosageAlertLevelTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.assist.DosageAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageCheckRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.assist.DosageCheckRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DosageCheckResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.DosageCheckResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DoseWarningTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.assist.DoseWarningTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.DoseWarningTypeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.assist.DoseWarningTypeTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AlertSeverityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.AlertSeverityTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AllergyDetailTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.audit.AllergyDetailTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditAlertTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.AuditAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditIssueTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.audit.AuditIssueTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.AuditRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.AuditResponseTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.AuditResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.BlockResponseTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.audit.BlockResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.DrugInteractionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.dto.audit.DrugInteractionTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.PatientInfoTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.dto.audit.PatientInfoTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.PrescriptionItemTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.PrescriptionItemTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SubmitRequestTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.dto.audit.SubmitRequestTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SubmitResponseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.dto.audit.SubmitResponseTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.SuggestionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.dto.audit.SuggestionTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.WarnAlertTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.dto.audit.WarnAlertTest
[INFO] Running com.aimedical.modules.prescription.dto.audit.WarnResultTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.prescription.dto.audit.WarnResultTest
[INFO] Running com.aimedical.modules.prescription.entity.AuditRecordTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.entity.AuditRecordTest
[INFO] Running com.aimedical.modules.prescription.event.DrugDictChangeEventListenerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.prescription.event.DrugDictChangeEventListenerTest
[INFO] Running com.aimedical.modules.prescription.event.DrugDictChangeEventTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.prescription.event.DrugDictChangeEventTest
[INFO] Running com.aimedical.modules.prescription.PrescriptionErrorCodeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.prescription.PrescriptionErrorCodeTest
[INFO] Running com.aimedical.modules.prescription.PrescriptionPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.prescription.PrescriptionPlaceholderTest
[INFO] Running com.aimedical.modules.prescription.rule.AllergyCheckRuleTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.668 s -- in com.aimedical.modules.prescription.rule.AllergyCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.ContraindicationCheckRuleTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.090 s -- in com.aimedical.modules.prescription.rule.ContraindicationCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DefaultLocalRuleEngineTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.219 s -- in com.aimedical.modules.prescription.rule.DefaultLocalRuleEngineTest
[INFO] Running com.aimedical.modules.prescription.rule.DosageLimitRuleTest
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.092 s -- in com.aimedical.modules.prescription.rule.DosageLimitRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DrugInteractionRuleTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.rule.DrugInteractionRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.DuplicateCheckRuleTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.modules.prescription.rule.DuplicateCheckRuleTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugAllergyMappingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.rule.entity.DrugAllergyMappingTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugCompositionDictTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.rule.entity.DrugCompositionDictTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugContraindicationMappingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.rule.entity.DrugContraindicationMappingTest
[INFO] Running com.aimedical.modules.prescription.rule.entity.DrugInteractionPairTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.prescription.rule.entity.DrugInteractionPairTest
[INFO] Running com.aimedical.modules.prescription.rule.LocalRuleResultTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.rule.LocalRuleResultTest
[INFO] Running com.aimedical.modules.prescription.rule.SpecialPopulationDosageRuleTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.196 s -- in com.aimedical.modules.prescription.rule.SpecialPopulationDosageRuleTest
[INFO] Running com.aimedical.modules.prescription.service.assist.DedupTaskSchedulerTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.080 s -- in com.aimedical.modules.prescription.service.assist.DedupTaskSchedulerTest
[INFO] Running com.aimedical.modules.prescription.service.assist.DosageThresholdServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.prescription.service.assist.DosageThresholdServiceTest
[INFO] Running com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest
[INFO] Tests run: 35, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.862 s -- in com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest
[INFO] Running com.aimedical.modules.prescription.service.audit.AuditRiskLevelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 s -- in com.aimedical.modules.prescription.service.audit.AuditRiskLevelTest
[INFO] Running com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditEnforcerImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditEnforcerImplTest
[INFO] Running com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
14:31:36.365 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=AI_UNAVAILABLE
14:31:36.389 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=RX_AUDIT_AI_EXECUTION_ERROR
14:31:36.430 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
14:31:36.436 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=RX_AUDIT_AI_EXECUTION_ERROR
14:31:36.443 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
14:31:36.459 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
14:31:36.473 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
14:31:36.476 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- Failed to serialize audit issues
com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest$2: fail
	at com.fasterxml.jackson.databind.ObjectMapper.writeValueAsString(ObjectMapper.java:3962)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl.persistAuditRecord(PrescriptionAuditServiceImpl.java:430)
	at com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl.audit(PrescriptionAuditServiceImpl.java:144)
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
14:31:36.482 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR_FAIL
14:31:36.487 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=RX_AUDIT_AI_TIMEOUT
14:31:36.493 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
14:31:36.541 [main] WARN com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl -- AI service unavailable, switching to local rule engine. aiResult=ERR
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.474 s -- in com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImplTest
[INFO] Running com.aimedical.modules.prescription.task.DraftContextCleanupTaskTest
14:31:36.556 [main] INFO com.aimedical.modules.prescription.task.DraftContextCleanupTask -- Removed expired draft context: expired-key
14:31:36.560 [main] INFO com.aimedical.modules.prescription.task.DraftContextCleanupTask -- Removed expired draft context: key-1
14:31:36.566 [main] INFO com.aimedical.modules.prescription.task.DraftContextCleanupTask -- Removed expired draft context: key-1
14:31:36.567 [main] INFO com.aimedical.modules.prescription.task.DraftContextCleanupTask -- Removed expired draft context: key-1
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.aimedical.modules.prescription.task.DraftContextCleanupTaskTest
[INFO] Running com.aimedical.modules.prescription.task.SuggestionCleanupTaskTest
14:31:36.583 [main] INFO com.aimedical.modules.prescription.task.SuggestionCleanupTask -- Removed expired suggestion: expired-key
14:31:36.585 [main] INFO com.aimedical.modules.prescription.task.SuggestionCleanupTask -- Removed expired suggestion: key-1
14:31:36.589 [main] INFO com.aimedical.modules.prescription.task.SuggestionCleanupTask -- Removed expired suggestion: key-1
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.aimedical.modules.prescription.task.SuggestionCleanupTaskTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 309, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --------------------< com.aimedical:medical-record >--------------------
[INFO] Building medical-record 0.0.1-SNAPSHOT                           [14/16]
[INFO]   from modules\medical-record\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ medical-record ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\medical-record\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ medical-record ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\medical-record\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\medical-record\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ medical-record ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 21 source files with javac [debug release 17] to target\classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ medical-record ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\medical-record\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ medical-record ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 17 source files with javac [debug release 17] to target\test-classes
[INFO] ��������·���з�����һ�����������������������
  ��ע������δ�����а�� javac ���ܻ������ע������
  �������ٰ�����ָ����һ���������� (-processor)��
  ��ָ��������·�� (--processor-path, --processor-module-path)��
  ����ʽ��������ע���� (-proc:only, -proc:full)��
  ��ʹ�� -Xlint:-options ���ش���Ϣ��
  ��ʹ�� -proc:none ������ע������
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManagerTest.java: ĳЩ�����ļ�ʹ�û򸲸����ѹ�ʱ�� API��
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManagerTest.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:deprecation ���±��롣
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManagerTest.java: ĳЩ�����ļ�ʹ����δ�����򲻰�ȫ�Ĳ�����
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManagerTest.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:unchecked ���±��롣
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ medical-record ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.medicalrecord.api.MedicalRecordControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in com.aimedical.modules.medicalrecord.api.MedicalRecordControllerTest
[INFO] Running com.aimedical.modules.medicalrecord.converter.MedicalRecordContentConverterTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.396 s -- in com.aimedical.modules.medicalrecord.converter.MedicalRecordContentConverterTest
[INFO] Running com.aimedical.modules.medicalrecord.converter.MedicalRecordConverterTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in com.aimedical.modules.medicalrecord.converter.MedicalRecordConverterTest
[INFO] Running com.aimedical.modules.medicalrecord.detector.MissingFieldDetectorImplTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in com.aimedical.modules.medicalrecord.detector.MissingFieldDetectorImplTest
[INFO] Running com.aimedical.modules.medicalrecord.dto.FieldMissingHintTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.medicalrecord.dto.FieldMissingHintTest
[INFO] Running com.aimedical.modules.medicalrecord.dto.RecordGenerateRequestTest
14:31:40.846 [main] INFO org.hibernate.validator.internal.util.Version -- HV000001: Hibernate Validator 8.0.1.Final
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.533 s -- in com.aimedical.modules.medicalrecord.dto.RecordGenerateRequestTest
[INFO] Running com.aimedical.modules.medicalrecord.dto.RecordGenerateResponseTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.medicalrecord.dto.RecordGenerateResponseTest
[INFO] Running com.aimedical.modules.medicalrecord.entity.DeptTemplateConfigTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.medicalrecord.entity.DeptTemplateConfigTest
[INFO] Running com.aimedical.modules.medicalrecord.entity.MedicalRecordTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.medicalrecord.entity.MedicalRecordTest
[INFO] Running com.aimedical.modules.medicalrecord.enums.MedicalRecordErrorCodeTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.medicalrecord.enums.MedicalRecordErrorCodeTest
[INFO] Running com.aimedical.modules.medicalrecord.enums.MedicalRecordFieldTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.medicalrecord.enums.MedicalRecordFieldTest
[INFO] Running com.aimedical.modules.medicalrecord.event.TemplateConfigChangeEventTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.medicalrecord.event.TemplateConfigChangeEventTest
[INFO] Running com.aimedical.modules.medicalrecord.MedicalRecordPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in com.aimedical.modules.medicalrecord.MedicalRecordPlaceholderTest
[INFO] Running com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest
14:31:41.257 [main] WARN com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl -- VisitFacade failed for encounterId: E001, fallback to encounterId
java.util.concurrent.ExecutionException: java.lang.RuntimeException: ģ���쳣
	at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
	at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2096)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.resolveVisitId(MedicalRecordServiceImpl.java:134)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.generate(MedicalRecordServiceImpl.java:77)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest.shouldSetVisitIdFallbackWhenEncounterIdFallbackUsed(MedicalRecordServiceImplTest.java:250)
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
Caused by: java.lang.RuntimeException: ģ���쳣
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest$StubVisitFacade.findVisitIdByEncounterId(MedicalRecordServiceImplTest.java:287)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.lambda$resolveVisitId$0(MedicalRecordServiceImpl.java:133)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1760)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
14:31:41.265 [main] WARN com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl -- Optimistic lock conflict on medical record save
org.springframework.orm.ObjectOptimisticLockingFailureException: Object of class [com.aimedical.modules.medicalrecord.entity.MedicalRecord] with identifier [1]: optimistic locking failed
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest$StubMedicalRecordRepository.save(MedicalRecordServiceImplTest.java:367)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest$StubMedicalRecordRepository.save(MedicalRecordServiceImplTest.java:358)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.generate(MedicalRecordServiceImpl.java:113)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest.shouldHandleOptimisticLockException(MedicalRecordServiceImplTest.java:164)
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
14:31:41.268 [main] WARN com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl -- AI medical record generation execution error
java.util.concurrent.ExecutionException: java.lang.RuntimeException: timeout
	at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
	at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2096)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.callAiWithTimeout(MedicalRecordServiceImpl.java:148)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.generate(MedicalRecordServiceImpl.java:93)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest.shouldReturnDegradedWhenAiTimesOut(MedicalRecordServiceImplTest.java:122)
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
Caused by: java.lang.RuntimeException: timeout
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest.lambda$shouldReturnDegradedWhenAiTimesOut$0(MedicalRecordServiceImplTest.java:116)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1760)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
14:31:41.270 [main] WARN com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl -- VisitFacade failed for encounterId: E001, fallback to encounterId
java.util.concurrent.ExecutionException: java.lang.RuntimeException: ģ���쳣
	at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
	at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2096)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.resolveVisitId(MedicalRecordServiceImpl.java:134)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.generate(MedicalRecordServiceImpl.java:77)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest.shouldUseFallbackWhenVisitFacadeThrowsException(MedicalRecordServiceImplTest.java:106)
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
Caused by: java.lang.RuntimeException: ģ���쳣
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest$StubVisitFacade.findVisitIdByEncounterId(MedicalRecordServiceImplTest.java:287)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.lambda$resolveVisitId$0(MedicalRecordServiceImpl.java:133)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1760)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
14:31:41.274 [main] WARN com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl -- VisitFacade failed for encounterId: E001, fallback to encounterId
java.util.concurrent.ExecutionException: java.lang.RuntimeException: ģ�ⳬʱ
	at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
	at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2096)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.resolveVisitId(MedicalRecordServiceImpl.java:134)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.generate(MedicalRecordServiceImpl.java:77)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest.shouldUseFallbackWhenVisitFacadeTimesOut(MedicalRecordServiceImplTest.java:91)
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
Caused by: java.lang.RuntimeException: ģ�ⳬʱ
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest$StubVisitFacade.findVisitIdByEncounterId(MedicalRecordServiceImplTest.java:284)
	at com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImpl.lambda$resolveVisitId$0(MedicalRecordServiceImpl.java:133)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768)
	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1760)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.066 s -- in com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest
[INFO] Running com.aimedical.modules.medicalrecord.task.VisitIdReconciledTaskTest
14:31:41.283 [main] WARN com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Failed to reconcile visitId for record 1: VisitFacade error for ENC-FAIL
14:31:41.283 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 2: VISIT-OK
14:31:41.284 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 1: VISIT-001
14:31:41.285 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 1: VISIT-001
14:31:41.287 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 1: VISIT-RESOLVED
14:31:41.287 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 1: VISIT-001
14:31:41.289 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 2: VISIT-002
14:31:41.290 [main] INFO com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask -- Reconciled visitId for record 1: VISIT-001
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in com.aimedical.modules.medicalrecord.task.VisitIdReconciledTaskTest
[INFO] Running com.aimedical.modules.medicalrecord.template.DatabaseTemplateConfigManagerTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.096 s -- in com.aimedical.modules.medicalrecord.template.DatabaseTemplateConfigManagerTest
[INFO] Running com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfigTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfigTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ---------------------< com.aimedical:application >----------------------
[INFO] Building application 0.0.1-SNAPSHOT                              [15/16]
[INFO]   from application\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ application ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\application\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ application ---
[INFO] Copying 3 resources from src\main\resources to target\classes
[INFO] Copying 4 resources from src\main\resources to target\classes
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
[INFO] Compiling 4 source files with javac [debug release 17] to target\test-classes
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/application/src/test/java/com/aimedical/config/SecurityConfigPhase0Test.java: C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\src\test\java\com\aimedical\config\SecurityConfigPhase0Test.javaʹ����δ�����򲻰�ȫ�Ĳ�����
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/application/src/test/java/com/aimedical/config/SecurityConfigPhase0Test.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:unchecked ���±��롣
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ application ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.ApplicationMainTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-07-01T14:31:46.930+08:00  INFO 3312 --- [           main] com.aimedical.Application                : Starting Application using Java 21.0.11 with PID 3312 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\application)
2026-07-01T14:31:46.932+08:00  INFO 3312 --- [           main] com.aimedical.Application                : The following 1 profile is active: "test"
2026-07-01T14:31:48.166+08:00  INFO 3312 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-07-01T14:31:48.274+08:00  INFO 3312 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 100 ms. Found 9 JPA repository interfaces.
2026-07-01T14:31:49.979+08:00  INFO 3312 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-07-01T14:31:50.002+08:00  INFO 3312 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-07-01T14:31:50.002+08:00  INFO 3312 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.20]
2026-07-01T14:31:50.084+08:00  INFO 3312 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2026-07-01T14:31:50.084+08:00  INFO 3312 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 3075 ms
2026-07-01T14:31:50.512+08:00  INFO 3312 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-07-01T14:31:50.605+08:00  INFO 3312 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-07-01T14:31:50.654+08:00  INFO 3312 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-07-01T14:31:51.013+08:00  INFO 3312 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-07-01T14:31:51.056+08:00  INFO 3312 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-07-01T14:31:51.474+08:00  INFO 3312 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:aimedical_test user=SA
2026-07-01T14:31:51.478+08:00  INFO 3312 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-07-01T14:31:51.518+08:00  WARN 3312 --- [           main] org.hibernate.orm.deprecation            : HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-07-01T14:31:53.314+08:00  INFO 3312 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-07-01T14:31:53.430+08:00  INFO 3312 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-07-01T14:31:53.464+08:00  WARN 3312 --- [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'jwtTokenProvider' defined in file [C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\target\classes\com\aimedical\modules\commonmodule\auth\jwt\JwtTokenProvider.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'jwtConfig': Invocation of init method failed
2026-07-01T14:31:53.464+08:00  INFO 3312 --- [           main] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2026-07-01T14:31:53.482+08:00  INFO 3312 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2026-07-01T14:31:53.484+08:00  INFO 3312 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
2026-07-01T14:31:53.487+08:00  INFO 3312 --- [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2026-07-01T14:31:53.506+08:00  INFO 3312 --- [           main] .s.b.a.l.ConditionEvaluationReportLogger : 

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-07-01T14:31:53.534+08:00 ERROR 3312 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'jwtTokenProvider' defined in file [C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\target\classes\com\aimedical\modules\commonmodule\auth\jwt\JwtTokenProvider.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'jwtConfig': Invocation of init method failed
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:795)
	at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:237)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1355)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1192)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:962)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146)
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754)
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1343)
	at com.aimedical.Application.main(Application.java:14)
	at com.aimedical.ApplicationMainTest.shouldInvokeMainMethod(ApplicationMainTest.java:10)
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
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtConfig': Invocation of init method failed
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
	... 89 common frames omitted
Caused by: java.lang.IllegalStateException: JWT��Կδ���ã���ͨ���������� JWT_SECRET �������� jwt.secret �ṩ��Կ��
	at com.aimedical.modules.commonmodule.jwt.JwtConfig.validate(JwtConfig.java:52)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401)
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219)
	... 102 common frames omitted

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 7.846 s -- in com.aimedical.ApplicationMainTest
[INFO] Running com.aimedical.ApplicationPlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.ApplicationPlaceholderTest
[INFO] Running com.aimedical.config.SecurityConfigPhase0Test
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.962 s -- in com.aimedical.config.SecurityConfigPhase0Test
[INFO] Running com.aimedical.HealthControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in com.aimedical.HealthControllerTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] ---------------------< com.aimedical:integration >----------------------
[INFO] Building integration 0.0.1-SNAPSHOT                              [16/16]
[INFO]   from integration\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ integration ---
[INFO] Skipping JaCoCo execution because property jacoco.skip is set.
[INFO] argLine set to empty
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
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.191 s]
[INFO] common ............................................. SUCCESS [  7.623 s]
[INFO] Common Module Aggregator ........................... SUCCESS [  0.005 s]
[INFO] common-module-api .................................. SUCCESS [  1.361 s]
[INFO] common-module-impl ................................. SUCCESS [ 28.801 s]
[INFO] AI Module Aggregator ............................... SUCCESS [  0.003 s]
[INFO] ai-api ............................................. SUCCESS [  1.200 s]
[INFO] ai-impl ............................................ SUCCESS [  3.512 s]
[INFO] patient ............................................ SUCCESS [  1.211 s]
[INFO] doctor ............................................. SUCCESS [  1.312 s]
[INFO] admin .............................................. SUCCESS [  1.369 s]
[INFO] consultation ....................................... SUCCESS [  8.111 s]
[INFO] prescription ....................................... SUCCESS [  9.405 s]
[INFO] medical-record ..................................... SUCCESS [  4.621 s]
[INFO] application ........................................ SUCCESS [ 13.289 s]
[INFO] integration ........................................ SUCCESS [  0.741 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:23 min
[INFO] Finished at: 2026-07-01T14:31:55+08:00
[INFO] ------------------------------------------------------------------------
