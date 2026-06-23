# 编译 Warning 处理指导原则

以下为处理原则而非必须遵守的规则，可以根据实际情况灵活处理。

--

## 涉及到宏展开的情况

如果涉及到宏展开，需要根据 warning 信息以及生成的 macrocall 文件推断原始代码中发生 warning 的位置，再做对应修改。

---

## 当前处理范围

**仅处理以下列出的情况**，其他 warning 暂不处理。

---

## 1 Unused Import

直接删除未使用的 import 行。

## 2 Unused Variable

优先删除。不适合删除的情况使用弃元 `_` 。

## 3 Possibly Confusing Line Terminator

将引发异常的元素提到上一行，例如将

```
someValue
  |> someFunc
  |> someOtherFunc
```

变为：

```
someValue |>
  someFunc |>
  someOtherFunc
```

## 其他 Warning

暂不处理。
