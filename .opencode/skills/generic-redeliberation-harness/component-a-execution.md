# 组件A — 审议式通用执行模式

## 引用的指令文件

复用 generic-deliberative-execution-harness 的子Agent指令文件，启动子Agent时让其自行阅读：

- **执行agent指令**：`{this_skill_dir}/../generic-deliberative-execution-harness/executor.md`
- **审查agent指令**：`{this_skill_dir}/../generic-deliberative-execution-harness/reviewer.md`

## 文件命名

| 文件 | 维护者 |
|------|--------|
| `{workdir}/a_v{M}_output_v{N}.md`（N 从1开始递增） | 执行agent |
| `{workdir}/a_v{M}_review_v{N}.md` | 审查agent |

## 编排流程

主Agent按照以下流程编排内部审议循环（最多 `{a_max_rounds}` 轮）。

初始化内部轮次计数器 `N = 1`。每轮依次执行步骤 A → B → C：

**步骤 A — 启动执行agent**

{若 edit_mode 且 N>1：主Agent须先将 N-1 轮输出文件复制为本轮输出文件路径，再启动子Agent。}

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/../generic-deliberative-execution-harness/executor.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{M=1 时为 {workdir}/requirement.md；M>1 时为 parser 返回的 REQUIREMENT_FOR_A 文件路径}
产出输出文件：{若 edit_mode 且 N=1：{workdir}/a_v{M}_copy_from_v{M-1}.md；否则：{workdir}/a_v{M}_output_v{N}.md}
{若 edit_mode 且 N=1，追加：【首要指令】你的首要任务是修改输出文件中的已有内容，而非从零创建。输出文件中已有上一轮的完整产出，请务必先完整阅读该文件，再在此基础上进行定向修改。任何从头重写的行为都是错误的。}
{若 edit_mode 且 N>1，追加：请基于输出文件中已有的内容进行定向修改，不要从头重写。}
{若 N > 1，追加：上一轮审查文件：{workdir}/a_v{M}_review_v{N-1}.md}

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待agent返回。agent应仅返回 `OUTPUT_WRITTEN:路径`。

**步骤 B — 启动审查agent**

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/../generic-deliberative-execution-harness/reviewer.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{M=1 时为 {workdir}/requirement.md；M>1 时为 parser 返回的 REQUIREMENT_FOR_A 文件路径}
{若 M>1，追加：原始用户需求（供对照）：{workdir}/requirement.md}
待审查产出文件：{若 edit_mode 且 N=1：{workdir}/a_v{M}_copy_from_v{M-1}.md；否则：{workdir}/a_v{M}_output_v{N}.md}
审查输出文件：{workdir}/a_v{M}_review_v{N}.md

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待agent返回。agent应仅返回 `APPROVED:路径` 或 `REJECTED:路径`。

**步骤 C — 判断终止**

- 返回 `APPROVED` → 内部循环结束，组件A完成
- 若 N 已达 `{a_max_rounds}` → 内部循环结束，组件A完成
- 返回 `REJECTED` → `N++`，回到步骤 A

组件A完成后，最终产出路径为：若 `edit_mode` 且最终 `N=1`，则为 `{workdir}/a_v{M}_copy_from_v{M-1}.md`；否则为 `{workdir}/a_v{M}_output_v{N}.md`。
