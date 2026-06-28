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
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`   VARCHAR(64)   NOT NULL                COMMENT '登录账号',
  `password`   VARCHAR(128)  NOT NULL                COMMENT '密码',
  `nickname`   VARCHAR(64)   NOT NULL                COMMENT '昵称',
  `phone`      VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
  `email`      VARCHAR(128)  DEFAULT NULL            COMMENT '邮箱',
  `user_type`  VARCHAR(20)   NOT NULL                COMMENT '用户类型 ADMIN/DOCTOR/PATIENT',
  `enabled`    TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `password_change_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '需要修改密码',
  `token_version` INT           NOT NULL DEFAULT 0      COMMENT '令牌版本号',
  `remark`     VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`    TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
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
  `enabled`     TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `sort`          INT           NOT NULL DEFAULT 0      COMMENT '排序',
  `remark`      VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`     TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_code` (`code`)
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
  `enabled`     TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `sort`        INT           DEFAULT 0               COMMENT '排序',
  `remark`      VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`     TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_code` (`code`),
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
  `visible`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否可见',
  `perms`         VARCHAR(128)  DEFAULT NULL            COMMENT '权限字符串',
  `query_method`  VARCHAR(10)   DEFAULT NULL            COMMENT '请求方法 GET/POST...',
  `description`   VARCHAR(500)  DEFAULT NULL            COMMENT '描述',
  `enabled`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`    DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`    DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`       TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`    TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`    TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`          DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`          DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`             TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
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
  `created_at`         DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `updated_at`         DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `deleted`            TINYINT(1)     NOT NULL DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
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
  `created_at` DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`    TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`     DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`     DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`        TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `created_at`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`           TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
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
  `refresh_token` VARCHAR(2048) DEFAULT NULL            COMMENT '刷新令牌',
  `token_type`    VARCHAR(20)   DEFAULT NULL            COMMENT '令牌类型',
  `expires_at`    DATETIME      DEFAULT NULL            COMMENT '过期时间',
  `created_at`    DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`    DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`       TINYINT(1)    NOT NULL DEFAULT 0               COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id_expires_at` (`user_id`, `expires_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='令牌表';

-- ---------------------------------------------
-- 22. consultation_queue  接诊/叫号队列
-- ---------------------------------------------
DROP TABLE IF EXISTS `consultation_queue`;
CREATE TABLE `consultation_queue` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id`     BIGINT       NOT NULL                COMMENT '患者档案ID',
  `patient_name`   VARCHAR(64)  NOT NULL                COMMENT '患者姓名（冗余展示）',
  `doctor_id`      BIGINT       DEFAULT NULL            COMMENT '接诊医生用户ID',
  `department`     VARCHAR(64)  DEFAULT NULL            COMMENT '科室',
  `queue_no`       VARCHAR(32)  NOT NULL                COMMENT '排队号',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/CALLED/IN_CONSULTATION/FINISHED/SKIPPED',
  `registered_at`  DATETIME     DEFAULT NULL            COMMENT '挂号时间',
  `called_at`     DATETIME     DEFAULT NULL            COMMENT '叫号时间',
  `finished_at`    DATETIME     DEFAULT NULL            COMMENT '完成时间',
  `remark`         VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `created_at`     DATETIME     DEFAULT NULL            COMMENT '创建时间',
  `updated_at`     DATETIME     DEFAULT NULL            COMMENT '更新时间',
  `deleted`        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_doctor_status` (`doctor_id`, `status`),
  KEY `idx_patient_id` (`patient_id`),
  CONSTRAINT `fk_consultation_queue_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='接诊/叫号队列';

-- ---------------------------------------------
-- 23. medical_record_template  病历模板（按科室）
-- ---------------------------------------------
DROP TABLE IF EXISTS `medical_record_template`;
CREATE TABLE `medical_record_template` (
  `id`                       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `department`               VARCHAR(64)   NOT NULL                COMMENT '所属科室',
  `name`                     VARCHAR(128)  NOT NULL                COMMENT '模板名称',
  `chief_complaint_tpl`      TEXT          DEFAULT NULL            COMMENT '主诉模板',
  `present_illness_tpl`      TEXT          DEFAULT NULL            COMMENT '现病史模板',
  `past_history_tpl`         TEXT          DEFAULT NULL            COMMENT '既往史模板',
  `diagnosis_tpl`            TEXT          DEFAULT NULL            COMMENT '诊断模板',
  `treatment_plan_tpl`       TEXT          DEFAULT NULL            COMMENT '治疗方案模板',
  `enabled`                  TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `remark`                   VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`               DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`               DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`                  TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_department_enabled` (`department`, `enabled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='病历模板表';

-- ---------------------------------------------
-- 24. medical_record  病历（含版本管理）
-- ---------------------------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id`         BIGINT        NOT NULL                COMMENT '患者档案ID',
  `doctor_id`          BIGINT        NOT NULL                COMMENT '医生用户ID',
  `department`         VARCHAR(64)   DEFAULT NULL            COMMENT '科室',
  `version_no`         INT           NOT NULL DEFAULT 1      COMMENT '版本号',
  `status`             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/OFFICIAL',
  `chief_complaint`    TEXT          DEFAULT NULL            COMMENT '主诉',
  `present_illness`   TEXT          DEFAULT NULL            COMMENT '现病史',
  `past_history`       TEXT          DEFAULT NULL            COMMENT '既往史',
  `diagnosis`          TEXT          DEFAULT NULL            COMMENT '诊断',
  `treatment_plan`     TEXT          DEFAULT NULL            COMMENT '治疗方案',
  `prescription_id`    BIGINT        DEFAULT NULL            COMMENT '关联处方ID',
  `template_id`        BIGINT        DEFAULT NULL            COMMENT '使用的模板ID',
  `ai_generated`       TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否AI生成',
  `remark`             VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`         DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`         DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`            TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_patient_status` (`patient_id`, `status`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='病历表';

-- ---------------------------------------------
-- 25. prescription  处方
-- ---------------------------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id`     BIGINT        NOT NULL                COMMENT '患者档案ID',
  `patient_name`   VARCHAR(64)   NOT NULL                COMMENT '患者姓名（冗余展示）',
  `doctor_id`      BIGINT        NOT NULL                COMMENT '开方医生用户ID',
  `department`     VARCHAR(64)   DEFAULT NULL            COMMENT '科室',
  `status`         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING_REVIEW/APPROVED/REJECTED',
  `diagnosis`      VARCHAR(500)  DEFAULT NULL            COMMENT '诊断',
  `ai_checked`     TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否经AI审核',
  `ai_risk_level`  VARCHAR(20)   DEFAULT NULL            COMMENT 'AI风险等级 LOW/MEDIUM/HIGH',
  `audit_remark`   VARCHAR(500)  DEFAULT NULL            COMMENT '审核备注',
  `audited_by`     BIGINT        DEFAULT NULL            COMMENT '审核人用户ID',
  `audited_at`     DATETIME      DEFAULT NULL            COMMENT '审核时间',
  `remark`         VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`     DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`     DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`        TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_patient_status` (`patient_id`, `status`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处方表';

-- ---------------------------------------------
-- 26. prescription_item  处方明细
-- ---------------------------------------------
DROP TABLE IF EXISTS `prescription_item`;
CREATE TABLE `prescription_item` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prescription_id` BIGINT        NOT NULL                COMMENT '处方ID',
  `drug_name`       VARCHAR(128)  NOT NULL                COMMENT '药品名称',
  `specification`   VARCHAR(128)  DEFAULT NULL            COMMENT '规格',
  `dosage`          VARCHAR(64)   DEFAULT NULL            COMMENT '剂量',
  `usage_method`    VARCHAR(128)  DEFAULT NULL            COMMENT '用法',
  `frequency`       VARCHAR(64)   DEFAULT NULL            COMMENT '频次',
  `quantity`        DECIMAL(10,2) DEFAULT NULL            COMMENT '数量',
  `unit`            VARCHAR(32)   DEFAULT NULL            COMMENT '单位',
  `remark`          VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`      DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `updated_at`      DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `deleted`         TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`),
  CONSTRAINT `fk_prescription_item_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处方明细表';

SET FOREIGN_KEY_CHECKS = 1;
