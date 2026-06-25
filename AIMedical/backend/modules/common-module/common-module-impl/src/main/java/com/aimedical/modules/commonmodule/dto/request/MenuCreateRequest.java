package com.aimedical.modules.commonmodule.dto.request;

import com.aimedical.common.base.MenuType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建菜单请求DTO
 *
 * <p>用于创建新菜单的请求参数。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class MenuCreateRequest {

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    private String name;

    @NotBlank(message = "菜单编码不能为空")
    @Size(max = 100, message = "菜单编码长度不能超过100")
    private String code;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    private Long parentId;

    @Size(max = 255, message = "路径长度不能超过255")
    private String path;

    @Size(max = 100, message = "图标长度不能超过100")
    private String icon;

    @NotNull(message = "菜单类型不能为空")
    private MenuType type;

    private Integer sortOrder;

    private Boolean visible;

    private Boolean enabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public MenuType getType() {
        return type;
    }

    public void setType(MenuType type) {
        this.type = type;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
}
