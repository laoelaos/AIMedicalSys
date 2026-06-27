# 任务指令（v3）

## 动作
NEW

## 任务描述
完成 Phase 1 包 B OOD 第 12 节 Stage 1 剩余 P0 任务（1.4/1.19、1.16、1.17、1.18）。共 4 个子项：

### 子项 A：Function→PermissionFunction 重命名（M8）
- `permission/Function.java` → `PermissionFunction.java`（类名 + 文件名）
- `permission/FunctionRepository.java` → `PermissionFunctionRepository.java`
- `permission/FunctionTest.java` → `PermissionFunctionTest.java`
- 级联更新以下 5 个源文件的 import：
  - `service/impl/MenuServiceImpl.java`（import Function, FunctionRepository）
  - `service/impl/AuthServiceImpl.java`（import Function）
  - `service/AuthServiceTest.java`（import Function）
  - `service/MenuServiceTest.java`（import Function, FunctionRepository）
  - `integration/EntityMappingIT.java`（import Function）
- `Function` 在 MenuServiceImpl/AuthServiceImpl 等文件中的本地变量引用不变（仅 import 路径和类名变更）
- 关联表名 `sys_function` 不变，仅 Java 类名变更

### 子项 B：JwtUtil SecretKey 缓存（H2 + OOD 4.7）
在 `jwt/JwtUtil.java` 中：
- 新增 `private SecretKey secretKey;` 字段
- 新增 `@PostConstruct` 初始化方法，执行：
  1. `jwtConfig.getSecret()` 为 null 或空 → 抛出 `IllegalStateException("JWT_SECRET must be configured")`
  2. Base64 解码后字节长度 ≥ 32 → 不满足抛出 `IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding")`
  3. 仅含 Base64 URL-safe 字符 → 不满足抛出 `IllegalStateException("JWT_SECRET contains invalid characters")`
  4. 缓存 `Keys.hmacShaKeyFor(...)` 到 `secretKey`
- `generateToken()` 中 `SecretKey key = Keys.hmacShaKeyFor(...)` → 改为 `this.secretKey`
- `parseToken()` 中 `SecretKey key = Keys.hmacShaKeyFor(...)` → 改为 `this.secretKey`

### 子项 C：schema.sql DDL 变更
`application/src/main/resources/db/schema.sql` 中：

**sys_user**（对齐 User.java 新字段）：
- `nickname`：`DEFAULT NULL` → `NOT NULL`
- `enabled`：`DEFAULT 1` → `NOT NULL DEFAULT 1`
- 新增 `password_change_required`：`TINYINT(1) NOT NULL DEFAULT 0 COMMENT '需要修改密码'`（在 email 之后）
- 新增 `token_version`：`INT NOT NULL DEFAULT 0 COMMENT '令牌版本号'`（在 remark 之前）

**sys_role**（对齐 Role.java 新增/修复）：
- `enabled`：`DEFAULT 1` → `NOT NULL DEFAULT 1`
- 新增 `sort`：`INT NOT NULL DEFAULT 0 COMMENT '排序'`（在 remark 之前）

**sys_post**（对齐 Post.java 修复）：
- `enabled`：`DEFAULT 1` → `NOT NULL DEFAULT 1`

**sys_function**（对齐 Function→PermissionFunction 修复）：
- `visible`：`DEFAULT 1` → `NOT NULL DEFAULT 1`
- `enabled`：`DEFAULT 1` → `NOT NULL DEFAULT 1`

### 子项 D：种子数据 data.sql
在 `application/src/main/resources/db/` 下新建或更新 `data.sql`：
- 种子用户密码使用满足新复杂度规则的密码（如 `Admin@123`、`Doctor@123`、`Patient@123`），经 BCrypt 加密
- 统一设置 `password_change_required = 1`
- 若已有种子数据 INSERT 在 schema.sql 中，可追加或单独成文件

## 选择理由
所有 4 项均为 Stage 1（P0）剩余任务，完成后方可进入 Stage 2（P1 安全 Filter）：
- **1.4/1.19**（PermissionFunction 重命名）：影响 7 个文件的 import/引用，先完成避免后续新文件继续引用旧类名。与 R2 的 DTO 改造无冲突。
- **1.16**（JwtUtil 增强）：JwtTokenProvider（Stage 3.9）的前置依赖，添加 `@PostConstruct` 启动验证和 SecretKey 缓存，简单且独立。
- **1.17/1.18**（DDL + 种子数据）：确保数据库模式与已完成的实体注解（User.passwordChangeRequired、User.tokenVersion、Role.sort、所有 enabled NOT NULL）对齐。Stage 2 的 Filter 测试依赖正确的 DDL。

## 任务上下文

### PermissionFunction 重命名要点（OOD 8.1 M8 + 5.1）
- 类名：`Function` → `PermissionFunction`，文件名同步
- Repo：`FunctionRepository` → `PermissionFunctionRepository`
- 测试：`FunctionTest` → `PermissionFunctionTest`
- DB 表名 `sys_function` 不变
- 影响文件清单（已验证）：

  | 文件 | 当前 import | 新 import |
  |------|------------|-----------|
  | `permission/Function.java` | 类自身 | 改名为 PermissionFunction |
  | `permission/FunctionRepository.java` | 类自身 | 改名为 PermissionFunctionRepository |
  | `service/impl/MenuServiceImpl.java:8` | `import com.aimedical.modules.commonmodule.permission.Function` | `PermissionFunction` |
  | `service/impl/MenuServiceImpl.java:9` | `import com.aimedical.modules.commonmodule.permission.FunctionRepository` | `PermissionFunctionRepository` |
  | `service/impl/AuthServiceImpl.java:12` | `import com.aimedical.modules.commonmodule.permission.Function` | `PermissionFunction` |
  | `service/AuthServiceTest.java:12` | `import com.aimedical.modules.commonmodule.permission.Function` | `PermissionFunction` |
  | `service/MenuServiceTest.java:7-8` | `import ...permission.Function; import ...permission.FunctionRepository` | `PermissionFunction`, `PermissionFunctionRepository` |
  | `integration/EntityMappingIT.java:8` | `import com.aimedical.modules.commonmodule.permission.Function` | `PermissionFunction` |
  | `permission/FunctionTest.java` | 测试类自身 | `PermissionFunctionTest` |

### JwtUtil SecretKey 缓存设计（OOD 4.7）
当前代码（问题）：
- `generateToken()` 每调用一次执行 `Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(...))`
- `parseToken()` 同样每次重新计算
- 无启动验证

改造目标：
```java
@Component
public class JwtUtil {
    private SecretKey secretKey;  // 缓存

    @PostConstruct
    public void init() {
        String secret = jwtConfig.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        // 检查 Base64 URL-safe
        if (!secret.matches("^[A-Za-z0-9\\-_]+$")) {
            throw new IllegalStateException("JWT_SECRET contains invalid characters");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 实际项目使用 Base64 解码后再检查长度
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }
}
```
注意：上述 base64 检查为示意；实际解码方式需与 jjwt 库的 `Keys.hmacShaKeyFor` 对输入的处理保持一致——若配置的 secret 已经是 Base64 编码的字符串，需先 decode 再检查字节长度。

### schema.sql DDL 变更细节

#### sys_user 表
```sql
CREATE TABLE `sys_user` (
  ...
  `nickname`   VARCHAR(64)   NOT NULL               COMMENT '昵称',
  ...
  `email`      VARCHAR(128)  DEFAULT NULL            COMMENT '邮箱',
  `password_change_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '需要修改密码',
  `user_type`  VARCHAR(20)   NOT NULL                COMMENT '用户类型 ADMIN/DOCTOR/PATIENT',
  `enabled`    TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `token_version` INT        NOT NULL DEFAULT 0      COMMENT '令牌版本号',
  ...
);
```

#### sys_role 表
```sql
CREATE TABLE `sys_role` (
  ...
  `enabled`     TINYINT(1)    NOT NULL DEFAULT 1     COMMENT '是否启用',
  `sort`        INT           NOT NULL DEFAULT 0     COMMENT '排序',
  ...
);
```

#### sys_post 表
```sql
CREATE TABLE `sys_post` (
  ...
  `enabled`     TINYINT(1)    NOT NULL DEFAULT 1     COMMENT '是否启用',
  ...
);
```

#### sys_function 表
```sql
CREATE TABLE `sys_function` (
  ...
  `visible`       TINYINT(1)    NOT NULL DEFAULT 1   COMMENT '是否可见',
  ...
  `enabled`       TINYINT(1)    NOT NULL DEFAULT 1   COMMENT '是否启用',
  ...
);
```

### 种子数据 data.sql
示例格式（BCrypt 加密密码需使用实际编码值）：
```sql
-- 管理员（密码: Admin@123）
UPDATE sys_user SET password = '$2a$10$...', password_change_required = 1 WHERE username = 'admin';
-- 医生（密码: Doctor@123）
UPDATE sys_user SET password = '$2a$10$...', password_change_required = 1 WHERE username = 'doctor001';
-- 患者（密码: Patient@123）
UPDATE sys_user SET password = '$2a$10$...', password_change_required = 1 WHERE username = 'patient001';
```
或采用 INSERT + ON DUPLICATE KEY UPDATE 形式。

## 已有代码上下文

### PermissionFunction 当前状态
- `Function.java` 位于 `common-module-impl/.../permission/Function.java`，159 行，手写 getter/setter
- `FunctionRepository.java` 位于同目录
- `FunctionTest.java` 位于 `common-module-impl/src/test/.../permission/`
- 7 个文件引用 `Function` import（已验证 grep）
- `MenuServiceImpl` 同时引用 `Function` 和 `FunctionRepository`

### JwtUtil 当前状态
- `JwtUtil.java` 位于 `common-module-impl/.../jwt/`，242 行
- 使用 `@Component` 注册，构造器注入 `JwtConfig`
- `generateToken()` 和 `parseToken()` 每调用一次执行 `Keys.hmacShaKeyFor(...)` 从 `jwtConfig.getSecret()` 字符串重新计算
- 无启动验证
- `extractToken(authHeader, tokenType)` 为静态方法，不依赖实例

### schema.sql 当前状态
- 位于 `application/src/main/resources/db/schema.sql`，428 行
- 关键表列状态：
  - `sys_user.nickname`: `DEFAULT NULL`（需改为 NOT NULL）
  - `sys_user.enabled`: `DEFAULT 1`（需加 NOT NULL）
  - `sys_user` 缺 `password_change_required` 和 `token_version` 列
  - `sys_role.enabled`: `DEFAULT 1`（需加 NOT NULL）
  - `sys_role` 缺 `sort` 列
  - `sys_post.enabled`: `DEFAULT 1`（需加 NOT NULL）
  - `sys_function.visible` 和 `enabled`: `DEFAULT 1`（需加 NOT NULL）

### 不在此范围
- schema.sql 中 `function_id` 列和 `fk_post_function_function` 外键名称不变（关联表名 `sys_function` 不变）
- JwtUtil 之外的其他 JWT 改造（JwtTokenProvider 属于 Stage 3.9，Phase 3 任务）
- 集成测试用例新增（阶段 4 统一处理）
- Stage 2 Filter 新建（在 Stage 1 完成后独立任务）

## 修订说明（v3 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| plan.md 遗漏 `sys_function.enabled` DDL 变更（第 37 行只列了 `visible` 未列 `enabled`，与 task_v3.md 第 49-51 行及实体注解不一致） | plan.md 第 37 行末尾追加 `sys_function.enabled` DEFAULT 1→NOT NULL DEFAULT 1 |

> 注：task_v3.md 第 49-51 行原本就已正确包含 `sys_function.enabled`，无需修改。本轮仅同步更新 plan.md。**任务内容无变化，按原 task_v3.md 执行。**
