# 实现报告（v3）

## 概述

修复 JwtConfig.validate() 的错误消息与测试断言不匹配的问题。错误消息中多了一个"为"字（"至少**为**32字节"），导致测试 `shouldThrowWhenDecodedKeyTooShort` 的 `contains("至少32字节")` 断言失败。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common-module-impl/.../jwt/JwtConfig.java` | 移除错误消息中多余的"为"字，使消息内容与设计规格/测试断言一致 |
| 修改 | `backend/pom.xml` | 添加 `project.build.sourceEncoding=UTF-8` 确保编译器使用 UTF-8 编码读取源文件 |

## 编译验证

```
mvn clean test -pl :common-module-impl -am
Tests run: 342, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 设计偏差说明

### 偏差 1：错误消息文字与测试断言不匹配

| 项目 | 内容 |
|------|------|
| 设计规格 | 消息含中文"至少32字节" |
| 实际代码 | 消息为"JWT密钥解码后字节长度必须至少**为**32字节（256位）…"（含多余"为"字） |
| 测试断言 | `assertTrue(ex.getMessage().contains("至少32字节"))` |
| 处理措施 | 移除错误消息中的"为"字，与设计规格和测试断言保持一致 |

### 偏差 2：POM 缺少编码配置

| 项目 | 内容 |
|------|------|
| 设计规格 | 无文件变更 |
| 实际需要 | `pom.xml` 未显式设置 `project.build.sourceEncoding`，虽非此次测试失败的根因，但有潜在编码风险 |
| 处理措施 | 添加 `project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` 确保编译器读取 UTF-8 编码的源文件 |
