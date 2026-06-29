-- AIMedical 种子数据 (H2 MERGE INTO, 幂等 — DevTools 重启安全)
-- MERGE INTO KEY(id)  = 有则跳过，无才插入，不覆盖已存在数据

MERGE INTO sys_role (id, code, name, description, enabled, sort, deleted, created_at, updated_at) KEY(id) VALUES
(1, 'ADMIN',  '系统管理员', '拥有系统全部权限',         true, 1, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 'DOCTOR', '医生',      '医生角色，拥有医生端功能权限', true, 2, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 'PATIENT', '患者',     '患者角色，拥有患者端功能权限', true, 3, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

MERGE INTO sys_post (id, code, name, description, enabled, sort, role_id, deleted, created_at, updated_at) KEY(id) VALUES
(1, 'SUPER_ADMIN',    '超级管理员', '系统超级管理员岗位', true, 1, 1, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 'DOCTOR_GENERAL', '普通医生',   '普通医生岗位',       true, 2, 2, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 'PATIENT_GENERAL', '普通患者',  '普通患者岗位',       true, 3, 3, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

MERGE INTO sys_function (id, code, name, description, enabled, deleted, created_at, updated_at, sort_order, visible, type, icon, path) KEY(id) VALUES
(1, 'menu:dashboard',    '仪表盘',   '查看仪表盘',     true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 1, true, 'MENU',     'dashboard',    '/dashboard'),
(2, 'menu:registration', '挂号管理', '挂号管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 2, true, 'MENU',     'registration', '/registration'),
(3, 'menu:patient',      '患者管理', '患者管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 3, true, 'MENU',     'patient',      '/patient'),
(4, 'menu:appointment',  '预约管理', '预约管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 4, true, 'MENU',     'appointment',  '/appointment'),
(5, 'menu:system',       '系统管理', '系统管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 5, true, 'DIRECTORY', 'setting',     '/system'),
(6, 'menu:user',         '用户管理', '用户管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 1, true, 'MENU',     'user',         '/system/user'),
(7, 'menu:role',         '角色管理', '角色管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 2, true, 'MENU',     'role',         '/system/role'),
(8, 'menu:menu',         '菜单管理', '菜单管理菜单',   true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 3, true, 'MENU',     'menu',         '/system/menu');

-- 密码统一 password123 (BCrypt)
MERGE INTO sys_user (id, username, password, nickname, phone, email, enabled, password_change_required, token_version, user_type, deleted, created_at, updated_at) KEY(id) VALUES
(1, 'admin',      '$2a$10$S2kRnxEIV3e8UuvncH3cGuOhu1XSdaVJuwg9f3T6gfPmWeJsFOCYq', '系统管理员', '13800138001', 'admin@aimedical.com',      true, false, 0, 'ADMIN',   false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 'doctor001',  '$2a$10$S2kRnxEIV3e8UuvncH3cGuOhu1XSdaVJuwg9f3T6gfPmWeJsFOCYq', '张医生',     '13800138002', 'doctor001@aimedical.com', true, false, 0, 'DOCTOR',  false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, '13800138003', '$2a$10$S2kRnxEIV3e8UuvncH3cGuOhu1XSdaVJuwg9f3T6gfPmWeJsFOCYq', '李患者',     '13800138003', 'patient001@aimedical.com',true, false, 0, 'PATIENT', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

MERGE INTO user_role (user_id, role_id) KEY(user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3);

MERGE INTO user_post (user_id, post_id) KEY(user_id, post_id) VALUES
(1, 1), (2, 2), (3, 3);

MERGE INTO post_function (post_id, function_id) KEY(post_id, function_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(2, 1), (2, 2), (2, 3), (2, 4),
(3, 1);

-- 种子患者档案 (user_id=3 → 患者李患者)
MERGE INTO patient_profile (id, user_id, real_name, gender, phone, emergency_contact, avatar_url, deleted, created_at, updated_at) KEY(id) VALUES
(1, 3, '李明', 'MALE', '13800138003', '王芳 13700000001', NULL, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 种子患者过敏史
MERGE INTO patient_allergy (id, patient_id, allergen, reaction_type, severity, occurred_at, deleted, created_at, updated_at) KEY(id) VALUES
(1, 1, '青霉素', '皮疹', 'MILD', '2015-03-10', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 种子患者慢性病史
MERGE INTO patient_chronic_disease (id, patient_id, disease_name, diagnosed_at, current_status, deleted, created_at, updated_at) KEY(id) VALUES
(1, 1, '高血压', '2022-01-15', 'STABLE', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 种子挂号记录 (user_id=3 即患者 13800138003)
MERGE INTO registration (id, user_id, registration_type, doctor_name, department_name, time_slot, status, deleted, created_at, updated_at) KEY(id) VALUES
(1, 3, 'OUTPATIENT', '王主任', '神经内科', '07-01 08:00-08:30', 'CONFIRMED', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 3, 'EXAMINATION', NULL, NULL, '07-02 10:30-11:00', 'PENDING', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 3, 'OUTPATIENT', '李主治医师', '普通内科', '07-01 15:00-15:30', 'DISPENSED', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 种子分诊记录 (patient_id=3 即患者 13800138003)
MERGE INTO triage_record (id, patient_id, chief_complaint, session_id, recommended_departments, recommended_doctors, is_degraded, rule_version, rule_set_id, matched_rules, deleted, created_at, updated_at) KEY(id) VALUES
(1, 3, '头痛3天，伴有恶心，前额搏动性疼痛', 'mock-session-001', '神经内科,普通内科,中医科', '王主任,张副主任,李主治医师', false, 'v1.0.0', 'rule-set-neuro', '头痛规则-偏头痛,头痛规则-紧张性', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 3, '发烧2天，体温38.5°C，咳嗽咽痛', 'mock-session-002', '呼吸内科,普通内科,感染科', '王主任,李主治医师', false, 'v1.0.0', 'rule-set-resp', '发热规则-上感,咳嗽规则', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 3, '腹痛1天，右下腹持续性疼痛', 'mock-degraded-001', '普通内科', '张副主任', true, 'v1.0.0', 'rule-set-abd', '', false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
