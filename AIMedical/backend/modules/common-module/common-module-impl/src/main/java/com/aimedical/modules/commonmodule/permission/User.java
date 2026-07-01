package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import com.aimedical.modules.commonmodule.api.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * 用户实体
 *
 * <p>系统用户，支持多种用户类型（管理员/医生/患者）。
 */
@Entity
@Table(name = "sys_user")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private String phone;

    private String email;

    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 是否必须修改密码
     *
     * <p>true 表示首次登录或被管理员标记密码过期，需在 PasswordChangeCheckFilter
     * 阶段强制走 /api/auth/password 流程；默认 false。
     */
    @Column(nullable = false)
    private Boolean passwordChangeRequired = false;

    /**
     * 令牌版本号
     *
     * <p>Refresh Token 刷新时与 claims 中的 tokenVersion 比对，不一致即拒绝；
     * 密码变更后递增（+1），使已签发的旧 Refresh Token 即时失效。默认 0。
     */
    @Column(nullable = false)
    private Integer tokenVersion = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @Column(length = 10)
    private String gender;

    @Column
    private Integer age;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @EqualsAndHashCode.Exclude
    private Set<Role> roles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_post",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id"))
    @EqualsAndHashCode.Exclude
    private Set<Post> posts;

    @Column(length = 500)
    private String remark;
}
