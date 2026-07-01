# 再审议判定报告（v3）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出7项问题，其中1项严重（问题1：AuditRecord落库在降级路径中被遗漏）、5项一般（问题2、4、5、6、7）、1项轻微（问题3）。质询报告以LOCATED确认了全部问题。由于存在严重和一般等级的问题，不符合PASS条件，需重新运行组件A进行修复。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：AuditRecord落库在降级路径中被遗漏，与§3.2行为契约矛盾
- **所在位置**：§4.2 "处方审核场景"——降级流程描述
- **严重程度**：严重
- **改进建议**：在§4.2降级流程中显式补充AuditRecord持久化步骤，或与正常路径合并为一个统一表述以消除歧义。同时确认：本地规则校验结果写入AuditRecord的riskLevel字段时，是否需额外标记fromFallback=true以区分AI结果与本地规则结果。

- **问题描述**：分诊降级链路不完整——AI为空且规则也为空时的行为未定义
- **所在位置**：§4.1 "智能分诊场景"
- **严重程度**：一般
- **改进建议**：将三条路径改为线性降级链：AI无结果→规则匹配无结果→FallbackProvider兜底。在§4.1中明确TriageRuleEngine.match()返回空列表时，应继续调用DepartmentFallbackProvider.getFallbackDepartments()；TriageService的协作描述也需同步更新。

- **问题描述**：TriageResponse DTO字段结构未定义，无法支撑编码实现
- **所在位置**：§3.1 包C核心抽象；§2.1目录
- **严重程度**：一般
- **改进建议**：补充TriageResponse的字段定义，至少包含：departments（推荐科室列表）、sessionId（多轮场景）、needFollowUp（是否需要追问）、followUpQuestion（追问内容）、confidence（可选置信度标量）。同时明确DialogueCreateRequest是否为首轮请求DTO，若是则说明其字段。

- **问题描述**：剂量单位转换规则集未定义，实现面临随意假设风险
- **所在位置**：§8.3 "单位一致性校验"
- **严重程度**：一般
- **改进建议**：补充剂量单位兼容分组的枚举或表格定义。推荐方案：定义一个DosageUnitGroup枚举（如MASS_GROUP: mcg↔mg↔g、VOLUME_GROUP: ml↔L等），各组内支持自动换算，跨组返回RX_ASSIST_UNIT_MISMATCH。对于IU等非质量/体积单位可单独分组或定义为不可换算。转换系数以常量形式固化在DosageThresholdService中，后续可扩展为数据库可配置。

- **问题描述**：MedicalRecord实体字段未定义，病历结构化输出模型缺失
- **所在位置**：§3.3 包D-AI2；§2.1目录 entity/MedicalRecord.java
- **严重程度**：一般
- **改进建议**：在§3.3中补充核心字段枚举或值对象定义（如MedicalRecordField枚举），列出病历结构化输出的顶层字段标识。RecordGenerateRequest和RecordGenerateResponse补充字段定义。DepartmentTemplateConfig.requiredFields的类型明确为List<MedicalRecordField>。这与§9.1中DEFAULT模板字段列表形成呼应，使字段标识符与模板内容一致。

- **问题描述**：规则/模板配置变更缺少审计溯源能力
- **所在位置**：§3.1 TriageRuleEngine（规则热加载）；§3.3 TemplateConfigManager（模板事件驱动刷新）；§9（模板更新）
- **严重程度**：一般
- **改进建议**：在规则变更和模板更新的事件处理中添加配置变更日志记录。推荐方案：(a) 定义ConfigChangeLog实体（归属admin模块或common模块）；(b) 在TemplateConfigChangeEvent和规则变更的处理链中写入审计日志；(c) 事件发布者提供新旧值语义。Phase 2可先以日志文件方式实现，后续迁移至数据库审计表。
