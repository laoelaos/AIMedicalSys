# 待办事项

---

- [ ] `[严重]` *(无严重问题)*

---

- [ ] `[一般] BaseEnum 为设计文档未定义类型` — 位置：`common/.../base/BaseEnum.java:1-6` — 描述：BaseEnum interface 存在于 common.base 包中，符合 §2.1 目录布局规划（base/ 含 BaseEntity, BaseEnum），但 §3.x 核心抽象规范中**未对该类型做任何定义或说明**。缺少字段/方法命名约定、使用规范或与 ErrorCode 接口的关系说明。 — 来源：`review_v1_A1.md`

- [ ] `[一般] 父 POM modules 路径与设计 §2.1 不一致` — 位置：`AIMedical/backend/pom.xml:18-29` — 描述：设计文档 §2.1 明确使用分层目录结构 modules/common-module/common-module-api、modules/ai/ai-api、modules/patient 等，但实际 modules 声明为扁平路径 backend/common-module-api、backend/ai-api、backend/patient（即模块直接位于 backend/ 根目录，无 modules/ 中间层）。实际目录结构也证实不存在 modules/ 目录。 — 来源：`review_v1_A2.md`

- [ ] `[一般] 聚合 POM 缺失` — 位置：`modules/common-module/pom.xml`、`modules/ai/pom.xml`（均不存在） — 描述：设计 §2.1 明确指定 modules/common-module/pom.xml 聚合 common-module-api 和 common-module-impl，以及 modules/ai/pom.xml 聚合 ai-api 和 ai-impl。两个聚合 POM 均未创建。由于实际采用扁平目录结构且根 POM 直接引用叶子模块，聚合 POM 的功能完全缺失。 — 来源：`review_v1_A2.md`

- [ ] `[一般] Spring Boot 版本与设计不一致` — 位置：`AIMedical/backend/pom.xml:9` — 描述：设计文档 §2.1 父 POM 骨架指定 spring-boot-starter-parent 版本为 3.3.0，实际 POM 使用 3.2.5。 — 来源：`review_v1_A2.md`

- [ ] `[一般] dependencyManagement 缺少外部 starter 统一声明` — 位置：`AIMedical/backend/pom.xml:39-101` — 描述：设计 §2.2 "依赖管理（父 POM）"要求父 POM 的 `<dependencyManagement>` 统一声明 spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-security、spring-boot-starter-validation、spring-boot-starter-test 的版本。实际 `<dependencyManagement>` 仅声明了 springdoc-openapi-starter-webmvc-ui、h2 和内部模块，完全缺少上述外部 starter 条目。 — 来源：`review_v1_A2.md`

- [ ] `[一般] Common POM 引入 spring-boot-starter-validation (optional)` — 位置：`AIMedical/backend/common/pom.xml:27-31` — 描述：设计 §2.2 的 common 模块依赖规约仅指定 spring-boot-starter-web (optional) 和 spring-boot-starter-data-jpa (optional)，但实际 POM 额外引入了 spring-boot-starter-validation 作为 optional。设计文档未授权 common 模块声明 validation 依赖。 — 来源：`review_v1_A2.md`

- [ ] `[一般] 父 POM maven-dependency-plugin 额外忽略 business 模块` — 位置：`AIMedical/backend/pom.xml:109-115` — 描述：设计 §2.2 的 `<ignoredUnusedDeclaredDependencies>` 仅包含 ai-api 和 common-module-api 两个条目。实际 POM 额外添加了 patient、doctor、admin 三个豁免条目。这些 business 模块是 application 模块的真实编译依赖，不应豁免。该配置会继承到所有子模块，可能导致合法的 unused dependency 问题被掩盖。 — 来源：`review_v1_A2.md`

- [ ] `[一般] GlobalExceptionHandler 缺少 HttpMessageNotReadableException / HttpMessageNotWritableException 专用处理器` — 位置：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java:30-35` — 描述：设计文档 §5.3 明确要求注册 @ExceptionHandler 方法处理 HttpMessageNotReadableException（请求体序列化错误 → 400）和 HttpMessageNotWritableException（响应体序列化错误 → 500），但当前实现仅通过通用的 @ExceptionHandler(Exception.class) 兜底捕获，实际会返回 500 SYSTEM_ERROR 而非正确的 400/500 区分。当前行为与设计约定部分偏离。 — 来源：`review_v2_A2.md`

- [ ] `[一般] FallbackAiServiceTest 未验证空委托列表的日志输出` — 位置：`AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java:35-42` — 描述：shouldReturnFallbackResultWhenNoDelegateAvailable() 正确验证了空委托列表场景下返回 degraded=true, success=false, fallbackReason="No available AiService delegate"，但设计文档 §3.4 还要求启动期输出 ERROR 日志、运行期输出 WARN 日志。该行为未在测试中验证。 — 来源：`review_v2_A2.md`

- [ ] `[一般] ApiClient 错误拦截器未按 §3.5 实现 NETWORK_ERROR 处理` — 位置：`packages/shared/src/api/index.ts:16-18` — 描述：设计文档 §3.5 要求 Axios 错误拦截器统一捕获网络错误（DNS 解析失败、连接超时、请求被取消等），返回固定格式 `{ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" }`。当前实现仅为 return Promise.reject(error)，直接透传原始 Axios 错误，未做任何转换。前端代码若依赖 code === "NETWORK_ERROR" 判断网络错误将无法生效，未捕获的 Promise 异常可能在运行期导致控制台报错。 — 来源：`review_v2_A3.md`
