# 任务指令（v10）

## 动作
NEW

## 任务描述
实现三个独立的 P2 编码修复，对应 Phase1B 报告的 T12、T14、T16：

**T12**: JwtTokenProvider.init() 改用 URL-safe Base64 字符集校验（正则 `^[A-Za-z0-9_\\-]+$`）和 `Base64.getUrlDecoder()` 解码

**T14**: JwtUtil.generateToken() 移除 role/position claims，添加 jti claim；方法标记 `@Deprecated`

**T16**: SimpleMessageInterpolator.interpolate() 预检数字占位符跳过不必要的 MessageFormat 异常开销

## 选择理由
R6 (T13+T15+T19) 已通过验证。R7 是下一组编码修复任务，三个文件独立无交叉依赖，适合同一轮次实施。

## 任务上下文

### T12 — JwtTokenProvider.init() 变更
- **文件**: `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java`
- **正则**: `secret.matches("^[A-Za-z0-9_\\-]+$")` 替换 `"^[A-Za-z0-9+/]+=*$"`
- **错误消息**: `"JWT secret contains invalid URL-safe Base64 characters"` 替换原消息
- **解码器**: `Base64.getUrlDecoder().decode(secret)` 替换 `Base64.getDecoder().decode(secret)`
- **测试文件**: `JwtTokenProviderTest.java` — 测试数据移除 padding `=`，断言检查 `contains("URL-safe")`

### T14 — JwtUtil.generateToken() 变更
- **文件**: `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java`
- 移除 `claims.put("role", role);`
- 移除 `if (position != null) { claims.put("position", position); }`
- 添加 `claims.put("jti", UUID.randomUUID().toString());`
- 方法添加 `@Deprecated` 注解（参数保留不动向后兼容）
- 新增 import: `java.util.UUID`
- **测试文件**: `JwtUtilTest.java` — 移除 `shouldGenerateTokenWithPosition`，更新 `shouldParseValidToken` 和 `shouldGetRoleFromValidToken` 断言

### T16 — SimpleMessageInterpolator.interpolate() 变更
- **文件**: `common/src/main/java/com/aimedical/common/util/SimpleMessageInterpolator.java`
- 在现有逻辑前增加预检：当 template 不含数字占位符（`\{数字` 模式）时跳过 MessageFormat，直接走 replaceFirst 路径
- **测试文件**: `SimpleMessageInterpolatorTest.java` — 无需变更（现有 6 用例已覆盖所有路径）

## 已有代码上下文
- JwtTokenProvider 已有 `init()` 方法使用 `Base64.getDecoder()` 和 `[A-Za-z0-9+/]+=*$` 正则
- JwtUtil 已有 `generateToken(Long, String, String, String)` 方法写入 role/position claims
- SimpleMessageInterpolator 已有先 MessageFormat 再 replaceFirst 回退的逻辑
- 详细设计见 `detail_v9.md`
- 实现报告见 `code_v9.md`
- 测试报告见 `test_v9.md`
