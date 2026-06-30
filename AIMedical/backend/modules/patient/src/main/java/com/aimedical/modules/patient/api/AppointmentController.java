package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/patient/appointment")
public class AppointmentController {

    private final AuthService authService;

    public AppointmentController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{doctorId}/slots")
    public Result<List<Map<String, Object>>> getSlots(@PathVariable Long doctorId) {
        authService.getCurrentUser();
        return Result.success(List.of(
                Map.<String, Object>of("slot_id", 1, "time_slot", "07-01 08:00-08:30", "available", true),
                Map.<String, Object>of("slot_id", 2, "time_slot", "07-01 09:00-09:30", "available", true),
                Map.<String, Object>of("slot_id", 3, "time_slot", "07-01 10:00-10:30", "available", true),
                Map.<String, Object>of("slot_id", 4, "time_slot", "07-01 14:00-14:30", "available", false),
                Map.<String, Object>of("slot_id", 5, "time_slot", "07-02 08:30-09:00", "available", true),
                Map.<String, Object>of("slot_id", 6, "time_slot", "07-02 10:30-11:00", "available", true)
        ));
    }

    @PostMapping
    public Result<Map<String, Object>> book(@RequestBody Map<String, Object> body) {
        authService.getCurrentUser();
        Integer doctorId = (Integer) body.get("doctor_id");
        Integer slotId = (Integer) body.get("slot_id");
        if (doctorId == null || slotId == null) {
            return Result.fail("PARAM_INVALID", "缺少必填参数");
        }
        return Result.success(Map.of("success", true, "message", "挂号预约成功！请按时前往就诊"));
    }
}
