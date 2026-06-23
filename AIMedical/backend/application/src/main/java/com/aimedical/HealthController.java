package com.aimedical;

import com.aimedical.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }
}
