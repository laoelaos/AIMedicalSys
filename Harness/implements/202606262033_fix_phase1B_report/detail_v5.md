# 详细设计（v5）

## 概述

修复三项 error code 标准化缺陷：GlobalErrorCode.java 中 UNAUTHORIZED 和 FORBIDDEN 的消息文本对齐 OOD 规范，MenuServiceImpl.deleteMenu() 的错误码从 PARAM_INVALID 替换为 CHILDREN_EXIST。涉及 2 个源文件 + 1 个测试文件修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common/.../exception/GlobalErrorCode.java` | 修改 | UNAUTHORIZED 消息 `"未认证"` → `"未认证或令牌已失效"`；FORBIDDEN 消息 `"无权限"` → `"无权限访问"` |
| `common/.../exception/GlobalErrorCodeTest.java` | 修改 | 同步更新 UNAUTHORIZED 和 FORBIDDEN 消息断言预期值 |
| `common-module-impl/.../service/impl/MenuServiceImpl.java` | 修改 | deleteMenu() 中 `GlobalErrorCode.PARAM_INVALID` → `GlobalErrorCode.CHILDREN_EXIST`；异常消息文本随枚举 getMessage() 变化 |

未变更文件及理由：
- `SecurityConfigPhase1Test.java` — 不引用 UNAUTHORIZED/FORBIDDEN 消息文本
- `AuthControllerTest.java` — 使用 `GlobalErrorCode.UNAUTHORIZED` 作为错误码枚举而非断言消息文本
- `MenuServiceTest.java` — deleteMenu 测试仅断言 `BusinessException.class` 未检查具体 errorCode，无需修改
- `GlobalExceptionHandler.java` — 引用 UNAUTHORIZED/FORBIDDEN 的 `getCode()` 而非消息文本，不受影响

## 类型定义

### 1. GlobalErrorCode

**形态**：enum（修改）
**包路径**：`com.aimedical.common.exception`

**修改点**：

| 枚举常量 | 原消息 | 新消息 |
|---------|--------|--------|
| `UNAUTHORIZED("UNAUTHORIZED", ...)` | `"未认证"` | `"未认证或令牌已失效"` |
| `FORBIDDEN("FORBIDDEN", ...)` | `"无权限"` | `"无权限访问"` |

```java
// L9 原
UNAUTHORIZED("UNAUTHORIZED", "未认证"),
// L9 修改后
UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌已失效"),

// L10 原
FORBIDDEN("FORBIDDEN", "无权限"),
// L10 修改后
FORBIDDEN("FORBIDDEN", "无权限访问"),
```

**受影响的公开接口**：无新增/删除接口。`getCode()` 和 `getMessage()` 签名不变，仅 `getMessage()` 返回值变化。

### 2. MenuServiceImpl

**形态**：class（修改）
**包路径**：`com.aimedical.modules.commonmodule.service.impl`

**修改点**：

```java
// L162-166 原
@Override
public void deleteMenu(Long id) {
    List<PermissionFunction> children = functionRepository.findByParentId(id);
    if (children != null && !children.isEmpty()) {
        throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "存在子菜单，无法删除，请先删除子菜单");
    }

// 改为
@Override
public void deleteMenu(Long id) {
    List<PermissionFunction> children = functionRepository.findByParentId(id);
    if (children != null && !children.isEmpty()) {
        throw new BusinessException(GlobalErrorCode.CHILDREN_EXIST, "存在子菜单，无法删除，请先删除子菜单");
    }
```

参数说明：
- 枚举从 `PARAM_INVALID` 替换为 `CHILDREN_EXIST`（已在 GlobalErrorCode 第23行定义，code=`"CHILDREN_EXIST"`, message=`"存在子菜单，无法删除"`）
- `BusinessException(ErrorCode, Object...)` 的第二个参数（即 `args`，此处传入 `"存在子菜单，无法删除，请先删除子菜单"`）仅在异常对象中存储。全局异常处理器 `handleBusinessException` 调用 `Result.fail(errorCode)`，即 `errorCode.getCode()` 和 `errorCode.getMessage()` 构造响应体，**args 参数不会被 handler 用于响应体**。因此实际响应 message 由枚举 `getMessage()` 决定：
  - 修改前：`errorCode = PARAM_INVALID` → 响应 code=`"PARAM_INVALID"`, message=`"参数校验失败"`
  - 修改后：`errorCode = CHILDREN_EXIST` → 响应 code=`"CHILDREN_EXIST"`, message=`"存在子菜单，无法删除"`
- 响应 message 从 `"参数校验失败"` 变为 `"存在子菜单，无法删除"`，此变化符合 OOD 10.1 节规范，修改方向正确可接受。第二个参数字符串 `"存在子菜单，无法删除，请先删除子菜单"` 保留不影响响应体，可通过 `BusinessException.getArgs()` 为日志记录等场景提供额外调试信息。

## 错误处理

无新增错误类型。BusinessException 抛出机制不变，仅错误码枚举值变化：
- `CHILDREN_EXIST` 已在 GlobalErrorCode 中定义，无需新增
- 全局异常处理器 `GlobalExceptionHandler` 通过 `ex.getErrorCode()` 的 code 字段判断 HTTP 状态码（比对的是 `getCode()` 返回的字符串 key），不受消息文本变更影响

## 行为契约

### GlobalErrorCode.getMessage() 返回值变更
- **UNAUTHORIZED**：`"未认证"` → `"未认证或令牌已失效"`
- **FORBIDDEN**：`"无权限"` → `"无权限访问"`
- 其他枚举常量的 getMessage() 返回值不变
- `getCode()` 返回值全部不变

### deleteMenu() 错误码语义变更
- **前置条件**：不变（菜单有子菜单时抛出异常）
- **后置条件**：异常中 `getErrorCode()` 返回值从 `PARAM_INVALID` 变为 `CHILDREN_EXIST`
- **HTTP 响应**：全局异常处理器 `resolveHttpStatus(ErrorCode errorCode)` 中：
  - `PARAM_INVALID.getCode()` → 400（`"PARAM_INVALID"`）
  - `CHILDREN_EXIST.getCode()` → 400（`"CHILDREN_EXIST"` 未被 handler 特殊匹配，走 `SYSTEM_ERROR`/default? 需确认 handler 逻辑）
  
  > 检查 `GlobalExceptionHandler.resolveHttpStatus`：仅对 `UNAUTHORIZED`、`FORBIDDEN`、`NOT_FOUND`、`PARAM_INVALID`、`SYSTEM_ERROR` 做特殊匹配，其余走 `400` 默认分支。`CHILDREN_EXIST.getCode()` 为 `"CHILDREN_EXIST"`，不匹配上述任意条件，落入 `default -> 400`。因此 HTTP 状态码同为 400，与替换前一致，**无行为差异**。

## 依赖关系

### 修改涉及的类型依赖

| 类型 | 依赖方 | 变更类型 |
|------|--------|---------|
| `GlobalErrorCode` | 全局：所有引用该枚举的应用代码 | 消息文本变更（仅影响 `getMessage()` 返回值） |
| `GlobalErrorCode.CHILDREN_EXIST` | `MenuServiceImpl.deleteMenu()` | 新增引用（原引用 `PARAM_INVALID`） |
| `GlobalErrorCode.PARAM_INVALID` | `MenuServiceImpl.deleteMenu()` | 删除引用（同一方法内其他位置保持 `PARAM_INVALID` 不变） |

### 测试文件同步依赖

| 测试文件 | 需同步原因 | 修改内容 |
|---------|-----------|---------|
| `GlobalErrorCodeTest.java` L61 | 断言了 UNAUTHORIZED.getMessage() 的旧值 | `"未认证"` → `"未认证或令牌已失效"` |
| `GlobalErrorCodeTest.java` L67 | 断言了 FORBIDDEN.getMessage() 的旧值 | `"无权限"` → `"无权限访问"` |

### 无需修改的文件（确认清单）

| 文件 | 查阅结论 |
|------|---------|
| `GlobalExceptionHandler.java` | 通过 `getCode()` 比对，不受 message 变更影响 |
| `SecurityConfigPhase1Test.java` | 不引用 UNAUTHORIZED/FORBIDDEN 消息 |
| `AuthControllerTest.java` | 仅引用 `GlobalErrorCode.UNAUTHORIZED` 枚举常量，不断言 message |
| `AuthServiceTest.java` | 引用 `GlobalErrorCode.UNAUTHORIZED` 枚举常量，不断言 message |
| `MenuServiceTest.java` | `shouldThrowBusinessExceptionWhenHasChildren` 仅断言异常类型 |
| `MenuControllerTest.java` | 若通过 controller 层断言，关注 HTTP 状态码而非 message 文本，不受影响 |

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 文件规划表中 MenuServiceImpl.java 行"移除冗余消息参数"与详细代码变更（保留消息参数）矛盾 | 将文件规划表描述从"移除冗余消息参数"修正为"保留消息参数保持 API 响应不变"，与详细设计第 74-76 行表述一致 |

## 修订说明（v5 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| 第74-76行对 BusinessException 消息参数运行时行为分析有事实性错误：声称 args 参数会"覆盖枚举 message 作为返回体的 message 字段"，实际 handler 调用 `Result.fail(errorCode)` 使用 `errorCode.getMessage()` 构造响应，args 从不参与响应体 | 修正分析：澄清 args 仅在异常对象中存储，handler 通过 `Result.fail(errorCode)` 使用枚举 `getMessage()` 构造响应体；响应 message 从 `"参数校验失败"` 变为 `"存在子菜单，无法删除"`（符合 OOD，可接受）。同步更新文件规划表格描述 |
| 第99行引用方法名 `GlobalExceptionHandler.getHttpStatus`，实际方法名为 `resolveHttpStatus`（GlobalExceptionHandler.java:38） | 将 `getHttpStatus(String code)` 修正为 `resolveHttpStatus(ErrorCode errorCode)`，将块引用中 `GlobalExceptionHandler.getHttpStatus` 修正为 `GlobalExceptionHandler.resolveHttpStatus` |
