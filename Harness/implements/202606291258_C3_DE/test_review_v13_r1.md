# 测试审查报告（v13 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `common/src/test/java/com/aimedical/common/pom/MovedModulePomVerificationTest.java:41` — 方法 `rootPomModulesShouldContainAllExpectedEntries` 调用了 `exists(String)`（其签名声明 `throws Exception`），但该方法未声明 `throws Exception`，亦未用 try-catch 处理，导致编译错误。

- **[严重]** `common/src/test/java/com/aimedical/common/pom/ParentPomVerificationTest.java:35,41,47,53,64` — 全部 5 个测试方法调用 `exists(String)`，但均未声明 `throws Exception`，导致编译错误。

## 修改要求（仅 REJECTED 时）

### 问题 1：编译错误 — throws Exception 缺失

**文件**：`common/src/test/java/com/aimedical/common/pom/MovedModulePomVerificationTest.java`  
**位置**：第 40 行 `void rootPomModulesShouldContainAllExpectedEntries()`  
**问题**：方法体调用了 `private boolean exists(String expr) throws Exception`，但该方法未声明 `throws Exception`。Java 编译器禁止调用抛出受检异常的方法而不捕获或声明。  
**期望修方向**：在方法签名后追加 `throws Exception`，与项目中已有测试（如 `MovedModulePomTest.java:150`）保持一致。

### 问题 2：编译错误 — throws Exception 缺失

**文件**：`common/src/test/java/com/aimedical/common/pom/ParentPomVerificationTest.java`  
**位置**：第 35、41、47、53、64 行（全部 5 个测试方法）  
**问题**：每个测试方法均调用 `exists(String)`（抛出 `Exception`），但均未声明 `throws Exception`。  
**期望修方向**：在每个测试方法签名后追加 `throws Exception`，与项目中已有测试保持一致。
