package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.ConsultationQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 接诊队列仓储
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface ConsultationQueueRepository extends JpaRepository<ConsultationQueueEntity, Long> {

    /**
     * 按医生ID和状态查询队列（按挂号时间正序，先挂号先就诊）
     *
     * @param doctorId 医生用户ID
     * @param status    状态
     * @return 队列列表
     */
    List<ConsultationQueueEntity> findByDoctorIdAndStatusOrderByRegisteredAtAsc(Long doctorId, String status);

    /**
     * 按医生ID查询非完成队列（候诊+已叫号+接诊中）
     *
     * @param doctorId 医生用户ID
     * @param statuses 状态集合
     * @return 队列列表
     */
    List<ConsultationQueueEntity> findByDoctorIdAndStatusInOrderByRegisteredAtAsc(Long doctorId, List<String> statuses);
}
