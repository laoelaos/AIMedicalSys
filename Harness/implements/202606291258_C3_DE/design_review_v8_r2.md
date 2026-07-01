# 设计审查报告（v8 r2）

## 审查结果

REJECTED

## 发现

- **[一般]** DeadLetterEventRepository.findByStateAndRetryCountLessThan(String state, Integer retryCount) 签名与 task_v8 要求的 retryCount<maxRetryCount（实体字段级比较）不匹配。该派生查询生成 WHERE retryCount < ?（参数绑定），而非 WHERE retryCount < maxRetryCount（同实体另一字段）。当不同 DeadLetterEvent 实体拥有不同 maxRetryCount 值时，查询结果将不正确。应改为使用 @Query("... WHERE e.state = :state AND e.retryCount < e.maxRetryCount") 或等价的 JPQL。

## 修改要求（仅 REJECTED 时）

1. DeadLetterEventRepository — 将 findByStateAndRetryCountLessThan 替换为自定义 @Query 方法（如 findByCompensableEvents），语义为 state='FAILED' AND retryCount < maxRetryCount。同步更新 DeadLetterCompensationService 中对该方法的调用。
