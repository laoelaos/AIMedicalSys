package com.aimedical.modules.commonmodule.patient;

import java.util.Optional;

/**
 * 患者信息查询端口接口。
 *
 * <p>定义跨模块患者信息查询的抽象接口，由 patient 模块提供实现，
 * 消费方（如 doctor 模块）仅依赖此接口，避免编译期跨模块硬依赖。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface PatientInfoPort {

    /**
     * 按患者档案ID查询患者姓名。
     *
     * @param patientId 患者档案ID
     * @return 患者姓名（存在时），或 Optional.empty()
     */
    Optional<String> findNameById(Long patientId);
}
