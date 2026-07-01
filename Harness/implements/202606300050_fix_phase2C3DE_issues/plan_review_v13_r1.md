# 计划审查报告（v13 r1）

## 审查结果
REJECTED

## 发现

### 1. [一般] Item 5 异常类型修复的示例代码会导致 NPE

**位置**: task_v13.md Item 5（第 49-58 行）

**问题**: 示例代码中 `auditRecordRepository.save(any()).getClass()` 的执行路径：
1. `auditRecordRepository` 是 `@Mock` 字段，其 `save(any())` 默认返回 `null`
2. `null.getClass()` 抛出 NPE
3. 该表达式作为 `ObjectOptimisticLockingFailureException` 构造参数传入，在 mock 配置阶段就会失败

正确的写法应为：
```java
when(auditRecordRepository.save(any())).thenThrow(
    new ObjectOptimisticLockingFailureException(
        "com.aimedical.modules.prescription.entity.AuditRecord",
        new jakarta.persistence.OptimisticLockException()));
```
或使用 `AuditRecord.class` 作为第一个参数（需确认该类可访问）。

**期望的修正方向**: 修复 item 5 的示例代码，确保其可直接编译执行，或明确指定可用的正确构造方式。

### 2. [轻微] "只改 4 个文件"与实际文件数不一致

**位置**: task_v13.md 第 11 行 "修复清单（只改 4 个文件，不动无关生产代码）"

**问题**: 实际修改 5 个文件（4 个测试 + 1 个生产代码 `PrescriptionAuditServiceImpl.java`）。虽然改动很小，但计数不符。

**期望的修正方向**: 将 "只改 4 个文件" 更正为 "只改 5 个文件（4 个测试 + 1 个生产代码）"。

### 3. [轻微] plan.md R13 行涉及问题编号与修复清单无映射关系

**位置**: plan.md 第 22 行涉及问题列 `P02,P05,P06,P11,P14`

**问题**: 这 5 个问题编号未在 R13 段落（第 167-178 行）的修复清单中出现，读者无法将编号与具体修复项对应。这可能影响后续的问题追踪。

**期望的修正方向**: 在 R13 段落修复清单中标注每个修复项对应的问题编号，或添加映射说明。
