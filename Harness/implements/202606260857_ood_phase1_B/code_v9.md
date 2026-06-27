# 实现报告（v9）

## 概述

实现了 `JwtAuthenticationFilter` 迁移重构至 `common-module-impl` 的 `auth/security/` 包，新增 TokenBlacklist 校验、DB 用户状态验证、type claim 校验、passwordChangeRequired 传递、完整权限装配（roles+posts→functions）。为 `UserRepository` 补充 `@EntityGraph` 方法。编写 9 个单元测试并全部通过。删除旧 Filter 文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java` | 实现 JwtAuthenticationFilter，继承 OncePerRequestFilter，包级私有构造器 |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` | 新增 `@EntityGraph(attributePaths = {"roles", "posts"}) Optional<User> findWithDetailsById(Long id)` |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilterTest.java` | 9 个单元测试，无 Spring 上下文，全部通过 |
| 删除 | `application/src/main/java/com/aimedical/config/JwtAuthenticationFilter.java` | 旧 Filter 迁移移除 |

## 编译验证

编译通过。9 个新测试全部通过（`mvn test -pl modules/common-module/common-module-impl -Dtest="com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest"`）。

已有 `UserRepositoryTest` 存在 H2 DDL 表不存在的预置失败（与本次变更无关）。

## 设计偏差说明

无偏差。严格按 v9 详细设计编码，包括：
- `JwtAuthenticationFilter` 完整 doFilterInternal 11 步流程
- 私有辅助方法：extractToken、extractJti、collectAuthorities、throwAccountDisabled、extractUserId
- 包级私有构造器，无 @Component
- 9 个测试用例对应设计清单
