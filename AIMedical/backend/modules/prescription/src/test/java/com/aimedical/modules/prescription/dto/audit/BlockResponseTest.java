package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockResponseTest {

    @Test
    void shouldSetAndGetFields() {
        LocalDateTime now = LocalDateTime.now();
        BlockResponse resp = new BlockResponse(List.of("reason1"), "BLOCK_CODE", now);

        assertEquals(1, resp.getBlockReasons().size());
        assertEquals("BLOCK_CODE", resp.getBlockCode());
        assertEquals(now, resp.getBlockTime());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        BlockResponse resp = new BlockResponse();
        resp.setBlockReasons(List.of("a"));
        resp.setBlockCode("C");
        resp.setBlockTime(LocalDateTime.now());

        assertEquals("C", resp.getBlockCode());
    }
}
