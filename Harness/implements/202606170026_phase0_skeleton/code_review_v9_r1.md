# 代码审查报告（v9 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `backend/patient/src/test/java/com/aimedical/modules/patient/entity/PatientEntityTest.java:19` — `assertNull(entity.getDeleted())` 断言始终失败，因为 `BaseEntity.deleted` 初始化为 `false`（非 null）。该缺陷同样存在于 `backend/doctor/src/test/java/com/aimedical/modules/doctor/entity/DoctorEntityTest.java:19` 和 `backend/admin/src/test/java/com/aimedical/modules/admin/entity/AdminEntityTest.java:19`。

## 修改要求（仅 REJECTED 时）

### 1. EntityTest 断言错误
- **文件**: `backend/patient/src/test/java/com/aimedical/modules/patient/entity/PatientEntityTest.java`（及 doctor/admin 对应文件）
- **位置**: `shouldExtendBaseEntity()` 方法内第19行
- **问题**: `assertNull(entity.getDeleted())` — `BaseEntity` 的 `deleted` 字段初始值为 `Boolean.FALSE`，非 `null`，该断言必然失败，导致测试套件不可通过。
- **修正方向**: 将 `assertNull(entity.getDeleted())` 改为 `assertFalse(entity.getDeleted())` 或 `assertNotNull(entity.getDeleted())`。
