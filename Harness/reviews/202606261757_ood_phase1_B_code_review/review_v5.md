# R5: OOD Phase 1 包 B — 实体、仓库、Schema & 菜单模块

审查时间：2026-06-26

### 审查范围

**实体 & 仓库:**
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PermissionFunction.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/RoleRepository.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PostRepository.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PermissionFunctionRepository.java`

**Schema & 配置:**
- `AIMedical/backend/application/src/main/resources/db/schema.sql`
- `AIMedical/backend/application/src/main/resources/db/data.sql`
- `AIMedical/backend/application/src/main/resources/application.yml`

**菜单模块:**
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/MenuController.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/MenuService.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/MenuResponse.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuCreateRequest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuUpdateRequest.java`

### 发现

#### [一般] 菜单更新端点使用 PUT 而非 PATCH 方法，与设计约定的 PATCH 语义不一致

- **位置**：`MenuController.java:117`
- **描述**：设计文档 4.4 节和 6.1 节要求 `PATCH /api/menu/{id}` 实现局部更新语义（RFC 7231 §4.3.4），但 Controller 使用 `@PutMapping("/{id}")`。PUT 语义是完整替换，与 MenuUpdateRequest 的局部更新设计（省略字段保持不变的 PATCH 策略）矛盾。
- **建议**：改为 `@PatchMapping("/{id}")`

#### [一般] 删除菜单时错误码使用 PARAM_INVALID 而非设计约定的 CHILDREN_EXIST

- **位置**：`MenuServiceImpl.java:165`
- **描述**：设计文档 10.1 节和 6.1 节要求有子菜单阻止删除时返回 ErrorCode.CHILDREN_EXIST（HTTP 400），实现中使用了 `GlobalErrorCode.PARAM_INVALID`。前端无法据此区分"参数错误"和"子菜单阻止删除"两种场景。
- **建议**：在 `GlobalErrorCode` 中新增 `CHILDREN_EXIST` 枚举，替换 `PARAM_INVALID`

#### [一般] MenuController 缺少路径 id 与请求体 id 的一致性校验

- **位置**：`MenuController.java:117-125`
- **描述**：设计文档 5.2 节要求 `PATCH /api/menu/{id}` 的路径参数 `{id}` 与请求体中的 `id` 字段（若携带）必须相同，不一致时返回 400（PARAM_INVALID）。当前 update 方法未实现此校验。
- **建议**：在 `update()` 方法中添加校验逻辑：若请求体 id 非 null 且不等于路径 id，返回错误

#### [一般] PermissionFunction 实体缺少 component 字段映射

- **位置**：`PermissionFunction.java`（整体）
- **描述**：`sys_function` 表存在 `component` 列（存储前端组件路径如 `Layout`, `system/user/index`），但 PermissionFunction 实体未映射该字段。导致：(1) MenuResponse 中的 component 始终为 null；(2) MenuUpdateRequest 中的 component 变更无法持久化；(3) data.sql 中写入的 component 值（如 `Layout`）无法被业务代码读取。
- **建议**：在 PermissionFunction 中增加 `private String component;` 字段映射，并在 `convertToMenuResponse` 中传入该值

#### [一般] getUserMenuTree 未使用 @EntityGraph，存在 N+1 查询风险

- **位置**：`MenuServiceImpl.java:44`
- **描述**：`getUserMenuTree()` 使用 `userRepository.findById(userId)`（无 EntityGraph），随后遍历 `user.getPosts()`（触发懒加载 N 次查询）和 `post.getFunctions()`（触发额外 N 次查询）。UserRepository 已定义 `findWithDetailsById` 带有 `@EntityGraph(attributePaths = {"roles", "posts"})`，但未被使用。
- **建议**：将 `findById` 替换为 `findWithDetailsById` 以预先加载 posts 集合，避免 N+1

#### [一般] getMenuById 缺少 @Transactional(readOnly=true) 注解

- **位置**：`MenuServiceImpl.java:175-180`
- **描述**：`getMenuById` 是纯查询操作，但未覆盖 `@Transactional(readOnly = true)`，继承了类级别的读写事务。
- **建议**：添加 `@Transactional(readOnly = true)` 以匹配其他查询方法

#### [轻微] User.passwordChangeRequired 注解缺 columnDefinition

- **位置**：`User.java:51`
- **描述**：设计文档 4.3 节约定 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`，实际仅 `@Column(nullable=false)`。不影响运行（schema.sql 已正确），但与 JPA 注解约定不一致。
- **建议**：补全 `columnDefinition="BIT(1) DEFAULT 0"` 以对齐设计文档

#### [轻微] MenuController 直接操作 SecurityContextHolder 而非使用 CurrentUser 接口

- **位置**：`MenuController.java:148-158`
- **描述**：设计文档 1.3 节定义 `CurrentUser` 接口作为当前用户的类型化访问器，消除 Controller 层对 `SecurityContextHolder` 的直接操作。MenuController 的 `getCurrentUserId()` 直接调用 `SecurityContextHolder.getContext().getAuthentication()`，未注入 `CurrentUser`。
- **建议**：注入 `CurrentUser` 接口替换当前实现

#### [轻微] MenuServiceImpl 中 inline 转换逻辑未提取到 MenuConverter

- **位置**：`MenuServiceImpl.java:182-193`
- **描述**：设计文档 2.1 节目录结构规划了 `converter/MenuConverter.java` 用于 PermissionFunction → MenuResponse 转换，但该文件不存在。转换逻辑以私有方法 `convertToMenuResponse` 内联在 Service 中。
- **建议**：提取 MenuConverter 类以遵循单一职责原则，与 UserConverter 模式保持一致

### 已确认通过项

以下检查点全部通过，符合设计文档要求：

1. **User.java**: `passwordChangeRequired` 和 `tokenVersion` 已添加，均有 `@Column(nullable=false)` ✅
2. **User.java**: `enabled` 有 `@Column(nullable=false)` ✅
3. **Role.java**: `enabled` 有 `@Column(nullable=false)`, `sort` 字段已新增且 `@Column(nullable=false)` ✅
4. **Post.java**: `enabled` 有 `@Column(nullable=false)` ✅
5. **PermissionFunction 重命名**: 所有引用（MenuServiceImpl、UserFacadeImpl、JwtAuthenticationFilter、UserConverter）均已更新为新的类名 ✅
6. **UserRepository**: `findByUsername` 返回 `Optional<User>` ✅；`findTokenVersionById` 已实现 ✅；`findWithDetailsById` 已有 `@EntityGraph` ✅
7. **Schema.sql sys_user**: `enabled NOT NULL DEFAULT 1` ✅；`password_change_required NOT NULL DEFAULT 0` ✅；`token_version NOT NULL DEFAULT 0` ✅
8. **Schema.sql sys_role**: `enabled NOT NULL DEFAULT 1` ✅；`sort INT NOT NULL DEFAULT 0` ✅
9. **Schema.sql sys_post**: `enabled NOT NULL DEFAULT 1` ✅
10. **Schema.sql sys_function**: `visible NOT NULL DEFAULT 1` ✅；`enabled NOT NULL DEFAULT 1` ✅
11. **Data.sql**: 三个种子用户的 `password_change_required` 均为 `1` ✅
12. **Data.sql**: 密码均为 BCrypt 编码 `$2a$10$` 格式 ✅
13. **application.yml**: profiles 为 `phase1,dev`，已移除 `phase0` ✅
14. **旧代码清理**: application 模块下的 `JwtAuthenticationFilter.java` 和 `SecurityConfigPhase1.java` 已删除 ✅
15. **MenuCreateRequest**: record 格式，字段匹配设计文档 5.2 节 ✅
16. **MenuUpdateRequest**: POJO 格式，`@JsonInclude(NON_NULL)` 支持 PATCH 语义 ✅
17. **MenuResponse**: record 格式，`withChildren` 方法支持递归树构建 ✅
18. **MenuService 接口**: 完整覆盖 getUserMenuTree / getAllMenus / createMenu / updateMenu / deleteMenu / getMenuById ✅
19. **依赖方向**: MenuServiceImpl 直接使用同模块 UserRepository/PermissionFunctionRepository，符合 7.1 节"同模块内部依赖，不构成跨模块耦合"的设计约定

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 6 |
| 轻微 | 3 |

### 总评

模块四（实体/仓库/Schema）质量良好：User 实体已正确添加 `passwordChangeRequired` 和 `tokenVersion` 字段；Role/Post 的 `enabled` NOT NULL 注解已补全；`PermissionFunction` 重命名及所有引用更新已到位；Schema.sql 对齐 4.3 节全部 DDL 变更；种子数据 `password_change_required = 1`。模块五（菜单模块）存在若干实现与设计文档不一致的问题，主要集中在：(1) 更新端点使用 PUT 而非 PATCH，(2) 删除菜单错误码使用 PARAM_INVALID 而非 CHILDREN_EXIST，(3) 路径 id 与请求体 id 缺少一致性校验，(4) PermissionFunction 缺少 `component` 字段映射导致响应中 component 恒为 null，(5) 未使用 `@EntityGraph` 存在 N+1 风险。建议优先修复 PATCH 方法和 CHILDREN_EXIST 错误码两个接口契约问题，其次补全 component 字段映射和 EntityGraph 优化。
