package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "sys_role")
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

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<Post> posts;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @PrePersist
    private void validateBeforePersist() {
        if (enabled == null) {
            throw new org.hibernate.PropertyValueException(
                    "not-null property references a null or transient value: "
                            + Role.class.getName() + ".enabled",
                    "enabled",
                    Role.class.getName());
        }
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Set<Post> getPosts() {
        return posts;
    }

    public void setPosts(Set<Post> posts) {
        this.posts = posts;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
