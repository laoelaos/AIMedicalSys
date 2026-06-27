# 设计审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** 计划删除 `SecurityConfigPhase1.java` 中的 `import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;`，但 `jwtAuthenticationFilter()` 方法签名仍以 `TokenBlacklist tokenBlacklist` 参数引用该类型。删除该 import 将导致编译失败。`TokenBlacklist` import 必须保留。

## 修改要求（仅 REJECTED 时）

1. **[严重]** 设计第 32-35 行：`SecurityConfigPhase1.java` 中只能删除 `InMemoryTokenBlacklist` 的 import，**不得删除** `TokenBlacklist` 的 import，因为 `jwtAuthenticationFilter(JwtUtil, TokenBlacklist, UserRepository)` 方法签名依然依赖此类型。
