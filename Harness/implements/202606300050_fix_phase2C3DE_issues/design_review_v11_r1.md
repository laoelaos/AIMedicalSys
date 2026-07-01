# 设计审查报告（v11 r1）

## 审查结果
REJECTED

## 发现

### **[一般] M10: @Column 注解错误地移除了 columnDefinition = "TEXT"**

- **位置**: `detail_v11.md` §M10 L82 — `@Column(columnDefinition = "TEXT")` → `@Column(name = "content_json")`
- **问题**: 设计将 `columnDefinition = "TEXT"` 完全移除，但正确的修复应是在保留 `columnDefinition = "TEXT"` 的基础上增加 `name = "content_json"`。
- **为什么是问题**:
  1. 诊断报告证据 (06_phase2C3DE_report.md:360) 明确列出应为 `@Column(name="content_json", columnDefinition = "TEXT")`，即保留 columnDefinition。
  2. OOD §3.3 (07_ood_phase2_C_3_DE.md:686) 要求存储列为 "单列 JSON TEXT（contentJson 字段）"，明确要求 TEXT 类型。
  3. 移除 `columnDefinition = "TEXT"` 后，若环境启用 `spring.jpa.hibernate.ddl-auto`，Hibernate 可能将列默认映射为 VARCHAR(255)，导致 JSON 病历内容（远超 255 字符）被截断。
- **期望修正方向**: 修改为 `@Column(name = "content_json", columnDefinition = "TEXT")`，仅在现有注解上补充 `name` 属性，不删除 `columnDefinition`。

### **[轻微] M11: partialContent 使用 String.valueOf() 可能丢失数据结构**

- **位置**: `detail_v11.md` §M11 L95 — `String.valueOf(aiResponse.getPartialContent())`
- **问题**: `MedicalRecordGenResponse.partialContent` 类型为 `Object`，`String.valueOf(Object)` 对于复杂类型（如 Map、List）会输出 `ClassName@hashCode` 形式的无意义字符串，丢失原始数据结构。任务文件 (task_v11.md:185) 明确提及 "JSON 序列化" 作为可选方案。
- **期望修正方向**: 对于 `Object partialContent`，建议使用 Jackson JSON 序列化（`objectMapper.writeValueAsString()`）替代 `String.valueOf()`，以保留结构化数据。如 AI 返回的实际类型确定仅为简单 String，可在实现中保持 `String.valueOf()`，但设计应明确说明此前提假设。
