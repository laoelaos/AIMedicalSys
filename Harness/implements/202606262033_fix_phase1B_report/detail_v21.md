# 详细设计（v21）

## 概述

为 `SecurityConfigPhase1Test.java` 新增一个测试方法，验证 `SecurityFilterChain` 中 `GlobalRateLimitFilter` → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 三个自定义 Filter 的注册顺序与 OOD 3.3 节规定一致。不修改现有 3 个测试方法。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | 修改（追加方法） | 新增 filter order 验证测试 |

## 类型定义

### 新增测试方法

**方法签名**：
```java
@Test
void shouldRegisterFiltersInExpectedOrder() throws Exception
```

**所属类**：`SecurityConfigPhase1Test`（已有，`com.aimedical.modules.commonmodule.auth.security` 包）

**职责**：创建 `HttpSecurity` 实例，调用 `config.filterChain()`，验证三个自定义 Filter 的相对顺序

### 构造 HttpSecurity

使用 `spring-security-test` 提供的 Spring Security 基础设施，手动构造 `HttpSecurity`：

```java
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

ObjectPostProcessor<Object> opp = new ObjectPostProcessor<>() {
    @Override
    public <O> O postProcess(O object) { return object; }
};
AuthenticationManagerBuilder amb = new AuthenticationManagerBuilder(opp);
HttpSecurity http = new HttpSecurity(opp, amb, new HashMap<>());
```

### Filter 实例创建

复用已有 bean 工厂方法：

| Filter | 创建方式 |
|--------|---------|
| `GlobalRateLimitFilter` | `config.globalRateLimitFilter(new SlidingWindowCounter())` |
| `JwtAuthenticationFilter` | `config.jwtAuthenticationFilter(jwtTokenProvider, mock(TokenBlacklist.class), userRepository)` |
| `PasswordChangeCheckFilter` | `config.passwordChangeCheckFilter()` |

> `SlidingWindowCounter` 可使用 `new SlidingWindowCounter()`（纯逻辑类，无外部依赖）

### 获取 Filter 列表

```java
SecurityFilterChain chain = config.filterChain(http, globalRateLimitFilter, jwtAuthenticationFilter, passwordChangeCheckFilter);
List<Filter> filters = chain.getFilters();
```

### 验证相对顺序

不依赖绝对索引（Spring Security 内置 Filter 干扰），仅提取三个目标 Filter 的索引比较相对位置：

```java
List<Class<?>> classes = filters.stream().map(Filter::getClass).toList();
int idxGlobal = classes.indexOf(GlobalRateLimitFilter.class);
int idxJwt   = classes.indexOf(JwtAuthenticationFilter.class);
int idxPwd   = classes.indexOf(PasswordChangeCheckFilter.class);

assertNotEquals(-1, idxGlobal, "GlobalRateLimitFilter must be registered");
assertNotEquals(-1, idxJwt, "JwtAuthenticationFilter must be registered");
assertNotEquals(-1, idxPwd, "PasswordChangeCheckFilter must be registered");
assertTrue(idxGlobal < idxJwt, "GlobalRateLimitFilter must precede JwtAuthenticationFilter");
assertTrue(idxJwt < idxPwd, "JwtAuthenticationFilter must precede PasswordChangeCheckFilter");
```

## 新增 import

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

## 行为契约

1. **前置条件**：`config` 字段已初始化（当前通过 `new SecurityConfigPhase1()` 在字段声明处初始化）
2. **无副作用**：不修改 `config` 状态，不修改已有测试方法
3. **不依赖 Spring 容器**：纯单元测试，无 `@SpringBootTest`、`@ExtendWith(SpringExtension.class)` 等注解

## 错误处理

- `config.filterChain()` 声明 `throws Exception`，测试方法签名同样 `throws Exception` 透传
- 断言失败时 JUnit 报告具体错误信息（使用 `assertTrue` 的消息参数）

## 依赖关系

- 依赖 `spring-security-test:6.2.4`（已在 pom.xml `test` scope 中）
- 依赖 `spring-security-web`（提供 `HttpSecurity`、`SecurityFilterChain` 等核心类）
- 无其他新增外部依赖
- 不导出公开接口，仅类内部测试使用

## 修订说明（v21 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 缺少 Filter 存在性断言：`indexOf` 在元素不存在时返回 -1，而 `-1 < 0` 为 true，导致缺失 Filter 时测试仍通过 | 在顺序断言之前，新增三条 `assertNotEquals(-1, idx*)` 断言，确保三个 Filter 均已注册；同时追加 `assertNotEquals` 导入 |
