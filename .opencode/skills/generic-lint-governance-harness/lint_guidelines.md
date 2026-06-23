# cjlint 处理指导原则

以下为处理原则而非必须遵守的规则，可以根据实际情况灵活处理。

## 1 抑制语法

### 行抑制

在目标行末尾添加注释：

```lang
func foo() { // cjlint-ignore !G.ERR.01
```

### 块抑制

用 `-start` / `-end` 包裹代码块：

```lang
// cjlint-ignore -start !G.FUN.01
func bar(a: Int64, b: Int64, c: Int64, d: Int64) {
    // ...
}
// cjlint-ignore -end !G.FUN.01
```

根据实际情况选择行抑制或块抑制。

---

## 2 各规则处理策略

### G.PKG.01 — 通配符导入

将 `import xxx.*` 替换为精确导入。操作步骤：

1. 注释掉通配符导入行
2. 执行 `cjpm build && cjpm test --no-run`
3. 根据编译报错逐个添加精确导入

特殊情况：

- **枚举**：使用裸构造器名（如 `Red` 而非 `Color.Red`）报错时，需导入枚举类型本身（`import pkg.Color`），根据文档定位类型所属包
- **接口扩展**：调用某类型方法报"找不到成员函数"时，该函数可能来自接口扩展，需同时导入类型和对应接口，根据文档定位扩展函数所属接口
- **自动导入**：`std.unittest.*` 和 `std.unittest.testmacro.*` 在测试模式下自动导入，直接删除即可

### G.NAM.02 — 文件名不匹配

将文件重命名为全小写下划线格式。例如 `pr_list.cj` → `pr_list_command.cj`。

### G.ERR.01 — 异常处理

对测试代码中的 throw 语句和空 catch 块添加行抑制：

```lang
// cjlint-ignore !G.ERR.01
```

### G.ERR.03 — 对 Option 使用 getOrThrow

使用 `if-let` 表达式、`match` 表达式、`??` 操作符等替代 `getOrThrow`。

### G.FUN.01 — 函数参数/行数超限

视情况处理：适合提取辅助函数则提取，不适合则抑制：

```lang
// cjlint-ignore !G.FUN.01
```

### G.FUN.02 — 未使用参数

视情况处理：

- 必要场景（如 API 声明）：忽略不处理
- 确属冗余参数：使用弃元 `_` 代替

### G.FUN.03 — 函数间重名重载

重命名函数以消除歧义。

### G.ITF.04 — 接口类型参数

添加抑制：

```lang
// cjlint-ignore !G.ITF.04
```

### G.VAR.02 — 变量作用域过大

按优先级依次尝试：

1. **内联**：将中间变量直接内联到调用处
2. **缩小作用域**：将变量声明移入 `if`/`for`/`try` 块内
3. **抑制**：以上方式均不适用时添加抑制 `// cjlint-ignore !G.VAR.02`

### G.OTH.02 — 硬编码敏感信息

添加抑制：

```lang
// cjlint-ignore G.OTH.02
```

### G.OTH.03 — 硬编码 URL

按优先级依次尝试：

1. **提取常量**：将 URL 提取为顶层常量（局限：常量声明行本身仍会被检测）
2. **抑制**：不适合提取时添加抑制 `// cjlint-ignore !G.OTH.03`

### 其他规则

简单且处理方案明确的问题直接修复。不确定的问题暂不处理。
