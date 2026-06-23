package com.aimedical.modules.admin.entity.dict;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sys_dict_type")
@Data
public class DictType extends BaseEntity {

    @Column(length = 100)
    private String dictName;

    @Column(unique = true, nullable = false, length = 100)
    private String dictType;

    private Boolean status;

    @Column(length = 500)
    private String remark;

}
