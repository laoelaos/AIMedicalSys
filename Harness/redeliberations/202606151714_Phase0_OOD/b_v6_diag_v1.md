# OOD 设计方案质量审查报告（v6）

## 审查说明

本报告审查维度侧重内部审议未充分覆盖的方面：需求响应充分度、整体深度与完整性、从实际落地视角的可实现性。内部审议已确认的技术可行性、标准库覆盖等技术维度不在此重复验证。

---

## 发现的问题

### P1（重要 — 需求响应充分度/完整性缺失）：Phase 0 骨架缺少数据库驱动策略，无法满足"骨架可运行"需求

- **位置**：全局，集中体现于 2.1 节目录布局的 POM 依赖、9.1 节配置管理、3.2 节 BaseEntity
- **问题描述**：设计指定的 JPA 实体（BaseEntity、User、Role 等）需要数据库支持才能运行，但未指定 Phase 0 骨架应使用何种数据库技术。9.1 节仅提及 `application-dev.yml` 包含"数据库连接"，未指明是 H2 内存数据库（Spring Boot 默认的骨架阶段标准选择）还是指向实际的 MySQL/PostgreSQL。POM 依赖中未列出任何数据库驱动依赖（H2/MySQL/PostgreSQL），开发者按设计实现时无法确定该添加什么依赖和数据源配置。
- **影响**：骨架无法可预期地"直接运行"——开发者要么猜测使用 H2（Spring Boot 默认），要么自行决定生产中使用的数据库，造成实现偏离设计预期。这与需求"骨架可运行"和"可直接指导编码实现"的要求矛盾。
- **改进建议**：在 9.1 节或 2.1 节的 POM 配置中明确 Phase 0 使用 H2 内存数据库（`spring-boot-starter-data-jpa` 的测试依赖或显式添加 `h2` runtime scope 依赖），并提供对应的 `application-dev.yml` datasource 配置示例（URL、driver-class-name、ddl-auto 等）。同时说明 Phase 1+ 切换为 MySQL/PostgreSQL 的连接方式。

---

### P2（重要 — 深度与完整性不足）：前端 Vite 代理跨域配置仅一句话提及，无法直接指导编码

- **位置**：9.3 节
- **问题描述**：9.3 节仅写"Vite 开发服务器代理跨域到后端"，没有提供具体的代理配置规则。前端开发者实现 `vite.config.ts` 时无法获知需要配置哪些路径前缀的代理规则、目标后端 URL 是什么、是否需要配置 WebSocket 代理。项目技术栈文档（`Docs/02_tech.md` §3.4）明确将 `vite.config.ts` 开发代理配置列为必需技术组件。
- **影响**：前端骨架无法按设计文档直接实现可联调的前端—后端通信。设计文档"可直接指导编码实现"的要求在此处落空。
- **改进建议**：在 9.3 节补充 Vite 代理配置示例，至少包含：
  ```typescript
  // vite.config.ts
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
  ```
  同时说明 Phase 1+ 认证引入后需要补充的 cookie/token 处理。

---

### P3（轻微 — 事实错误）：CI 流水线第三阶段存在重复行

- **位置**：第 10 节「CI 占位」
- **问题描述**：第三阶段的命令出现两次完全相同的行：
  ```
  第三阶段（聚合层）: mvn install -DskipTests -pl application
  第三阶段（聚合层）: mvn install -DskipTests -pl application
  ```
  这是明显的复制粘贴错误。
- **影响**：实现者会困惑是否应该执行两次（无意义地重复安装同一个模块），或认为这是笔误。
- **改进建议**：删除重复行，使第三阶段只保留一行 `mvn install -DskipTests -pl application`。

---

### P4（轻微 — 逻辑矛盾/遗漏）：common-module 包命名规范未包含 api 子包，与 8.4 节跨模块门面接口路径不一致

- **位置**：2.3 节 vs 8.4 节
- **问题描述**：2.3 节 common-module 的包结构仅列出 `permission`、`config`、`dict` 三个子包。但是 8.4 节的跨模块门面接口示例将 `PermissionService` 定义在 `.../commonmodule/api/PermissionService.java` 路径下，引用了一个未声明的 `api` 子包。此问题在内部审议（a_v6_review_v1.md）中已识别为轻微问题但当前产出仍未修正。
- **影响**：实现者按 2.3 节创建包结构时会缺少 `api` 子包（不知道是否应该创建），而按 8.4 节实现时则会额外创建该包，两份节点不一致影响设计可信度。
- **改进建议**：在 2.3 节 common-module 的包命名列表中补充 `api` 子包（`com.aimedical.modules.commonmodule.api`），标明其职责为"跨模块门面接口定义"。

---

## 整体评价

设计在技术可行性、生态覆盖、核心抽象一致性等方面已经过充分审议（内部审议已确认通过）。上述 4 个问题集中存在于"直接落地指导"层面——数据库驱动策略和前端代理配置两个缺口使设计文档在关键路径上无法直接指导编码实现。建议在下一轮迭代中优先解决 P1 和 P2，确保骨架可运行的基础前提完备。
