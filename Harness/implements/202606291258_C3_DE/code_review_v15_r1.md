# 代码审查报告（v15 R1）

## 审查结果
REJECTED

## 发现
- **[一般]** `MedicalRecordServiceImpl.java:69` — visitIdFallback 标志逻辑取反，与实际含义相反

## 修改要求
### 问题：visitIdFallback 逻辑反转

**位置**：`.../service/impl/MedicalRecordServiceImpl.java` 第 69 行

**问题描述**：实现中写为 `boolean visitIdFallback = !request.getEncounterId().equals(visitId);`，但该逻辑与实际含义恰好相反：
- VisitFacade 正常返回（成功查找 visitId）时，encounterId ≠ visitId → `visitIdFallback = true`（错误，应为 false）
- VisitFacade 超时/异常走降级（返回 encounterId 自身）时，encounterId == visitId → `visitIdFallback = false`（错误，应为 true）

这与详细设计"行为契约 #6"（fromFallback=true 表示 visitId 来自降级）以及步骤 1(a)（降级时 visitIdFallback=true）完全相反。

**期望的修正方向**：移除该行简单的比较，改为在 `resolveVisitId()` 方法中通过返回值（如封装结果对象或通过布尔标志位）区分"正常成功"与"降级回退"两个场景，并依此设置 `visitIdFallback`。例如：

```java
// resolveVisitId 改为返回封装结果，或新增一个字段记录是否降级
// 在 generate() 中：
//   正常成功时 visitIdFallback = false
//   降级回退时 visitIdFallback = true
```
