---
name: generic-deliberative-execution-harness
description: "通用审议式流程，采用「执行-审查」审议框架完成任意用户任务。"
---

## 框架概述

本技能实现一个通用的「执行-审查」审议循环：两个子agent以文件为唯一沟通媒介，交替运行，审议迭代，直至产出的结果充分满足任务要求。主Agent仅负责编排，禁止读取子Agent产出文件，仅凭返回状态标记路由。

框架不预设产出物的形态——executor 根据任务自行判断应产出什么类型的文档、分析、方案或报告。审查维度也不预设——reviewer 根据任务本身的特性自行判断哪些质量维度是关键的。

子agent完整指令文档（启动时让agent自行阅读，不要将内容复制到prompt中）：

[executor](./executor.md)：执行agent的角色定义、工作流程与输出规范

[reviewer](./reviewer.md)：审查agent的审查维度、报告格式与通过/驳回判定标准

## 调用参数

调用本技能时需提供以下参数：

- **`workdir`**（必填）— 工作目录的绝对路径，所有中间文件将直接写入该目录
- **`task`**（必填）— 完整任务描述，初始化时写入 `{workdir}/task.md`
- **`max_rounds`**（可选）— 最大审议轮次，默认 5

### workdir 命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

workdir 应位于工作区的 `Harness/deliberations/` 目录下，命名格式为 `Harness/deliberations/{YYYYMMDDHHMM}_{简要描述}/`，例如 `Harness/deliberations/202604111600_api-error-analysis/`。

## 文件约定

中间文件直接写入 `{workdir}/` 目录：

| 文件 | 维护者 |
|------|--------|
| `{workdir}/task.md` | 主Agent，初始化时写入 |
| `{workdir}/output_v{N}.md`（N 从1开始递增） | executor |
| `{workdir}/review_v{N}.md` | reviewer |

## 编排流程

收到用户任务后，按以下流程执行。

### 1. 初始化

- 创建 `{workdir}/` 目录（如不存在）
- 将 `{task}` 写入 `{workdir}/task.md`
- 初始化轮次计数器 `N = 1`

### 2. 审议循环（最多 max_rounds 轮）

每轮依次执行步骤 A → B → C：

**步骤 A — 启动执行agent**

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/executor.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{workdir}/task.md
产出输出文件：{workdir}/output_v{N}.md
{若 N > 1，追加：上一轮审查文件：{workdir}/review_v{N-1}.md}
```

等待agent返回。agent应仅返回 `OUTPUT_WRITTEN:路径` 格式的信息。

**步骤 B — 启动审查agent**

执行agent完成后，使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/reviewer.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{workdir}/task.md
待审查产出文件：{workdir}/output_v{N}.md
审查输出文件：{workdir}/review_v{N}.md
```

等待agent返回。agent应仅返回 `APPROVED:路径` 或 `REJECTED:路径` 格式的信息。

**步骤 C — 判断终止**

- 返回 `APPROVED` → 循环结束，进入步骤 3
- 返回 `REJECTED` → `N++`，回到步骤 A
- 若 N 已达 max_rounds → 循环结束，进入步骤 3

### 3. 向用户返回结果

仅返回以下信息：

- 达成一致时：`审议式执行已完成，最终产出：{workdir}/output_v{N}.md`
- 未达成一致时：从最新 `{workdir}/review_v{N}.md` 中提取未解决问题摘要，附上最终产出路径和审查路径

**不要**将产出内容或审查内容输出到对话中，仅返回文件路径。
