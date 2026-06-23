# 计划审查报告（v7 r1）

## 审查结果
REJECTED

## 发现

### [严重] task_v7.md 中 relativePath 值存在系统性的路径计算错误

task_v7.md 给出的第（8）（9）项 relativePath 值与实际文件系统路径不符，若被后续环节直接采用，父 POM 解析失败将导致 `mvn compile` 无法通过。

| 子模块 | 移动后路径 | task 给出的 relativePath | 正确的 relativePath |
|--------|-----------|------------------------|-------------------|
| common-module-api | `modules/common-module/common-module-api/` | `../common-module/pom.xml` | `../pom.xml` |
| common-module-impl | `modules/common-module/common-module-impl/` | `../common-module/pom.xml` | `../pom.xml` |
| ai-api | `modules/ai/ai-api/` | `../ai/pom.xml` | `../pom.xml` |
| ai-impl | `modules/ai/ai-impl/` | `../ai/pom.xml` | `../pom.xml` |

**验证**：`modules/common-module/common-module-api/../pom.xml` = `modules/common-module/pom.xml`（聚合 POM）✓；而 `../common-module/pom.xml` = `modules/common-module/common-module/pom.xml`（路径不存在）✗

### [严重] application/common/integration 的 relativePath 不应修改

task 第（8）项要求将这三个模块的 `<relativePath>` 从 `../pom.xml` 改为 `./pom.xml`，但三个模块未移动，当前 `../pom.xml` 已正确指向根 POM（`backend/application/../pom.xml` = `backend/pom.xml`）。改为 `./pom.xml` 将导致指向自身（`backend/application/pom.xml` → 自引用），构建必失败。

### [严重] task 未指定 common-module-api/impl 和 ai-api/impl 的 parent artifactId 变更

task 约束节（第103行）明确要求"common-module-api/impl 和 ai-api/impl 的 parent 改为各自聚合 POM"，但 relativePath 节（第93-96行）只给出了 relativePath 的修改值，未提及 `<parent><artifactId>` 的变更：

- `common-module-api/pom.xml` 和 `common-module-impl/pom.xml`：`<artifactId>` 应从 `aimedical-sys` → `common-module`
- `ai-api/pom.xml` 和 `ai-impl/pom.xml`：`<artifactId>` 应从 `aimedical-sys` → `ai`

若只改 relativePath 不改 artifactId，Maven 会在 `aimedical-sys` 下寻找 `common-module/ai` 子模块 POM，与实际聚合 POM 的 artifactId 不匹配，构建失败。

## 修改要求

1. **修正 relativePath 值**：
   - common-module-api → `../pom.xml`
   - common-module-impl → `../pom.xml`
   - ai-api → `../pom.xml`
   - ai-impl → `../pom.xml`
   - application/common/integration → 不修改（保持 `../pom.xml`）

2. **补充 parent artifactId 变更**：common-module-api/impl 的 parent artifactId → `common-module`；ai-api/impl 的 parent artifactId → `ai`

3. 建议在 plan 或 task 中补充一条**关键验证步骤**：完成所有修改后，应在移动前的目录（backend/）下执行 `mvn validate -N` 和 `mvn validate`，确认 11/11 模块依赖解析全部通过，再执行 `mvn compile -DskipTests`
