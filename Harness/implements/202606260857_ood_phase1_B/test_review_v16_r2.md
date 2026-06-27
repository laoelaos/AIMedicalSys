# 测试审查报告（v16 r2）

## 审查结果
APPROVED

## 发现

无严重或一般问题。

- **[轻微]** `AuthServiceTest.java` — 部分行为契约中的异常路径未覆盖测试（如 `changePassword` 的 NOT_FOUND、`getCurrentUser` 的 UNAUTHORIZED/NOT_FOUND、`updateProfile` 的 UNAUTHORIZED/NOT_FOUND、`logout` 的 null token/claims 空返回）。这些路径在详细设计的测试用例表中也未被要求，属于设计层面的覆盖取舍，不影响当前测试的正确性和可靠性。测试实际还超出了设计表 2 个用例（`refreshToken_shouldThrowOnUsernameLocked`、`refreshToken_shouldThrowOnTokenVersionNotFound`），表明覆盖方向正确。
- **[轻微]** `code_v16.md` 中记载"19 tests run"与实际的 21 个用例不一致（test_v16.md 和源代码均为 21）。报告版本过时，不影响测试代码本身的质量。

## 修改要求（仅 REJECTED 时）
N/A
