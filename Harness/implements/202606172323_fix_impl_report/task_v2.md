# 任务指令（v2）

## 动作
RETRY

## 任务描述
POM依赖治理（T3+T4+T7）——修正方案A：完全移除父POM dependencyManagement中5个Spring Boot Starter条目、删除h2的runtime scope、删除ai-impl冗余common依赖。附加OP-04：标记ParentPomVersionTest基线测试为废弃（@Disabled）

## 选择理由
R1验证失败，原设计假设（子POM dependencyManagement继承父BOM版本）不成立。方案A彻底移除Starter条目，由spring-boot-starter-parent:3.2.5的BOM统一管理版本。

## 任务上下文
修复diagnosis报告02_impl_report.md中T3/T4/T7：
- T3：父POM `dependencyManagement` 有5个Spring Boot Starter条目显式标注`<version>3.2.5</version>`，与BOM统一管理原则冲突
- T4：父POM `dependencyManagement` 中h2条目含`<scope>runtime</scope>`，与OOD §2.2矛盾
- T7：ai-impl/pom.xml声明了common直接依赖，但源码无common引用，common已通过ai-api传递获得

## 已有代码上下文
- `AIMedical/backend/pom.xml:78-109` — dependencyManagement中h2（第78-83行，含scope） + 5个Starter条目（第84-109行，含version）
- `AIMedical/backend/modules/ai/ai-impl/pom.xml:17-20` — common依赖块

## RETRY 说明
**R1失败原因**：原设计OP-01仅删除Starter的`<version>`行（期望继承父BOM版本），但Maven中子POM的`<dependencyManagement>`条目会**覆盖**父BOM（spring-boot-starter-parent）中相同条目的版本管理。删除version后Starter条目变为无版本状态，导致10个引用这些starter的子模块（common、common-module-api、common-module-impl、ai-api、ai-impl、patient、doctor、admin、application、integration）全部编译失败。

**修正方案（方案A）**：

### 操作1（T3，修正）：完全移除5个Starter条目
**文件**：`AIMedical/backend/pom.xml`
**操作**：删除第84-109行，即以下5个dependency块（每块约5行）：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.2.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>3.2.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
    <version>3.2.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>3.2.5</version>
    <scope>test</scope>
</dependency>
```
**理由**：移出后，子模块中声明这些starter时版本自动从`spring-boot-starter-parent:3.2.5`的BOM继承。

### 操作2（T4，与原设计一致）：删除h2的runtime scope
**文件**：`AIMedical/backend/pom.xml`
**操作**：删除第82行 `<scope>runtime</scope>`

### 操作3（T7，与原设计一致）：删除ai-impl冗余common依赖
**文件**：`AIMedical/backend/modules/ai/ai-impl/pom.xml`
**操作**：删除第17-20行，即：
```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>common</artifactId>
</dependency>
```

### 操作4（审查修订新增）：标记ParentPomVersionTest基线测试为废弃
**文件**：`AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomVersionTest.java`
**操作**：在每个@Test方法上添加`@Disabled`注解（或在类级别添加），并添加注释说明该测试为变更前基线，5个Spring Boot Starter条目从dependencyManagement移除后此测试不再适用。
**理由**：ParentPomVersionTest的5条XPath断言均验证starter条目在dependencyManagement中存在且含version。方案A完全移除这些条目后该测试必然全量失败。作为基线测试不再有效，标记为废弃而非删除以保留历史参考。

### 验证方式
1. 运行 `mvn compile` — 确认所有10个子模块依赖解析正常，无missing version错误
2. 运行 `mvn test -pl common -Dtest=ParentPomDependencyManagementCleanupTest` — 6项测试全部通过（验证5个starter不在dependencyManagement中、h2无scope）
3. 运行 `mvn test -pl ai-impl -Dtest=AiImplPomCleanDependencyTest` — 5项测试全部通过（验证common已移除、条目数正确）
4. 确认 `ParentPomVersionTest` 已被 `@Disabled` 标记，不再纳入测试套件（该测试为变更前基线，5项断言均验证删除的starter条目，已不适用）

## 修订说明（v2 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 验证步骤3声称ParentPomVersionTest在改动后仍会"保持通过"，但事实相反——5个Starter条目被移出dependencyManagement后，该测试的5条XPath断言必然全部失败 | 1) 从验证步骤中移除ParentPomVersionTest条目；2) 新增验证步骤4确认该测试已@Disabled标记；3) 新增操作OP-04（标记ParentPomVersionTest为废弃） |
| 任务应包含删除ParentPomVersionTest.java或明确指示该测试将被标记为过期 | 新增OP-04操作说明：在ParentPomVersionTest.java的@Test方法上添加@Disabled注解，保留文件但标记废弃 |
