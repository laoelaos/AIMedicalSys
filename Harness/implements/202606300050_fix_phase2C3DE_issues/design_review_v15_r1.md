# 设计审查报告（v15 r1）

## 审查结果
APPROVED

## 发现

- **[轻微]** 设计列出 `DrugInfo` 为两个 Service 的新增导入。设计正文说明"返回值（DrugInfo）当前不使用"，若调用方式为 `drugFacade.findByDrugCode(drugCode)`（不捕获返回值），则 `DrugInfo` 的 import 并非必需。不构成编译错误，属于可优化项，不影响正确性。

- **[轻微]** `drugFacadeTimeout` 字段注入后在当前设计中无实际消费路径（`DrugFacade.findByDrugCode()` 为同步调用，无 Future 机制应用超时）。但此字段的注入与任务要求一致（"用于 future.get 或预留扩展"），属于前瞻性预留，非设计缺陷。

## 修改要求
无
