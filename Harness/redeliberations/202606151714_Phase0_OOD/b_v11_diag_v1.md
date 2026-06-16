# 质量审查报告 — Phase 0 OOD 设计方案（v11）

审查对象：`Harness/redeliberations/202606151714_Phase0_OOD/a_v11_design_v1.md`
审查轮次：首轮（基于第 11 迭代版本）
审查视角：需求响应充分度、完整性、深度、从设计到编码的可落地性
前置声明：该产出已通过 10 轮内部审议，本报告侧重内部审议未充分覆盖的维度

---

## 1. Integration 模块依赖 application 模块的 Spring Boot 打包冲突

**问题描述**：第 10 节 `integration/pom.xml` 骨架配置以 `test scope` 依赖 `application` 模块。但 `application` 模块作为 Spring Boot 入口模块，默认使用 `spring-boot-maven-plugin` 的 repackage 目标，该目标会将普通 JAR 替换为可执行 fat JAR。fat JAR 的依赖以 `BOOT-INF/lib` 内嵌方式存放，Maven 依赖解析器无法从中解析出 transitive 依赖。因此 `integration` 模块的 `@SpringBootTest` 将因缺少 `spring-boot-starter-web`、`spring-boot-starter-data-jpa` 等关键依赖而启动失败，整个 CI 第四阶段的集成测试无法运行。

**所在位置**：第 10 节集成测试模块描述、`integration/pom.xml` 骨架

**严重程度**：严重

**改进建议**：在 `application/pom.xml` 的 `spring-boot-maven-plugin` 配置中添加 `<classifier>exec</classifier>`，使 repackage 后的 fat JAR 保留原始 JAR 作为 `test` scope 的依赖使用：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>repackage</goal></goals>
            <configuration>
                <classifier>exec</classifier>
            </configuration>
        </execution>
    </executions>
</plugin>
```

并在第 10 节集成测试配置中补充说明该机制的用途和原理。

---

## 2. `-DskipTests` 对 Failsafe 插件的影响描述存在事实错误

**问题描述**：第 10 节 CI 第四阶段命令注释称 `-DskipTests 为 Maven 标准属性，跳过 Surefire 单元测试，不影响 Failsafe 集成测试执行`。此描述与 Maven 官方行为不符——`maven-failsafe-plugin` 同样响应用户属性 `skipTests`（等价于其 `<skip>` 配置项），`-DskipTests` 设置为 `true` 时将同时跳过 Surefire 和 Failsafe 测试执行。开发者按此命令执行会发现集成测试未运行，且无法从文档中获得正确指引。

**所在位置**：第 10 节，第四阶段命令注释行

**严重程度**：一般

**改进建议**：将 `-DskipTests` 替换为仅跳过 Surefire 的标准属性 `-Dsurefire.skip=true`，并修正注释说明；或明确在 `integration/pom.xml` 的 Failsafe 配置中设置 `<skipTests>false</skipTests>` 以覆盖全局属性。

---

## 3. 多模块聚合父 POM 骨架结构缺失，不足以直接指导编码

**问题描述**：第 2.1 节目录布局列出了 `modules/common-module/pom.xml`（公共模块父 POM）和 `modules/ai/pom.xml`（AI 模块父 POM），但设计文档给出了 `backend/pom.xml` 根 POM 的核心骨架，却未给出这两个中间层聚合 POM 的定义。实际实现时开发者需要自行推断：
- `common-module/pom.xml` 的 `<parent>` 如何指向 `backend/pom.xml`
- `<packaging>pom</packaging>` 声明
- `<modules>` 列表引用 `common-module-api` / `common-module-impl`
- 子模块 POM 的 `<relativePath>` 路径

这种信息缺失意味着开发者在 Phase 0 搭建骨架时可能设置错误的依赖关系，导致编译失败或模块结构不一致。设计文档明确要求"可直接指导编码实现"，此处存在 gap。

**所在位置**：第 2.1 节目录布局，未在第 2.1 节或附录中提供 `common-module/pom.xml` 和 `modules/ai/pom.xml` 的 POM 骨架

**严重程度**：一般

**改进建议**：参照 `backend/pom.xml` 骨架示例，在第 2.1 节（或附录）中补充 `modules/common-module/pom.xml` 和 `modules/ai/pom.xml` 的聚合 POM 定义：

```xml
<!-- modules/common-module/pom.xml -->
<project>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-module</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>common-module-api</module>
        <module>common-module-impl</module>
    </modules>
</project>
```

---

## 4. 前端 CI 构建缺少依赖安装步骤，流水线不完整

**问题描述**：第 10 节 CI 第五阶段描述为"构建前端（各 apps/* 中 npm run build）→ 归档制品"。在 CI 洁净环境中，`node_modules` 目录不存在，`npm run build` 会因缺少依赖而直接失败。此外，CI 环境应使用 `npm ci`（基于 `package-lock.json` 确定性安装）而非 `npm install`，以保证可重复构建。

**所在位置**：第 10 节 CI 第五阶段

**严重程度**：一般

**改进建议**：将第五阶段的前端构建步骤补充为：

```
cd frontend
npm ci                    # 或 npm install（Phase 0 暂未锁定版本时可使用）
npm run build --workspaces  # 构建所有 workspace 包
```

同时建议在根 `package.json` 中定义 `"build:all"` 脚本以分别构建三个端。

---

## 5. `common` 模块 POM 中三组 Starter 依赖带来的 transitive 依赖传播未评估

**问题描述**：第 2.2 节规定 `common` 模块依赖 `spring-boot-starter-web`、`spring-boot-starter-security` 和 `spring-boot-starter-data-jpa` 三个 Starter。由于所有业务模块（patient、doctor、admin）均直接依赖 `common`，它们将无条件获得这三个 Starter 的全部 transitive 依赖——包括完整的嵌入式 Tomcat（servlet-api、Tomcat 嵌入包）、Spring Security（security-config、authentication 框架）和 Spring Data JPA（Hibernate ORM、连接池等）。设计文档未评估这种依赖传播的影响，也未讨论是否应通过 `<optional>true</optional>` 或 `provided scope` 控制传播范围。

**所在位置**：第 2.2 节 `common` 模块依赖描述

**严重程度**：轻微

**改进建议**：评估三个 Starter 是否都需声明为 `compile` 默认 scope，或对部分依赖使用 `<optional>true</optional>`（让业务模块按需引入）。在文档中补充说明 transitive 依赖传播的决策依据，避免 Phase 2+ 模块拆分时被动处理依赖膨胀问题。

---

## 综合质量评价

该设计文档经过 10 轮内部迭代修复，在接口一致性、技术可行性、异常处理覆盖等内部审议重点维度上已达到较高成熟度。上述 5 个问题集中分布在**下游可消费性（Integration 模块可行性）**和**编码直接指导性（聚合 POM 缺失、CI 步骤不完整）**两个维度，属于内部审议侧重设计正确性而容易忽略的落地细节。修复后该设计文档可满足"直接指导编码实现"的要求。
