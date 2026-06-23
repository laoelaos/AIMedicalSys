---
name: generic-deliberative-implementation-harness
description: "采用审议式「计划-设计-编码-验证」管线框架，将任务逐步实现为可运行的代码。"
---

## 概述

```
Planner ↔ Plan Reviewer   (审议)
Designer ↔ Design Reviewer (审议)
Coder    ↔ Code Reviewer   (审议)
Verifier ↔ Test Reviewer   (审议)
Runner
```

主Agent：加载本SKILL的Agent，仅负责调度子Agent执行各步骤，不参与任何环节的执行。

输入：整个任务的完整描述 → 输出：源码 + 单元测试。每个生产环节最多 3 轮审议式审查。

## 硬性约束

- 步骤 A–J 每一步必须独立启动 agent，严禁将多个步骤合并到一次调用
- 生产者与审查员必须在不同的 agent 中执行，不得角色兼任
- agent 间只传文件路径，不传内容
- 主Agent仅做路由，禁止读取子Agent产出文件或参与任何环节执行，仅凭返回状态标记路由
- 整个管线在与 workdir 同名的分支上运行，初始化时创建，运行期间不得切换

子agent指令（启动时让agent自行阅读）：

- [planner](./planner.md) · [plan_reviewer](./plan_reviewer.md)
- [designer](./designer.md) · [design_reviewer](./design_reviewer.md)
- [coder](./coder.md) · [code_reviewer](./code_reviewer.md)
- [verifier](./verifier.md) · [test_reviewer](./test_reviewer.md) · [runner](./runner.md)

## 参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `workdir` | 是 | 工作目录绝对路径，命名：`Harness/implements/{YYYYMMDDHHMM}_{简要描述}/`，`{YYYYMMDDHHMM}` 由 `date +%Y%m%d%H%M` 生成 |
| `requirement` | 是 | 整个任务的完整描述，初始化时写入 `{workdir}/requirement.md` |
| `project_root` | 是 | 项目根目录绝对路径 |
| `max_rounds` | 否 | 最大任务轮次，默认 30 |

## 文件约定

| 文件 | 维护者 |
|------|--------|
| `{workdir}/requirement.md` | 主Agent，初始化时写入 |
| `{workdir}/plan.md` | Planner |
| `{workdir}/task_v{N}.md` | Planner |
| `{workdir}/plan_review_v{N}_r{R}.md` | Plan Reviewer |
| `{workdir}/detail_v{N}.md` | Designer |
| `{workdir}/design_review_v{N}_r{R}.md` | Design Reviewer |
| `{workdir}/code_v{N}.md` | Coder |
| `{workdir}/code_review_v{N}_r{R}.md` | Code Reviewer |
| `{workdir}/test_v{N}.md` | Verifier |
| `{workdir}/test_review_v{N}_r{R}.md` | Test Reviewer |
| `{workdir}/verify_v{N}.md` | 主Agent |

源码和测试直接写入 `{project_root}/` 目录树。

### workdir 命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

workdir 应位于工作区的 `Harness/implements/` 目录下，命名格式为 `Harness/implements/{YYYYMMDDHHMM}_{简要描述}/`。

## 审议循环规则

步骤 A-B、C-D、E-F、G-H 均遵循相同规则：

1. 生产者写出产出文件，返回 `{TOKEN_WRITTEN}:{路径}`
2. 审查员审查，返回 `APPROVED` 或 `REJECTED`
3. `APPROVED` → 进入下一环节
4. `REJECTED` 且 `R < 12` → `R++`，回到生产者修订
5. `REJECTED` 且 `R ≥ 12` → plan.md 追加 BLOCKED 记录，`N++`，回到步骤 A

## 编排流程

### 1. 初始化

创建 `{workdir}/`，将 `{requirement}` 写入 `{workdir}/requirement.md`。`N = 1`。以 workdir 路径最后一段为分支名，执行 `git checkout -b {branch}`。

### 2. 管线循环（最多 max_rounds 轮）

**步骤 A — Planner** → **步骤 B — Plan Reviewer**

`PR = 1`。按审议循环规则执行。

Planner prompt（`subagent_type: "general"`）：
```
请先阅读 {this_skill_dir}/planner.md 获取指令，然后：
整个任务的完整描述：{workdir}/requirement.md
计划文件：{workdir}/plan.md
任务输出：{workdir}/task_v{N}.md
项目根目录：{project_root}
{PR > 1 时追加：审查文件：{workdir}/plan_review_v{N}_r{PR-1}.md 审议修订时覆写原产出文件，禁止创建新版本号文件}
{N > 1 时追加：上一轮最终产出：{workdir}/detail_v{N-1}.md, {workdir}/code_v{N-1}.md, {workdir}/test_v{N-1}.md, {workdir}/verify_v{N-1}.md}
```

返回 `TASK_ASSIGNED:{路径}` 继续，或 `ALL_DONE:{路径}` 跳到步骤 3。

Plan Reviewer prompt：
```
请先阅读 {this_skill_dir}/plan_reviewer.md 获取指令，然后：
整个任务的完整描述：{workdir}/requirement.md
计划文件：{workdir}/plan.md
任务文件：{workdir}/task_v{N}.md
审查输出：{workdir}/plan_review_v{N}_r{PR}.md
项目根目录：{project_root}
{N > 1 时追加：上一轮最终产出：{workdir}/detail_v{N-1}.md, {workdir}/code_v{N-1}.md, {workdir}/test_v{N-1}.md, {workdir}/verify_v{N-1}.md}
```

---

**步骤 C — Designer** → **步骤 D — Design Reviewer**

`DR = 1`。按审议循环规则执行。

Designer prompt：
```
请先阅读 {this_skill_dir}/designer.md 获取指令，然后：
整个任务的完整描述：{workdir}/requirement.md
任务文件：{workdir}/task_v{N}.md
设计输出：{workdir}/detail_v{N}.md
项目根目录：{project_root}
{DR > 1 时追加：审查文件：{workdir}/design_review_v{N}_r{DR-1}.md 审议修订时覆写原产出文件，禁止创建新版本号文件}
{N > 1 时追加：上一轮最终产出：{workdir}/detail_v{N-1}.md, {workdir}/code_v{N-1}.md, {workdir}/test_v{N-1}.md, {workdir}/verify_v{N-1}.md}
```

Design Reviewer prompt：
```
请先阅读 {this_skill_dir}/design_reviewer.md 获取指令，然后：
待审查设计：{workdir}/detail_v{N}.md
整个任务的完整描述：{workdir}/requirement.md
任务文件：{workdir}/task_v{N}.md
审查输出：{workdir}/design_review_v{N}_r{DR}.md
项目根目录：{project_root}
{N > 1 时追加：上一轮最终产出：{workdir}/detail_v{N-1}.md, {workdir}/code_v{N-1}.md, {workdir}/test_v{N-1}.md, {workdir}/verify_v{N-1}.md}
```

---

**步骤 E — Coder** → **步骤 F — Code Reviewer**

`CR = 1`。按审议循环规则执行。

Coder prompt：
```
请先阅读 {this_skill_dir}/coder.md 获取指令，然后：
详细设计：{workdir}/detail_v{N}.md
实现报告输出：{workdir}/code_v{N}.md
项目根目录：{project_root}
{CR > 1 时追加：审查文件：{workdir}/code_review_v{N}_r{CR-1}.md}
```

Code Reviewer prompt：
```
请先阅读 {this_skill_dir}/code_reviewer.md 获取指令，然后：
详细设计：{workdir}/detail_v{N}.md
实现报告：{workdir}/code_v{N}.md
审查输出：{workdir}/code_review_v{N}_r{CR}.md
项目根目录：{project_root}
```

---

**步骤 G — Verifier** → **步骤 H — Test Reviewer**

`TR = 1`。按审议循环规则执行。

Verifier prompt：
```
请先阅读 {this_skill_dir}/verifier.md 获取指令，然后：
详细设计：{workdir}/detail_v{N}.md
实现报告：{workdir}/code_v{N}.md
测试报告输出：{workdir}/test_v{N}.md
项目根目录：{project_root}
{TR > 1 时追加：审查文件：{workdir}/test_review_v{N}_r{TR-1}.md}
```

Test Reviewer prompt：
```
请先阅读 {this_skill_dir}/test_reviewer.md 获取指令，然后：
详细设计：{workdir}/detail_v{N}.md
实现报告：{workdir}/code_v{N}.md
测试报告：{workdir}/test_v{N}.md
审查输出：{workdir}/test_review_v{N}_r{TR}.md
项目根目录：{project_root}
```

---

**步骤 I — Runner**（`subagent_type: "general"`）

```
请先阅读 {this_skill_dir}/runner.md 获取指令，然后：
项目根目录：{project_root}
验证报告输出：{workdir}/verify_v{N}.md
当前轮次：v{N}
```

Runner 负责执行测试、写验证报告并提交（`git add -A && git commit -m "v{N} done"`）。主Agent 不做额外提交。

返回 `PASSED:{路径}` 或 `FAILED:{路径}`。

> ⚠ Runner 不是终点。无论 PASSED 或 FAILED，都必须执行步骤 J → 回到步骤 A。

**步骤 J — 轮次推进**

`N++`，回到步骤 A。达 max_rounds 进入步骤 3。

### 3. 返回结果

- 正常：`实现已完成，计划文件：{workdir}/plan.md，共完成 {已完成任务数} 个任务，分支：{branch}`
- 达上限：`达到最大轮次限制，计划文件：{workdir}/plan.md，最新验证报告：{workdir}/verify_v{N-1}.md，分支：{branch}`

仅返回文件路径和统计信息，不输出设计/代码/测试内容到对话。
