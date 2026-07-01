# 设计审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** P11: DosageLimitRule.findBestMatch() 部分 null 分支将 agePartial+weightComplete 错误路由至 Level 4

**OOD §8.4 决策表 #5 和 #7 明确规定**：ageRange 部分 null（仅一个非 null）+ weightRange 完整（均非 null）→ Level 5（默认阈值），而非 Level 4（体重范围匹配）。

**设计代码**（detail_v5.md:272-276）：
```java
if (agePartial && !weightPartial) {
    if (weightComplete && weight != null
            && weight.compareTo(ws.doubleValue()) >= 0
            && weight.compareTo(we.doubleValue()) <= 0 && level4 == null) {
        level4 = ds;  // ← 错误：应为 level5
    }
    if (weightNull && level5 == null) {
        level5 = ds;
    }
}
```

**设计文档描述**（detail_v5.md:340-341）也明确声明了错误路由：
> ageRange 部分 null + weightRange 完整 → 仅尝试 Level 4（体重范围匹配）和 Level 5（无分级默认）

**根因**：Level 4 的前提条件是 `ageRangeStart == null && ageRangeEnd == null`（年龄维度完全 null），而 agePartial 意味着 ageRangeStart/ageRangeEnd 中有一个非 null，不满足 Level 4 的"ageRange 均为 null"先决条件。OOD 决策表 #5 的说明明确指出："ageRange 部分 null——Level 3 先决条件不满足（ageRange 字段需'均为 null'或'均非 null'，部分 null 视为无效）；weightRange 均非 null 但 ageRange 无效 → Level 5"。同理 #7（ageRangeStart null + ageRangeEnd 非 null + weightRange 完整）也属于 agePartial，应降级至 Level 5。

**影响**：当数据库中存在 ageRange 部分 null + weightRange 完整的 DosageStandard 记录时，findBestMatch 会错误地将其作为 Level 4 匹配返回，而非按 OOD 要求降级至 Level 5。这可能导致体重范围匹配优先级高于 OOD 预期，产生不精确的剂量校验结果。

### **[一般]** P11: DosageLimitRule.findBestMatch() 部分 null 分支体重比较方式与主逻辑不一致，存在精度损失风险

**部分 null 分支**（detail_v5.md:274-275）使用 `weight.compareTo(ws.doubleValue())`，将 BigDecimal 先转为 double 再与 Double 比较，存在浮点精度损失。

**主匹配逻辑**（detail_v5.md:299-301）正确使用 `BigDecimal.valueOf(weight).compareTo(ws)`，通过 BigDecimal 比较避免精度问题。

同一方法内两种比较方式不一致，且部分 null 分支的 `ws.doubleValue()` 转换在 weightRangeStart/End 的 scale=2 时通常安全，但若未来 scale 变更或值超出 double 精确表示范围，将产生比较错误。

### **[轻微]** P11: SpecialPopulationDosageRule.check() 未显式处理 weightRange 部分 null 场景

当 weightRangeStart 非 null 而 weightRangeEnd 为 null（或反之）时，`weightMatch` 条件中 `ds.getWeightRangeStart() != null && ds.getWeightRangeEnd() != null` 为 false，导致该标准被静默跳过。此行为与 OOD §8.4"部分 null 视为无效"的语义一致，但设计中未在变更点或错误处理中显式记录此行为，可能导致后续维护者误认为是遗漏。

## 修改要求（仅 REJECTED 时）

### 严重问题修正

**问题**：agePartial+weightComplete 被错误路由至 Level 4

**为什么是问题**：违反 OOD §8.4 决策表 #5/#7 的明确定义，Level 4 要求 ageRange 均为 null，agePartial 不满足此先决条件

**期望修正方向**：
1. 将 `agePartial && !weightPartial` 分支中的 `weightComplete` 匹配从 `level4` 改为 `level5`（即删除 level4 赋值，仅保留 level5 赋值）
2. 同步修正设计文档描述（detail_v5.md:340-341），将"ageRange 部分 null + weightRange 完整 → 仅尝试 Level 4 和 Level 5"改为"ageRange 部分 null + weightRange 完整 → 仅尝试 Level 5"
3. 同步修正行为契约表（detail_v5.md:428-430）中"ageRange 部分 null：跳过 Level 1-3，仅尝试 Level 4-5"改为"跳过 Level 1-4，仅尝试 Level 5"

### 一般问题修正

**问题**：部分 null 分支体重比较使用 `weight.compareTo(ws.doubleValue())` 存在精度损失

**为什么是问题**：同一方法内比较方式不一致，BigDecimal→double 转换存在精度损失风险

**期望修正方向**：将 `weight.compareTo(ws.doubleValue()) >= 0 && weight.compareTo(we.doubleValue()) <= 0` 改为 `BigDecimal.valueOf(weight).compareTo(ws) >= 0 && BigDecimal.valueOf(weight).compareTo(we) <= 0`，与主匹配逻辑保持一致
