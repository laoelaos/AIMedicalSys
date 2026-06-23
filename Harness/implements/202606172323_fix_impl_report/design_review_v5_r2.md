# 设计审查报告（v5 r2）

## 审查结果
REJECTED

## 发现

- **[一般]** `isBusinessError?: true` 标记字段在 `BusinessError` 类型定义中已按前轮审查要求添加，但 success interceptor 的返回值代码（`detail_v5.md:72`）中并未实际设置该属性：
  ```typescript
  // 当前：return (Promise.resolve({ code: body.code, message: body.message ?? '' }) as BusinessError) as unknown
  // 缺少 isBusinessError 属性
  ```
  设计文档声称（第34行、第180行）"消费者可通过 `'isBusinessError' in result` 在运行时区分返回值为成功数据还是 `BusinessError`"，但由于属性未设置在运行时对象上，`'isBusinessError' in result` 将始终返回 `false`，消费者无法按设计推荐的运行时区分方式进行判断。对应测试用例（第140-143行）也未对 `isBusinessError` 做断言，进一步暴露了实现与类型定义的不一致。

## 修改要求

1. success interceptor 的 non-SUCCESS 返回值应实际包含 `isBusinessError: true as const` 属性，例如：
   ```typescript
   return (Promise.resolve({ code: body.code, message: body.message ?? '', isBusinessError: true as const }) as BusinessError) as unknown
   ```
   这样 `'isBusinessError' in result` 运行时才能返回 `true`，类型收窄到 `BusinessError` 的分支才可正常命中。

2. 同步更新对应测试断言（第139-143行、第146-149行），增加 `isBusinessError: true` 的 `toEqual` 预期。

3. （可选）考虑 error 拦截器的返回值是否也应包含 `isBusinessError: true`，以便消费者用统一方式处理所有业务/网络错误。
