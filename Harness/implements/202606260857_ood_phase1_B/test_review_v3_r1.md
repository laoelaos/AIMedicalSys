# 测试审查报告（v3 r1）

## 审查结果
APPROVED

## 发现

- **[轻微]** test_v3.md — "测试执行结果"表格仅列出 `JwtUtilTest`（27 用例）和 `AuthServiceTest`（13 用例），未包含 `PermissionFunctionTest`、`PostTest`、`MenuServiceTest` 等本次变更涉及的测试文件。建议补充完整执行结果或说明执行范围。
- **[轻微]** test_v3.md — "测试文件变更"表格未列出 `PermissionFunctionTest`（`FunctionTest` 重命名）、`PostTest`（类型引用更新）、`MenuServiceTest`（import 更新）的变更记录，信息不完整。

代码验证结果：
- `JwtUtilTest.java:247-306` — 5 个 InitTests 正确覆盖设计契约 B-1~B-6（null/空/非法字符/解码后不足 32 字节 + 正常路径），异常类型和消息断言正确
- `PostTest.java:67-68` — `Set<PermissionFunction>` / `new PermissionFunction()` 引用正确
- `PermissionFunctionTest.java:15,22,29,36,43,52` — `PermissionFunction` 类型使用正确
- `AuthServiceTest.java:12,59` — import 和 `jwtUtil.init()` 调用正确
- `MenuServiceTest.java:7-8,36,42,48,62,123,132,165,173,199,211,228,233,244,278,288,298,328,360,388,396` — 全部 `PermissionFunction`/`PermissionFunctionRepository` 引用正确
- `EntityMappingIT.java:8,143,150,158,166,471,473,481,489` — `PermissionFunction` import 和使用正确

无严重或一般级别问题。
