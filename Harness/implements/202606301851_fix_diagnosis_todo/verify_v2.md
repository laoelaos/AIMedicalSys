# 验证报告（v2）

## 结果
PASSED

## 统计
- 通过：122
- 失败：0

## 测试执行日志
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------------< com.aimedical:consultation >---------------------
[INFO] Building consultation 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ consultation ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\consultation\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ consultation ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ consultation ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ consultation ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.consultation.DialogueSessionManagerTest
19:27:19.638 [Thread-7] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-8] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-9] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-11] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-10] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-3] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-2] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.649 [Thread-5] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.650 [Thread-4] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:27:19.675 [main] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 550e8400-e29b-41d4-a716-446655440000, returning existing session
19:27:19.695 [Thread-16] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.695 [Thread-13] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.695 [Thread-19] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.695 [Thread-18] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.695 [Thread-14] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.697 [Thread-20] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.697 [Thread-17] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.697 [Thread-21] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:27:19.697 [Thread-15] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.404 s -- in com.aimedical.modules.consultation.DialogueSessionManagerTest
[INFO] Running com.aimedical.modules.consultation.TriageConverterTest
[ERROR] Tests run: 17, Failures: 0, Errors: 10, Skipped: 0, Time elapsed: 0.112 s <<< FAILURE! -- in com.aimedical.modules.consultation.TriageConverterTest
[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionIsNull -- Time elapsed: 0.016 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionIsNull(TriageConverterTest.java:123)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldReturnEmptyStringWhenItemsListIsEmpty -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldReturnEmptyStringWhenItemsListIsEmpty(TriageConverterTest.java:241)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionCcIsNull -- Time elapsed: 0.009 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionCcIsNull(TriageConverterTest.java:135)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldHandleNullQuestionOrAnswerInItems -- Time elapsed: 0.006 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldHandleNullQuestionOrAnswerInItems(TriageConverterTest.java:254)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldConcatenateItemsWithQAndAFormat -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldConcatenateItemsWithQAndAFormat(TriageConverterTest.java:198)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldTruncateWhenOver3000CharsAndAppendTruncatedMarker -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldTruncateWhenOver3000CharsAndAppendTruncatedMarker(TriageConverterTest.java:229)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldMergeAdditionalResponsesFromSessionAndRequest -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldMergeAdditionalResponsesFromSessionAndRequest(TriageConverterTest.java:59)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldNotTruncateWhenExactly3000Chars -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldNotTruncateWhenExactly3000Chars(TriageConverterTest.java:213)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldConvertToAiTriageRequest -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldConvertToAiTriageRequest(TriageConverterTest.java:36)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageConverterTest.shouldPassCorrectedChiefComplaintFromSessionToAiRequest -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.TriageConverterTest.shouldPassCorrectedChiefComplaintFromSessionToAiRequest(TriageConverterTest.java:113)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] Running com.aimedical.modules.consultation.TriageServiceImplTest
[ERROR] Tests run: 49, Failures: 1, Errors: 41, Skipped: 0, Time elapsed: 0.626 s <<< FAILURE! -- in com.aimedical.modules.consultation.TriageServiceImplTest
[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseTransactionTemplateForSaveTriageRecord -- Time elapsed: 0.454 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseTransactionTemplateForSaveTriageRecord(TriageServiceImplTest.java:429)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldPassWhenOnlyChiefComplaintPresent -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldPassWhenOnlyChiefComplaintPresent(TriageServiceImplTest.java:709)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseChineseFallbackHintAfterMaxFailures -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseChineseFallbackHintAfterMaxFailures(TriageServiceImplTest.java:807)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull(TriageServiceImplTest.java:473)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnDoctorsOnFallbackPath -- Time elapsed: 0.005 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnDoctorsOnFallbackPath(TriageServiceImplTest.java:663)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldFallbackToDefaultDepartmentsWhenRuleEngineReturnsEmpty -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldFallbackToDefaultDepartmentsWhenRuleEngineReturnsEmpty(TriageServiceImplTest.java:128)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetRuleVersionMismatchOnFallbackResponse -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetRuleVersionMismatchOnFallbackResponse(TriageServiceImplTest.java:677)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseChineseFallbackReason -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseChineseFallbackReason(TriageServiceImplTest.java:795)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseSessionRuleVersionInFallbackMatch -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldUseSessionRuleVersionInFallbackMatch(TriageServiceImplTest.java:783)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnEmptyWhenAllDepartmentsThrow -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnEmptyWhenAllDepartmentsThrow(TriageServiceImplTest.java:626)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldResetAiFailCountOnSuccessfulTriage -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldResetAiFailCountOnSuccessfulTriage(TriageServiceImplTest.java:242)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSaveAiRecommendedDepartmentsWhenNotDegraded -- Time elapsed: 0.004 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSaveAiRecommendedDepartmentsWhenNotDegraded(TriageServiceImplTest.java:456)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldWriteBackCorrectedChiefComplaintFromAiResultToSessionAndRecord -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldWriteBackCorrectedChiefComplaintFromAiResultToSessionAndRecord(TriageServiceImplTest.java:300)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotSetFallbackHintAfterTwoAiFailures -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotSetFallbackHintAfterTwoAiFailures(TriageServiceImplTest.java:156)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldLimitToFiveDoctorsAcrossDepartments -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldLimitToFiveDoctorsAcrossDepartments(TriageServiceImplTest.java:552)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSkipDepartmentOnDoctorFacadeException -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSkipDepartmentOnDoctorFacadeException(TriageServiceImplTest.java:604)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldPerformTriageWithAiSuccess -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldPerformTriageWithAiSuccess(TriageServiceImplTest.java:100)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldKeepScoreAsZeroForAllMappedDoctors -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldKeepScoreAsZeroForAllMappedDoctors(TriageServiceImplTest.java:649)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldFallbackToRuleEngineWhenAiFails -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldFallbackToRuleEngineWhenAiFails(TriageServiceImplTest.java:113)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldFallbackOnTimeout -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldFallbackOnTimeout(TriageServiceImplTest.java:232)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldUpdateExistingTriageRecordWhenRecordAlreadyExists -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldUpdateExistingTriageRecordWhenRecordAlreadyExists(TriageServiceImplTest.java:409)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldInsertNewTriageRecordWhenNoExistingRecord -- Time elapsed: 0.004 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldInsertNewTriageRecordWhenNoExistingRecord(TriageServiceImplTest.java:395)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldCopyRuleVersionFromRequestToSessionWhenSessionNull -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldCopyRuleVersionFromRequestToSessionWhenSessionNull(TriageServiceImplTest.java:744)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldIncrementFailCountOnInterruptedException -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldIncrementFailCountOnInterruptedException(TriageServiceImplTest.java:202)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldRollbackTransactionWhenExceptionOccursInSave -- Time elapsed: 0.005 s <<< FAILURE!
org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.RuntimeException> but was: <java.lang.NoSuchMethodError>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
	at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldRollbackTransactionWhenExceptionOccursInSave(TriageServiceImplTest.java:489)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.lambda$shouldRollbackTransactionWhenExceptionOccursInSave$1(TriageServiceImplTest.java:489)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
	... 6 more

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetCorrectedChiefComplaintFromRequestToSession -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetCorrectedChiefComplaintFromRequestToSession(TriageServiceImplTest.java:285)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnEmptyWhenDepartmentsIsNull -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnEmptyWhenDepartmentsIsNull(TriageServiceImplTest.java:519)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldPersistTriageRecordOnTriage -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldPersistTriageRecordOnTriage(TriageServiceImplTest.java:257)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldMapDoctorsFromSingleDepartment -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldMapDoctorsFromSingleDepartment(TriageServiceImplTest.java:501)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotSetCorrectedChiefComplaintOnRecordWhenSessionCcIsNull -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotSetCorrectedChiefComplaintOnRecordWhenSessionCcIsNull(TriageServiceImplTest.java:329)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnAllDoctorsWhenTotalIsLessThanFive -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnAllDoctorsWhenTotalIsLessThanFive(TriageServiceImplTest.java:590)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotOverwriteSessionRuleVersionWhenAlreadySet -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotOverwriteSessionRuleVersionWhenAlreadySet(TriageServiceImplTest.java:759)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest(TriageServiceImplTest.java:316)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures(TriageServiceImplTest.java:142)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotDoubleCountWhenMixedFailurePaths -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldNotDoubleCountWhenMixedFailurePaths(TriageServiceImplTest.java:214)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldUpdateExistingTriageRecordOnSecondCallWithSameSessionId -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldUpdateExistingTriageRecordOnSecondCallWithSameSessionId(TriageServiceImplTest.java:268)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSaveRuleMatchedDepartmentsWhenDegraded -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSaveRuleMatchedDepartmentsWhenDegraded(TriageServiceImplTest.java:441)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldIncrementFailCountOnExecutionException -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldIncrementFailCountOnExecutionException(TriageServiceImplTest.java:170)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldSortDoctorsBySlotCountDescending -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldSortDoctorsBySlotCountDescending(TriageServiceImplTest.java:572)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldPassWhenOnlyAdditionalResponsesPresent -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldPassWhenOnlyAdditionalResponsesPresent(TriageServiceImplTest.java:723)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldRequireThreeExecutionExceptionsForFallbackHint -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldRequireThreeExecutionExceptionsForFallbackHint(TriageServiceImplTest.java:183)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnEmptyWhenDepartmentsIsEmpty -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoSuchMethodError: 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
	at com.aimedical.modules.consultation.converter.TriageConverter.toAiTriageRequest(TriageConverter.java:61)
	at com.aimedical.modules.consultation.service.impl.TriageServiceImpl.triage(TriageServiceImpl.java:120)
	at com.aimedical.modules.consultation.TriageServiceImplTest.shouldReturnEmptyWhenDepartmentsIsEmpty(TriageServiceImplTest.java:532)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   TriageServiceImplTest.shouldRollbackTransactionWhenExceptionOccursInSave:489 Unexpected exception type thrown, expected: <java.lang.RuntimeException> but was: <java.lang.NoSuchMethodError>
[ERROR] Errors: 
[ERROR]   TriageConverterTest.shouldConcatenateItemsWithQAndAFormat:198 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldConvertToAiTriageRequest:36 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldHandleNullQuestionOrAnswerInItems:254 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldMergeAdditionalResponsesFromSessionAndRequest:59 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionCcIsNull:135 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldNotSetCorrectedChiefComplaintWhenSessionIsNull:123 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldNotTruncateWhenExactly3000Chars:213 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldPassCorrectedChiefComplaintFromSessionToAiRequest:113 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldReturnEmptyStringWhenItemsListIsEmpty:241 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageConverterTest.shouldTruncateWhenOver3000CharsAndAppendTruncatedMarker:229 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldCopyRuleVersionFromRequestToSessionWhenSessionNull:744 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldFallbackOnTimeout:232 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldFallbackToDefaultDepartmentsWhenRuleEngineReturnsEmpty:128 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldFallbackToRuleEngineWhenAiFails:113 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldIncrementFailCountOnExecutionException:170 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldIncrementFailCountOnInterruptedException:202 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldInsertNewTriageRecordWhenNoExistingRecord:395 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldKeepScoreAsZeroForAllMappedDoctors:649 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldLimitToFiveDoctorsAcrossDepartments:552 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldMapDoctorsFromSingleDepartment:501 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldNotDoubleCountWhenMixedFailurePaths:214 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldNotOverwriteSessionRuleVersionWhenAlreadySet:759 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldNotSetCorrectedChiefComplaintOnRecordWhenSessionCcIsNull:329 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull:473 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldNotSetFallbackHintAfterTwoAiFailures:156 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest:316 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldPassWhenOnlyAdditionalResponsesPresent:723 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldPassWhenOnlyChiefComplaintPresent:709 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldPerformTriageWithAiSuccess:100 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldPersistTriageRecordOnTriage:257 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldRequireThreeExecutionExceptionsForFallbackHint:183 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldResetAiFailCountOnSuccessfulTriage:242 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldReturnAllDoctorsWhenTotalIsLessThanFive:590 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldReturnDoctorsOnFallbackPath:663 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldReturnEmptyWhenAllDepartmentsThrow:626 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldReturnEmptyWhenDepartmentsIsEmpty:532 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldReturnEmptyWhenDepartmentsIsNull:519 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSaveAiRecommendedDepartmentsWhenNotDegraded:456 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSaveRuleMatchedDepartmentsWhenDegraded:441 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSetCorrectedChiefComplaintFromRequestToSession:285 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures:142 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSetRuleVersionMismatchOnFallbackResponse:677 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSkipDepartmentOnDoctorFacadeException:604 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldSortDoctorsBySlotCountDescending:572 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldUpdateExistingTriageRecordOnSecondCallWithSameSessionId:268 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldUpdateExistingTriageRecordWhenRecordAlreadyExists:409 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldUseChineseFallbackHintAfterMaxFailures:807 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldUseChineseFallbackReason:795 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldUseSessionRuleVersionInFallbackMatch:783 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldUseTransactionTemplateForSaveTriageRecord:429 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[ERROR]   TriageServiceImplTest.shouldWriteBackCorrectedChiefComplaintFromAiResultToSessionAndRecord:300 ? NoSuchMethod 'void com.aimedical.modules.ai.api.dto.triage.TriageRequest.setAdditionalResponsesText(java.lang.String)'
[INFO] 
[ERROR] Tests run: 84, Failures: 1, Errors: 51, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  5.326 s
[INFO] Finished at: 2026-06-30T19:27:20+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project consultation: There are test failures.
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ ai-api ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\ai\\ai-api\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-api ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.133 s -- in com.aimedical.modules.ai.api.dto.triage.TriageDtoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.945 s
[INFO] Finished at: 2026-06-30T19:27:31+08:00
[INFO] ------------------------------------------------------------------------
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ ai-api ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\ai\\ai-api\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ ai-api ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ ai-api ---
[INFO] Tests are skipped.
[INFO] 
[INFO] --- jar:3.3.0:jar (default-jar) @ ai-api ---
[INFO] Building jar: C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\target\ai-api-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- jacoco:0.8.12:check (jacoco-check) @ ai-api ---
[INFO] Loading execution data file C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\target\jacoco.exec
[INFO] Analyzed bundle 'ai-api' with 42 classes
[INFO] All coverage checks have been met.
[INFO] 
[INFO] --- install:3.1.1:install (default-install) @ ai-api ---
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\pom.xml to C:\Users\laoE\.m2\repository\com\aimedical\ai-api\0.0.1-SNAPSHOT\ai-api-0.0.1-SNAPSHOT.pom
[INFO] Installing C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\ai\ai-api\target\ai-api-0.0.1-SNAPSHOT.jar to C:\Users\laoE\.m2\repository\com\aimedical\ai-api\0.0.1-SNAPSHOT\ai-api-0.0.1-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.791 s
[INFO] Finished at: 2026-06-30T19:28:08+08:00
[INFO] ------------------------------------------------------------------------
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------------< com.aimedical:consultation >---------------------
[INFO] Building consultation 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- jacoco:0.8.12:prepare-agent (jacoco-prepare-agent) @ consultation ---
[INFO] argLine set to -javaagent:C:\\Users\\laoE\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.12\\org.jacoco.agent-0.8.12-runtime.jar=destfile=C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\backend\\modules\\consultation\\target\\jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ consultation ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ consultation ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\consultation\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ consultation ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ consultation ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.consultation.DialogueSessionManagerTest
19:28:19.924 [Thread-3] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.934 [Thread-11] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.934 [Thread-10] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.934 [Thread-9] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.934 [Thread-8] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.935 [Thread-7] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.935 [Thread-6] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.935 [Thread-5] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.935 [Thread-4] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 33333333-3333-4333-8333-333333333333, returning existing session
19:28:19.955 [main] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 550e8400-e29b-41d4-a716-446655440000, returning existing session
19:28:19.970 [Thread-14] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.970 [Thread-12] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.970 [Thread-19] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.971 [Thread-20] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.971 [Thread-18] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.971 [Thread-17] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.971 [Thread-15] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.971 [Thread-16] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
19:28:19.971 [Thread-21] WARN com.aimedical.modules.consultation.dialogue.DialogueSessionManager -- Session already exists for sessionId: 44444444-4444-4444-8444-444444444444, returning existing session
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.354 s -- in com.aimedical.modules.consultation.DialogueSessionManagerTest
[INFO] Running com.aimedical.modules.consultation.TriageConverterTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.066 s -- in com.aimedical.modules.consultation.TriageConverterTest
[INFO] Running com.aimedical.modules.consultation.TriageServiceImplTest
19:28:20.648 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.RuntimeException DoctorFacade error
19:28:20.648 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-02 after 0ms: java.lang.RuntimeException DoctorFacade error
19:28:20.675 [main] WARN com.aimedical.modules.consultation.service.impl.TriageServiceImpl -- DoctorFacade call failed for department dept-01 after 0ms: java.lang.RuntimeException DoctorFacade error
[INFO] Tests run: 49, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.733 s -- in com.aimedical.modules.consultation.TriageServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 84, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.834 s
[INFO] Finished at: 2026-06-30T19:28:25+08:00
[INFO] ------------------------------------------------------------------------
