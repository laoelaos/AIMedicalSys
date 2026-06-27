# 计划审查报告（v14 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** 计划声称"变更仅两个文件各一行，无交叉依赖"，但 `findWithDetailsById` 方法同时被 `JwtAuthenticationFilter.java:85` 调用。修改该方法上的 `@EntityGraph`（新增 `posts.functions`）将导致 `JwtAuthenticationFilter` 在每次认证请求时也强制 JOIN `posts.functions`，造成不必要的性能开销。计划未提及此副作用，且"无交叉依赖"的声明与事实不符。

## 修改要求（仅 REJECTED 时）

1. **交叉依赖未识别**：`findWithDetailsById` 有多个调用方（`JwtAuthenticationFilter` + `MenuServiceImpl`），修改其 EntityGraph 会无差别影响所有调用者。
   - 修正方向：方案 A — 为 `MenuServiceImpl` 单独新增一个查询方法（如 `findWithDetailsForMenuById`），使用扩展后的 `@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})`，保持现有 `findWithDetailsById` 不变。方案 B — 在计划中明确标注此副作用的已知影响并说明接受理由（如数据量级很小），并修正"无交叉依赖"的表述。
