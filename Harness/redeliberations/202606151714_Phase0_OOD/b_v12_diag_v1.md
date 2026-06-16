# 质量审查报告 v1

## 审查范围

审查产出：`a_v12_copy_from_v11.md`（Phase 0 最小化骨架 — 架构级 OOD 设计方案）
需求文件：`requirement.md`
审查视角：实际落地编码指导、下游消费者接口充足性、异常场景与边界条件覆盖

---

## 发现的问题

### 1. [严重] CI 第四阶段使用非标准 Maven 属性 `-Dsurefire.skip=true`

**问题描述**：
第 10 节 CI 流水线的第四阶段命令为：
```
mvn verify -pl integration -Dsurefire.skip=true
```
注释称此参数「仅跳过 Surefire 单元测试，不影响 Failsafe 集成测试执行」。但 `-Dsurefire.skip=true` **不是** Maven Surefire 插件认识的标准属性名。Surefire 插件标准属性为 `-DskipTests`（对应参数 `skipTests`），`-Dsurefire.skip` 不会被 Surefire 插件解析，实际执行中该参数静默失效，所有测试（含 Surefire 单元测试）都会照常运行。这是一个事实错误。

此外，注释同时声称「`-DskipTests` 也会影响 Failsafe 故避免使用」，该说法正确，但当前采用的替代方案是错误的。对于 integration 模块而言，其预期不含单元测试（仅含 `*IT.java` 集成测试），Surefire 默认命名模式不会拾取 `*IT.java`，因此实际执行 `mvn verify -pl integration`（不加任何 `-D` 参数）即可正确达到「仅运行 Failsafe 集成测试」的效果。

**所在位置**：第 10 节「CI 占位」第四阶段命令及注释

**严重程度**：严重

**改进建议**：删除 `-Dsurefire.skip=true` 参数，改为：
```
mvn verify -pl integration
```
同时在注释中说明：integration 模块仅含 `*IT.java` 测试类，Surefire 不拾取、Failsafe 拾取，无需额外参数干预。或（如需显式保障）在 `integration/pom.xml` 的 Surefire 插件配置中添加 `<skipTests>true</skipTests>`，但当前无需参数的设计更简洁。

---

### 2. [一般] `DegradationContext` 缺省实例中 null 字段的风险未注明

**问题描述**：
3.4 节定义 `DegradationContext` 包含 `LocalDateTime lastFailureTime`、`Duration elapsedTime`、`String requestType` 三个引用类型字段。文档同时说明「Phase 0 中 FallbackAiService 构造默认 DegradationContext 实例（字段取零值或 null），为未来真实策略预留扩展点」。

但文档未注明：当 Phase 2+ 的真实降级策略（如 `TimeoutDegradationStrategy`）使用此 `DegradationContext` 进行判定时，若策略实现在未做空值防御的情况下直接调用 `lastFailureTime.isBefore(...)` 或 `elapsedTime.compareTo(...)` 等方法，将因 null 引用引发 `NullPointerException`。

当前实现中，由于 Phase 0 仅注册 `NoOpDegradationStrategy`（`shouldDegrade()` 始终返回 `false`，且不访问上下文参数），null 字段问题在 Phase 0 不会暴露，但设计文档未将这一风险传递给后续阶段的实现者。

**所在位置**：3.4 节「降级策略框架」— `DegradationContext` 定义段落

**严重程度**：一般

**改进建议**：在 `DegradationContext` 定义末尾补充风险说明：
- 标注 Phase 2+ 的策略实现者在 `shouldDegrade()` 中对引用类型字段做 `null` 防御性检查
- 或建议在 FallbackAiService 构造 `DegradationContext` 时使用 `Optional` 包装引用类型字段
- 或明确约定 Phase 0 的默认实例仅在 NoOpDegradationStrategy 上下文中是安全的

---

## 未发现问题的维度

- **需求响应充分度**：产出覆盖了 requirement.md 中全部 7 个设计维度的 21 个子项，无关键遗漏。
- **架构一致性**：模块解耦、依赖方向、接口隔离等设计原则在文档内保持一致，无逻辑矛盾。
- **编码指导性**：包含 POM 骨架、目录布局、包命名、配置示例、安全配置骨架等可直接编码的产出，整体满足 Phase 0 骨架启动目标。
- **接口契约完整性**：`Result<T>`、`PageQuery/PageResponse`、`ErrorCode` interface、`AiService` 13 方法、DTO 字段定义等足以支撑下游业务模块和前端独立开发。

---

DIAG_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\b_v12_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
