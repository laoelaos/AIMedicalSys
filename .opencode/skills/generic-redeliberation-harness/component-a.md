# 组件A — 审议式技能执行

## 概述

组件A是一个审议式技能的执行过程。主Agent扮演所选技能的编排角色，按照其内部审议循环逐环节启动子Agent完成工作、产出文件。

## 可用模式

| 模式 | 技能 | 流程文档 |
|------|------|---------|
| ood | 审议式OOD设计 | component-a-ood.md |
| execution | 审议式通用执行 | component-a-execution.md |
| diagnosis | 审议式问题定位 | component-a-diagnosis.md |
| requirement | 审议式需求设计 | component-a-requirement.md |
| technical | 审议式技术设计 | component-a-technical.md |

> **注**：`generic-deliberative-implementation-harness`（审议式实现管线）因其四阶段管线结构与本框架的双Agent循环编排模式不兼容，未纳入可用模式。

## 外部文件依赖

各模式文件通过相对路径引用对应技能的子Agent指令文件（如 `designer.md`、`verifier.md`、`executor.md` 等），具体引用关系见 SKILL.md「外部文件依赖」章节。若被引用技能的目录结构发生变化，组件A将在运行时静默失败。

## 主Agent的编排职责

1. **掌握流程节奏**：按照对应模式的审议循环推进各环节
2. **逐环节调度子Agent**：每个环节启动一个子Agent执行具体工作
3. **跟踪轮次**：维护内部审议轮次计数器 N，判断终止条件（最多 `{a_max_rounds}` 轮）
4. **不读取运行时产出文件内容**：主Agent在整个编排过程中不读取任何运行时产出文件内容，仅传递文件路径。读取本技能目录内的指令文件除外
5. **记录最终产出路径**：循环结束后记录最终产出文件的路径

## 迭代模式（M > 1）

当组件A因组件B不通过而重新运行时：

- 主Agent将 parser 返回的 `REQUIREMENT_FOR_A` 作为首轮需求，替代原始 requirement.md 传递给第一个子Agent
- 若 `edit_mode = true`，主Agent已将上一轮最终产出复制为副本 `{workdir}/a_v{M}_copy_from_v{M-1}.md`
  - 每轮子Agent均在副本上定向修改：N=1 使用该副本路径；N>1 由主Agent先将 N-1 轮输出复制为 N 轮输出文件路径，再启动子Agent
  - 每轮子Agent的 prompt 中追加定向修改指令：N=1 时强调"输出文件已有内容，先阅读再修改，不要从头重写"；N>1 时追加"基于已有内容定向修改"
- 后续内部审议轮次（N>1）不受迭代影响，使用正常的文件命名规则
