---
name: generic-redeliberation-harness
description: "再审议框架 SKILL。只有用户明确要求使用再审议框架时，才使用此 SKILL。并且使用此 SKILL 时，不需要再加载其他审议式 SKILL ，但可以加载准备 SKILL。"
---

## 框架概述

本技能实现「A-B迭代」审议闭环：

- **组件A**：执行某个审议式技能（OOD设计、通用执行、问题定位、需求设计、技术设计），主Agent扮演其编排角色
- **组件B**：审议式质量审查，诊断组件A产出中的质量问题

先运行A产出结果（或通过 `initial_artifact` 导入已有产出），再运行B诊断质量。若B发现问题，根据反馈重新运行A，形成迭代闭环，直到B通过或达到最大迭代次数。

由于opencode中子Agent无法启动子Agent，采用以下协作模式：

- **子Agent（parser）**：解析用户输入，为组件A准备运行参数；迭代时拼接含B反馈的新输入
- **主Agent**：扮演A和B的编排角色，逐环节调度子Agent
- **子Agent（judge）**：阅读B的结果，判定是否需要重新运行A

子agent指令文档（启动时让agent自行阅读，不要将内容复制到prompt中）：

> **关于 `{this_skill_dir}`**：本技能文件中所有 `{this_skill_dir}` 占位符指代本技能的目录路径（即 SKILL.md 所在目录的绝对路径）。主Agent在加载技能时可从技能的 `location` 属性中提取该路径，在构造子Agent prompt 前完成替换。

[parser](./parser.md)：输入解析agent

[judge](./judge.md)：判定agent

[history-extractor](./history-extractor.md)：历史提取agent

组件流程文档（主Agent自行阅读以掌握编排流程）：

[component-a](./component-a.md)：组件A的整体功能与可用模式

[component-a-ood](./component-a-ood.md)：审议式OOD模式

[component-a-execution](./component-a-execution.md)：审议式通用执行模式

[component-a-diagnosis](./component-a-diagnosis.md)：审议式问题定位模式

[component-a-requirement](./component-a-requirement.md)：审议式需求设计模式

[component-a-technical](./component-a-technical.md)：审议式技术设计模式

[component-b](./component-b.md)：组件B的整体功能与流程

## 调用参数

- **`workdir`**（必填）— 工作目录的绝对路径，所有中间文件将直接写入该目录
- **`mode`**（必填）— 组件A运行模式：ood / execution / diagnosis / requirement / technical
- **`requirement`**（必填）— 完整需求描述，初始化时写入 `{workdir}/requirement.md`
- **`initial_artifact`**（可选）— 已有审议产出文件的绝对路径。提供时，首轮跳过组件A，直接对该产出运行组件B审查
- **`max_iterations`**（可选）— 最大A-B迭代次数，默认 30
- **`a_max_rounds`**（可选）— 组件A内部审议循环最大轮次，默认 12
- **`b_max_rounds`**（可选）— 组件B内部审议循环最大轮次，默认 12

### workdir 命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

workdir 应位于工作区的 `Harness/redeliberations/` 目录下，命名格式为 `Harness/redeliberations/{YYYYMMDDHHMM}_{简要描述}/`。

## 文件约定

| 文件 | 维护者 |
|------|--------|
| `{workdir}/requirement.md` | 主Agent，初始化时写入 |
| `{workdir}/a_v1_imported.md` | 主Agent，initial_artifact 模式下复制已有产出 |
| `{workdir}/a_v{M}_*`（M 从1开始递增） | 组件A各子Agent |
| `{workdir}/b_v{M}_*` | 组件B各子Agent |
| `{workdir}/iteration_v{M}.md` | judge agent |
| `{workdir}/iteration_history.md` | history-extractor agent |

## 外部文件依赖

组件A的模式文件通过相对路径引用其他审议式技能的子Agent指令文件。若被引用技能的目录结构发生变化，本技能将在运行时静默失败。

| 模式文件 | 外部技能 | 引用文件 |
|---------|---------|---------|
| component-a-ood.md | generic-architecture-design-harness | designer.md、verifier.md |
| component-a-execution.md | generic-deliberative-execution-harness | executor.md、reviewer.md |
| component-a-diagnosis.md | generic-problem-diagnosis-harness | diagnostician.md、challenger.md |
| component-a-requirement.md | generic-requirement-design-harness | designer.md、verifier.md |
| component-a-technical.md | generic-technical-design-harness | designer.md、verifier.md |



## 编排流程

### 1. 初始化

- 创建 `{workdir}/` 目录（如不存在）
- 将 `{requirement}` 写入 `{workdir}/requirement.md`
- 校验外部文件依赖：根据当前 `mode` 检查对应模式文件所引用的外部技能指令文件是否存在（如 mode=ood 时检查 `{this_skill_dir}/../generic-architecture-design-harness/designer.md` 和 `verifier.md`）。若任一文件不存在，直接向用户报错并终止
- 初始化迭代计数器 `M = 1`
- 初始化变量 `a_result_path = null`、`a_last_result_path = null`、`b_last_diag_path = null`、`b_last_challenge_path = null`
- 初始化变量 `edit_mode = false`
- 初始化变量 `iteration_history_path = null`（由 history-extractor 子Agent 维护的文件路径）
- 若提供了 `initial_artifact`，将其复制为 `{workdir}/a_v1_imported.md`，设置 `a_result_path = {workdir}/a_v1_imported.md`，设置 `skip_component_a = true`；否则 `skip_component_a = false`

### 2. 解析输入

若 `skip_component_a = true` 且 M = 1，跳过本步骤。

使用 Task 工具（`subagent_type: "general"`）启动 parser agent，prompt 结构：

```
请先阅读文件 {this_skill_dir}/parser.md 获取完整工作指令，然后按要求完成以下任务：

工作目录：{workdir}
用户需求文件：{workdir}/requirement.md
运行模式：{mode}
当前迭代轮次：{M}
{若 M > 1，追加：
上一轮组件B诊断报告：{b_last_diag_path}
{若 b_last_challenge_path 不为 null：上一轮组件B质询报告：{b_last_challenge_path}}
{若 b_last_challenge_path 为 null：上一轮组件B质询报告：无（质询未完成，跳过质询环节）}
上一轮组件A最终产出：{a_last_result_path}
历史迭代反馈文件：{iteration_history_path}}

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待 parser 返回，格式：

```
MODE:{mode}
REQUIREMENT_FOR_A:{为组件A准备的需求文件路径}
{若 M > 1：PREVIOUS_A_RESULT:{上一轮组件A最终产出路径}}
{若 M > 1 且上一轮结果超过1000行：EDIT_MODE:COPY_AND_EDIT}
```

解析返回信息，提取 `REQUIREMENT_FOR_A`（文件路径）。重置 `edit_mode = false`。若存在 `EDIT_MODE:COPY_AND_EDIT`：

- 将 `{a_last_result_path}` 复制为 `{workdir}/a_v{M}_copy_from_v{M-1}.md`
- 设置 `edit_mode = true`
- 后续组件A首轮子Agent的输出文件使用该副本路径

### 3. 运行组件A

若 `skip_component_a = true` 且 M = 1，跳过本步骤（`a_result_path` 已在初始化中设置）。

主Agent阅读 `{this_skill_dir}/component-a-{mode}.md`，扮演该技能的编排角色，逐环节启动子Agent执行工作。关键要点：

- 主Agent掌握组件A的内部审议循环节奏
- 按环节逐个调度子Agent
- 跟踪内部轮次计数器 `N` 和终止条件（最多 `{a_max_rounds}` 轮）
- 主Agent在整个编排过程中不读取任何运行时产出文件内容，仅传递文件路径。读取本技能目录内的指令文件除外
- 对于 M > 1，将 parser 返回的 `REQUIREMENT_FOR_A` 作为首轮需求传递给组件A的第一个子Agent（替代 requirement.md）
- 若 `edit_mode = true`，每轮子Agent均在副本上定向修改：N=1 使用 `{workdir}/a_v{M}_copy_from_v{M-1}.md`；N>1 由主Agent先将 N-1 轮输出复制为 N 轮输出文件路径再启动子Agent
- 组件A后续内部轮次（N>1，非 edit_mode）使用正常的文件命名规则

组件A运行结束后，记录其最终产出文件路径 `{a_result_path}`。

### 4. 运行组件B

主Agent阅读 `{this_skill_dir}/component-b.md`，扮演其编排角色。

组件B的任务描述：

```
请对指定产出进行质量审查，识别其中存在的问题：

用户需求：{workdir}/requirement.md

注意：该产出已通过组件A的内部审议（设计-验证/执行-审查等循环），内部审议已覆盖技术可行性等维度。你的审查应侧重内部审议未充分覆盖的维度，如需求响应充分度、整体深度和完整性等，避免重复验证内部审议已确认的维度。

当前迭代轮次：第 {M} 次
{若 M > 1 且 iteration_history_path 不为 null，追加：历史迭代反馈文件：{iteration_history_path}}

请从以下角度诊断：
1. 产出是否充分响应了用户需求
2. 产出中是否存在事实错误或逻辑矛盾
3. 产出的深度和完整性是否满足后续使用需要
```

阅读 `component-b.md` 的"各模式审查视角"章节，根据当前 `mode` 在任务描述中追加对应的审查视角说明。

按照 `component-b.md` 流程（内部审议循环最多 `{b_max_rounds}` 轮），逐环节启动子Agent。组件B运行结束后，记录：

- **最终诊断报告路径** `{b_diag_result_path}`：最后产出的诊断文件路径
- **最终质询报告路径** `{b_challenge_result_path}`：最后产出的质询文件路径
- **内部循环实际轮次** `b_actual_rounds`：循环终止时的 N 值

### 5. 判定

使用 Task 工具（`subagent_type: "general"`）启动 judge agent，prompt 结构：

```
请先阅读文件 {this_skill_dir}/judge.md 获取完整工作指令，然后按要求完成以下任务：

组件B诊断报告文件：{b_diag_result_path}
{若质询报告文件存在：组件B质询报告文件：{b_challenge_result_path}}
{若质询报告文件不存在：组件B质询报告：无（质询未完成，仅依据诊断报告判定）}
组件B内部循环最大轮次：{b_max_rounds}
组件B内部循环实际轮次：{b_actual_rounds}
判定输出文件：{workdir}/iteration_v{M}.md

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待 judge 返回：

- `PASS:{路径}` → 进入步骤 6
- `RETRY:{路径}` → 更新 `a_last_result_path ← a_result_path`、`b_last_diag_path ← b_diag_result_path`、`b_last_challenge_path ← b_challenge_result_path`，启动 history-extractor 子Agent（见下方），`M++`，若 M > max_iterations 则进入步骤 6，否则回到步骤 2

### 5.1 追加迭代历史（仅 RETRY 时执行）

使用 Task 工具（`subagent_type: "general"`）启动 history-extractor agent，prompt 结构：

```
请先阅读文件 {this_skill_dir}/history-extractor.md 获取完整工作指令，然后按要求完成以下任务：

判定报告文件：{workdir}/iteration_v{M}.md
当前迭代轮次：{M}
迭代历史文件：{workdir}/iteration_history.md

注意：你的返回结果中不要包含产出文件的内容摘要或节选，主Agent不会阅读文件内容，只会将文件路径转发给相关方。
```

等待返回，格式为 `HISTORY_WRITTEN:{路径}`。设置 `iteration_history_path = {路径}`。若返回异常，直接继续后续流程。

### 6. 向用户返回结果

仅返回以下信息：

- 通过时：`再审议完成，最终产出：{a_result_path}`
- 未通过时（达到最大迭代次数）：`再审议未通过。最终产出：{a_last_result_path}，诊断报告：{b_last_diag_path}，质询报告：{b_last_challenge_path}（若为 null 则注明"无"）`

**不要**将产出内容输出到对话中，仅返回文件路径。

### 异常处理

各步骤中子Agent异常处理（主Agent不读取文件内容，仅检查文件路径是否存在）：

- **返回非预期格式**：直接重新调用该子Agent
- **输出文件路径不存在**：同上
- **连续两次异常**：终止当前组件的内部循环，使用已有产出继续后续流程
- **组件A首轮（M=1）连续两次异常且无已有产出**：直接终止整个再审议流程，向用户报错
- **parser/judge/history-extractor 连续两次异常**：终止整个再审议流程，返回已有最佳产出
- **组件B质询文件缺失**：若质询agent异常未产出文件，直接终止组件B内部循环，以已有审查报告继续后续流程；judge agent 收到空质询路径时，仅依据审查报告判定
- **edit_mode 下副本覆写异常**：edit_mode=true 时子agent在副本上覆写。N=1 副本内容丢失可从原始文件（`a_v{M-1}_*`）恢复，N>1 副本丢失可从 N-1 轮输出恢复。需重新创建副本后再启动子Agent
