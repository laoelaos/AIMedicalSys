package com.aimedical.modules.admin.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_profile")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AdminEntity extends BaseEntity {

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "department", length = 64)
    private String department;

    @Column(name = "remark", length = 500)
    private String remark;

}
