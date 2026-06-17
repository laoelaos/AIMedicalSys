# 已知问题

---

- K1: BaseEnum 在目录布局 §2.1 中提及但 §3.x 无规范定义 — 位置：`BaseEnum.java`，原因：设计文档遗漏，需在 OOD 文档中补充定义或移除引用
- K2: @phase0-mock-field 注解约定在 §8.2 定义但代码中无 Java 注解定义 — 位置：`Docs/04_ood_phase0.md §8.2`，原因：该约定目前为文档级规范，MockAiService 的占位数据已按约定填充；如需强制冻结语义应在后续阶段补充注解定义
- K3: FallbackAiService 空委托 ERROR 日志触发时机为首次调用而非启动期 — 位置：`FallbackAiService.java:60-67`，原因：文档描述与实际实现间的细微偏差；若按调用时机区分 ERROR/WARN 的行为可接受，则需更新设计说明
- K4: SecurityConfigPhase0 无独立单元测试 — 位置：`SecurityConfigPhase0.java`，原因：Phase 0 占位期由集成测试隐式覆盖（ApplicationContextIT + HealthCheckIT 验证 permitAll 生效），Phase 1 切换认证策略后需补充专项测试
