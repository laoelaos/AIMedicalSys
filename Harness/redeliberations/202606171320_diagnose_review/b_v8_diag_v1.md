# 质量审查报告

## 审查信息

- **审查对象**：`a_v8_diag_v1.md`（第8轮问题诊断报告）
- **需求来源**：`requirement.md`
- **审查维度**：需求响应充分度、整体深度和完整性、可操作性（诊断建议可执行性、修复方案副作用、优先级合理性）
- **审查时间**：2026-06-17

---

## 总体评价

产出已完成8轮内部审议（诊断-质询循环），在技术准确性、证据链完整性、分类正确性、优先级标注方面已较为成熟。本审查从内部审议未充分覆盖的维度（需求响应充分度、可操作性、潜在副作用、系统完整性）出发，识别以下4项质量问题。

---

## 发现问题

### 问题1（可操作性—中）：问题9的"独立执行"断言与已知偏差K3存在隐蔽依赖

**所在位置**：「可执行修复顺序编排」(${a_v8_diag_v1.md}:595-616) 及问题9分析(${a_v8_diag_v1.md}:449-452)

**问题描述**：
报告将问题9（FallbackAiServiceTest 日志验证）标记为"完全独立，不依赖任何前置步骤，可随时启动"，但后续分析明确指出测试断言策略取决于 known_issues.md K3（日志触发时机偏差：首次调用 vs 启动期）的解决状态：
- 按当前代码行为编写断言（首次调用→ERROR），则测试与K3绑定，K3修复后测试失效
- 按设计语义编写断言（启动期→ERROR），则需先触发 `@PostConstruct` 启动期逻辑，不属于"零前置"操作

执行者按"独立执行"指引接手问题9后，将面临测试策略选择的决策困境：需先了解K3、评估对日志行为的影响、决定测试策略方向，才能开始编码。这与"完全独立"的描述矛盾，实际执行效率低于报告预期。

**改进建议**：
在修复顺序编排中将问题9的依赖标注改为"需由执行者先评估K3影响后选择测试策略"，或在问题9中明确指定一个默认测试策略（如"建议先按当前代码行为编写断言并显式标注`// ⚠️ 依赖 K3 已知偏差，K3 修复后需同步更新`注释，确保执行者无需等待K3决策即可启动"）。

---

### 问题2（可操作性—中）：问题7备选方案的操作前提未验证——application/pom.xml 无 maven-dependency-plugin 配置

**所在位置**：「问题7 修复方向」(${a_v8_diag_v1.md}:690-695)

**问题描述**：
报告推荐将 patient/doctor/admin 三条 `<ignoredUnusedDeclaredDependency>` 从父POM移入 `application/pom.xml`，隐含假设该文件已有对应插件配置可供扩展。经核实（`backend/application/pom.xml`），该文件**仅含 `spring-boot-maven-plugin`，不存在 `maven-dependency-plugin` 的任何配置**。

执行者按当前描述操作时，需要在 `application/pom.xml` 中从头编写完整的 `maven-dependency-plugin` 声明（groupId/artifactId/executions/configuration 等），而报告未提供任何配置骨架或参考。当前"移入"的表述暗示存在可修改的已有配置，与实际情况不符。

**改进建议**：
在修复方向中显式声明 `application/pom.xml` 当前无 `maven-dependency-plugin` 配置的事实，并提供在该文件中添加插件配置的完整示例（利用父POM pluginManagement 继承机制简化声明），例如：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
        <ignoredUnusedDeclaredDependencies>
            <ignoredUnusedDeclaredDependency>com.aimedical:patient</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:doctor</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:admin</ignoredUnusedDeclaredDependency>
        </ignoredUnusedDeclaredDependencies>
    </configuration>
</plugin>
```

---

### 问题3（可操作性—中）：问题8修复方向缺失测试覆盖建议——新增 handler 的验证未纳入修复范围

**所在位置**：「问题8 修复方向」(${a_v8_diag_v1.md}:356-378)

**问题描述**：
报告为问题8（GlobalExceptionHandler 缺少序列化异常处理器）提供了两个 `@ExceptionHandler` 方法的代码骨架，但完全未提及：
1. **测试覆盖**：现有 `GlobalExceptionHandlerTest.java` 已为 BusinessException、MethodArgumentNotValidException、Exception 三个 handler 编写专项测试。新增 handler 后应补充对应测试用例以维持测试覆盖率，但报告未提出测试补充要求。
2. **测试方法兼容性**：现有测试通过直接调用 handler 实例方法验证（如 `handler.handleBusinessException(ex)`），新增的 handler 可以采用相同的测试模式，但报告未提供参考。
3. **日志级别验证**：修复方向代码中使用 `log.warn`（HttpMessageNotReadableException）和 `log.error`（HttpMessageNotWritableException）的不同日志级别，但未建议对日志行为添加测试验证（而问题9中却专门强调日志验证的重要性）。

对于一个经分类为 P0(Phase 1+) 且被明确标记为"代码缺陷"的问题，缺失测试覆盖建议会削弱修复的完整度，修复者可能仅补充 handler 方法而跳过了测试环节。

**改进建议**：
在问题8的修复方向末尾补充"测试覆盖要求"，至少建议：
- 为 `handleMessageNotReadable` 编写测试：验证返回 400 及 PARAM_INVALID
- 为 `handleMessageNotWritable` 编写测试：验证返回 500 及 SYSTEM_ERROR
- 可复用现有测试模式（直接调用 handler 实例方法）
- 可在 Phase 1+ 激活条件触发时同步编写，无需 Phase 0 立即完成

---

### 问题4（完整性—低）：known_issues.md 变更建议分散在全文中，缺乏系统性汇总

**所在位置**：问题5「修复方向」(${a_v8_diag_v1.md}:221-222)、问题6「修复方向」(${a_v8_diag_v1.md}:269)、问题7「修复方向」(${a_v8_diag_v1.md}:695)、问题9「修复方向」(${a_v8_diag_v1.md}:449-452)、问题2/3 决策引导「偏离监控」(${a_v8_diag_v1.md}:663) 等

**问题描述**：
报告在至少5处提出了 known_issues.md 的变更建议（新增偏离记录或更新现有条目），但这些建议散布在各问题的正文中，没有汇总清单。执行者需要通读全文手动提取所有建议，遗漏风险高。

考虑到：
- known_issues.md 是团队维护技术债务的核心载体
- 报告中多个问题建议"若不修复应记入 known_issues.md"
- 多个问题的修复状态可能不同（即修一部分、记一部分）

缺少集中汇总清单会降低报告的可执行程度，执行者可能遗漏某些应该记录但未修复的偏离项。

**改进建议**：
在「修复者指引」中新增「known_issues.md 建议更新汇总」小节，以表格形式集中列出所有建议的新增/修改条目：

| 操作 | 条目 | 关联问题 | 内容摘要 | 触发条件 |
|------|------|---------|---------|---------|
| 新增 | K5 | 问题5 | dependencyManagement 未声明外部 starter（若不修复） | 选择保留现状时 |
| 新增 | K6 | 问题6 | Common POM 含未授权 validation starter（若不修复） | 选择保留现状时 |
| 新增 | K7 | 问题7 | maven-dependency-plugin 豁免范围过宽（若不修复） | Phase 0 暂不处理时 |
| 更新 | K3 | 问题9 | 补充测试策略与当前日志行为的已知偏差关联说明 | 测试编写时 |
| 新增 | K8 | 问题2/3 | 扁平目录布局偏离 OOD §2.1 及偏离监控阈值 | 决策后立即 |

---

## 需求响应充分度评价

产出已**完整覆盖** todo.md 全部 10 项待办事项（含 `[严重]` 声明的验证），并对 requirement.md 的四类判断标准（真实代码缺陷/误报/OOD文档问题/其他类型）做出了逐项归类。需求响应充分度**合格**。

---

## 优先级合理性评价

优先级体系（P0/P1/P2 + Phase 0/Phase 1+ 上下文切分）经7轮内部审议修正后已趋于合理。P0(Phase 1+)/P2(Phase 0)的双值格式有效缓解了"扫描阅读者忽略Phase上下文"的风险。"激活条件"字段的补充进一步增强了可操作性。**无显著问题**。
