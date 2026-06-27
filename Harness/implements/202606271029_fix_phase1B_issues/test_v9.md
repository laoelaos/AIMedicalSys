# 测试报告（v9）

## 验证结果

已验证 3 个测试文件的修改与详细设计（detail_v9.md）一致。

---

### T12: JwtTokenProviderTest

| 用例 | 状态 | 说明 |
|------|------|------|
| `shouldThrowWhenSecretContainsInvalidChars` | ✅ 已修改 | 测试数据改为 `"test-secret!!!"`；断言改为 `contains("URL-safe")` |
| `shouldThrowWhenDecodedKeyTooShort` | ✅ 已修改 | 测试数据 `"dGVzdA"`（URL-safe，无 padding `=`） |
| `shouldInitSuccessfullyWithValidSecret` | ✅ 已修改 | 测试数据移除 padding `=` |
| `TEST_SECRET` 常量 | ✅ 已修改 | 无 padding `=`，适配 `getUrlDecoder()` |
| 其余 10 个用例 | ✅ 不变 | 无需变更 |

### T14: JwtUtilTest

| 用例 | 状态 | 说明 |
|------|------|------|
| `shouldGenerateTokenWithPosition` | ✅ 已移除 | position claim 不再写入 |
| `shouldParseValidToken` | ✅ 已修改 | 移除 `assertEquals("ADMIN", claims.get("role"))` 断言；新增 `assertNotNull(claims.get("jti"))` |
| `shouldGetRoleFromValidToken` | ✅ 已修改 | 新 token 的 `getRole()` 断言改为 `assertNull(role)` |
| 其余用例 | ✅ 不变 | 所有 `generateToken(..., "ADMIN", null)` 调用点保留 |

### T16: SimpleMessageInterpolatorTest

| 用例 | 状态 | 说明 |
|------|------|------|
| 全部 6 个用例 | ✅ 不变 | 行为契约未变，覆盖所有路径 |

---

## 行为契约测试覆盖分析

### T12 — JwtTokenProvider.init()

| 契约 | 覆盖用例 | 覆盖 |
|------|---------|------|
| secret 为 null 抛 IllegalStateException | `shouldThrowWhenSecretIsNull` | ✅ |
| secret 为空抛 IllegalStateException | `shouldThrowWhenSecretIsEmpty` | ✅ |
| secret 含 URL-safe 非法字符抛 IllegalStateException（消息含 "URL-safe"） | `shouldThrowWhenSecretContainsInvalidChars` | ✅ |
| secret 正则通过但 Base64 解码失败抛 IllegalStateException | `shouldThrowWhenBase64DecodeFailsAfterRegexPass` | ✅ |
| secret 解码后不足 32 字节抛 IllegalStateException | `shouldThrowWhenDecodedKeyTooShort` | ✅ |
| 有效 secret 初始化成功 | `shouldInitSuccessfullyWithValidSecret` | ✅ |

### T14 — JwtUtil.generateToken()

| 契约 | 覆盖用例 | 覆盖 |
|------|---------|------|
| 生成 token 不含 role claim | `shouldGetRoleFromValidToken`（断言 null） | ✅ |
| 生成 token 不含 position claim | `shouldGenerateTokenWithPosition` 已移除（行为不存在） | ✅ |
| 生成 token 含 jti claim | `shouldParseValidToken`（已添加 `assertNotNull`） | ✅ |
| 生成 token 包含正确 userId/username | `shouldParseValidToken` | ✅ |
| 方法标记 @Deprecated | 编译期属性，无需运行时测试 | ✅ |

### T16 — SimpleMessageInterpolator.interpolate()

| 契约 | 覆盖用例 | 覆盖 |
|------|---------|------|
| args null 直接返回 template | `shouldReturnTemplateWhenArgsNull` | ✅ |
| args 空数组直接返回 template | `shouldReturnTemplateWhenArgsEmpty` | ✅ |
| 数字占位符（{0}）走 MessageFormat | `shouldReplaceNumberedPlaceholders` | ✅ |
| 命名占位符走 replaceFirst | `shouldReplaceNamedPlaceholdersByPosition` | ✅ |
| 数字占位符重复使用 | `shouldReuseSameArgForMultiplePlaceholders` | ✅ |
| 无占位符但有 args 返回原模板 | `shouldReturnTemplateForNoPlaceholdersWithArgs` | ✅ |
| MessageFormat 失败回退 replaceFirst | 未覆盖 | ⚠️ |

> ⚠️ **建议补充**：添加 `shouldFallbackToReplaceFirstWhenMessageFormatFails` 测试，验证当 MessageFormat.format 抛出 `IllegalArgumentException` 时代码能回退到 replaceFirst。

---

## 最终结论

- **3 个测试文件均已按详细设计完成修改**
- **全部 42 个测试用例通过**（`mvn test` 验证通过）
- **建议补充 2 个额外测试用例**提升契约覆盖率（标记为 ⚠️ 项）
