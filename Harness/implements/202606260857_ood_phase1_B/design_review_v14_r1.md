# 设计审查报告（v14 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** 测试用例 #4 输入数据与预期结果矛盾

`PasswordPolicyImplTest` 用例 4 `validate_whenOnlyTwoCharTypes_shouldReturnWeak` 使用 password=`"aaaaaaA1"`，预期结果为 `PASSWORD_WEAK`。

但 `"aaaaaaA1"` 包含：
- 小写字母 `a`（`aaaaaa`）
- 大写字母 `A`（`A`）
- 数字 `1`（`1`）

共计 **3 种字符类型**。按设计文档中 `countCharTypes` 方法实现，`countCharTypes("aaaaaaA1")` 返回 3，因 `3 >= 3` 条件满足，方法会返回 `null`（合规），而非 `PASSWORD_WEAK`。

该测试用例必然失败，导致验证阶段出错。

**期望修正方向**：将 password 改为仅包含 2 种字符类型的值，例如 `"aaaaaaAA"`（仅小写+大写）或 `"aaaaaa11"`（仅小写+数字），使其匹配用例名称中的 "OnlyTwoCharTypes" 语义。

### **[一般]** 设计文档包含非规范的内部写作注记

第 323–359 行包含了大量流式自我对话（"Wait, 用例 4 的预期结果是...", "Hmm, but...", "Actually wait...", "Let me re-read..." 等），这些是编写过程中的思考过程，不属于正式设计规格说明的一部分。包含这些内容会：

- 导致规范文档含义模糊，编码人员难以区分最终决策与中间思考
- 与文档其余部分的正式风格严重不一致
- 包含不确定性表述（如 "I think this might be a typo"），在规范中不应存在

**期望修正方向**：删除第 323–359 行的全部写作注记，仅保留最终的正式测试用例描述。如需向编码人员传递注意事项，应以规范的方式写在"注意事项"或"设计说明"节中，而非以流式思考文本出现。

## 修改要求

1. ****[严重]** 修复 PasswordPolicyImplTest 用例 4 的 password 输入数据，确保其仅包含 2 种字符类型，使预期结果 `PASSWORD_WEAK` 可被正确触发。
2. **_[一般]_ 清理设计文档中的写作注记（第 323–359 行），仅保留正式的规格说明内容。**
