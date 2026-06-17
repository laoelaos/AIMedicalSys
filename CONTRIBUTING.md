# Contribution Guide

## 1. 适用范围

本文件是 `AIMedicalSys` 仓库的统一协作规范，合并承载以下内容：

- 分支约定
- Commit 格式
- Pull Request 提交要求
- Code Review 必查项

Phase 0 目标是保证多人并行开发时的最小协作一致性，不扩展到发布流程、版本管理策略或 Phase 1+ 的业务开发细则。

## 2. 分支约定

### 2.1 基础分支

- `main`：稳定主分支，仅合入通过评审和基础验证的变更
- `develop`：日常集成分支，用于汇总各功能分支改动

### 2.2 功能分支命名

从 `develop` 切出，命名格式统一为：

```text
type/short-description
```

推荐 `type`：

- `feat`：新功能或新模块骨架
- `fix`：缺陷修复
- `docs`：文档修改
- `refactor`：重构
- `test`：测试补充或调整
- `chore`：构建、脚本、依赖、目录治理

示例：

- `feat/backend-common-skeleton`
- `docs/phase0-quickstart`
- `chore/frontend-workspaces`

### 2.3 分支使用规则

- 不直接向 `main` 提交代码
- 非紧急情况下也不直接向 `develop` 提交代码
- 所有改动通过 Pull Request 合入
- 单个分支只处理一个明确主题，避免混入无关改动

## 3. Commit 格式

Commit message 使用以下格式：

```text
<type>: <summary>
```

示例：

- `feat: add phase0 backend module skeleton`
- `fix: correct ai mock bean wiring`
- `docs: add phase0 quickstart guide`

约束如下：

- `summary` 使用简洁英文短句
- 首行尽量控制在 72 个字符内
- 一次 commit 只表达一个完整意图
- 不提交与当前任务无关的格式化噪音

推荐 `type`：

- `feat`
- `fix`
- `docs`
- `refactor`
- `test`
- `chore`

## 4. Pull Request 要求

- PR 必须说明改动目的、改动范围和验证方式
- PR 不应混入无关文件修改
- PR 标题应与主要改动一致，避免使用模糊标题
- 若改动涉及 OOD、Roadmap 或技术基线，需在描述中标明对应文档路径
- 合并前至少完成一次自检

PR 模板文件路径：`.github/pull_request_template.md`

## 5. Code Review 必查项

提交 PR 前自检，Review 时也按以下清单执行：

- 改动是否符合 `Docs/04_ood_phase0.md` 当前冻结边界
- 是否引入跨模块直接依赖，破坏既定依赖方向
- 是否修改了不属于当前任务的文件
- 是否补充了必要的文档或配置说明
- 后端接口是否遵循 `Result<T>`、错误处理和分页契约
- 前端是否遵循 workspace 包引用方式，未绕过共享层直接复制逻辑
- 是否包含最小验证步骤，且验证结果可复现
- 命名、目录结构、包路径是否与现有约定一致

## 6. Phase 0 最小提交流程

1. 从 `develop` 切出功能分支。
2. 完成单一主题改动。
3. 本地完成最小验证。
4. 按规范提交 commit。
5. 发起 PR 到 `develop`。
6. 完成 Review 后合入。

## 7. 本地验证最低要求

Phase 0 期间，按改动类型至少满足以下之一：

- 文档改动：检查文档路径、章节引用和命令示例是否一致
- 后端改动：能启动后端，或至少能完成相关 Maven 构建/测试命令
- 前端改动：能完成 `npm ci` 和对应 workspace 的构建或启动验证
- 骨架联动改动：确认 `/api/ping` 和三端占位页路径未被破坏

## 8. 冲突处理原则

- 与 `Docs/04_ood_phase0.md` 冲突时，优先以 OOD 冻结口径为准
- 与后续阶段设计冲突时，不提前引入 Phase 1+ 实现细节
- 若发现规范与仓库现状不一致，先修正文档或配置，再继续扩展实现
