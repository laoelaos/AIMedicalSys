# 设计审查报告（v17 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** LoggingSecurityAuditLogger 构造方式自相矛盾

- 文件规划表（detail_v17.md:13）写明："构造时注入 `@Value("${audit.security.file-path:#{null}}")`"
- 类设计段（detail_v17.md:88）却注明："构造：无参，依赖 logback-spring.xml 中配置的 appender"

两处信息直接冲突。若采用无参构造则无法注入可配置的文件路径；若采用构造器注入则 logback-spring.xml 的 appender 路径需与 `audit.security.file-path` 保持一致或依赖 Spring 属性动态解析，设计均未说明。实现时会因歧义产生错误。

**期望修正**：明确构造方式——要么统一为无参构造（路径完全由 logback-spring.xml 静态配置），要么统一为 `@Value` 构造注入（此时 logback-spring.xml 应使用 `<springProperty>` 引用同一属性，并在设计文档中说明联动关系）。

### **[一般]** 非 login 方法中 clientIp 来源未指定

- `SecurityAuditEvent.clientIp` 标记为 `@NotNull`，每个审计事件必须填充
- 行为契约中 `login` 方法在注意事项里明确说明 `clientIp` 从 `getClientIp()` 获取
- `logout()`、`refreshToken()`、`changePassword()` 三个方法的调用点表格及注意事项均未说明 `clientIp` 的来源

实现者无法确定在这些方法中如何获取客户端 IP（当前无 HTTP request 上下文传入 service 层）。

**期望修正**：为 logout/refreshToken/changePassword 分别说明 clientIp 的获取方式（例如：从 `RequestContextHolder` 提取、或从 controller 层传入、或从 security context 中已有的 authentication details 提取）。

### **[一般]** logback-spring.xml 内容未设计

- detail_v17.md:19 将 `logback-spring.xml` 列为新建文件，职责为"配置 SECURITY_AUDIT logger 的 RollingFileAppender"
- 但无任何小节描述该文件的具体内容：appender 名称、文件路径模板、rolling policy（基于时间/大小、最大历史等）、encoder pattern、与 `audit.security.file-path` 属性的对接方式

实现者缺乏准确依据创建该配置。

**期望修正**：补充 logback-spring.xml 的设计小节，至少包含 appender 名称、文件路径模式（是否引 audit.security.file-path）、rolling 策略的关键参数。

### **[轻微]** `SecurityAuditEventType.ACCOUNT_LOCKED` 在当前四种方法中均未使用

枚举定义了 `ACCOUNT_LOCKED`，但 login/logout/refreshToken/changePassword 四个方法的审计事件类型分别是 LOGIN_SUCCESS/FAILED、LOGOUT、TOKEN_REFRESH_SUCCESS/REJECTED、PASSWORD_CHANGED，无任何调用点使用 ACCOUNT_LOCKED。虽然不造成错误，但属于无用定义，可考虑移除或加注释说明预留用途。

### **[轻微]** `SecurityAuditEvent.now()` 工厂方法参数过多（8 个参数）

8 个参数的静态工厂方法可读性较差，调用处易混淆参数顺序。建议考虑 Builder 模式或拆分为更细粒度的工厂方法（如 `nowLoginSuccess(...)`、`nowLoginFailed(...)`），但当前方案功能正确，不影响实现。
