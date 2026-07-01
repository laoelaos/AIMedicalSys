# 设计审查报告（v15 R3）

## 审查结果
REJECTED

## 发现

### 发现 1（一般）
**模板提示文案的占位符替换机制缺失**

设计第 397 行定义 DEFAULT 模板的 `promptMessages` 为 `"{{fieldName}}字段缺失"`、`suggestedActions` 为 `"请补充{{fieldName}}信息"`，但未定义 `{{fieldName}}` 占位符的解析机制。`MissingFieldDetectorImpl` 中存在 `getFieldName()` 方法（将枚举名映射为中文名），但该方法与提示文案生成流程之间无明确关联。若按字面量直接存储，客户端收到的 `FieldMissingHint.promptMessage` 将为 `"{{fieldName}}字段缺失"` 等未解析的文字，而非期望的中文提示。

**期望修正方向**：明确占位符解析策略——要么在 DEFAULT 模板中直接存储已解析的完整中文文案（如 `"主诉字段缺失"`），要么在 detector 中定义 `{{fieldName}}` 的替换逻辑，确保 `getFieldName()` 的输出实际用于替换占位符。

### 发现 2（一般）
**缺失字段检测采用反射调用 getter，方案脆弱**

设计第 437 行 `isFieldFilled` 方法标注使用反射调用 `MedicalRecordGenResponse` 的对应 getter。此方案在字段名或 getter 方法签名变更时将静默失效（编译期无检查）。`MedicalRecordConverter.toFieldsMap()` 已具备将 aiResponse 映射为 `Map<MedicalRecordField, String>` 的能力，detector 直接基于此 Map 判断字段是否为 null/空即可，无需反射。

**期望修正方向**：移除反射方案，改为通过 `MedicalRecordConverter.toFieldsMap()` 获得 `Map<MedicalRecordField, String>` 后进行差集比对，或显式调用 7 个 getter 方法进行非空判断。

### 发现 3（轻微）
**MedicalRecordServiceImpl 过度依赖 MedicalRecordContentConverter**

设计第 478 行将 `MedicalRecordContentConverter` 列为 `MedicalRecordServiceImpl` 的构造器依赖。由于 `MedicalRecord.content` 字段已标注 `@Convert(converter = MedicalRecordContentConverter.class)`，JPA 在持久化/读取时会自动调用该 Converter，Service 层无需显式持有它。此依赖不会导致错误，但会产生无用代码和冗余注入。

**期望修正方向**：从 `MedicalRecordServiceImpl` 的依赖列表中移除 `MedicalRecordContentConverter`。

## 修改要求（REJECTED）

### 针对发现 1
**问题**：DEFAULT 模板的 `{{fieldName}}` 占位符无解析机制，将导致客户端收到字面未替换的提示文案。
**期望**：在 DEFAULT 模板中存储已映射中文名的完整文案（如 `"主诉字段缺失"`），或在 detector 中实现 `{{fieldName}}` → 中文名的替换逻辑，并确保 `getFieldName()` 实际被调用。

### 针对发现 2
**问题**：使用反射检测字段填充状态，当 ai-api 的 DTO 字段变更时运行时静默失效，且无编译期安全保障。
**期望**：移除反射，改用 `MedicalRecordConverter.toFieldsMap()` 输出的 Map 进行差集比对，或逐字段显式调用非空判断。
