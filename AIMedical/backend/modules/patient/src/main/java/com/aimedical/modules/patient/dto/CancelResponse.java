package com.aimedical.modules.patient.dto;

public class CancelResponse {

    private boolean success;
    private String message;
    private double refundAmount;
    private boolean overWindow;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double v) { this.refundAmount = v; }
    public boolean isOverWindow() { return overWindow; }
    public void setOverWindow(boolean v) { this.overWindow = v; }
}
