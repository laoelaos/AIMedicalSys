# 再审议判定报告（v4）

## 判定结果

RETRY

## 判定理由

组件B诊断报告经质询确认LOCATED，共识别5个问题：1个严重（sessionId跨模块传播路径架构未闭合）、4个一般（VisitFacade降级策略缺失、FieldMissingHint字段生成规则未定义、错误码遗漏、contentJson并发写丢失）。质询报告验证了各问题的证据充分性、逻辑完整性、覆盖完备性和分级合理性。根据判定标准，存在严重及一般等级问题，应判定RETRY重新运行组件A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：RegistrationEvent sessionId跨模块传播路径在架构层面未闭合——consultation与registration为独立模块（按§2.2依赖规则不允许互相依赖），产出未定义sessionId从DiaglogueSession传递到registration模块的具体路径
- **所在位置**：§1.1a外部依赖表（registration模块行）；§2.2"跨模块事件传递机制"段（line 293-294）；§2.2 RegistrationEvent字段描述
- **严重程度**：严重
- **改进建议**：明确定义sessionId跨模块传播机制，可选方案：(a)前端侧传递——分诊结束后前端保留sessionId并在挂号时作为请求参数传给registration模块；(b)后端侧查询——consultation模块暴露轻量级查询接口；(c)方案(a)与(b)结合。选定后同步更新§1.1a和§2.2。

- **问题描述**：VisitFacade调用失败无降级策略——DoctorFacade有完整降级保护，但VisitFacade（encounterId→visitId转换）未定义任何故障处理行为
- **所在位置**：§3.3 RecordGenerateRequest（line 593-595）；§1.1a外部依赖表（visit模块行）；§4.3病历生成场景行为契约
- **严重程度**：一般
- **改进建议**：为VisitFacade补充对称的降级保护：(a)定义超时阈值(如2s)；(b)定义失败行为——返回RX_MR_GEN_VISIT_NOT_FOUND错误码+病历内容部分返回，或encounterId作为visitId fallback；(c)同步更新§4.3和§5.1错误码表。

- **问题描述**：FieldMissingHint字段生成规则未定义——MissingFieldDetector仅定义缺失检测逻辑，但promptMessage和suggestedAction的内容来源和生成规则未被具体化
- **所在位置**：§1.3 FieldMissingHint条目（line 111-112）；§3.3 MissingFieldDetector职责描述；§3.3 MedicalRecordService描述
- **严重程度**：一般
- **改进建议**：定义FieldMissingHint字段生成策略，至少明确以下之一：(a)基于DepartmentTemplateConfig中每个MedicalRecordField的预定义提示模板；(b)或由AI在生成病历同时返回补全建议。采用(a)需同步补充提示内容加载/缓存机制并在§2.1中扩展DeptTemplateConfig。

- **问题描述**：部分错误码遗漏于§5.1错误码表——RX_ASSIST_UNIT_MISMATCH（§8.3）、TRIAGE_SESSION_NOT_FOUND（§3.1/§4.1）、RX_MR_GEN_VISIT_NOT_FOUND（若采纳问题2建议需新增）未进表
- **所在位置**：§5.1模块级错误码表（line 965-977）
- **严重程度**：一般
- **改进建议**：将上述遗漏错误码补充至§5.1错误码表中，归类到对应模块的合适分类行。

- **问题描述**：MedicalRecord.contentJson并发写更新丢失未处理——"单列JSON TEXT + 读取→合并→写回"模式在并发写入时后提交的写入会覆盖前一个变更
- **所在位置**：§3.3 MedicalRecordRepository描述（line 567）；§3.3 MedicalRecord实体（line 561-567）
- **严重程度**：一般
- **改进建议**：在MedicalRecord实体中增加@Version乐观锁字段，写冲突时返回并发错误码（如MR_GEN_CONCURRENT_MODIFICATION），由前端提示用户刷新后重试。同步更新§3.3和§7设计决策。
