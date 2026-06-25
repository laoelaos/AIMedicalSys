package com.aimedical.modules.commonmodule.dto.response;

import java.util.List;

/**
 * 用户信息响应DTO
 *
 * <p>用于返回当前登录用户的详细信息。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色类型
     */
    private String role;

    /**
     * 岗位类型（医生端必填）
     */
    private String position;

    /**
     * 权限标识列表
     */
    private List<String> permissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}