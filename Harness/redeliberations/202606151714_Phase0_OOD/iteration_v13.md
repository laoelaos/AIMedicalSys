# 再审议判定报告（v13）

## 判定结果

RETRY

## 判定理由

诊断报告（b_v13_diag_v1.md）共发现 7 个问题：严重 1 个（FallbackAiService 自引用循环依赖导致 StackOverflow），一般 2 个（目录布局与包命名不一致、`ai.mock.enabled=false` 行为描述不准），轻微 4 个。质询报告（b_v13_challenge_v1.md）结果为 LOCATED，确认诊断有效。内部循环实际轮次（1）未达到最大轮次（12），问题已被确认定位。因存在严重和一般等级问题，不符合 PASS 条件，需重新运行组件 A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：FallbackAiService 通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 注入，因其本身标注 `@Primary` 且 implements AiService，导致 `ObjectProvider.getIfAvailable()` 返回自身实例，引发无限递归 StackOverflowError
- **所在位置**：Section 3.4「Bean 装配策略」
- **严重程度**：严重
- **改进建议**：方案 A：FallbackAiService 不标注 `@Primary`，业务模块通过 `@Qualifier("fallbackAiService")` 按名称注入；方案 B：内部通过 `ApplicationContext.getBeanNamesForType(AiService.class)` 排除自身后取首个可用实例；方案 C：引入 `AiServiceRegistry` 中间层

---

- **问题描述**：Section 2.1 目录树中 patient/doctor/admin 模块仅列出 api/、service/、repository/、entity/ 四个子目录，缺失 dto/ 和 converter/，与 Section 2.3 包命名规范不一致
- **所在位置**：Section 2.1 vs Section 2.3
- **严重程度**：一般
- **改进建议**：在 Section 2.1 目录树中补全 dto/ 和 converter/ 子目录

---

- **问题描述**：装配条件汇总表描述 `ai.mock.enabled=false` 时「激活的 AiService 实现」为「真实 AiService 实现」，但 Phase 0 无真实实现，最终仅 FallbackAiService 返回降级结果，可能误导 QA 或运维人员
- **所在位置**：Section 3.4「装配条件汇总表」
- **严重程度**：一般
- **改进建议**：增加说明 Phase 0 下 `ai.mock.enabled=false` 的行为，或明确注明 Phase 0 仅支持 `ai.mock.enabled=true`
