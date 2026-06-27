# 测试审查报告（v9 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `JwtUtilTest.java` (T14) — `shouldParseValidToken` 缺少对新增 `jti` claim 的断言。详细设计明确要求 "generateToken() 返回的 token 中含 jti claim（UUID 格式）"，该契约未被测试覆盖。
- **[轻微]** — MessageFormat 异常回退 replaceFirst 路径（T16）缺少测试用例。该路径为原有行为保留，非本次变更引入的新缺陷，但可提升覆盖率。

## 修改要求（仅 REJECTED 时）

1. **JwtUtilTest.java — shouldParseValidToken 缺少 jti 断言**
   - 问题：`shouldParseValidToken` 在解析 token 后仅验证 userId 和 username，未验证 jti claim 存在。
   - 原因：jti 是 T14 修复中新增的 claim，是行为契约的一部分。未测试意味着新功能的可验证性缺失。
   - 修正方向：在 `shouldParseValidToken` 中添加 `assertNotNull(claims.get("jti"))`。
