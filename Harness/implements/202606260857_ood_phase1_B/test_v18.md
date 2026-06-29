# 测试报告（v18）

## 测试文件

| 文件 | 操作 | 测试数 | 状态 |
|------|------|--------|------|
| `modules/common-module/common-module-impl/src/test/java/.../jwt/JwtConfigTest.java` | 新建 | 11 | ✅ 全部通过 |
| `modules/common-module/common-module-impl/src/test/java/.../jwt/JwtUtilTest.java` | 已有（v18 适配） | 27 | ✅ 全部通过 |
| `integration/src/test/java/.../EntityMappingIT.java` | 已有（v18 新增 2 方法） | 24 | ❌ 上下文加载失败（**非本阶段引入的问题**） |

## 测试执行结果

### common-module-impl 单元测试

```
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
```

#### JwtConfigTest（新建，11 个用例）

**覆盖维度**：

| 维度 | 用例数 | 说明 |
|------|--------|------|
| 正常路径（默认值） | 3 | `accessTokenExpiration` 默认 900L、`refreshTokenExpiration` 默认 604800L、`tokenType` 默认 "Bearer" |
| 正常路径（getter/setter） | 4 | 各字段设置和读取成功 |
| 错误路径（validate） | 3 | secret 为 null、空字符串、长度 < 32 均抛出 `IllegalStateException` |
| 正常路径（validate） | 1 | 有效 secret 不抛出异常 |

**嵌套组织**：
- `DefaultValueTests`（3 tests）
- `GetterSetterTests`（4 tests）
- `ValidateTests`（4 tests）

#### JwtUtilTest（适配验证，27 个用例）

全部 27 个测试通过，确认 `setExpiration()` → `setAccessTokenExpiration()` 适配正确，断言值 `86400L` → `900L` 已更新。

### integration 集成测试 — EntityMappingIT

```
Tests run: 24, Failures: 0, Errors: 24, Skipped: 0
```

全部 24 个测试因 Spring ApplicationContext 加载失败而报错，**根因与本阶段变更无关**：

```
BeanDefinitionOverrideException: 
  Invalid bean definition with name 'tokenBlacklist' 
  defined in SecurityConfigPhase1.class 
  since there is already one bound in AuthModuleConfig.class
```

`SecurityConfigPhase1.tokenBlacklist()` 与 `AuthModuleConfig.tokenBlacklist()` 存在 bean 名称冲突。该问题在 v18 变更前即存在（两个配置类均定义同名 `@Bean`），非本阶段引入。

**本阶段新增的两个测试方法**（`user_shouldMapPasswordChangeRequired`、`user_shouldMapTokenVersion`）已确认存在于源文件中（`EntityMappingIT.java:389-434`），因上下文未加载而无法执行。

## 结论

- ✅ **JwtConfig** 行为契约验证通过：默认值、getter/setter、validate 校验全部正确
- ✅ **JwtUtil** 适配验证通过：`getAccessTokenExpiration()` 调用正确，返回值 900L
- ❌ **EntityMappingIT** 存在独立于本阶段的 Spring bean 冲突问题，需单独治理
- ✅ 本阶段新增/修改的代码通过单元测试验证
