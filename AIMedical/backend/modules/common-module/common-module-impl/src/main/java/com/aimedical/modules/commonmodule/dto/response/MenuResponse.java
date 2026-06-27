package com.aimedical.modules.commonmodule.dto.response;

import java.util.List;

public record MenuResponse(
    Long id,
    String name,
    String path,
    String component,
    String icon,
    String permission,
    Integer sort,
    List<MenuResponse> children
) {
    public MenuResponse withChildren(List<MenuResponse> children) {
        return new MenuResponse(this.id, this.name, this.path, this.component, this.icon, this.permission, this.sort, children);
    }
}
