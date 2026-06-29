package com.aimedical.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class SimpleMessageInterpolator implements MessageInterpolator {

    private static final Logger log = LoggerFactory.getLogger(SimpleMessageInterpolator.class);

    @Override
    public String interpolate(String template, Object[] args) {
        if (args == null || args.length == 0) {
            return template;
        }
        if (template.matches(".*\\{\\d+.*\\}.*")) {
            try {
                return MessageFormat.format(template, args);
            } catch (IllegalArgumentException e) {
                log.warn("MessageFormat 参数非法，返回原模板: template={}, err={}", template, e.getMessage());
            } catch (Exception e) {
                log.warn("MessageFormat 格式化异常，返回原模板: template={}", template, e);
                return template;
            }
        }
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{[^}]+\\}", String.valueOf(arg));
        }
        return result;
    }
}
