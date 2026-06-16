# 再审议需求 - OOD Phase 0 文档问题定位

## 任务背景

用户已交付 OOD 设计文档 `Docs/04_ood_phase0.md`（Phase 0 最小化骨架 — 架构级 OOD 设计方案）。本任务对该 OOD 文档进行问题定位。

## 任务目标

定位 `Docs/04_ood_phase0.md` OOD 文档中的问题，包括：

1. **定义矛盾**：文档自身不同段落/章节之间的定义、术语、规则不一致
2. **事实错误**：与公认事实、Maven/Spring Boot/Vue 3 等技术规范不符的内容
3. **逻辑错误**：依赖方向、装配条件、状态机推断等技术逻辑不自洽之处
4. **偏离需求文档**：与 `Docs/01_requirement.md` 需求规格说明书的角色、权限矩阵、业务边界、数据契约等不一致
5. **偏离路线图 Phase 0 阶段**：与 `Docs/03_roadmap.md` 中 Phase 0 阶段的范围、交付物、明确不包含事项等不一致；或越界到 Phase 1+ 才应落地的内容

## 诊断对象

`Docs/04_ood_phase0.md` — 用户的 OOD 设计文档，是被诊断对象

## 参考基线

- **`Docs/01_requirement.md`**：需求规格说明书（角色与权限矩阵 2.6、功能需求 3.1-3.3、AI 能力 3.4.x、数据需求 5.x 等）
- **`Docs/03_roadmap.md`**：实现路线图（Phase 0 ~ Phase 6，重点是 Phase 0 的"骨架必备/推荐补齐/明确不包含"）

## 模式

再审议框架 / mode=diagnosis / 跳过组件A=false，组件A 真正运行诊断，组件B 审查诊断报告

## 上下文

用户已在消息中附上三份文档的当前可见内容。但因为 50KB 输出截断，三份文档可能未完全展示；子Agent 在需要时自行 `Read` 对应文件路径获取完整内容，无需主Agent 复制内容。

文档路径速查：
- 被诊断对象：`Docs/04_ood_phase0.md`
- 需求基线：`Docs/01_requirement.md`
- 路线图基线：`Docs/03_roadmap.md`
