# 计划审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

### [一般] 1. 过期重置测试设计未收敛，生产类需为可测试性做出妥协
计划"测试策略"小节同时并列了多个未明确选择的方案：
- "自定义测试构造器支持可注入阈值"
- "将 lockDurationMs 暴露为 protected/package-private 常量"
- "通过 Thread.sleep 在 5-10 秒范围内验证短窗口逻辑"
- "构造可配置 threshold 的测试版本"

生产类将 `USERNAME_LOCK_DURATION_MS = 15*60*1000L`、`IP_LOCK_DURATION_MS = 30*60*1000L` 设为 `private static final` 常量（与 OOD §4.1 阈值一致）。要让测试在 5-10 秒内验证过期逻辑（避免 CI 等待 15+ 分钟），必须修改生产类的可见性或 API 形状：

- 增加测试专用构造器 → 生产类 API 扩展
- 将常量改为 `protected static`（非 final）→ 破坏封装，final 字段无法被子类覆盖
- 测试使用反射（如 `ReflectionTestUtils.setField`）→ 测试代码依赖实现细节
- 实际等待 15 分钟 → CI 不可接受

每种方案都会反向影响生产类的最终形态或测试策略。计划同时否决了 Clock 抽象（"避免过度设计"），又保留多个并列备选，等于把"生产类对测试的妥协方式"这一关键设计决策推给实施阶段。应当明确指定一种并说明取舍。

### [轻微] 2. AttemptRecord 形态未明确
计划描述为"内部静态类（或 record-like POJO）"，两种形式在 Java 中差异显著。项目使用 Java 21，可直接使用 `record` 关键字（如 `private static final record AttemptRecord(int failures, long firstFailureTime) { }`），简洁且语义明确（不可变快照 + 自动 equals/hashCode/toString）。计划应二选一，避免实施阶段产生风格分歧。

### [轻微] 3. 缺少未知 key 的 isLocked 行为测试
计划未明确覆盖 `isUsernameLocked`/`isIpLocked` 对从未失败过的 key 返回 false 的显式用例。"4 次失败未锁定"用例间接覆盖（4 次调用在 map 中存在 entry）但语义不同。建议增加 `shouldReturnFalseForUnknownUsername`/`shouldReturnFalseForUnknownIp` 两个用例，明确"未失败过的 key 视为未锁定"的契约（与 ConcurrentHashMap `compute` 在 key 不存在时返回 null 的行为一致）。

## 修改要求

### 问题 1（过期重置测试设计）
**问题**：测试需要在 5-10 秒内验证过期逻辑，但生产类锁定时长为 15/30 分钟的 `private static final` 常量。
**修正方向**：从以下方案中明确选择一种并写入计划"测试策略"小节，删除其他并列备选：

- **方案 A（推荐）**：在 `LoginAttemptTracker` 中增加 package-private 测试构造器 `LoginAttemptTracker(int usernameThreshold, long usernameLockDurationMs, int ipThreshold, long ipLockDurationMs)`，无参构造器委托默认常量；测试中创建短窗口实例（如 100ms 窗口）以 Thread.sleep 验证过期
- **方案 B**：将 `USERNAME_LOCK_DURATION_MS`/`IP_LOCK_DURATION_MS` 改为 `protected static`（非 final），测试通过子类覆盖；接受封装性下降
- **方案 C**：测试使用反射（`ReflectionTestUtils.setField`）修改常量；接受测试代码对实现细节的依赖
- **方案 D**：实际等待 15+ 分钟；接受 CI 时间

推荐方案 A：在不破坏 `private static final` 默认值封装的前提下，通过包内可见构造器显式支持短窗口实例化，对生产代码无侵入（除新增一个 package-private 构造器外）。
