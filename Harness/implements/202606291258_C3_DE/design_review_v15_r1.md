# 设计审查报告（v15 R1）

## 审查结果
REJECTED

## 发现

### **[严重]** MedicalRecord.entity.contentJson 字段类型与 AttributeConverter 类型参数不匹配

设计中将 `MedicalRecord` 实体的 `contentJson` 字段声明为 `String` 类型：
```java
@Column(columnDefinition = "TEXT")
@Convert(converter = MedicalRecordContentConverter.class)
private String contentJson;
```

但 `MedicalRecordContentConverter` 的签名是 `AttributeConverter<Map<MedicalRecordField, String>, String>`，其第一个类型参数（实体属性类型）为 `Map<MedicalRecordField, String>`，而非 `String`。JPA 规范要求 `@Convert` 注解的实体字段类型必须与 `AttributeConverter<X,Y>` 的 X 类型一致，否则编译报错。

**修正方向**：将 `contentJson` 字段类型改为 `Map<MedicalRecordField, String>`；或移除 `@Convert` 注解并在 Service 层手动处理 JSON 序列化/反序列化。

### **[一般]** DeptTemplateConfig 实体缺少 @Version 字段

task_v15.md 明确要求 `DeptTemplateConfig` 实体包含 `version(Integer, @Version)` 字段，但详细设计中未列出该字段。这会导致乐观锁能力缺失，可能引发后续并发更新问题。

**修正方向**：在 `DeptTemplateConfig` 实体中添加 `@Version private Integer version;` 字段。

### **[轻微]** Repository 接口缺乏方法签名定义

设计仅在文件规划表中列出 Repository 文件名，未给出方法签名（如 `findByVisitId(String visitId)`、`findByDepartmentId(String departmentId)` 等），虽然 task 中有定义，但设计本身不完整。
