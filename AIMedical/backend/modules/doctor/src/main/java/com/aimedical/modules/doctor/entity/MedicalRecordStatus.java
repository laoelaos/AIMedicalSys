package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 病历状态枚举（版本管理）
 *
 * <p>DRAFT(草稿) -> OFFICIAL(正式)：医生确认后由草稿转为正式版本。
 * 同一患者的正式病历保留历史版本，通过 version_no 区分。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MedicalRecordStatus implements BaseEnum {

    DRAFT("DRAFT", "草稿"),
    OFFICIAL("OFFICIAL", "正式");

    private final String code;
    private final String desc;
}
