# 设计审查报告（v9 r2）

## 审查结果
REJECTED

## 发现

### [一般] 父 POM `ignoredUnusedDeclaredDependency` 配置覆盖范围的错误断言

**问题**：设计「父 POM 遗漏依赖豁免」决策行中声称 "`backend/pom.xml:108-111` 已通过 `maven-dependency-plugin` 统一配置 ... 新模块直接继承无需重复配置"。实际审查发现，`backend/pom.xml:108-111` 仅包含两条配置：

```xml
<ignoredUnusedDeclaredDependency>com.aimedical:ai-api</ignoredUnusedDeclaredDependency>
<ignoredUnusedDeclaredDependency>com.aimedical:common-module-api</ignoredUnusedDeclaredDependency>
```

这三个新模块的 artifactId 为 `patient`、`doctor`、`admin`，**不在**上述忽略清单中。设计断言它们"直接继承无需重复配置"与事实不符。

**为什么是问题**：若 `application/pom.xml` 按设计要求的"为三个新模块添加 compile 范围依赖"，当运行 `mvn dependency:analyze` 时，`patient`/`doctor`/`admin` 将被报告为已声明未使用的依赖项（declared but unused），导致分析目标失败。

**期望修正方向**：
- 修正该决策行的描述，明确标注 `patient`/`doctor`/`admin` 需要被补充进父 POM 的 `<ignoredUnusedDeclaredDependency>` 列表中
- 或说明本项目不会绑定 `maven-dependency-plugin:analyze` 到生命周期，此配置仅供参考

### [轻微] 目录树描述与实际项目结构不一致

**问题**：设计「目录布局」节将现有模块展示为：

```
backend/
├── modules/
│   ├── ai/                          # AI 能力模块（已存在）
│   └── common-module/               # 公共业务模块（已存在）
```

但实际项目结构为根级扁平目录：

```
backend/
├── ai-api/
├── ai-impl/
├── common-module-api/
├── common-module-impl/
```

不存在 `modules/` 中间子目录，且 AI 与 common-module 各自拆分为 api/impl 两个独立模块。

**为什么是问题**：设计文档中的目录树是开发者理解项目布局的直接依据。此不一致可能导致新加入项目的开发者对现有模块位置产生错误认知。但该问题不影响对三个新模块（patient/doctor/admin）的实现，故评级为轻微。

**期望修正方向**：将目录树中的现有模块路径修正为与 `ls` 输出一致的实际扁平布局。

## 修改要求

### 针对 [一般] 问题

1. 修正设计决策表中关于 `ignoredUnusedDeclaredDependency` 的描述，明确 `patient`/`doctor`/`admin` 需要额外补充配置，或明确说明不绑定 `analyze` 目标到生命周期。

### 针对 [轻微] 问题

2. 更新目录树部分，使 `ai-api`/`ai-impl`/`common-module-api`/`common-module-impl` 的路径与实际项目结构一致。
