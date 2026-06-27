# 设计审查报告 v3 r1

## 审查结果
REJECTED

## 发现的缺陷

### [严重] 遗漏同包文件 Post.java
`com.aimedical.modules.commonmodule.permission` 包中的 `Post.java` 直接使用 `Function` 类型（第40行 `Set<Function> functions`、第93行 `getFunctions()`、第97行 `setFunctions()`），无需 import 语句。`Function` 重命名后 `Post.java` 将编译失败。设计文件规划表未列出此文件。

### [严重] 遗漏同包测试文件 PostTest.java
`com.aimedical.modules.commonmodule.permission` 包中的 `PostTest.java` 直接引用 `Function`（第67行 `Set<Function> functions = new HashSet<>()`、第68行 `new Function()`）。重命名后将编译失败。设计文件规划表未列出此文件。

### [一般] JwtUtil.init() 中 Base64 字符校验正则问题
正则 `^[A-Za-z0-9\\-_]+$` 与解码器 `Base64.getDecoder()` 不匹配。正则允许 URL-safe Base64 字符（`-`、`_`），但 `Base64.getDecoder()` 只接受标准 Base64 字符（`+`、`/`）。应统一。

### [轻微] PermissionFunctionRepository.java 冗余条目
文件规划表第18行标注"保留引用更新"，实际与第17行重命名后为同一文件，属冗余条目。

### [轻微] javax.crypto.SecretKey 标注问题
标注为"新增 import"，但在现有 `JwtUtil.java` 中已存在该 import。

## 修改要求

1. **Post.java 和 PostTest.java**：将这两个文件加入文件规划表，将其中所有 `Function` 类型引用更新为 `PermissionFunction`。两个文件与 `Function` 同包，无需修改 import 语句，但需修改类型声明和变量声明。
2. **JwtUtil.init() 字符校验**：将正则 `^[A-Za-z0-9\\-_]+$` 修改为 `^[A-Za-z0-9+/]+=*$`（若保持标准 Base64），或改为 `Base64.getUrlDecoder()`（若保持 URL-safe 语义），确保校验与解码一致。
