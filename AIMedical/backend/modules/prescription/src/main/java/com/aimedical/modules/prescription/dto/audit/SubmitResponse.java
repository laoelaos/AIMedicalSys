package com.aimedical.modules.prescription.dto.audit;

public class SubmitResponse {

    private boolean submitted;
    private String prescriptionOrderId;
    private BlockResponse blockInfo;
    private String errorCode;
    private WarnResult warnResult;

    public SubmitResponse() {
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public String getPrescriptionOrderId() {
        return prescriptionOrderId;
    }

    public void setPrescriptionOrderId(String prescriptionOrderId) {
        this.prescriptionOrderId = prescriptionOrderId;
    }

    public BlockResponse getBlockInfo() {
        return blockInfo;
    }

    public void setBlockInfo(BlockResponse blockInfo) {
        this.blockInfo = blockInfo;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public WarnResult getWarnResult() {
        return warnResult;
    }

    public void setWarnResult(WarnResult warnResult) {
        this.warnResult = warnResult;
    }
}
