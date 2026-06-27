# 计划审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

### [严重] task_v1.md 文件路径缺失 `AIMedical/` 前缀
task_v1.md 第10-11行指定的路径为：
```
backend/modules/common-module/common-module-impl/src/main/java/...
backend/modules/common-module/common-module-impl/src/test/java/...
```
但相对于项目根目录 `C:\Develop\Software\AIMedicalSys`，实际文件位于 `AIMedical/backend/modules/common-module/common-module-impl/...`。实现者依据当前路径将无法找到目标文件，R1 无法执行。

同一项目下的 review 文件（如 `review_v1_B.md`、`review_v2_E.md`）均正确使用 `AIMedical/backend/modules/...` 前缀，task_v1.md 与此惯例不一致。

### [一般] T2-OOD 归属存在双重分配
R2 将 T2 描述为"异常刷新检测改为拒绝请求（OOD + 代码）— 1 source file"，暗示 OOD 文档更新在 R2 完成。但 R8 又包含"T2-OOD"，造成同一 OOD 文档修改任务被分配到两个不同轮次，产生归属歧义，可能导致重复修改或遗漏。

## 修改要求

1. **task_v1.md 路径修正**（针对严重问题）：将所有文件路径补充 `AIMedical/` 前缀，例如 `backend/modules/...` → `AIMedical/backend/modules/...`

2. **计划中 T2-OOD 归属明确**（针对一般问题）：选择以下方向之一 —
   - 方案A：T2 的 OOD 文档更新在 R2 一并完成，R8 移除"T2-OOD"
   - 方案B：R2 仅实现 T2 代码变更（依现有 OOD 实现拒绝逻辑），OOD 文档更新推迟至 R8 统一处理；此时 R2 标注应改为"（代码）"而非"（OOD + 代码）"
