# 测试报告（v2）

## 概述

为 T1（JwtTokenProvider 启动验证）和 T10（JwtConfig 解码后字节长度检查）变更编写/更新单元测试。

## 文件变更

### 1. JwtTokenProviderTest.java（新增 5 个测试方法）

路径：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java`

| 测试方法 | 场景 | secret | 验证 |
|---------|------|--------|------|
| `shouldThrowWhenSecretIsNull` | null | `null` | ISE, msg contains "JWT secret must be configured" |
| `shouldThrowWhenSecretIsEmpty` | 空字符串 | `""` | ISE, msg contains "JWT secret must be configured" |
| `shouldThrowWhenSecretContainsInvalidChars` | 非法 Base64 字符 | `"test-secret-with-dash"` | ISE, msg contains "invalid Base64 characters" |
| `shouldThrowWhenDecodedKeyTooShort` | 解码后 < 32 字节 | `"dGVzdA=="` | ISE, msg contains "at least 32 bytes" |
| `shouldInitSuccessfullyWithValidSecret` | 合法长 Base64 | `"AAA...A="`(32 字节) | `assertDoesNotThrow` |

原有 8 个测试方法（generate/validate/getUserId/getJti）保持不变。

### 2. JwtConfigTest.java（修改 2 个、新增 1 个测试方法）

路径：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtConfigTest.java`

| 操作 | 方法 | 说明 |
|------|------|------|
| 替换 | `shouldThrowWhenDecodedKeyTooShort` | 原 `shouldThrowExceptionWhenSecretLengthLessThan32`（字符串长度检查）→ 解码后字节长度检查 |
| 重命名 | `shouldPassWithValidLongSecret` | 原 `shouldNotThrowExceptionWhenSecretIsValid` |
| **新增** | `shouldThrowWhenSecretIsInvalidBase64` | 覆盖 `Base64.decode()` 异常包装路径 |

新增测试：

| 测试方法 | secret | 验证 |
|---------|--------|------|
| `shouldThrowWhenSecretIsInvalidBase64` | `"!!!invalid!!!base64"` | ISE, msg contains "not a valid Base64 string" |

原有 DefaultValueTests（3 个）、GetterSetterTests（4 个）、`shouldThrowExceptionWhenSecretIsNull`、`shouldThrowExceptionWhenSecretIsEmpty` 保持不变。

## 行为契约覆盖

### JwtConfig.validate()

| 契约 | 覆盖 |
|------|------|
| null → ISE | ✅ `shouldThrowExceptionWhenSecretIsNull` |
| empty → ISE | ✅ `shouldThrowExceptionWhenSecretIsEmpty` |
| decode 失败 → ISE 包装 | ✅ `shouldThrowWhenSecretIsInvalidBase64`（**补充**） |
| keyBytes < 32 → ISE | ✅ `shouldThrowWhenDecodedKeyTooShort` |
| 通过 → 静默返回 | ✅ `shouldPassWithValidLongSecret` |

### JwtTokenProvider.init()

| 契约 | 覆盖 |
|------|------|
| null/empty → ISE | ✅ `shouldThrowWhenSecretIsNull`, `shouldThrowWhenSecretIsEmpty` |
| 非法 Base64 字符 → ISE | ✅ `shouldThrowWhenSecretContainsInvalidChars` |
| decode 失败 → ISE 包装 | ✅（由正则拦截，实现中的 try-catch 为兜底，未单独设参因合法 Base64 字符串通过正则后必然可解码） |
| keyBytes < 32 → ISE | ✅ `shouldThrowWhenDecodedKeyTooShort` |
| 通过 → 密钥初始化 | ✅ `shouldInitSuccessfullyWithValidSecret` |

## 与设计偏差

无偏差。补充的 `shouldThrowWhenSecretIsInvalidBase64` 测试基于详细设计行为契约（错误处理表）中 JwtConfig decode 失败路径，原有设计测试表中遗漏，现补全。
