-- =============================================
-- 智慧云脑诊疗平台 - 数据库 schema (H2 开发环境)
-- =============================================

DROP ALL OBJECTS;
SET REFERENTIAL_INTEGRITY FALSE;

-- ---------------------------------------------
-- 1. sys_user
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(64)   NOT NULL,
  `password`   VARCHAR(128)  NOT NULL,
  `nickname`   VARCHAR(64)   NOT NULL,
  `phone`      VARCHAR(20)   DEFAULT NULL,
  `email`      VARCHAR(128)  DEFAULT NULL,
  `gender`     VARCHAR(10)   DEFAULT NULL,
  `age`        INT           DEFAULT NULL,
  `user_type`  VARCHAR(20)   NOT NULL,
  `enabled`    BOOLEAN       NOT NULL DEFAULT TRUE,
  `password_change_required` BOOLEAN NOT NULL DEFAULT FALSE,
  `token_version` INT           NOT NULL DEFAULT 0,
  `remark`     VARCHAR(500)  DEFAULT NULL,
  `created_at` TIMESTAMP     DEFAULT NULL,
  `updated_at` TIMESTAMP     DEFAULT NULL,
  `deleted`    BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_username_user_type` (`username`, `user_type`)
);

-- ---------------------------------------------
-- 2. sys_role
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(64)   NOT NULL,
  `name`        VARCHAR(64)   DEFAULT NULL,
  `description` VARCHAR(500)  DEFAULT NULL,
  `enabled`     BOOLEAN       DEFAULT TRUE,
  `sort`        INT           DEFAULT 0,
  `remark`      VARCHAR(500)  DEFAULT NULL,
  `version`     BIGINT        DEFAULT 0,
  `created_at`  TIMESTAMP     DEFAULT NULL,
  `updated_at`  TIMESTAMP     DEFAULT NULL,
  `deleted`     BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
);

-- ---------------------------------------------
-- 3. sys_post
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(64)   NOT NULL,
  `name`        VARCHAR(64)   DEFAULT NULL,
  `description` VARCHAR(500)  DEFAULT NULL,
  `role_id`     BIGINT        DEFAULT NULL,
  `enabled`     BOOLEAN       DEFAULT TRUE,
  `sort`        INT           DEFAULT 0,
  `remark`      VARCHAR(500)  DEFAULT NULL,
  `version`     BIGINT        DEFAULT 0,
  `created_at`  TIMESTAMP     DEFAULT NULL,
  `updated_at`  TIMESTAMP     DEFAULT NULL,
  `deleted`     BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  CONSTRAINT `fk_sys_post_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
);

-- ---------------------------------------------
-- 4. sys_function
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_function`;
CREATE TABLE `sys_function` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `parent_id`     BIGINT        DEFAULT NULL,
  `code`          VARCHAR(128)  NOT NULL,
  `name`          VARCHAR(64)   DEFAULT NULL,
  `type`          VARCHAR(20)   DEFAULT 'MENU',
  `path`          VARCHAR(128)  DEFAULT NULL,
  `component`     VARCHAR(255)  DEFAULT NULL,
  `icon`          VARCHAR(64)   DEFAULT NULL,
  `sort`          INT           DEFAULT 0,
  `visible`       BOOLEAN       DEFAULT TRUE,
  `perms`         VARCHAR(128)  DEFAULT NULL,
  `query_method`  VARCHAR(10)   DEFAULT NULL,
  `description`   VARCHAR(500)  DEFAULT NULL,
  `enabled`       BOOLEAN       DEFAULT TRUE,
  `remark`        VARCHAR(500)  DEFAULT NULL,
  `version`       BIGINT        DEFAULT 0,
  `created_at`    TIMESTAMP     DEFAULT NULL,
  `updated_at`    TIMESTAMP     DEFAULT NULL,
  `deleted`       BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_code_type` (`code`, `type`),
  CONSTRAINT `fk_sys_function_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_function` (`id`)
);

-- ---------------------------------------------
-- 5. sys_dict_type
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `dict_name`  VARCHAR(100)  DEFAULT NULL,
  `dict_type`  VARCHAR(100)  NOT NULL,
  `status`     BOOLEAN       DEFAULT TRUE,
  `remark`     VARCHAR(500)  DEFAULT NULL,
  `version`    BIGINT        DEFAULT 0,
  `created_at` TIMESTAMP     DEFAULT NULL,
  `updated_at` TIMESTAMP     DEFAULT NULL,
  `deleted`    BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
);

-- ---------------------------------------------
-- 6. sys_dict_data
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `dict_sort`  INT           DEFAULT 0,
  `dict_label` VARCHAR(100)  DEFAULT NULL,
  `dict_value` VARCHAR(100)  DEFAULT NULL,
  `dict_type`  VARCHAR(100)  DEFAULT NULL,
  `css_class`  VARCHAR(100)  DEFAULT NULL,
  `list_class` VARCHAR(100)  DEFAULT NULL,
  `is_default` BOOLEAN       DEFAULT FALSE,
  `status`     BOOLEAN       DEFAULT TRUE,
  `remark`     VARCHAR(500)  DEFAULT NULL,
  `version`    BIGINT        DEFAULT 0,
  `created_at` TIMESTAMP     DEFAULT NULL,
  `updated_at` TIMESTAMP     DEFAULT NULL,
  `deleted`    BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`),
  CONSTRAINT `fk_sys_dict_data_type` FOREIGN KEY (`dict_type`) REFERENCES `sys_dict_type` (`dict_type`)
);

-- ---------------------------------------------
-- 7. user_role
-- ---------------------------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
);

-- ---------------------------------------------
-- 8. user_post
-- ---------------------------------------------
DROP TABLE IF EXISTS `user_post`;
CREATE TABLE `user_post` (
  `user_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `post_id`),
  CONSTRAINT `fk_user_post_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_user_post_post` FOREIGN KEY (`post_id`) REFERENCES `sys_post` (`id`)
);

-- ---------------------------------------------
-- 9. post_function
-- ---------------------------------------------
DROP TABLE IF EXISTS `post_function`;
CREATE TABLE `post_function` (
  `post_id`     BIGINT NOT NULL,
  `function_id` BIGINT NOT NULL,
  PRIMARY KEY (`post_id`, `function_id`),
  CONSTRAINT `fk_post_function_post` FOREIGN KEY (`post_id`) REFERENCES `sys_post` (`id`),
  CONSTRAINT `fk_post_function_function` FOREIGN KEY (`function_id`) REFERENCES `sys_function` (`id`)
);

-- ---------------------------------------------
-- 10. patient_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `patient_profile`;
CREATE TABLE `patient_profile` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`             BIGINT        DEFAULT NULL,
  `real_name`           VARCHAR(64)   DEFAULT NULL,
  `gender`              VARCHAR(20)   DEFAULT NULL,
  `birth_date`          DATE          DEFAULT NULL,
  `age`                 INT           DEFAULT NULL,
  `id_card`             VARCHAR(32)   DEFAULT NULL,
  `phone`               VARCHAR(20)   DEFAULT NULL,
  `emergency_contact`   VARCHAR(64)   DEFAULT NULL,
  `emergency_phone`     VARCHAR(20)   DEFAULT NULL,
  `address`             VARCHAR(255)  DEFAULT NULL,
  `avatar_url`          VARCHAR(500)  DEFAULT NULL,
  `remark`              VARCHAR(500)  DEFAULT NULL,
  `version`             BIGINT        DEFAULT 0,
  `created_at`          TIMESTAMP     DEFAULT NULL,
  `updated_at`          TIMESTAMP     DEFAULT NULL,
  `deleted`             BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`),
  KEY `idx_phone` (`phone`),
  CONSTRAINT `fk_patient_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 11. doctor_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `doctor_profile`;
CREATE TABLE `doctor_profile` (
  `id`                 BIGINT         NOT NULL AUTO_INCREMENT,
  `user_id`            BIGINT         DEFAULT NULL,
  `real_name`          VARCHAR(64)    DEFAULT NULL,
  `gender`             VARCHAR(20)    DEFAULT NULL,
  `title`              VARCHAR(64)    DEFAULT NULL,
  `department`         VARCHAR(64)    DEFAULT NULL,
  `specialty`          VARCHAR(255)   DEFAULT NULL,
  `introduction`       TEXT           DEFAULT NULL,
  `license_no`         VARCHAR(64)    DEFAULT NULL,
  `practice_years`     INT            DEFAULT NULL,
  `consultation_fee`   DECIMAL(10, 2) DEFAULT NULL,
  `remark`             VARCHAR(500)   DEFAULT NULL,
  `version`            BIGINT         DEFAULT 0,
  `created_at`         TIMESTAMP      DEFAULT NULL,
  `updated_at`         TIMESTAMP      DEFAULT NULL,
  `deleted`            BOOLEAN        DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_license_no` (`license_no`),
  KEY `idx_department` (`department`),
  CONSTRAINT `fk_doctor_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 12. admin_profile
-- ---------------------------------------------
DROP TABLE IF EXISTS `admin_profile`;
CREATE TABLE `admin_profile` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT        DEFAULT NULL,
  `real_name`  VARCHAR(64)   DEFAULT NULL,
  `gender`     VARCHAR(20)   DEFAULT NULL,
  `phone`      VARCHAR(20)   DEFAULT NULL,
  `department` VARCHAR(64)   DEFAULT NULL,
  `remark`     VARCHAR(500)  DEFAULT NULL,
  `version`    BIGINT        DEFAULT 0,
  `created_at` TIMESTAMP     DEFAULT NULL,
  `updated_at` TIMESTAMP     DEFAULT NULL,
  `deleted`    BOOLEAN       DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  CONSTRAINT `fk_admin_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 13. consultation_queue
-- ---------------------------------------------
DROP TABLE IF EXISTS `consultation_queue`;
CREATE TABLE `consultation_queue` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `patient_id`    BIGINT        NOT NULL,
  `patient_name`  VARCHAR(64)   NOT NULL,
  `doctor_id`     BIGINT        NOT NULL,
  `department`    VARCHAR(64)   DEFAULT NULL,
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'WAITING',
  `registered_at` TIMESTAMP     DEFAULT NULL,
  `called_at`     TIMESTAMP     DEFAULT NULL,
  `finished_at`   TIMESTAMP     DEFAULT NULL,
  `version`       BIGINT        NOT NULL DEFAULT 0,
  `created_at`    TIMESTAMP     DEFAULT NULL,
  `updated_at`    TIMESTAMP     DEFAULT NULL,
  `deleted`       BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_doctor_status` (`doctor_id`, `status`),
  KEY `idx_patient_id` (`patient_id`),
  CONSTRAINT `fk_consultation_queue_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`),
  CONSTRAINT `fk_consultation_queue_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 14. medical_record
-- ---------------------------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `patient_id`         BIGINT        NOT NULL,
  `doctor_id`          BIGINT        NOT NULL,
  `department`         VARCHAR(64)   DEFAULT NULL,
  `version_no`         INT           NOT NULL DEFAULT 0,
  `version`            BIGINT        NOT NULL DEFAULT 0,
  `status`             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  `chief_complaint`    TEXT          DEFAULT NULL,
  `present_illness`   TEXT          DEFAULT NULL,
  `past_history`       TEXT          DEFAULT NULL,
  `diagnosis`          TEXT          DEFAULT NULL,
  `treatment_plan`     TEXT          DEFAULT NULL,
  `prescription_id`    BIGINT        DEFAULT NULL,
  `template_id`        BIGINT        DEFAULT NULL,
  `ai_generated`       BOOLEAN       NOT NULL DEFAULT FALSE,
  `remark`             VARCHAR(500)  DEFAULT NULL,
  `draft_key`          VARCHAR(60),
  `official_key`       VARCHAR(60),
  `created_at`         TIMESTAMP     DEFAULT NULL,
  `updated_at`         TIMESTAMP     DEFAULT NULL,
  `deleted`            BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_doctor_draft` (`draft_key`),
  UNIQUE KEY `uk_patient_official_version` (`official_key`),
  KEY `idx_patient_status` (`patient_id`, `status`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_prescription_id` (`prescription_id`),
  CONSTRAINT `fk_medical_record_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`),
  CONSTRAINT `fk_medical_record_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 15. prescription
-- ---------------------------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `patient_id`     BIGINT        NOT NULL,
  `patient_name`   VARCHAR(64)   NOT NULL,
  `doctor_id`      BIGINT        NOT NULL,
  `department`     VARCHAR(64)   DEFAULT NULL,
  `status`         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  `diagnosis`      VARCHAR(500)  DEFAULT NULL,
  `ai_checked`     BOOLEAN       NOT NULL DEFAULT FALSE,
  `ai_risk_level`  VARCHAR(20)   DEFAULT NULL,
  `audit_remark`   VARCHAR(500)  DEFAULT NULL,
  `audited_by`     BIGINT        DEFAULT NULL,
  `audited_at`     TIMESTAMP     DEFAULT NULL,
  `remark`         VARCHAR(500)  DEFAULT NULL,
  `created_at`     TIMESTAMP     DEFAULT NULL,
  `updated_at`     TIMESTAMP     DEFAULT NULL,
  `deleted`        BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_patient_status` (`patient_id`, `status`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_patient_doctor_created` (`patient_id`, `doctor_id`, `created_at`),
  CONSTRAINT `fk_prescription_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`),
  CONSTRAINT `fk_prescription_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 16. prescription_item
-- ---------------------------------------------
DROP TABLE IF EXISTS `prescription_item`;
CREATE TABLE `prescription_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `prescription_id` BIGINT     NOT NULL,
  `drug_name`    VARCHAR(128) NOT NULL,
  `specification` VARCHAR(128) DEFAULT NULL,
  `dosage`       VARCHAR(64)  DEFAULT NULL,
  `usage_method` VARCHAR(64)  DEFAULT NULL,
  `frequency`    VARCHAR(64)  DEFAULT NULL,
  `quantity`     DECIMAL(10,2) NOT NULL DEFAULT 1,
  `unit`         VARCHAR(32)  DEFAULT NULL,
  `remark`       VARCHAR(500) DEFAULT NULL,
  `created_at`   TIMESTAMP    DEFAULT NULL,
  `updated_at`   TIMESTAMP    DEFAULT NULL,
  `deleted`      BOOLEAN      NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`),
  CONSTRAINT `fk_prescription_item_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`id`)
);

-- ---------------------------------------------
-- 17. medical_record_template
-- ---------------------------------------------
DROP TABLE IF EXISTS `medical_record_template`;
CREATE TABLE `medical_record_template` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `doctor_id`   BIGINT        NOT NULL,
  `department`  VARCHAR(64)   DEFAULT NULL,
  `name`        VARCHAR(128)  NOT NULL,
  `content`     TEXT          DEFAULT NULL,
  `version_no`  INT           NOT NULL DEFAULT 1,
  `version`     BIGINT        NOT NULL DEFAULT 0,
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  `created_at`  TIMESTAMP     DEFAULT NULL,
  `updated_at`  TIMESTAMP     DEFAULT NULL,
  `deleted`     BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_department` (`department`),
  CONSTRAINT `fk_template_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `sys_user` (`id`)
);

-- ---------------------------------------------
-- 18. condition_entry
-- ---------------------------------------------
DROP TABLE IF EXISTS `condition_entry`;
CREATE TABLE `condition_entry` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `patient_id` BIGINT        NOT NULL,
  `content`    TEXT          NOT NULL,
  `created_at` TIMESTAMP     DEFAULT NULL,
  `updated_at` TIMESTAMP     DEFAULT NULL,
  `deleted`    BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_patient_id` (`patient_id`),
  CONSTRAINT `fk_condition_entry_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient_profile` (`id`)
);

SET REFERENTIAL_INTEGRITY TRUE;
