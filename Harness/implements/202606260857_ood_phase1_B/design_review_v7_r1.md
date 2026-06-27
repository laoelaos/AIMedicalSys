# 设计审查报告（v7 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** 算法伪代码中的 HTTP Header 名称错误（`detail_v7.md:99`）。第 99 行的伪代码使用 `request.getHeader("X-Forwarded-Forged")`，其中 `"X-Forwarded-Forged"` 是错误的 header 名称（"Forged" 而非 "For"）。正确的标准 header 名称为 `"X-Forwarded-For"`。该错误仅在算法伪代码中出现，文本规格说明（第 136 行）和测试用例 9（第 227 行）均正确使用了 `"X-Forwarded-For"`，造成设计文档内部不一致。若开发者直接按伪代码实现，会导致 IP 提取的 X-Forwarded-For 分支始终走不到，破坏 reverse proxy 场景的 IP 获取行为。

## 修改要求（仅 REJECTED 时）

1. **[一般]** 将 `detail_v7.md:99` 伪代码中的 `"X-Forwarded-Forged"` 更正为 `"X-Forwarded-For"`，与文档其他位置的规格保持一致。
