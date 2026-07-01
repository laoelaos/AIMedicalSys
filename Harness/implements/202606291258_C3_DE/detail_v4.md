# 详细设计（v4）

## 概述

在 common 模块的 `entity` 包中新建 DosageStandard JPA 实体，继承 BaseEntity，按药品编码+给药途径+年龄/体重分级定义单次/日剂量上限标准，并建立数据库复合索引。供 prescription 模块只读查询、admin 模块写入管理。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/common/src/main/java/com/aimedical/common/entity/DosageStandard.java` | 新建 | 药品剂量标准 JPA 实体 |

## 类型定义

### DosageStandard

**形态**：class（JPA @Entity）
**包路径**：`com.aimedical.common.entity`
**职责**：药品剂量标准持久化实体，按（drugCode + routeOfAdministration + 年龄/体重分级）维度存储剂量上限

```java
package com.aimedical.common.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "dosage_standard", indexes = {
    @Index(name = "idx_dosage_drug_route", columnList = "drugCode, routeOfAdministration"),
    @Index(name = "idx_dosage_drug_route_age_weight", columnList = "drugCode, routeOfAdministration, ageRangeStart, ageRangeEnd, weightRangeStart, weightRangeEnd")
})
@Data
public class DosageStandard extends BaseEntity {

    @NotBlank
    @Column(nullable = false, length = 50)
    private String drugCode;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String routeOfAdministration;

    @Column
    private Integer ageRangeStart;

    @Column
    private Integer ageRangeEnd;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightRangeStart;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightRangeEnd;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal singleMax;

    @Column(precision = 12, scale = 3)
    private BigDecimal dailyMax;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String unit;
}
```

**字段明细**：

| 字段名 | 类型 | 数据库列 | 约束 | 说明 |
|--------|------|---------|------|------|
| drugCode | String | drug_code VARCHAR(50) | NOT NULL | 药品编码（国药准字号），逻辑外键 |
| routeOfAdministration | String | route_of_administration VARCHAR(20) | NOT NULL | 给药途径 |
| ageRangeStart | Integer | age_range_start INT | nullable | 适用年龄下限（含，岁） |
| ageRangeEnd | Integer | age_range_end INT | nullable | 适用年龄上限（含，岁） |
| weightRangeStart | BigDecimal(10,2) | weight_range_start DECIMAL(10,2) | nullable | 适用体重下限（含，kg） |
| weightRangeEnd | BigDecimal(10,2) | weight_range_end DECIMAL(10,2) | nullable | 适用体重上限（含，kg） |
| singleMax | BigDecimal(12,3) | single_max DECIMAL(12,3) | NOT NULL | 单次剂量上限 |
| dailyMax | BigDecimal(12,3) | daily_max DECIMAL(12,3) | nullable | 日剂量上限 |
| unit | String | unit VARCHAR(20) | NOT NULL | 剂量单位 |

**公开接口**：
- 继承 BaseEntity：`getId()` / `setId()` / `getCreatedAt()` / `setCreatedAt()` / `getUpdatedAt()` / `setUpdatedAt()` / `getDeleted()` / `setDeleted()`
- Lombok `@Data` 生成所有字段的 getter/setter、`equals()`、`hashCode()`、`toString()`
- 默认无参构造器（Lombok 生成，JPA 要求）

**构造方式**：默认无参构造器（配合 setter 赋值）
**类型关系**：继承 `BaseEntity`（`@MappedSuperclass`，含 id/createdAt/updatedAt/deleted）

## 索引定义

| 索引名 | 列组合 | 用途 |
|--------|--------|------|
| `idx_dosage_drug_route` | drugCode, routeOfAdministration | 按药品+给药途径查询剂量标准 |
| `idx_dosage_drug_route_age_weight` | drugCode, routeOfAdministration, ageRangeStart, ageRangeEnd, weightRangeStart, weightRangeEnd | 按完整维度精确匹配剂量标准 |

通过 `@Table(indexes = {...})` 声明，Hibernate DDL auto 自动建表时生成。

## 错误处理

- JPA 持久化层面：字段约束由 `@Column(nullable = false)` 确保数据库 NOT NULL
- Bean Validation 层面：`@NotBlank` / `@NotNull` / `@Positive` 在实体生命周期回调前校验（需配合 `@Valid` 或 Spring 的 `Validator` 触发）
- 无自定义异常类型或错误码

## 行为契约

- **字段默认值**：所有字段默认 null（除继承自 BaseEntity 的 `deleted = false`）
- **年龄/体重范围语义**：ageRangeStart / ageRangeEnd / weightRangeStart / weightRangeEnd 均为含边界值（inclusive）。null 表示无下限/上限
- **unit 值域约束**：字符串类型，值域限定为 DosageUnitGroup 枚举中各分组的单位值。DosageUnitGroup 尚未在代码库中定义，当前设计保留为纯 String 字段，值域约束待后续 DosageUnitGroup 枚举定义完成后补充 `@Pattern` 或自定义校验注解
- **逻辑删除**：继承 BaseEntity 的 `@SQLDelete` / `@SQLRestriction`，删除时执行 UPDATE SET deleted = true，查询自动过滤已删除记录
- **无物理外键**：drugCode 为逻辑外键，不建立 FK 约束

## 依赖关系

- **编译期依赖**：
  - `com.aimedical.common.base.BaseEntity`（父类，同一 common 模块）
  - `jakarta.persistence.*`（由 spring-boot-starter-data-jpa 传递引入）
  - `jakarta.validation.constraints.*`（由 spring-boot-starter-validation 传递引入）
  - `lombok`（已在 common pom 中声明）
  - `java.math.BigDecimal`（JDK 内置）
- **运行时依赖**：无特殊要求，JPA EntityManager 自动管理
- **被依赖**（后续任务）：
  - prescription 模块的 `DosageStandardRepository`（只读，`extends Repository<DosageStandard, Long>`）
  - admin 模块的管理 Service（写入，CRUD）
