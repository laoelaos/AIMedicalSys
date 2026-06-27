# 实现报告（v9）

## 概述

实现 Phase1B 报告的 3 个 P2 问题修复（T12、T14、T16），修改 4 个源文件和 2 个测试文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java` | T12：正则改为 URL-safe 字符集，解码器换为 getUrlDecoder |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java` | T12：测试数据改为 URL-safe 格式，移除 padding `=` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java` | T14：generateToken 移除 role/position claims，添加 jti claim，添加 @Deprecated |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtUtilTest.java` | T14：移除 shouldGenerateTokenWithPosition，更新两个断言 |
| 修改 | `AIMedical/backend/common/src/main/java/com/aimedical/common/util/SimpleMessageInterpolator.java` | T16：增加数字占位符预检，跳过不必要的 MessageFormat |

## 编译验证

- `mvn compile -pl modules/common-module/common-module-impl,common -am` — 通过
- `mvn test -pl common -Dtest=SimpleMessageInterpolatorTest` — 6/6 通过
- `mvn test -pl modules/common-module/common-module-impl -am -Dtest=JwtUtilTest,JwtTokenProviderTest` — 42/42 通过

## 设计偏差说明

| 设计规格 | 偏差原因 | 实际处理 |
|---------|---------|---------|
| T12 测试数据直接用 `"test-secret!!!"` | `=` padding 字符不被新 URL-safe 正则允许 | `TEST_SECRET` 及 `shouldInitSuccessfullyWithValidSecret`、`shouldThrowWhenDecodedKeyTooShort` 中的测试数据均移除末尾 `=`，符合 `getUrlDecoder()` 无 padding 解码能力 |
