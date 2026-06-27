# 验证报告（v21）

## 结果
PASSED

## 统计
- 通过：4
- 失败：0
- 错误：0
- 跳过：0

## 测试执行日志
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
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
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 44 source files with javac [debug release 17] to target\test-classes
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java: ĳЩ�����ļ�ʹ����δ�����򲻰�ȫ�Ĳ�����
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:unchecked ���±��롣
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
01:52:17.749 [main] INFO org.springframework.security.web.DefaultSecurityFilterChain -- Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@26bbe604, org.springframework.security.web.header.HeaderWriterFilter@fe34b86, org.springframework.web.filter.CorsFilter@3c98781a, com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter@3f736a16, com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter@4601203a, com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilter@53abfc07, org.springframework.security.web.session.SessionManagementFilter@2c8c16c0, org.springframework.security.web.access.ExceptionTranslationFilter@80bfa9d, org.springframework.security.web.access.intercept.AuthorizationFilter@47c40b56]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.402 s -- in com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  16.680 s
[INFO] Finished at: 2026-06-27T01:52:17+08:00
[INFO] ------------------------------------------------------------------------
