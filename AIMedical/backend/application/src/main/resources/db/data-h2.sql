-- =============================================
-- 智慧云脑诊疗平台 - 初始数据 (H2 开发环境)
-- =============================================

SET REFERENTIAL_INTEGRITY FALSE;

-- ---------------------------------------------
-- 角色
-- ---------------------------------------------
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'ADMIN',   '系统管理员', '拥有系统全部权限', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, 'DOCTOR',  '医生',       '医生角色',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(3, 'PATIENT', '患者',       '患者角色',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 岗位
-- ---------------------------------------------
INSERT INTO `sys_post` (`id`, `code`, `name`, `description`, `role_id`, `enabled`, `sort`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'ADMIN_MAIN',      '主管理员', '系统主管理员', 1, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, 'DOCTOR_CLINIC',   '门诊医生', '门诊医生岗位', 2, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(3, 'DOCTOR_INSPECT',  '检查医生', '检查医生岗位', 2, TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(4, 'DOCTOR_LAB',      '检验医生', '检验医生岗位', 2, TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(5, 'DOCTOR_PHARMACY', '药房医生', '药房医生岗位', 2, TRUE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(6, 'PATIENT_USER',    '普通患者', '普通患者用户', 3, TRUE, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 菜单 / 功能
-- ---------------------------------------------
INSERT INTO `sys_function` (`id`, `parent_id`, `code`, `name`, `type`, `path`, `component`, `icon`, `sort`, `visible`, `perms`, `enabled`, `created_at`, `updated_at`, `deleted`) VALUES
(1,  NULL, 'system',              '系统管理', 'MENU', '/system',               'Layout',                     'setting',     1, TRUE, NULL,                       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2,  1,    'system:user',         '用户管理', 'MENU', '/system/user',          'system/user/index',          'user',        1, TRUE, 'system:user:list',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(3,  1,    'system:role',         '角色管理', 'MENU', '/system/role',          'system/role/index',          'peoples',     2, TRUE, 'system:role:list',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(4,  1,    'system:post',         '岗位管理', 'MENU', '/system/post',          'system/post/index',          'user-tag',    3, TRUE, 'system:post:list',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(5,  1,    'system:menu',         '菜单管理', 'MENU', '/system/menu',          'system/menu/index',          'menu',        4, TRUE, 'system:menu:list',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(6,  1,    'system:dict',         '字典管理', 'MENU', '/system/dict',          'system/dict/index',          'edit',        5, TRUE, 'system:dict:list',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(7,  NULL, 'monitor',             '系统监控', 'MENU', '/monitor',              'Layout',                     'monitor',     2, TRUE, NULL,                       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(8,  7,    'monitor:log',         '操作日志', 'MENU', '/monitor/operationLog', 'monitor/operationLog/index', 'form',        1, TRUE, 'monitor:operationLog:list', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(9,  7,    'monitor:loginLog',    '登录日志', 'MENU', '/monitor/loginLog',     'monitor/loginLog/index',     'logininfor',  2, TRUE, 'monitor:loginLog:list',    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(10, NULL, 'patient',             '患者中心', 'MENU', '/patient',              'Layout',                     'user-friend', 3, TRUE, NULL,                       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(11, 10,   'patient:profile',     '个人中心', 'MENU', '/patient/profile',      'patient/profile/index',      'id-card',     1, TRUE, 'patient:profile:view',     TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(12, 10,   'patient:health',      '健康档案', 'MENU', '/patient/health',       'patient/health/index',       'heart',       2, TRUE, 'patient:health:view',      TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 字典类型
-- ---------------------------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '用户性别',     'sys_user_sex',       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, '系统开关',     'sys_normal_disable', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(3, '是否',         'sys_yes_no',         TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(4, '处方状态',     'prescription_status', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(5, '病历状态',     'medical_record_status', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(6, '就诊状态',     'consultation_status', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 字典数据
-- ---------------------------------------------
INSERT INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '男', 'M', 'sys_user_sex', '', '', TRUE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, 2, '女', 'F', 'sys_user_sex', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(3, 1, '是', '1', 'sys_yes_no', '', '', TRUE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(4, 2, '否', '0', 'sys_yes_no', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(5, 1, '启用', '1', 'sys_normal_disable', '', '', TRUE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(6, 2, '禁用', '0', 'sys_normal_disable', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(7, 1, '草稿', 'DRAFT', 'prescription_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(8, 2, '待审核', 'PENDING_REVIEW', 'prescription_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(9, 3, '已通过', 'APPROVED', 'prescription_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(10, 4, '已驳回', 'REJECTED', 'prescription_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(11, 1, '草稿', 'DRAFT', 'medical_record_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(12, 2, '正式', 'OFFICIAL', 'medical_record_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(13, 1, '等待中', 'WAITING', 'consultation_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(14, 2, '已叫号', 'CALLED', 'consultation_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(15, 3, '就诊中', 'IN_CONSULTATION', 'consultation_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(16, 4, '已完成', 'FINISHED', 'consultation_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(17, 5, '已过号', 'SKIPPED', 'consultation_status', '', '', FALSE, TRUE, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 用户
-- ---------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `gender`, `age`, `user_type`, `enabled`, `password_change_required`, `token_version`, `remark`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'admin',    '$2a$10$7JB720yubYU0gE7aN7ZQ5Og8F6tN7J7aL7aL7aL7aL7aL7aL7aL7', '管理员', '13800138000', 'admin@aimedical.com', 'M', 30, 'ADMIN', TRUE, FALSE, 0, '系统管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, 'doctor01', '$2a$10$7JB720yubYU0gE7aN7ZQ5Og8F6tN7J7aL7aL7aL7aL7aL7aL7aL7', '张医生', '13800138001', 'doctor01@aimedical.com', 'M', 45, 'DOCTOR', TRUE, FALSE, 0, '门诊医生', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(3, 'doctor02', '$2a$10$7JB720yubYU0gE7aN7ZQ5Og8F6tN7J7aL7aL7aL7aL7aL7aL7aL7', '李医生', '13800138002', 'doctor02@aimedical.com', 'F', 38, 'DOCTOR', TRUE, FALSE, 0, '检验医生', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(4, 'patient01', '$2a$10$7JB720yubYU0gE7aN7ZQ5Og8F6tN7J7aL7aL7aL7aL7aL7aL7aL7', '王患者', '13800138003', 'patient01@aimedical.com', 'M', 28, 'PATIENT', TRUE, FALSE, 0, '普通患者', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(5, 'patient02', '$2a$10$7JB720yubYU0gE7aN7ZQ5Og8F6tN7J7aL7aL7aL7aL7aL7aL7aL7', '赵患者', '13800138004', 'patient02@aimedical.com', 'F', 52, 'PATIENT', TRUE, FALSE, 0, '普通患者', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 用户-角色关联
-- ---------------------------------------------
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 2),
(4, 3),
(5, 3);

-- ---------------------------------------------
-- 用户-岗位关联
-- ---------------------------------------------
INSERT INTO `user_post` (`user_id`, `post_id`) VALUES
(1, 1),
(2, 2),
(3, 4),
(4, 6),
(5, 6);

-- ---------------------------------------------
-- 医生档案
-- ---------------------------------------------
INSERT INTO `doctor_profile` (`id`, `user_id`, `real_name`, `gender`, `title`, `department`, `specialty`, `introduction`, `license_no`, `practice_years`, `consultation_fee`, `remark`, `version`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 2, '张三', 'M', '主任医师', '内科', '心血管疾病诊治', '从事内科临床工作20年，擅长心血管疾病的诊断与治疗', 'D12345678', 20, 100.00, '', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, 3, '李四', 'F', '副主任医师', '检验科', '临床检验', '从事检验工作15年，擅长各类临床检验项目', 'D87654321', 15, 80.00, '', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 患者档案
-- ---------------------------------------------
INSERT INTO `patient_profile` (`id`, `user_id`, `real_name`, `gender`, `birth_date`, `age`, `id_card`, `phone`, `emergency_contact`, `emergency_phone`, `address`, `avatar_url`, `remark`, `version`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 4, '王五', 'M', '1998-01-15', 28, '110101199801151234', '13800138003', '王六', '13800138005', '北京市朝阳区XX街道XX号', '', '', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
(2, 5, '赵六', 'F', '1974-06-20', 52, '110101197406205678', '13800138004', '钱七', '13800138006', '北京市海淀区XX路XX号', '', '', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- ---------------------------------------------
-- 岗位-功能关联
-- ---------------------------------------------
INSERT INTO `post_function` (`post_id`, `function_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(2, 10), (2, 11), (2, 12),
(3, 10), (3, 11), (3, 12),
(4, 10), (4, 11), (4, 12),
(5, 10), (5, 11), (5, 12),
(6, 11), (6, 12);

SET REFERENTIAL_INTEGRITY TRUE;
