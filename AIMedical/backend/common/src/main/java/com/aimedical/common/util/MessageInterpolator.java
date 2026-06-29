package com.aimedical.common.util;

public interface MessageInterpolator {
    String interpolate(String template, Object[] args);
}
