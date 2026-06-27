# 详细设计（v3 r1）

## 概述

完成 Phase 1 包 B OOD 第 12 节 Stage 1 剩余 P0 任务（1.4/1.19、1.16、1.17、1.18），共 4 个子项：

- **子项 A（M8）**：`Function` → `PermissionFunction` 重命名，含类名、文件名、Repository、Test 及 5 个引用文件级联更新
- **子项 B（H2 + OOD 4.7）**：`JwtUtil` 新增 `@PostConstruct` SecretKey 缓存 + 启动验证
- **子项 C（OOD 4.3 + 5.1）**：`schema.sql` DDL 变更（NOT NULL 归一化 + 新列）
- **子项 D（OOD 4.3）**：`data.sql` 种子数据更新（新复杂度密码 + `password_change_required = 1`）

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java` | 重命名 → `PermissionFunction.java` | 类名变更 + 文件名变更 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/FunctionRepository.java` | 重命名 → `PermissionFunctionRepository.java` | 接口名变更 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` | 修改 | `Function` 类型引用 → `PermissionFunction`（同包，无需改 import） |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | 修改 | import `PermissionFunction`, `PermissionFunctionRepository` |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | import `PermissionFunction` |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java` | 修改 | 新增 `secretKey` 字段 + `@PostConstruct init()` |
| `application/src/main/resources/db/schema.sql` | 修改 | DDL 变更（4 张表） |
| `application/src/main/resources/db/data.sql` | 修改 | 种子数据密码更新 + `password_change_required` |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/FunctionTest.java` | 重命名 → `PermissionFunctionTest.java` | 测试类名变更 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/PostTest.java` | 修改 | `Function` 类型引用 → `PermissionFunction`（同包，无需改 import） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | import `PermissionFunction` |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | 修改 | import `PermissionFunction`, `PermissionFunctionRepository` |
| `integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 修改 | import `PermissionFunction` |

## 类型定义

### 子项 A：PermissionFunction 重命名

#### PermissionFunction（原名 Function）

**形态**：class（JPA Entity，extends BaseEntity）
**包路径**：`com.aimedical.modules.commonmodule.permission.PermissionFunction`
**职责**：功能权限实体，同原名 Function，仅类名变更；表名 `sys_function` 不变

**变更说明**：
- 文件 `Function.java` → `PermissionFunction.java`（物理文件名重命名）
- 类声明 `public class Function extends BaseEntity` → `public class PermissionFunction extends BaseEntity`
- 所有字段、getter/setter、JPA 注解不变
- 内联类引用 `Function`（如 `private Function parent;`）→ `PermissionFunction`

**公开接口**：同原名（全部 getter/setter 不变）：
- `getCode()` / `setCode(String)`
- `getName()` / `setName(String)`
- `getDescription()` / `setDescription(String)`
- `getEnabled()` / `setEnabled(Boolean)`
- `getParent()` / `setParent(PermissionFunction)`
- `getSortOrder()` / `setSortOrder(Integer)`
- `getVisible()` / `setVisible(Boolean)`
- `getType()` / `setType(String)`
- `getIcon()` / `setIcon(String)`
- `getPath()` / `setPath(String)`
- `getPosts()` / `setPosts(Set<Post>)`

**构造方式**：`new PermissionFunction()`（默认无参构造器）
**类型关系**：extends `BaseEntity`，同原名

#### PermissionFunctionRepository（原名 FunctionRepository）

**形态**：interface（extends `JpaRepository<PermissionFunction, Long>`）
**包路径**：`com.aimedical.modules.commonmodule.permission.PermissionFunctionRepository`
**职责**：功能权限数据访问层

**变更说明**：
- 文件 `FunctionRepository.java` → `PermissionFunctionRepository.java`
- 接口声明 `public interface FunctionRepository extends JpaRepository<Function, Long>` → `public interface PermissionFunctionRepository extends JpaRepository<PermissionFunction, Long>`

**方法签名**（不变）：
```java
Optional<PermissionFunction> findByCode(String code);
List<PermissionFunction> findByParentId(Long parentId);
List<PermissionFunction> findByVisible(Boolean visible);
boolean existsByCode(String code);
```

#### Post.java 变更（同包引用）

**形态**：class（JPA Entity，extends BaseEntity）
**包路径**：`com.aimedical.modules.commonmodule.permission.Post`
**职责**：岗位实体；同包使用 `PermissionFunction` 类型，无需修改 import 语句

**类型引用变更**：
| 位置 | 旧代码 | 新代码 |
|------|--------|--------|
| 第 40 行字段声明 | `private Set<Function> functions;` | `private Set<PermissionFunction> functions;` |
| 第 93 行 getter 返回类型 | `public Set<Function> getFunctions()` | `public Set<PermissionFunction> getFunctions()` |
| 第 97 行 setter 参数类型 | `public void setFunctions(Set<Function> functions)` | `public void setFunctions(Set<PermissionFunction> functions)` |

#### PostTest.java 变更（同包引用）

**形态**：class（JUnit 5 test）
**包路径**：`com.aimedical.modules.commonmodule.permission.PostTest`
**职责**：Post 实体测试；同包直接引用 `PermissionFunction`，无需修改 import

**类型引用变更**：
| 位置 | 旧代码 | 新代码 |
|------|--------|--------|
| 第 67 行 | `Set<Function> functions = new HashSet<>();` | `Set<PermissionFunction> functions = new HashSet<>();` |
| 第 68 行 | `functions.add(new Function());` | `functions.add(new PermissionFunction());` |

#### FunctionTest → PermissionFunctionTest 变更

**形态**：class（JUnit 5 test）
**包路径**：`com.aimedical.modules.commonmodule.permission.PermissionFunctionTest`
**职责**：PermissionFunction 实体测试

**变更说明**：
- 文件 `FunctionTest.java` → `PermissionFunctionTest.java`
- 类名 `class FunctionTest` → `class PermissionFunctionTest`
- 所有 `Function` 类型引用 → `PermissionFunction`（包括变量声明 `Function function = new Function()` → `PermissionFunction permissionFunction = new PermissionFunction()`，或保持变量名 `function` 仅改类型）
- 类内引用（如第 53 行 `Set<Post> posts = new HashSet<>()`）不涉及 `Function` 类型不变

#### 级联引用更新清单

| 文件 | 变更内容 |
|------|---------|
| `PermissionFunction.java`（类自身） | 类名 `Function` → `PermissionFunction`，内联 `private Function parent` → `PermissionFunction parent` |
| `PermissionFunctionRepository.java` | 接口名及泛型参数 `Function` → `PermissionFunction` |
| `Post.java` | 类型引用 `Set<Function>` → `Set<PermissionFunction>`（同包，无 import 变更） |
| `PostTest.java` | 类型引用 `Set<Function>` / `new Function()` → `PermissionFunction`（同包，无 import 变更） |
| `MenuServiceImpl.java` | import `Function` → `PermissionFunction`；import `FunctionRepository` → `PermissionFunctionRepository`；内联类型引用同步更新 |
| `AuthServiceImpl.java` | import `Function` → `PermissionFunction`；内联类型引用同步更新 |
| `AuthServiceTest.java` | import `Function` → `PermissionFunction` |
| `MenuServiceTest.java` | import `Function`, `FunctionRepository` → `PermissionFunction`, `PermissionFunctionRepository` |
| `EntityMappingIT.java` | import `Function` → `PermissionFunction` |
| `PermissionFunctionTest.java`（原名 FunctionTest.java） | 类名变更，`Function` 类型引用 → `PermissionFunction` |

**行为契约**：`MenuServiceImpl` 和 `AuthServiceImpl` 中的本地变量名（如 `function`）引用不变；仅 import 路径、类名和类型声明变更。

---

### 子项 B：JwtUtil SecretKey 缓存

#### JwtUtil 变更

**形态**：@Component class
**包路径**：`com.aimedical.modules.commonmodule.jwt.JwtUtil`
**职责**：JWT 令牌工具类，新增 SecretKey 缓存 + 启动验证

**新增私有字段**：
```java
private SecretKey secretKey;
```

**新增初始化方法**：
```java
@PostConstruct
public void init() {
    String secret = jwtConfig.getSecret();
    if (secret == null || secret.isEmpty()) {
        throw new IllegalStateException("JWT_SECRET must be configured");
    }
    if (!secret.matches("^[A-Za-z0-9+/]+=*$")) {
        throw new IllegalStateException("JWT_SECRET contains invalid characters");
    }
    byte[] keyBytes = Base64.getDecoder().decode(secret);
    if (keyBytes.length < 32) {
        throw new IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding");
    }
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
}
```

**新增 import**：
```java
import jakarta.annotation.PostConstruct;
import java.util.Base64;
```
说明：`javax.crypto.SecretKey` 已在现有 `JwtUtil.java` 第 17 行存在，无需新增。

**变更方法**：

`generateToken` 中：
```java
// 原：
SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
// 新：
// 使用 this.secretKey（已在 @PostConstruct 中缓存）
```

`parseToken` 中：
```java
// 原：
SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
// 新：
// 使用 this.secretKey
```

**行为契约**：
1. `@PostConstruct init()` 在 Spring 完成依赖注入后自动调用
2. secret 验证阶段顺序：null/empty → 标准 Base64 字符 → Base64 解码后字节长度
3. 任一验证不通过抛出 `IllegalStateException`，阻止应用启动
4. SecretKey 初次计算后缓存，运行期不变
5. `generateToken()` 和 `parseToken()` 不再每次重新计算 `Keys.hmacShaKeyFor`
6. `extractToken(authHeader, tokenType)` 为静态方法，不依赖实例，不变

---

### 子项 C：schema.sql DDL 变更

#### sys_user 表变更

```sql
-- nickname: DEFAULT NULL → NOT NULL
`nickname`      VARCHAR(64)   NOT NULL                COMMENT '昵称',

-- enabled: DEFAULT 1 → NOT NULL DEFAULT 1
`enabled`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',

-- 新增（email 之后，user_type 之前）：
`password_change_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '需要修改密码',

-- 新增（remark 之前）：
`token_version` INT           NOT NULL DEFAULT 0      COMMENT '令牌版本号',
```

#### sys_role 表变更

```sql
-- enabled: DEFAULT 1 → NOT NULL DEFAULT 1
`enabled`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',

-- 新增（remark 之前）：
`sort`          INT           NOT NULL DEFAULT 0      COMMENT '排序',
```

#### sys_post 表变更

```sql
-- enabled: DEFAULT 1 → NOT NULL DEFAULT 1
`enabled`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
```

#### sys_function 表变更

```sql
-- visible: DEFAULT 1 → NOT NULL DEFAULT 1
`visible`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否可见',

-- enabled: DEFAULT 1 → NOT NULL DEFAULT 1
`enabled`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
```

---

### 子项 D：data.sql 种子数据

**sys_user INSERT 变更**：

新增列 `password_change_required`、`token_version`；密码更新为满足新复杂度规则的 BCrypt 加密值：

```sql
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `user_type`, `enabled`, `password_change_required`, `token_version`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'admin',       '$2a$10$...BCRYPT_HASH_ADMIN...',    '系统管理员', '13800000001', 'admin@aimedical.com',    'ADMIN',   1, 1, 0, NOW(), NOW(), 0),
(2, 'doctor01',    '$2a$10$...BCRYPT_HASH_DOCTOR...',   '张医生',     '13800000002', 'doctor01@aimedical.com', 'DOCTOR',  1, 1, 0, NOW(), NOW(), 0),
(3, '13900000003', '$2a$10$...BCRYPT_HASH_PATIENT...',  '李先生',     '13900000003', 'patient01@aimedical.com','PATIENT', 1, 1, 0, NOW(), NOW(), 0);
```

**变更说明**：
- `password` 列使用 BCrypt 加密的满足新复杂度规则的密码
- `password_change_required` 统一设为 `1`
- `token_version` 统一设为 `0`
- 已有 INSERT 的位置和关联数据（user_role、user_post、post_function、profile 等）不变

**密码规则**（BCrypt 加密前）：
| 用户名 | 明文密码 | 说明 |
|--------|---------|------|
| admin | `Admin@123` | 大写+小写+数字+特殊字符 ≥8 位 |
| doctor01 | `Doctor@123` | 同上 |
| 13900000003 | `Patient@123` | 同上 |

## 错误处理

### JwtUtil 启动验证错误

| 验证阶段 | 触发条件 | 异常类型 | 异常消息 |
|---------|---------|---------|---------|
| 1 | `jwtConfig.getSecret()` 为 null 或空 | `IllegalStateException` | "JWT_SECRET must be configured" |
| 2 | 标准 Base64 字符检查失败（正则 `^[A-Za-z0-9+/]+=*$`） | `IllegalStateException` | "JWT_SECRET contains invalid characters" |
| 3 | Base64 解码后字节长度 < 32 | `IllegalStateException` | "JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding" |

**说明**：字符校验使用标准 Base64 正则 `^[A-Za-z0-9+/]+=*$`，与 `Base64.getDecoder()` 解码器一致。`jwtConfig.getSecret()` 中已有的 `@PostConstruct validate()` 密钥长度检查与 JwtUtil 中的 Base64 解码后字节长度检查语义不同；两处验证均通过方可启动，保留向后兼容。

**非 JwtUtil 相关错误**：子项 A/C/D 不引入新的错误处理路径。

## 行为契约

### 子项 A：PermissionFunction 重命名
- DB 表名 `sys_function` 不变，外键约束名 `fk_post_function_function` 不变
- 仅 Java 类名变更，不改变任何运行时行为
- 不改变 `Function` 与 `Post` 的 `@ManyToMany(mappedBy = "functions")` 关系（`PermissionFunction` 的 `mappedBy` 属性值不变）
- `Post.java` 和 `PostTest.java` 与 `PermissionFunction` 同包，无需 import 语句变更，仅类型声明和变量声明中的 `Function` 需改为 `PermissionFunction`

### 子项 B：JwtUtil SecretKey 缓存
- **前置条件**：`jwtConfig.getSecret()` 返回合法的标准 Base64 字符串，解码后 ≥ 32 字节
- **后置条件**：`init()` 成功后 `secretKey` 字段为缓存密钥，运行期不可变
- **不变量**：`generateToken()` 和 `parseToken()` 使用 `this.secretKey`
- **调用顺序**：`init()` 在首次 `generateToken()` / `parseToken()` 调用前由 Spring 容器自动调用

### 子项 C：schema.sql DDL
- `sys_user.nickname` 改为 `NOT NULL` 确保与 `User.java @Column(nullable=false)` 一致
- `sys_user.password_change_required` 与 `User.java` 新增字段 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")` 对齐
- `sys_user.token_version` 与 `User.java` 新增字段 `@Column(nullable=false) private Integer tokenVersion = 0;` 对齐
- `sys_role.sort` 与 `Role.java` 新增字段 `@Column(nullable=false) private Integer sort = 0;` 对齐
- `sys_role.enabled`、`sys_post.enabled`、`sys_function.visible`、`sys_function.enabled` NOT NULL 归一化，对齐各实体的 `@Column(nullable=false)`

### 子项 D：data.sql
- 种子用户密码满足 8-64 位 + 3/4 字符种类复杂度规则
- `password_change_required = 1` 强制首次登录修改密码
- 使用 `INSERT ... ON DUPLICATE KEY UPDATE` 或仅 INSERT（因 schema.sql 使用 DROP TABLE IF EXISTS + CREATE TABLE）

## 依赖关系

### 已有依赖（不变）
- `com.aimedical.modules.commonmodule.jwt.JwtConfig`：JwtUtil 构造注入
- `io.jsonwebtoken.*`：JwtUtil 的 JJWT 库依赖
- `jakarta.annotation.PostConstruct`：JwtUtil init 方法注解
- `javax.crypto.SecretKey`：JwtUtil 密钥字段类型（已在现有代码中 import）
- `java.util.Base64`：JwtUtil Base64 解码
- `org.springframework.stereotype.Component`：JwtUtil Bean 注册

### 新增依赖
无（所有新增类型均为 JDK/Jakarta/现有库标准类）

### 移除依赖
- `java.nio.charset.StandardCharsets`：JwtUtil `generateToken`/`parseToken` 中改为使用 Base64 解码后的字节数组，不再需要 `getBytes(StandardCharsets.UTF_8)`

### 暴露给后续任务的公开接口
- `PermissionFunction` entity：替换原名 `Function`，供 Phase 2 `JwtAuthenticationFilter`、`MenuServiceImpl`、Phase 4 集成测试引用
- `PermissionFunctionRepository`：同上
- `JwtUtil.secretKey` 缓存：供 Phase 3 `JwtTokenProvider` 复用（JwtTokenProvider 的 `@PostConstruct` 验证和 SecretKey 缓存逻辑与 JwtUtil 重复，Phase 3 需抽取公共 Base64SecretKeyProvider 或统一迁移到 JwtTokenProvider）
- `schema.sql` DDL 更新：供 Phase 2 H2 测试初始化使用
- `data.sql`：供 Phase 2 集成测试种子数据使用

### 不在此范围
- `JwtUtil` → `JwtTokenProvider` 重构（Phase 3 任务）
- Phase 2 的 Filter/限流/黑名单 等任务
- `MenuServiceImpl.buildMenuTree` 中 `Function` → `PermissionFunction` 类型引用在本次设计中涵盖（属于 import 级联更新）
- 集成测试新增（阶段 4 统一处理）

## 修订说明（v3 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 遗漏同包文件 Post.java — `Set<Function>` 等类型引用在 `Function` 重命名后将编译失败 | 将 Post.java 加入文件规划表；在类型定义小节新增「Post.java 变更」段落，列出 3 处 `Function` → `PermissionFunction` 类型引用变更 |
| 遗漏同包测试文件 PostTest.java — `Set<Function>` 和 `new Function()` 引用重命名后将编译失败 | 将 PostTest.java 加入文件规划表；在类型定义小节新增「PostTest.java 变更」段落，列出 2 处 `Function` → `PermissionFunction` 类型引用变更 |
| JwtUtil.init() 中 Base64 字符校验正则 `^[A-Za-z0-9\\-_]+$` 与解码器 `Base64.getDecoder()` 不匹配（正则允许 URL-safe 字符，解码器只接受标准 Base64） | 将正则修改为 `^[A-Za-z0-9+/]+=*$`，与 `Base64.getDecoder()` 的标准 Base64 字符集保持一致 |
| PermissionFunctionRepository.java 冗余条目（文件规划表第 18 行标注"保留引用更新"，与第 17 行重命名后为同一文件） | 移除文件规划表中关于 PermissionFunctionRepository.java 的冗余行 |
| javax.crypto.SecretKey 标注为"新增 import"，但现有 JwtUtil.java 中已存在该 import | 修正说明：`javax.crypto.SecretKey` 已在现有代码中 import，仅标注 `jakarta.annotation.PostConstruct` 和 `java.util.Base64` 为新增 |

DESIGN_WRITTEN:C:/Develop/Software/AIMedicalSys/Harness/implements/202606260857_ood_phase1_B/detail_v3.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
