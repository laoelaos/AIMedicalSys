# 详细设计（v7 r1）

## 概述
将后端 Maven 多模块从扁平目录布局迁移至 OOD §2.1 定义的分层布局，补齐缺失的聚合 POM。本设计仅涉及 POM 配置和目录结构调整，不修改任何 Java 源代码。

## 文件规划

### 新建文件
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/pom.xml` | 新建 | common-module 聚合 POM，聚合 common-module-api + common-module-impl |
| `AIMedical/backend/modules/ai/pom.xml` | 新建 | ai 聚合 POM，聚合 ai-api + ai-impl |

### 移动后修改的文件
| 原路径 | 新路径 | 操作 |
|-------|--------|------|
| `AIMedical/backend/common-module-api/pom.xml` | `AIMedical/backend/modules/common-module/common-module-api/pom.xml` | 移动 + 修改 parent |
| `AIMedical/backend/common-module-impl/pom.xml` | `AIMedical/backend/modules/common-module/common-module-impl/pom.xml` | 移动 + 修改 parent |
| `AIMedical/backend/ai-api/pom.xml` | `AIMedical/backend/modules/ai/ai-api/pom.xml` | 移动 + 修改 parent |
| `AIMedical/backend/ai-impl/pom.xml` | `AIMedical/backend/modules/ai/ai-impl/pom.xml` | 移动 + 修改 parent |
| `AIMedical/backend/patient/pom.xml` | `AIMedical/backend/modules/patient/pom.xml` | 移动 + 修改 relativePath |
| `AIMedical/backend/doctor/pom.xml` | `AIMedical/backend/modules/doctor/pom.xml` | 移动 + 修改 relativePath |
| `AIMedical/backend/admin/pom.xml` | `AIMedical/backend/modules/admin/pom.xml` | 移动 + 修改 relativePath |

### 不移动但修改的文件
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/pom.xml` | 修改 | 更新 `<modules>` 声明为分层路径 |

### 不修改的文件
| 文件路径 | 理由 |
|---------|------|
| `AIMedical/backend/application/pom.xml` | 保持在 backend/ 根级不变，relativePath 保持 `../pom.xml` |
| `AIMedical/backend/common/pom.xml` | 保持在 backend/ 根级不变，relativePath 保持 `../pom.xml` |
| `AIMedical/backend/integration/pom.xml` | 保持在 backend/ 根级不变，relativePath 保持 `../pom.xml` |

## 类型定义
（无新增 Java/TypeScript 类型，纯 POM 结构变更）

## 聚合 POM 设计

### modules/common-module/pom.xml
**形态**：XML POM（packaging pom）
**路径**：`AIMedical/backend/modules/common-module/pom.xml`
**职责**：common-module 子树的聚合入口，为 common-module-api 和 common-module-impl 提供 parent 引用

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-module</artifactId>
    <packaging>pom</packaging>
    <name>Common Module Aggregator</name>
    <modules>
        <module>common-module-api</module>
        <module>common-module-impl</module>
    </modules>
</project>
```

- parent: `com.aimedical:aimedical-sys:0.0.1-SNAPSHOT`
- relativePath: `../../pom.xml`（从 `modules/common-module/` → `backend/pom.xml`）
- artifactId: `common-module`
- packaging: `pom`
- modules: `common-module-api`, `common-module-impl`
- 不含 dependencyManagement 或 dependencies

### modules/ai/pom.xml
**形态**：XML POM（packaging pom）
**路径**：`AIMedical/backend/modules/ai/pom.xml`
**职责**：ai 子树的聚合入口，为 ai-api 和 ai-impl 提供 parent 引用

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>ai</artifactId>
    <packaging>pom</packaging>
    <name>AI Module Aggregator</name>
    <modules>
        <module>ai-api</module>
        <module>ai-impl</module>
    </modules>
</project>
```

- parent: `com.aimedical:aimedical-sys:0.0.1-SNAPSHOT`
- relativePath: `../../pom.xml`
- artifactId: `ai`
- packaging: `pom`
- modules: `ai-api`, `ai-impl`
- 不含 dependencyManagement 或 dependencies

## 子模块 POM 修改

### common-module-api/pom.xml（移动后）
**新路径**：`modules/common-module/common-module-api/pom.xml`
**变更**：
- parent artifactId: `aimedical-sys` → `common-module`
- relativePath: `../pom.xml`（保持不变——从 `modules/common-module/common-module-api/../pom.xml` = `modules/common-module/pom.xml` ✓）
- 其余内容（artifactId、dependencies 等）不变

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>common-module</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>common-module-api</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### common-module-impl/pom.xml（移动后）
**新路径**：`modules/common-module/common-module-impl/pom.xml`
**变更**：
- parent artifactId: `aimedical-sys` → `common-module`
- relativePath: `../pom.xml`（保持不变）

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>common-module</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>common-module-impl</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common-module-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### ai-api/pom.xml（移动后）
**新路径**：`modules/ai/ai-api/pom.xml`
**变更**：
- parent artifactId: `aimedical-sys` → `ai`
- relativePath: `../pom.xml`（保持不变）

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>ai</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>ai-api</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### ai-impl/pom.xml（移动后）
**新路径**：`modules/ai/ai-impl/pom.xml`
**变更**：
- parent artifactId: `aimedical-sys` → `ai`
- relativePath: `../pom.xml`（保持不变）

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>ai</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>ai-impl</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>ai-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### patient/pom.xml（移动后）
**新路径**：`modules/patient/pom.xml`
**变更**：
- relativePath: `../pom.xml` → `../../pom.xml`
- parent artifactId: `aimedical-sys`（不变）
- 其余内容不变

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <!-- 其余不变 -->
</project>
```

doctor/pom.xml、admin/pom.xml 同理。

## 根 POM 修改

### backend/pom.xml modules 声明
**当前**：
```xml
<module>common</module>
<module>common-module-api</module>
<module>common-module-impl</module>
<module>ai-api</module>
<module>ai-impl</module>
<module>patient</module>
<module>doctor</module>
<module>admin</module>
<module>application</module>
<module>integration</module>
```

**改为**：
```xml
<module>common</module>
<module>modules/common-module/common-module-api</module>
<module>modules/common-module/common-module-impl</module>
<module>modules/ai/ai-api</module>
<module>modules/ai/ai-impl</module>
<module>modules/patient</module>
<module>modules/doctor</module>
<module>modules/admin</module>
<module>application</module>
<module>integration</module>
```

**注意**：dependencyManagement 中的 artifactId 不变（仍为 `common-module-api`、`common-module-impl`、`ai-api`、`ai-impl`、`patient`、`doctor`、`admin`），因为 artifactId 本身不随目录变化。

## 目录移动操作顺序
为保证中间状态一致，操作应按以下顺序执行：
1. 创建 `backend/modules/`、`backend/modules/common-module/`、`backend/modules/ai/` 目录
2. 创建 `backend/modules/common-module/pom.xml`（新聚合 POM）
3. 创建 `backend/modules/ai/pom.xml`（新聚合 POM）
4. 移动 `backend/common-module-api/` → `backend/modules/common-module/common-module-api/`
5. 移动 `backend/common-module-impl/` → `backend/modules/common-module/common-module-impl/`
6. 移动 `backend/ai-api/` → `backend/modules/ai/ai-api/`
7. 移动 `backend/ai-impl/` → `backend/modules/ai/ai-impl/`
8. 移动 `backend/patient/` → `backend/modules/patient/`
9. 移动 `backend/doctor/` → `backend/modules/doctor/`
10. 移动 `backend/admin/` → `backend/modules/admin/`
11. 修改 `backend/pom.xml` 的 `<modules>` 声明
12. 修改各子模块 POM 的 parent 引用（relativePath / artifactId）

## 错误处理
- Maven POM 解析错误：如果 `<relativePath>` 指向错误，Maven 将在 `mvn validate` 阶段报 `Non-resolvable parent POM` 错误
- 如果 `<modules>` 目录路径与文件系统不匹配，Maven 报 `Child module directory does not exist` 错误
- 如果聚合 POM 的 `<modules>` 声明遗漏子模块，Maven 不会报错但对应模块不会参与构建
- 所有错误均在构建期暴露，无运行期影响

## 行为契约
### 目录移动契约
- 移动后 Java 源代码的包路径不受影响（`src/main/java/com/aimedical/` 下的包路径以 `pom.xml` 中的 `<groupId>` 为准，不依赖目录结构）
- 移动后 `Git` 识别为文件重命名（rename），非新增+删除（需在 git mv 操作后验证）
- `application/pom.xml`、`common/pom.xml`、`integration/pom.xml` 不移动
- `application/pom.xml`、`common/pom.xml`、`integration/pom.xml` 的 `<relativePath>../pom.xml</relativePath>` 不变（仍指向根 POM）

### module 引用契约
- 根 POM `<modules>` 声明使用相对于 `backend/` 的路径
- 聚合 POM `<modules>` 声明使用相对于聚合 POM 所在目录的路径
- dependencyManagement 中的 artifactId 不随目录路径变化
- application/pom.xml 中引用各模块的 `<version>` 仍使用 `${project.version}`，无需变更

### 验证契约
1. `mvn validate -N`：验证父 POM 自身解析（无模块参与，验证 POM 的 XML 结构和 parent 解析）
2. `mvn validate`：验证全部 11 模块依赖解析（reactor 解析所有模块，确认 `relativePath` 和父引用正确）
3. `mvn compile -DskipTests`：确认编译全部通过（验证所有模块的 Java 源码编译）

## 依赖关系
- 依赖的已有类型：Spring Boot parent POM 3.2.5、Maven 3.6+
- 无新增外部依赖
- 无新增 Java 类型依赖
- 暴露给后续任务：`modules/common-module/pom.xml` 和 `modules/ai/pom.xml` 作为子树的独立构建入口
