# 设计审查报告（v17 r3）

## 审查结果
APPROVED

## 发现

- **[轻微]** 设计说明 "若不 stub 则 Mockito 返回 null"（第18行）不准确。Spring Boot 3.2.5 使用的 Mockito 5.x 对 `List` 类型默认返回 `Collections.emptyList()` 而非 `null`，故缺失 stub 不会导致 `aggregateRiskLevel(null)` NPE。但增加 stub 仍为正确做法，与同类 fallback 测试保持一致，建议修改说明文字为 "Mockito 返回空列表，但显式 stub 可增强可读性与防护"。

其余所有变更经逐项验证与代码库一致，无其他问题。
