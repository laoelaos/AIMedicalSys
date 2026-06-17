# 设计审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

### **[一般] — 未指定拦截器返回值保留字面量类型的约束**

**问题**：设计定义了 `ApiError` 类型的 `code` 为字面量联合类型（`'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'`），并规定错误拦截器通过 `Promise.resolve()` 返回 `ApiError` 类型对象。但设计未要求返回对象在 TypeScript 层面保留 code 字段的字面量类型。

**为什么是问题**：当实现为 `Promise.resolve({ code: 'NETWORK_ERROR', message: '...' })` 时，TypeScript 默认将 `code: 'NETWORK_ERROR'` 拓宽为 `code: string`。拓宽后的类型与 `ApiError` 的字面量联合不兼容——`string` 不可赋值给 `'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'`。若在拦截器函数上标注返回类型为 `Promise<ApiError>`（或显式约束返回值），TypeScript 将报编译错误。即使跳过编译错误（Axios 拦截器签名为 `any`），消费者通过 `if (res.code === 'SUCCESS')` 判别式也无法获得类型收窄——`res.code` 的类型为 `string` 而非字面量联合，discriminated union 的类型安全价值完全丧失。

**期望的修正方向**：在设计中增加约束条件——拦截器返回对象必须保留 `code` 字段的字面量类型。具体可指定实现方式：
- 选项A：在返回对象上使用 `as const` 断言（`{ code: 'NETWORK_ERROR' as const, message: '...' }`）
- 选项B：在错误拦截器函数上标注显式返回类型 `Promise<ApiError>`，利用 TypeScript 对象字面量的上下文类型推断保留字面量

### **[一般] — 未定义消费者如何从 Axios 泛型获取 ApiResponse<T> 类型**

**问题**：设计规定成功拦截器统一返回 `response.data`（类型为 `ApiResponse<T>`），但未说明消费者在 TypeScript 层面如何获得 `ApiResponse<T>` 类型的返回结果。

**为什么是问题**：当消费者调用 `apiClient.get<SomeType>('/url')` 时，Axios 类型系统将返回值类型解析为 `Promise<SomeType>`（`SomeType` 是 Axios 请求的泛型参数），而非 `Promise<ApiResponse<SomeType>>`。即使拦截器在运行时正确返回了 `ApiResponse<T>` 形状的对象，TypeScript 仍将消费者通过 `await` 获得的值视为 `SomeType`。若 `SomeType` 不含 `code` 属性，消费者直接访问 `res.code` 将产生 TS 编译错误；若消费者用 `as ApiResponse<SomeType>` 强转，则失去了拦截器类型推断的价值。核心设计目标——"消费方通过 `if (res.code === 'SUCCESS')` 判别式自动缩小类型获取 data 字段"——在类型层面无法自动达成。

**期望的修正方向**：在设计中补充消费者使用指南或类型干涉策略，例如：
- 选项A：声明消费者应使用 `apiClient.get<ApiResponse<SomeType>>('/url')` 模式发起调用
- 选项B：在 Axios 实例或拦截器上通过 TypeScript 函数重载/泛型约束覆盖默认返回类型，使 `apiClient.get<T>()` 自动返回 `Promise<ApiResponse<T>>`
- 选项C：定义包装函数（如 `apiClient.getTyped<T>()`）封装类型转换逻辑，并在设计中明确说明推荐的消费者调用方式
