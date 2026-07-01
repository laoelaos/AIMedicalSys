package com.aimedical.modules.prescription.dto.audit;

import java.time.LocalDateTime;
import java.util.List;

public class BlockResponse {

    private List<String> blockReasons;
    private String blockCode;
    private LocalDateTime blockTime;

    public BlockResponse() {
    }

    public BlockResponse(List<String> blockReasons, String blockCode, LocalDateTime blockTime) {
        this.blockReasons = blockReasons;
        this.blockCode = blockCode;
        this.blockTime = blockTime;
    }

    public List<String> getBlockReasons() {
        return blockReasons;
    }

    public void setBlockReasons(List<String> blockReasons) {
        this.blockReasons = blockReasons;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public LocalDateTime getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(LocalDateTime blockTime) {
        this.blockTime = blockTime;
    }
}
