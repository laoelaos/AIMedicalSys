# 详细设计（v3）

## 概述

创建两个缺失的包占位目录及其 `package-info.java` 文件，对齐 OOD §2.3 包命名规范。纯结构补充，无运行时影响，无代码变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/config/package-info.java` | 新建 | T2：common-module-impl 业务级配置包占位 |
| `AIMedical/backend/common/src/main/java/com/aimedical/common/util/package-info.java` | 新建 | T9：common 通用工具包占位 |

## 类型定义

（本任务为纯目录 + 占位文件创建，无新增 Java 类型）

## 操作定义

### OP-01（T2）：创建 common-module-impl config/ 包占位

**目标路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/config/`

**产出文件**：`config/package-info.java`
```java
/**
 * 业务级配置类（如 CommonConfig、CustomValidator 等）。
 */
package com.aimedical.modules.commonmodule.config;
```

**说明**：
- 当前 `commonmodule/` 下已有 `permission/`（含实体+Repository）和 `dict/`（含 .gitkeep），缺少 OOD §2.3 规划的 `config/` 子包
- 使用 `package-info.java`（含 Javadoc 包描述）作为标准 Java 包占位方式，优于 `.gitkeep`
- 后续扩展为真实配置类后，本文件自然融入包结构

### OP-02（T9）：创建 common util/ 包占位

**目标路径**：`AIMedical/backend/common/src/main/java/com/aimedical/common/util/`

**产出文件**：`util/package-info.java`
```java
/**
 * 通用工具类集合（如 StringUtils、DateUtils、BeanUtils 扩展等）。
 */
package com.aimedical.common.util;
```

**说明**：
- 当前 `common/` 下已有 `base/`、`config/`、`exception/`、`result/` 四个子包，缺少 OOD §2.3 规划的 `util/` 子包
- OOD 文档（§2.1 Monorepo 目录布局）第 73 行明确列出 `util/` 作为 common 的六个子包之一

## 错误处理

无。操作为纯目录创建 + 占位文件写入，无编译期或运行期错误路径。

## 行为契约

1. **操作顺序**：无依赖，OP-01 和 OP-02 可任意顺序或并行执行
2. **原子性**：建议单次 PR 提交
3. **验证标准**：
   - 目标目录 `config/` 在 common-module-impl 下可见，且含 `package-info.java`
   - 目标目录 `util/` 在 common 下可见，且含 `package-info.java`
   - `mvn compile -q` 无错误（package-info.java 为标准 Java 包注解文件，不影响编译）
   - 不新增测试（纯结构占位，无可测试的业务行为）

## 依赖关系

- 无新增外部依赖
- 无运行时代码变更
- 受影响文件仅新建的两个 `package-info.java`
