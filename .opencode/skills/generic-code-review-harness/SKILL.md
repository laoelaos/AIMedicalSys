---
name: generic-code-review-harness
description: "与用户协作进行审议式、渐进式代码审查。"
---

## 调用参数

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

- **`workdir`**（必填）— 工作目录绝对路径，命名格式 `Harness/reviews/{YYYYMMDDHHMM}_{简要描述}/`
- **`project_root`**（必填）— 项目根目录绝对路径
- **`source_branch`**（必填）— 待审查的源分支名
- **`target_branch`**（必填）— squash merge 的目标分支名

## 文件约定

所有文件写入 `{workdir}/`：

| 文件 | 用途 |
|------|------|
| `scope.md` | 审查范围界定 |
| `review.md` | 审查进度跟踪（主Agent维护） |
| `review_v{N}.md` | 第N轮详细审查报告 |
| `todo.md` | 待办事项（格式见 [todo](./todo.md)，主Agent维护） |
| `known_issues.md` | 已知但暂不解决的问题（格式见 [known_issues](./known_issues.md)，主Agent维护） |

子agent指令：[reviewer](./reviewer.md)（启动时让agent自行阅读，不复制到prompt中）

## 流程

### 阶段零：分支准备

以 workdir 路径最后一段为分支名（`{branch}`），依次执行：

```bash
git checkout {target_branch} && git pull
git checkout -b {branch}
git merge --squash {source_branch}
```

审查范围自动锚定为暂存区全部变更（`git diff --cached`）。

### 阶段一：范围界定

检查 `{workdir}/scope.md`：
- **不存在**：用 question 工具与用户确认审查目标、依据、重点、背景、排除范围，整理后写入文件
- **存在**：展示当前范围，询问是否调整

### 阶段二：渐进式审查循环

1. 检查 `{workdir}/review.md`，向用户汇报已完成的审查概况（或初始化 N=1）
2. 询问用户本轮审查目标（默认：暂存区全部变更；用户可缩小至特定文件/模块/功能点）
3. 用 Task 工具（`subagent_type: "general"`）启动审查agent，prompt：

```
请先阅读文件 {this_skill_dir}/reviewer.md 获取完整工作指令，然后按要求完成以下任务：

范围界定文件：{workdir}/scope.md
审查报告输出文件：{workdir}/review_v{N}.md
项目根目录：{project_root}
本轮审查目标：{用户指定的具体审查内容描述}
```

4. 等待agent返回 `REVIEW_WRITTEN:{审查报告文件路径}`
5. 读取报告，向用户展示摘要（问题数量和严重程度分布）
6. 在 `review.md` 追加本轮结论：

```markdown
## R{N}: {审查目标简述} — 严重 {N} / 一般 {N} / 轻微 {N} — {一句话总评} → `review_v{N}.md`
```

7. 与用户讨论本轮结果，收集意见，在 `review.md` 追加决定摘要：
   - 有待办 → 按 [todo](./todo.md) 格式追加到 `{workdir}/todo.md`，分配编号 T{N}，`review.md` 记录 `> 决定：T{N} {简述}`
   - 暂不解决 → 按 [known_issues](./known_issues.md) 格式追加到 `{workdir}/known_issues.md`，分配编号 K{N}，`review.md` 记录 `> 决定：K{N} {简述}`
   - 无待办 → `review.md` 记录 `> 决定：{用户意见简述}`
8. `N++`，询问是否继续 → 继续：回到步骤2；结束：进入阶段三

### 阶段三：审查总结

向用户返回摘要：审查范围、轮次数、问题统计、待办状态。**不**输出审查全文，引导用户查看详细报告和待办文件。

### 阶段四：收尾

先暂存所有内容，再提交当前分支暂存内容：

```bash
git commit -m "review: {source_branch} squash into {target_branch}"
```
