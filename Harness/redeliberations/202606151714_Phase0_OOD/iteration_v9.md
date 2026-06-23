# 再审议判定报告（v9）

## 判定结果

RETRY

## 判定理由

组件B诊断报告在第9轮审查中发现9个问题：1个严重（问题6：跨模块门面接口返回JPA实体违反自身设计原则）、4个一般（问题1：CI非标准Maven属性、问题2：过时事件模式、问题5：分页参数缺少最大size约束、问题4/7/8/9为轻微）。质询报告确认全部问题为LOCATED，审查结论成立。内部循环实际轮次（2）未达到最大轮次（12），质询确认后提前终止。根据判定标准，诊断报告包含严重及一般等级问题，判定为RETRY，需要重新运行组件A进行修复。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：跨模块门面接口返回JPA实体违反自身设计原则（8.1节明确要求模块内部类不对外暴露），PermissionService.getUserById()直接返回User实体，引入LazyInitializationException、编译期耦合、序列化循环引用风险
- **所在位置**：8.4节PermissionService门面示例（第804-806行）
- **严重程度**：严重
- **改进建议**：将门面接口返回类型从User改为UserDTO，在common-module的dto子包中定义UserDTO（仅含必要字段，不含JPA注解和懒加载关联）

- **问题描述**：CI流水线第四阶段使用非标准Maven属性-Dskip.unit.tests=true，如POM中未为Surefire插件配置对应的<skip>属性，该命令无法直接使用
- **所在位置**：第10节CI占位，第四阶段命令
- **严重程度**：一般
- **改进建议**：在integration/pom.xml的surefire插件配置中添加<skip>${skip.unit.tests}</skip>并在properties中定义默认值，或改用标准属性组合

- **问题描述**：UserRegisteredEvent extends ApplicationEvent使用过时模式，Spring Framework 4.2+（项目使用Spring Boot 3对应Spring Framework 6）已支持任意POJO作为事件对象发布
- **所在位置**：8.4节事件驱动模式示例（第822-825行）
- **严重程度**：一般
- **改进建议**：去掉extends ApplicationEvent，改为普通POJO类，ApplicationEventPublisher.publishEvent(Object)可发布任意POJO事件

- **问题描述**：分页参数size字段缺少最大值上限约束，恶意或错误的请求可传入极大size值导致后端内存压力甚至OOM
- **所在位置**：3.1节PageQuery字段描述
- **严重程度**：一般
- **改进建议**：在PageQuery的size字段上补充@Max约束（建议上限100-500），将@Valid注解添加到所有分页Controller接口参数上

- **问题描述**：ScheduleRequest.dateRange类型为String且无格式约束，不同开发者可能采用不同格式导致前后端接口不兼容
- **所在位置**：8.2节AI能力DTO定义（第753行）
- **严重程度**：轻微
- **改进建议**：拆分为两个LocalDate字段startDate/endDate，或在Javadoc中明确String格式的精确约定

- **问题描述**：核心配置ai.mock.enabled未在配置文件示例中显式声明，新开发者需阅读MockAiService注解才能发现此属性
- **所在位置**：3.4节Bean装配策略、9.1节应用配置示例
- **严重程度**：轻微
- **改进建议**：在9.1节application-dev.yml示例末尾添加一行ai.mock.enabled: true

- **问题描述**：Mock数据占位约定未处理可选/可空字段，所有字段均填充占位值无法测试字段缺失场景
- **所在位置**：3.4节Mock数据占位约定
- **严重程度**：轻微
- **改进建议**：补充可选字段约定：对@Nullable标注或Javadoc标记为optional的字段，Mock应返回null

- **问题描述**：H2 Console启用但未说明生产环境关闭策略，生产环境jar包仍可能通过spring.h2.console.enabled访问控制台
- **所在位置**：9.1节H2配置
- **严重程度**：轻微
- **改进建议**：补充说明H2 Console仅在dev profile中启用，application-prod.yml应设置spring.h2.console.enabled: false；明确Phase 1+切换MySQL时将h2依赖scope从runtime调整为test

- **问题描述**：前端ApiClient未定义网络错误处理路径（DNS解析失败、连接超时等），Axios错误场景若未在拦截器中捕获，前端将面临未捕获的Promise异常
- **所在位置**：3.5节ApiClient描述
- **严重程度**：轻微
- **改进建议**：在3.5节补充Axios错误拦截器约定，网络错误时返回统一格式提示或走全局错误弹窗占位逻辑
