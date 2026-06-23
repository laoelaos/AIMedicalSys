# 验证报告（v5）

## 结果
PASSED

## 统计
- 通过：7
- 失败：0

| 测试项 | 结果 | 说明 |
|--------|------|------|
| FallbackAiServiceTest (`mvn test -pl ai-impl -Dtest=FallbackAiServiceTest`) | ✅ PASSED | 7 tests run, 0 failures — 新增 `shouldLogErrorOnFirstCallThenWarnOnSubsequent` 测试全部通过 |

## 测试执行日志

### FallbackAiServiceTest (mvn test -pl ai-impl -Dtest=FallbackAiServiceTest)
[INFO] Scanning for projects...
[INFO]
[INFO] ---------------------< com.aimedical:ai-impl >---------------------
[INFO] Building ai-impl 0.0.1-SNAPSHOT
[INFO]   from pom.xml
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
[INFO] Running com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for bootstrap loader classes because bootstrap classpath has been appended
17:20:30.126 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
17:20:30.161 [main] ERROR com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
17:20:30.161 [main] WARN com.aimedical.modules.ai.impl.fallback.FallbackAiService -- No available AiService delegate
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.080 s -- in com.aimedical.modules.ai.impl.fallback.FallbackAiServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.962 s
[INFO] Finished at: 2026-06-17T17:20:30+08:00
[INFO] ------------------------------------------------------------------------
