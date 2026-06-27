# 实现报告（v2）

## 概述

完成了 Phase 1 包 B 的认证/菜单 DTO Java 17 record 改造、新 DTO 补全、GlobalErrorCode 扩展及调用方适配。涉及 9 个 DTO 文件（6 改 + 3 新）、1 个枚举扩展、4 个 service/controller 文件适配。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/LoginRequest.java` | POJO→record，字段缩减，密码约束改 1-64 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/ProfileUpdateRequest.java` | POJO→record，加 @NotBlank + 手机号正则 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuCreateRequest.java` | POJO→record，字段按 OOD 5.2 重定义 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuUpdateRequest.java` | POJO PATCH 语义，@JsonInclude(NON_NULL)，移除 MenuType 依赖 |
| 新增 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/RefreshTokenRequest.java` | 新 record |
| 新增 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequest.java` | 新 record |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/LoginResponse.java` | POJO→record，token→accessToken，新增字段 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponse.java` | POJO→record，新增 phone/email，permissions Set<String> |
| 新增 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/TokenRefreshResponse.java` | 新 record |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/MenuResponse.java` | POJO→record，字段重定义 + withChildren 方法 |
| 修改 | `common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java` | 新增 14 个枚举值 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java` | refreshToken 返回类型 LoginResponse→TokenRefreshResponse |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | DTO 构造/访问适配 + refresh 返回类型变更 + buildUserInfoResponse 适配 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | refresh 端点改请求体参数与返回类型，logout 加可选 RefreshTokenRequest，新增 password 端点 |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | MenuCreateRequest/MenuResponse 访问适配 + buildMenuTree 重构为不可变 |

## 编译验证

编译通过（`mvn compile -pl common,modules/common-module/common-module-impl -am -q`，无错误）。

## 设计偏差说明

无偏差。所有实现与 detail_v2.md 中的类型定义、方法签名和行为契约保持一致。

### 关键实现决策

- **MenuResponse.withChildren**：按设计推荐在 record 中添加实例方法，用于不可变树构建。
- **buildMenuTree 重构**：采用 `Map<Long, Long> parentIdMap` 外部映射方案，在 `getUserMenuTree`/`getAllMenus` 中从 Function 实体构建 parentId 映射后传入 `buildMenuTree`，避免 MenuResponse 暴露 parentId 字段。
- **AuthController.changePassword**：新增端点但 Phase 1 为 stub（返回 `Result.success(null)`），因 `AuthServiceImpl.changePassword()` 属于 Phase 2 任务。
- **MenuController**：无变更（符合设计判断——Controller 不直接访问 MenuResponse 字段）。
- **import 清理**：`MenuServiceImpl` 移除 `com.aimedical.common.base.MenuType` 依赖；新增 `java.util.HashMap` 用于 parentIdMap。
