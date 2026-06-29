# 详细设计（v12）

## 概述

合并修复 16 项剩余测试相关待办项（T3-T34），包括：文档路径修正、测试增强、测试重构、测试修正。所有变更均为独立文件修改，无交叉依赖。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `Harness/reviews/202606270204_fix_phase1B_code_review/review_v2_D.md` | MODIFY | T3: 修正审查范围中 3 条测试文件路径 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilterTest.java` | MODIFY | T4: 新增 principal=null / principal=非Long 测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/UserFacadeImplTest.java` | MODIFY | T5+T28: UserConverter 去 mock + 新增 DataAccessException 测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLoggerTest.java` | MODIFY | T7: 新增写入失败降级路径测试 |
| `common/src/test/java/com/aimedical/common/util/SimpleMessageInterpolatorTest.java` | MODIFY | T20: 新增 3 个插值回退路径测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounterTest.java` | MODIFY | T24: 并发断言改为精确验证 + T25: 锁相关已无残留 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/password/PasswordPolicyImplTest.java` | MODIFY | T26: 新增全4字符集边界测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/converter/UserConverterTest.java` | MODIFY | T27: 新增 sort=null + enabled=false 组合测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImplTest.java` | MODIFY | T29: 新增 principal 非 Long 测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | MODIFY | T30: filter 顺序改为非反射方式 |
| `integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | MODIFY | T31: 新增 Role.enabled NOT NULL 约束验证 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequestTest.java` | MODIFY | T32: 新增 oldPassword 1 字符最小长度边界 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | MODIFY | T33: 重命名 shouldNotFilterDeletedInJavaLayer |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/RoleTest.java` | MODIFY | T34: 新增 sort NOT NULL 约束验证 |

## 类型定义

当前任务仅修改已有测试文件，不引入新类型。以下列出各变更的测试方法签名及行为。

---

### T3 — 审查文档路径修正

**文件**：`Harness/reviews/202606270204_fix_phase1B_code_review/review_v2_D.md`

**变更位置**：第 5-7 行（审查范围段落）

**原文路径**（错误）：
```
AIMedical/backend/application/src/test/java/com/aimedical/...
```

**修正为**（3 条独立路径）：
```
AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java
AIMedical/backend/common/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java
AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java
```

---

### T4 — PasswordChangeCheckFilterTest 新增 principal 异常场景

**新增方法 1**：
```java
@Test
void shouldSkipWhenPrincipalIsNull() throws Exception
```
- 设置 authentication 但 principal=null
- 验证 `chain.doFilter` 被调用（跳过）
- **依据**：`auth.getPrincipal()` 返回 null 时 isWhitelisted 不会被触发，`auth.getPrincipal()` 用于日志，但 filter 逻辑仅在 check `auth == null || !auth.isAuthenticated()` 后检查 principal 的类型——实际上 `PasswordChangeCheckFilter` 不检查 principal 类型，null principal 走正常通过路径。

**新增方法 2**：
```java
@Test
void shouldSkipWhenPrincipalIsNotLong() throws Exception
```
- 设置 authentication 且 principal 为 String 类型（如 `"someString"`)
- 验证 `chain.doFilter` 被调用（跳过）

---

### T5 — UserFacadeImplTest 解 mock UserConverter

**变更**：
```java
// 旧：
private final UserConverter userConverter = mock(UserConverter.class);
// 新：
private final UserConverter userConverter = new UserConverter();
```

**删除**所有测试中的以下模式行：
```java
when(userConverter.toUserInfoResponse(user)).thenReturn(expectedResponse);
```

各测试保留实体 mock 不变。真实 `UserConverter` 调用 mock entity 的 getter 方法产生等价 `UserInfoResponse`。删除 `expectedResponse` 局部变量。

**影响范围**：以下 5 个测试方法中的 `expectedResponse` 变量定义和 mock 行需移除：
- `findById_whenUserExists_shouldReturnUserInfo`
- `findByUsername_whenUserExists_shouldReturnUserInfo`
- `findById_whenUserHasNoRoles_shouldReturnEmptyRole`
- `findById_whenAllRolesDisabled_shouldReturnEmptyRole`
- `findById_shouldMergePermissionsFromRolesAndPosts`

---

### T7 — LoggingSecurityAuditLoggerTest 新增写入失败降级路径测试

**新增方法**：
```java
@Test
void logAudit_shouldFallbackGracefullyOnWriteFailure()
```
- 构造一个 `SecurityAuditEvent` 对象
- 模拟 logback appender 抛出 `IOException`（通过自定义 `Appender` 实现，在 append 时抛 RuntimeException）
- 验证：
  - `logger.logAudit(event)` 不抛出异常
  - 日志记录 `log.warn("Audit log write failed: ...")` 被触发

**注意**：由于 `LoggingSecurityAuditLogger` 的 catch 块捕获 `Exception`，任何 appender 抛出的异常都会被降级。本测试通过注入会抛出异常的 appender 来验证降级路径。

---

### T20 — SimpleMessageInterpolatorTest 新增插值回退路径测试

**新增方法 1**：
```java
@Test
void shouldReturnTemplateWhenArgsNullWithPlaceholders()
```
- template 含编号占位符（如 `"订单{0}已过期"`），args=null
- 期望：返回模板原文（不抛 NPE）

**新增方法 2**：
```java
@Test
void shouldReturnTemplateWhenNoPlaceholdersWithNonEmptyArgs()
```
- template 无占位符（如 `"纯文本"`），args 非空（如 `["extra"]`）
- 期望：返回模板原文（静默忽略多余 args）

**新增方法 3**：
```java
@Test
void shouldHandleWhenMorePlaceholdersThanArgs()
```
- template 含多个占位符（如 `"{0}和{1}"`），args 数量少于占位符（如 `["only"]`）
- 期望：不抛异常，尽可能替换

---

### T24 — SlidingWindowCounterTest 并发断言精确化

**修改方法**：`shouldHandleConcurrentRequestsForSameKey`

```java
// 旧：
assertTrue(allowed.get() <= limit);
// 新：
assertEquals(limit, allowed.get());
```

**理由**：`ConcurrentHashMap.compute` 保证每个 key 的 Deque 访问原子性，并发下严格限流数量应精确等于 limit。

---

### T25 — SlidingWindowCounterTest 锁反射测试

**结论**：当前 `SlidingWindowCounterTest.java` 中已无锁反射测试（T13 代码修复已在之前轮次完成），无需修改。验证方式已通过 `tryAcquire` 返回值语义体现。

---

### T26 — PasswordPolicyImplTest 新增全字符集边界测试

**新增方法**：
```java
@Test
@DisplayName("包含4种字符类型时应返回 null")
void validate_whenAllFourCharTypes_shouldReturnNull()
```
- 密码包含大写、小写、数字、特殊字符4类，如 `"Abc1!xyz"`
- 期望：返回 null（校验通过）
- **边界属性**：刚好覆盖所有4种字符类型的最小长度样例

---

### T27 — UserConverterTest 新增 sort=null + enabled=false 组合测试

**新增方法**：
```java
@Test
void shouldHandleSortNullAndDisabledRole()
```
- 构造 Role：`setSort(null)`, `setEnabled(false)`, `setCode("disabled_role")`
- 用户含该角色
- 期望：response.role() 返回 `""`

---

### T28 — UserFacadeImplTest 新增 DataAccessException 测试

**新增方法**：
```java
@Test
@DisplayName("findById Repository 抛出 DataAccessException 时传播异常")
void findById_whenRepositoryThrowsDataAccessException_shouldPropagate()
```
- `when(userRepository.findById(any())).thenThrow(new DataAccessException("...") {});`
- 验证：`assertThrows(DataAccessException.class, () -> userFacade.findById(1L))`
- 注：`org.springframework.dao.DataAccessException` 为抽象类，需要匿名子类实例化

---

### T29 — CurrentUserImplTest 新增 principal 非 Long 测试

**新增方法**：
```java
@Test
void getUserId_whenPrincipalIsNotLong_shouldReturnNull()
```
- `when(authentication.getPrincipal()).thenReturn("not a long")`
- 期望：`currentUser.getUserId()` 返回 null

---

### T30 — SecurityConfigPhase1Test filter 顺序改为非反射方式

**修改方法**：`shouldRegisterFiltersInExpectedOrder`

**重构方案**：
- 移除 `Field filterOrdersField` / `Method getOrderMethod` / `Method putMethod` 反射调用
- 改用 `http.addFilterBefore(filter, class)` 和 `http.addFilterAfter(filter, class)` 的公开 API，与 `SecurityConfigPhase1.filterChain()` 中使用的调用模式一致
- 保持 `HttpSecurity` 构建和 filter 链获取方式不变，仅移除反射部分

**具体变更**：
```java
// 删除以下反射代码：
Field filterOrdersField = HttpSecurity.class.getDeclaredField("filterOrders");
filterOrdersField.setAccessible(true);
Object reg = filterOrdersField.get(http);
Method getOrderMethod = reg.getClass().getDeclaredMethod("getOrder", Class.class);
getOrderMethod.setAccessible(true);
Integer usernamePwdOrder = (Integer) getOrderMethod.invoke(reg, UsernamePasswordAuthenticationFilter.class);
Method putMethod = reg.getClass().getDeclaredMethod("put", Class.class, int.class);
putMethod.setAccessible(true);
putMethod.invoke(reg, JwtAuthenticationFilter.class, usernamePwdOrder - 1);

// 替换为：
http.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class);
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
http.addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class);
```

---

### T31 — EntityMappingIT 新增 Role.enabled NOT NULL 约束验证

**新增方法**：
```java
@Test
void role_shouldRejectNullEnabled()
```
- 构造 Role，不设置 enabled（保持 null）
- 期望：`assertThrows(org.hibernate.PropertyValueException.class, () -> { entityManager.persist(role); entityManager.flush(); })`

---

### T32 — PasswordChangeRequestTest 新增 oldPassword 1 字符最小长度边界

**新增方法**：
```java
@Test
@DisplayName("oldPassword 为 1 个字符时校验通过（仅 upper bound 128）")
void shouldPassWhenOldPasswordIsOneChar()
```
- `new PasswordChangeRequest("a", "newPass123")`
- 期望：validator.validate(request) 为空（通过校验）
- **依据**：`PasswordChangeRequest.oldPassword` 的约束为 `@NotBlank @Size(max = 128)`，无 `@Size(min = ...)`，因此 1 个字符是合法边界

---

### T33 — MenuServiceTest 重命名测试方法

**修改方法**：
```java
// 旧：
void shouldNotFilterDeletedInJavaLayer()
// 新：
void shouldReturnAllMenusIncludingDeletedFromRepository()
```

**位置**：第 394 行，`shouldNotFilterDeletedInJavaLayer` → 更具语义的名称

---

### T34 — RoleTest 新增 sort NOT NULL 约束验证

**新增方法**：
```java
@Test
void shouldHaveNonNullSort()
```
- 创建新 Role，不显式设置 sort
- 验证：`assertNotNull(role.getSort())`
- **依据**：`Role.sort` 字段有 `@Column(nullable = false)` 注解，默认值为 0

---

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| UserConverter 去 mock 后测试因 entity mock 不完整失败 | 验证已有 entity mock 覆盖了 UserConverter 使用的所有 getter |
| SecurityConfigPhase1Test 反射移除后构造 HttpSecurity 失败 | 使用与 SecurityConfigPhase1.filterChain() 相同的公开 API |
| SlidingWindowCounter 并发测试严格断言偶尔失败 | `ConcurrentHashMap.compute` 保证原子性；若偶现失败需调整线程同步机制 |

## 行为契约

- 所有变更限于指定文件内，不修改生产代码
- 每个测试文件仅修改本任务清单对应的测试方法
- 删除的 mock 行不更改测试的验证逻辑
- 所有现有测试保持通过（Passed）

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `PasswordChangeCheckFilter` 源码 | T4: 确认 filter 逻辑中 principal 类型处理方式 |
| `UserConverter.toUserInfoResponse()` | T5: 确认真实转换器对 mock entity 的行为 |
| `LoggingSecurityAuditLogger.logAudit()` | T7: catch (Exception) 降级逻辑 |
| `SimpleMessageInterpolator.interpolate()` | T20: args=null/空/不足的回退行为 |
| `SlidingWindowCounter.tryAcquire()` | T24: compute 原子性保证 |
| `PasswordPolicyImpl.validate()` | T26: 4字符类型分类逻辑 |
| `UserConverter.resolveRole()` | T27: sort null + enabled false 处理 |
| `CurrentUserImpl.getUserId()` | T29: instanceof Long 检查 |
| `SecurityConfigPhase1.filterChain()` | T30: 公开 filter 注册 API 参考 |
| `Role.enabled/sort` 字段注解 | T31/T34: `@Column(nullable = false)` |
| `PasswordChangeRequest` record 约束 | T32: `@Size(max=128)` 无 min |
