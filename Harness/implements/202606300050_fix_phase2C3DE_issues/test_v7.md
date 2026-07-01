# 测试报告（v7）

## 概述

根据详细设计 v7 的行为契约，为 DefaultTriageRuleEngine 补充 C13（快照回退）和 C16（关键词解析）相关测试，为 TriageServiceImpl 补充 ruleVersionMismatch 传播测试。

## 文件变更

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `DefaultTriageRuleEngineTest.java` | 增加 15 个新测试用例，覆盖 C13、C16 及排序契约 |
| 修改 | `TriageServiceImplTest.java` | 增加 `returnMismatch` 字段到 StubTriageRuleEngine；增加 1 个测试用例验证降级响应中 ruleVersionMismatch 传播 |

### DefaultTriageRuleEngineTest.java — 新增测试

| 测试方法 | 契约维度 |
|---------|---------|
| `shouldFallbackWhenVersionFilterEmpty` | C13: 版本过滤为空时回退，mismatch=true |
| `shouldNotFallbackWhenVersionMatches` | C13: 版本匹配时不回退 |
| `shouldFallbackWhenSetIdFilterEmpty` | C13: 规则集过滤为空时回退 |
| `shouldNotFallbackWhenBothVersionAndSetIdAreNull` | C13: 两者为 null 时不回退 |
| `shouldMatchWithAndLogicWhenAllKeywordsPresent` | C16: AND 全部命中 |
| `shouldNotMatchWithAndLogicWhenKeywordMissing` | C16: AND 部分未命中 |
| `shouldMatchWithOrLogicWhenAnyKeywordPresent` | C16: OR 任一命中 |
| `shouldNotMatchWithOrLogicWhenNoKeywordPresent` | C16: OR 全未命中 |
| `shouldDefaultToAndLogicWhenLogicFieldMissing` | C16: 默认 AND |
| `shouldMatchCaseInsensitively` | C16: 大小写不敏感 |
| `shouldPassRuleWhenConditionsNull` | 向后兼容: conditions 为 null |
| `shouldPassRuleWhenConditionsEmpty` | 向后兼容: conditions 为空字符串 |
| `shouldPassRuleWhenConditionsInvalidJson` | 向后兼容: JSON 解析失败 |
| `shouldSortMatchedRulesByScoreDescending` | 排序: score 降序 |

### TriageServiceImplTest.java — 新增测试

| 测试方法 | 契约维度 |
|---------|---------|
| `shouldSetRuleVersionMismatchOnFallbackResponse` | 降级路径将 MatchResult.ruleVersionMismatch 传播到 TriageResponse |

## 未覆盖说明

无。所有行为契约均已有正向/负向用例覆盖。
