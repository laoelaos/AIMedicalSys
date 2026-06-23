# 设计审查报告（v9 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** 新模块 POM 缺少 `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa` 依赖声明。
  - 设计指定新模块依赖 `common`、`common-module-api`、`ai-api`，但 `common/pom.xml` 将 `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa` 声明为 `<optional>true</optional>`，不会透传。
  - 新模块需要 `@RestController`、`@RequestMapping`、`@Entity`、`JpaRepository`（均来自上述 starter），若未显式声明则模块无法编译。
  - 参考现有 `ai-impl/pom.xml` 模式（显式声明 `spring-boot-starter`），新模块应显式声明 `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa`。

- **[一般]** 未明确要求更新父 POM `<modules>` 区域。
  - 设计仅笼统说"同步更新父 POM"，但未列出须将 `patient`、`doctor`、`admin` 三个 module 加入 `backend/pom.xml` 的 `<modules>` 块。
  - 缺少此声明则 Maven reactor 不会构建新模块。

- **[一般]** 未明确要求更新 `application/pom.xml` 依赖。
  - 设计指出"application 模块依赖 patient/doctor/admin 各模块的 compile 产物"，但当前 `application/pom.xml` 不含任何 `<dependency>`。
  - 须在 `application/pom.xml` 中显式添加对 `patient`、`doctor`、`admin` 的 compile 范围依赖，否则编译期无法引用新模块类。

- **[轻微]** 设计决策表提及的 `ignoredUnusedDeclaredDependencies` 配置在父 POM 中已存在（`backend/pom.xml:90-93`），属冗余陈述。

- **[轻微]** `PatientPlaceholderTest.java` 仅提及文件名，未指定任何测试内容或验证目标，实现时无法对齐期望。

## 修改要求

1. 新模块 POM 补充 `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa` 依赖（或参考现有模块最小依赖原则，精确声明所需 artifact）。
2. 在父 POM `<modules>` 中显式添加 `patient`、`doctor`、`admin`。
3. 在 `application/pom.xml` 中为 `patient`、`doctor`、`admin` 添加 compile 范围 `<dependency>`。
4. （可选）去除冗余的 `ignoredUnusedDeclaredDependencies` 陈述。
5. （可选）为占位测试类补充简要测试契约描述。
