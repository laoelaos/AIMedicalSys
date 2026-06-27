# 计划审查报告（v18 r1）

## 审查结果
REJECTED

## 发现

### [一般] 计划关于 JwtConfig 属性绑定的描述与代码库事实不符

计划"上下文"部分声称：
> `JwtConfig 类（OOD 4.7）已使用 jwt.access-token-expiration / jwt.refresh-token-expiration 属性绑定，application.yml 需对齐属性名`

但实际代码库中 `JwtConfig.java`（`common-module-impl/.../jwt/JwtConfig.java:33`）仅有一个 `expiration` 字段（`private long expiration = 86400L`），**不包含** `accessTokenExpiration` 或 `refreshTokenExpiration` 字段。全库搜索亦未发现任何 Java 文件引用 `access-token-expiration` 或 `refresh-token-expiration` 属性名。

这是一个事实错误。按计划描述的变更执行后：
- `jwt.expiration` 从 yml 中移除 → `JwtConfig.expiration` 失去绑定，回退至默认值 86400L（当前 yml 配置值亦为 86400，功能上无差异但依赖巧合）
- `jwt.access-token-expiration` / `jwt.refresh-token-expiration` 写入 yml 但无任何 Java 类绑定 → 成为死配置
- `JwtUtil.generateToken()`（`JwtUtil.java:81`）仍通过 `jwtConfig.getExpiration()` 使用旧绑定路径

该错误会误导后续 Designer/Coder 以为 `JwtConfig` 已具备新属性无需处理，实际则需要补全或做合理说明。

### 修正方向

方案 A（推荐）：在 R18 任务中补充更新 `JwtConfig`，将单一 `expiration` 字段拆分为 `accessTokenExpiration` 和 `refreshTokenExpiration`，与 yml 新属性名对齐。

方案 B：若 `JwtConfig.expiration` 已不再被实际使用（`JwtUtil` 仍在使用），则修正上下文描述以反映真实状态，说明 yml 属性名对齐仅作为 OOD 文档一致性要求，当前运行期由 `JwtTokenProvider` 硬编码常量控制。
