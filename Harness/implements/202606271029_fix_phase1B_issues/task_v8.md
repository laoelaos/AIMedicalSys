# 任务指令（v8）

## 动作
NEW

## 任务描述
**T13 + T15 + T19**: 修复 SlidingWindowCounter 锁粒度、LoginAttemptTracker 窗口过期防御、MenuController CurrentUser 注入 — 3 个源文件

### 预期文件路径
- **MODIFY**: `modules/common-module/common-module-impl/src/main/java/.../auth/rateLimit/SlidingWindowCounter.java`
- **MODIFY**: `modules/common-module/common-module-impl/src/main/java/.../auth/login/LoginAttemptTracker.java`
- **MODIFY**: `modules/common-module/common-module-impl/src/main/java/.../controller/MenuController.java`

### 行为

#### T13: SlidingWindowCounter 锁粒度调整
- 移除全局 `ReentrantLock lock` 字段
- `tryAcquire()` 中移除 `lock.lock()`/`lock.unlock()` 包裹，直接使用 `ConcurrentHashMap.compute` 自身原子性
- `cleanup()` 方法改为使用 `windows.replaceAll()` 或流式迭代 + 原子替换，不再依赖全局锁保护
  - 具体方案：`windows.forEach((k, v) -> { if (v.isEmpty()) windows.remove(k, v); })` 或等效原子操作
- 移除 `import java.util.concurrent.locks.ReentrantLock;`
- 保留 `cleanupExecutor` 定时清理逻辑不变

#### T15: LoginAttemptTracker record* 窗口过期防御
- `recordUsernameFailure()` 中，在 `compute` 闭包内检查：若 `prev != null` 且 `(now - prev.firstFailureTime()) >= usernameLockDurationMs`，则重置记录（从新窗口开始计数，`new AttemptRecord(1, now)`）而非继续累加
- `recordIpFailure()` 同理，使用 `ipLockDurationMs`
- `isUsernameLocked()` / `isIpLocked()` 行为不变（已有惰性清除）

#### T19: MenuController 注入 CurrentUser
- 移除 `import org.springframework.security.core.Authentication;`
- 移除 `import org.springframework.security.core.context.SecurityContextHolder;`
- 新增 `import com.aimedical.modules.commonmodule.auth.CurrentUser;`
- 构造器注入 `CurrentUser currentUser` 参数
- `getCurrentUserId()` 方法体改为：`return currentUser.getUserId();`
- 保留 `getCurrentUserId()` 方法签名（private，供内部使用），抛出 `IllegalStateException` 的行为由 `currentUser.getUserId()` 返回 null 时触发（现有实现返回 null，因此调用方需处理 null → 异常 转换）

## 选择理由
三任务均属 P1 优先级，修改 3 个独立文件，无交叉依赖，可在同轮次实施。R5（T17 MessageInterpolator）已通过验证，继续按计划推进。

## 任务上下文
### T13 依据（05_phase1B_report.md）
> **T13**: SlidingWindowCounter 使用全局锁而非"每个 IP 独立加锁"
> - 根源：OOD 4.1 节文本矛盾（第 433 行"每 IP 独立加锁" vs 第 444 行"ReentrantLock 保护原子性"）
> - 修复方向：每个 key 的 Deque 在 `ConcurrentHashMap.compute` 闭包内原子访问，`cleanup()` 同步调整

### T15 依据（05_phase1B_report.md）
> **T15**: LoginAttemptTracker record* 方法缺少窗口过期防御
> - 如果代码绕过 `isLocked()` 直接调用 `record*()`，失败计数会无限累积，导致永久锁定
> - 修复方向：在 record* 方法中增加窗口过期检查（惰性清除）

### T19 依据（05_phase1B_report.md）
> **T19**: MenuController 直接操作 SecurityContextHolder 而非使用 CurrentUser 接口
> - OOD 1.3 节定义了 CurrentUser 接口作为"消除 Controller 层对 SecurityContextHolder 的直接操作"
> - 修复方向：注入 CurrentUser 接口，替换 SecurityContextHolder 直接调用

## 已有代码上下文
详见上一轮：
- `SlidingWindowCounter.java` — 67 行，`ReentrantLock lock` 保护 `tryAcquire` 和 `cleanup`
- `LoginAttemptTracker.java` — 105 行，`recordUsernameFailure`/`recordIpFailure` 无窗口过期检查
- `MenuController.java` — 163 行，`getCurrentUserId()` 直接访问 `SecurityContextHolder`
- `CurrentUser.java`（API 接口）：`Long getUserId()` / `String getUsername()` / `UserType getUserType()`
- `CurrentUserImpl.java`（实现）：已注入 `@Component`，`getUserId()` 从 SecurityContext 获取

## 验证方式
项目根目录 `AIMedical/backend/` 运行：
1. `mvn compile -pl common,modules/common-module -am`
2. `mvn test -pl common,modules/common-module -am`
