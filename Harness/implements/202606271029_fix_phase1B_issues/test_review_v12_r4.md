# 测试审查报告（v12 r4）

## 审查结果
APPROVED

## 发现

所有 15 项测试变更（T3/T4/T5/T7/T20/T24/T26/T27/T28/T29/T30/T31/T32/T33/T34）已逐项对照详细设计（detail_v12.md）验证实际代码：

- **T3** — `review_v2_D.md` 三条路径已正确修正 ✅
- **T4** — `PasswordChangeCheckFilterTest.java` principal=null 及 principal=非Long 测试已添加 ✅
- **T5** — `UserFacadeImplTest.java` UserConverter 已替换为真实实例，mock 行已清除 ✅
- **T7** — `LoggingSecurityAuditLoggerTest.java` 降级路径测试使用自定义抛出异常的 Appender 验证降级 ✅
- **T20** — `SimpleMessageInterpolatorTest.java` 三个回退路径测试已添加（args=null/多余args/占位符多于args）✅
- **T24** — `SlidingWindowCounterTest.java` 并发断言已改为 `assertEquals(limit, allowed.get())` ✅
- **T26** — `PasswordPolicyImplTest.java` 全4字符集边界测试 `"Abc1!xyz"` 已添加 ✅
- **T27** — `UserConverterTest.java` sort=null + enabled=false 组合测试已添加 ✅
- **T28** — `UserFacadeImplTest.java` DataAccessException 传播测试已添加（匿名子类实例化）✅
- **T29** — `CurrentUserImplTest.java` principal 非 Long 返回 null 测试已添加 ✅
- **T30** — `SecurityConfigPhase1Test.java` 反射代码已移除，改用 `addFilterBefore/addFilterAfter` 公开 API ✅
- **T31** — `EntityMappingIT.java` `setEnabled(null)` + `PropertyValueException` 断言，符合 `@Column(nullable = false)` 运行时行为 ✅
- **T32** — `PasswordChangeRequestTest.java` oldPassword 1字符边界测试已添加 ✅
- **T33** — `MenuServiceTest.java` 方法已重命名为 `shouldReturnAllMenusIncludingDeletedFromRepository` ✅
- **T34** — `RoleTest.java` sort NOT NULL 断言测试已添加 ✅

所有变更均严格遵循详细设计的行为契约，未发现导致测试无效、不可靠或覆盖不足的缺陷。
