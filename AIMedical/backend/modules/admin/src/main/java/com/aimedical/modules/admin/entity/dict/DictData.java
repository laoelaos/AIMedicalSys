package com.aimedical.modules.admin.entity.dict;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne
    @JoinColumn(name = "dict_type", referencedColumnName = "dict_type")
    private DictType dictType;

    @Column(length = 100)
    private String cssClass;

    @Column(length = 100)
    private String listClass;

    private Boolean isDefault;

    private Boolean status;

    @Column(length = 500)
    private String remark;

}