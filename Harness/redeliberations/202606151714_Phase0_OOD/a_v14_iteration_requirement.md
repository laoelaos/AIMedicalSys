根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：FallbackAiService 的 `ObjectProvider<AiService>` 注入存在自引用循环依赖（运行时 StackOverflow）
- **所在位置**：Section 3.4「Bean 装配策略」
- **描述**：FallbackAiService 标注 `@Primary` 且 implements AiService，内部通过 `ObjectProvider<AiService>` 延迟获取底层实现。`ObjectProvider.getIfAvailable()` 按类型查找时返回 @Primary 标注的 Bean——即 FallbackAiService 自身，导致无限递归 → StackOverflowError
- **严重程度**：严重
- **建议**：方案 A：FallbackAiService 不标 `@Primary`，业务模块通过 `@Qualifier("fallbackAiService")` 按名称注入；方案 B：内部通过 `ApplicationContext.getBeanNamesForType(AiService.class)` 排除自身后取首个可用实例；方案 C：引入 `AiServiceRegistry` 中间层

### 问题 2（一般）：目录布局与包命名规范之间存在不一致（dto/converter 子包缺失）
- **所在位置**：Section 2.1（Monorepo 目录布局）vs Section 2.3（包命名规范）
- **描述**：Section 2.3 中 patient/doctor/admin 模块包含 api/、dto/、service/、repository/、entity/、converter/ 六个子包，但 Section 2.1 目录树中 patient/ 仅列出 api/、service/、repository/、entity/ 四个，缺失 dto/ 和 converter/
- **严重程度**：一般
- **建议**：在 Section 2.1 目录树中补全 dto/ 和 converter/

### 问题 3（一般）：`ai.mock.enabled=false` 在 Phase 0 下的实际行为与装配条件汇总表描述不一致
- **所在位置**：Section 3.4「装配条件汇总表」
- **描述**：原表描述 `ai.mock.enabled=false` 时激活「真实 AiService 实现」，但 Phase 0 不存在真实实现。最终仅 FallbackAiService 可用，返回降级结果
- **严重程度**：一般
- **建议**：增加 Phase 0 下的行为说明，或注明 Phase 0 仅支持 `ai.mock.enabled=true`

### 问题 4（轻微）：`PermissionService.getUserPermissions()` 返回值语义不明确
- **所在位置**：Section 8.4「PermissionService 门面接口定义」
- **描述**：`Set<String> getUserPermissions(Long userId)` 中 String 的具体含义未定义
- **严重程度**：轻微
- **建议**：明确 String 语义（如功能权限编码 `"{module}:{action}"`），或定义 `PermissionCode` 值类型

### 问题 5（轻微）：`mvn spring-boot:run` 一键启动命令缺少依赖模块编译支持
- **所在位置**：Section 9.3「一键启动」
- **描述**：`mvn spring-boot:run -pl application` 首次构建时依赖模块未安装到本地仓库，解析失败
- **严重程度**：轻微
- **建议**：补充 `-am` 参数或添加前置安装说明

### 问题 6（轻微）：错误分类表中「配置加载失败」的 HTTP 状态码与实际场景矛盾
- **所在位置**：Section 5.1「错误分类表」
- **描述**：应用启动失败发生在 HTTP 容器就绪之前，标 500 会产生误导
- **严重程度**：轻微
- **建议**：将 HTTP 状态码改为 `N/A（启动失败，无 HTTP 响应）`

### 问题 7（轻微）：前端 `packages/shared` 与 `packages/ui-core` 的导出入口内容未定义
- **所在位置**：Section 2.4「前端模块划分」
- **描述**：`shared/package.json` 和 `ui-core/package.json` 的 `main` 指向 `src/index.ts`，但未定义应导出哪些内容
- **严重程度**：轻微
- **建议**：补充 `src/index.ts` 的导出内容说明

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但本轮不再提及）
- 第 1 轮全部 8 个问题（AI 方法中文括号、权限实体归属、同步非阻塞表述、BaseEntity 字段定义、MockAiService 注入、SecurityConfig 骨架、User-UserDetails 适配、ai-api/ai-impl 分离）
- 第 2 轮全部 6 个问题（ai.mock.enabled 默认值、AI 方法 DTO 类型、配置加载失败事实错误、ui-core 定义、SecurityConfig 归属、CI 流水线）
- 第 3 轮全部 4 个问题（CI compile→install、FallbackAiService 按名称注入、DegradationStrategy 签名、嵌套 DTO 定义）
- 第 4 轮全部 9 个问题（@EnableJpaAuditing、SecurityConfig permitAll、ObjectProvider 方案、PageQuery 0-based、deleted boolean 类型、OpenAPI 生成、ErrorCode interface、Integration 模块用途、API 版本管理声明）
- 第 5 轮全部 5 个问题（common security dependency、模块调用规范、组件扫描、BusinessException 继承、PageRequest→PageQuery 重命名）
- 第 6 轮全部 4 个问题（H2 数据库驱动、Vite 代理配置、CI 重复命令、common-module api 子包）
- 第 7 轮全部 4 个问题（common data-jpa 依赖、真实 AiService 注册、ScheduleRequest.doctorIds 类型、springdoc-openapi 配置）
- 第 8 轮全部 4 个问题（spring-boot-starter-web 依赖、spring-boot-starter-test 依赖、spring-boot-starter-validation 依赖、前端 workspaces 配置）
- 第 9 轮全部 9 个问题（User→UserDTO 返回类型、非标准 Maven 属性、事件 ApplicationEvent→POJO、page size @Max 约束、DateRange→startDate/endDate、ai.mock.enabled 配置示例、Mock 可选字段约定、H2 Console 安全、ApiClient 错误拦截器）
- 第 10 轮全部 4 个问题（AiResult<T> 返回类型统一、common-module api/impl 拆分、FallbackAiService-DegradationStrategy 协作、前端占位页面结构）
- 第 11 轮全部 4 个问题（Integration 模块 fat JAR、surefire.skip→mvn verify、中间层聚合 POM、前端 CI 依赖安装）
- 第 12 轮全部 2 个问题（-Dsurefire.skip 失效、DegradationContext null 防御）

### 持续存在的问题（需重点解决）
- **FallbackAiService 自引用循环依赖**（第 3 轮 Issue 2→第 4 轮 Issue 3→第 13 轮 Issue 1→本轮 Issue 1）：先后尝试了 @Resource 按名称注入、ObjectProvider 延迟获取等方案，均未根本解决。本轮需采用非 @Primary 方案彻底修复
- **装配条件汇总表 `ai.mock.enabled=false` 描述不一致**（第 2 轮 Issue 1→第 13 轮 Issue 3→本轮 Issue 3）：多次修正后仍有误导性描述，本轮应明确标注 Phase 0 下不支持 `false` 配置或补充降级路径说明
- **配置加载失败 HTTP 状态码表述**（第 2 轮 Issue 3→本轮 Issue 6）：虽已修正了「运行时捕获」的错误描述，但启动失败的 HTTP 状态码标注仍不准确

### 新发现的问题
- Issue 2（目录树缺失 dto/converter）：已在第 13 轮被识别，本轮再次出现，需彻底修复
- Issue 4（PermissionService 返回值语义）、Issue 5（Maven 命令 -am）、Issue 7（前端导出入口）：本轮首次识别

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v13_copy_from_v12.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
