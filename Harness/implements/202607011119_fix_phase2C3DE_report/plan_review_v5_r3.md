# 计划审查报告（v5 r3）

## 审查结果
REJECTED

## 发现

### [一般] 缺少构造函数参数计数测试更新

`PrescriptionAssistServiceImplTest.java:861-865` 中存在如下测试：

```java
@Test
void constructorShouldAcceptNineParameters() throws Exception {
    var constructor = PrescriptionAssistServiceImpl.class.getConstructors();
    assertEquals(1, constructor.length);
    assertEquals(10, constructor[0].getParameterCount());
}
```

当前构造函数有 10 个参数（R4 加入 DrugFacade 后已更新为 10，但测试名称仍为 NineParameters）。P01 修复将为构造函数增加 `@Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor` 参数，使参数总数变为 **11**。该测试的 `assertEquals(10, ...)` 断言将因此失败。

计划未提及此测试文件的修改，属于遗漏。需在 R5「涉及文件 — PrescriptionAssistServiceImplTest」中补充：将 `assertEquals(10, ...)` 改为 `assertEquals(11, ...)`（同时可考虑重命名测试方法消除误导）。

## 修改要求

1. [一般] 在 R5 的「涉及文件 — PrescriptionAssistServiceImplTest」补充中，增加对 `constructorShouldAcceptNineParameters` 测试的修改说明：`assertEquals(10, ...)` → `assertEquals(11, ...)`。
