# 任务指令（v3）

## 动作
NEW

## 任务描述
工程结构占位补全：在 common-module-impl 创建 `config/` 包目录（package-info.java 占位）、在 common 创建 `util/` 包目录（package-info.java 占位）。

**预期文件路径：**
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/config/package-info.java`（T2）
- `AIMedical/backend/common/src/main/java/com/aimedical/common/util/package-info.java`（T9）

## 选择理由
T2（common-module-impl 缺少 config/ 包）与 T9（common 缺少 util/ 包）同为 OOD §2.3 包命名规范未对齐的结构占位缺失。两者均为纯目录 + 占位文件创建，无运行时风险，无代码变更，可与其他任务完全并行。作为底层基础修复，消除包结构缺失的基线噪音，为后续任务提供完整的工程结构。

## 任务上下文
- T2：OOD §2.3 包命名规范要求 common-module-impl 包含 `permission/`、`config/`、`dict/` 三个子包。当前代码中 `dict/`（含 .gitkeep 占位）和 `permission/`（含 User/Role/Post/Function 实体及 Repository）已存在，但 `config/` 缺失
- T9：OOD §2.3 包命名规范要求 common 模块包含 `base/`、`result/`、`exception/`、`util/`、`config/` 五个子包。当前代码中 `base/`、`config/`、`exception/`、`result/` 均已存在并包含 Java 实现类，但 `util/` 缺失

## 已有代码上下文
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/` 下现有子目录：`permission/`（5 个 Java 文件）、`dict/`（1 个 .gitkeep）
- `AIMedical/backend/common/src/main/java/com/aimedical/common/` 下现有子目录：`base/`（BaseEntity.java, BaseEnum.java）、`result/`（Result.java, PageQuery.java, PageResponse.java）、`exception/`（ErrorCode.java, BusinessException.java, GlobalErrorCode.java）、`config/`（JpaConfig.java, JacksonConfig.java, GlobalExceptionHandler.java）
- 占位风格参考：`dict/` 目录使用 `.gitkeep` 而非 `package-info.java`。但 Phase 0 OOD 规范及诊断报告修复指引建议使用 `package-info.java`（含 Javadoc 包描述）作为 Java 包占位标准方式，后续扩展为真实 Java 类后 package-info.java 自然融入包结构
