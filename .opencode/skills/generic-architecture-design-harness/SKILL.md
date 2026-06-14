---
name: generic-architecture-design-harness
description: "采用「设计-验证」审议框架完成项目架构级OOD设计。"
---

## 框架概述

本技能实现一个「设计-验证」审议循环：两个子agent以文件为唯一沟通媒介，交替运行，审议迭代，直至就目标语言OOD设计方案达成一致。主Agent仅负责编排，禁止读取子Agent产出文件，仅凭返回状态标记路由。

本框架产出的是**架构级OOD设计**，聚焦于职责划分、抽象层次、协作模式和关键设计决策，而非具体字段、方法签名等实现细节。设计方案的抽象度应足以指导后续的详细设计和编码实现，但不直接包含可执行的代码规格。

子agent完整指令文档（启动时让agent自行阅读，不要将内容复制到prompt中）：

[designer](./designer.md)：设计agent的角色定义、设计流程与输出规范

[verifier](./verifier.md)：验证agent的审查维度、报告格式与通过/驳回判定标准

## 调用参数

调用本技能时需提供以下参数：

- **`workdir`**（必填）— 工作目录的绝对路径，所有中间文件将直接写入该目录
- **`requirement`**（必填）— 完整需求描述，初始化时写入 `{workdir}/requirement.md`

### workdir 命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

workdir 应位于工作区的 `Harness/designs-oo/` 目录下，命名格式为 `Harness/designs-oo/{YYYYMMDDHHMM}_{简要描述}/`，例如 `Harness/designs-oo/202604031140_gh-pr-arch-design/`。

## 文件约定

中间文件直接写入 `{workdir}/` 目录：

| 文件 | 维护者 |
|------|--------|
| `{workdir}/requirement.md` | 主Agent，初始化时写入 |
| `{workdir}/design_v{N}.md`（N 从1开始递增） | designer |
| `{workdir}/review_v{N}.md` | verifier |

## 编排流程

收到用户需求后，按以下流程执行。

### 1. 初始化

- 创建 `{workdir}/` 目录（如不存在）
- 将 `{requirement}` 写入 `{workdir}/requirement.md`
- 初始化轮次计数器 `N = 1`

### 2. 审议循环（最多 5 轮）

每轮依次执行步骤 A → B → C：

**步骤 A — 启动设计agent**

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/designer.md 获取完整工作指令，然后按要求完成以下任务：

完整需求描述：{workdir}/requirement.md
设计输出文件：{workdir}/design_v{N}.md
{若 N > 1，追加：上一轮审查文件：{workdir}/review_v{N-1}.md}
```

等待agent返回。agent应仅返回 `DESIGN_WRITTEN:路径` 格式的信息。

**步骤 B — 启动验证agent**

设计agent完成后，使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/verifier.md 获取完整工作指令，然后按要求完成以下任务：

完整需求描述：{workdir}/requirement.md
待审查设计文件：{workdir}/design_v{N}.md
审查输出文件：{workdir}/review_v{N}.md
```

等待agent返回。agent应仅返回 `APPROVED:路径` 或 `REJECTED:路径` 格式的信息。

**步骤 C — 判断终止**

- 返回 `APPROVED` → 循环结束，进入步骤 3
- 返回 `REJECTED` → `N++`，回到步骤 A
- 若 N 已达 5 → 循环结束，进入步骤 3

### 3. 向用户返回结果

仅返回以下信息：

- 达成一致时：`OOD设计已完成，最终方案：{workdir}/design_v{N}.md`
- 未达成一致时：从最新 `{workdir}/review_v{N}.md` 中提取分歧点，附上最终设计路径和审查路径

**不要**将设计内容或审查内容输出到对话中，仅返回文件路径。
