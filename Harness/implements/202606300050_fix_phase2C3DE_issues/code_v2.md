# 实现报告（v2）

## 概述
按详细设计 v2 完成 9 项文件变更：TriageService 接口签名的 4 参→3 参变更、新建 TriageErrorCode 枚举、TriageServiceImpl selectDepartment 重写及错误码替换、TriageController/DeadLetterCompensationService/RegistrationEventListener 调用同步、三份测试文件的 stub 签名与测试方法对齐。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `consultation/service/TriageService.java` | selectDepartment 签名从 4 参改为 3 参，移除 overwrite 参数 |
| **新建** | `consultation/exception/TriageErrorCode.java` | 实现 ErrorCode 接口的枚举，定义 TRIAGE_SESSION_NOT_FOUND |
| 修改 | `consultation/service/impl/TriageServiceImpl.java` | selectDepartment 始终覆盖写入 finalDepartmentId/Name，改用 TriageErrorCode.TRIAGE_SESSION_NOT_FOUND；移除 GlobalErrorCode import |
| 修改 | `consultation/api/TriageController.java` | selectDepartment 调用从 4 参改为 3 参 |
| 修改 | `consultation/event/RegistrationEventListener.java` | 注入 TriageService，事件处理委托给 triageService.selectDepartment；@Retryable 范围限缩为 DataAccessException/TimeoutException |
| 修改 | `consultation/service/DeadLetterCompensationService.java` | selectDepartment 调用从 4 参改为 3 参 |
| 修改 | `test/.../TriageControllerTest.java` | StubTriageService.selectDepartment 签名从 4 参改为 3 参 |
| 修改 | `test/.../TriageServiceImplTest.java` | 3 个 selectDepartment 调用改为 3 参；移除 shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse 测试方法 |
| 修改 | `test/.../DeadLetterCompensationServiceTest.java` | StubTriageService.selectDepartment 签名改为 3 参，移除 lastOverwrite 字段；移除 shouldCallSelectDepartmentWithOverwriteFalse 测试方法 |

## 编译验证
执行 `mvn compile`，唯一编译错误为 `TriageConverter.java:52` 的预存问题（`TriageRequest.setCorrectedChiefComplaint` 符号找不到），该文件不在本次变更范围内，不影响本实现正确性。

## 设计偏差说明
无偏差，所有变更严格按详细设计 v2 实施。
