# 设计审查报告（v9 r1）

## 审查结果
REJECTED

## 发现

### [一般] 7c 测试未真正验证缓存策略变更

`shouldLoadRulesFromRepositoryOnCacheMiss` 测试使用 `assertSame(engine.match("a", null, null), engine.match("a", null, null))` 验证两次调用返回同一对象。但该断言在 **新旧两种策略（`refreshAfterWrite(60s)` 和 `expireAfterWrite(30s)`）下均能通过**——因为连续两次调用间隔远小于超时阈值，Always 命中缓存。

设计文档声称"本测试确保 expireAfterWrite 配置正确"，这是不准确的。该测试仅验证"缓存仍在工作"，而非"已从 refreshAfterWrite 切换为 expireAfterWrite"。若后续重构误回退到 refreshAfterWrite，该测试无法拦截。

**期望的修正方向**：调整测试策略，使其能区分两种策略。可选方案：
- 通过 `Caffeine` 的 `ticker`（`Caffeine.newBuilder().ticker(...)`）模拟时间推进，使条目在 expireAfterWrite 超时后失效，验证第二次调用触发同步加载（而非返回陈旧值）；或
- 直接通过反射或配置注入验证 `ruleCache` 实际使用的 `Caffeine` 配置参数；或
- 在集成测试层覆盖 30s 不一致窗口的行为。

### [轻微] 7a 行号表述不一致

设计文档写"第62行后"，任务文件写"第63行"。两者指向同一逻辑位置（`ruleVersionMismatch = true` 赋值后），不影响实现正确性，建议与任务文件对齐。

## 修改要求（仅 REJECTED 时）

### 问题 1：7c 测试未验证缓存策略变更
- **问题**：测试 `shouldLoadRulesFromRepositoryOnCacheMiss` 无法区分 `expireAfterWrite(30s)` 与 `refreshAfterWrite(60s)`，对核心变更提供零覆盖率。
- **为什么是问题**：该缺陷在代码审查阶段不易发现（测试通过），导致变更实际未经测试覆盖。若后续维护人员误回退到 refreshAfterWrite，或调整了超时值，此测试不会失败。
- **期望方向**：见上方「发现」中的修正方案择一实施。
