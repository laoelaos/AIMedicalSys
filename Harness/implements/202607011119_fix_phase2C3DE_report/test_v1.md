# 测试报告（v1）

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 修正现有测试构造参数；修复因 supplyAsync 包装导致的异常类型变更断言；新增 3 个测试方法覆盖超时/中断/混合场景 |

## 设计偏差说明

无偏差。

## 新增测试用例

### `shouldSkipDepartmentOnDoctorFacadeTimeout`
- **契约**：DoctorFacade 调用超过 `doctorFacadeTimeout` 秒时跳过该科室
- **场景**：1 个科室，配置 100ms 延迟 + `doctorFacadeTimeout=0`
- **验证**：医生列表为空；WARN 日志包含 `TimeoutException`

### `shouldRestoreInterruptFlagOnDoctorFacadeInterrupt`
- **契约**：`InterruptedException` 被捕获后恢复中断标志
- **场景**：预中断当前线程后调用 `triage()`（AI 返回已完成 Future 不受影响，DoctorFacade Future 未完成则抛出 `InterruptedException`）
- **验证**：医生列表为空；`Thread.interrupted()` 返回 true

### `shouldSkipOnlyFailedDepartmentOnMixedResults`
- **契约**：失败/异常的科室不参与推荐，其他科室结果不受影响
- **场景**：2 个科室——dept-01 成功、dept-02 抛出 `RuntimeException`（被 supplyAsync 包装为 `ExecutionException`）
- **验证**：仅 dept-01 的医生出现在结果中；WARN 日志 1 条且包含 `ExecutionException`

## 现有测试修正

| 问题 | 修改内容 |
|------|---------|
| 构造函数新增 `doctorFacadeTimeout` 参数 | `setUp()` 补传 `10L`；`shouldFallbackOnTimeout` 补传 `10L` |
| `RuntimeException` 被 `supplyAsync` 包装为 `ExecutionException` | `shouldSkipDepartmentOnDoctorFacadeException`、`shouldReturnEmptyWhenAllDepartmentsThrow` 中断言改为 `ExecutionException` |
