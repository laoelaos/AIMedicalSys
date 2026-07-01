# 测试报告（v11）

## 测试文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordFieldTest.java` | M11: 7→9 个枚举常量，新增 MISSING_FIELDS/PARTIAL_CONTENT 校验 |
| 修改 | `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequestTest.java` | M05: 新增 @NotNull/@Size 验证测试；M09: doctorId 字段 getter/setter 测试 |
| 修改 | `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverterTest.java` | M07: toRecordGenerateResponse success 条件逻辑（非超时→false）；M11: toFieldsMap 扩展至 9 字段，ObjectMapper 构造注入 |
| 修改 | `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImplTest.java` | M04: UPDATE 路径（findByVisitId 复用实体）+ INSERT 路径；M09: doctorId 写入实体 |

## 行为契约覆盖

| 问题 | 契约描述 | 正向用例 | 边界/异常用例 |
|------|---------|---------|-------------|
| M04 | UPDATE 路径复用已有 entity（保留 recordId/version）| `shouldReuseExistingRecordOnUpdatePath` | `shouldCreateNewRecordOnInsertPath`（无已有记录时 INSERT） |
| M04 | 乐观锁冲突→catch 设置 errorCode | — | `shouldHandleOptimisticLockException`（已有） |
| M05 | dialogueText 验证 @NotNull/@Size(min=50,max=10000) | `shouldPassValidationWithValidDialogueText` | `shouldFailValidationWhenDialogueTextIsNull`, `shouldFailValidationWhenDialogueTextIsTooShort`, `shouldFailValidationWhenDialogueTextIsTooLong` |
| M06 | @Value 注入（超时参数化）| — | 编译验证即可，单元测试中构造注入替代 |
| M07 | success=true: aiResult.isSuccess() && data!=null 或 MR_GEN_AI_TIMEOUT | `toRecordGenerateResponseShouldBuildResponseFromAiResult`（data!=null）| `toRecordGenerateResponseShouldReturnSuccessFalseWhenNonTimeoutFailure`（ERROR→false）|
| M07 | success=false: 其他非超时失败 | — | `toRecordGenerateResponseShouldReturnSuccessFalseWhenNonTimeoutFailure`, `toRecordGenerateResponseShouldReturnSuccessFalseWhenSuccessWithNullData` |
| M07 | 超时降级 success=true | `toRecordGenerateResponseShouldReturnSuccessTrueWhenTimeoutEvenWithNullData` | — |
| M09 | generate() 写入 doctorId | `shouldWriteDoctorIdToEntity` | — |
| M10 | @Column name = "content_json" | — | 编译验证即可，JPA 注解无反光测试必要 |
| M11 | toFieldsMap 返回 9 个键值对 | `toFieldsMapShouldMapAllNineFields` | `toFieldsMapShouldPreserveNullValues`（全 null→key 存在值为 null）|
| M11 | missingFields 序列化（String.join）| — | `toFieldsMapShouldHandleNullMissingFields`→空串, `toFieldsMapShouldHandleEmptyMissingFields`→空串 |
| M11 | partialContent JSON 序列化 | `toFieldsMapShouldMapAllNineFields`（assertNotNull+contains）| `toFieldsMapShouldHandleNullPartialContent`→null |

## 编译验证

`mvn compile -pl modules/medical-record -am -q` 无编译错误。

## 设计偏差说明

无偏差。所有测试覆盖详细设计中定义的行为契约。
