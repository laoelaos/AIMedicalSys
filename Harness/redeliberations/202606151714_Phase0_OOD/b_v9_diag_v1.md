# 质量审查报告

## 审查概述

审查轮次：第 9 轮
审查对象：Phase 0 OOD 设计方案（v9）
审查视角：需求响应充分度、事实错误/逻辑矛盾、深度与完整性

经过 8 轮迭代修复，设计文档已非常成熟，未发现严重质量问题。以下为审查发现的若干问题：

---

## 问题清单

### 问题 1：CI 流水线使用非标准 Maven 属性名 `-Dskip.unit.tests=true`

- **位置**：第 10 节 CI 占位，第四阶段命令
- **严重程度**：一般
- **问题描述**：`-Dskip.unit.tests` 不是 Maven Surefire 或 Failsafe 的标准属性。Surefire 的标准跳过属性为 `-DskipTests` 或 `-Dmaven.test.skip=true`，Failsafe 的标准跳过属性为 `-DskipITs`。如果 POM 中没有为 Surefire 插件显式配置 `<skip>${skip.unit.tests}</skip>`，此属性将无效，第四阶段集成测试运行时单元测试不会被跳过（或更糟：`verify` 生命周期在运行集成测试前会执行 `test` 阶段，未跳过的单元测试可能与 Failsafe 集成测试的执行时序冲突）。当前设计未提供 POM 中 Surefire 的 `<skip>` 属性配置，该命令无法直接照搬使用。
- **改进建议**：方案一：在 `integration/pom.xml` 的 surefire 插件配置中添加 `<skip>${skip.unit.tests}</skip>` 并在 `properties` 中定义默认值。方案二：改用标准属性组合——第四阶段使用 `-DskipTests`（跳过全部测试）+ 通过 failsafe 插件的 `<includes>` 精确匹配 `*IT.java`；或更清晰地拆分为 `mvn verify -pl integration -DskipITs=false` 并配合 Surefire 的 `-DskipTests=true`。

---

### 问题 2：`UserRegisteredEvent extends ApplicationEvent` 使用过时模式

- **位置**：8.4 节事件驱动模式示例（第 822-825 行）
- **严重程度**：一般
- **问题描述**：示例中 `UserRegisteredEvent` 继承 `ApplicationEvent`。Spring Framework 4.2+（当前项目使用 Spring Boot 3，对应 Spring Framework 6）已支持任意 POJO 作为事件对象发布，无需继承 `ApplicationEvent`。继承该框架类导致领域事件与 Spring 框架代码产生不必要的编译期耦合。
- **改进建议**：去掉 `extends ApplicationEvent`，改为普通 POJO 类（保留 `userId` 字段和构造函数即可）。`ApplicationEventPublisher.publishEvent(Object)` 可发布任意 POJO 事件。

---

### 问题 3：`ScheduleRequest.dateRange` 类型为 `String` 且无格式约束

- **位置**：8.2 节 AI 能力 DTO 定义（第 753 行）
- **严重程度**：轻微
- **问题描述**：排班日期范围的字段定义为 `String dateRange`，未约定字符串的格式规范（如 `"2026-06-01,2026-06-30"`、ISO 8601 间隔 `"2026-06-01/2026-06-30"` 或自然语言描述）。不同开发者可能采用不同格式导致前后端接口不兼容。
- **改进建议**：拆分为两个 `LocalDate` 字段 `startDate` / `endDate`（更符合 Java 类型系统），或在 Javadoc 中明确 `String` 格式的精确约定。

---

### 问题 4：核心配置 `ai.mock.enabled` 未在配置文件示例中显式声明

- **位置**：3.4 节 Bean 装配策略（依赖此属性）、9.1 节应用配置示例（未包含此属性）
- **严重程度**：轻微
- **问题描述**：`ai.mock.enabled` 是控制整个 AiService Bean 装配方案的核心开关，但 9.1 节的 `application-dev.yml` 示例中未包含该配置项。虽然 `@ConditionalOnProperty(matchIfMissing = true)` 提供了安全默认值，但新开发者需要通过阅读 `MockAiService` 注解才能发现此属性的存在，增加了认知负担。
- **改进建议**：在 9.1 节 `application-dev.yml` 示例末尾添加一行 `ai.mock.enabled: true`，使 Phase 0 的默认行为在配置文件中显式化。

---

## 整体质量评价

该设计文档经过 8 轮迭代修正后质量较高：需求覆盖全面（7 个维度均已响应），逻辑自洽，技术决策合理且有充分理由支撑。以上 4 个问题中 2 个为「一般」严重程度、2 个为「轻微」，均不影响 Phase 0 的核心骨架可用性。文档已达到可直接指导编码实现的成熟度。
