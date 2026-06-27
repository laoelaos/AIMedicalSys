# 测试报告（v3）

## 概述

v3 为 RETRY 轮次，源码已在磁盘上正确（JwtConfig.validate() 错误消息已移除多余"为"字，与测试断言匹配）。本次仅补充 JwtTokenProvider.init() 行为契约中遗漏的 Base64 decode 异常路径测试。

## 文件变更

### 1. JwtTokenProviderTest.java（新增 1 个测试方法）

路径：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java`

| 操作 | 测试方法 | secret | 场景 | 验证 |
|------|---------|--------|------|------|
| **新增** | `shouldThrowWhenBase64DecodeFailsAfterRegexPass` | `"abcde"` | 输入通过 Base64 正则校验（`^[A-Za-z0-9+/]+=*$`），但 `Base64.getDecoder().decode()` 因长度非 4 的倍数抛出异常 | ISE, msg contains "not a valid Base64 string" |

其余 14 个测试方法（含原有 8 个 + v2 新增 5 个 + 本次新增 1 个）保持不变。

### 2. JwtConfigTest.java（无变更）

所有行为契约均已覆盖，无需修改。

## 行为契约覆盖

### JwtConfig.validate()

| 契约 | 覆盖 |
|------|------|
| null → ISE | ✅ `shouldThrowExceptionWhenSecretIsNull` |
| empty → ISE | ✅ `shouldThrowExceptionWhenSecretIsEmpty` |
| decode 失败 → ISE 包装 | ✅ `shouldThrowWhenSecretIsInvalidBase64` |
| keyBytes < 32 → ISE（含"至少32字节"） | ✅ `shouldThrowWhenDecodedKeyTooShort` |
| 通过 → 静默返回 | ✅ `shouldPassWithValidLongSecret` |

### JwtTokenProvider.init()

| 契约 | 覆盖 |
|------|------|
| null/empty → ISE | ✅ `shouldThrowWhenSecretIsNull`, `shouldThrowWhenSecretIsEmpty` |
| 非法 Base64 字符 → ISE | ✅ `shouldThrowWhenSecretContainsInvalidChars` |
| decode 失败 → ISE 包装 | ✅ **`shouldThrowWhenBase64DecodeFailsAfterRegexPass`（本次补充）** |
| keyBytes < 32 → ISE | ✅ `shouldThrowWhenDecodedKeyTooShort` |
| 通过 → 密钥初始化 | ✅ `shouldInitSuccessfullyWithValidSecret` |

## 与 v2 偏差修正

v2 测试报告声称"合法 Base64 字符串通过正则后必然可解码"，此论断不准确。正则 `^[A-Za-z0-9+/]+=*$` 允许非 4 倍数长度的输入（如`"abcde"`），而 `Base64.getDecoder().decode()` 要求输入长度为 4 的倍数。本次补充的测试覆盖该路径，确保 try-catch 兜底逻辑被验证。
