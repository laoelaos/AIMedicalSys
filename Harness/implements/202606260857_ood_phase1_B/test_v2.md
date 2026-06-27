# 测试报告（v2）

## 概述

完成 Phase 1 包 B 的单元测试编写与适配。覆盖所有 DTO record 对象、GlobalErrorCode 扩展、Service 层和 Controller 层的单元测试。

## 测试文件清单

### 新增 DTO 测试（10 个文件，51 个测试用例）

| 测试文件 | 测试数 | 说明 |
|---------|-------|------|
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/LoginRequestTest.java` | 5 | record 构造 + 校验（@NotBlank, @Size） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/ProfileUpdateRequestTest.java` | 9 | 构造 + 校验（nickname, phone 正则, email） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/MenuCreateRequestTest.java` | 6 | 构造 + 校验（@NotBlank, @NotNull visible） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/MenuUpdateRequestTest.java` | 4 | getter/setter + JSON 序列化 @JsonInclude(NON_NULL) |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/RefreshTokenRequestTest.java` | 3 | 构造 + 校验 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequestTest.java` | 7 | 构造 + 校验（oldPassword/newPassword 约束） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/response/LoginResponseTest.java` | 4 | record 构造 + 原始类型（long/boolean） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponseTest.java` | 5 | record 构造 + Set\<String\> permissions |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/response/TokenRefreshResponseTest.java` | 3 | record 构造 + null refreshToken |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/response/MenuResponseTest.java` | 5 | record 构造 + withChildren 不可变性 |

### 更新适配测试（5 个文件，68 个测试用例）

| 测试文件 | 测试数 | 适配内容 |
|---------|-------|---------|
| `common/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java` | 21 | 新增 14 个枚举值 → 验证全部 20 个常量的 code/message |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 13 | record 构造器 + accessor 适配；新增 updateProfile 测试；refreshToken 返回 TokenRefreshResponse |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | 17 | MenuCreateRequest record 构造器 + MenuResponse accessor 适配 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 11 | 适配 refresh 端点返回 TokenRefreshResponse + RefreshTokenRequest + PasswordChangeRequest |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/MenuControllerTest.java` | 6 | MenuResponse record accessor 适配 |

## 测试运行结果

### 模块：common

```
Tests run: 121, Failures: 0, Errors: 0, Skipped: 5
  GlobalErrorCodeTest ....................... 21 tests, 0 failures
```

### 模块：common-module-impl（新增/更新测试）

```
DTO Request Tests (34 tests):
  LoginRequestTest .........................  5 tests, 0 failures
  ProfileUpdateRequestTest .................  9 tests, 0 failures
  MenuCreateRequestTest ....................  6 tests, 0 failures
  MenuUpdateRequestTest ....................  4 tests, 0 failures
  RefreshTokenRequestTest ..................  3 tests, 0 failures
  PasswordChangeRequestTest ................  7 tests, 0 failures

DTO Response Tests (17 tests):
  LoginResponseTest ........................  4 tests, 0 failures
  UserInfoResponseTest .....................  5 tests, 0 failures
  TokenRefreshResponseTest .................  3 tests, 0 failures
  MenuResponseTest .........................  5 tests, 0 failures

Service Tests (30 tests):
  AuthServiceTest.LoginTests ...............  4 tests, 0 failures
  AuthServiceTest.LogoutTests ..............  1 test,  0 failures
  AuthServiceTest.RefreshTokenTests ........  3 tests, 0 failures
  AuthServiceTest.GetCurrentUserTests ......  3 tests, 0 failures
  AuthServiceTest.UpdateProfileTests .......  2 tests, 0 failures
  MenuServiceTest.GetUserMenuTreeTests .....  3 tests, 0 failures
  MenuServiceTest.GetAllMenusTests .........  3 tests, 0 failures
  MenuServiceTest.CreateMenuTests ..........  3 tests, 0 failures
  MenuServiceTest.UpdateMenuTests ..........  3 tests, 0 failures
  MenuServiceTest.DeleteMenuTests ..........  3 tests, 0 failures
  MenuServiceTest.GetMenuByIdTests .........  2 tests, 0 failures

Controller Tests (17 tests):
  AuthControllerTest.LoginTests ............  2 tests, 0 failures
  AuthControllerTest.LogoutTests ...........  2 tests, 0 failures
  AuthControllerTest.RefreshTests ..........  2 tests, 0 failures
  AuthControllerTest.MeTests ...............  2 tests, 0 failures
  AuthControllerTest.UpdateMeTests .........  2 tests, 0 failures
  AuthControllerTest.ChangePasswordTests ...  1 test,  0 failures
  MenuControllerTest.GetMenuTests ..........  2 tests, 0 failures
  MenuControllerTest.CreateMenuTests .......  1 test,  0 failures
  MenuControllerTest.UpdateMenuTests .......  2 tests, 0 failures
  MenuControllerTest.DeleteMenuTests .......  1 test,  0 failures
```

### 已知遗留问题

`UserRepositoryTest` 中的 5 个失败和错误为**预存问题**，非本次变更引入：
- 原因：H2 测试数据库未找到 `SYS_USER` 表（`Table "SYS_USER" not found`）— 该测试继承自 Phase 0，与本次 DTO record 改造无关。

## 测试覆盖维度

| 维度 | 覆盖情况 |
|------|---------|
| **正常路径** | 每个 record 的构造 + accessor + 合法参数校验 |
| **边界条件** | 字符串最大长度（password 64、nickname 50、email 100 等）|
| **错误路径** | @NotBlank/@NotNull 校验失败、@Pattern 格式无效、密码过短/过长 |
| **不可变性** | MenuResponse.withChildren 返回新实例；原实例不受影响 |
| **类型变化** | LoginResponse.expiresIn 为原始 long；passwordChangeRequired 为原始 boolean |
| **JSON 序列化** | MenuUpdateRequest 的 `@JsonInclude(NON_NULL)` 排除 null 字段 |
| **状态交互** | LoginResponse.user 始终非 null；UserInfoResponse.permissions 为 Set 类型 |

## 构建命令

```bash
mvn test -pl common,modules/common-module/common-module-impl -am
```
