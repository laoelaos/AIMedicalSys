# 测试审查报告（v2 r2）

## 审查结果
REJECTED

## 发现

- **[一般]** `consultation/src/test/java/.../TriageServiceImplTest.java:297` — 测试方法 `shouldSelectDepartmentWithOverwriteFalseWhenFinalIsNull` 的方法名保留旧语义。`overwrite` 参数已从 API 中移除，新实现始终覆盖写入 finalDepartmentId/Name，但方法名却暗示存在 `overwrite=false` 的条件覆盖逻辑，造成阅读歧义。test_v2.md 修订说明中记录"已采纳"将该方法重命名为 `shouldSelectDepartmentWhenFinalIsNull`，但实际代码未被更新，说明要么测试报告不准确、要么实现未完成。

其余测试代码均正确、完备，无其他问题：
- 所有 StubTriageService 的 selectDepartment 签名已改为 3 参 ✅
- TriageServiceImplTest 覆盖了正常路径、错误路径（BusinessException）、幂等性覆盖 ✅
- RegistrationEventListenerTest 覆盖了 finalDepartmentId==null 调用的路径、finalDepartmentId 已设置时的跳过、record 不存在、@Recover 死信写入及原因记录 ✅
- DeadLetterCompensationServiceTest 覆盖了正常补偿、失败重试计数、多事件、无事件跳过 ✅
- TriageControllerTest 覆盖了委托调用 ✅

## 修改要求（仅 REJECTED 时）

**`TriageServiceImplTest.java:297`** — 方法 `shouldSelectDepartmentWithOverwriteFalseWhenFinalIsNull` 重命名为 `shouldSelectDepartmentWhenFinalIsNull`，与 test_v2.md 修订说明中已采纳的修正一致。
