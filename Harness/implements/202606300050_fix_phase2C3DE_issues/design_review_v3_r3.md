# 设计审查报告（v3 r3）

## 审查结果
APPROVED

## 发现

- **[轻微]** — 「行为契约」中声称 `restoreSession` 不会覆盖 `createSession` 刚创建的 session，但实际存在并发竞争窗口：T1 `restoreSession.get` 返回 null → T2 `createSession`（synchronized）put 完成 → T1 DB 恢复后 `put` 覆盖。此场景在客户端对同一 sessionId 并发发起两次请求时成立。该问题是预存条件，本设计未恶化，但描述不准确，建议修正或注明风险。

- **[轻微]** — `createSession` 使用 `synchronized` 实例级粗粒度锁，对不同 sessionId 的并发创建也会串行化。设计已注明未来 `SessionStore` 扩展 `putIfAbsent` 后可优化，此约束可接受。

- **[轻微]** — `setAdditionalResponses` 中的 `instanceof CopyOnWriteArrayList` 转换检查在实际使用中很少需要，但功能正确，无运行时问题。
