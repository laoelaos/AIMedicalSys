package com.aimedical.modules.registration.repository;

import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByPatientId(Long patientId);

    List<Registration> findByDoctorId(Long doctorId);

    List<Registration> findByStatus(RegistrationStatus status);

    List<Registration> findByScheduledDate(LocalDate date);
}