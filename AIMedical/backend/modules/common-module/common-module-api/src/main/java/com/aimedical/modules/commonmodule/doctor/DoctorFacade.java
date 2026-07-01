package com.aimedical.modules.commonmodule.doctor;

import java.util.List;

public interface DoctorFacade {
    List<AvailableDoctor> findAvailableDoctorsByDepartment(String departmentId);
}
