# 详细设计（v3）

## 概述

本次为 RETRY 任务，无需修改任何源码文件。v2 验证失败的唯一原因是 Maven 增量编译未检测到 JwtConfig.java 的变更，导致测试运行时使用了旧版字节码。本轮的修复方案单纯为：**强制 clean 编译后重跑测试**。

## 文件规划

无文件变更。已有文件内容确认正确：

| 文件路径 | 当前状态 |
|---------|---------|
| `common-module-impl/.../jwt/JwtConfig.java` | ✅ `validate()` 含 `Base64.getDecoder().decode()` + `keyBytes.length < 32` 字节检查 |
| `common-module-impl/.../auth/jwt/JwtTokenProvider.java` | ✅ `init()` 含 null/空/字符集/decode异常/字节长度/密钥构建 |
| `integration/src/test/resources/application-test.yml` | ✅ secret 为合法 Base64 |
| `common-module-impl/.../jwt/JwtConfigTest.java` | ✅ 测试适配解码后字节长度检查 |
| `common-module-impl/.../auth/jwt/JwtTokenProviderTest.java` | ✅ 新增 5 个启动验证测试 |

## 操作步骤（替代传统设计）

### 步骤 1：强制重编译

```bash
mvn clean compile -pl :common-module-impl -am
```

- `clean` — 删除 `target/` 目录，彻底清除旧 .class 字节码
- `-pl :common-module-impl` — 定位到 common-module-impl 模块
- `-am`（also-make）— 同时编译依赖模块（common、common-module-api），确保 classpath 一致
- **预期输出**：compile 阶段输出 `Compiling X source files`（非 "Nothing to compile"）

### 步骤 2：运行单元测试

```bash
mvn test -pl :common-module-impl
```

- **预期输出**：342 tests, 0 failures, 0 errors
- 关键验证点：`JwtConfigTest$ValidateTests.shouldThrowWhenDecodedKeyTooShort` 应通过
- 该测试使用 secret `"dGVzdA=="`（"test" 的 Base64，解码后 4 字节），期望 `IllegalStateException` 消息包含中文 "至少32字节"

### 步骤 3（可选）：单测聚焦验证

```bash
mvn test -pl :common-module-impl -Dtest=JwtConfigTest
```

- 验证 JwtConfigTest 全部 12 个测试通过

### 步骤 4（可选）：集成测试

```bash
mvn test -pl integration -am
```

- 验证 ApplicationContextIT 和 EntityMappingIT 通过

## 失败预案

| 场景 | 原因 | 对策 |
|------|------|------|
| `clean compile` 仍显示 "Nothing to compile" | `-pl` 未正确匹配 artifactId | 检查 `common-module-impl` 是否与 pom.xml 中 `<artifactId>` 一致，改用 `-pl modules/common-module/common-module-impl` |
| 测试仍失败 | JwtConfig.class 未更新 | `git status` 检查 JwtConfig.java 是否有未提交改动；确认 `validate()` 方法包含 Base64 decode + 字节长度检查 |
| 集成测试因 ApplicationContext 启动失败 | secret 配置问题 | 确认 `application-test.yml` 中 secret 值为合法 Base64 |

## 行为契约（同 v2，状态确认）

### JwtConfig.validate() 调用顺序

1. `!StringUtils.hasText(secret)` → `IllegalStateException`（不变）
2. `Base64.getDecoder().decode(secret)` 抛出 `IllegalArgumentException` → 捕获并包装为 `IllegalStateException`
3. `keyBytes.length < 32` → `IllegalStateException`（消息含中文"至少32字节"）
4. 通过 → 静默返回

### JwtTokenProvider.init() 调用顺序

1. `secret == null || secret.isEmpty()` → `IllegalStateException`
2. 正则不匹配 → `IllegalStateException`
3. `Base64.getDecoder().decode(secret)` 异常 → 捕获并包装为 `IllegalStateException`
4. `keyBytes.length < 32` → `IllegalStateException`
5. 通过 → `this.secretKey = Keys.hmacShaKeyFor(keyBytes)`

## v2→v3 差异

| 方面 | v2 | v3 |
|------|----|----|
| 代码变更 | 修改 5 个文件 | 无变更，源码已正确 |
| 核心问题 | 编译+测试 | 仅编译（clean 重编译） |
| 验证步骤 | 增量编译测试 | 先 clean 再测试 |
| 设计形态 | 标准类型定义设计 | 操作步骤设计（无需改代码） |

## 修订说明（v3 r0）

本为 RETRY 轮次，设计目标从"编码实现"转为"构建操作指导"。所有源码已在磁盘上正确，仅需通过 `mvn clean compile` 强制重新编译确保新字节码生效。
