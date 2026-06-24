package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "sys_post")
@Getter
@Setter
public class Post extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    private Boolean enabled = true;

    private Integer sort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_function",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "function_id"))
    private Set<Function> functions;

    @ManyToMany(mappedBy = "posts", fetch = FetchType.LAZY)
    private Set<User> users;

    @Column(length = 500)
    private String remark;

}
