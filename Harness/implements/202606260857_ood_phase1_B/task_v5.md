# 任务指令（v5）

## 动作
NEW

## 任务描述
实现 Stage 2 登录失败计数与账户锁定基础设施：新建 `LoginAttemptTracker` 工具类及对应单元测试，位于公共模块的 `auth.login` 子包下。

### 1. LoginAttemptTracker（新建）
- **路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTracker.java`
- **包**：`com.aimedical.modules.commonmodule.auth.login`
- **形态**：public class（工具类，非 Spring Bean；后续 AuthModuleConfig 可显式注册为 @Bean）
- **职责**：登录失败计数与锁定状态封装。防暴力破解三层防护中的第二层（失败计数）和第三层（账户锁定）。AuthServiceImpl.login() 步骤 3 与步骤 8 依赖此组件。
- **数据结构**：`ConcurrentHashMap<String, AttemptRecord>`，key = 维度标识（username 或 IP），value = `AttemptRecord{int failures, long firstFailureTime}`
- **阈值默认值**（OOD 4.1 第二层防护表格；定义为 `private static final` 常量以保证生产封装性）：
  - `USERNAME_THRESHOLD = 5`、`USERNAME_LOCK_DURATION_MS = 15 * 60 * 1000L`（15 分钟）
  - `IP_THRESHOLD = 20`、`IP_LOCK_DURATION_MS = 30 * 60 * 1000L`（30 分钟）
- **内部类型**：`AttemptRecord` 使用 Java 21 `record` 关键字（不可变快照，自动 equals/hashCode/toString）：`private static record AttemptRecord(int failures, long firstFailureTime) { }`
  - 选择理由：项目已使用 Java 21（OOD 9.1 已确认），record 形式简洁、语义明确（不可变）、无需手写构造器/equals/hashCode；与"compute 闭包内 new AttemptRecord(prev.failures+1, prev.firstFailureTime) 创建新快照"的不可变更新模式天然契合
- **构造器**：
  - `public LoginAttemptTracker()` — 无参构造器，委托到全参构造器并传入上述 4 个默认常量（保持生产 API 形状不变）
  - `LoginAttemptTracker(int usernameThreshold, long usernameLockDurationMs, int ipThreshold, long ipLockDurationMs)` — **package-private** 测试构造器，接受任意阈值和锁定时长，供 `LoginAttemptTrackerTest` 在同包内创建短窗口实例（如 100ms 窗口）以验证过期逻辑
  - 选择 package-private（非 public）的原因：测试构造器为测试便利而存在，对外公开 API 仅暴露无参构造器；同包测试类可见
- **公开方法**（与 OOD 4.1 接口契约一致）：
  | 方法签名 | 返回 | 说明 |
  |---------|------|------|
  | `recordUsernameFailure(String username)` | void | 递增用户名维度失败计数；首次失败时 `firstFailureTime = now` |
  | `recordIpFailure(String ip)` | void | 递增 IP 维度失败计数；首次失败时 `firstFailureTime = now` |
  | `isUsernameLocked(String username)` | boolean | 锁定条件：`failures >= USERNAME_THRESHOLD` 且 `now - firstFailureTime < USERNAME_LOCK_DURATION_MS`；惰性清除过期条目 |
  | `isIpLocked(String ip)` | boolean | 锁定条件：`failures >= IP_THRESHOLD` 且 `now - firstFailureTime < IP_LOCK_DURATION_MS`；惰性清除过期条目 |
  | `clearUsername(String username)` | void | 登录成功时清除该 username 计数（步骤 8） |
  | `clearIp(String ip)` | void | 登录成功时清除该 ip 计数（步骤 8） |
- **并发安全**：所有读写通过 `ConcurrentHashMap.compute()` 闭包完成原子操作；`recordFailure` 在闭包内创建新 `AttemptRecord`（不可变快照）；`isXxxLocked` 在闭包内同时完成"读取 + 过期检查 + 惰性清除（过期时返回 null 等同 remove）"
- **惰性清除**：`isXxxLocked()` 中检测 `now - firstFailureTime >= lockDurationMs` 时，在 `compute` 闭包内返回 null（`ConcurrentHashMap.compute` 收到 null 返回值会移除该 key）；不引入后台清理线程
- **未知 key 行为**：`isXxxLocked(unknownKey)` 通过 `map.get(key)` 直接读取（不在 compute 闭包内触发 entry 创建）；key 不存在时返回 false（与 ConcurrentHashMap 契约一致）
- **不抛异常**：LoginAttemptTracker 仅返回 boolean 状态；锁定消息生成（"30 分钟后重试" vs "15 分钟后重试"）由调用方 AuthServiceImpl 根据维度决定
- **key 为 null 时**：`ConcurrentHashMap.compute` 抛出 NullPointerException，自然传播（与 SlidingWindowCounter 行为一致）

### 2. 单元测试（新建）
- **路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java`
- **形态**：class（JUnit 5），不引入 Spring 上下文；测试与生产类同包（`com.aimedical.modules.commonmodule.auth.login`），可直接使用 package-private 测试构造器
- **覆盖维度**：
  | 测试方法 | 覆盖维度 | 验证点 |
  |---------|---------|--------|
  | `shouldReturnFalseForUnknownUsername` | 契约 - 未知 key | 从未失败过的 username，`isUsernameLocked` 返回 false（不在 map 中创建 entry） |
  | `shouldReturnFalseForUnknownIp` | 契约 - 未知 key | 从未失败过的 ip，`isIpLocked` 返回 false |
  | `shouldNotLockUsernameBelowThreshold` | 正常路径 | 4 次失败后 `isUsernameLocked=false` |
  | `shouldLockUsernameWhenThresholdReached` | 锁定触发 | 第 5 次失败后 `isUsernameLocked=true` |
  | `shouldRemainLockedAfterThreshold` | 锁定持续 | 第 6/7 次检查仍返回 true（持续锁定状态） |
  | `shouldClearUsernameAfterSuccess` | 清除逻辑 | `clearUsername` 后 `isUsernameLocked=false`，可重新计数 |
  | `shouldNotLockIpBelowThreshold` | IP 维度 | 19 次失败后 `isIpLocked=false` |
  | `shouldLockIpWhenThresholdReached` | IP 维度锁定 | 第 20 次失败后 `isIpLocked=true` |
  | `shouldClearIpAfterSuccess` | IP 维度清除 | `clearIp` 后 `isIpLocked=false` |
  | `shouldMaintainIndependentUsernames` | 状态交互 | 不同 username 计数独立 |
  | `shouldMaintainIndependentIps` | 状态交互 | 不同 ip 计数独立 |
  | `shouldUnlockAfterLockDurationExpiry` | 过期重置（短窗口） | 通过 package-private 测试构造器创建短窗口（如 100ms），等待窗口到期后 `isXxxLocked` 返回 false，验证惰性清除生效 |
  | `shouldThrowNpeWhenUsernameIsNull` | 错误路径 | `recordUsernameFailure(null)` 抛 NPE |
  | `shouldThrowNpeWhenIpIsNull` | 错误路径 | `recordIpFailure(null)` 抛 NPE |
  | `shouldHandleConcurrentRecordUsernameFailure` | 并发安全 | 多线程并发 `recordUsernameFailure` 同一 username，最终失败计数等于总调用次数 |
  | `shouldHandleConcurrentRecordIpFailure` | 并发安全 | 多线程并发 `recordIpFailure` 同一 ip，最终失败计数等于总调用次数 |
- **测试策略**：
  - **过期重置统一采用方案 A**（包内测试构造器 + Thread.sleep）：使用 package-private 构造器创建短窗口实例（如 `new LoginAttemptTracker(5, 100L, 20, 100L)`），调用 `recordUsernameFailure` 后 `Thread.sleep(150)`，再调用 `isUsernameLocked` 验证返回 false
  - **不使用方案 B**（protected static 常量）— 破坏封装性，与项目对 `private static final` 常量的统一约定不一致
  - **不使用方案 C**（反射修改常量）— 测试代码依赖实现细节，且 final 字段反射在 JDK 17+ 受限
  - **不使用方案 D**（实际等待 15+ 分钟）— CI 不可接受
  - **未引入 Clock 抽象**— 增加注入复杂度，对当前任务属于过度设计；短窗口测试构造器已足以验证过期逻辑
  - 未知 key 用例同时验证契约语义（`isLocked` 不创建 entry）和与 `recordFailure` 的对比
  - 并发测试使用 `CountDownLatch` + `CyclicBarrier` 确保线程同时启动，最终断言通过 `recordUsernameFailure` 总调用次数 = ConcurrentHashMap 中 entry 的 failures 值
  - 不 mock 并发工具，直接构造真实 LoginAttemptTracker 实例验证端到端行为

## 选择理由
- **依赖层次**：LoginAttemptTracker 与 R4 完成的 RateLimitGuard/SlidingWindowCounter 同属 Stage 2 底层工具类（无 Spring/Filters 依赖），适合在 Filter 集成之前完成。AuthServiceImpl.login() 步骤 3（锁定检查）和步骤 8（清除）将直接调用本组件，但 Stage 2 范围内只需完成组件本身，业务集成属于 Stage 3 任务 3.6
- **风险等级低**：纯内存状态管理，OOD 已明确阈值、数据结构、并发策略；与 SlidingWindowCounter 同构（`ConcurrentHashMap.compute` 原子操作），可直接复用 R4 的测试模式
- **完成后续 Stage 2 任务的前置**：TokenBlacklist（2.9）和 GlobalRateLimitFilter（2.2）不依赖 LoginAttemptTracker；Filter 层（2.1/2.2/2.3）依赖 Spring Filter 编排，需更多集成工作。LoginAttemptTracker 是当前可独立交付的最小单元
- **不阻塞其他路径**：本任务与其他 Stage 2 子任务无编译期依赖关系，可独立编码、测试、验证
- **可测试性设计（方案 A）**：package-private 测试构造器在不破坏 `private static final` 默认值封装的前提下，为测试提供短窗口实例化能力；与项目惯例（其它工具类的无参构造器即对外 API）保持一致；唯一的额外 API 是一个 package-private 构造器，对生产代码无侵入

## 任务上下文
- **OOD 4.1 防暴力破解三层防护**：
  - 第一层（IP 速率限制）：R4 已完成 InMemoryRateLimitGuard（5次/10秒）+ SlidingWindowCounter
  - 第二层（本任务）：登录失败计数 — username 5次/15min、IP 20次/30min
  - 第三层（本任务）：账户锁定 — 触发条件由 LoginAttemptTracker 的 isXxxLocked 返回 true 判定
- **OOD 3.1.1 登录流程 步骤 3**：
  - 双重维度锁定状态检查：先查 IP 维度（连续 20 次 → 30 分钟），再查 username 维度（连续 5 次 → 15 分钟）
  - 任一维度命中 → 返回 ErrorCode.ACCOUNT_LOCKED（HTTP 429）
  - IP 维度锁定 → 消息"账户已锁定，请 30 分钟后重试"
  - username 维度锁定 → 消息"账户已锁定，请 15 分钟后重试"
  - 任一维度命中均不继续执行步骤 4-11
- **OOD 3.1.1 登录流程 步骤 5/6/7**：
  - 步骤 5（用户名不存在）：递增 IP 维度失败计数
  - 步骤 6（用户被禁用）：递增 username 和 IP 双维度失败计数
  - 步骤 7（密码错误）：递增 username 维度失败计数
- **OOD 3.1.1 登录流程 步骤 8**：
  - 登录成功 → LoginAttemptTracker 清除 username 维度失败计数 + 清除请求来源 IP 失败计数
  - 已知局限性：NAT/代理共享 IP 场景下，用户 A 成功登录将清除同一公网 IP 下所有用户的 IP 维度失败计数（OOD 11 节决策记录）
- **OOD 4.1 第二层表格**：
  | 维度 | 阈值 | 锁定时间 | 重置时机 |
  |------|------|---------|---------|
  | 同一用户名 | 连续 5 次失败 | 15 分钟 | 锁定到期/登录成功 |
  | 同一 IP | 连续 20 次失败 | 30 分钟 | 锁定到期/登录成功 |
- **OOD 4.1 "连续"语义定义**：同维度在指定时间窗口内的累计登录失败次数；首次失败时记录时间戳；窗口期内任意数量的失败均计入累计；重置条件：(a) 发起成功登录；(b) 窗口超时（惰性清除机制）
- **OOD 9.3 并发设计**：LoginAttemptTracker 使用 ConcurrentHashMap，单个用户名的失败计数更新通过 compute 方法原子性操作；锁定状态的检查和更新在同一个 compute 闭包中完成
- **OOD 10.1 错误码使用**：锁定触发时调用方（AuthServiceImpl）抛 BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, msg)，HTTP 429；消息根据维度动态生成

## 已有代码上下文
- **SlidingWindowCounter.java**：R4 完成，位于 `auth/rateLimit/`。提供滑动窗口限流算法参考实现（同样使用 ConcurrentHashMap.compute 原子操作）。LoginAttemptTracker 的并发模式与之一致，但语义不同：SlidingWindowCounter 维护时间戳队列（滑动窗口），LoginAttemptTracker 维护首次失败时间戳 + 失败计数（不可变 record）
- **InMemoryRateLimitGuard.java**：R4 完成，位于 `auth/rateLimit/`。登录端点专用限流（5次/10秒）。LoginAttemptTracker 是另一独立的防护层，不与 RateLimitGuard 共享状态或数据结构
- **GlobalErrorCode.java**：位于 `common/exception/`，已包含 `ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户已锁定，请{锁定时间}后重试")` 枚举值（消息模板由 AuthServiceImpl 在抛异常前替换 `{锁定时间}` 占位符）。LoginAttemptTracker 本身不引用此枚举，调用方按维度生成消息
- **包结构**：`auth/login/` 子包当前不存在，需新建目录。包名遵循 OOD 2.1 目录结构（`auth/login/LoginAttemptTracker.java`）。测试类与生产类同包，以便使用 package-private 测试构造器
- **项目技术栈**：Java 21 + Spring Boot 3.2.5 + Maven，测试框架为 JUnit 5（与 R4 InMemoryRateLimitGuardTest/SlidingWindowCounterTest 风格保持一致）。Java 21 `record` 已可用于不可变值对象

## RETRY 说明（仅 RETRY 时）
N/A（首轮 NEW）

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 1. 过期重置测试设计未收敛：测试策略小节同时列出"自定义测试构造器 / protected static 常量 / Thread.sleep 短窗口 / 反射修改"等多个并列备选，等于把生产类对测试的妥协方式推给实施阶段 | 收敛为方案 A：增加 package-private 测试构造器 `LoginAttemptTracker(int usernameThreshold, long usernameLockDurationMs, int ipThreshold, long ipLockDurationMs)`；无参构造器委托默认常量；测试通过短窗口实例（100ms）+ Thread.sleep 验证过期。已删除方案 B（破坏封装）/ 方案 C（依赖实现细节）/ 方案 D（CI 不可接受）的并列描述，仅保留对方案 A 的明确选择与不选择其他方案的理由 |
| [轻微] 2. AttemptRecord 形态未明确（"内部静态类或 record-like POJO"两种形式差异显著） | 二选一明确为 Java 21 `record AttemptRecord(int failures, long firstFailureTime)`：项目已使用 Java 21，record 形式简洁且与 compute 闭包内"创建新不可变快照"的更新模式天然契合；提供自动 equals/hashCode/toString |
| [轻微] 3. 缺少未知 key 的 isLocked 行为测试用例 | 新增 `shouldReturnFalseForUnknownUsername` 与 `shouldReturnFalseForUnknownIp` 两个用例，明确"从未失败过的 key 视为未锁定"的契约；同时在生产类设计说明中明确 `isXxxLocked(unknownKey)` 通过 `map.get(key)` 读取（避免在 compute 闭包内意外创建 entry） |
