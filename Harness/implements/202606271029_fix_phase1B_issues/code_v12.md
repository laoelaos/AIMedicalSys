# 实现报告（v12）

## 概述

针对 14 项测试相关待办项（T3-T34 中的 14 项）完成测试文件修改和文档修正，覆盖 T3/T4/T5/T7/T20/T24/T26/T27/T28/T29/T30/T31/T32/T33/T34，共修改 14 个文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `Harness/reviews/202606270204_fix_phase1B_code_review/review_v2_D.md` | T3: 修正审查范围中 3 条测试文件路径 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilterTest.java` | T4: 新增 principal=null / principal=非Long 测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/UserFacadeImplTest.java` | T5: UserConverter 去 mock + T28: 新增 DataAccessException 测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLoggerTest.java` | T7: 新增写入失败降级路径测试 |
| 修改 | `AIMedical/backend/common/src/test/java/com/aimedical/common/util/SimpleMessageInterpolatorTest.java` | T20: 新增 3 个插值回退路径测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounterTest.java` | T24: 并发断言改为精确 assertEquals(limit, allowed.get()) |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/password/PasswordPolicyImplTest.java` | T26: 新增全4字符集边界测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/converter/UserConverterTest.java` | T27: 新增 sort=null + enabled=false 组合测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImplTest.java` | T29: 新增 principal 非 Long 测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | T30: filter 顺序改为非反射方式 |
| 修改 | `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | T31: 新增 Role.enabled NOT NULL 约束验证 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequestTest.java` | T32: 新增 oldPassword 1 字符最小长度边界 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | T33: 重命名 shouldNotFilterDeletedInJavaLayer |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/RoleTest.java` | T34: 新增 sort NOT NULL 约束验证 |

## 编译验证

未执行编译验证。

## 设计偏差说明

无偏差。
