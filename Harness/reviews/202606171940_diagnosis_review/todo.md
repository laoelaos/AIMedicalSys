# 待办事项

---

- [ ] T1: [一般] PageQuery 缺少 @Min(0) / @Max(500) 校验注解 — 来源：review_v1_A1.md — 位置：`AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:7-9`

  OOD §3.1 要求 page 标注 @Min(0)、size 标注 @Max(500) 上限防止恶意大分页 OOM，当前代码的 page=0 和 size=20 仅有默认值而无校验注解。

- [ ] T2: [一般] common-module-impl 缺少 config/ 和 dict/ 包目录 — 来源：review_v1_A3.md + review_v3_A1.md — 位置：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/`

  OOD §2.3 包命名规范要求 common-module-impl 包含 permission、config、dict 三个子包，当前仅实现了 permission 包。

- [ ] T3: [一般] 父 POM dependencyManagement 中 Starter 依赖冗余显式版本号 — 来源：review_v2_A2.md — 位置：`AIMedical/backend/pom.xml:84-109`

  父 POM dependencyManagement 中 spring-boot-starter-web/data-jpa/security/validation/test 显式标注了 `<version>3.2.5</version>`，与 spring-boot-starter-parent BOM 统一管理版本的原则重复。

- [ ] T4: [一般] 父 POM dependencyManagement 中 h2 依赖误设 scope — 来源：review_v2_A2.md — 位置：`AIMedical/backend/pom.xml:82`

  父 POM dependencyManagement 中 h2 条目已设 `<scope>runtime</scope>`，违反"dependencyManagement 仅管理版本，scope 由各消费模块自行声明"的设计约定。

- [ ] T5: [一般] common 模块缺少 MeterRegistryCustomizer 占位配置 — 来源：review_v2_A2.md — 位置：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/`

  OOD §10.1 要求在 common 模块的 com.aimedical.common.config 包中声明 MeterRegistryCustomizer 配置类占位（设置通用标签如 application=aimedical-sys），当前不存在。

- [ ] T6: [一般] Axios 响应拦截器未实现 OOD §4.2 规定的 Result.code 拆包逻辑 — 来源：review_v2_A3.md — 位置：`AIMedical/frontend/packages/shared/src/api/index.ts:10-26`

  OOD §4.2 规定响应拦截器应对 Result.code 做拆包（code === "SUCCESS" → 返回 response.data.data，code !== "SUCCESS" → 走错误处理），但实际拦截器返回 response.data 完整包装体。

- [ ] T7: [一般] ai-impl POM 声明了冗余的 common 直接依赖 — 来源：review_v3_A1.md — 位置：`AIMedical/backend/modules/ai/ai-impl/pom.xml:17-20`

  ai-impl POM 同时声明了 com.aimedical:ai-api 和 com.aimedical:common。根据 OOD §2.2，common 作为传递性依赖已可通过 ai-api 获得。

- [ ] T8: [一般] common-module-impl POM 声明了冗余的 common 直接依赖 — 来源：review_v3_A1.md — 位置：`AIMedical/backend/modules/common-module/common-module-impl/pom.xml:17-20`

  common-module-impl POM 同时声明了 common-module-api 和 common，common 作为传递性依赖已可通过 common-module-api 获得。

- [ ] T9: [一般] common 模块缺少 util 包目录 — 来源：review_v3_A1.md — 位置：`AIMedical/backend/common/src/main/java/com/aimedical/common/`

  OOD §2.3 要求 common 模块包含 base、result、exception、util、config 五个子包，当前缺少 util 包。

- [ ] T10: [一般] FallbackAiService ERROR 日志触发时机与 OOD §3.4 不一致 — 来源：review_v3_A3.md — 位置：`AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java:60-67`

  OOD §3.4 要求"启动期输出 ERROR 日志、运行期输出 WARN 日志"，但当前实现仅在首次调用时触发 ERROR，而非在构造器/启动期检查。

- [ ] T11: [一般] BaseEntityTest 未验证审计字段自动填充 — 来源：review_v3_A3.md — 位置：`AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityTest.java:46-48`

  OOD §3.2 明确 createdAt 由 @CreatedDate + AuditingEntityListener 自动填充、updatedAt 由 @LastModifiedDate 自动填充，但当前测试仅验证了 setter/getter 的 POJO 行为，未在 Spring Data JPA 上下文中验证审计监听器是否按预期自动填充时间戳。
