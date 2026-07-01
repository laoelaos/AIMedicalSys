# 设计审查报告（v12 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** — 9i 文件路径在文件规划表与类型定义中不一致

- **文件规划表**（第16-17行）：`medical-record/.../store/DraftContextStoreImpl.java`、`medical-record/.../store/DraftContextStore.java`
- **类型定义 9i**（第197-198行）：`common-module/common-module-api/.../store/DraftContextStore.java`、`common-module/common-module-api/.../store/impl/DraftContextStoreImpl.java`
- 两处归属模块不同（`medical-record` vs `common-module`），实施者无法判断哪个路径是正确的。若按错误路径操作将导致文件找不到或改错文件。

### **[一般]** — 文件规划表列了 AuditConverter.java（9k）但无对应变更描述

- 文件规划表第25行：`prescription/.../converter/AuditConverter.java | 修改 | 9k`
- 但类型定义 9k（第260-266行）仅描述了 `PrescriptionAuditServiceImpl.java` 的变更
- 要么该文件无需修改（规划表多余），要么缺少变更描述（遗漏）。实施者无法判断。

### **[一般]** — 设计未覆盖测试变更

- task_v12.md 包含了各子项详细的测试修改方案（9a/9b/9c/9e/9f/9g/9h/9i/9j/9k/9l/9p 的测试断言适配、新增测试用例等）
- 设计文档仅在"行为契约"中提及 `MedicalRecordErrorCodeTest.shouldHaveEightConstants` 需改为 10，其余测试变更均未纳入设计
- 设计应对关键测试变动做出规划，确保实现可验证

## 修改要求

1. **统一 9i 文件路径**：确认 `DraftContextStore.java` 和 `DraftContextStoreImpl.java` 的正确模块路径，确保文件规划表和类型定义一致。
2. **补齐或删除 AuditConverter.java 条目**：若确认无需修改则从规划表移除；若确有变更则补充变更描述。
3. **补充测试变更设计**：将 task_v12.md 中的测试修改方案摘要纳入设计文档，至少涵盖断言调整和新测试的关键点。
