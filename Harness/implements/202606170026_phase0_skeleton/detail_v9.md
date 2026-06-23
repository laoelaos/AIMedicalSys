# 详细设计（v9）

## 概述

创建三个业务模块骨架（patient/doctor/admin），每个模块提供占位 Controller、Service 接口+实现、Repository 骨架、Entity 骨架、DTO 占位和 Converter 占位，并同步更新父 POM（在 `backend/pom.xml` 的 `<modules>` 中依次添加 `patient`、`doctor`、`admin`），同时在 `application/pom.xml` 中为三个新模块添加 compile 范围依赖。设计目标：在 Phase 0 骨架已经具备 common 基础模块、权限模型和 AI 能力模块的前提下，补齐业务模块的工程占位，使后端依赖树完整可编译，使 application 启动层可以正确引用。三个模块采用相同的占位模板模式，不包含任何业务逻辑。

## 模块划分

### 目录布局

三个业务模块均位于 `backend/` 下，与 `common/`、`modules/`、`application/` 同级：

```
backend/
├── pom.xml                          # 父 POM（聚合 + 依赖管理）
├── common/                          # 共享基础模块（已存在）
├── ai-api/                          # AI 能力接口模块（已存在）
├── ai-impl/                         # AI 能力实现模块（已存在）
├── common-module-api/               # 公共业务接口模块（已存在）
├── common-module-impl/              # 公共业务实现模块（已存在）
├── patient/                         # 患者模块（新建）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/aimedical/modules/patient/
│       │   ├── api/PatientController.java
│       │   ├── service/PatientService.java
│       │   ├── service/impl/PatientServiceImpl.java
│       │   ├── repository/PatientRepository.java
│       │   ├── entity/PatientEntity.java
│       │   ├── dto/PatientDto.java
│       │   └── converter/PatientConverter.java
│       └── test/java/com/aimedical/modules/patient/
│           └── PatientPlaceholderTest.java   # 纯 POJO 占位测试，不加载 Spring 上下文
├── doctor/                          # 医生模块（新建，结构同 patient，含 DoctorPlaceholderTest）
├── admin/                           # 管理员模块（新建，结构同 patient，含 AdminPlaceholderTest）
├── application/                     # 启动聚合模块（已存在）
└── integration/                     # 集成测试模块（已存在）
```

### 模块职责

| 模块 | 职责定位 | Phase 0 范围 |
|------|---------|-------------|
| `patient` | 患者端业务能力：注册登录、智能问诊、挂号、报告查询等 | 仅占位 Controller/Service，不实现任何业务逻辑 |
| `doctor` | 医生端业务能力：挂号管理、门诊接诊、检查/检验/药房管理等 | 仅占位 Controller/Service，不实现任何业务逻辑 |
| `admin` | 管理员端业务能力：基础数据维护、排班管理、综合管理等 | 仅占位 Controller/Service，不实现任何业务逻辑 |

### 依赖方向

```
patient ─> common
patient ─> common-module-api
patient ─> ai-api
patient ─> spring-boot-starter-web          (@RestController, @RequestMapping)
patient ─> spring-boot-starter-data-jpa     (@Entity, JpaRepository)
patient ─> spring-boot-starter-validation   (Phase 1+ 校验注解)

doctor ─> common
doctor ─> common-module-api
doctor ─> ai-api
doctor ─> spring-boot-starter-web
doctor ─> spring-boot-starter-data-jpa
doctor ─> spring-boot-starter-validation

admin ─> common
admin ─> common-module-api
admin ─> ai-api
admin ─> spring-boot-starter-web
admin ─> spring-boot-starter-data-jpa
admin ─> spring-boot-starter-validation
```

三个业务模块之间**不允许互相依赖**。application 模块依赖 patient/doctor/admin 各模块的 compile 产物（在 `application/pom.xml` 中显式声明）。

### 模块 POM 依赖清单

每个模块 POM 显式声明以下依赖（以 patient 为例，doctor/admin 同）：

| Maven artifact | scope | 用途 |
|---------------|-------|------|
| `com.aimedical:common` | compile | 共享异常/响应/工具类 |
| `com.aimedical:common-module-api` | compile | 公共业务 DTO/接口 |
| `com.aimedical:ai-api` | compile | AI 能力调用接口 |
| `spring-boot-starter-web` | compile | `@RestController`、`@RequestMapping` |
| `spring-boot-starter-data-jpa` | compile | `@Entity`、`JpaRepository` |
| `spring-boot-starter-validation` | compile | `@Valid`、校验注解（Phase 1+ 启用） |
| `spring-boot-starter-test` | test | 单元测试骨架 |

> 原因：`common/pom.xml` 将 `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa` 声明为 `<optional>true</optional>`，不会透传给业务模块，故各模块必须显式声明。

## 核心抽象

### PatientController / DoctorController / AdminController（占位 @RestController）

**形态**：class，标注 `@RestController` + `@RequestMapping("/api/{module}")`

**职责**：为对应终端提供 Phase 0 可验证的 REST 端点占位。每个 Controller 提供至少一个 GET 占位方法，委托对应 Service 占位方法获取结果（Controller 不直接生产返回值），确保 application 模块启动后可以通过 `/api/patient`、`/api/doctor`、`/api/admin` 验证模块可达。

**协作对象**：
- 返回 `Result<T>`（common 模块）
- 不注入 `AiService` 或任何其他业务模块的 Service（Phase 0 约束）

**为何使用 class 而非 interface**：Controller 是 Spring 管理的具体端点容器，单一职责、无多态需求。

### PatientService / DoctorService / AdminService（interface）

**形态**：interface，定义在 `service/` 子包

**职责**：为 Controller 层提供业务方法契约。Phase 0 仅声明占位方法签名，不包含业务逻辑。

**为何使用 interface**：业务层接口将"做什么"与"怎么做"分离，使 Controller 只依赖接口、不依赖实现，为 Phase 1+ 的多种实现（Mock / 真实业务 / 测试替身）预留扩展点。

### PatientServiceImpl / DoctorServiceImpl / AdminServiceImpl（占位 @Service）

**形态**：class，标注 `@Service`，位于 `service/impl/` 子包，实现对应的 Service interface

**职责**：Phase 0 提供占位方法实现，每个方法返回 `Result.success("{module} placeholder")`，使 Controller 可委托获得占位响应。满足编译期依赖注入要求。

**协作**：被 Controller 通过接口注入，Phase 0 无需注入任何 Repository 或其他模块依赖。

### PatientRepository / DoctorRepository / AdminRepository（JPA Repository 骨架）

**形态**：interface，`extends JpaRepository<PatientEntity, Long>`

**职责**：Phase 0 为空的 Repository 骨架声明，仅通过 `extends JpaRepository` 继承基础 CRUD 方法。Phase 1+ 按业务需求扩充。

**为何继承 JpaRepository 而非 CrudRepository**：JpaRepository 提供 `getOne`、`findAll(Sort)`、`findAll(Pageable)` 等分页与排序能力，与 Phase 0 约定的 `PageQuery`/`PageResponse` 分页体系自然匹配。

### PatientEntity / DoctorEntity / AdminEntity（占位实体）

**形态**：class，标注 `@Entity`，`extends BaseEntity`

**职责**：Phase 0 提供空实体（仅继承 BaseEntity 的 id/createdAt/updatedAt/deleted），满足 Repository 的泛型约束。Phase 1+ 按业务需求扩充业务字段。

**为何使用 JPA @Entity 而非普通 POJO**：Repository 的泛型参数要求实体类型为 `@Entity`，普通 POJO 无法通过 Hibernate 的委托校验。

### PatientDto / DoctorDto / AdminDto（DTO 占位）

**形态**：class（或 Java 16+ record）

**职责**：Phase 0 为空的 DTO 占位，满足 Controller → Service 方法签名的编译期要求。Phase 1+ 按业务需求扩充传输字段。

### PatientConverter / DoctorConverter / AdminConverter（Converter 占位）

**形态**：interface 或 class（取决于团队偏好），定义 `Entity → DTO` 与 `DTO → Entity` 的转换方法签名

**职责**：Phase 0 提供空的转换方法声明（或空方法体），为 Phase 1+ 的转换逻辑建立目录约定和代码结构。

## 关键行为契约

### 模块可达性验证

```
GET /api/patient/placeholder
→ 200 OK, Result<String> { code: "SUCCESS", data: "patient placeholder" }

GET /api/doctor/placeholder
→ 200 OK, Result<String> { code: "SUCCESS", data: "doctor placeholder" }

GET /api/admin/placeholder
→ 200 OK, Result<String> { code: "SUCCESS", data: "admin placeholder" }
```

### Controller → Service 协作

```
PatientController
  └─> PatientService (interface)
       └─> PatientServiceImpl (@Service)
            └─> 返回 Result.success("placeholder")
```

Controller 通过构造器注入 Service 接口，不感知具体实现类。Phase 0 所有占位方法均返回固定字符串。

### Repository → Entity 关系

```
PatientRepository extends JpaRepository<PatientEntity, Long>
                              └─> PatientEntity extends BaseEntity
```

Repository 的实体泛型参数使 Spring Data JPA 可以正确推断表名和主键类型。Phase 0 不定义任何自定义查询方法。

## 错误处理策略

三个业务模块的占位 Controller 复用 common 模块的 `GlobalExceptionHandler`，无需在每个模块内重复定义。Phase 0 占位方法不会抛出业务异常，全局异常处理器只需覆盖系统异常即可。

| 场景 | 处理方式 |
|------|---------|
| 占位方法正常返回 | `Result.success("placeholder")`，无错误处理逻辑 |
| 系统异常（NPE、序列化失败等） | 由 common 的 `GlobalExceptionHandler` 统一捕获并返回 `Result.fail(SYSTEM_ERROR)` |

## 并发设计

Phase 0 业务模块无业务功能，不涉及并发问题。Spring Boot 默认 Tomcat 线程池（200）可满足占位端点的简单请求处理。

## 占位测试契约

每个模块提供一个纯 POJO 占位测试类（如 `PatientPlaceholderTest.java`），**不使用** `@SpringBootTest`、`TestRestTemplate` 或 `MockMvc`，仅确保测试框架可发现并执行测试。

| 验证项目 | 方式 | 预期 |
|---------|------|------|
| 测试框架可达 | JUnit 5 `@Test` 空方法 | 测试运行器可发现并执行测试类，无类加载错误 |

```java
class PatientPlaceholderTest {
    @Test
    void placeholder() {}
}
```

三个模块的测试模板相同，仅类名和包名不同。Phase 1+ 在 `integration` 模块中补充 `@SpringBootTest` 集成测试，覆盖 Controller 端点可达性验证（通过 `TestRestTemplate` 或 `MockMvc` 调用 `GET /api/{module}/placeholder`，验证返回 `200 OK` 且 data 为 `"{module} placeholder"`）。

## 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 模块位置 | `modules/` 子目录 vs 根级目录 | 根级目录（与 common、application 同级） | OOD §2.1 的 `modules/` 布局为中期规划，Phase 0 按现有惯例放置在根级，与已存在的 common/、application/ 保持一致 |
| 模块 POM 父引用 | 直接继承父 POM vs 通过中间聚合 POM | 直接继承父 POM | 三个模块为叶子模块，无需中间聚合 POM。如后续需要按子树构建，可参照 ai 模块或 common-module 模式添加中间层聚合 |
| Controller 占位方法命名 | `/placeholder` vs `/ping` | `/placeholder` | 避免与 application 模块的 `/api/ping` 健康检查端点语义混淆；placeholder 明确表示"本模块占位可达，业务逻辑待实现" |
| Service 形态 | interface + impl 分离 vs 单 class | interface + impl 分离 | 符合 Spring Boot 分层架构的 Controller-Service-Repository 三层分离规范；interface 为 Phase 1+ 的多实现预留扩展点 |
| Entity 基础字段 | 仅继承 BaseEntity vs 自带占位字段 | 仅继承 BaseEntity | Phase 0 不冻结业务字段，避免在需求未冻结前引入无用字段 |
| DTO 形态 | class vs Java 16+ record | class（兼容 JDK 17 语法） | 团队可能使用 Lombok，class 是最低公共分母；record 可作为备选项 |
| Converter 形态 | interface vs abstract class vs 单 class | interface | 定义 Entity ↔ DTO 的转换契约，interface 使 Controller/Service 层可以依赖抽象的转换逻辑 |
| 构建顺序 | patient/doctor/admin 互不依赖 | 任意顺序并行构建 | 三个模块之间无依赖关系，Maven reactor 可同时构建 |
| 父 POM 遗漏依赖豁免 | 按需逐模块配置 vs 统一追加列表 | 在父 POM 的 `<ignoredUnusedDeclaredDependency>` 中为 patient/doctor/admin 各添加一条配置 | `application/pom.xml` 对三个新模块声明 compile 依赖后，若不加入豁免列表将导致 `mvn dependency:analyze` 报告"declared but unused"告警；统一追加比按需逐模块配置更简洁 |
## 修订说明（v9 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 新模块 POM 缺少 spring-boot-starter-web 和 spring-boot-starter-data-jpa 依赖声明（common 声明为 optional，无法透传） | 在「依赖方向」中补充 starter 依赖链；新增「模块 POM 依赖清单」表格，逐一列出每个模块 POM 必须声明的 artifactId、scope 及用途，并注明原因 |
| [一般] 未明确要求更新父 POM `<modules>` 区域 | 在概述中将"同步更新父 POM"扩展为"在`backend/pom.xml`的`<modules>`中依次添加 patient、doctor、admin"，给出精确位置 |
| [一般] 未明确要求更新 application/pom.xml 依赖 | 在概述和依赖方向中均补充说明：application/pom.xml 须为 patient/doctor/admin 添加 compile 范围 `<dependency>` |
| [轻微] 设计决策表提及的 ignoredUnusedDeclaredDependencies 配置在父 POM 中已存在，属冗余陈述 | 修改设计决策表该行，将"统一配置"改为"利用已有配置"，并注明具体行号（backend/pom.xml:108-111） |
| [轻微] PatientPlaceholderTest.java 仅提及文件名，未指定测试内容或验证目标 | 在目录树中补充注释说明验证契约；新增「占位测试契约」章节，定义 Spring 上下文加载和模块端点可达两项测试目标 |

## 修订说明（v9 r2）

| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 父 POM `ignoredUnusedDeclaredDependency` 配置覆盖范围错误断言：原设计称 `backend/pom.xml:108-111` 已统一配置、新模块直接继承无需重复配置；实际该清单仅含 `ai-api` 和 `common-module-api`，不涵盖 `patient`/`doctor`/`admin` | 修正设计决策表对应行：改为"在父 POM 的 `<ignoredUnusedDeclaredDependency>` 中为 patient/doctor/admin 各添加一条配置"，并更新理由说明 |
| [轻微] 目录树中将现有模块展示为 `modules/ai/` 和 `modules/common-module/`，与实际扁平结构（`ai-api/`、`ai-impl/`、`common-module-api/`、`common-module-impl/` 位于根级）不一致 | 将目录树中的 `modules/` 中间层替换为实际存在的扁平模块路径，与 `ls` 输出一致 |

## 修订说明（v9 r3）

| 审查意见 | 修改措施 |
|---------|---------|
| [一般] `@SpringBootTest` 占位测试设计在多模块结构下不可行：业务模块无独立 `@SpringBootConfiguration`，`application` 模块亦无主启动类，`@SpringBootTest` 将因找不到引导配置而失败 | 「占位测试契约」章节改为纯 POJO 占位测试：删除 `@SpringBootTest`/`TestRestTemplate`/`MockMvc` 要求，改为 JUnit 5 单测空方法，仅验证测试框架可达；将集成测试推迟至 Phase 1+ 在 `integration` 模块中补充 |
| [轻微] Controller 职责描述"返回 `Result.success("placeholder")`"与行为契约中"Service 生产、Controller 透传"的描述不完全一致 | 「核心抽象/PatientController」职责改为"委托对应 Service 占位方法获取结果，Controller 不直接生产返回值"；「PatientServiceImpl」职责补充"每个方法返回 `Result.success("{module} placeholder")`"以明确职责归属 |

DESIGN_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\implements\202606170026_phase0_skeleton\detail_v9.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
