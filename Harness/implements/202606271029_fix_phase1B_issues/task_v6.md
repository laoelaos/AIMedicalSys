# 任务指令（v6）

## 动作
NEW

## 任务描述
抽取 `MessageInterpolator` 组件，统一注入 GlobalExceptionHandler + RestAuthenticationEntryPoint + RestAccessDeniedHandler

对应报告：T17 — RestAuthenticationEntryPoint/RestAccessDeniedHandler 的 Result 未经过消息插值管线

## 选择理由
- P1 优先级（功能正确性受影响），不依赖其他未完成任务
- 后续 T20（插值回退测试增强）依赖此重构完成后实施

## 任务上下文

### 问题
- `RestAuthenticationEntryPoint.commence()`: `Result.fail(errorCode)` 直接将 `errorCode.getMessage()` 作为 message 字段输出，未经过 `formatMessage()` 插值
- `RestAccessDeniedHandler.handle()`: 同上
- OOD 10.3 节定义的插值管线仅覆盖 `GlobalExceptionHandler` 路径，未包含 AuthenticationEntryPoint/AccessDeniedHandler 出口

### 要求
从 `GlobalExceptionHandler.formatMessage()` 抽取消息插值逻辑为独立 `MessageInterpolator` 组件，统一注入三个出口。

## 已有代码上下文

### 模块依赖关系
- `common-module-impl` 依赖 `common`（common-module-impl/pom.xml:18-20）
- `common` 不依赖 `common-module-impl`
- 主应用扫描 `com.aimedical` 下所有包（`@SpringBootApplication(scanBasePackages = "com.aimedical")`）
- `common` 模块已有 `com.aimedical.common.util` 包

### GlobalExceptionHandler (common/src/.../config/GlobalExceptionHandler.java)
- `@ControllerAdvice` Spring 组件（会被自动扫描）
- 内联 `formatMessage()` 方法（第34-48行）：先 `MessageFormat.format(template, args)` 尝试，捕获 `IllegalArgumentException` 后降级 `replaceFirst` 处理命名占位符
- `handleBusinessException()` 调用 `formatMessage(errorCode.getMessage(), e.getArgs())` 获取插值后消息
- 当前测试文件：`GlobalExceptionHandlerTest.java`（271行，11个测试方法）

### RestAuthenticationEntryPoint (common-module-impl/.../auth/security/RestAuthenticationEntryPoint.java)
- `implements AuthenticationEntryPoint`
- 无参数构造器 `new ObjectMapper()`，`new RestAuthenticationEntryPoint()` 在 `SecurityConfigPhase1:68` 创建
- `commence()` 方法：`Result.fail(errorCode)` — 直接使用 errorCode.getMessage()
- 当前测试：`RestAuthenticationEntryPointTest.java`（3个测试，验证 ACCOUNT_DISABLED / UNAUTHORIZED）

### RestAccessDeniedHandler (common-module-impl/.../auth/security/RestAccessDeniedHandler.java)
- `implements AccessDeniedHandler`
- 无参数构造器 `new ObjectMapper()`，`new RestAccessDeniedHandler()` 在 `SecurityConfigPhase1:69` 创建
- `handle()` 方法：`Result.fail(errorCode)` — 直接使用 errorCode.getMessage()
- 当前测试：`RestAccessDeniedHandlerTest.java`（2个测试，验证 PASSWORD_CHANGE_REQUIRED / FORBIDDEN）

### SecurityConfigPhase1 (common-module-impl/.../auth/security/SecurityConfigPhase1.java)
- `@Configuration @EnableWebSecurity @Profile("phase1")`
- `filterChain()` 方法（第57-94行）内联创建 handler：`new RestAuthenticationEntryPoint()` 和 `new RestAccessDeniedHandler()`
- 其他 Bean 方法已有依赖注入模式（如 `globalRateLimitFilter(SlidingWindowCounter)`）

### 现有 errorCode 消息模板
- `ACCOUNT_LOCKED`: `"账户已锁定，请{锁定时间}后重试"` — 含命名占位符，需插值
- `UNAUTHORIZED`: `"未认证或令牌已失效"` — 无占位符
- `FORBIDDEN`: `"无权限访问"` — 无占位符
- `ACCOUNT_DISABLED`: `"账户已被管理员停用"` — 无占位符
- `PASSWORD_CHANGE_REQUIRED`: `"需要修改密码"` — 无占位符

## 实施指引

### 1. 创建 MessageInterpolator 接口
- 路径：`common/src/main/java/com/aimedical/common/util/MessageInterpolator.java`
- 包：`com.aimedical.common.util`
- 方法：`String interpolate(String template, Object[] args)`
- args 为 null 或空数组时直接返回 template（不修改）

### 2. 创建 SimpleMessageInterpolator 实现
- 路径：`common/src/main/java/com/aimedical/common/util/SimpleMessageInterpolator.java`
- 包：`com.aimedical.common.util`
- 注解：`@Component`
- 逻辑：复制现有 `GlobalExceptionHandler.formatMessage()` 实现
  ```java
  if (args == null || args.length == 0) return template;
  try { return MessageFormat.format(template, args); }
  catch (IllegalArgumentException e) {
      String result = template;
      for (Object arg : args) result = result.replaceFirst("\\{[^}]+\\}", String.valueOf(arg));
      return result;
  } catch (Exception e) { return template; }
  ```

### 3. 修改 GlobalExceptionHandler
- 注入 `MessageInterpolator` via 构造器
- 删除内联 `formatMessage()` 方法
- `handleBusinessException()` 中调用 `messageInterpolator.interpolate(errorCode.getMessage(), e.getArgs())`

### 4. 修改 RestAuthenticationEntryPoint
- 构造器增加 `MessageInterpolator` 参数
- `commence()` 中在 `Result.fail(errorCode)` 前对 message 插值：`String message = messageInterpolator.interpolate(errorCode.getMessage(), null)`（当前无动态参数）
- 注意保留现有 `ObjectMapper` 逻辑

### 5. 修改 RestAccessDeniedHandler
- 同上

### 6. 修改 SecurityConfigPhase1
- `filterChain()` 方法增加 `MessageInterpolator messageInterpolator` 参数
- `new RestAuthenticationEntryPoint(messageInterpolator)` 替代 `new RestAuthenticationEntryPoint()`
- `new RestAccessDeniedHandler(messageInterpolator)` 替代 `new RestAccessDeniedHandler()`

### 7. 更新测试

#### GlobalExceptionHandlerTest
- 构造 `GlobalExceptionHandler` 时传入 mock 或 real `MessageInterpolator`
- 仍测试 `handleBusinessException` 的输出消息是否正确（可注入 `SimpleMessageInterpolator` 作为真实依赖，或 mock 插值行为）
- 原有 formatMessage 逻辑已移至 `SimpleMessageInterpolator`，需为 `SimpleMessageInterpolator` 新增独立测试

#### RestAuthenticationEntryPointTest
- 构造 `RestAuthenticationEntryPoint(messageInterpolator)` 传入 mock `MessageInterpolator`
- mock 插值行为验证返回的消息已被插值

#### RestAccessDeniedHandlerTest
- 同上

#### 新增 SimpleMessageInterpolatorTest
- 路径：`common/src/test/java/com/aimedical/common/util/SimpleMessageInterpolatorTest.java`
- 覆盖场景：
  - args=null 返回原模板
  - 空数组返回原模板
  - 命名占位符替换（如 `{锁定时间}` → `30分钟`）
  - 编号占位符替换（如 `{0}` `{1}`）
  - 占位符数量 > args 数量（未匹配占位符保留原文）
  - args=null 有占位符（回退行为，保持原模板）

## 行为契约（不变）
- `GlobalExceptionHandler.handleBusinessException` 输出消息不变（插值逻辑相同）
- `UNAUTHORIZED/FORBIDDEN/ACCOUNT_DISABLED/PASSWORD_CHANGE_REQUIRED` 消息不变（无动态参数）
- 所有现有测试行为不变（仅调整构造方式）
