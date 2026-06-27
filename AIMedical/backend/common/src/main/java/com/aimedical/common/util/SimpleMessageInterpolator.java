package com.aimedical.common.util;

import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class SimpleMessageInterpolator implements MessageInterpolator {

    @Override
    public String interpolate(String template, Object[] args) {
        if (args == null || args.length == 0) {
            return template;
        }
        if (template.matches(".*\\{\\d+.*\\}.*")) {
            try {
                return MessageFormat.format(template, args);
            } catch (IllegalArgumentException e) {
            } catch (Exception e) {
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
