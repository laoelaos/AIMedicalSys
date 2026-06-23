package com.aimedical.modules.admin.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "admin_profile")
@Data
public class AdminEntity extends BaseEntity {

    @Column(unique = true)
    private Long userId;

    @Column(length = 64)
    private String realName;

    @Column(length = 20)
    private String gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String department;

    @Column(length = 500)
    private String remark;

}
