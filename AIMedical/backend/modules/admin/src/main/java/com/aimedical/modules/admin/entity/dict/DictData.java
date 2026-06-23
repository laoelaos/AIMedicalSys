package com.aimedical.modules.admin.entity.dict;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sys_dict_data")
@Data
public class DictData extends BaseEntity {

    private Integer dictSort;

    @Column(length = 100)
    private String dictLabel;

    @Column(length = 100)
    private String dictValue;

    @Column(length = 100)
    private String dictType;

    @Column(length = 100)
    private String cssClass;

    @Column(length = 100)
    private String listClass;

    private Boolean isDefault;

    private Boolean status;

    @Column(length = 500)
    private String remark;

}