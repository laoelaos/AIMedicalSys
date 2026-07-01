# 任务指令（v7）

## 动作
NEW

## 任务描述
在 backend/pom.xml 中注册 consultation/prescription/medical-record 三个新 Maven 模块；为每个模块创建 pom.xml 和 src/main/java/src/test/java 目录骨架。

预期文件：
- `AIMedical/backend/pom.xml` — 追加 `<module>modules/consultation</module>`、`<module>modules/prescription</module>`、`<module>modules/medical-record</module>` 到 `<modules>` 块
- `AIMedical/backend/modules/consultation/pom.xml` — 新建，参照 patient 模块
- `AIMedical/backend/modules/prescription/pom.xml` — 新建，参照 patient 模块
- `AIMedical/backend/modules/medical-record/pom.xml` — 新建，参照 patient 模块
- `AIMedical/backend/modules/consultation/src/main/java/` — 创建目录
- `AIMedical/backend/modules/consultation/src/test/java/` — 创建目录
- `AIMedical/backend/modules/prescription/src/main/java/` — 创建目录
- `AIMedical/backend/modules/prescription/src/test/java/` — 创建目录
- `AIMedical/backend/modules/medical-record/src/main/java/` — 创建目录
- `AIMedical/backend/modules/medical-record/src/test/java/` — 创建目录

## 选择理由
T1–T6（ai-api DTO 扩展、Store 接口、门面接口+事件、DosageStandard 实体）已全部完成。T7 是 T8–T11（业务模块实现代码）的编译容器前置依赖，必须先创建模块骨架，后续任务才能将 Java 源文件写入正确的包路径。

## 任务上下文
- 父 pom 当前 modules 列表（backend/pom.xml:18–27）：common、modules/common-module、modules/ai、modules/patient、modules/doctor、modules/admin、application、integration
- 新模块 artifactId：consultation、prescription、medical-record
- 新模块 packaging：jar
- 每个新模块需依赖：common、common-module-api、ai-api、spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-validation、spring-boot-starter-test（scope=test）、lombok（optional=true）
- 每个新模块需设置 `<properties>` 启用 JaCoCo：`<jacoco.skip>false</jacoco.skip>`、`<jacoco.skip.check>false</jacoco.skip.check>`
- 父 pom 的 `<modules>` 插入顺序建议在 modules/admin 之后，保持按字母序/逻辑分组

## 已有代码上下文
patient 模块 pom.xml 是标准模板：
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>patient</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
        <dependency><groupId>com.aimedical</groupId><artifactId>common</artifactId></dependency>
        <dependency><groupId>com.aimedical</groupId><artifactId>common-module-api</artifactId></dependency>
        <dependency><groupId>com.aimedical</groupId><artifactId>ai-api</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <properties>
        <jacoco.skip>false</jacoco.skip>
        <jacoco.skip.check>false</jacoco.skip.check>
    </properties>
</project>
```
