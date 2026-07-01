# 实现报告（v6）

## 概述

在 common-module-api 中新增 6 个类型：AvailableDoctor（record DTO）、DoctorFacade（interface）、DrugInfo（record DTO）、DrugFacade（interface）、VisitFacade（interface）、RegistrationEvent（class）。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | com/aimedical/modules/commonmodule/doctor/AvailableDoctor.java | 可预约医生 DTO record |
| 新建 | com/aimedical/modules/commonmodule/doctor/DoctorFacade.java | 跨模块医生排班查询门面接口 |
| 新建 | com/aimedical/modules/commonmodule/drug/DrugInfo.java | 药品信息 DTO record |
| 新建 | com/aimedical/modules/commonmodule/drug/DrugFacade.java | 跨模块药品信息查询门面接口 |
| 新建 | com/aimedical/modules/commonmodule/visit/VisitFacade.java | 跨模块就诊标识查询门面接口 |
| 新建 | com/aimedical/modules/commonmodule/event/RegistrationEvent.java | 挂号事件契约 class |

## 编译验证

Maven compile 通过，无错误输出。

## 设计偏差说明

无偏差。
