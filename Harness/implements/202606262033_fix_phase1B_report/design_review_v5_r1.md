# 设计审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

### **[一般]** 文件规划表与详细代码变更自相矛盾

**问题**：`detail_v5.md` 的"文件规划"表格中，`MenuServiceImpl.java` 行描述为：
> `deleteMenu() 中 GlobalErrorCode.PARAM_INVALID → GlobalErrorCode.CHILDREN_EXIST；**移除冗余消息参数**`

但第 2 节详细代码变更（第 65-71 行）保留第二个消息参数不变：
```java
throw new BusinessException(GlobalErrorCode.CHILDREN_EXIST, "存在子菜单，无法删除，请先删除子菜单");
```

第 74-76 行进一步解释保留消息参数的理由（避免 API 响应变化），明确选择了保留。

**为什么是问题**：同一份设计文档中存在两处矛盾的表述——汇总表格说"移除"，详细设计说"保留"。编码实现者依据汇总表工作会导致错误（误删消息参数），依据详细代码则工作正确，但设计文档本身的一致性不满足交付标准。

**期望修正方向**：将文件规划表中 `MenuServiceImpl.java` 行的描述修正为仅描述实际变更（错误码枚举替换），去除"移除冗余消息参数"的描述，或将措辞改为"保留冗余消息参数以保持 API 响应不变"以使与详细设计一致。

## 修改要求（仅 REJECTED 时）

### 必须修正的问题

1. 文件规划表中 MenuServiceImpl.java 行的"移除冗余消息参数" → 修正为与实际详细代码一致的表述，消除内部矛盾。
