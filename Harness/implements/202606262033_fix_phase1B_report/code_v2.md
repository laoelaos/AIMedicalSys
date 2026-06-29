# 实现报告（v2）

## 概述

修复 T1（JwtTokenProvider @PostConstruct 缺少启动验证）和 T10（JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度），同时修复 integration 测试 JWT secret 为非法 Base64 导致 ApplicationContextIT/EntityMappingIT 启动失败的问题。共计修改 5 个文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java` | `init()` 增加 null/空值检查、Base64 字符集检查、解码后字节长度 >= 32 检查、decode 异常包装 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtConfig.java` | `validate()` 将 `secret.length() < 32` 改为解码后字节长度检查，增加 decode 异常包装，新增 `java.util.Base64` import |
| 修改 | `AIMedical/backend/integration/src/test/resources/application-test.yml` | JWT secret 改为合法 Base64 字符串 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java` | 新增 5 个启动验证场景测试方法 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtConfigTest.java` | 替换 `shouldThrowExceptionWhenSecretLengthLessThan32` 为 `shouldThrowWhenDecodedKeyTooShort`；重命名 `shouldNotThrowExceptionWhenSecretIsValid` 为 `shouldPassWithValidLongSecret` |

## 编译验证

common-module-impl：compile 和 test-compile 均通过，无错误。

## 设计偏差说明

无偏差。所有实现严格遵循详细设计 v2 规格。
