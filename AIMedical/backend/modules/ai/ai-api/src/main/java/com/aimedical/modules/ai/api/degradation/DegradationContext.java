package com.aimedical.modules.ai.api.degradation;

public class DegradationContext {

    private String serviceName;
    private String operationName;

    public DegradationContext() {
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
