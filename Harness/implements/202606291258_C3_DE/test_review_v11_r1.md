# 测试审查报告（v11 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `test_v11.md` — 测试报告文件不存在，测试交付物完全缺失，无法审查 v11 测试覆盖范围
- **[严重]** 测试源文件未针对 v11 代码变更进行更新：
  - `PrescriptionAuditServiceImplTest.java` — `hasNewAlerts()` 逻辑反转修复（code_v11.md §修订说明）无对应测试用例验证修正后的双向增量检测正确性
  - `PrescriptionAuditControllerTest.java` — `enforce()` 返回值修正（code_v11.md §修订说明）无对应断言验证 422 响应体使用 `blockInfo.getBlockCode()` / `blockInfo.getBlockReasons()` 而非硬编码值
  - `PrescriptionErrorCodeTest.java` — `PrescriptionErrorCode` 导入路径从 `entity` 包变更为根包 `prescription` 无对应测试更新（虽当前 import 正确，但无测试文档确认该变更）

## 修改要求（仅 REJECTED 时）

1. **[严重]** `Harness/implements/202606291258_C3_DE/test_v11.md` — 创建 v11 测试报告，索引全部测试文件并说明覆盖维度
2. **[严重]** `prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` — 为 `hasNewAlerts()` 方法增加专用测试用例，验证：（a）快照为空、current 非空 → 有增量（返回 true）；（b）快照非空、current 为空 → 无增量（返回 false）；（c）快照与 current 元素完全一致 → 无增量（返回 false）；（d）快照包含 current 全部元素但 current 有新增元素 → 有增量（返回 true）
3. **[严重]** `prescription/.../api/PrescriptionAuditControllerTest.java` — `auditShouldReturn422WhenBlocked()` 用例增加断言：验证 422 响应 body 中 `errorCode` 等于 `blockInfo.getBlockCode()`（而非任意字符串），验证 `message` 包含 `blockReasons` 内容
