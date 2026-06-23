package com.aimedical.modules.admin.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.admin.service.AdminService;
import com.aimedical.modules.admin.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {

    private final AdminService service = new AdminServiceImpl();
    private final AdminController controller = new AdminController(service);

    @Test
    void shouldDelegateToServiceAndReturnResult() {
        Result<String> result = controller.placeholder();
        assertEquals("SUCCESS", result.getCode());
        assertEquals("admin placeholder", result.getData());
    }
}
