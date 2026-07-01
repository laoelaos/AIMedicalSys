# 任务指令（v4）

## 动作
NEW

## 任务描述
在 common 模块中创建 DosageStandard JPA 实体，包含年龄/体重分级字段、日剂量上限、单次剂量上限、剂量单位等字段，并建立数据库索引。

预期文件：
- `AIMedical/backend/common/src/main/java/com/aimedical/common/entity/DosageStandard.java`

## 选择理由
T6 是底层依赖优先策略中的无依赖任务之一。DosageStandard 实体是 prescription 模块（T9 处方审核、T10 辅助开方）的前置依赖，且与 T4/T5 无依赖关系。单实体任务规模小、验证快，适合作为下一增量。

## 任务上下文
- 实体需迁移至 common 模块的 entity 包下（当前 common 模块无 entity 子包），使 prescription 模块和 admin 模块均可引用而不产生跨模块编译期依赖
- 使用 `@Table(indexes=...)` 建立索引
- 需继承 BaseEntity（继承 id/createdAt/updatedAt/deleted 字段）
- common 模块已依赖 spring-boot-starter-data-jpa

## 已有代码上下文
- BaseEntity 位于 `common/src/main/java/com/aimedical/common/base/BaseEntity.java`，使用 `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)` + 逻辑删除（`@SQLDelete`/`@SQLRestriction`）
- common 模块 pom.xml 已包含 spring-boot-starter-data-jpa、lombok、spring-boot-starter-validation 依赖
- 当前无其他 entity 类可作为实体样式参考，需参照 BaseEntity 模式

## 字段定义

| 字段名 | 类型 | 说明 |
|--------|------|------|
| drugCode | String | 药品编码（国药准字号），逻辑外键引用药品基础信息 |
| routeOfAdministration | String | 给药途径 |
| ageRangeStart | Integer（nullable） | 适用年龄下限（含，单位：岁） |
| ageRangeEnd | Integer（nullable） | 适用年龄上限（含，单位：岁） |
| weightRangeStart | BigDecimal（nullable） | 适用体重下限（含，单位：kg） |
| weightRangeEnd | BigDecimal（nullable） | 适用体重上限（含，单位：kg） |
| singleMax | BigDecimal | 单次剂量上限 |
| dailyMax | BigDecimal（nullable） | 日剂量上限 |
| unit | String | 剂量单位，值域限定为 DosageUnitGroup 枚举中各分组的单位值 |

## 索引定义
- `idx_dosage_drug_route`：(drugCode, routeOfAdministration)
- `idx_dosage_drug_route_age_weight`：(drugCode, routeOfAdministration, ageRangeStart, ageRangeEnd, weightRangeStart, weightRangeEnd)
