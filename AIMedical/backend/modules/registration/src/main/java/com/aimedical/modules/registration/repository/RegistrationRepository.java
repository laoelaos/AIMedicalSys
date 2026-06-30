package com.aimedical.modules.registration.repository;

import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Page<Registration> findByPatientId(Long patientId, Pageable pageable);

    Page<Registration> findByDoctorId(Long doctorId, Pageable pageable);

    List<Registration> findByStatus(RegistrationStatus status);

    List<Registration> findByScheduledDate(LocalDate date);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.scheduledDate = :date AND r.doctorId = :doctorId")
    long countByScheduledDateAndDoctorId(@Param("date") LocalDate date, @Param("doctorId") Long doctorId);
}
