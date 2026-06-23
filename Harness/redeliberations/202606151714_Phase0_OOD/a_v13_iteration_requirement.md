根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **[严重] CI 第四阶段使用非标准 Maven 属性 `-Dsurefire.skip=true`**
   - **问题描述**：第 10 节 CI 流水线的第四阶段命令为 `mvn verify -pl integration -Dsurefire.skip=true`，注释称此参数「仅跳过 Surefire 单元测试，不影响 Failsafe 集成测试执行」。但 `-Dsurefire.skip=true` 不是 Maven Surefire 插件认识的标准属性名，Surefire 不会解析它，实际执行中该参数静默失效，所有测试（含单元测试）都会照常运行。这是一个事实错误。对于 integration 模块，其预期不含单元测试（仅含 `*IT.java` 集成测试），Surefire 默认命名模式不会拾取 `*IT.java`，直接执行 `mvn verify -pl integration` 即可正确达到仅运行集成测试的效果。
   - **所在位置**：第 10 节「CI 占位」第四阶段命令及注释
   - **改进建议**：删除 `-Dsurefire.skip=true`，改为 `mvn verify -pl integration`；同时更新注释说明：integration 模块仅含 `*IT.java` 测试类，Surefire 不拾取、Failsafe 拾取，无需额外参数干预。

2. **[一般] `DegradationContext` 缺省实例中 null 字段的风险未注明**
   - **问题描述**：3.4 节定义 `DegradationContext` 包含 `LocalDateTime lastFailureTime`、`Duration elapsedTime`、`String requestType` 三个引用类型字段。文档说明「Phase 0 中 FallbackAiService 构造默认 DegradationContext 实例（字段取零值或 null），为未来真实策略预留扩展点」。但文档未注明：当 Phase 2+ 的真实降级策略使用此上下文进行判定时，若策略实现在未做空值防御的情况下直接调用 `lastFailureTime.isBefore(...)` 或 `elapsedTime.compareTo(...)` 等方法，将因 null 引用引发 NPE。当前 Phase 0 仅注册 `NoOpDegradationStrategy`（不访问上下文参数），问题不会暴露，但设计文档未将这一风险传递给后续阶段的实现者。
   - **所在位置**：3.4 节「降级策略框架」— `DegradationContext` 定义段落
   - **改进建议**：在 `DegradationContext` 定义末尾补充风险说明：标注 Phase 2+ 的策略实现者在 `shouldDegrade()` 中对引用类型字段做 null 防御性检查；或建议在 FallbackAiService 构造 `DegradationContext` 时使用 `Optional` 包装引用类型字段；或明确约定 Phase 0 的默认实例仅在 NoOpDegradationStrategy 上下文中是安全的。

## 历史迭代回顾

- **已解决的问题**：第 1-11 轮中除 CI 测试属性问题外的大部分质量问题（中文方法名、权限模型归属、BaseEntity 字段定义、Bean 装配策略、SecurityConfig 骨架、DTO 类型同步、ErrorCode 架构、数据库驱动策略、前端 Monorepo 配置、Spring 扫描策略、跨模块调用规范等）已在修订说明（v2-v12）中标注解决。
- **持续存在的问题**：CI 第四阶段测试属性问题持续未解决（第 11 轮使用 `-DskipTests` 影响 Failsafe，本轮更改为非标准属性 `-Dsurefire.skip=true`，方案错误，需按改进建议修正）。
- **新发现的问题**：无原生新增。DegradationContext null 字段风险初诊于第 12 轮，本轮需一并修复。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v12_copy_from_v11.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
