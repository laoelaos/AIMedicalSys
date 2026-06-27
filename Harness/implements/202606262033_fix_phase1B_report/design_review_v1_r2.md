# 设计审查报告（v1 r2）

## 审查结果
REJECTED

## 发现

### [严重] shouldCreateAllBeans() 删除操作导致编译错误

**问题描述**：
设计指示删除 `shouldCreateAllBeans()` 第 25-26 行：
```java
TokenBlacklist blacklist = config.tokenBlacklist();
assertNotNull(blacklist);
```

但第 29 行（实际源文件）仍引用变量 `blacklist`：
```java
JwtAuthenticationFilter j = config.jwtAuthenticationFilter(jwtUtil, blacklist, userRepository);
```

删除变量声明后，第 29 行的 `blacklist` 成为未定义符号，导致编译失败。

**期望修正**：
设计需明确处理第 29 行的编译依赖，至少提供以下任一解决方案：
1. 将 `blacklist` 替换为 `mock(TokenBlacklist.class)` 或 `new InMemoryTokenBlacklist()`
2. 或删除第 27-30 行整个 `jwtAuthenticationFilter` 的验证（因其已在 `shouldCreateJwtAuthenticationFilterWithDeps()` 中覆盖）

### [一般] shouldCreateJwtAuthenticationFilterWithDeps() 未明确 import 影响

**问题描述**：
设计指示将 `shouldCreateJwtAuthenticationFilterWithDeps()` 中的 `config.tokenBlacklist()` 替换为 `new InMemoryTokenBlacklist()`。虽然测试文件已有 `InMemoryTokenBlacklist` import，但设计未明确提及此依赖关系，也未确认 import 的存留。若后续重构中移除了该 import，将引入新的编译错误。

**期望修正**：
在设计的修改详情中明确补充：`SecurityConfigPhase1Test.java` 的 `InMemoryTokenBlacklist` import（第 3 行）因 `shouldCreateJwtAuthenticationFilterWithDeps` 和 `shouldCreateAllBeans` 仍使用 `InMemoryTokenBlacklist`，故需保留。
