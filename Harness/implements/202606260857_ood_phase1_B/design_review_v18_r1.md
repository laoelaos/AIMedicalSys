# 设计审查报告（v18 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** 设计遗漏 `JwtUtilTest.java` 的适配修改。移除 `JwtConfig.expiration` 字段及其 getter/setter 后，现有 `JwtUtilTest.java` 在 5 处调用 `setExpiration(86400L)`（第 28、256、268、280、292 行），编译会失败。同时第 237 行断言 `assertEquals(86400L, jwtUtil.getExpirationTime())` 将失败，因为 `getAccessTokenExpiration()` 默认值为 900L。修正方向：将文件规划补充 `JwtUtilTest.java`，把全部 `setExpiration(86400L)` 替换为 `setAccessTokenExpiration(900L)`，并将第 237 行断言值从 `86400L` 更新为 `900L`。

## 修改要求

1. **[严重]** 在文件规划中补充 `JwtUtilTest.java`，操作类型为"修改"。变更内容：(a) 将 `setUp()` 和四个 `InitTests` 子测试中的 `config.setExpiration(86400L)` 替换为 `config.setAccessTokenExpiration(900L)`；(b) 将 `GetExpirationTests.shouldGetExpiration()` 中的断言 `assertEquals(86400L, jwtUtil.getExpirationTime())` 更新为 `assertEquals(900L, jwtUtil.getExpirationTime())`。
