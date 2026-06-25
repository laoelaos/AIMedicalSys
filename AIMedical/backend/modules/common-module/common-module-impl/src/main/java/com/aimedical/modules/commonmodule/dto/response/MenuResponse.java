package com.aimedical.modules.commonmodule.dto.response;

import java.util.List;

/**
 * 菜单响应DTO
 *
 * <p>用于返回菜单树结构，支持递归嵌套。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class MenuResponse {

    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 图标名称
     */
    private String icon;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 父菜单ID，用于前端构建树形结构
     */
    private Long parentId;

    /**
     * 菜单类型（DIRECTORY/MENU/BUTTON）
     */
    private String type;

    /**
     * 是否可见
     */
    private Boolean visible;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 子菜单列表
     */
    private List<MenuResponse> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<MenuResponse> getChildren() {
        return children;
    }

    public void setChildren(List<MenuResponse> children) {
        this.children = children;
    }
}
