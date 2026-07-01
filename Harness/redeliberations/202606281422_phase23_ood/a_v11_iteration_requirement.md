根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 1. [严重] CRITICAL 剂量告警在提交流程的阻断链路不完整

§3.4 定义 DosageAlertLevel.CRITICAL 写入 PrescriptionDraftContext，"供处方提交时 BLOCK 判定消费"。但 §4.2 处方提交端点的行为契约中，只定义了"最新审核结果为 BLOCK 时拒绝提交"，完全没有提及 PrescriptionDraftContext 的消费路径。此外，PrescriptionDraftContext 的更新语义未定义——当 check-dose 陆续返回不同结果时，旧 CRITICAL 标记是否会因新检查结果而被清除，缺乏契约定义。

改进建议：(1) 在 §4.2 提交端点补充检查 PrescriptionDraftContext 中该 prescriptionId 是否存在 CRITICAL 级别告警；(2) 在 §4.4 check-dose 流程中明确每次重算并覆盖 CRITICAL 标记；(3) 在 §3.4 补充覆盖更新行为契约。

### 2. [严重] POST /api/prescription/submit 端点缺少 Controller 归属

§4.2 定义了 submit 端点的完整契约，但 §2.1 目录结构中两个 Controller 均未包含此端点。

改进建议：新增 PrescriptionSubmitController 或将 submit 端点归入 PrescriptionAuditController。

### 3. [一般] AllergyWarningItem 与 AllergyWarning 命名不一致，且业务层 DTO 字段定义缺失

§2.1 目录列出 AllergyWarning.java，但 §3.4 PrescriptionAssistResponse 引用的是 AllergyWarningItem。业务层 AllergyWarning DTO 字段定义在各处均未出现。

改进建议：统一命名，在 §3.4 或 §1.3 中补充业务层 AllergyWarning DTO 的完整字段定义。

### 4. [一般] DoseWarning 业务层 DTO 字段定义缺失

§3.4 PrescriptionAssistResponse 包含 doseWarnings（List\<DoseWarning\>），§2.1 目录列出 DoseWarning.java，但设计文本中没有任何节段定义业务层 DoseWarning 的字段结构。

改进建议：在 §3.4 或 §1.3 中补充 DoseWarning DTO 字段定义，至少包含：drugId、warningType（枚举，对齐需求文档 3.4.10 的 OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）、message、severity。

### 5. [轻微] MedicalRecord 实体部分字段缺少显式字段名

§3.3 MedicalRecord 字段描述中，"记录标识"、"就诊科室"、"医生 ID" 三个字段只有中文含义说明，没有给出 Java 字段名。

改进建议：补齐 Java 字段名：记录标识→recordId、就诊科室→departmentId、医生 ID→doctorId。

### 6. [严重] 业务层 DosageAlert 缺少 warningType 字段，导致需求 3.4.10 字段级契约断裂

需求文档 3.4.10 的输出契约明确定义 dose_warnings 数组中每项包含 warning_type（枚举值：OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）。设计文档在 §10.4 ai-api 层正确包含了 warningType，但 §3.4 业务层 DosageAlert 没有任何 warningType 字段。DosageThresholdService 的描述只提到"比较剂量阈值"和"日剂量校验"，没有提及告警类型的分类逻辑或输出路径。OVER_DURATION 在整个设计中没有对应实现。

改进建议：(1) 在 DosageAlert 中增加 warningType 字段（建议独立枚举 DoseWarningType：OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）；(2) 在 DosageThresholdService 的描述中明确剂量校验的三种输出路径及对应的 warningType 赋值规则；(3) 补充 OVER_DURATION 的实现说明——若暂不实现，应在设计决策中显式声明。

### 7. [一般] RecordGenerateRequest 缺少 dialogueText 的 50–10000 字符约束

需求文档 3.4.3 输入契约明确规定 dialogue_text 的字符数约束为 50–10000。§3.3 RecordGenerateRequest 的描述只列出了字段名称，未提及该字符数约束。

改进建议：在 §3.3 RecordGenerateRequest 的 dialogueText 字段后追加"(必填，字符数 50–10000)"约束说明。

### 8. [一般] Phase 5 迁移透明性断言缺少条件限定

§1.1 设计目标中声明"业务模块代码无须修改"。此断言成立的前提是 Phase 5 的 ai-api 层 DTO 不发生变化。但 §10 本身已说明 ai-api 层 DTO 会在本设计阶段扩展字段，§4.5 说明 Converter 依赖于 ai-api DTO 结构。如果 Phase 5 需要修改 ai-api DTO，则业务模块的 Converter 也需要同步修改。

改进建议：将"业务模块代码无须修改"修订为有条件的表述，例如："业务模块编译期依赖仅限 ai-api 的 AiService 接口；若 Phase 5 保持 AiService 接口签名和 DTO 字段结构不变，业务模块代码无须修改"。

## 历史迭代回顾

以上问题在迭代历史中的状态分析：

- **持续存在的问题（在多轮反馈中反复出现，需重点解决）**：
  - 问题 #1（CRITICAL→BLOCK 链路断裂）：源自第5轮 DosageAlertLevel 与 AuditRiskLevel 联动问题，历经第10轮持续未修复，属处方安全闭环核心缺陷
  - 问题 #2（submit 端点 Controller 归属）：从第7轮"处方提交端点设计边界"问题演化，第10轮再次检出
  - 问题 #3/#4（DTO 命名和字段定义缺失）：属反复出现的 DTO 契约完整性问题，第4轮已检出类似 DTO 字段缺失问题
  - 问题 #6（DosageAlert 缺少 warningType）：第10轮首次检出但在本版诊断中被确认仍有效，且涉及需求文档 3.4.10 字段级契约断裂

- **新发现的问题（本轮新识别）**：
  - 问题 #5（MedicalRecord 部分字段缺少显式 Java 字段名）：本版诊断新增的轻微问题

- **已解决的问题（出现在历史反馈但当前反馈中不再提及）**：
  - 第9轮的问题（辅助开方"医生确认后生效"流程、admin 管理接口契约、跨模块事件补偿策略、PrescriptionAssistResponse errorCode 字段、AllergyWarningItem.severity 类型、encounterId/visitId 映射、模板初始数据集）——当前诊断未再检出，但设计文本中是否已修复需核实

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v10_copy_from_v9.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md
