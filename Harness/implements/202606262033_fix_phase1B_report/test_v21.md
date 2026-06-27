# 测试报告（v21）

## 测试文件

`modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java`

## 变更说明

### 新增 `shouldRegisterFiltersInExpectedOrder()`

| 项目 | 说明 |
|------|------|
| 方法签名 | `@Test void shouldRegisterFiltersInExpectedOrder() throws Exception` |
| 行号 | L56–L82 |
| 职责 | 验证 `GlobalRateLimitFilter` → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 三者的相对顺序 |

**测试逻辑**：

1. 手动构造 `HttpSecurity`（`ObjectPostProcessor` + `AuthenticationManagerBuilder`）
2. 通过 config 工厂方法创建三个 Filter 实例
3. 调用 `config.filterChain(http, ...)` 获取 `SecurityFilterChain`
4. 提取 Filter 列表，按 `Class` 查找索引
5. 断言三个 Filter 均已注册（`assertNotEquals(-1, idx*)`）
6. 断言相对顺序（`assertTrue(idxGlobal < idxJwt)`、`assertTrue(idxJwt < idxPwd)`）

**新增 import**：

```java
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.Filter;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

## 行为契约覆盖

| 契约 | 覆盖 |
|------|------|
| GlobalRateLimitFilter 必须注册 | ✅ `assertNotEquals(-1, idxGlobal)` |
| JwtAuthenticationFilter 必须注册 | ✅ `assertNotEquals(-1, idxJwt)` |
| PasswordChangeCheckFilter 必须注册 | ✅ `assertNotEquals(-1, idxPwd)` |
| GlobalRateLimitFilter 在 JwtAuthenticationFilter 之前 | ✅ `assertTrue(idxGlobal < idxJwt)` |
| JwtAuthenticationFilter 在 PasswordChangeCheckFilter 之前 | ✅ `assertTrue(idxJwt < idxPwd)` |
| 无副作用，不修改 config 状态 | ✅ 仅读取 `config.filterChain()` 返回的 chain |
| 不依赖 Spring 容器 | ✅ 纯单元测试，无 `@SpringBootTest` 注解 |

## 覆盖维度

| 维度 | 状态 |
|------|------|
| 正常路径 | ✓ 三个 Filter 按顺序注册 |
| 错误路径 | ✓ 任一 Filter 缺失时 `assertNotEquals(-1)` 失败 |
| 边界条件 | 基于相对索引而非绝对索引，兼容 Spring Security 内置 Filter 位置变化 |
| 状态交互 | 无状态修改 |

## 与设计偏差

| 设计规格 | 实际处理 | 原因 |
|---------|---------|------|
| `List<Class<?>>` | `List<Class<? extends Filter>>` | Java 类型推断，行为一致 |

## 修订说明

无（v21 首轮，无审查反馈）。
