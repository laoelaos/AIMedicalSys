-- AIMedical系统数据初始化脚本
-- Phase1 包D 认证功能种子数据

-- 清空现有数据（按依赖顺序删除）
DELETE FROM user_post;
DELETE FROM user_role;
DELETE FROM post_function;
DELETE FROM sys_user;
DELETE FROM sys_post;
DELETE FROM sys_function;
DELETE FROM sys_role;

-- 插入角色数据
INSERT INTO sys_role (id, code, name, description, enabled, deleted, created_at, updated_at) VALUES
(1, 'ADMIN', '系统管理员', '拥有系统全部权限', true, false, NOW(), NOW()),
(2, 'DOCTOR', '医生', '医生角色，拥有医生端功能权限', true, false, NOW(), NOW()),
(3, 'PATIENT', '患者', '患者角色，拥有患者端功能权限', true, false, NOW(), NOW());

-- 插入岗位数据
INSERT INTO sys_post (id, code, name, description, enabled, role_id, deleted, created_at, updated_at) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '系统超级管理员岗位', true, 1, false, NOW(), NOW()),
(2, 'DOCTOR_GENERAL', '普通医生', '普通医生岗位', true, 2, false, NOW(), NOW()),
(3, 'PATIENT_GENERAL', '普通患者', '普通患者岗位', true, 3, false, NOW(), NOW());

-- 插入功能数据（菜单功能）
INSERT INTO sys_function (id, code, name, description, enabled, deleted, created_at, updated_at) VALUES
(1, 'menu:dashboard', '仪表盘', '查看仪表盘', true, false, NOW(), NOW()),
(2, 'menu:registration', '挂号管理', '挂号管理菜单', true, false, NOW(), NOW()),
(3, 'menu:patient', '患者管理', '患者管理菜单', true, false, NOW(), NOW()),
(4, 'menu:appointment', '预约管理', '预约管理菜单', true, false, NOW(), NOW()),
(5, 'menu:system', '系统管理', '系统管理菜单', true, false, NOW(), NOW()),
(6, 'menu:user', '用户管理', '用户管理菜单', true, false, NOW(), NOW()),
(7, 'menu:role', '角色管理', '角色管理菜单', true, false, NOW(), NOW()),
(8, 'menu:menu', '菜单管理', '菜单管理菜单', true, false, NOW(), NOW());

-- 插入用户数据（密码使用BCrypt加密，统一密码为：password123）
-- BCrypt password123: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.FqvIdBI
INSERT INTO sys_user (id, username, password, nickname, phone, email, enabled, user_type, deleted, created_at, updated_at) VALUES
(1, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.FqvIdBI', '系统管理员', '13800138001', 'admin@aimedical.com', true, 'ADMIN', false, NOW(), NOW()),
(2, 'doctor001', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.FqvIdBI', '张医生', '13800138002', 'doctor001@aimedical.com', true, 'DOCTOR', false, NOW(), NOW()),
(3, 'patient001', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.FqvIdBI', '李患者', '13800138003', 'patient001@aimedical.com', true, 'PATIENT', false, NOW(), NOW());

-- 插入用户角色关联
INSERT INTO user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

-- 插入用户岗位关联
INSERT INTO user_post (user_id, post_id) VALUES
(1, 1),
(2, 2),
(3, 3);

-- 插入岗位功能关联（超级管理员拥有全部功能）
INSERT INTO post_function (post_id, function_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(2, 1), (2, 2), (2, 3), (2, 4),
(3, 1);