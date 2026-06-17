package com.aimedical.modules.admin.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.admin.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/placeholder")
    public Result<String> placeholder() {
        return adminService.getPlaceholder();
    }
}
