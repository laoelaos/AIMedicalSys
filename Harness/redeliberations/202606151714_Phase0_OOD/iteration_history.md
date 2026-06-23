## 迭代第 1 轮

1. **问题描述**：AI 方法标识 `analysisReport(检查)` / `analysisReport(检验)` 包含中文括号且方法名相同，Java 接口无法编译
   - 所在位置：诊断报告 8.2 节 AI 能力方法清单表格
   - 严重程度：严重
   - 改进建议：分别命名，如 `analysisReportForInspection` 与 `analysisReportForLabTest`

2. **问题描述**：权限模型实体 Role、Post、Function、User 的 Maven 模块归属未说明，User 实体跨模块共享引用机制未定义
   - 所在位置：诊断报告 3.3 节
   - 严重程度：严重
   - 改进建议：明确各实体归属模块与包路径，推荐统一归入 common-module 的 permission 包

3. **问题描述**："同步非阻塞"表述在操作系统/网络模型中不存在
   - 所在位置：诊断报告 6 节
   - 严重程度：一般
   - 改进建议：区分为 Phase 0 同步阻塞与 Phase 2+ 异步非阻塞两个时态描述

4. **问题描述**：BaseEntity 缺少字段类型、ID 策略、逻辑删除实现方式、乐观锁版本字段等详细定义
   - 所在位置：诊断报告 3.2 节
   - 严重程度：一般
   - 改进建议：补充 BaseEntity 伪代码或具体字段定义，包含字段类型、JPA 注解、ID 策略

5. **问题描述**：MockAiService 注入机制与 Bean 装配策略不完整
   - 所在位置：诊断报告 3.4 节
   - 严重程度：一般
   - 改进建议：补充 AiService 的 Bean 装配策略说明，明确三者的装配条件

6. **问题描述**：Spring Security 配置骨架未定义
   - 所在位置：诊断报告 4.5 节
   - 严重程度：一般
   - 改进建议：补充 SecurityConfig 配置类的设计骨架或 Phase 0 的放通配置示例

7. **问题描述**：User 实体与 Spring Security UserDetails 的适配关系未定义
   - 所在位置：诊断报告 3.3 节、4.5 节
   - 严重程度：一般
   - 改进建议：明确 User 与 UserDetails 的关系，推荐 Adapter 模式

8. **问题描述**："仅依赖 ai 的 api 子包"缺乏编译期强制保障
   - 所在位置：诊断报告 2.2 节
   - 严重程度：一般
   - 改进建议：推荐分离 ai-api 和 ai-impl 两个 Maven 模块，或补充 ArchUnit 检查规则

## 迭代第 2 轮

1. **问题描述**：`ai.mock.enabled` 未配置时 Phase 0 无 AiService Bean 可用，装配条件汇总表表述存在误导
   - 所在位置：3.4 节「Bean 装配策略」及「装配条件汇总表」
   - 严重程度：严重
   - 改进建议：在 `application.yml` 中统一显式设置 `ai.mock.enabled: true` 作为全局默认值，或改用 `matchIfMissing = true`；同时修正装配条件汇总表使其准确反映注解语义

2. **问题描述**：AI 方法输入/输出使用中文自然语言描述，缺乏具体 DTO 类型名
   - 所在位置：8.2 节「AI 能力方法清单」表格
   - 严重程度：严重
   - 改进建议：为每个 AI 方法的输入和输出指定具体的 DTO 类名，并在 3.4 节或 8.2 节以类图或伪代码骨架形式定义各 DTO 的核心字段结构

3. **问题描述**：事实错误——"配置加载失败"无法被 GlobalExceptionHandler 捕获
   - 所在位置：5.1 节错误分类表
   - 严重程度：严重
   - 改进建议：删除该行中"运行时由 GlobalExceptionHandler 统一捕获 → Result"的描述，替换为"应用启动失败，由 Spring Boot 的失败分析器（FailureAnalyzer）输出诊断信息"

4. **问题描述**：前端 `ui-core` 包出现在目录树中但完全未定义
   - 所在位置：2.1 节目录布局、2.4 节前端模块划分
   - 严重程度：一般
   - 改进建议：在 2.4 节补充 `packages/ui-core/` 的定义，明确其内容职责、与 shared 包的依赖关系，以及三端应用的引用方式

5. **问题描述**：SecurityConfig 模块归属存在内部矛盾
   - 所在位置：4.5 节与 2.1 节目录布局
   - 严重程度：一般
   - 改进建议：统一 SecurityConfig 的归属决策（推荐 `common.config`），在 common 模块的依赖描述中明确标注 spring-boot-starter-security 为其必需依赖

6. **问题描述**：CI 流水线未体现模块依赖构建顺序
   - 所在位置：第 10 节「CI 占位」
   - 严重程度：一般
   - 改进建议：在 CI 流水线中标注各 Maven 模块的构建阶段归属，体现基础模块优先编译、业务模块并行编译、application 统一聚合的分阶段策略

## 迭代第 3 轮

1. **问题描述**：CI多阶段流水线中`mvn compile`不会将产物安装到本地仓库，后续阶段依赖解析将失败
   - 所在位置：第10节「CI占位」
   - 严重程度：严重
   - 改进建议：将前三个阶段中的`mvn compile`改为`mvn install -DskipTests`；或在一个Maven调用中完成全部模块编译，或每个阶段使用`-am`自动包含依赖模块

2. **问题描述**：FallbackAiService标注`@Primary`后内部通过`@Autowired`注入AiService将导致循环依赖，委托实例获取方式未定义
   - 所在位置：第3.4节「Bean装配策略」
   - 严重程度：一般
   - 改进建议：明确FallbackAiService内部通过`@Resource(name = "mockAiService")`或`@Qualifier`按名称注入底层实现；或补充Factory模式/DelegatingAiService方案

3. **问题描述**：`DegradationStrategy.shouldDegrade()`方法签名无入参，实现类无法做出有意义的降级决策
   - 所在位置：第3.4节「降级策略框架」
   - 严重程度：一般
   - 改进建议：将shouldDegrade()签名调整为接受调用上下文参数，例如`boolean shouldDegrade(DegradationContext context)`；或在接口上注明Phase 0暂返回false

4. **问题描述**：8个嵌套DTO类型（RecommendedDoctor、PrescriptionDrug、PatientInfo等）被引用但自身未定义字段结构
   - 所在位置：第8.2节「AI能力方法清单—DTO核心字段定义」
   - 严重程度：一般
   - 改进建议：为每个嵌套类型补充核心字段伪代码定义，或标注"字段结构由Phase 1业务分析时细化，Phase 0暂使用Map<String, Object>占位"

## 迭代第 4 轮

1. **问题描述**：`@EnableJpaAuditing` 配置缺失导致 `createdAt`/`updatedAt` 自动填充静默失效
   - 所在位置：3.2 节 BaseEntity 字段定义
   - 严重程度：严重
   - 改进建议：在 3.2 节末尾补充 `@EnableJpaAuditing` 的配置位置声明（推荐放在 common 模块的 config 包的 JpaConfig 类中）

2. **问题描述**：SecurityConfig 认证策略与 Phase 0「骨架可运行」目标冲突——「其余接口要求认证」与「permitAll 临时放通」矛盾
   - 所在位置：4.5 节 SecurityConfig 设计骨架
   - 严重程度：严重
   - 改进建议：明确 Phase 0 统一使用 `permitAll` 放通所有接口，或提供显式的 `@Profile("!phase0")` 条件化配置

3. **问题描述**：FallbackAiService 在 `ai.mock.enabled=false` 时按名称硬编码注入 `mockAiService`，生产环境无此 Bean 导致启动失败
   - 所在位置：3.4 节 Bean 装配策略
   - 严重程度：一般
   - 改进建议：FallbackAiService 内部使用 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取可用实现

4. **问题描述**：PageRequest.page 起始值歧义（从 0 或 1 开始），存在后续模块 off-by-one 风险
   - 所在位置：3.1 节 PageRequest/PageResponse 字段描述
   - 严重程度：一般
   - 改进建议：明确约定为 0-based，添加 Javadoc 注释，前端补充分页参数适配逻辑

5. **问题描述**：BaseEntity.deleted 使用包装类型 `Boolean` 存在自动拆箱 NPE 风险
   - 所在位置：3.2 节 BaseEntity 字段定义
   - 严重程度：一般
   - 改进建议：将 `deleted` 字段类型从 `Boolean` 改为 `boolean`（基本类型）

6. **问题描述**：前后端 26+ DTO 类型定义的一致性同步机制未定义，多人并行协作存在类型不匹配风险
   - 所在位置：8.2 节 AI 能力方法清单及 2.4 节
   - 严重程度：一般
   - 改进建议：补充 OpenAPI 规范生成配置（springdoc-openapi）并声明前端通过 openapi-generator 自动生成 TypeScript 类型

7. **问题描述**：ErrorCode 定义为 `enum` 与「每个模块维护自己的错误码枚举」矛盾，enum 为 final 不可被继承或实现，BusinessException 无法引用多模块错误码
   - 所在位置：3.1 节 ErrorCode + 5.2 节 BusinessException
   - 严重程度：严重
   - 改进建议：将 ErrorCode 从 `enum` 改为 `interface`，各模块提供 enum 实现该接口

8. **问题描述**：Integration 模块用途完全未定义，开发者无法判断是否需要创建
   - 所在位置：2.1 节目录布局及第 10 节 CI 占位
   - 严重程度：轻微
   - 改进建议：若需要集成测试骨架则补充职责和交付物要求，否则从目录布局中删除该条目

9. **问题描述**：API 版本管理策略描述分散在决策表与正文中，未形成单一声明
   - 所在位置：第 7 节设计决策表及 8.1 节
   - 严重程度：轻微
   - 改进建议：在 8.1 节开头用一句话总结 API 版本管理策略，形成单一声明

## 迭代第 5 轮

1. **问题描述**：`common`模块依赖声明与SecurityConfig实现存在矛盾
   - 所在位置：第2.2节（line 117）及4.5节（line 442-443）
   - 严重程度：一般
   - 改进建议：修订第2.2节common模块依赖声明，明确SecurityConfig实现依赖于`spring-boot-starter-security`存在的假设

2. **问题描述**：业务模块间调用方式未定义，限制后续迭代实现
   - 所在位置：第2.2节（line 20-21）
   - 严重程度：一般
   - 改进建议：新增模块调用规范章节，明确RPC模式、事件接口或直连Feign调用的选择，并给出典型示例

3. **问题描述**：Spring Boot包扫描配置缺失，骨架启动缺少关键前置
   - 所在位置：缺失，应在第9.2节或每个模块说明中
   - 严重程度：一般
   - 改进建议：在第9.2节明确`@SpringBootApplication(scanBasePackages = "com.aimedical")`的使用方式，并补充`@EntityScan`和`@EnableJpaRepositories`

4. **问题描述**：`BusinessException`未明确定义继承层次，类型系统不完整
   - 所在位置：第5.2节（line 492）
   - 严重程度：一般
   - 改进建议：明确BusinessException extends RuntimeException

5. **问题描述**：自定义`PageRequest`与Spring Data的`PageRequest`命名冲突未处理
   - 所在位置：第3.1节（line 197）
   - 严重程度：一般
   - 改进建议：将自定义类重命名为`PageQuery`或`PageCriteria`，消除命名冲突

## 迭代第 6 轮

1. **问题描述**：Phase 0 骨架缺少数据库驱动策略，POM 依赖未列出任何数据库驱动，9.1 节仅提及"数据库连接"未指定具体技术（H2/MySQL/PostgreSQL）
   - 所在位置：2.1 节目录布局的 POM 依赖、9.1 节配置管理、3.2 节 BaseEntity
   - 严重程度：严重
   - 改进建议：在 9.1 节或 2.1 节明确 Phase 0 使用 H2 内存数据库（添加 h2 runtime scope 依赖），提供 application-dev.yml datasource 配置示例（URL、driver-class-name、ddl-auto 等），并说明 Phase 1+ 切换 MySQL/PostgreSQL 的连接方式

2. **问题描述**：9.3 节 Vite 开发服务器代理跨域配置仅一句话提及，缺少具体的代理配置规则
   - 所在位置：9.3 节
   - 严重程度：一般
   - 改进建议：补充 Vite 代理配置示例，至少包含 /api 路径到 http://localhost:8080 的代理规则，说明 Phase 1+ 认证引入后 cookie/token 处理

3. **问题描述**：CI 流水线第三阶段存在重复的命令行（完全相同的内容出现两次）
   - 所在位置：第 10 节"CI 占位"
   - 严重程度：轻微
   - 改进建议：删除重复行，使第三阶段只保留一行 mvn install -DskipTests -pl application

4. **问题描述**：2.3 节 common-module 包命名规范未包含 api 子包，与 8.4 节跨模块门面接口路径不一致
   - 所在位置：2.3 节 vs 8.4 节
   - 严重程度：轻微
   - 改进建议：在 2.3 节 common-module 的包命名列表中补充 api 子包（com.aimedical.modules.commonmodule.api），标明其职责为"跨模块门面接口定义"

## 迭代第 7 轮

1. **问题描述**：common 模块缺少 spring-boot-starter-data-jpa 依赖声明，直接编译将导致编译失败
   - 所在位置：2.2 节 common 模块依赖声明
   - 严重程度：严重
   - 改进建议：在 2.2 节 common 模块依赖声明中明确补充 spring-boot-starter-data-jpa，并注明使用范围（common-module 是否为正式依赖或 optional）

2. **问题描述**：真实 AiService 实现与 FallbackAiService 的 Bean 装配顺序未定义，Phase 2+ 装配策略存在漏洞
   - 所在位置：3.4 节「Bean 装配策略」及「装配条件汇总表」
   - 严重程度：一般
   - 改进建议：选择方案 A：取消 @Primary，引入 AiServiceConfig 工厂创建；或方案 B：保留 @Primary，添加 @ConditionalOnProperty，修正装配条件汇总表

3. **问题描述**：ScheduleRequest.doctorIds 字段类型与系统已有 doctor ID 字段类型不一致
   - 所在位置：8.2 节 ScheduleRequest DTO 定义
   - 严重程度：一般
   - 改进建议：将 ScheduleRequest.doctorIds 类型统一为 List<Long>

4. **问题描述**：springdoc-openapi 依赖缺失导致无法生成文档
   - 所在位置：8.3 节
   - 严重程度：一般
   - 改进建议：明确 springdoc-openapi 依赖的归属模块，在父 POM 的 <dependencyManagement> 中统一版本声明，业务模块按需引用

## 迭代第 8 轮

1. **问题描述**：spring-boot-starter-web 未被任何模块显式声明，@RestController、@ControllerAdvice、嵌入式 Tomcat 等不可用，破坏骨架可运行目标
   - 所在位置：第 2.2 节「模块职责与依赖」
   - 严重程度：严重
   - 改进建议：在 common 模块依赖声明中解决「Spring Boot Starter 遗漏」问题，确认为 spring-boot-starter-web，在 <dependencyManagement> 中统一版本，application 及业务模块引用

2. **问题描述**：spring-boot-starter-test 未声明，JUnit 5 和 @SpringBootTest 不可用，占位测试无法编写，mvn test 将失败
   - 所在位置：第 10 节「CI 占位」及第 2.2 节「模块职责与依赖」
   - 严重程度：一般
   - 改进建议：在父 POM 的 <dependencyManagement> 中声明 spring-boot-starter-test，需要编写测试的模块以 test scope 引用

3. **问题描述**：spring-boot-starter-validation 未声明，Spring Boot 3 中 spring-boot-starter-web 不再自动引入 Bean Validation，@Valid 将静默失效，校验错误不会触发
   - 所在位置：第 4.2 节「统一响应处理」及第 5.1 节「错误分类」
   - 严重程度：一般
   - 改进建议：在父 POM 的 <dependencyManagement> 中声明 spring-boot-starter-validation，在需要 Controller 的业务模块 POM 中引用

4. **问题描述**：前端 Monorepo 内部包引用机制未定义，缺少 package.json 的 workspaces 字段、内部包的导入路径配置、对应 project 的 workspace 引用方式
   - 所在位置：第 2.4 节「前端模块划分」
   - 严重程度：一般
   - 改进建议：新增 package.json 的 workspaces 配置示例，内部包 package.json 的 name 引用规则配置及对应引用方式的典型示例

## 迭代第 9 轮

1. **问题描述**：跨模块接口返回 JPA 实体违反接口隔离原则，8.4 节明确要求模块内部类不对外暴露，PermissionService.getUserById() 直接返回 User 实体，存在 LazyInitializationException、循环引用、序列化循环引用等风险
   - 所在位置：8.4 节 PermissionService 接口示例（804-806 行）
   - 严重程度：严重
   - 改进建议：将接口返回类型从 User 改为 UserDTO，在 common-module 的 dto 子包中定义 UserDTO（包含必要字段，不含 JPA 注解），序列化时避免循环引用

2. **问题描述**：CI 流水线第四阶段使用非标准 Maven 属性 -Dskip.unit.tests=true，但 POM 中未为 Surefire 插件配置对应 <skip> 属性，该属性无法直接使用
   - 所在位置：第 10 节 CI 占位第四阶段命令
   - 严重程度：一般
   - 改进建议：在 integration/pom.xml 的 surefire 插件配置中添加 <skip>${skip.unit.tests}</skip>，在 properties 中定义默认值，修正为标准配置

3. **问题描述**：UserRegisteredEvent extends ApplicationEvent 使用过时模式，Spring Framework 4.2+（项目使用 Spring Boot 3 对应 Spring Framework 6）已支持纯 POJO 作为事件对象发布
   - 所在位置：8.4 节事件驱动模式示例（822-825 行）
   - 严重程度：一般
   - 改进建议：去掉 extends ApplicationEvent，改为普通 POJO 类，ApplicationEventPublisher.publishEvent(Object) 可发布纯 POJO 事件

4. **问题描述**：分页参数 size 字段缺少最大值约束，客户端可传入极端 size 值导致后端内存压力甚至 OOM
   - 所在位置：3.1 节 PageQuery 字段定义
   - 严重程度：一般
   - 改进建议：在 PageQuery 的 size 字段上添加 @Max 约束（建议值 100-500），并将 @Valid 注解添加到所有分页 Controller 接口参数

5. **问题描述**：ScheduleRequest.dateRange 类型为 String 无格式约束，不同客户端可能采用不同格式导致前后端接口不一致
   - 所在位置：8.2 节 AI 方法 DTO 定义（约 753 行）
   - 严重程度：轻微
   - 改进建议：改为两个 LocalDate 字段 startDate/endDate，或在 Javadoc 中明确 String 格式的精确约定

6. **问题描述**：配置项 ai.mock.enabled 未在配置文件中以示例方式显式提供，开发者需阅读 MockAiService 注解才能发现配置
   - 所在位置：3.4 节 Bean 装配策略、9.1 节应用配置示例
   - 严重程度：轻微
   - 改进建议：在 9.1 节 application-dev.yml 示例末尾添加一行 ai.mock.enabled: true

7. **问题描述**：Mock 数据占位约定未区分可选/必选字段，必选字段的占位值无法反映字段缺失
   - 所在位置：3.4 节 Mock 数据占位约定
   - 严重程度：轻微
   - 改进建议：新增可选字段约定，使用 @Nullable 注解或 Javadoc 标记为 optional 的字段，Mock 应返回 null

8. **问题描述**：H2 Console 用途未加说明，若生产忘记关闭存在风险，打包为 jar 仍可通过 spring.h2.console.enabled 开启控制台
   - 所在位置：9.1 节 H2 配置
   - 严重程度：轻微
   - 改进建议：额外说明 H2 Console 仅限于 dev profile 使用，application-prod.yml 应设置 spring.h2.console.enabled: false，并明确 Phase 1+ 切换 MySQL 时，h2 依赖 scope 从 runtime 改为 test

9. **问题描述**：前端 ApiClient 未定义错误处理策略，DNS 解析失败、连接超时等 Axios 请求场景未处理可能导致前端被未处理 Promise 异常中断
   - 所在位置：3.5 节 ApiClient 定义
   - 严重程度：轻微
   - 改进建议：在 3.5 节补充 Axios 拦截器定义及超时设置、请求配置、统一格式显示及全局错误弹窗占位逻辑

## 迭代第 10 轮

1. **问题描述**：AiService 方法返回类型不明确，8.2 节表格返回 DTO，但 3.4 节/4.3 节关于 AiResult<T> 的封装约定存在矛盾
   - 所在位置：3.4 节 AiService 职责定义 vs 8.2 节方法清单表格 vs 4.3 节 AI 响应处理约定
   - 严重程度：严重
   - 改进建议：统一使用 AiResult<T>，修正 8.2 节表格并补充方法签名示例

2. **问题描述**：common-module 为单一 Maven 模块，无法在编译期强制接口隔离原则，AI 模块与网关模块耦合不一致
   - 所在位置：2.2 节模块依赖说明、8.4 节跨模块接口模式约定 vs 2.1 节目录结构
   - 严重程度：严重
   - 改进建议：拆分 common-module 为 api/impl 两模块，或补充 ArchUnit 测试规则

3. **问题描述**：FallbackAiService 与 DegradationStrategy 的创建/组合方式完全未定义
   - 所在位置：3.4 节降级策略框架定义
   - 严重程度：一般
   - 改进建议：明确构造注入方式，Phase 0 默认 NoOpDegradationStrategy，提供伪代码骨架示例

4. **问题描述**：前端应用占位首页和路由文件结构未定义，开发者无法直接启动
   - 所在位置：2.4 节前端模块划分
   - 严重程度：一般
   - 改进建议：新增 app 应用初始文件结构说明，index.html、main.ts、App.vue

## 迭代第 11 轮

1. **问题描述**：Integration 模块以 test scope 依赖 application 模块，但 application 的 spring-boot-maven-plugin repackage 生成 fat JAR，integration 无法获取 transitive 依赖，测试无法运行
   - 所在位置：第 10 节集成测试模块依赖及 integration/pom.xml 层级
   - 严重程度：严重
   - 改进建议：在 application/pom.xml 的 spring-boot-maven-plugin 配置中添加 <classifier>exec</classifier>，保留原始 JAR 供 test scope 使用

2. **问题描述**：-DskipTests 同时跳过 Surefire 和 Failsafe，注释声称不影响 Failsafe 执行，与 Maven 官方行为不一致
   - 所在位置：第 10 节，第四阶段命令及注释
   - 严重程度：一般
   - 改进建议：将 -DskipTests 替换为 -Dsurefire.skip=true，并确保 integration/pom.xml 中设置 <skipTests>false</skipTests>

3. **问题描述**：目录树中列出 common-module/pom.xml 和 modules/ai/pom.xml 但聚合 POM 未定义，开发者无法推导 parent 设置和 module 列表
   - 所在位置：第 2.1 节目录结构
   - 严重程度：一般
   - 改进建议：补充 backend/pom.xml 聚合示例，展示聚合 POM 基本结构

4. **问题描述**：前端 CI 构建阶段只有 npm run build，缺少依赖安装步骤
   - 所在位置：第 10 节 CI 构建阶段
   - 严重程度：一般
   - 改进建议：新增 npm ci 或 npm install 步骤，并在根 package.json 中定义 build:all 脚本

## 迭代第 12 轮

1. **问题描述**：CI 第四阶段使用非标准 Maven 属性 `-Dsurefire.skip=true`，该属性不会被 Surefire 插件识别，静默失效
   - 所在位置：第 10 节「CI 占位」第四阶段命令及注释
   - 严重程度：严重
   - 改进建议：删除 `-Dsurefire.skip=true`，改为 `mvn verify -pl integration`

2. **问题描述**：`DegradationContext` 缺省实例中引用类型字段为 null，未注明 Phase 2+ 策略实现可能触发 NPE 的风险
    - 所在位置：3.4 节「降级策略框架」— `DegradationContext` 定义段落
    - 严重程度：一般
    - 改进建议：补充 null 防御性检查说明，或使用 `Optional` 包装，或明确约定默认实例的安全使用范围

## 迭代第 13 轮

1. **问题描述**：FallbackAiService 通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 注入，因其本身标注 `@Primary` 且 implements AiService，导致 `ObjectProvider.getIfAvailable()` 返回自身实例，引发无限递归 StackOverflowError
   - 所在位置：Section 3.4「Bean 装配策略」
   - 严重程度：严重
   - 改进建议：方案 A：FallbackAiService 不标注 `@Primary`，业务模块通过 `@Qualifier("fallbackAiService")` 按名称注入；方案 B：内部通过 `ApplicationContext.getBeanNamesForType(AiService.class)` 排除自身后取首个可用实例；方案 C：引入 `AiServiceRegistry` 中间层

2. **问题描述**：Section 2.1 目录树中 patient/doctor/admin 模块仅列出 api/、service/、repository/、entity/ 四个子目录，缺失 dto/ 和 converter/，与 Section 2.3 包命名规范不一致
   - 所在位置：Section 2.1 vs Section 2.3
   - 严重程度：一般
   - 改进建议：在 Section 2.1 目录树中补全 dto/ 和 converter/ 子目录

3. **问题描述**：装配条件汇总表描述 `ai.mock.enabled=false` 时「激活的 AiService 实现」为「真实 AiService 实现」，但 Phase 0 无真实实现，最终仅 FallbackAiService 返回降级结果，可能误导 QA 或运维人员
   - 所在位置：Section 3.4「装配条件汇总表」
   - 严重程度：一般
   - 改进建议：增加说明 Phase 0 下 `ai.mock.enabled=false` 的行为，或明确注明 Phase 0 仅支持 `ai.mock.enabled=true`

## 迭代第 14 轮

1. **问题描述**：Section 2.2 依赖关系图中 patient/doctor/admin 模块箭头指向 common-module-impl，违反业务模块不可见 common-module-impl 原则
   - 所在位置：Section 2.2，第 245–261 行
   - 严重程度：严重
   - 改进建议：箭头指向 common-module-api，并注明 common-module-impl 仅对 application 模块可见

2. **问题描述**：父 POM dependencyManagement 缺少 spring-boot-starter-security 条目
   - 所在位置：Section 2.1 第 146–191 行、Section 2.2 第 279 行
   - 严重程度：一般
   - 改进建议：在 dependencyManagement 中添加 spring-boot-starter-security 条目

3. **问题描述**：前端 build:all 脚本对 packages 范围定义不准确，CI 环境不安装 build 脚本将导致 workspace 构建失败
   - 所在位置：Section 2.4 第 352–380 行、Section 10 第 1152 行
   - 严重程度：一般
   - 改进建议：明确 shared/ui-core 是否需要构建，调整 build:all 脚本使用 --if-present 并限定构建范围

4. **问题描述**：DegradationStrategy 泛型 <T, R> 与 FallbackAiService 13 个方法的泛型签名类型约束未统一
   - 所在位置：Section 3.4 第 600 行
   - 严重程度：一般
   - 改进建议：建议方案 A：取消泛型，按 fallback 方法返回值类型在 FallbackAiService 中统一管理

## 迭代第 16 轮

1. **问题描述**：4.3 节 AI 调用契约图中的 `DegradationStrategy.fallback` 引用已不存在的方法
   - 所在位置：第 4.3 节，第 677–683 行（流程图）
   - 严重程度：严重
   - 改进建议：将 4.3 节流程图中的 `DegradationStrategy.fallback(request)` 替换为 `DegradationStrategy.shouldDegrade(context)` + 直接返回 `AiResult(success=false, degraded=true, data=null)`

2. **问题描述**：springdoc-openapi 配置在生产环境暴露 Swagger UI 的安全风险未处理
   - 所在位置：第 8.3 节，第 1011–1020 行（springdoc 配置块）
   - 严重程度：中等
   - 改进建议：将 springdoc 配置移至 `application-dev.yml` 或要求在 `application-prod.yml` 中显式禁用

3. **问题描述**：运行时环境要求未明确说明
   - 所在位置：通篇缺失，应在第 1 节「概述」或第 9 节「本地开发体验」中补充
   - 严重程度：中等
   - 改进建议：补充 JDK 17+、Node.js 18+/20 LTS、npm 9+ 等最低版本要求

4. **问题描述**：PageQuery.page/PageQuery.sort 的边界条件和异常输入未定义
   - 所在位置：第 3.1 节「PageQuery / PageResponse<T>」字段描述
   - 严重程度：低
   - 改进建议：添加 `@Min(0)` 注解并说明 sort 格式无效时的异常处理方式

5. **问题描述**：前端骨架缺少明确的验收标准
   - 所在位置：第 4.1 节「健康检查」及整体缺失
   - 严重程度：低
   - 改进建议：在 4.1 节补充前端三端应用的验收标准
