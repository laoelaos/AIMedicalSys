# R1-A2: 审查后端业务模块层实现与 OOD 设计一致性

审查时间：2026-06-18

### 审查范围

#### AI 能力模块 (ai-api / ai-impl)
- `AIMedical/backend/modules/ai/pom.xml`
- `AIMedical/backend/modules/ai/ai-api/pom.xml`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiService.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationContext.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationStrategy.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/**/*.java` (13 组 DTO)
- `AIMedical/backend/modules/ai/ai-impl/pom.xml`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/degradation/NoOpDegradationStrategy.java`

#### Common Module (common-module-api / common-module-impl)
- `AIMedical/backend/modules/common-module/pom.xml`
- `AIMedical/backend/modules/common-module/common-module-api/pom.xml`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/api/UserType.java`
- `AIMedical/backend/modules/common-module/common-module-impl/pom.xml`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java`

#### 业务模块 (patient / doctor / admin)
- 各模块 POM (`modules/patient/pom.xml`, `modules/doctor/pom.xml`, `modules/admin/pom.xml`)
- 各模块 Controller / Service 接口和实现 / Entity / Repository / Converter / DTO

### 发现

#### [轻微] TriageRequest.chiefComplaint 字段未标记 @phase0-mock-field

- **位置**：`AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageRequest.java:5`
- **描述**：OOD §8.2 Phase 0 Mock 字段子集明确要求 `TriageRequest.chiefComplaint` 标记 `@phase0-mock-field`，但实际代码中该字段无任何相应标记（Java 注解或注释）。目前字段存在且被 MockAiService 使用，功能正确，但缺少 OOD 约定的标记。
- **建议**：在 `chiefComplaint` 字段上方添加注释 `// @phase0-mock-field`，或自定义 Java 注解 `@Phase0MockField` 并在该字段上标注。

#### [轻微] PatientDto / DoctorDto / AdminDto 未显式声明默认构造器

- **位置**：`AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/dto/PatientDto.java:3`（及同级 doctor/admin DTO）
- **描述**：这三个业务模块 DTO 为空类，未显式声明默认构造器。尽管 Java 编译器会生成隐式默认构造器，但 OOD §8.2 的"两层冻结"策略要求 DTO "保留类声明和默认构造器"以在编译期满足契约。隐式构造器不存在违反，但显式声明更符合 OOD 关于"默认构造器"的表述。
- **建议**：在各空壳 DTO 中显式添加 `public PatientDto() {}` 等无参构造器。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 0 |
| 轻微 | 2 |

### 总评

代码实现整体与 OOD 设计高度一致。关键项全部通过：

1. **AiService 13 个方法** — 全部存在，签名完全匹配 OOD §8.2 表格（13 个 `CompletableFuture<AiResult<T>>` 方法）
2. **AiResult 字段** — `success`, `data`, `errorCode`, `degraded`, `fallbackReason` 五字段完整，含静态工厂方法 `success()`/`failure()`/`degraded()`
3. **MockAiService @ConditionalOnProperty** — `name="ai.mock.enabled"`, `havingValue="true"`, `matchIfMissing=true` 完全正确
4. **FallbackAiService 自引用排除** — 通过 `!(s instanceof FallbackAiService)` 正确排除自身；空委托时返回 `AiResult.degraded()` 符合兜底保护规范
5. **NoOpDegradationStrategy** — `@ConditionalOnMissingBean(DegradationStrategy.class)` 正确，`shouldDegrade()` 返回 false
6. **JPA 映射** — 所有 `@ManyToMany`/`@OneToMany`/`@ManyToOne` 均使用 `FetchType.LAZY`，无 cascade 设置，完全符合 OOD §3.3
7. **关联表命名** — `user_role`, `user_post`, `post_function` 均正确
8. **UserType 枚举引用** — `User.userType` 字段使用 `@Enumerated(EnumType.STRING)` 正确引用 `common-module-api` 中的 `UserType`
9. **业务模块 POM 依赖** — patient/doctor/admin 均正确依赖 `common`, `common-module-api`, `ai-api`；未引入 `ai-impl` 或 `common-module-impl`
10. **Controller 路径** — `/api/patient`, `/api/doctor`, `/api/admin` 前缀完全正确
11. **DTO 两层冻结** — 所有 13 组 ai-api DTO 均保留类声明和默认构造器
12. **DegradationContext** — 仅保留零值构造器，无业务字段，符合 OOD

仅发现 2 个轻微问题，不影响骨架运行与验收。
