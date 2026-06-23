---
name: generic-problem-diagnosis-harness
description: "采用「诊断-质询」审议框架完成项目的问题定位。"
---

## 框架概述

本技能实现一个「诊断-质询」审议循环：两个子agent以文件为唯一沟通媒介，交替运行，审议迭代，直至问题根因准确定位且证据充分。主Agent仅负责编排，禁止读取子Agent产出文件，仅凭返回状态标记路由。

子agent完整指令文档（启动时让agent自行阅读，不要将内容复制到prompt中）：

[diagnostician](./diagnostician.md)：问题诊断agent的角色定义、工作流程与输出规范

[challenger](./challenger.md)：诊断质询agent的审查维度、报告格式与通过/驳回判定标准

## 调用参数

调用本技能时需提供以下参数：

- **`workdir`**（必填）— 工作目录的绝对路径，所有中间文件将直接写入该目录
- **`requirement`**（必填）— 完整问题描述，初始化时写入 `{workdir}/requirement.md`

### workdir 命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

workdir 应位于工作区的 `Harness/diags/` 目录下，命名格式为 `Harness/diags/{YYYYMMDDHHMM}_{简要描述}/`，例如 `Harness/diags/202604051000_login-crash-diagnosis/`。

## 文件约定

中间文件直接写入 `{workdir}/` 目录：

| 文件 | 维护者 |
|------|--------|
| `{workdir}/requirement.md` | 主Agent，初始化时写入 |
| `{workdir}/diag_v{N}.md`（N 从1开始递增） | diagnostician |
| `{workdir}/challenge_v{N}.md` | challenger |

## 编排流程

收到用户需求后，按以下流程执行。

### 1. 初始化

- 创建 `{workdir}/` 目录（如不存在）
- 将 `{requirement}` 写入 `{workdir}/requirement.md`
- 初始化轮次计数器 `N = 1`

### 2. 审议循环（最多 5 轮）

每轮依次执行步骤 A → B → C：

**步骤 A — 启动诊断agent**

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/diagnostician.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{workdir}/requirement.md
诊断报告输出文件：{workdir}/diag_v{N}.md
{若 N > 1，追加：上一轮质询文件：{workdir}/challenge_v{N-1}.md}
```

等待agent返回。agent应仅返回 `DIAG_WRITTEN:路径` 格式的信息。

**步骤 B — 启动质询agent**

诊断agent完成后，使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/challenger.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{workdir}/requirement.md
待审查诊断报告文件：{workdir}/diag_v{N}.md
质询输出文件：{workdir}/challenge_v{N}.md
```

等待agent返回。agent应仅返回 `LOCATED:路径` 或 `CHALLENGED:路径` 格式的信息。

**步骤 C — 判断终止**

- 返回 `LOCATED` → 循环结束，进入步骤 3
- 返回 `CHALLENGED` → `N++`，回到步骤 A
- 若 N 已达 5 → 循环结束，进入步骤 3

### 3. 向用户返回结果

仅返回以下信息：

- 达成一致时：`问题诊断已完成，最终报告：{workdir}/diag_v{N}.md`
- 未达成一致时：从最新 `{workdir}/challenge_v{N}.md` 中提取分歧点，附上最终诊断路径和质询路径

**不要**将诊断内容或质询内容输出到对话中，仅返回文件路径。
