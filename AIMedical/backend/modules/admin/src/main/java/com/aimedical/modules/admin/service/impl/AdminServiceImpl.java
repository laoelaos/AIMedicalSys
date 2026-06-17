package com.aimedical.modules.admin.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.admin.service.AdminService;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    @Override
    public Result<String> getPlaceholder() {
        return Result.success("admin placeholder");
    }
}
