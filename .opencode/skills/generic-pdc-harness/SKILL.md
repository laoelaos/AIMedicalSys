---
name: generic-pdc-harness
description: "审议式通用 PDC 循环技能，以 Plan-Do-Check 渐进式规划驱动任意任务逐步推进。"
---

## 概述

```
Planner ↔ Plan Reviewer    (审议)
Doer     ↔ Do Reviewer     (审议)
Checker  ↔ Check Reviewer  (审议)
```

主Agent：加载本SKILL的Agent，仅负责调度子Agent执行各步骤，不参与任何环节的执行。

输入：完整任务描述 → 输出：任务产出（形式不限）。每个环节最多 12 轮审议式审查。

## 硬性约束

- 步骤 A–H 每一步必须独立启动 agent，严禁将多个步骤合并到一次调用
- 生产者与审查员必须在不同的 agent 中执行，不得角色兼任
- agent 间只传文件路径，不传内容
- 主Agent仅做路由、N++ 轮次推进和步骤中规定的 git 提交，禁止读取子Agent产出文件或参与任何环节执行，仅凭返回状态标记路由
- 整个管线在与 workdir 同名的分支上运行，初始化时创建，运行期间不得切换

子agent指令（启动时让agent自行阅读）：

- [planner](./planner.md) · [plan_reviewer](./plan_reviewer.md)
- [doer](./doer.md) · [do_reviewer](./do_reviewer.md)
- [checker](./checker.md) · [check_reviewer](./check_reviewer.md)

## 参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `workdir` | 是 | 工作目录绝对路径，命名：`Harness/pdc/{YYYYMMDDHHMM}_{简要描述}/`，`{YYYYMMDDHHMM}` 由 `date +%Y%m%d%H%M` 生成 |
| `task` | 是 | 完整任务描述，初始化时写入 `{workdir}/task.md` |
| `max_rounds` | 否 | 最大 PDC 轮次，默认 30 |

## 文件约定

| 文件 | 维护者 |
|------|--------|
| `{workdir}/task.md` | 主Agent，初始化时写入 |
| `{workdir}/plan.md` | Planner |
| `{workdir}/task_v{N}.md` | Planner |
| `{workdir}/plan_review_v{N}_r{R}.md` | Plan Reviewer |
| `{workdir}/do_v{N}.md` | Doer |
| `{workdir}/do_review_v{N}_r{R}.md` | Do Reviewer |
| `{workdir}/check_v{N}.md` | Checker |
| `{workdir}/check_review_v{N}_r{R}.md` | Check Reviewer |

workdir 中的其他文件均为产出，由 Doer 创建和修改，所有 agent 可读。

### workdir 命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

workdir 应位于工作区的 `Harness/pdc/` 目录下，命名格式为 `Harness/pdc/{YYYYMMDDHHMM}_{简要描述}/`。

## 审议循环规则

步骤 A-B、C-D、E-F 均遵循相同规则：

1. 生产者写出产出文件，返回 `{TOKEN_WRITTEN}:{路径}`
2. 审查员审查，返回 `APPROVED` 或 `REJECTED`
3. `APPROVED` → 进入下一环节
4. `REJECTED` 且 `R < 12` → `R++`，回到生产者修订
5. `REJECTED` 且 `R ≥ 12` → 主Agent 执行 `git add -A && git commit -m "v{N} blocked"` → `N++`，若 N > max_rounds 进入步骤 3，否则回到步骤 A（Planner 在步骤 A 中识别审议超限并自行写入 BLOCKED 记录到 plan.md）

### Token 映射表

| 环节 | 生产者返回 Token | 审查员返回 Token |
|------|-----------------|-----------------|
| Plan（步骤 A-B） | `TASK_ASSIGNED:{路径}` 或 `ALL_DONE:{路径}:{已完成任务数}` | `APPROVED:{路径}` / `REJECTED:{路径}` |
| Do（步骤 C-D） | `DO_DONE:{路径}` | `APPROVED:{路径}` / `REJECTED:{路径}` |
| Check（步骤 E-F） | `CHECK_PASSED:{路径}` / `CHECK_FAILED:{路径}` | `APPROVED:{路径}` / `REJECTED:{路径}` |

## 编排流程

### 1. 初始化

创建 `{workdir}/`，将 `{task}` 写入 `{workdir}/task.md`。`N = 1`。以 workdir 路径最后一段为分支名，执行 `git checkout -b {branch}`。

### 2. PDC 循环（最多 max_rounds 轮）

**步骤 A — Planner** → **步骤 B — Plan Reviewer**

`PR = 1`。按审议循环规则执行。

Planner prompt（`subagent_type: "general"`）：
```
请先阅读 {this_skill_dir}/planner.md 获取指令，然后：
任务描述：{workdir}/task.md
计划文件：{workdir}/plan.md
任务输出：{workdir}/task_v{N}.md
工作目录：{workdir}
{PR > 1 时追加：审查文件：{workdir}/plan_review_v{N}_r{PR-1}.md 审议修订时覆写原产出文件，禁止创建新版本号文件}
{N > 1 时追加：上一轮产出（部分文件可能因上一轮审议超限而不存在，不存在则跳过）：{workdir}/do_v{N-1}.md, {workdir}/check_v{N-1}.md}
{审议超限时追加：上一轮{超限环节}审议达到 12 轮上限，请在 plan.md 中记录 BLOCKED 并尝试绕过}
```

返回 `TASK_ASSIGNED:{路径}` 继续，或 `ALL_DONE:{路径}:{已完成任务数}` → 主Agent 执行 `git add -A && git commit -m "all done"` 后跳到步骤 3。

Plan Reviewer prompt：
```
请先阅读 {this_skill_dir}/plan_reviewer.md 获取指令，然后：
任务描述：{workdir}/task.md
计划文件：{workdir}/plan.md
任务文件：{workdir}/task_v{N}.md
审查输出：{workdir}/plan_review_v{N}_r{PR}.md
工作目录：{workdir}
{N > 1 时追加：上一轮产出（部分文件可能因上一轮审议超限而不存在，不存在则跳过）：{workdir}/do_v{N-1}.md, {workdir}/check_v{N-1}.md}
```

---

**步骤 C — Doer** → **步骤 D — Do Reviewer**

`DR = 1`。按审议循环规则执行。

Doer prompt（`subagent_type: "general"`）：
```
请先阅读 {this_skill_dir}/doer.md 获取指令，然后：
任务描述：{workdir}/task.md
任务文件：{workdir}/task_v{N}.md
执行报告输出：{workdir}/do_v{N}.md
工作目录：{workdir}
注意：不要创建、修改或删除除当前轮次执行报告（do_v{N}.md）以外的状态文件（task.md、plan.md、task_v*.md、*_review_*.md、do_v*.md、check_v*.md），这些是 PDC 流程的协调文件，不是产出物。当前轮次执行报告是你的合法产出，不在禁止范围内。
{DR > 1 时追加：审查文件：{workdir}/do_review_v{N}_r{DR-1}.md 审议修订时覆写原产出文件，禁止创建新版本号文件}
```

Do Reviewer prompt：
```
请先阅读 {this_skill_dir}/do_reviewer.md 获取指令，然后：
任务描述：{workdir}/task.md
任务文件：{workdir}/task_v{N}.md
执行报告：{workdir}/do_v{N}.md
审查输出：{workdir}/do_review_v{N}_r{DR}.md
工作目录：{workdir}
```

---

**步骤 E — Checker** → **步骤 F — Check Reviewer**

`CR = 1`。按审议循环规则执行。

Checker prompt（`subagent_type: "general"`）：
```
请先阅读 {this_skill_dir}/checker.md 获取指令，然后：
任务描述：{workdir}/task.md
任务文件：{workdir}/task_v{N}.md
执行报告：{workdir}/do_v{N}.md
检查报告输出：{workdir}/check_v{N}.md
工作目录：{workdir}
{CR > 1 时追加：审查文件：{workdir}/check_review_v{N}_r{CR-1}.md 审议修订时覆写原产出文件，禁止创建新版本号文件}
```

Check Reviewer prompt：
```
请先阅读 {this_skill_dir}/check_reviewer.md 获取指令，然后：
任务描述：{workdir}/task.md
任务文件：{workdir}/task_v{N}.md
执行报告：{workdir}/do_v{N}.md
检查报告：{workdir}/check_v{N}.md
审查输出：{workdir}/check_review_v{N}_r{CR}.md
工作目录：{workdir}
```

---

**步骤 G — 提交**

Checker 审议通过后，主Agent 执行 `git add -A && git commit -m "v{N} done"`。

**步骤 H — 轮次推进**

`N++`，回到步骤 A。若 N > max_rounds 进入步骤 3。

### 3. 返回结果

- 正常：`任务已完成，计划文件：{workdir}/plan.md，共完成 {从 ALL_DONE token 提取的已完成任务数} 个任务，分支：{branch}`
- 达上限：`达到最大轮次限制，计划文件：{workdir}/plan.md，分支：{branch}`

仅返回文件路径和统计信息，不输出内容到对话。