# 代码审查报告（v8 r1）

## 审查结果
APPROVED

## 发现

无严重、无一般、无轻微问题。

### T13: SlidingWindowCounter
- `lock` 字段已移除，构造器中无初始化
- `import java.util.concurrent.locks.ReentrantLock` 已移除
- `tryAcquire` 无 `lock.lock()/unlock()` 包裹，直接依赖 `ConcurrentHashMap.compute` 原子性
- `cleanup()` 使用 `windows.forEach` + `remove(k, v)` 原子条件移除空队列
- 符合详细设计规格

### T15: LoginAttemptTracker record* 窗口过期防御
- `recordUsernameFailure` 内 `prev == null` → 新建、`prev != null && 窗口过期` → 重置、`prev != null && 窗口未过期` → 累加，使用 `usernameLockDurationMs`
- `recordIpFailure` 同理，使用 `ipLockDurationMs`
- `isUsernameLocked / isIpLocked` 保持不变（惰性清除）
- 符合详细设计规格

### T19: MenuController 注入 CurrentUser
- 新增 `private final CurrentUser currentUser` 字段
- 构造器签名 `MenuController(MenuService, CurrentUser)`
- `getCurrentUserId()` 使用 `currentUser.getUserId()`，null 时抛出 `IllegalStateException`
- `import org.springframework.security.core.Authentication` 和 `.context.SecurityContextHolder` 已移除
- `import com.aimedical.modules.commonmodule.auth.CurrentUser` 已新增
- 符合详细设计规格

### 测试文件
- `SlidingWindowCounterTest`：已移除 lock 反射测试用例及相关 import
- `MenuControllerTest`：新增 `@Mock CurrentUser`、构造器参数、import；未测试 `/tree` 端点
- 符合详细设计规格
