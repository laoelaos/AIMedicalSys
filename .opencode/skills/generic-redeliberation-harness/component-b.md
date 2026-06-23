# 组件B — 审议式质量审查

## 概述

组件B使用审议式质量审查流程，诊断组件A产出中是否存在质量问题。主Agent扮演其编排角色，按照审查-质询循环逐环节启动子Agent。

## 引用的指令文件

使用本目录下的专用质量审查agent指令文件，启动子Agent时让其自行阅读：

- **质量审查agent指令**：`{this_skill_dir}/quality-reviewer.md`
- **质量质询agent指令**：`{this_skill_dir}/quality-challenger.md`

## 文件命名

| 文件 | 维护者 |
|------|--------|
| `{workdir}/b_v{M}_diag_v{N}.md`（N 从1开始递增） | 质量审查agent |
| `{workdir}/b_v{M}_challenge_v{N}.md` | 质量质询agent |

## 各模式审查视角

主Agent应在任务描述中根据不同模式追加以下审查视角：

- `ood`："注意：你正在审查的是一份 OOD 设计文档。请从实际落地视角评估：设计是否可直接指导编码实现、接口定义是否足以支持下游消费者、异常场景和边界条件是否已考虑。"
- `execution`："注意：你正在审查的是一份通用执行产出。请从使用者视角评估：产出是否可直接投入使用、是否覆盖了所有显式和隐式需求、边界情况和异常处理是否完备。"
- `diagnosis`："注意：你正在审查的是一份问题诊断报告。请从可操作性视角评估：诊断建议是否足以让执行者直接采取行动、修复方案是否有潜在副作用、优先级排序是否合理。"
- `requirement`："注意：你正在审查的是一份需求设计文档。请从实现者视角评估：需求是否可直接作为开发依据、验收标准是否明确可量化、是否存在隐含的模糊地带。"
- `technical`："注意：你正在审查的是一份技术方案设计文档。请从工程实施视角评估：方案是否可直接指导具体实现、技术风险和缓解措施是否充分、是否有遗漏的关键技术决策。"

## 编排流程

主Agent按照以下流程编排组件B的内部审议循环（最多 `{b_max_rounds}` 轮，默认 12）。

初始化内部轮次计数器 `N = 1`。每轮依次执行步骤 A → B → C：

**步骤 A — 启动质量审查agent**

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/quality-reviewer.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{SKILL.md 步骤4中构造的任务描述文本}
待审查产出文件：{组件A最终产出路径，即 a_result_path}
审查报告输出文件：{workdir}/b_v{M}_diag_v{N}.md
{若 N > 1，追加：
上一轮审查报告文件：{workdir}/b_v{M}_diag_v{N-1}.md
上一轮质询文件：{workdir}/b_v{M}_challenge_v{N-1}.md}

注意：你的审查报告应重点描述产出中存在的具体质量问题（如事实错误、关键遗漏、逻辑矛盾等），而非仅给出整体评价。每个问题应包含：问题描述、所在位置、严重程度、改进建议。

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待agent返回。agent应仅返回 `DIAG_WRITTEN:路径`。

**步骤 B — 启动质量质询agent**

使用 Task 工具（`subagent_type: "general"`）启动，prompt 结构：

```
请先阅读文件 {this_skill_dir}/quality-challenger.md 获取完整工作指令，然后按要求完成以下任务：

任务描述：{同上的任务描述文本}
待审查报告文件：{workdir}/b_v{M}_diag_v{N}.md
质询输出文件：{workdir}/b_v{M}_challenge_v{N}.md

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待agent返回。agent应仅返回 `LOCATED:路径` 或 `CHALLENGED:路径`。

**步骤 C — 判断终止**

- 返回 `LOCATED` → 内部循环结束
- 若 N 已达 `{b_max_rounds}` → 内部循环结束
- 返回 `CHALLENGED` → `N++`，回到步骤 A
- **异常保护**：若质询agent异常导致质询文件未生成，内部循环结束（不再重试），使用已有的审查报告继续后续流程

组件B完成后，记录：

- **最终诊断报告路径** `{b_diag_result_path}`：`{workdir}/b_v{M}_diag_v{N}.md`
- **最终质询报告路径** `{b_challenge_result_path}`：`{workdir}/b_v{M}_challenge_v{N}.md`（若质询agent异常则记录为 null）
- 两个路径均传递给步骤 5 的 judge agent；当质询路径为 null 时，主Agent在 judge prompt 中省略质询文件行并注明"无"
