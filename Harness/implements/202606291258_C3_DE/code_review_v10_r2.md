# 代码审查报告（v10 r2）

## 审查结果
REJECTED

## 发现

- **[一般]** `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/api/PrescriptionAuditController.java:50` — audit 端点阻断时返回的 blockCode 为硬编码 `"RX_BLOCK_AUDIT"`，但设计错误码表（detail_v10.md §错误码清单）明确要求 audit 端点的阻断错误码为 `"RX_AUDIT_BLOCKED"`。PrescriptionErrorCode 枚举已定义该常量但未被使用。

- **[一般]** `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/AllergyCheckRule.java:42` — allergens 使用 `String.contains()` 做子串文本匹配而非 JSON 结构化解析。设计指定 `allergens` 字段存储格式为 JSON（`@Column(columnDefinition="TEXT") (JSON)`），要求"按 allergen 精确匹配"。ContraindicationCheckRule 在 v10 r1 中已通过注入 ObjectMapper + JSON 解析修复同类问题，但 AllergyCheckRule 仍存在该缺陷，可能导致子串误匹配（如"pen"误匹配"penicillin"）。

## 修改要求（REJECTED）

1. **PrescriptionAuditController.java:50** — 将 `"RX_BLOCK_AUDIT"` 替换为 `PrescriptionErrorCode.RX_AUDIT_BLOCKED.getCode()`，对齐设计定义的 errorCode。

2. **AllergyCheckRule.java:42** — 注入 ObjectMapper，将 `mapping.getAllergens()` 的 JSON 字符串解析为结构化列表（如 `List<String>`），对 `allergen` 做精确元素匹配而非子串包含，与 ContraindicationCheckRule 的修复模式一致。
