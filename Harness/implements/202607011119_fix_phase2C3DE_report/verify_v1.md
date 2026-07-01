# 验证报告（v1）

## 结果
PASSED

## 统计
- 通过：52
- 失败：0

## 测试执行日志

[INFO] Building consultation 0.0.1-SNAPSHOT
[INFO] --- surefire:3.1.2:test (default-test) @ consultation ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.consultation.TriageServiceImplTest
12:02:23.909 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.TimeoutException null
12:02:23.933 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.InterruptedException null
12:02:23.981 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
12:02:23.982 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 1ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
12:02:24.008 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
12:02:29.125 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.util.concurrent.ExecutionException java.lang.RuntimeException: DoctorFacade error
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.931 s -- in com.aimedical.modules.consultation.TriageServiceImplTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.924 s
[INFO] Finished at: 2026-07-01T12:02:29+08:00
