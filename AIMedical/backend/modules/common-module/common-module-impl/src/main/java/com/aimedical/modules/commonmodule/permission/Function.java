package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import com.aimedical.common.base.MenuType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "sys_function")
@Getter
@Setter
public class Function extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    private Boolean enabled;

    @ManyToMany(mappedBy = "functions", fetch = FetchType.LAZY)
    private Set<Post> posts;

    @Column(name = "parent_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MenuType type = MenuType.MENU;

    @Column(length = 128)
    private String path;

    @Column(length = 255)
    private String component;

    @Column(length = 64)
    private String icon;

    @Column(name = "sort")
    private Integer sort;

    private Boolean visible;

    @Column(length = 128)
    private String perms;

    @Column(name = "query_method", length = 10)
    private String queryMethod;

    @Column(length = 500)
    private String remark;

}