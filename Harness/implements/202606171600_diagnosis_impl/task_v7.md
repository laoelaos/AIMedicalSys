# 任务指令（v7）

## 动作
NEW

## 任务描述
**目录结构调整**（问题2/3）：将后端 Maven 多模块从扁平目录布局迁移至 OOD §2.1 定义的分层布局，并补齐缺失的聚合 POM。

具体变更：
1. 创建 `backend/modules/` 中间目录
2. 移动 `patient`、`doctor`、`admin` → `modules/patient`、`modules/doctor`、`modules/admin`
3. 移动 `common-module-api`、`common-module-impl` → `modules/common-module/common-module-api`、`modules/common-module/common-module-impl`
4. 移动 `ai-api`、`ai-impl` → `modules/ai/ai-api`、`modules/ai/ai-impl`
5. 创建 `modules/common-module/pom.xml`（聚合 POM，聚合 common-module-api + common-module-impl）
6. 创建 `modules/ai/pom.xml`（聚合 POM，聚合 ai-api + ai-impl）
7. 更新 `backend/pom.xml` 的 `<modules>` 声明：所有模块路径改为分层路径
8. 更新 `common-module-api/pom.xml` 和 `common-module-impl/pom.xml`：`<relativePath>` 指向 `../pom.xml`（即对应聚合 POM `modules/common-module/pom.xml`）
9. 更新 `ai-api/pom.xml` 和 `ai-impl/pom.xml`：`<relativePath>` 指向 `../pom.xml`（即对应聚合 POM `modules/ai/pom.xml`）
10. 更新 `patient/pom.xml`、`doctor/pom.xml`、`admin/pom.xml`：`<relativePath>` 指向 `../../pom.xml`（即根 POM）
11. 更新 `common-module-api/pom.xml` 和 `common-module-impl/pom.xml`：`<parent><artifactId>` 从 `aimedical-sys` 改为 `common-module`
12. 更新 `ai-api/pom.xml` 和 `ai-impl/pom.xml`：`<parent><artifactId>` 从 `aimedical-sys` 改为 `ai`

预期文件路径：`AIMedical/backend/pom.xml`, `AIMedical/backend/modules/common-module/pom.xml`, `AIMedical/backend/modules/ai/pom.xml`, `AIMedical/backend/modules/patient/pom.xml`, `AIMedical/backend/modules/doctor/pom.xml`, `AIMedical/backend/modules/admin/pom.xml`, `AIMedical/backend/modules/common-module/common-module-api/pom.xml`, `AIMedical/backend/modules/common-module/common-module-impl/pom.xml`, `AIMedical/backend/modules/ai/ai-api/pom.xml`, `AIMedical/backend/modules/ai/ai-impl/pom.xml`

## 选择理由
问题2/3 是诊断报告 10 项问题中唯二尚未实现的任务。所有代码缺陷修复（问题8/10）和 POM 配置对齐（问题5/6/7）已在 R1-R6 完成。目录结构调整是纯结构性变更，不涉及 Java 代码逻辑修改，可通过 `mvn compile` 验证正确性。需要聚合 POM 以支持子树独立构建（OOD §2.1 的 `mvn -f modules/ai/pom.xml -am install` 等命令）。

## 任务上下文
### OOD §2.1 要求的分层布局（摘录）
```
backend/
├── pom.xml
├── application/
├── common/
├── modules/
│   ├── patient/
│   ├── doctor/
│   ├── admin/
│   ├── common-module/
│   │   ├── pom.xml
│   │   ├── common-module-api/
│   │   └── common-module-impl/
│   └── ai/
│       ├── pom.xml
│       ├── ai-api/
│       └── ai-impl/
└── integration/
```

### 聚合 POM 骨架（OOD §2.1）
`modules/common-module/pom.xml`：
- parent: com.aimedical:aimedical-sys (version ${project.version})
- relativePath: ../../pom.xml
- artifactId: common-module
- packaging: pom
- modules: common-module-api, common-module-impl

`modules/ai/pom.xml`：
- parent: com.aimedical:aimedical-sys (version ${project.version})
- relativePath: ../../pom.xml
- artifactId: ai
- packaging: pom
- modules: ai-api, ai-impl

### 根 POM `<modules>` 声明更新
将当前扁平路径：
```
<module>common</module>
<module>common-module-api</module>
<module>common-module-impl</module>
<module>ai-api</module>
<module>ai-impl</module>
<module>patient</module>
<module>doctor</module>
<module>admin</module>
<module>application</module>
<module>integration</module>
```
改为分层路径：
```
<module>common</module>
<module>modules/common-module/common-module-api</module>
<module>modules/common-module/common-module-impl</module>
<module>modules/ai/ai-api</module>
<module>modules/ai/ai-impl</module>
<module>modules/patient</module>
<module>modules/doctor</module>
<module>modules/admin</module>
<module>application</module>
<module>integration</module>
```

### relativePath 更新
- `application/pom.xml`, `common/pom.xml`, `integration/pom.xml`：**不修改**，保持现有 `<relativePath>../pom.xml`（它们仍位于 backend/ 根级不移动，`../pom.xml` 已正确指向根 POM `backend/pom.xml`）
- `common-module-api/pom.xml`：当前 `<relativePath>../pom.xml</relativePath>` → `../pom.xml`（父为 common-module 聚合 POM，位于 `modules/common-module/pom.xml`；`modules/common-module/common-module-api/../pom.xml` = `modules/common-module/pom.xml` ✓）
- `common-module-impl/pom.xml`：当前 `<relativePath>../pom.xml</relativePath>` → `../pom.xml`
- `ai-api/pom.xml`：当前 `<relativePath>../pom.xml</relativePath>` → `../pom.xml`（父为 ai 聚合 POM，位于 `modules/ai/pom.xml`；`modules/ai/ai-api/../pom.xml` = `modules/ai/pom.xml` ✓）
- `ai-impl/pom.xml`：当前 `<relativePath>../pom.xml</relativePath>` → `../pom.xml`
- `patient/pom.xml`、`doctor/pom.xml`、`admin/pom.xml`：当前 `<relativePath>../pom.xml</relativePath>` → `../../pom.xml`

### parent artifactId 变更
- `common-module-api/pom.xml` 和 `common-module-impl/pom.xml`：`<parent><artifactId>` 从 `aimedical-sys` 改为 `common-module`（对应的 `<relativePath>` 为 `../pom.xml` 指向聚合 POM `modules/common-module/pom.xml`）
- `ai-api/pom.xml` 和 `ai-impl/pom.xml`：`<parent><artifactId>` 从 `aimedical-sys` 改为 `ai`（对应的 `<relativePath>` 为 `../pom.xml` 指向聚合 POM `modules/ai/pom.xml`）
- `patient/pom.xml`、`doctor/pom.xml`、`admin/pom.xml`：保持 `<parent><artifactId>aimedical-sys</artifactId></parent>` 不变，`<relativePath>` 改为 `../../pom.xml` 仍指向根 POM
- `application/pom.xml`、`common/pom.xml`、`integration/pom.xml`：不做任何修改（保持 `<parent><artifactId>aimedical-sys</artifactId></parent>` 和 `<relativePath>../pom.xml</relativePath>`）

### 约束
- 不修改任何 Java 源代码（`src/main/java/com/aimedical/` 下的包路径不变）
- 不修改 application/common/integration 模块的 POM（它们保持在 backend/ 根级不变）
- 移动后 `mvn compile -DskipTests` 必须全部通过
- 保留各子模块原有的 parent artifactId（仍为 `aimedical-sys`，不变；仅 common-module-api/impl 和 ai-api/impl 的 parent 改为各自聚合 POM）
- 每个聚合 POM 仅聚合其子模块，不引入新依赖或 plugin

### 关键验证步骤
完成所有修改后，在 `backend/` 目录下顺序执行：
1. `mvn validate -N`（验证父 POM 自身解析）
2. `mvn validate`（验证全部 11/11 模块依赖解析）
3. `mvn compile -DskipTests`（确认编译全部通过）

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| common-module-api/impl 和 ai-api/impl 的 relativePath 路径计算错误（`../common-module/pom.xml` → 实际应为 `../pom.xml`） | 修正 items 8/9 中 relativePath 值；修正 relativePath 节中对应值 |
| application/common/integration 的 relativePath 不应修改（`./pom.xml` 导致自引用） | 删除该条修改要求，标注"不修改，保持现有 `../pom.xml`" |
| 未指定 common-module-api/impl 和 ai-api/impl 的 parent artifactId 变更 | 新增 items 11/12；新增 parent artifactId 变更节 |
| 建议补充验证步骤 | 在约束后添加关键验证步骤 |
