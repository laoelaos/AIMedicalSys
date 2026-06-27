# 实现报告（v18）

## 概述
完成 OOD Phase 1 B 四项收尾工作的代码实现：清理 application.yml 中 phase0 profile、更新 JWT 配置格式、拆分 JwtConfig.expiration 为 accessTokenExpiration + refreshTokenExpiration、适配 JwtUtil 调用点、在 EntityMappingIT 中新增两个 User 字段映射测试。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `application/src/main/resources/application.yml` | 移除 phase0 profile；`jwt.expiration` 替换为 `jwt.access-token-expiration` 和 `jwt.refresh-token-expiration` |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtConfig.java` | 移除 `expiration` 字段及 getter/setter；新增 `accessTokenExpiration`(900L) 和 `refreshTokenExpiration`(604800L) 字段及 getter/setter |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java` | `generateToken()` 和 `getExpirationTime()` 中调用 `getExpiration()` 改为 `getAccessTokenExpiration()` |
| 修改 | `integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 在 User 区域新增 `user_shouldMapPasswordChangeRequired()` 和 `user_shouldMapTokenVersion()` 测试方法 |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtUtilTest.java` | 全部 5 处 `setExpiration(86400L)` 替换为 `setAccessTokenExpiration(900L)`；`getExpiration` 测试中断言值从 `86400L` 更新为 `900L` |

## 编译验证
`mvn compile -pl modules/common-module/common-module-impl -am` 和 `mvn compile test-compile -pl integration -am` 均静默通过，无编译错误。

## 设计偏差说明
无偏差。
