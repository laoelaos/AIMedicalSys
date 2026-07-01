# 验证报告（v9）

## 结果
PASSED

## 统计
- 通过：90
- 失败：0

## 测试执行日志
[INFO] Scanning for projects...
[INFO] ---------------------< com.aimedical:consultation >---------------------
[INFO] Building consultation 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ consultation ---
[INFO] argLine set to -javaagent:...jacoco.agent-0.8.12-runtime.jar
[INFO] --- compiler:3.11.0:compile (default-compile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] --- surefire:3.1.2:test (default-test) @ consultation ---
[INFO] Tests run: 22, Failures: 0 -- in ConsultationDtoTest
[INFO] Tests run: 6, Failures: 0 -- in ConsultationEntityTest
[INFO] Tests run: 1, Failures: 0 -- in ConsultationPlaceholderTest
[INFO] Tests run: 5, Failures: 0 -- in DeadLetterCompensationServiceTest
[INFO] Tests run: 7, Failures: 0 -- in DefaultTriageRuleEngineTest
[INFO] Tests run: 7, Failures: 0 -- in DialogueSessionManagerTest
[INFO] Tests run: 5, Failures: 0 -- in DialogueSessionTest
[INFO] Tests run: 5, Failures: 0 -- in RegistrationEventListenerTest
[INFO] Tests run: 3, Failures: 0 -- in SchedulingRetryConfigTest
[INFO] Tests run: 5, Failures: 0 -- in StaticDepartmentFallbackProviderTest
[INFO] Tests run: 2, Failures: 0 -- in TriageControllerTest
[INFO] Tests run: 7, Failures: 0 -- in TriageConverterTest
[INFO] Tests run: 15, Failures: 0 -- in TriageServiceImplTest
[INFO]
[INFO] Results:
[INFO] Tests run: 90, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.322 s
[INFO] Finished at: 2026-06-29T16:17:05+08:00

