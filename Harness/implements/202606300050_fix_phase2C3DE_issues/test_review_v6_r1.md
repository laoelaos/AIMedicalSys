# 测试审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `TriageServiceImplTest.java` — 缺少 WARN 日志格式验证。设计文档（detail_v6.md:40-44）明确指定 WARN 日志格式 `"DoctorFacade call failed for department {} after {}ms: {} {}"` 为"固定字符串，不可更改"，但 `shouldSkipDepartmentOnDoctorFacadeException` 和 `shouldReturnEmptyWhenAllDepartmentsThrow` 仅验证了行为结果（跳过科室、返回空列表），未验证日志级别、消息格式及参数（departmentId、elapsedMs、异常类名、异常消息）。应补充日志捕获验证（如通过 LogCaptor 或自定义 Appender）。

- **[轻微]** `test_v6.md:10` — 文档矛盾：声称"新增 11 个"测试用例，但清单和源码中均仅有 10 个。

- **[轻微]** `test_v6.md:10` — 拼写错误："findDoorsForDepartments" 应为 "findDoctorsForDepartments"。

- **[轻微]** `TriageServiceImplTest.java:510-527` — `shouldLimitToFiveDoctorsAcrossDepartments` 仅断言 `size() == 5`，未验证实际保留了哪些医生（slot count 最低的 doc-6 应被排除）。增加内容断言可增强测试健壮性。

## 修改要求（仅 REJECTED 时）

1. **`TriageServiceImplTest.java`** — 在异常路径测试中补充 WARN 日志格式验证：
   - **问题**：设计文档明确指定 WARN 日志格式为不可变固定字符串，但无任何测试验证日志输出。
   - **为什么是问题**：生产环境中日志聚合系统依赖固定格式进行解析和告警；无测试覆盖则格式变更无法被捕获。
   - **期望修正方向**：引入日志捕获机制（如 `@ExtendWith` + `LogCaptor` 或 logback `ListAppender`），在 `shouldSkipDepartmentOnDoctorFacadeException` 或 `shouldReturnEmptyWhenAllDepartmentsThrow` 中验证 `log.warn()` 被调用、日志消息匹配 `"DoctorFacade call failed for department {} after {}ms: {} {}"` 模式且参数正确。
