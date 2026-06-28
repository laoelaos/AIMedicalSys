-- =============================================
-- 智慧云脑诊疗平台 - 数据库 schema
-- MySQL / InnoDB / utf8mb4
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------
-- 1. sys_user
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`               VARCHAR(64)   NOT NULL                COMMENT '登录账号',
  `password`               VARCHAR(128)  NOT NULL                COMMENT '密码',
  `nickname`               VARCHAR(64)   DEFAULT NULL            COMMENT '昵称',
  `phone`                  VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
  `email`                  VARCHAR(128)  DEFAULT NULL            COMMENT '邮箱',
  `user_type`              VARCHAR(20)   NOT NULL                COMMENT '用户类型 ADMIN/DOCTOR/PATIENT',
  `enabled`                TINYINT(1)    DEFAULT 1               COMMENT '是否启用',
  `password_change_required` TINYINT(1)  DEFAULT 0               COMMENT '是否必须修改密码',
  `token_version`          INT           DEFAULT 0               COMMENT '令牌版本号',
  `remark`                 VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`                BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`             DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`             DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`                TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_username_user_type` (`username`, `user_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- ---------------------------------------------
-- 2. sys_role
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code`        VARCHAR(64)   NOT NULL                COMMENT '角色编码',
  `name`        VARCHAR(64)   DEFAULT NULL            COMMENT '角色名称',
  `description` VARCHAR(500)  DEFAULT NULL            COMMENT '角色描述',
  `enabled`     TINYINT(1)    DEFAULT 1               COMMENT '是否启用',
  `sort`        INT           DEFAULT 0               COMMENT '排序号',
  `remark`      VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`     BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`     TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统角色表';

-- ---------------------------------------------
-- 3. sys_post
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code`        VARCHAR(64)   NOT NULL                COMMENT '岗位编码',
  `name`        VARCHAR(64)   DEFAULT NULL            COMMENT '岗位名称',
  `description` VARCHAR(500)  DEFAULT NULL            COMMENT '岗位描述',
  `role_id`     BIGINT        DEFAULT NULL            COMMENT '关联角色ID',
  `enabled`     TINYINT(1)    DEFAULT 1               COMMENT '是否启用',
  `sort`        INT           DEFAULT 0               COMMENT '排序',
  `remark`      VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`     BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`     TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  CONSTRAINT `fk_sys_post_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统岗位表';

-- ---------------------------------------------
-- 4. sys_function
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_function`;
CREATE TABLE `sys_function` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`     BIGINT        DEFAULT NULL            COMMENT '父级ID',
  `code`          VARCHAR(128)  NOT NULL                COMMENT '功能编码',
  `name`          VARCHAR(64)   DEFAULT NULL            COMMENT '功能名称',
  `type`          VARCHAR(20)   DEFAULT 'MENU'          COMMENT '类型 DIRECTORY/MENU/BUTTON',
  `path`          VARCHAR(128)  DEFAULT NULL            COMMENT '路由路径',
  `component`     VARCHAR(255)  DEFAULT NULL            COMMENT '前端组件',
  `icon`          VARCHAR(64)   DEFAULT NULL            COMMENT '图标',
  `sort`          INT           DEFAULT 0               COMMENT '排序',
  `visible`       TINYINT(1)    DEFAULT 1               COMMENT '是否可见',
  `perms`         VARCHAR(128)  DEFAULT NULL            COMMENT '权限字符串',
  `query_method`  VARCHAR(10)   DEFAULT NULL            COMMENT '请求方法 GET/POST...',
  `description`   VARCHAR(500)  DEFAULT NULL            COMMENT '描述',
  `enabled`       TINYINT(1)    DEFAULT 1               COMMENT '是否启用',
  `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`       BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`    DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`    DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`       TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_code_type` (`code`, `type`),
  CONSTRAINT `fk_sys_function_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_function` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统功能/菜单表';

-- ---------------------------------------------
-- 5. sys_dict_type
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_name`  VARCHAR(100)  DEFAULT NULL            COMMENT '字典名称',
  `dict_type`  VARCHAR(100)  NOT NULL                COMMENT '字典类型',
  `status`     TINYINT(1)    DEFAULT 1               COMMENT '状态',
  `remark`     VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`    BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`    TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典类型表';

-- ---------------------------------------------
-- 6. sys_dict_data
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_sort`  INT           DEFAULT 0               COMMENT '字典排序',
  `dict_label` VARCHAR(100)  DEFAULT NULL            COMMENT '字典标签',
  `dict_value` VARCHAR(100)  DEFAULT NULL            COMMENT '字典键值',
  `dict_type`  VARCHAR(100)  DEFAULT NULL            COMMENT '字典类型',
  `css_class`  VARCHAR(100)  DEFAULT NULL            COMMENT '样式属性',
  `list_class` VARCHAR(100)  DEFAULT NULL            COMMENT '表格回显样式',
  `is_default` TINYINT(1)    DEFAULT 0               COMMENT '是否默认',
  `status`     TINYINT(1)    DEFAULT 1               COMMENT '状态',
  `remark`     VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`    BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`    TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`),
  CONSTRAINT `fk_sys_dict_data_type` FOREIGN KEY (`dict_type`) REFERENCES `sys_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典数据表';

-- ---------------------------------------------
-- 7. user_role
-- ---------------------------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户-角色关联表';

-- ---------------------------------------------
-- 8. user_post
-- ---------------------------------------------
DROP TABLE IF EXISTS `user_post`;
CREATE TABLE `user_post` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`),
  CONSTRAINT `fk_user_post_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_user_post_post` FOREIGN KEY (`post_id`) REFERENCES `sys_post` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户-岗位关联表';

-- ---------------------------------------------
-- 9. post_function
-- ---------------------------------------------
DROP TABLE IF EXISTS `post_function`;
CREATE TABLE `post_function` (
  `post_id`     BIGINT NOT NULL COMMENT '岗位ID',
  `function_id` BIGINT NOT NULL COMMENT '功能ID',
  PRIMARY KEY (`post_id`, `function_id`),
  CONSTRAINT `fk_post_function_post` FOREIGN KEY (`post_id`) REFERENCES `sys_post` (`id`),
  CONSTRAINT `fk_post_function_function` FOREIGN KEY (`function_id`) REFERENCES `sys_function` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='岗位-功能关联表';

-- ---------------------------------------------
-- 10. patient_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `patient_profile`;
CREATE TABLE `patient_profile` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`             BIGINT        DEFAULT NULL            COMMENT '关联用户ID',
  `real_name`           VARCHAR(64)   DEFAULT NULL            COMMENT '真实姓名',
  `gender`              VARCHAR(20)   DEFAULT NULL            COMMENT '性别',
  `birth_date`          DATE          DEFAULT NULL            COMMENT '出生日期',
  `age`                 INT           DEFAULT NULL            COMMENT '年龄',
  `id_card`             VARCHAR(32)   DEFAULT NULL            COMMENT '身份证号',
  `phone`               VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
  `emergency_contact`   VARCHAR(64)   DEFAULT NULL            COMMENT '紧急联系人',
  `emergency_phone`     VARCHAR(20)   DEFAULT NULL            COMMENT '紧急联系电话',
  `address`             VARCHAR(255)  DEFAULT NULL            COMMENT '地址',
  `avatar_url`          VARCHAR(500)  DEFAULT NULL            COMMENT '头像URL',
  `remark`              VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`             BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`          DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`          DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`             TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`),
  KEY `idx_phone` (`phone`),
  CONSTRAINT `fk_patient_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='患者档案表';

-- ---------------------------------------------
-- 11. doctor_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `doctor_profile`;
CREATE TABLE `doctor_profile` (
  `id`                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`            BIGINT         DEFAULT NULL            COMMENT '关联用户ID',
  `real_name`          VARCHAR(64)    DEFAULT NULL            COMMENT '真实姓名',
  `gender`             VARCHAR(20)    DEFAULT NULL            COMMENT '性别',
  `title`              VARCHAR(64)    DEFAULT NULL            COMMENT '职称',
  `department`         VARCHAR(64)    DEFAULT NULL            COMMENT '科室',
  `specialty`          VARCHAR(255)   DEFAULT NULL            COMMENT '擅长领域',
  `introduction`       TEXT           DEFAULT NULL            COMMENT '个人介绍',
  `license_no`         VARCHAR(64)    DEFAULT NULL            COMMENT '执业证书号',
  `practice_years`     INT            DEFAULT NULL            COMMENT '从业年限',
  `consultation_fee`   DECIMAL(10, 2) DEFAULT NULL            COMMENT '咨询费用',
  `remark`             VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `version`            BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`         DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`         DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`            TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_license_no` (`license_no`),
  KEY `idx_department` (`department`),
  CONSTRAINT `fk_doctor_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='医生档案表';

-- ---------------------------------------------
-- 12. admin_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `admin_profile`;
CREATE TABLE `admin_profile` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`    BIGINT        DEFAULT NULL            COMMENT '关联用户ID',
  `real_name`  VARCHAR(64)   DEFAULT NULL            COMMENT '真实姓名',
  `gender`     VARCHAR(20)   DEFAULT NULL            COMMENT '性别',
  `phone`      VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
  `department` VARCHAR(64)   DEFAULT NULL            COMMENT '部门',
  `remark`     VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`    BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`    TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  CONSTRAINT `fk_admin_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理员档案表';

-- ---------------------------------------------
-- 13. health_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `health_profile`;
CREATE TABLE `health_profile` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id`     BIGINT        DEFAULT NULL            COMMENT '患者档案ID',
  `blood_type`     VARCHAR(20)   DEFAULT NULL            COMMENT '血型',
  `height_cm`      DECIMAL(5, 1) DEFAULT NULL            COMMENT '身高(cm)',
  `weight_kg`      DECIMAL(5, 1) DEFAULT NULL            COMMENT '体重(kg)',
  `bmi`            DECIMAL(4, 1) DEFAULT NULL            COMMENT 'BMI指数',
  `marital_status` VARCHAR(32)   DEFAULT NULL            COMMENT '婚姻状况',
  `lifestyle_note` TEXT          DEFAULT NULL            COMMENT '生活方式备注',
  `version`        BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`     DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`     DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`        TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_id` (`patient_id`),
  CONSTRAINT `fk_health_profile_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='健康档案表';

-- ---------------------------------------------
-- 14. allergy_history
-- ---------------------------------------------
DROP TABLE IF EXISTS `allergy_history`;
CREATE TABLE `allergy_history` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `health_profile_id` BIGINT        DEFAULT NULL            COMMENT '健康档案ID',
  `allergen`          VARCHAR(255)  NOT NULL                COMMENT '过敏原',
  `reaction_type`     VARCHAR(255)  DEFAULT NULL            COMMENT '反应类型',
  `severity`          VARCHAR(20)   DEFAULT NULL            COMMENT '严重程度',
  `occurred_at`       DATE          DEFAULT NULL            COMMENT '发生时间',
  `note`              VARCHAR(500)  DEFAULT NULL            COMMENT '说明',
  `remark`            VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`           BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_health_profile_id` (`health_profile_id`),
  CONSTRAINT `fk_allergy_history_health` FOREIGN KEY (`health_profile_id`) REFERENCES `health_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='过敏史表';

-- ---------------------------------------------
-- 15. chronic_disease
-- ---------------------------------------------
DROP TABLE IF EXISTS `chronic_disease`;
CREATE TABLE `chronic_disease` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `health_profile_id` BIGINT        DEFAULT NULL            COMMENT '健康档案ID',
  `disease_name`      VARCHAR(255)  NOT NULL                COMMENT '疾病名称',
  `diagnosed_at`      DATE          DEFAULT NULL            COMMENT '确诊时间',
  `current_status`    VARCHAR(20)   DEFAULT NULL            COMMENT '当前状态',
  `remark`            VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`           BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_health_profile_id` (`health_profile_id`),
  CONSTRAINT `fk_chronic_disease_health` FOREIGN KEY (`health_profile_id`) REFERENCES `health_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='慢性疾病表';

-- ---------------------------------------------
-- 16. family_history
-- ---------------------------------------------
DROP TABLE IF EXISTS `family_history`;
CREATE TABLE `family_history` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `health_profile_id` BIGINT        DEFAULT NULL            COMMENT '健康档案ID',
  `relationship`      VARCHAR(64)   NOT NULL                COMMENT '亲属关系',
  `disease_name`      VARCHAR(255)  NOT NULL                COMMENT '疾病名称',
  `note`              VARCHAR(500)  DEFAULT NULL            COMMENT '说明',
  `remark`            VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`           BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_health_profile_id` (`health_profile_id`),
  CONSTRAINT `fk_family_history_health` FOREIGN KEY (`health_profile_id`) REFERENCES `health_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='家族病史表';

-- ---------------------------------------------
-- 17. surgery_history
-- ---------------------------------------------
DROP TABLE IF EXISTS `surgery_history`;
CREATE TABLE `surgery_history` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `health_profile_id` BIGINT        DEFAULT NULL            COMMENT '健康档案ID',
  `surgery_name`      VARCHAR(255)  NOT NULL                COMMENT '手术名称',
  `surgery_at`        DATE          DEFAULT NULL            COMMENT '手术时间',
  `hospital`          VARCHAR(255)  DEFAULT NULL            COMMENT '医院',
  `remark`            VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`           BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_health_profile_id` (`health_profile_id`),
  CONSTRAINT `fk_surgery_history_health` FOREIGN KEY (`health_profile_id`) REFERENCES `health_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='手术史表';

-- ---------------------------------------------
-- 18. medication_history
-- ---------------------------------------------
DROP TABLE IF EXISTS `medication_history`;
CREATE TABLE `medication_history` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `health_profile_id` BIGINT        DEFAULT NULL            COMMENT '健康档案ID',
  `drug_name`         VARCHAR(255)  NOT NULL                COMMENT '药品名称',
  `reason`            VARCHAR(500)  DEFAULT NULL            COMMENT '用药原因',
  `started_at`        DATE          DEFAULT NULL            COMMENT '开始时间',
  `ended_at`          DATE          DEFAULT NULL            COMMENT '结束时间',
  `remark`            VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `version`           BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_health_profile_id` (`health_profile_id`),
  CONSTRAINT `fk_medication_history_health` FOREIGN KEY (`health_profile_id`) REFERENCES `health_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用药史表';

-- ---------------------------------------------
-- 19. sys_operation_log
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`       BIGINT        DEFAULT NULL            COMMENT '用户ID',
  `username`      VARCHAR(64)   DEFAULT NULL            COMMENT '用户名',
  `operation`     VARCHAR(100)  DEFAULT NULL            COMMENT '操作内容',
  `method`        VARCHAR(200)  DEFAULT NULL            COMMENT '请求方法',
  `request_method` VARCHAR(10)  DEFAULT NULL            COMMENT '请求方式',
  `params`        TEXT          DEFAULT NULL            COMMENT '请求参数',
  `ip`            VARCHAR(64)   DEFAULT NULL            COMMENT 'IP地址',
  `location`      VARCHAR(255)  DEFAULT NULL            COMMENT '位置',
  `status`        TINYINT(1)    DEFAULT NULL            COMMENT '状态',
  `time`          DATETIME      DEFAULT NULL            COMMENT '操作时间',
  `cost_time_ms`  BIGINT        DEFAULT NULL            COMMENT '耗时(毫秒)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id_time` (`user_id`, `time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

-- ---------------------------------------------
-- 20. sys_login_log
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`    BIGINT        DEFAULT NULL            COMMENT '用户ID',
  `username`   VARCHAR(64)   DEFAULT NULL            COMMENT '用户名',
  `login_type` VARCHAR(20)   DEFAULT NULL            COMMENT '登录类型',
  `ip`         VARCHAR(64)   DEFAULT NULL            COMMENT 'IP地址',
  `location`   VARCHAR(255)  DEFAULT NULL            COMMENT '位置',
  `device`     VARCHAR(128)  DEFAULT NULL            COMMENT '设备',
  `browser`    VARCHAR(128)  DEFAULT NULL            COMMENT '浏览器',
  `os`         VARCHAR(128)  DEFAULT NULL            COMMENT '操作系统',
  `status`     TINYINT(1)    DEFAULT NULL            COMMENT '状态',
  `message`    VARCHAR(500)  DEFAULT NULL            COMMENT '消息',
  `created_at` DATETIME      DEFAULT NULL            COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id_created_at` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志表';

-- ---------------------------------------------
-- 21. sys_token
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_token`;
CREATE TABLE `sys_token` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`       BIGINT        DEFAULT NULL            COMMENT '用户ID',
  `token`         VARCHAR(768)  NOT NULL                COMMENT '令牌',
  `refresh_token` VARCHAR(768)  DEFAULT NULL            COMMENT '刷新令牌',
  `token_type`    VARCHAR(20)   DEFAULT NULL            COMMENT '令牌类型',
  `expires_at`    DATETIME      DEFAULT NULL            COMMENT '过期时间',
  `created_at`    DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`    DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`       TINYINT(1)    DEFAULT 0               COMMENT '逻辑删除',
  `version`       BIGINT        DEFAULT 0               COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id_expires_at` (`user_id`, `expires_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='令牌表';

-- ---------------------------------------------
-- 22. registration
-- ---------------------------------------------
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id`        BIGINT         DEFAULT NULL            COMMENT '患者档案ID',
  `doctor_id`         BIGINT         DEFAULT NULL            COMMENT '医生档案ID',
  `registration_type` VARCHAR(20)    NOT NULL                COMMENT '挂号类型 OUTPATIENT/EXAMINATION/EMERGENCY',
  `department`        VARCHAR(64)    DEFAULT NULL            COMMENT '科室',
  `scheduled_date`    DATE           DEFAULT NULL            COMMENT '预约日期',
  `scheduled_time_slot` VARCHAR(20)  DEFAULT NULL            COMMENT '时间段 HH:mm-HH:mm',
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/CONFIRMED/COMPLETED/CANCELLED/NO_SHOW',
  `cancel_reason`     VARCHAR(500)   DEFAULT NULL            COMMENT '取消原因',
  `cancel_time`       DATETIME       DEFAULT NULL            COMMENT '取消时间',
  `cancel_type`       VARCHAR(20)    DEFAULT NULL            COMMENT '取消方式 ONLINE/OFFLINE',
  `triage_level`      VARCHAR(20)    DEFAULT NULL            COMMENT '分诊级别 LEVEL_1/LEVEL_2/LEVEL_3/LEVEL_4',
  `chief_complaint`   VARCHAR(500)   DEFAULT NULL            COMMENT '主诉',
  `registration_fee`  DECIMAL(10, 2) DEFAULT NULL            COMMENT '挂号费',
  `queue_number`      INT            DEFAULT NULL            COMMENT '排队号',
  `version`           BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `remark`            VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `created_at`        DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_status` (`status`),
  KEY `idx_scheduled_date` (`scheduled_date`),
  UNIQUE KEY `uk_doctor_date_queue` (`doctor_id`, `scheduled_date`, `queue_number`),
  CONSTRAINT `fk_registration_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`),
  CONSTRAINT `fk_registration_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='挂号记录表';

-- ---------------------------------------------
-- 23. triage_record
-- ---------------------------------------------
DROP TABLE IF EXISTS `triage_record`;
CREATE TABLE `triage_record` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `registration_id`   BIGINT         DEFAULT NULL            COMMENT '挂号记录ID',
  `patient_id`        BIGINT         DEFAULT NULL            COMMENT '患者档案ID',
  `nurse_id`          BIGINT         DEFAULT NULL            COMMENT '分诊护士ID(admin_profile)',
  `symptoms`          TEXT           DEFAULT NULL            COMMENT '症状描述',
  `temperature`       DECIMAL(4, 1)  DEFAULT NULL            COMMENT '体温',
  `blood_pressure`    VARCHAR(20)    DEFAULT NULL            COMMENT '血压',
  `heart_rate`        INT            DEFAULT NULL            COMMENT '心率',
  `triage_department` VARCHAR(64)    DEFAULT NULL            COMMENT '分诊科室',
  `triage_level`      VARCHAR(20)    DEFAULT NULL            COMMENT '分诊级别 LEVEL_1/LEVEL_2/LEVEL_3/LEVEL_4',
  `triage_note`       VARCHAR(500)   DEFAULT NULL            COMMENT '分诊备注',
  `version`           BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`        DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_registration_id` (`registration_id`),
  KEY `idx_patient_id` (`patient_id`),
  CONSTRAINT `fk_triage_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`),
  CONSTRAINT `fk_triage_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`),
  CONSTRAINT `fk_triage_nurse` FOREIGN KEY (`nurse_id`) REFERENCES `admin_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='分诊记录表';

-- ---------------------------------------------
-- 24. medical_order
-- ---------------------------------------------
DROP TABLE IF EXISTS `medical_order`;
CREATE TABLE `medical_order` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id`      BIGINT         DEFAULT NULL            COMMENT '患者档案ID',
  `doctor_id`       BIGINT         DEFAULT NULL            COMMENT '医生档案ID',
  `registration_id` BIGINT         DEFAULT NULL            COMMENT '挂号记录ID',
  `order_no`        VARCHAR(32)    NOT NULL                COMMENT '医嘱编号',
  `order_type`      VARCHAR(20)    NOT NULL                COMMENT '医嘱类型 DRUG/EXAMINATION/LAB_TEST',
  `order_status`    VARCHAR(20)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/SUBMITTED/CHARGED/DISPENSED/COMPLETED/CANCELLED',
  `diagnosis`       TEXT           DEFAULT NULL            COMMENT '诊断',
  `total_amount`    DECIMAL(10, 2) DEFAULT NULL            COMMENT '总金额',
  `is_urgent`       TINYINT(1)     DEFAULT 0               COMMENT '是否紧急',
  `version`         BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `remark`          VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `created_at`      DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`      DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`         TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_status` (`order_status`),
  CONSTRAINT `fk_medical_order_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`),
  CONSTRAINT `fk_medical_order_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor_profile` (`id`),
  CONSTRAINT `fk_medical_order_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='医嘱主表';

-- ---------------------------------------------
-- 25. medical_order_item
-- ---------------------------------------------
DROP TABLE IF EXISTS `medical_order_item`;
CREATE TABLE `medical_order_item` (
  `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`    BIGINT         DEFAULT NULL            COMMENT '医嘱ID',
  `item_type`   VARCHAR(20)    NOT NULL                COMMENT '项目类型 DRUG/EXAMINATION/LAB_TEST',
  `item_code`   VARCHAR(64)    DEFAULT NULL            COMMENT '项目编码',
  `item_name`   VARCHAR(255)   NOT NULL                COMMENT '项目名称',
  `specification` VARCHAR(255) DEFAULT NULL            COMMENT '规格',
  `quantity`    DECIMAL(10, 2) DEFAULT NULL            COMMENT '数量',
  `unit`        VARCHAR(20)    DEFAULT NULL            COMMENT '单位',
  `unit_price`  DECIMAL(10, 2) DEFAULT NULL            COMMENT '单价',
  `amount`      DECIMAL(10, 2) DEFAULT NULL            COMMENT '金额',
  `dosage`      VARCHAR(100)   DEFAULT NULL            COMMENT '每次用量',
  `usage_method` VARCHAR(100)  DEFAULT NULL            COMMENT '用法',
  `frequency`   VARCHAR(50)    DEFAULT NULL            COMMENT '频次',
  `days`        INT            DEFAULT NULL            COMMENT '天数',
  `version`     BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `remark`      VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `created_at`  DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`  DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`     TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_item_code` (`item_code`),
  CONSTRAINT `fk_medical_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `medical_order` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='医嘱明细表';

-- ---------------------------------------------
-- 26. charge_pre_order
-- ---------------------------------------------
DROP TABLE IF EXISTS `charge_pre_order`;
CREATE TABLE `charge_pre_order` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`      BIGINT         DEFAULT NULL            COMMENT '医嘱ID',
  `patient_id`    BIGINT         DEFAULT NULL            COMMENT '患者档案ID',
  `charge_no`     VARCHAR(32)    NOT NULL                COMMENT '收费单号',
  `total_amount`  DECIMAL(10, 2) DEFAULT NULL            COMMENT '总金额',
  `charge_status` VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/CHARGED/REFUNDED',
  `version`       BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `remark`        VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `created_at`    DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`    DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`       TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_charge_no` (`charge_no`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_patient_id` (`patient_id`),
  CONSTRAINT `fk_charge_pre_order_order` FOREIGN KEY (`order_id`) REFERENCES `medical_order` (`id`),
  CONSTRAINT `fk_charge_pre_order_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='收费前置单';

-- ---------------------------------------------
-- 27. charge_pre_order_item
-- ---------------------------------------------
DROP TABLE IF EXISTS `charge_pre_order_item`;
CREATE TABLE `charge_pre_order_item` (
  `id`                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `charge_pre_order_id`  BIGINT         DEFAULT NULL            COMMENT '收费前置单ID',
  `order_item_id`        BIGINT         DEFAULT NULL            COMMENT '医嘱明细ID',
  `item_name`            VARCHAR(255)   DEFAULT NULL            COMMENT '项目名称',
  `quantity`             DECIMAL(10, 2) DEFAULT NULL            COMMENT '数量',
  `unit_price`           DECIMAL(10, 2) DEFAULT NULL            COMMENT '单价',
  `amount`               DECIMAL(10, 2) DEFAULT NULL            COMMENT '金额',
  `charge_item_type`     VARCHAR(20)    DEFAULT NULL            COMMENT '收费项目类型',
  `version`              BIGINT         DEFAULT 0               COMMENT '乐观锁版本号',
  `created_at`           DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`           DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`              TINYINT(1)     DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_charge_pre_order_id` (`charge_pre_order_id`),
  CONSTRAINT `fk_charge_item_pre_order` FOREIGN KEY (`charge_pre_order_id`) REFERENCES `charge_pre_order` (`id`),
  CONSTRAINT `fk_charge_item_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `medical_order_item` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='收费前置单明细';

SET FOREIGN_KEY_CHECKS = 1;
