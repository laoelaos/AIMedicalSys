-- =============================================
-- 智慧云脑诊疗平台 - 初始数据
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------
-- 角色
-- ---------------------------------------------
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `sort`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'ADMIN',   '系统管理员', '拥有系统全部权限', 1, 1, NOW(), NOW(), 0),
(2, 'DOCTOR',  '医生',       '医生角色',         1, 2, NOW(), NOW(), 0),
(3, 'PATIENT', '患者',       '患者角色',         1, 3, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 岗位
-- ---------------------------------------------
INSERT INTO `sys_post` (`id`, `code`, `name`, `description`, `role_id`, `enabled`, `sort`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'ADMIN_MAIN',      '主管理员', '系统主管理员', 1, 1, 1, NOW(), NOW(), 0),
(2, 'DOCTOR_CLINIC',   '门诊医生', '门诊医生岗位', 2, 1, 2, NOW(), NOW(), 0),
(3, 'DOCTOR_INSPECT',  '检查医生', '检查医生岗位', 2, 1, 3, NOW(), NOW(), 0),
(4, 'DOCTOR_LAB',      '检验医生', '检验医生岗位', 2, 1, 4, NOW(), NOW(), 0),
(5, 'DOCTOR_PHARMACY', '药房医生', '药房医生岗位', 2, 1, 5, NOW(), NOW(), 0),
(6, 'PATIENT_USER',    '普通患者', '普通患者用户', 3, 1, 6, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 菜单 / 功能
-- ---------------------------------------------
INSERT INTO `sys_function` (`id`, `parent_id`, `code`, `name`, `type`, `path`, `component`, `icon`, `sort_order`, `visible`, `enabled`, `created_at`, `updated_at`, `deleted`) VALUES
(1,  NULL, 'system',              '系统管理', 'MENU', '/system',               'Layout',                     'setting',     1, TRUE, TRUE, NOW(), NOW(), 0),
(2,  1,    'system:user',         '用户管理', 'MENU', '/system/user',          'system/user/index',          'user',        1, TRUE, TRUE, NOW(), NOW(), 0),
(3,  1,    'system:role',         '角色管理', 'MENU', '/system/role',          'system/role/index',          'peoples',     2, TRUE, TRUE, NOW(), NOW(), 0),
(4,  1,    'system:post',         '岗位管理', 'MENU', '/system/post',          'system/post/index',          'user-tag',    3, TRUE, TRUE, NOW(), NOW(), 0),
(5,  1,    'system:menu',         '菜单管理', 'MENU', '/system/menu',          'system/menu/index',          'menu',        4, TRUE, TRUE, NOW(), NOW(), 0),
(6,  1,    'system:dict',         '字典管理', 'MENU', '/system/dict',          'system/dict/index',          'edit',        5, TRUE, TRUE, NOW(), NOW(), 0),
(7,  NULL, 'monitor',             '系统监控', 'MENU', '/monitor',              'Layout',                     'monitor',     2, TRUE, TRUE, NOW(), NOW(), 0),
(8,  7,    'monitor:log',         '操作日志', 'MENU', '/monitor/operationLog', 'monitor/operationLog/index', 'form',        1, TRUE, TRUE, NOW(), NOW(), 0),
(9,  7,    'monitor:loginLog',    '登录日志', 'MENU', '/monitor/loginLog',     'monitor/loginLog/index',     'logininfor',  2, TRUE, TRUE, NOW(), NOW(), 0),
(10, NULL, 'patient',             '患者中心', 'MENU', '/patient',              'Layout',                     'user-friend', 3, TRUE, TRUE, NOW(), NOW(), 0),
(11, 10,   'patient:profile',     '个人中心', 'MENU', '/patient/profile',      'patient/profile/index',      'id-card',     1, TRUE, TRUE, NOW(), NOW(), 0),
(12, 10,   'patient:health',      '健康档案', 'MENU', '/patient/health',       'patient/health/index',       'heart',       2, TRUE, TRUE, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 字典类型
-- ---------------------------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '用户性别',     'sys_user_sex',       1, NOW(), NOW(), 0),
(2, '系统开关',     'sys_normal_disable', 1, NOW(), NOW(), 0),
(3, '是否',         'sys_yes_no',         1, NOW(), NOW(), 0),
(4, '血型',         'blood_type',         1, NOW(), NOW(), 0),
(5, '过敏严重程度', 'allergy_severity',   1, NOW(), NOW(), 0),
(6, '病情状态',     'disease_status',     1, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 字典数据
-- ---------------------------------------------
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '男',   'male',    'sys_user_sex',       '', '', 1, 1, NOW(), NOW(), 0),
(2, '女',   'female',  'sys_user_sex',       '', '', 0, 1, NOW(), NOW(), 0),
(3, '未知', 'unknown', 'sys_user_sex',       '', '', 0, 1, NOW(), NOW(), 0),
(1, '正常', '0',       'sys_normal_disable', '', '', 1, 1, NOW(), NOW(), 0),
(2, '停用', '1',       'sys_normal_disable', '', '', 0, 1, NOW(), NOW(), 0),
(1, '是', 'Y',         'sys_yes_no',         '', '', 0, 1, NOW(), NOW(), 0),
(2, '否', 'N',         'sys_yes_no',         '', '', 1, 1, NOW(), NOW(), 0),
(1, 'A型',  'A',       'blood_type',         '', '', 0, 1, NOW(), NOW(), 0),
(2, 'B型',  'B',       'blood_type',         '', '', 0, 1, NOW(), NOW(), 0),
(3, 'AB型', 'AB',      'blood_type',         '', '', 0, 1, NOW(), NOW(), 0),
(4, 'O型',  'O',       'blood_type',         '', '', 0, 1, NOW(), NOW(), 0),
(5, '未知', 'UNKNOWN', 'blood_type',         '', '', 1, 1, NOW(), NOW(), 0),
(1, '轻度', 'MILD',     'allergy_severity',   '', '', 1, 1, NOW(), NOW(), 0),
(2, '中度', 'MODERATE', 'allergy_severity',   '', '', 0, 1, NOW(), NOW(), 0),
(3, '重度', 'SEVERE',   'allergy_severity',   '', '', 0, 1, NOW(), NOW(), 0),
(1, '稳定',   'STABLE',   'disease_status',   '', '', 1, 1, NOW(), NOW(), 0),
(2, '不稳定', 'UNSTABLE', 'disease_status',   '', '', 0, 1, NOW(), NOW(), 0),
(3, '已康复', 'RECOVERED','disease_status',   '', '', 0, 1, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 测试用户
-- ---------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `user_type`, `enabled`, `password_change_required`, `token_version`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'admin',       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', '13800000001', 'admin@aimedical.com',    'ADMIN',   1, 0, 0, NOW(), NOW(), 0),
(2, 'doctor01',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张医生',     '13800000002', 'doctor01@aimedical.com', 'DOCTOR',  1, 0, 0, NOW(), NOW(), 0),
(3, '13900000003', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李先生',     '13900000003', 'patient01@aimedical.com','PATIENT', 1, 0, 0, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 用户-角色
-- ---------------------------------------------
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3);

-- ---------------------------------------------
-- 用户-岗位
-- ---------------------------------------------
INSERT INTO `user_post` (`user_id`, `post_id`) VALUES
(1, 1),
(2, 2),
(3, 6);

-- ---------------------------------------------
-- 岗位-功能
-- ---------------------------------------------
INSERT INTO `post_function` (`post_id`, `function_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12),
(2, 10), (2, 11), (2, 12),
(6, 10), (6, 11), (6, 12);

-- ---------------------------------------------
-- 患者档案 (user_id=3)
-- ---------------------------------------------
INSERT INTO `patient_profile` (`id`, `user_id`, `real_name`, `gender`, `birth_date`, `age`, `id_card`, `phone`, `emergency_contact`, `emergency_phone`, `address`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 3, '李明', 'MALE', '1990-05-15', 36, '110101199005150011', '13900000003', '王芳', '13900000004', '北京市海淀区中关村大街1号', NOW(), NOW(), 0);

-- ---------------------------------------------
-- 医生档案 (user_id=2)
-- ---------------------------------------------
INSERT INTO `doctor_profile` (`id`, `user_id`, `real_name`, `gender`, `title`, `department`, `specialty`, `introduction`, `license_no`, `practice_years`, `consultation_fee`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 2, '张建国', 'MALE', '副主任医师', '内科', '心血管疾病、高血压、冠心病', '从事内科临床工作15年，擅长心血管系统疾病诊断与治疗', '1101234567', 15, 200.00, NOW(), NOW(), 0);

-- ---------------------------------------------
-- 管理员档案 (user_id=1)
-- ---------------------------------------------
INSERT INTO `admin_profile` (`id`, `user_id`, `real_name`, `gender`, `phone`, `department`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '系统管理员', 'MALE', '13800000001', '信息中心', NOW(), NOW(), 0);

-- ---------------------------------------------
-- 患者健康记录 (关联 patient_profile.id=1, 使用 patient_* 新表体系)
-- ---------------------------------------------
INSERT INTO `patient_allergy` (`patient_id`, `allergen`, `reaction_type`, `severity`, `occurred_at`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '青霉素', '皮疹',     'MILD',     '2015-03-10', NOW(), NOW(), 0),
(1, '头孢类', '呼吸困难', 'MODERATE', '2018-07-20', NOW(), NOW(), 0);

INSERT INTO `patient_chronic_disease` (`patient_id`, `disease_name`, `diagnosed_at`, `current_status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '高血压', '2022-01-15', 'STABLE', NOW(), NOW(), 0);

INSERT INTO `patient_family_history` (`patient_id`, `relationship`, `disease_name`, `note`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '父亲', '冠心病', '60岁发病', NOW(), NOW(), 0),
(1, '母亲', '糖尿病', '55岁发病', NOW(), NOW(), 0);

INSERT INTO `patient_surgery_history` (`patient_id`, `surgery_name`, `surgery_at`, `hospital`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '阑尾切除术', '2010-06-15', '北京市第一人民医院', NOW(), NOW(), 0);

INSERT INTO `patient_medication_history` (`patient_id`, `drug_name`, `reason`, `started_at`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '硝苯地平缓释片', '高血压', '2022-02-01', NOW(), NOW(), 0);

-- ---------------------------------------------
-- 病历模板（按科室）
-- ---------------------------------------------
INSERT INTO `medical_record_template` (`id`, `department`, `name`, `chief_complaint_tpl`, `present_illness_tpl`, `past_history_tpl`, `diagnosis_tpl`, `treatment_plan_tpl`, `enabled`, `remark`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '内科', '内科通用病历模板', '主诉：[症状] [持续时间]', '现病史：患者[时间]前无明显诱因出现[症状]，伴[伴随症状]，无明显诱因，未诊治。', '既往史：高血压病史[年]，否认糖尿病、冠心病史，否认手术史。', '诊断：1. [初步诊断]\n2. 待排查', '治疗方案：1. 完善相关检查\n2. 对症治疗\n3. 注意休息，随诊', 1, '内科门诊通用模板', NOW(), NOW(), 0),
(2, '内科', '高血压随访病历模板', '主诉：高血压复诊', '现病史：患者确诊高血压[年]，长期口服[药物]，血压控制在[范围]。', '既往史：同前。', '诊断：原发性高血压', '治疗方案：1. 继续原方案服药\n2. 监测血压\n3. 低盐饮食', 1, '高血压随访专用模板', NOW(), NOW(), 0),
(3, '外科', '外科通用病历模板', '主诉：[部位] [症状] [持续时间]', '现病史：患者[时间]前[受伤机制/起病情况]，[症状演变]。', '既往史：否认慢性病史，否认药物过敏史。', '诊断：[初步诊断]', '治疗方案：1. 完善影像学检查\n2. 必要时手术', 1, '外科门诊通用模板', NOW(), NOW(), 0);

-- ---------------------------------------------
-- 接诊/叫号队列（doctor_id=2 内科张医生，patient_id=1 李明）
-- ---------------------------------------------
INSERT INTO `consultation_queue` (`id`, `patient_id`, `patient_name`, `doctor_id`, `department`, `queue_no`, `status`, `registered_at`, `called_at`, `finished_at`, `remark`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '李明', 2, '内科', 'A001', 'WAITING', NOW(), NULL, NULL, '初诊', NOW(), NOW(), 0),
(2, 1, '李明', 2, '内科', 'A002', 'WAITING', NOW(), NULL, NULL, '复诊', NOW(), NOW(), 0),
(3, 1, '李明', 2, '内科', 'A003', 'CALLED',  NOW(), NOW(), NULL, NULL, NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;
