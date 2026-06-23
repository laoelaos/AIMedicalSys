package com.aimedical.modules.admin.entity.dict;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sys_dict_type")
@Data
public class DictType extends BaseEntity {

    @Column(length = 100)
    private String dictName;

    @Column(name = "dict_type", unique = true, nullable = false, length = 100)
    private String dictType;

    private Boolean status;

    @OneToMany(mappedBy = "dictType", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DictData> dictDataList = new ArrayList<>();

    @Column(length = 500)
    private String remark;

}
