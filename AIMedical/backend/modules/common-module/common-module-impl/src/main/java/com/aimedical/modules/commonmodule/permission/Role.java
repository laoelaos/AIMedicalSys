package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "sys_role")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"users", "posts"})
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 排序号（角色优先级）
     *
     * <p>值越小优先级越高。Phase 3 中由 UserConverter 按此字段排序取用户主角色。
     * 默认 0。
     */
    @Column(nullable = false)
    private Integer sort = 0;

    @Column(length = 500)
    private String remark;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<Post> posts;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users;

    @PrePersist
    private void validateBeforePersist() {
        if (enabled == null) {
            enabled = true;
        }
        if (sort == null) {
            sort = 0;
        }
    }

}
