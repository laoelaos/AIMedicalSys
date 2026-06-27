# 设计审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

### **[一般] Issue 1: `cleanupExecutor` 字段设计模糊，测试构造器可能无法编译**

**问题**：设计的生产构造器中使用 `this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(...)` 明确暗示 `cleanupExecutor` 是实例字段。但测试构造器 `InMemoryTokenBlacklist(ConcurrentHashMap<String, Long> blacklist)` 被设计为"不启动 ScheduledExecutorService"，即不初始化该字段。

若 `cleanupExecutor` 被声明为 `private final`（与已有的 `SlidingWindowCounter` 模式一致，第 15 行 `private final ScheduledExecutorService cleanupExecutor`），Java 编译器要求所有构造器都必须对 `final` 字段赋值——测试构造器将产生**编译错误**。设计未提供任何对此矛盾的解决方案。

**为什么是问题**：直接按设计文字编码会导致测试构造器无法编译，阻碍实现正常推进。

**期望修正方向**：明确说明 `cleanupExecutor` 字段的处理方案。建议方案：
- **方案 A**：声明为非 final 字段（`private ScheduledExecutorService cleanupExecutor`），仅在公开构造器中赋值，测试构造器留 `null`
- **方案 B**：不在类中存储 `cleanupExecutor` 字段，仅在公开构造器中以局部变量创建 executor 并调度清理任务
- **方案 C**：引入一个 `boolean cleanupEnabled` 参数或分离生产/测试子类
并在设计的类签名和字段表中明确标注该字段的可见性、是否为 final、以及两个构造器各自的行为。

---

### **[轻微] Issue 2: `cleanup()` 可见性理由与现有代码不一致**

**问题**：第 93 行声明 `cleanup()` 为 `public` 的理由是"因 ScheduledExecutorService 在匿名闭包中引用 `this::cleanup`，需为 public 方法才能被定时线程调用"。但同项目已有的 `SlidingWindowCounter`（第 54 行）中 `cleanup()` 为 **`private`**，同样通过 `this::cleanup` 引用并成功编译运行，证明该理由不成立。

**为什么是问题**：错误的设计理由可能导致后续维护者对代码意图产生困惑，或在修复其他问题时误改可见性。

**期望修正方向**：将理由更正为"测试用例（4/5/11/12）需要直接调用 `cleanup()` 验证清理行为"；或者考虑使用 `package-private` 可见性（测试与生产同包）以进一步缩小 API 暴露面，仅暴露确实必要的公开接口。

---

### **[轻微] Issue 3: `cleanupExecutor` 字段未出现在"字段说明"表中**

**问题**：第 64 行的"字段说明"只列出了 `blacklist — private final ConcurrentHashMap<String, Long>`，但未列出构造器中显式赋值的 `cleanupExecutor`。如果 `cleanupExecutor` 按方案 A/B 设计成为字段，应补充到字段表中；如果按方案 B 设计为局部变量，应在设计中注明"不作为字段存储"以免歧义。

**为什么是问题**：字段表不完整，结合 Issue 1，增加了实现者误解的可能性。

**期望修正方向**：在字段表中补充 `cleanupExecutor` 的声明（含可见性、类型、final 状态），或明确说明其存储方式。

## 修改要求

1. **[一般]** 解决 `cleanupExecutor` 字段在双构造器场景下的初始化问题，避免编译错误。在字段表和构造器行为描述中明确该字段的处理方式。
2. **[轻微]** 更正 `cleanup()` 为 `public` 的理由；考虑使用 `package-private` 替代。
3. **[轻微]** 补充字段表，将 `cleanupExecutor` 纳入文档范围。
