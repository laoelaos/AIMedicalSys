package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.ConsultationQueueEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 带悲观锁的活跃记录查询：用于 callNext 并发叫号时串行化，
     * 防止"两路都通过活跃校验后争抢同一 WAITING 记录"。
     *
     * @param doctorId 医生用户ID
     * @param statuses 活跃状态集合
     * @return 队列列表（持有行锁直到事务结束）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ConsultationQueueEntity q WHERE q.doctorId = :doctorId AND q.status IN :statuses ORDER BY q.registeredAt ASC")
    List<ConsultationQueueEntity> findActiveForUpdate(@Param("doctorId") Long doctorId, @Param("statuses") List<String> statuses);

    /**
     * 带悲观锁的候诊记录查询：callNext 取首条 WAITING 时加锁，
     * 保证并发叫号按序串行化。
     *
     * @param doctorId 医生用户ID
     * @param status   状态
     * @return 队列列表（持有行锁直到事务结束）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ConsultationQueueEntity q WHERE q.doctorId = :doctorId AND q.status = :status ORDER BY q.registeredAt ASC")
    List<ConsultationQueueEntity> findByDoctorIdAndStatusForUpdate(@Param("doctorId") Long doctorId, @Param("status") String status);
}
