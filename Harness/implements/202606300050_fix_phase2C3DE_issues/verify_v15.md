# 验证报告（v15）

## 结果
PASSED

## 统计
- 全量构建：16 模块全部 BUILD SUCCESS
- common: 201/0/0/5（Tests/Failures/Errors/Skipped）✅
- common-module-api: 86/0/0/0 ✅
- common-module-impl: 400/0/0/1 ✅
- ai-api: 132/0/0/0 ✅
- ai-impl: 53/0/0/0 ✅
- patient: 46/0/0/0 ✅
- doctor: 14/0/0/0 ✅
- admin: 27/0/0/0 ✅
- consultation: 140/0/0/0 ✅
- prescription: 163/0/0/0 ✅（v15 新增 8 个 DrugFacade 注入测试全部通过）
- medical-record: 87/0/0/0 ✅
- application: 6/0/0/0 ✅
- 总计：1355 测试用例，全部 0 失败 0 错误

## 测试执行日志

### 1. prescription 模块测试（mvn test -pl modules/prescription）

```
[INFO] Tests run: 163, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

关键输出片段（DrugFacade 调用测试验证通过）：
```
11:51:50.098 [main] WARN ...PrescriptionAssistServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 2ms: RuntimeException
11:51:50.164 [main] WARN ...PrescriptionAssistServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 1ms: RuntimeException
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 -- PrescriptionAssistServiceImplTest
...
11:51:50.717 [main] WARN ...PrescriptionAuditServiceImpl -- DrugFacade.findByDrugCode(drug-001) failed after 1ms: RuntimeException
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0 -- PrescriptionAuditServiceImplTest
```

### 2. 全量构建测试（mvn test）

```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.190 s]
[INFO] common ............................................. SUCCESS [  9.543 s]
[INFO] Common Module Aggregator ........................... SUCCESS [  0.007 s]
[INFO] common-module-api .................................. SUCCESS [  1.201 s]
[INFO] common-module-impl ................................. SUCCESS [ 29.811 s]
[INFO] AI Module Aggregator ............................... SUCCESS [  0.004 s]
[INFO] ai-api ............................................. SUCCESS [  1.225 s]
[INFO] ai-impl ............................................ SUCCESS [  2.783 s]
[INFO] patient ............................................ SUCCESS [  1.324 s]
[INFO] doctor ............................................. SUCCESS [  1.168 s]
[INFO] admin .............................................. SUCCESS [  1.117 s]
[INFO] consultation ....................................... SUCCESS [  2.053 s]
[INFO] prescription ....................................... SUCCESS [  6.576 s]
[INFO] medical-record ..................................... SUCCESS [  2.331 s]
[INFO] application ........................................ SUCCESS [ 13.612 s]
[INFO] integration ........................................ SUCCESS [  0.127 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:13 min
[INFO] Finished at: 2026-06-30T11:53:11+08:00
[INFO] ------------------------------------------------------------------------
```

模块级测试统计：
- common: 201 tests, 0 failures, 0 errors, 5 skipped
- common-module-api: 86 tests, 0 failures, 0 errors, 0 skipped
- common-module-impl: 400 tests, 0 failures, 0 errors, 1 skipped
- ai-api: 132 tests, 0 failures, 0 errors, 0 skipped
- ai-impl: 53 tests, 0 failures, 0 errors, 0 skipped
- patient: 46 tests, 0 failures, 0 errors, 0 skipped
- doctor: 14 tests, 0 failures, 0 errors, 0 skipped
- admin: 27 tests, 0 failures, 0 errors, 0 skipped
- consultation: 140 tests, 0 failures, 0 errors, 0 skipped
- prescription: 163 tests, 0 failures, 0 errors, 0 skipped
- medical-record: 87 tests, 0 failures, 0 errors, 0 skipped
- application: 6 tests, 0 failures, 0 errors, 0 skipped
- 总计：1355 测试用例，全部 0 失败 0 错误

