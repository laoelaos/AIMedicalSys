# 详细设计（v10）

## 概述

修复 Phase1B 报告中的 3 个 P2 问题：T12（JwtTokenProvider URL-safe Base64 校验）、T14（JwtUtil.generateToken 遗留 claims 清理并添加 jti）、T16（SimpleMessageInterpolator 数字占位符预检优化）。修改 3 个独立源文件及对应的测试文件，无交叉依赖。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/.../auth/jwt/JwtTokenProvider.java` | MODIFY | T12：正则改为 URL-safe 字符集，解码器换为 getUrlDecoder |
| `modules/common-module/common-module-impl/src/test/java/.../auth/jwt/JwtTokenProviderTest.java` | MODIFY | T12：测试数据移除 padding `=`，断言消息改为 "URL-safe" |
| `modules/common-module/common-module-impl/src/main/java/.../jwt/JwtUtil.java` | MODIFY | T14：generateToken 移除 role/position claims，添加 jti claim，添加 @Deprecated |
| `modules/common-module/common-module-impl/src/test/java/.../jwt/JwtUtilTest.java` | MODIFY | T14：移除 shouldGenerateTokenWithPosition 用例，更新断言 |
| `common/src/main/java/.../util/SimpleMessageInterpolator.java` | MODIFY | T16：增加数字占位符预检，跳过不必要的 MessageFormat 异常开销 |
| `common/src/test/java/.../util/SimpleMessageInterpolatorTest.java` | - | 无需变更 |

## 类型定义

### T12: JwtTokenProvider

**形态**：class（不变）
**包路径**：`com.aimedical.modules.commonmodule.auth.jwt`
**职责**：JWT 令牌的生成与验证（不变）

**public void init()** — 方法体内变更：
| 行 | 原 | 新 |
|----|----|----|
| 37 | `secret.matches("^[A-Za-z0-9+/]+=*$")` | `secret.matches("^[A-Za-z0-9_\\-]+$")` |
| 38 | `"JWT secret contains invalid Base64 characters"` | `"JWT secret contains invalid URL-safe Base64 characters"` |
| 42 | `Base64.getDecoder().decode(secret)` | `Base64.getUrlDecoder().decode(secret)` |

**import 变更**：无（已导入 `java.util.Base64`）

### T14: JwtUtil

**形态**：class（不变）
**包路径**：`com.aimedical.modules.commonmodule.jwt`
**职责**：JWT 工具类（不变）

**public String generateToken(Long userId, String username, String role, String position)** — 方法体变更：
- 移除 `claims.put("role", role);`
- 移除 `if (position != null) { claims.put("position", position); }`
- 添加 `claims.put("jti", UUID.randomUUID().toString());`
- 参数 `role` 和 `position` 保留不动（向后兼容）
- 方法体变更后签名不变；在方法上添加 `@Deprecated` 注解

**public String getRole(String token)** — 不变（仅作为 claims 读取器，无害）
**public String getPosition(String token)** — 不变（仅作为 claims 读取器，无害）

**import 变更**：
- 新增 `import java.util.UUID;`

### T16: SimpleMessageInterpolator

**形态**：class（不变）
**包路径**：`com.aimedical.common.util`
**职责**：消息模板插值（不变）

**public String interpolate(String template, Object[] args)** — 方法体变更：
```
if (args == null || args.length == 0) {
    return template;
}
// 预检：仅当模板包含数字占位符（如 {0}、{1,number}）时才尝试 MessageFormat
if (template.matches(".*\\{\\d+.*\\}.*")) {
    try {
        return MessageFormat.format(template, args);
    } catch (IllegalArgumentException e) {
        // MessageFormat 失败，回退到 replaceFirst 路径
    } catch (Exception e) {
        return template;
    }
}
// replaceFirst 路径（命名占位符或 MessageFormat 回退）
String result = template;
for (Object arg : args) {
    result = result.replaceFirst("\\{[^}]+\\}", String.valueOf(arg));
}
return result;
```

**import 变更**：无

## 测试文件变更

### JwtTokenProviderTest（T12）

| 用例 | 变更 |
|------|------|
| `shouldThrowWhenSecretContainsInvalidChars` | 测试数据由 `"test-secret-with-dash"` 改为 `"test-secret!!!"`；断言消息改为检查 `assertTrue(ex.getMessage().contains("URL-safe"))` |
| `TEST_SECRET` 常量 | 移除末尾 `=` padding，适配 `getUrlDecoder()` 的无 padding 解码能力 |
| `shouldThrowWhenDecodedKeyTooShort` | 测试数据由 `"dGVzdA=="` 改为 `"dGVzdA"`（URL-safe，无 padding） |
| `shouldInitSuccessfullyWithValidSecret` | 测试数据移除 padding `=` |
| 其他用例 | 不变 |

### JwtUtilTest（T14）

| 用例 | 操作 | 原因 |
|------|------|------|
| `shouldGenerateTokenWithPosition` (GenerateTokenTests) | **移除** | position claim 不再写入，该用例已验证的行为不复存在 |
| `shouldParseValidToken` (ParseTokenTests) | **修改断言** | 移除 `assertEquals("ADMIN", claims.get("role"))` — role claim 不再写入 generateToken；保留 userId 和 username 断言；新增 `assertNotNull(claims.get("jti"))` |
| `shouldGetRoleFromValidToken` (GetRoleTests) | **修改断言** | 新 token 的 `getRole()` 断言改为 `assertNull(role)`（role claim 不再写入） |

其他用例不变（所有 `generateToken(..., "ADMIN", null)` 的调用点不依赖返回值，仍可正常编译和执行）。

### SimpleMessageInterpolatorTest（T16）

无需变更。现有 6 个用例已覆盖所有路径：
- `shouldReturnTemplateWhenArgsNull` — args null → 直接返回
- `shouldReturnTemplateWhenArgsEmpty` — args empty → 直接返回
- `shouldReplaceNumberedPlaceholders` — 数字占位符 → 预检匹配 → MessageFormat
- `shouldReplaceNamedPlaceholdersByPosition` — 命名占位符 → 预检不匹配 → replaceFirst
- `shouldReuseSameArgForMultiplePlaceholders` — 数字占位符 → MessageFormat
- `shouldReturnTemplateForNoPlaceholdersWithArgs` — 无占位符 → 预检不匹配 → replaceFirst

## 错误处理

| 任务 | 错误场景 | 处理方式 |
|------|---------|---------|
| T12 | secret 含 URL-safe 非法字符 | 正则校验失败 → IllegalStateException，消息含 "URL-safe" |
| T12 | Base64 解码失败（正则通过但解码器抛异常） | 不变 — catch IllegalArgumentException → IllegalStateException |
| T12 | secret 解码后不足 32 字节 | 不变 — IllegalStateException |
| T14 | generateToken 传入 null role/position | 不变 — 参数保留但不再使用（role 直接忽略，position 条件移除） |
| T16 | MessageFormat.format 失败（数字占位符但参数不匹配） | 预检通过后进入 MessageFormat，失败则 replaceFirst 回退 |
| T16 | MessageFormat.format 抛其他异常 | 预检通过后进入 MessageFormat，其他异常则返回 template（catch Exception） |

## 行为契约

### T12
- 前置条件：`secret != null && !secret.isEmpty()`（不变）
- 正则只允许 URL-safe Base64 字符：`A-Z, a-z, 0-9, -, _`
- `Base64.getUrlDecoder()` 自动处理 padding（无需在正则中校验 `=`）
- 解码后 key 长度 >= 32 字节（不变）

### T14
- `generateToken()` 返回的 token 中：**不含** `role` 和 `position` claims，**含** `jti` claim（UUID 格式）
- `generateToken()` 标记 `@Deprecated`，新代码不应调用
- `getRole()` / `getPosition()` 对已有 token 的读取不受影响
- 其他方法（`parseToken`, `validateToken`, `getUserId` 等）完全不变

### T16
- 前置条件：不变（template 和 args 均可为 null）
- 当 template 不含 `{数字` 模式时，跳过 MessageFormat，直接走 replaceFirst 路径（减少异常开销）
- 当 template 含 `{数字` 模式时，行为与原来一致（先 MessageFormat，再 replaceFirst 回退）
- 对外行为完全不变（仅优化实现路径）

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `java.util.Base64` (JDK) | T12：getUrlDecoder |
| `java.util.UUID` (JDK) | T14：jti 生成（新增 import） |
| `java.text.MessageFormat` (JDK) | T16：数字占位符格式化 |
| `io.jsonwebtoken` (jjwt) | T12/T14：JWT 构建与解析 |
