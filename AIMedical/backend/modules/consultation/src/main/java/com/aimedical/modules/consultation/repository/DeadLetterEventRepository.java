package com.aimedical.modules.consultation.repository;

import com.aimedical.modules.consultation.entity.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    @Query("SELECT e FROM DeadLetterEvent e WHERE e.state = :state AND e.retryCount < e.maxRetryCount")
    List<DeadLetterEvent> findByCompensableEvents(@Param("state") String state);
}
