# 代码审查报告（v10 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `DefaultLocalRuleEngine.java:12,19,25,36` — `allergyCheckRules` 字段声明类型为 `List<AllergyCheckRule>`，但构造器参数类型为 `AllergyCheckRule`（单个实例），赋值 `this.allergyCheckRules = allergyCheckRules` 类型不匹配无法编译；且第36行 `allergyCheckRules.check()` 在 `List<AllergyCheckRule>` 上无此方法，同样无法编译。

- **[一般]** `ContraindicationCheckRule.java:38-41` — 使用 `contains()` 在原始 JSON 文本上做合并症匹配，而非解析 JSON 结构化比对 `diseaseName`，易产生误报；且未区分 ABSOLUTE_CONTRAINDICATION（应 BLOCK）和 RELATIVE_CONTRAINDICATION（应 WARN），所有命中均返回 BLOCK，偏离设计。

- **[一般]** `DuplicateCheckRule.java:33-41` — 使用 `ingredients` 原始 JSON 字符串作为 `Set` 元素检测成分重叠，仅当两个药品的完整 JSON 完全相同时才触发匹配，无法检测不同药品间部分成分重叠（如共享同一 ingredientCode）的场景，偏离设计。

- **[轻微]** `PrescriptionAuditController.java:46-50` — `audit()` 端点 BLOCK 路径调用 `prescriptionAuditEnforcer.enforce()` 但未使用其返回值（变量 `block` 无效），enforcer 形同虚设。

## 修改要求（REJECTED）

1. **[严重] `DefaultLocalRuleEngine.java:12`** — 将字段 `private final List<AllergyCheckRule> allergyCheckRules` 改为 `private final AllergyCheckRule allergyCheckRules`（去除 `List<>` 包装），使字段类型与构造器参数及第36行调用 `allergyCheckRules.check()` 一致。

2. **[一般] `ContraindicationCheckRule.java:38-41`** — 将 `contraindications` JSON 字符串解析为结构化对象列表（`diseaseName`/`level`/`description`），用 `diseaseName` 与 `patientInfo.comorbidities` 做交集比对；按 `level` 值区分 `ABSOLUTE_CONTRAINDICATION → BLOCK`、`RELATIVE_CONTRAINDICATION → WARN`。

3. **[一般] `DuplicateCheckRule.java:33-41`** — 将 `ingredients` JSON 字符串解析为结构化 ingredient 列表，提取 `ingredientCode` 集合；以 `Set<String>` 记录所有已见 ingredientCode，检测跨药品的 ingredientCode 交集。

4. **[轻微] `PrescriptionAuditController.java:45-50`** — 移除无效的 `prescriptionAuditEnforcer.enforce()` 调用，或将其返回值 `BlockResponse` 用于构建 422 响应体。
