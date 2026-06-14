---
name: generic-lint-governance-harness
description: "采用「检查-计划-执行」审议循环，对代码库进行渐进式lint治理。"
---

## 框架概述

三agent审议循环：checker运行cjlint诊断问题，planner形成增量治理计划，executor逐批修复。每轮聚焦一批问题，由checker独立验证修复效果，形成「诊断→计划→修复→验证」闭环。主Agent仅编排路由，禁止读取子Agent产出文件。

子agent指令文档（启动时让agent自行阅读，不要复制到prompt中）：

- [checker](./checker.md)：cjlint运行、结果解析与报告规范
- [planner](./planner.md)：增量治理策略与计划文档维护规范
- [executor](./executor.md)：修复执行与变更报告规范

## 工具适配说明

本技能以**目标语言工具链中的 lint 工具**（默认 `cjlint`）为执行入口。skill 主体框架（检查→计划→执行循环、报告规范、治理跟踪）可适用于任何支持 lint 工具链的目标语言，**实际命令与规则代码因语言而异**：

- 仓颉：`cjlint`（规则代码 `!G.ERR.01`、`!G.FUN.01` 等）
- 其他语言：替换为对应 `lint-tool`、其规则 ID 与抑制语法

`lint_guidelines.md` 中的抑制语法示例以仓颉语法展示，非仓颉项目需按目标语言规范改写。调用本技能时，应通过 `lint_args` 参数传入目标语言 lint 工具的实际命令与参数。

## 硬性约束

- 步骤 A–D 每步独立启动agent，严禁合并
- agent间只传文件路径，不传内容
- 主Agent仅做路由，禁止读取子Agent产出文件或参与执行
- 管线在与workdir同名的分支上运行（取路径最后一段），初始化时创建，运行期间不切换

## 调用参数

- **`workdir`**（必填）— 工作目录绝对路径，中间文件直接写入
- **`project_root`**（必填）— 项目根目录绝对路径
- **`lint_args`**（必填）— cjlint额外参数，默认 `-f [要检查的代码路径]/`
- **`max_rounds`**（可选）— 最大迭代轮次，默认 20

### workdir命名规范

运行 `date +%Y%m%d%H%M` 得到 `{YYYYMMDDHHMM}`。

位于工作区 `Harness/governances/` 目录下，格式 `Harness/governances/{YYYYMMDDHHMM}_{简要描述}/`。

## 文件约定

中间文件写入 `{workdir}/`：

| 文件 | 维护者 |
|------|--------|
| `governance.md` | Planner，活文档跟踪治理进度 |
| `lint_v{N}.md` | Checker，检查报告（N从1递增） |
| `plan_v{N}.md` | Planner，治理计划 |
| `exec_v{N}.md` | Executor，执行报告 |

源码修改直接操作 `{project_root}/` 目录树。

## 编排流程

### 1. 初始化

- 创建 `{workdir}/` 目录（如不存在）
- 初始化 `N = 1`
- 以workdir路径最后一段为分支名：`git checkout -b {branch}`

### 2. 治理循环（最多 max_rounds 轮）

每轮 A → B → C → D：

**步骤 A — 检查agent**

```
请先阅读文件 {this_skill_dir}/checker.md 获取完整工作指令，然后按要求完成以下任务：

项目根目录：{project_root}
lint参数：{lint_args}
检查报告输出文件：{workdir}/lint_v{N}.md
```

返回格式：
- `ISSUES_FOUND:{检查报告路径}` → 继续步骤B
- `CLEAN:{检查报告路径}` → 跳到步骤3

**步骤 B — 计划agent**

```
请先阅读文件 {this_skill_dir}/planner.md 获取完整工作指令，然后按要求完成以下任务：

当前检查报告：{workdir}/lint_v{N}.md
治理跟踪文件：{workdir}/governance.md
计划输出文件：{workdir}/plan_v{N}.md
lint指导原则文件：{this_skill_dir}/lint_guidelines.md
项目根目录：{project_root}
{若 N > 1，追加：上一轮执行报告：{workdir}/exec_v{N-1}.md}
```

返回格式：
- `PLAN_ASSIGNED:{计划文件路径}` → 继续步骤C
- `NO_ACTIONABLE_PLAN:{治理跟踪文件路径}` → 跳到步骤3

**步骤 C — 执行agent**

```
请先阅读文件 {this_skill_dir}/executor.md 获取完整工作指令，然后按要求完成以下任务：

治理计划文件：{workdir}/plan_v{N}.md
执行报告输出文件：{workdir}/exec_v{N}.md
lint指导原则文件：{this_skill_dir}/lint_guidelines.md
项目根目录：{project_root}
当前轮次：v{N}
```

返回 `EXEC_DONE:{执行报告文件路径}`。Executor负责修复、报告并提交（`git add -A && git commit -m "v{N} lint fix"`）。

**步骤 D — 推进**

`N++`，回到步骤A。若 `N > max_rounds` → 步骤3。

### 3. 返回结果

- 通过检查：`Lint治理完成，代码库已通过cjlint检查。治理跟踪：{workdir}/governance.md，共经历 {N} 轮，分支：{branch}`
- 达到轮次上限：`达到最大轮次限制，治理跟踪：{workdir}/governance.md，最新检查报告：{workdir}/lint_v{N}.md，分支：{branch}`
- 无可自动治理的问题：`剩余lint问题无法自动修复，治理跟踪：{workdir}/governance.md，最新检查报告：{workdir}/lint_v{N}.md，分支：{branch}`

不要将检查报告、计划或执行内容输出到对话中，仅返回文件路径和统计信息。
