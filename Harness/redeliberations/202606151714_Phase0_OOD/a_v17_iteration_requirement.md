根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题1（严重）：4.3 节 AI 调用契约图中的 `DegradationStrategy.fallback` 引用已不存在的方法

4.3 节流程图中显示 `FallbackAiService(调用失败) → DegradationStrategy.fallback(request)`，但根据 3.4 节的定义（v15 修订已确认），DegradationStrategy 已取消泛型 fallback 方法，仅保留 `boolean shouldDegrade(DegradationContext context)` 判定逻辑，降级结果由 FallbackAiService 直接构造 `AiResult(success=false, degraded=true, data=null)`。4.3 节的流程图引用了文档中不存在的方法签名，与当前设计存在事实矛盾。

**所在位置**：第 4.3 节，第 677–683 行（流程图）

**改进建议**：将 4.3 节流程图中的 `DegradationStrategy.fallback(request)` 替换为符合当前设计的正确描述，例如：
```
└── FallbackAiService(调用失败) → DegradationStrategy.shouldDegrade(context)
      ↓ degraded=true
      └── 直接返回 AiResult(success=false, degraded=true, data=null)
```

---

### 问题2（中等）：springdoc-openapi 配置在生产环境暴露 Swagger UI 的安全风险未处理

8.3 节的 springdoc-openapi 配置块被放置在 `application.yml`（全局配置，而非 profile 特定配置）中，且 `enabled: true`，这意味着 Swagger UI 和 OpenAPI 文档在生产环境（prod profile）下同样可被访问。文档在 9.1 节已明确处理了 H2 Console 的"生产环境关闭"策略，但对 Swagger UI 的类似安全风险缺少同等处理。

**所在位置**：第 8.3 节，第 1011–1020 行（springdoc 配置块）

**改进建议**：参照 9.1 节对 H2 Console 的处理方式，补充以下任一整改：
- 将 springdoc 配置移至 `application-dev.yml`，使其仅在 dev profile 下生效；或
- 在 8.3 节末尾补充说明，要求 `application-prod.yml` 中显式设置 `springdoc.api-docs.enabled: false` 和 `springdoc.swagger-ui.enabled: false`

---

### 问题3（中等）：运行时环境要求未明确说明

文档指定了 Spring Boot 3.3.0 和 Vite 等技术栈，但未明确声明运行时的最低环境要求：
- Spring Boot 3.3.0 要求 Java 17+，但文档未在任何位置说明所需 JDK 版本
- 前端 workspace 使用了 Vite、Vue 3、TypeScript，但未说明所需 Node.js 版本范围或 npm 版本

新加入项目的开发者无法从文档中获知本地环境配置的最低要求，可能因 JDK 版本不符导致编译失败。

**所在位置**：通篇缺失，应在第 1 节「概述」或第 9 节「本地开发体验」中补充

**改进建议**：在第 1 节末尾或 9.1 节顶部补充运行环境要求小节，至少包括：
- JDK：17+（Spring Boot 3.3.0 最低要求）
- Node.js：18+ 或 20 LTS（与 Vue 3 + Vite 兼容的主流版本）
- npm：9+ 或指定 pnpm/yarn

---

### 问题4（低）：PageQuery.page/PageQuery.sort 的边界条件和异常输入未定义

3.1 节定义了 PageQuery 的 page 为 0-based、size 上限 500（标注 `@Max(500)`），但对以下边界输入未定义处理行为：
- `page` 传入负值（如 `page=-1`）——Spring Data 的 `PageRequest.of(-1, size)` 会抛出 `IllegalArgumentException`
- `sort` 格式无效（如 `"createdAt"` 缺少 direction，或方向拼写错误 `"createdAt,descc"`）——Spring Data 的 `Sort.by()` 解析异常未描述捕获方式

**所在位置**：第 3.1 节「PageQuery / PageResponse<T>」字段描述

**改进建议**：
- 在 PageQuery 的 `page` 字段上标注 `@Min(0)` 注解
- 补充说明 `sort` 格式无效时的异常（`InvalidDataAccessApiUsageException` 或 `BadRequestException`）应由 GlobalExceptionHandler 统一捕获为 400 响应
- 或标注 Phase 0 暂不实现参数校验的 fallback 处理

---

### 问题5（低）：前端骨架缺少明确的验收标准

需求要求"骨架可运行——三端前端可一键启动到占位首页"，且 4.1 节定义了后端的验收标准（`GET /api/ping` → `"pong"`），但对前端三端应用缺少同等级别的验收标准定义。开发者无法明确判断前端骨架是否满足 Phase 0 验收要求。

**所在位置**：第 4.1 节「健康检查」及整体缺失

**改进建议**：在 4.1 节补充前端骨架的验收标准，例如：
- 各前端应用（patient/doctor/admin）执行 `npm run dev` 可正常启动 Vite 开发服务器
- 浏览器访问各端端口显示占位页面（包含系统名称及占位提示文本）
- 各端开发服务器可正常代理 `/api/ping` 请求到后端并收到 `"pong"` 响应

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

以下问题已在前期迭代中被逐一修复，当前 v16 诊断报告中不再提及：
- **迭代 1**：AI 方法名中文括号编译问题、权限模型模块归属、同步非阻塞表述、BaseEntity 字段定义、MockAiService 装配策略、SecurityConfig 骨架、User→UserDetails 适配、编译期模块隔离
- **迭代 2**：ai.mock.enabled 装配策略、AI 方法 DTO 类型化、配置加载失败分类错误、前端 ui-core/ 包定义、SecurityConfig 归属矛盾、CI 模块依赖顺序
- **迭代 3**：CI 分阶段 mvn compile 产物问题、FallbackAiService @Primary 循环依赖、DegradationStrategy 方法签名
- **迭代 4**：@EnableJpaAuditing 配置、Phase 0 permitAll 认证策略、FallbackAiService 生产环境启动、BaseEntity.deleted 类型、前后端 DTO 同步、ErrorCode 改为 interface
- **迭代 5**：common 模块依赖声明矛盾、Spring Boot 包扫描配置、BusinessException 继承层次、PageRequest 命名冲突
- **迭代 6**：H2 数据库驱动策略、Vite 代理配置、CI 命令重复、common-module 包命名一致性问题
- **迭代 7**：common 模块 data-jpa 依赖、真实 AiService 装配、ScheduleRequest 字段类型
- **迭代 8**：spring-boot-starter-web 依赖、spring-boot-starter-test 依赖、spring-boot-starter-validation 依赖、前端 Monorepo 引用机制
- **迭代 9**：跨模块接口隔离（UserDTO）、CI 非标准属性（skip.unit.tests）、过时事件模式（ApplicationEvent）、PageQuery @Max 约束、DTO 日期格式、ai.mock.enabled 配置示例、Mock 可选字段约定、H2 Console 生产关闭、前端 Axios 错误处理
- **迭代 10**：AiService 返回类型统一（AiResult）、common-module api/impl 拆分、FallbackAiService 构造注入、前端占位首页结构
- **迭代 11**：Integration fat JAR 依赖、CI -DskipTests 与 Failsafe 冲突、聚合 POM 结构、前端 CI 依赖安装
- **迭代 12**：CI 第四阶段 surefire.skip 属性问题、DegradationContext NPE 风险提示
- **迭代 13**：FallbackAiService 自引用循环依赖、目录树与包命名一致性、装配条件汇总表 Phase 0 说明
- **迭代 14**：Section 2.2 依赖关系图箭头指向错误、parent POM dependencyManagement security 条目、前端 build:all 脚本范围、DegradationStrategy 泛型

### 持续存在的问题（在多轮反馈中反复出现，需重点关注）

无严格意义上跨轮次持续存在的未解决问题。以下问题有相似主题但具体维度不同：
- **PageQuery 参数校验细节**：迭代 4（page 起始值歧义）→迭代 9（size 最大值约束）→v16 问题 4（page 负值与 sort 格式校验）。每轮发现不同维度的校验缺失，建议本轮在 Phase 0 范围限制说明中统一声明，避免每轮出现新细节

### 新发现的问题（本轮新识别的问题）

v16 诊断报告中的 5 个问题均为本轮新发现：
1. 4.3 节流程图 `DegradationStrategy.fallback` 引用已删除方法——v15 修订范围遗漏
2. springdoc-openapi 生产环境暴露 Swagger UI——安全保密维度
3. 运行时环境要求缺失——开发者入门体验维度
4. PageQuery.page/sort 边界条件——参数校验完备性维度
5. 前端骨架验收标准缺失——需求响应充分度维度

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v16_copy_from_v15.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
