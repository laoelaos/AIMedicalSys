package com.aimedical.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleMessageInterpolatorTest {

    private final SimpleMessageInterpolator interpolator = new SimpleMessageInterpolator();

    @Test
    void shouldReturnTemplateWhenArgsNull() {
        String result = interpolator.interpolate("测试消息", null);
        assertEquals("测试消息", result);
    }

    @Test
    void shouldReturnTemplateWhenArgsEmpty() {
        String result = interpolator.interpolate("测试消息", new Object[0]);
        assertEquals("测试消息", result);
    }

    @Test
    void shouldReplaceNumberedPlaceholders() {
        String result = interpolator.interpolate("订单{0}已过期，剩余{1}天", new Object[]{"ORD-001", "3"});
        assertEquals("订单ORD-001已过期，剩余3天", result);
    }

    @Test
    void shouldReplaceNamedPlaceholdersByPosition() {
        String result = interpolator.interpolate("账户已锁定，请{锁定时间}后重试", new Object[]{"30分钟"});
        assertEquals("账户已锁定，请30分钟后重试", result);
    }

    @Test
    void shouldReuseSameArgForMultiplePlaceholders() {
        String result = interpolator.interpolate("{0}和{0}", new Object[]{"相同"});
        assertEquals("相同和相同", result);
    }

    @Test
    void shouldReturnTemplateForNoPlaceholdersWithArgs() {
        String result = interpolator.interpolate("无占位符", new Object[]{"extra"});
        assertEquals("无占位符", result);
    }

    @Test
    void shouldReturnTemplateWhenArgsNullWithPlaceholders() {
        String result = interpolator.interpolate("订单{0}已过期", null);
        assertEquals("订单{0}已过期", result);
    }

    @Test
    void shouldReturnTemplateWhenNoPlaceholdersWithNonEmptyArgs() {
        String result = interpolator.interpolate("纯文本", new Object[]{"extra"});
        assertEquals("纯文本", result);
    }

    @Test
    void shouldHandleWhenMorePlaceholdersThanArgs() {
        String result = interpolator.interpolate("{0}和{1}", new Object[]{"only"});
        assertNotNull(result);
    }
}
