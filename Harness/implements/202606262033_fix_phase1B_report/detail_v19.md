# 详细设计（v19）

## 概述

在 `AuthServiceTest.java` 现有 `login_shouldThrowIpLocked()` 和 `login_shouldThrowUsernameLocked()` 两个方法末尾各追加一条 `ex.getArgs()` 断言，验证 `BusinessException` 携带正确的锁定时间参数。不新增测试方法。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | 两个方法末尾追加 args 断言 |

## 修改详情

### `login_shouldThrowIpLocked()` (当前 L122-139)

**位置**：在现有 audit 断言块后、方法结束花括号前插入。

**追加断言**：
```java
assertEquals("30分钟", ex.getArgs()[0]);
```

**插入后方法结构**：
1. Mock 设置（L124-125）— 不变
2. `assertThrows` 捕获 ex（L127-129）— 不变
3. `assertEquals(GlobalErrorCode.ACCOUNT_LOCKED, ex.getErrorCode())`（L129）— 不变
4. Audit 事件断言块（L131-138）— 不变
5. **新追加**：`assertEquals("30分钟", ex.getArgs()[0])`
6. 方法结束（L139）

### `login_shouldThrowUsernameLocked()` (当前 L141-159)

**位置**：在现有 audit 断言块后、方法结束花括号前插入。

**追加断言**：
```java
assertEquals("15分钟", ex.getArgs()[0]);
```

**插入后方法结构**：
1. Mock 设置（L143-145）— 不变
2. `assertThrows` 捕获 ex（L147-149）— 不变
3. `assertEquals(GlobalErrorCode.ACCOUNT_LOCKED, ex.getErrorCode())`（L149）— 不变
4. Audit 事件断言块（L151-158）— 不变
5. **新追加**：`assertEquals("15分钟", ex.getArgs()[0])`
6. 方法结束（L159）

## 行为契约

- `ex` 引用在 `assertThrows` 声明中捕获，在其作用域内（同一方法内）可达
- `ex.getArgs()` 返回 `Object[]`，索引 0 对应构造时的第一个 varargs 参数
- IP 锁定构造参数为 `"30分钟"`，用户名锁定构造参数为 `"15分钟"`
- 断言插入位置不影响前面任何 mock 设置和断言

## 依赖关系

- **被测对象**：`AuthServiceImpl.login(LoginRequest)` — 已由现有方法覆盖
- **断言目标**：`BusinessException.getArgs()` — BusinessException 的公开方法
- **无新增依赖**：不引入新的 mock 或 import
